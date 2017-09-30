package com.cn.zhongdun110.camlog.iotc;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import com.cn.zhongdun110.camlog.iotc.BufferQueue.MediaBuffer;

import com.ingenic.glassassistant.iotc.LiveDataCodec;
import com.tutk.IOTC.AVAPIs;
import com.tutk.IOTC.AVIOCTRLDEFs;
import com.tutk.IOTC.IOTCAPIs;

public class IOTCClient {
	private static final boolean DEBUG = false;
	private static final String TAG = "IOTCClient";

	public static final int CAMERA_SET_LOW = 11;
	public static final int CAMERA_SET_MEDIUM = 12;
	public static final int CAMERA_SET_HIGH = 13;

	/*
	 * CACHE_BUF_COUNT : 15 ---> 1s
	 */
	private static final int CACHE_BUF_COUNT = 15;
	private static final int VIDEO_BUF_SIZE = 100000;
	private static final int VIDEO_BUF_SUM = 20;
	private static final int AUDIO_BUF_SIZE = 500;
	private static final int AUDIO_BUF_SUM = 30;

	public static final int IOTC_OK_INIT = 0;
	public static final int IOTC_OK_GETDATA = 1;
	public static final int IOTC_ERR_DEVICE_DEVICE_OFFLINE = 2;
	public static final int IOTC_ERR_UID_UNLICENSE = 3;
	public static final int IOTC_ERR_DEVICE_NOT_LISTENING = 4;
	public static final int IOTC_ERR_EXCEED_MAX_SEESION = 5;
	public static final int IOTC_ERR_WORING_PWD = 6;
	public static final int IOTC_ERR_INIT = 7;
	public static final int IOTC_ERR_GETDATA_TIMEOUT = 8;
	public static final int IOTC_ERR_GETVIDEO = 9;
	public static final int IOTC_ERR_GETAUDIO = 10;
	public static final int IOTC_ERR_CLOSE_BY_REMOTE = 11;

	private boolean mIsConnecting = false;
	private boolean mIsIPCameraStarted = false;

	private Handler mHandler = null;

	private LiveDataCodec mCodec = new LiveDataCodec();
	private IOTCServer mIOTCServer = new IOTCServer();

	private Thread mIOTCMainThread = null;

	private Object mStopLock = new Object();
	private boolean mIsStopCMDSendOK = false;
	private Object mLock = new Object();
	private boolean mDataThreadExit = false;
	private boolean mDataThreadExitExc = false;
	/* Video */
	private Thread mVideoGetThread = null;
	private Thread mVideoPutThread = null;
	private BufferQueue mVideoQue = null;

	/* Audio */
	private Thread mAudioGetThread = null;
	private Thread mAudioPutThread = null;
	private BufferQueue mAudioQue = null;

	/* Audio PCM */
	class AudioPCM {
		ByteBuffer mData;
		int mFrameSize;
		int mCapacity;

		public AudioPCM(int capacity) {
			mCapacity = capacity;
			mData = ByteBuffer.allocateDirect(mCapacity);
		}
	};

	private AudioPCM mAudioPCMData = new AudioPCM(2048);

	/* Video Dump */
	private boolean VideoDumpDebug = false;
	private String mVideoDumpFile = "/sdcard/H264.dump";
	private FileOutputStream mVideoDumpOut = null;

	/* Audio Dump */
	private boolean AudioDumpDebug = false;
	private String mAudioDumpFile = "/sdcard/AAC.dump";
	private FileOutputStream mAudioDumpOut = null;

	public int start(String uid, String pwd, int cameraSet) {
		int ret = -1;
		Log.e(TAG, "uid = " + uid);
		Log.e(TAG, "pwd = " + pwd);
		if ((uid == null) || (pwd == null) || (cameraSet < 0)) {
			ret = -1;
		}
		mIOTCMainThread = new IOTCMainThread(uid, pwd, cameraSet);
		mIOTCMainThread.start();

		return ret;
	}

	public void stop() {
		synchronized (mLock) {
			mDataThreadExit = true;
			mDataThreadExitExc = false;
			if (mIsConnecting) {
				IOTCAPIs.IOTC_Connect_Stop();
			}
		}
	}

	public void registerHandler(Handler handler) {
		mHandler = handler;
	}

	public void unregisterHandler() {
		mHandler = null;
	}

	public void registerSurface(SurfaceHolder holder) {
		mCodec.setSurface(holder);
	}

	/*
	 * 开始对讲是在IOTC，AVAPI成功初始化后才可以进行。 所以对于图标，一定是在收到初始化成功的消息后方可显示出来。
	 */
	public void startSpeaker() {
		Log.e(TAG, "-----startSpeaker");
		mIOTCServer.registerHandler(mHandler);
		mIOTCServer.start(mCodec);
	}

	public void stopSpeaker() {
		Log.e(TAG, "-----stopSpeaker");
		mIOTCServer.unregisterHandler();
		mIOTCServer.stop();
	}

	/*
	 * fileName 示例： /sdcard/Movies/test.mp4 返回值：
	 * 请务必检测返回值，如果为负数，请检测fileName文件是否存在，存在则删除
	 */
	public int setOutpuFileAndStartMP4Record(String fileName) {
		return mCodec.setOutputFile(fileName);
	}

	/*
	 * 返回值：请务必检测返回值，如果为负数，说明在record时发生错误，请删除该文件
	 */
	public int stopMP4Record() {
		return mCodec.stopRecord();
	}

	private class IOTCMainThread extends Thread {
		private String mUid = null;
		private String mPwd = null;
		private int mCameraSet = -1;

		public IOTCMainThread(String uid, String pwd, int cameraSet) {
			mUid = uid;
			mPwd = pwd;
			mCameraSet = cameraSet;
			if ((mCameraSet != CAMERA_SET_LOW)
					&& (mCameraSet != CAMERA_SET_MEDIUM)
					&& (mCameraSet != CAMERA_SET_HIGH)) {
				mCameraSet = CAMERA_SET_LOW;
			}
		}

		@Override
		public void run() {
			super.run();
			int ret = -1;
			Log.w(TAG, "Before IOTC_Initialize2");
			ret = IOTCAPIs.IOTC_Initialize2(0);
			Log.w(TAG, "After IOTC_Initialize2");
			if (ret != IOTCAPIs.IOTC_ER_NoERROR) {
				if (ret == IOTCAPIs.IOTC_ER_ALREADY_INITIALIZED) {
					Log.w(TAG, "IOTC Initialize, but has already initialized.");
					AVAPIs.avDeInitialize();
					IOTCAPIs.IOTC_DeInitialize();
					ret = IOTCAPIs.IOTC_Initialize2(0);
					Log.w(TAG,
							"IOTC reInitialize done which has already initialized.");
					if (ret < 0) {
						Log.e(TAG, "-1-IOTCAPIs.IOTC_Initialize2 Failed of [ "
								+ ret + " ]");
						mHandler.sendEmptyMessage(IOTC_ERR_INIT);
						return;
					}
				} else {
					Log.e(TAG, "-2-IOTCAPIs.IOTC_Initialize2 Failed of [ "
							+ ret + " ]");
					mHandler.sendEmptyMessage(IOTC_ERR_INIT);
					return;
				}
			}
			// Allow 3 sessions for video and two-way audio
			ret = AVAPIs.avInitialize(3);
			if (ret < 0) {
				IOTCAPIs.IOTC_DeInitialize();
				mHandler.sendEmptyMessage(IOTC_ERR_INIT);
				return;
			}
			synchronized (mLock) {
				mIsConnecting = true;
			}
			int sid = IOTCAPIs.IOTC_Connect_ByUID(mUid);
			if (sid < 0) {
				synchronized (mLock) {
					mIsConnecting = false;
				}
				Log.e(TAG, "IOTC_Connect_ByUID Failed of [ " + sid + " ]");
				AVAPIs.avDeInitialize();
				IOTCAPIs.IOTC_DeInitialize();
				if (IOTCAPIs.IOTC_ER_UNLICENSE == sid) {
					if (mHandler != null) {
						mHandler.sendEmptyMessage(IOTC_ERR_UID_UNLICENSE);
					}
				} else if (IOTCAPIs.IOTC_ER_EXCEED_MAX_PACKET_SIZE == sid) {
					if (mHandler != null) {
						mHandler.sendEmptyMessage(IOTC_ERR_EXCEED_MAX_SEESION);
					}
				} else if (IOTCAPIs.IOTC_ER_DEVICE_NOT_LISTENING == sid) {
					if (mHandler != null) {
						mHandler.sendEmptyMessage(IOTC_ERR_DEVICE_NOT_LISTENING);
					}
				} else if ((IOTCAPIs.IOTC_ER_NETWORK_UNREACHABLE == sid)
						|| (IOTCAPIs.IOTC_ER_DEVICE_OFFLINE == sid)) {
					if (mHandler != null) {
						mHandler.sendEmptyMessage(IOTC_ERR_DEVICE_DEVICE_OFFLINE);
					}
				} else {
					if (mHandler != null) {
						mHandler.sendEmptyMessage(IOTC_ERR_INIT);
					}
				}
				return;
			}
			synchronized (mLock) {
				mIsConnecting = false;
			}
			int[] srvType = new int[1];
			int[] reSend = new int[1];
			reSend[0] = -1;
			int avIndex = AVAPIs.avClientStart2(sid, "admin", mPwd, 20000,
					srvType, 0, reSend);
			// int avIndex = AVAPIs.avClientStart(sid, "admin", mPwd, 20000,
			// srvType, 0);
			if (avIndex < 0) {
				Log.e(TAG, "avClientStart Failed of [ " + avIndex + " ]");
				IOTCAPIs.IOTC_Session_Close(sid);
				AVAPIs.avDeInitialize();
				IOTCAPIs.IOTC_DeInitialize();
				if (AVAPIs.AV_ER_WRONG_VIEWACCorPWD == avIndex) {
					mHandler.sendEmptyMessage(IOTC_ERR_WORING_PWD);
				} else {
					mHandler.sendEmptyMessage(IOTC_ERR_INIT);
				}
				return;
			}
			mIOTCServer.setSIDandAvIndex(avIndex); // for speaker
			mCodec.start();
			if (startIPCamStream(avIndex, mCameraSet)) {
				mIsIPCameraStarted = true;
				if (VideoDumpDebug) {
					try {
						mVideoDumpOut = new FileOutputStream(mVideoDumpFile);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (AudioDumpDebug) {
					try {
						mAudioDumpOut = new FileOutputStream(mAudioDumpFile);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				mHandler.sendEmptyMessage(IOTC_OK_INIT);
				mVideoQue = new BufferQueue(VIDEO_BUF_SUM, VIDEO_BUF_SIZE);
				mAudioQue = new BufferQueue(AUDIO_BUF_SUM, AUDIO_BUF_SIZE);
				mVideoGetThread = new Thread(new VideoGet(avIndex), "VideoGet");
				mAudioGetThread = new Thread(new AudioGet(avIndex), "AudioGet");
				mVideoPutThread = new Thread(new VideoPut("VideoPut"));
				mAudioPutThread = new Thread(new AudioPut("AudioPut"));
				mVideoGetThread.start();
				mAudioGetThread.start();
				mVideoPutThread.start();
				mAudioPutThread.start();
				try {
					mVideoGetThread.join();
					mAudioGetThread.join();
					mVideoPutThread.join();
					mAudioPutThread.join();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (VideoDumpDebug) {
					try {
						mVideoDumpOut.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (AudioDumpDebug) {
					try {
						mAudioDumpOut.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else {
				mHandler.sendEmptyMessage(IOTC_ERR_INIT);
			}
			mCodec.close();
			synchronized (mLock) {
				if (!mDataThreadExitExc) {
					mDataThreadExitExc = false;
				}
			}
			if (mIsIPCameraStarted) {
				mIsIPCameraStarted = false;
				Thread sendStopCmdThread = new Thread(new SendStopCMD(avIndex),
						"SendStop");
				sendStopCmdThread.start();
				synchronized (mStopLock) {
					try {
						mStopLock.wait(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Log.e(TAG, "mIsStopCMDSendOK = " + mIsStopCMDSendOK);
					if (!mIsStopCMDSendOK) {
						AVAPIs.avSendIOCtrlExit(avIndex);
					}
					mIsStopCMDSendOK = false;
				}
				try {
					sendStopCmdThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			AVAPIs.avClientStop(avIndex);
			IOTCAPIs.IOTC_Session_Close(sid);
			AVAPIs.avDeInitialize();
			IOTCAPIs.IOTC_DeInitialize();
			Log.e(TAG, "IOTC Main Thread Exit Success");
		}
	}

	private boolean startIPCamStream(int avIndex, int cameraSet) {
		int ret = -1;
		byte[] ioCtrlBuf = new byte[8];
		byte[] camSet = intToBytes(cameraSet);
		System.arraycopy(camSet, 0, ioCtrlBuf, 0, 4);
		ret = AVAPIs.avSendIOCtrl(avIndex,
				AVIOCTRLDEFs.IOTYPE_USER_IPCAM_START, ioCtrlBuf, 8);
		if (ret < 0) {
			Log.e(TAG, "-1-avSendIOCtrl Fialed of [ " + ret + " ]");
			return false;
		}
		return true;
	}

	private boolean stopIPCamStream(int avIndex) {
		int ret = -1;
		ret = AVAPIs.avSendIOCtrl(avIndex, AVIOCTRLDEFs.IOTYPE_USER_IPCAM_STOP,
				new byte[8], 8);
		if (ret < 0) {
			Log.e(TAG, "-2-avSendIOCtrl Fialed of [ " + ret + " ]");
			return false;
		}
		return true;
	}

	private class SendStopCMD implements Runnable {
		private int mAvIndex = -1;

		public SendStopCMD(int avIndex) {
			mAvIndex = avIndex;
		}

		@Override
		public void run() {
			stopIPCamStream(mAvIndex);
			synchronized (mStopLock) {
				mIsStopCMDSendOK = true;
				mStopLock.notify();
			}
		}
	}

	private class VideoGet implements Runnable {
		private int FRAME_INFO_SIZE = 16;
		private int mAvIndex = -1;
		private int mGetFailedCount = 0;

		public VideoGet(int avIndex) {
			mAvIndex = avIndex;
		}

		@Override
		public void run() {
			byte[] frameInfo = new byte[FRAME_INFO_SIZE];
			int[] actualFrameInfoSize = new int[1];
			int[] frameNumber = new int[1];
			int[] actualFrameSize = new int[1];
			int[] expectedFrameSize = new int[1];
			for (;;) {
				synchronized (mLock) {
					if (mDataThreadExit || mDataThreadExitExc) {
						break;
					}
				}
				MediaBuffer media = mVideoQue.getFreeBuffer(30);
				if (media != null) {
					int ret = AVAPIs.avRecvFrameData2(mAvIndex, media.mData,
							VIDEO_BUF_SIZE, actualFrameSize, expectedFrameSize,
							frameInfo, FRAME_INFO_SIZE, actualFrameInfoSize,
							frameNumber);
					if (DEBUG) {
						Log.e(TAG, "Recv Video Frame [ " + frameNumber[0]
								+ " ] = " + ret);
					}
					if (ret < 0) {
						if (mGetFailedCount > 800) {
							synchronized (mLock) {
								mDataThreadExitExc = true;
							}
							mHandler.sendEmptyMessage(IOTC_ERR_GETDATA_TIMEOUT);
							break;
						}
						if (ret == AVAPIs.AV_ER_DATA_NOREADY) {
							try {
								mGetFailedCount++;
								mVideoQue.undoFreeBuffer(media);
								Thread.sleep(30);
								continue;
							} catch (InterruptedException e) {
								e.printStackTrace();
								break;
							}
						} else if (ret == AVAPIs.AV_ER_LOSED_THIS_FRAME) {
							Log.w(TAG, "Lost video frame number [ "
									+ frameNumber[0] + " ]");
							mGetFailedCount++;
							mVideoQue.undoFreeBuffer(media);
							continue;
						} else if (ret == AVAPIs.AV_ER_INCOMPLETE_FRAME) {
							Log.w(TAG, "Incomplete video frame number [ "
									+ frameNumber[0] + " ]");
							mGetFailedCount++;
							mVideoQue.undoFreeBuffer(media);
							continue;
						} else if (ret == AVAPIs.AV_ER_SESSION_CLOSE_BY_REMOTE) {
							Log.e(TAG, "Thread "
									+ Thread.currentThread().getName()
									+ " AV_ER_SESSION_CLOSE_BY_REMOTE");
							synchronized (mLock) {
								mDataThreadExitExc = true;
							}
							mHandler.sendEmptyMessage(IOTC_ERR_CLOSE_BY_REMOTE);
							break;
						} else if (ret == AVAPIs.AV_ER_REMOTE_TIMEOUT_DISCONNECT) {
							Log.e(TAG, "Thread "
									+ Thread.currentThread().getName()
									+ " AV_ER_REMOTE_TIMEOUT_DISCONNECT");
							synchronized (mLock) {
								mDataThreadExitExc = true;
							}
							mHandler.sendEmptyMessage(IOTC_ERR_GETVIDEO);
							break;
						} else if (ret == AVAPIs.AV_ER_INVALID_SID) {
							Log.e(TAG, "Thread "
									+ Thread.currentThread().getName()
									+ " Session cant be used anymore");
							synchronized (mLock) {
								mDataThreadExitExc = true;
							}
							mHandler.sendEmptyMessage(IOTC_ERR_GETVIDEO);
							break;
						}
					} else {
						mGetFailedCount = 0;
						media.mFrameSize = ret;
						media.mTimeStamp = frameInfoToTimeStamp(frameInfo);
						mVideoQue.putFreeBuffer(media);
					}
				}
			}
			Log.e(TAG, "Thread VideoGet Exit Successful");
		}
	}

	private class VideoPut implements Runnable {
		private boolean mIsCacheOK = false;

		public VideoPut(String name) {
		}

		@Override
		public void run() {
			for (;;) {
				synchronized (mLock) {
					if (mDataThreadExit || mDataThreadExitExc) {
						break;
					}
				}
				if (!mIsCacheOK) {
					if (mVideoQue.getBusyBufferCount() < CACHE_BUF_COUNT) {
						try {
							Thread.sleep(60);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						continue;
					} else {
						mIsCacheOK = true;
						mHandler.sendEmptyMessage(IOTC_OK_GETDATA);
					}
				}
				MediaBuffer media = mVideoQue.getBusyBuffer(30);
				if (media != null) {
					mCodec.setVideoData(media);
					if (VideoDumpDebug) {
						try {
							mVideoDumpOut.write(media.mData, 0,
									media.mFrameSize);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					mVideoQue.putBusyBuffer(media);
				}
			}
			Log.e(TAG, "Thread VideoPut has Exit Sucessful");
		}
	}

	/*
	 * Audio Data Thread
	 */
	private class AudioGet implements Runnable {
		private int FRAME_INFO_SIZE = 16;
		private int mAvIndex = -1;

		public AudioGet(int avIndex) {
			mAvIndex = avIndex;
		}

		@Override
		public void run() {
			byte[] frameInfo = new byte[FRAME_INFO_SIZE];

			for (;;) {
				synchronized (mLock) {
					if (mDataThreadExit || mDataThreadExitExc) {
						break;
					}
				}
				int[] frameNumber = new int[1];
				MediaBuffer media = mAudioQue.getFreeBuffer(30);
				if (media != null) {
					int ret = AVAPIs.avRecvAudioData(mAvIndex, media.mData,
							AUDIO_BUF_SIZE, frameInfo, FRAME_INFO_SIZE,
							frameNumber);
					if (DEBUG) {
						Log.e(TAG, "Recv Audio Frame [ " + frameNumber[0]
								+ " ] = " + ret);
					}
					if (ret < 0) {
						if (ret == AVAPIs.AV_ER_DATA_NOREADY) {
							try {
								mAudioQue.undoFreeBuffer(media);
								Thread.sleep(30);
								continue;
							} catch (InterruptedException e) {
								e.printStackTrace();
								break;
							}
						} else if (ret == AVAPIs.AV_ER_LOSED_THIS_FRAME) {
							Log.w(TAG, "Lost Audio frame number [ "
									+ frameNumber[0] + " ]");
							mAudioQue.undoFreeBuffer(media);
							continue;
						} else if (ret == AVAPIs.AV_ER_INCOMPLETE_FRAME) {
							Log.w(TAG, "Incomplete Audio frame number [ "
									+ frameNumber[0] + " ]");
							mAudioQue.undoFreeBuffer(media);
							continue;
						} else if (ret == AVAPIs.AV_ER_SESSION_CLOSE_BY_REMOTE) {
							Log.e(TAG, "Thread "
									+ Thread.currentThread().getName()
									+ " AV_ER_SESSION_CLOSE_BY_REMOTE");
							synchronized (mLock) {
								mDataThreadExitExc = true;
							}
							mHandler.sendEmptyMessage(IOTC_ERR_CLOSE_BY_REMOTE);
							break;
						} else if (ret == AVAPIs.AV_ER_REMOTE_TIMEOUT_DISCONNECT) {
							Log.e(TAG, "Thread "
									+ Thread.currentThread().getName()
									+ " AV_ER_REMOTE_TIMEOUT_DISCONNECT");
							synchronized (mLock) {
								mDataThreadExitExc = true;
							}
							mHandler.sendEmptyMessage(IOTC_ERR_GETAUDIO);
							break;
						} else if (ret == AVAPIs.AV_ER_INVALID_SID) {
							Log.e(TAG, "Thread "
									+ Thread.currentThread().getName()
									+ " Session cant be used anymore");
							synchronized (mLock) {
								mDataThreadExitExc = true;
							}
							mHandler.sendEmptyMessage(IOTC_ERR_GETAUDIO);
							break;
						}
					} else {
						media.mFrameSize = ret;
						media.mTimeStamp = frameInfoToTimeStamp(frameInfo);
						mAudioQue.putFreeBuffer(media);
					}
				}
			}
			Log.e(TAG, "Thread AudioGet Exit Successful");
		}
	}

	private class AudioPut implements Runnable {
		private static final boolean PCM_DUMP_DEBUG = false;
		AudioTrack mTrack = null;
		private boolean mIsCacheOK = false;

		public AudioPut(String name) {
			mTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
					AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT, 2048,
					AudioTrack.MODE_STREAM);
		}

		@Override
		public void run() {
			FileOutputStream outPCM = null;
			if (PCM_DUMP_DEBUG) {
				String File = "/sdcard/PCMDE.dump";
				try {
					outPCM = new FileOutputStream(File);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			for (;;) {
				synchronized (mLock) {
					if (mDataThreadExit || mDataThreadExitExc) {
						break;
					}
				}
				if (!mIsCacheOK) {
					if (mAudioQue.getBusyBufferCount() < CACHE_BUF_COUNT) {
						try {
							Thread.sleep(60);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						continue;
					} else {
						mIsCacheOK = true;
					}
				}
				MediaBuffer media = mAudioQue.getBusyBuffer(30);
				if (media != null) {
					if (AudioDumpDebug) {
						try {
							mAudioDumpOut.write(media.mData, 0,
									media.mFrameSize);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					if (media.mFrameSize > 2) {
						mCodec.setAudioData(media, mAudioPCMData);
						if (PCM_DUMP_DEBUG) {
							try {
								outPCM.write(mAudioPCMData.mData.array());
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						mTrack.write(mAudioPCMData.mData.array(), 0, 2048);
						mTrack.play();
						mTrack.stop();
					}
					mAudioQue.putBusyBuffer(media);
				}
			}
			if (PCM_DUMP_DEBUG) {
				if (outPCM != null) {
					try {
						outPCM.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			mTrack.release();
			Log.e(TAG, "Thread AudioPut has Exit Sucessful");
		}
	}

	private int frameInfoToTimeStamp(byte[] b) {
		int i = (b[15] << 24) & 0xFF000000;
		i |= (b[14] << 16) & 0xFF0000;
		i |= (b[13] << 8) & 0xFF00;
		i |= b[12] & 0xFF;
		return i;
	}

	private static byte[] intToBytes(int i) {
		byte[] b = new byte[4];
		b[3] = (byte) (i >> 24);
		b[2] = (byte) (i >> 16);
		b[1] = (byte) (i >> 8);
		b[0] = (byte) i;
		return b;
	}
}
