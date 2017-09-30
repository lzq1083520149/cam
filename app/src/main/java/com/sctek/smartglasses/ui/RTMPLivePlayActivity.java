package com.sctek.smartglasses.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.cn.zhongdun110.camlog.R;
import com.cn.zhongdun110.camlog.SyncApp;
import com.cn.zhongdun110.camlog.iotc.BufferQueue;
import com.ingenic.glassassistant.iotc.LiveDataCodec;
import com.sctek.smartglasses.utils.RTMPLivePush;
import com.sctek.smartglasses.utils.Utils;
import com.sctek.smartglasses.utils.WifiUtils;
import tv.danmaku.ijk.media.sample.widget.media.IjkVideoView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.FileOutputStream;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class RTMPLivePlayActivity extends Activity implements IMediaPlayer.OnErrorListener, OnKeyListener, IMediaPlayer.OnInfoListener, IMediaPlayer.OnCompletionListener, IMediaPlayer.OnPreparedListener {
	private final String TAG = "RTMPLivePlayActivity";
	private final boolean DEBUG = true;

	private final String URL_BASE = "http://glass.ingenic.com:5080/live/";
	private final String URL_GET_LIVE_PLAY = URL_BASE + "getLivePlayUrl";
	private final String URL_GET_TALK_PUSH_URL = URL_BASE + "getTalkPushUrl";
	private final String URL_NOTICE_TALK_END = URL_BASE + "noticeTalkEnd";
	private final String URL_NOTICE_WATCH_END = URL_BASE + "noticeWatchEnd";

	private final String PARAMETER_CLIENT_UID = "ClientUid";
	private final String PARAMETER_GLASS_UID = "GlassUid";
	private final String PARAMETER_ERROR_CODE = "ErrorCode";
	private final String PARAMETER_ERROR_MESSAGE = "ErrorMessage";
	private final String PARAMETER_PLAY_URL = "PlayUrl";
	private final String PARAMETER_TALK_URL = "TalkUrl";
	private final String PARAMETER_CMP_UID = "CmpUid";
	private String mClientUid;
	private String mGlassUid;
	private String mPlayUrl;
	private String mTalkUrl;

	private IjkVideoView mIjkVideoView;
	//private AndroidMediaController mMediaController;
	private ProgressDialog mProgressDialog;
	private Builder mAlertDialog;
	private Dialog mVoiceDialog;
	private ProgressBar mProgressBar;
	private ImageView mTalkBackView;

	private Object mLock = new Object();
	private boolean mThreadExit = false;

	private Thread mAudioGetThread = null; // Get PCM Data and encode to AAC
	private Thread mAudioPutThread = null; // Put AAC
	private BufferQueue mAudioQue = null;
	private LiveDataCodec mLiveDataCodec;
	private RTMPTalkThread mRTMPTalkThread;
	private final int TALK_NO = 0;
	private final int TALK_REQUEST = 1;
	private final int TALK_ING = 2;
	private int mTalkState = TALK_NO;   // talk state
	private int mPlayState = STATE_IDLE;  // play state
	private static final int STATE_ERROR = -1;
	private static final int STATE_IDLE = 0;
	private static final int STATE_PREPARED = 1;
	private static final int STATE_PLAYING = 2;
	private int mRestartNum;

	private int AAC_BUFFER_SIZE = 300;
	private int AAC_BUFFER_SUM = 100;

	private RTMPLivePush mRTMPLivePush;
	private	final int MSG_GET_PALYURL_SUCCESS = 1;
	private	final int MSG_GET_PALYURL_FAIL = 2;
	private	final int MSG_GET_PALYURL_CLOSED = 3;
	private final int MSG_GET_TALKURL_SUCCESS = 4;
	private final int MSG_GET_TALKURL_FAIL = 5;
	private final int MSG_GET_TALKURL_INUSED = 6;
	private final int MSG_GET_DATA_SUCCESS = 7;
	private final int MSG_RECONNECT = 8;
	private Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_GET_PALYURL_SUCCESS:
					startLivePlay();
					break;
				case MSG_GET_PALYURL_FAIL:
					hideProgress();
					showDialog(getString(R.string.rtmp_playurl_get_fail));
					break;
				case MSG_GET_PALYURL_CLOSED:
					hideProgress();
					showDialog(getString(R.string.rtmp_live_closed));
					break;
				case MSG_GET_TALKURL_SUCCESS:
					mTalkBackView.setEnabled(true);
					startTalk(mTalkUrl);
					hideProgress();
					showVoiceDialog();
					break;
				case MSG_GET_TALKURL_FAIL:
					mTalkBackView.setEnabled(true);
					mTalkState = TALK_NO;
					showToast(getString(R.string.rtmp_talkurl_get_fail));
					hideProgress();
					break;
				case MSG_GET_TALKURL_INUSED:
					mTalkBackView.setEnabled(true);
					mTalkState = TALK_NO;
					showToast(getString(R.string.rtmp_talkurl_inused));
					hideProgress();
					break;
				case MSG_GET_DATA_SUCCESS:
					mTalkBackView.setVisibility(View.VISIBLE);
					hideProgress();
					break;
				case MSG_RECONNECT:
					mRestartNum++;
					restartPlay();
					break;
				default:
					break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rtmplive_play);
		mGlassUid = getIntent().getStringExtra("room_name");
		mRTMPLivePush = new RTMPLivePush(this);
		mClientUid = Utils.getClientUid(this);
		initView();
		initIjkpalyer();

		getLivePlayUrl();
	}


	private void initView() {
		mProgressDialog = new ProgressDialog(this,R.style.MyTheme);
		mProgressDialog.setMessage(getString(R.string.remote_live_msg_get_data));
		mProgressDialog.setCancelable(false);
		mProgressDialog.setOnKeyListener(this);
		mProgressDialog.show();


		mAlertDialog = new AlertDialog.Builder(this);
		mAlertDialog.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						finish();
					}
				});
		mProgressBar = (ProgressBar)findViewById(R.id.progress_bar);
		mTalkBackView = (ImageView) findViewById(R.id.talkback);
		mTalkBackView.setOnClickListener(mTalkOnClickListener);
		mTalkBackView.setAlpha(130);

	}

	private void initIjkpalyer() {
		System.loadLibrary("ijkffmpeg");
		System.loadLibrary("ijksdl");
		System.loadLibrary("ijkplayer");
		IjkMediaPlayer.native_profileBegin("libijkplayer.so");

		mIjkVideoView = (IjkVideoView) findViewById(R.id.video_view);
		mIjkVideoView.setOnErrorListener(this);
		mIjkVideoView.setOnInfoListener(this);
		mIjkVideoView.setOnPreparedListener(this);
		mIjkVideoView.setOnCompletionListener(this);
	}

	protected void startLivePlay() {
		mIjkVideoView.setVideoPath(mPlayUrl);
		mIjkVideoView.start();
		mIjkVideoView.setKeepScreenOn(true);

	}
	private void restartPlay(){
		Log.i(TAG, "restartPlay");
		mIjkVideoView.setVideoPath(mPlayUrl);
		mIjkVideoView.seekTo(0);
		mIjkVideoView.start();
	}

	private void showDialog(String msg){
		mAlertDialog.setMessage(msg);
		mAlertDialog.show();
	}

	private void showProgress(String msg){
		mProgressDialog.setMessage(msg);
		mProgressDialog.show();
	}

	private void hideProgress(){
		if(mProgressDialog.isShowing())
			mProgressDialog.hide();
	}

	private void showToast(String msg){
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}

	private void showVoiceDialog() {
		mVoiceDialog = new Dialog(this, R.style.DialogStyle);
		mVoiceDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mVoiceDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
		mVoiceDialog.getWindow().setGravity(Gravity.LEFT);
		mVoiceDialog.setContentView(R.layout.my_dialog);
		mVoiceDialog.setCanceledOnTouchOutside(false);
		mVoiceDialog.setOnKeyListener(this);
		ImageView dialog_img = (ImageView) mVoiceDialog.findViewById(R.id.dialog_img);
		AnimationDrawable animationDrawable = (AnimationDrawable) dialog_img
				.getDrawable();
		animationDrawable.start();
		mVoiceDialog.show();
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG,"onStop");
		mIjkVideoView.stopPlayback();
		mIjkVideoView.release(true);
		mIjkVideoView.stopBackgroundPlay();
		IjkMediaPlayer.native_profileEnd();
		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(DEBUG)Log.i(TAG,"onDestroy");
		mThreadExit = true;
		if (mLiveDataCodec != null) {
			mLiveDataCodec.close();
		}
		mProgressDialog.dismiss();
		mHandler.removeCallbacks(null);
		if(mTalkState != TALK_NO)
			stopTalk();
	}

	@Override
	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			dialog.cancel();
			finish();
		}
		return false;
	}

	private View.OnClickListener mTalkOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			if(DEBUG)Log.i(TAG, "onclick mTalkState="+mTalkState);
			if(mTalkState == TALK_NO){
				mTalkState = TALK_REQUEST;
				showProgress(getString(R.string.rtmp_talkurl_requesting));
				mTalkBackView.setEnabled(false);
				getTalkPushUrl();
			} else{
				mTalkState = TALK_NO;
				hideProgress();
				stopTalk();
			}
		}
	};

	private void getLivePlayUrl() {
		if(DEBUG)Log.i(TAG, "getLivePlayUrl");
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					StringBuilder getLiveUrl = new StringBuilder(
							URL_GET_LIVE_PLAY + "?");
					getLiveUrl.append(PARAMETER_GLASS_UID+"="+mGlassUid);
					getLiveUrl.append("&"+PARAMETER_CLIENT_UID + "=" + mClientUid);
					getLiveUrl.append("&" + PARAMETER_CMP_UID + "=" +SyncApp.COMPANY_UID);
					HttpGet httpGet = new HttpGet(getLiveUrl.toString());
					HttpClient httpClient = new DefaultHttpClient();
					HttpResponse httpResponse = httpClient.execute(httpGet);
					Log.i(TAG, "statusCode="
							+ httpResponse.getStatusLine().getStatusCode());
					if (httpResponse.getStatusLine().getStatusCode() == 200) {
						HttpEntity entity = httpResponse.getEntity();
						String response = EntityUtils.toString(entity, "utf-8");
						JSONObject resp = new JSONObject(response);
						int errorCode = resp.getInt(PARAMETER_ERROR_CODE);
						String errorMessage = resp
								.getString(PARAMETER_ERROR_MESSAGE);
						Log.i(TAG, "errorCode=" + errorCode + ",errorMessage="
								+ errorMessage);
						if (errorCode == 0) {
							String url = resp.getString(PARAMETER_PLAY_URL);
							if (url != null && url.length() != 0) {
								if(DEBUG)Log.i(TAG,"url="+url);
								mPlayUrl = url;
								mHandler.sendEmptyMessage(MSG_GET_PALYURL_SUCCESS);
							}
						} else {
							mHandler.sendEmptyMessage(MSG_GET_PALYURL_CLOSED);
						}
					}else {
						mHandler.sendEmptyMessage(MSG_GET_PALYURL_FAIL);
					}
				} catch (Exception e) {
					mHandler.sendEmptyMessage(MSG_GET_PALYURL_FAIL);
					e.printStackTrace();
				}
			}
		}).start();
	}

	private void getTalkPushUrl() {
		if(DEBUG)Log.i(TAG, "getTalkPushUrl");
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					StringBuilder getLiveUrl = new StringBuilder(URL_GET_TALK_PUSH_URL + "?");
					getLiveUrl.append(PARAMETER_GLASS_UID+"="+mGlassUid);
					getLiveUrl.append("&"+PARAMETER_CLIENT_UID + "=" + mClientUid);
					getLiveUrl.append("&" + PARAMETER_CMP_UID + "=" +SyncApp.COMPANY_UID);
					HttpGet httpGet = new HttpGet(getLiveUrl.toString());
					HttpClient httpClient = new DefaultHttpClient();
					HttpResponse httpResponse = httpClient.execute(httpGet);
					if(DEBUG)Log.i(TAG, "statusCode=" + httpResponse.getStatusLine().getStatusCode());
					if (httpResponse.getStatusLine().getStatusCode() == 200) {
						HttpEntity entity = httpResponse.getEntity();
						String response = EntityUtils.toString(entity, "utf-8");
						JSONObject resp = new JSONObject(response);
						int errorCode = resp.getInt(PARAMETER_ERROR_CODE);
						String errorMessage = resp
								.getString(PARAMETER_ERROR_MESSAGE);
						Log.i(TAG, "errorCode=" + errorCode + ",errorMessage="
								+ errorMessage);
						if (errorCode == 0) {
							String url = resp.getString(PARAMETER_TALK_URL);
							if (url != null && url.length() != 0) {
								if(DEBUG)Log.i(TAG,"talk url="+url);
								mTalkUrl = url;
								mHandler.sendEmptyMessage(MSG_GET_TALKURL_SUCCESS);
							}
						} else {
							mHandler.sendEmptyMessage(MSG_GET_TALKURL_INUSED);
						}
					}else {
						mHandler.sendEmptyMessage(MSG_GET_TALKURL_FAIL);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private void noticeTalkEnd() {
		if(DEBUG)Log.i(TAG, "noticeTalkEnd");
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					StringBuilder getLiveUrl = new StringBuilder(URL_NOTICE_TALK_END + "?");
					getLiveUrl.append(PARAMETER_GLASS_UID+"="+mGlassUid);
					getLiveUrl.append("&"+PARAMETER_CLIENT_UID + "=" + mClientUid);
					getLiveUrl.append("&" + PARAMETER_CMP_UID + "=" +SyncApp.COMPANY_UID);
					HttpGet httpGet = new HttpGet(getLiveUrl.toString());
					HttpClient httpClient = new DefaultHttpClient();
					HttpResponse httpResponse = httpClient.execute(httpGet);
					if(DEBUG)Log.i(TAG, "statusCode=" + httpResponse.getStatusLine().getStatusCode());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private void noticeWatchEnd() {
		if(DEBUG)Log.i(TAG, "noticeWatchEnd");
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					StringBuilder getLiveUrl = new StringBuilder(URL_NOTICE_WATCH_END + "?");
					getLiveUrl.append(PARAMETER_GLASS_UID+"="+mGlassUid);
					getLiveUrl.append("&"+PARAMETER_CLIENT_UID + "=" + mClientUid);
					getLiveUrl.append("&" + PARAMETER_CMP_UID + "=" +SyncApp.COMPANY_UID);
					HttpGet httpGet = new HttpGet(getLiveUrl.toString());
					HttpClient httpClient = new DefaultHttpClient();
					HttpResponse httpResponse = httpClient.execute(httpGet);
					if(DEBUG)Log.i(TAG, "statusCode=" + httpResponse.getStatusLine().getStatusCode());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	@Override
	public void onPrepared(IMediaPlayer arg0) {
		if(DEBUG)Log.i(TAG,"onPrepared");
		mPlayState = STATE_PREPARED;
	}

	@Override
	public boolean onError(IMediaPlayer arg0, int arg1, int arg2) {
		Log.i(TAG, "onerror arg0="+arg0+",arg1="+arg1+",arg2="+arg2+",mRestartNum="+mRestartNum);
		if(mPlayState == STATE_PLAYING)
			mProgressBar.setVisibility(View.VISIBLE);
		mPlayState = STATE_ERROR;
		if(mRestartNum < 5){
			mHandler.sendEmptyMessageDelayed(MSG_RECONNECT,500);
		}else{
			hideProgress();
			mProgressBar.setVisibility(View.GONE);
			if(WifiUtils.isNetAvailable(this)){
				showDialog(getString(R.string.rtmp_live_may_closed));
			}else {
				showDialog(getString(R.string.remote_live_no_network));
			}
		}
		return true;
	}

	@Override
	public boolean onInfo(IMediaPlayer arg0, int arg1, int arg2) {
		switch (arg1) {
			case IMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
				if(DEBUG)Log.i(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING:");
				break;
			case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
				mPlayState = STATE_PLAYING;
				mRestartNum = 0;
				mProgressBar.setVisibility(View.GONE);
				mHandler.sendEmptyMessage(MSG_GET_DATA_SUCCESS);
				break;
			case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
				mProgressBar.setVisibility(View.VISIBLE);
				if(DEBUG)Log.i(TAG, "MEDIA_INFO_BUFFERING_START:");
				break;
			case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
				if(DEBUG)Log.i(TAG, "MEDIA_INFO_BUFFERING_END:");
				mProgressBar.setVisibility(View.GONE);
				break;
			case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
				if(DEBUG)Log.i(TAG, "MEDIA_INFO_NETWORK_BANDWIDTH: " + arg2);
				break;
			case IMediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
				if(DEBUG)Log.i(TAG, "MEDIA_INFO_BAD_INTERLEAVING:");
				break;
			case IMediaPlayer.MEDIA_INFO_METADATA_UPDATE:
				if(DEBUG)Log.i(TAG, "MEDIA_INFO_METADATA_UPDATE:");
				break;
			case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
				if(DEBUG)Log.i(TAG, "MEDIA_INFO_AUDIO_RENDERING_START:");
				break;
		}
		return false;
	}

	@Override
	public void onCompletion(IMediaPlayer arg0) {
		if(DEBUG)if(DEBUG)Log.i(TAG,"onCompletion");
		hideProgress();
		mProgressBar.setVisibility(View.GONE);
		showDialog(getString(R.string.rtmp_live_closed));
	}

	private int startTalk(String url){
		if(DEBUG)Log.i(TAG, "startTalk mTalkState="+mTalkState);
		if(mTalkState != TALK_REQUEST)
			return -1;
		mTalkState = TALK_ING;
		if (mLiveDataCodec == null) {
			mLiveDataCodec = new LiveDataCodec();
			mLiveDataCodec.start();
		}
		mRTMPLivePush.start(url);
		synchronized (mLock) {
			mThreadExit = false;
		}
		mRTMPTalkThread = new RTMPTalkThread();
		mRTMPTalkThread.start();
		return 0;
	}

	private void stopTalk(){
		if(DEBUG)Log.i(TAG, "stopTalk");
		synchronized (mLock) {
			mThreadExit = true;
		}
		mRTMPLivePush.stop();
		if (null != mVoiceDialog && mVoiceDialog.isShowing()) {
			mVoiceDialog.dismiss();
		}
		noticeTalkEnd();
	}

	private class RTMPTalkThread extends Thread {

		@Override
		public void run() {
			super.run();
			mAudioQue = new BufferQueue(AAC_BUFFER_SUM, AAC_BUFFER_SIZE);
			mAudioGetThread = new Thread(new AudioGet(), "AudioEnc");
			mAudioPutThread = new Thread(new AudioPut(),"AudioSend");
			mAudioGetThread.start();
			mAudioPutThread.start();
			try {
				mAudioGetThread.join();
				mAudioPutThread.join();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if(DEBUG)Log.e(TAG, "Speaker IOTCMainThread exit successful....");
		}
	}

	class AudioPCM {
		byte[] mData;
		int mFrameSize;
		int mCapacity;

		public AudioPCM(int capacity) {
			mCapacity = capacity;
			mData = new byte[mCapacity];
		}
	};

	private class AudioGet implements Runnable {

		private static final boolean PCM_DUMP_DEBUG = false;
		private static final boolean AAC_DUMP_DEBUG = false;
		private int mAudioSource = MediaRecorder.AudioSource.MIC;
		private int mSampleRate = 8000;
		private int mChannel = AudioFormat.CHANNEL_IN_MONO;
		private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
		private int mBufferSizeInBytes = 2048;
		private AudioRecord mAudioRecord = null;
		private AudioPCM mAudioPCMData = null;

		public AudioGet() {
			mAudioPCMData = new AudioPCM(mBufferSizeInBytes);
			mAudioRecord = new AudioRecord(mAudioSource, mSampleRate, mChannel,
					mAudioFormat, mBufferSizeInBytes);
		}

		@SuppressWarnings("unused")
		@Override
		public void run() {
			mAudioRecord.startRecording();
			FileOutputStream outPCM = null;
			FileOutputStream outAAC = null;
			try {
				if (PCM_DUMP_DEBUG) {
					String File = "/sdcard/PCM.pcm";
					outPCM = new FileOutputStream(File);
				}
				if (AAC_DUMP_DEBUG) {
					String File = "/sdcard/AAC.aac";
					outAAC = new FileOutputStream(File);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			for (;;) {
				synchronized (mLock) {
					if (mThreadExit) {
						break;
					}
				}
				int readSize = mAudioRecord.read(mAudioPCMData.mData, 0,
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
					//if(DEBUG)Log.i(TAG, "audio pcm size="+readSize);
					BufferQueue.MediaBuffer media = mAudioQue.getFreeBuffer(30);
					if (media != null) {
						mLiveDataCodec.setAudioPCM(mAudioPCMData, media,
								AAC_BUFFER_SIZE);
						//if(DEBUG)Log.i(TAG, "mdeia frameSize="+media.mFrameSize);
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
			try {
				if (PCM_DUMP_DEBUG && (outPCM != null)) {
					outPCM.close();
				}
				if (AAC_DUMP_DEBUG && (outAAC != null)) {
					outAAC.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			mAudioRecord.stop();
			mAudioRecord.release();
			mAudioRecord = null;
			if(DEBUG)Log.e(TAG, "Speaker AudioGet Thread Exit Successful");
		}
	}

	class AudioPut implements Runnable {
		public AudioPut() {
		}
		@Override
		public void run() {
			int count = 0;
			for (;;) {
				synchronized (mLock) {
					if (mThreadExit) {
						break;
					}
				}
				BufferQueue.MediaBuffer media = mAudioQue.getBusyBuffer(30);
				if (media != null) {
					//aac inc++ *(
					int pts = count++ *(1024*1000*1000/8000);
					mRTMPLivePush.setAACData(media.mData, media.mFrameSize, pts);
					mAudioQue.putBusyBuffer(media);
				}
			}
			Log.e(TAG, "Speaker AudioPut Thread Exit Successful");
		}
	}

}
