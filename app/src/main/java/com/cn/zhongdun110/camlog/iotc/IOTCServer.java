package com.cn.zhongdun110.camlog.iotc;

import java.io.FileOutputStream;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;
import com.cn.zhongdun110.camlog.iotc.BufferQueue.MediaBuffer;

import com.ingenic.glassassistant.iotc.LiveDataCodec;
import com.tutk.IOTC.AVAPIs;
import com.tutk.IOTC.AVIOCTRLDEFs;

public class IOTCServer {
	private static final boolean DEBUG = true;
	private static final String TAG = "IOTCServer";

	public static final int SPEAKER_ERR_SEND_FAILED = 111;

	private int AAC_BUFFER_SIZE = 300;
	private int AAC_BUFFER_SUM = 100;

	private LiveDataCodec mEncoder = null;
	private Handler mHandler = null;

	private int mAvLiveIndex = -1;
	private boolean mIsSpeakerStarted = false;

	private Thread mIOTCMainThread = null;
	private Object mLock = new Object();
	private boolean mThreadExit = false;

	private Thread mAudioGetThread = null; // Get PCM Data and encode to AAC
	private Thread mAudioPutThread = null; // Put AAC
	private BufferQueue mAudioQue = null;

	class AudioPCM {
		byte[] mData;
		int mFrameSize;
		int mCapacity;

		public AudioPCM(int capacity) {
			mCapacity = capacity;
			mData = new byte[mCapacity];
		}
	};

	public int start(LiveDataCodec encoder) {
		if (encoder == null) {
			Log.e(TAG, "Err : Audio Data Encoder is NULL");
			return -1;
		}
		synchronized (mLock) {
			mThreadExit = false;
		}
		mEncoder = encoder;

		mIOTCMainThread = new IOTCMainThread();
		mIOTCMainThread.start();
		return 0;
	}

	public void stop() {
		synchronized (mLock) {
			mThreadExit = true;
		}
	}

	public void setSIDandAvIndex(int avIndex) {
		mAvLiveIndex = avIndex;
		Log.e(TAG, "mAvLiveIndex = " + mAvLiveIndex);
	}

	public void registerHandler(Handler handler) {
		mHandler = handler;
	}

	public void unregisterHandler() {
		mHandler = null;
	}

	private class IOTCMainThread extends Thread {

		@Override
		public void run() {
			super.run();
			synchronized (mLock) {
				mIsSpeakerStarted = false;
			}
			if (startIPCamSpeaker(mAvLiveIndex)) {
				synchronized (mLock) {
					mIsSpeakerStarted = true;
				}
				mAudioQue = new BufferQueue(AAC_BUFFER_SUM, AAC_BUFFER_SIZE);
				mAudioGetThread = new Thread(new AudioGet(), "AudioEnc");
				mAudioPutThread = new Thread(new AudioPut(mAvLiveIndex),
						"AudioSend");
				mAudioGetThread.start();
				mAudioPutThread.start();
				try {
					mAudioGetThread.join();
					mAudioPutThread.join();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				Log.e(TAG, "Send Start Speaker Cmd Failed.");
				return;
			}
			synchronized (mLock) {
				if (mIsSpeakerStarted) {
					stopIPCamSpeaker(mAvLiveIndex);
				}
			}
			Log.e(TAG, "Speaker IOTCMainThread exit successful....");
		}
	}

	private class AudioGet implements Runnable {

		private static final boolean PCM_DUMP_DEBUG = false;
		private static final boolean AAC_DUMP_DEBUG = false;
		private int mAudioSource = MediaRecorder.AudioSource.MIC;
		private int mSampleRate = 8000;
		private int mChannel = AudioFormat.CHANNEL_IN_MONO;
		private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
		private int mBufferSizeInBytes = 2048;
		private AudioRecord mRecord = null;
		private AudioPCM mAudioPCMData = null;

		public AudioGet() {
			mAudioPCMData = new AudioPCM(mBufferSizeInBytes);
			mRecord = new AudioRecord(mAudioSource, mSampleRate, mChannel,
					mAudioFormat, mBufferSizeInBytes);
		}

		@Override
		public void run() {
			mRecord.startRecording();
			FileOutputStream outPCM = null;
			if (PCM_DUMP_DEBUG) {
				String File = "/sdcard/PCM.dump";
				try {
					outPCM = new FileOutputStream(File);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			FileOutputStream outAAC = null;
			if (AAC_DUMP_DEBUG) {
				String File = "/sdcard/AAC.dump";
				try {
					outAAC = new FileOutputStream(File);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			for (;;) {
				synchronized (mLock) {
					if (mThreadExit) {
						break;
					}
				}
				int readSize = mRecord.read(mAudioPCMData.mData, 0,
						mBufferSizeInBytes);
				if (AudioRecord.ERROR_INVALID_OPERATION != readSize) {
					if (PCM_DUMP_DEBUG && (outPCM != null)) {
						try {
							outPCM.write(mAudioPCMData.mData, 0, readSize);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					mAudioPCMData.mFrameSize = readSize;
					MediaBuffer media = mAudioQue.getFreeBuffer(30);
					if (media != null) {
						mEncoder.setAudioPCM(mAudioPCMData, media,
								AAC_BUFFER_SIZE);
						if (media.mFrameSize > 0) {
							if (AAC_DUMP_DEBUG && (outAAC != null)) {
								try {
									outAAC.write(media.mData, 0,
											media.mFrameSize);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
							mAudioQue.putFreeBuffer(media);
						} else {
							mAudioQue.undoFreeBuffer(media);
						}
					}
				}
			}
			if (PCM_DUMP_DEBUG && (outPCM != null)) {
				try {
					outPCM.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (AAC_DUMP_DEBUG && (outAAC != null)) {
				try {
					outAAC.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			mRecord.stop();
			mRecord.release();
			mRecord = null;
			Log.e(TAG, "Speaker AudioGet Thread Exit Successful");
		}
	}

	class AudioPut implements Runnable {
		private boolean SEND_AAC_DUMP_DEBUG = false;
		private int SEND_PACKAGE_SUM = 4;
		int mLiveIndex = -1;
		int mErrCount = 0;
		int mFrameCount = 0;

		public AudioPut(int liveIndex) {
			mLiveIndex = liveIndex;
		}

		@Override
		public void run() {
			int ret = -1;
			FileOutputStream outAAC = null;
			if (SEND_AAC_DUMP_DEBUG) {
				String File = "/sdcard/ENAAC.dump";
				try {
					outAAC = new FileOutputStream(File);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			for (;;) {
				synchronized (mLock) {
					if (mThreadExit) {
						break;
					}
				}
				byte[] ioCtrlBuf = new byte[1024];
				int packageCount = 0;
				int ioCtrlBufCurrentSize = 0;
				for (;;) {
					synchronized (mLock) {
						if (mThreadExit) {
							break;
						}
					}
					if (packageCount == SEND_PACKAGE_SUM) {
						break;
					}
					MediaBuffer media = mAudioQue.getBusyBuffer(30);
					if (media != null) {
						if (media.mFrameSize + ioCtrlBufCurrentSize < 1024) {
							mFrameCount++;
							Log.i(TAG, "frame [ " + mFrameCount + " ] = "
									+ media.mFrameSize);
							byte[] frameCount = intToBytes(mFrameCount);
							byte[] frameSize = intToBytes(media.mFrameSize);
							System.arraycopy(frameCount, 0, ioCtrlBuf,
									ioCtrlBufCurrentSize, 4);
							System.arraycopy(frameSize, 0, ioCtrlBuf,
									ioCtrlBufCurrentSize + 4, 4);
							System.arraycopy(media.mData, 0, ioCtrlBuf,
									ioCtrlBufCurrentSize + 8, media.mFrameSize);
							ioCtrlBufCurrentSize += (4 + 4 + media.mFrameSize);
							if (SEND_AAC_DUMP_DEBUG && (outAAC != null)) {
								try {
									outAAC.write(media.mData, 0,
											media.mFrameSize);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
						packageCount++;
						mAudioQue.putBusyBuffer(media);
					}
				}
				ret = AVAPIs.avSendIOCtrl(mLiveIndex,
						AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SPEAKERDATA, ioCtrlBuf,
						ioCtrlBufCurrentSize);
				if (ret < 0) {
					Log.e(TAG, "IOTYPE_USER_IPCAM_SPEAKERDATA Fialed of [ "
							+ ret + " ]");
					mErrCount++;
					if (mErrCount > 15) {
						mHandler.sendEmptyMessage(SPEAKER_ERR_SEND_FAILED);
					}
				} else {
					mErrCount = 0;
				}
			}
			if (SEND_AAC_DUMP_DEBUG && (outAAC != null)) {
				try {
					outAAC.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			Log.e(TAG, "Speaker AudioPut Thread Exit Successful");
		}
	}

	private boolean startIPCamSpeaker(int avIndex) {
		int ret = -1;
		ret = AVAPIs.avSendIOCtrl(avIndex,
				AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SPEAKERSTART, new byte[8], 8);
		if (ret < 0) {
			Log.e(TAG, "-1-avSendIOCtrl Fialed of [ " + ret + " ]");
			return false;
		}
		return true;
	}

	private boolean stopIPCamSpeaker(int avIndex) {
		int ret = -1;
		ret = AVAPIs.avSendIOCtrl(avIndex,
				AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SPEAKERSTOP, new byte[8], 8);
		if (ret < 0) {
			Log.e(TAG, "-2-avSendIOCtrl Fialed of [ " + ret + " ]");
			return false;
		}
		return true;
	}

	public static byte[] intToBytes(int i) {
		byte[] b = new byte[4];
		b[3] = (byte) (i >> 24);
		b[2] = (byte) (i >> 16);
		b[1] = (byte) (i >> 8);
		b[0] = (byte) i;
		return b;
	}
}
