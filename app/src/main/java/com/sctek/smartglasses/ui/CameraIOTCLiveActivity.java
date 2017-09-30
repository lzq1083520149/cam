package com.sctek.smartglasses.ui;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.MediaStore.Video;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cn.zhongdun110.camlog.R;
import com.cn.zhongdun110.camlog.iotc.IOTCClient;
import com.cn.zhongdun110.camlog.iotc.IOTCServer;
import com.sctek.smartglasses.utils.WifiUtils;

@SuppressLint("Wakelock")
public class CameraIOTCLiveActivity extends Activity implements
		SurfaceHolder.Callback, DialogInterface.OnKeyListener {

	private static final boolean DEBUG = true;
	private static final String TAG = "CameraLiveIOTCActivity";
	private String mUid;
	private String mPwd;
	private SurfaceView mSurfaceView = null;
	private ProgressDialog mPD = null;
	private PowerManager.WakeLock mWakeLock;

	private IOTCClient mIOTCClient = new IOTCClient();
	private ImageView mRecord;
	private ImageView mStartLiveRecord, mVideoRecording;
	private TextView mRecordTime;
	private Dialog dialog;
	private final int MIX_TIME = 1;
	private final int RECORD_NO = 0;
	private final int RECORD_ING = 1;
	private final int RECODE_ED = 2;
	private int RECODE_STATE = RECORD_NO;
	private int mVideoRecordState = RECORD_NO;
	private long mStartRecordTime = 0;

	private final int MIN_VIDEO_RECORD_TIME = 3;
	private int mCameraSet = IOTCClient.CAMERA_SET_LOW;
	private ContentValues mCurrentVideoValues;
	private String mVideoFileFullPath = null;
	private String mTmpVideoFileFullPath = null;
	private ContentResolver mContentResolver;
	private Uri mCurrentVideoUri;
	private long mVideoRecordStartTime;
	private final int MSG_UPDATE_TIME = 1;
	private final int VIEW_TRANSPARENT = 130;
	/*
	 * 我是这么想的： 应用启动显示“正在连接”的弹窗 ---> 如果收到IOTC_OK_Initial，表示连接成功，启动“开始接收数据”的弹窗
	 * ---> 如果收到IOTC_OK_GetData，表示获取数据成功，将弹窗去掉，开始直播数据 --->
	 * 如果收到IOTC_Err_GetVideo或IOTC_Err_GetAudio表示获取数据失败（在直播启动时） --->
	 * 如果收到IOTC_Err_GetData_TimeOut，表示获取数据超时（可能在直播启动或直播过程中）
	 * 如果收到IOTC_Err_Initial，表示连接失败，退出直播
	 */

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			// if (DEBUG)
			// Log.i(TAG, "[ mHandler ] handle Message " + msg.what);
			switch (msg.what) {
			case IOTCClient.IOTC_OK_INIT:
				showProgress(getString(R.string.remote_live_msg_get_data));
				break;
			case IOTCClient.IOTC_OK_GETDATA:
				mRecord.setVisibility(View.VISIBLE);
				mStartLiveRecord.setVisibility(View.VISIBLE);
				hideProgress();
				if (WifiUtils
						.isMoblieNetworkConnected(CameraIOTCLiveActivity.this)) {
					Toast.makeText(CameraIOTCLiveActivity.this,
							R.string.remote_live_use_mobile_data,
							Toast.LENGTH_LONG).show();
				}
				break;
			case IOTCClient.IOTC_ERR_UID_UNLICENSE:
				showDialog(getString(R.string.remote_live_error_check_uid));
				break;
			case IOTCClient.IOTC_ERR_WORING_PWD:
				showDialog(getString(R.string.remote_live_error_check_pwd));
				break;
			case IOTCClient.IOTC_ERR_EXCEED_MAX_SEESION:
				showDialog(getString(R.string.remote_live_error_max_seesion));
				break;
			case IOTCClient.IOTC_ERR_DEVICE_NOT_LISTENING:
				showDialog("device not listening");
				break;
			case IOTCClient.IOTC_ERR_DEVICE_DEVICE_OFFLINE:
				showDialog(getString(R.string.remote_live_error_device_offline));
				break;
			case IOTCClient.IOTC_ERR_INIT:
				showDialog(getString(R.string.remote_live_error_check_uid));
				break;
			case IOTCClient.IOTC_ERR_GETDATA_TIMEOUT:
				showDialog(getString(R.string.remote_live_error_get_data_timeout));
				break;
			case IOTCClient.IOTC_ERR_GETVIDEO:
				showDialog(getString(R.string.remote_live_error_get_videodata_failed));
				break;
			case IOTCClient.IOTC_ERR_GETAUDIO:
				Log.e(TAG, "IOTC_ERR_GETAUDIO!!!!");
				// mRecord.setVisibility(View.GONE);
				break;
			case IOTCClient.IOTC_ERR_CLOSE_BY_REMOTE:
				Log.i(TAG, "IOTC_ERR_CLOSE_BY_REMOTE!!!");
				showDialog(getString(R.string.remote_live_error_close_by_remote));
				break;

			/* Speaker */
			case IOTCServer.SPEAKER_ERR_SEND_FAILED:
				showWarnToast(getString(R.string.remote_live_error_speaker_failed));
				break;
			default:
				Log.i(TAG, "Unknow message [ " + msg.what + " ]");
				break;
			}
		}
	};
	private Handler mUpdateHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (DEBUG)
				Log.i(TAG, "[ mUpdateHandler ] handle Message " + msg.what);
			switch (msg.what) {
			case MSG_UPDATE_TIME:
				updateTimerView(false);
				break;
			default:
				Log.i(TAG, "Unknow message [ " + msg.what + " ]");
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (DEBUG)
			Log.i(TAG, "--onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera_iotclive);
		mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
		mRecord = (ImageView) findViewById(R.id.record);
		mRecord.setOnTouchListener(mRecordOnTouchListener);
		mRecord.setAlpha(VIEW_TRANSPARENT);
		mStartLiveRecord = (ImageView) findViewById(R.id.video_button);
		mStartLiveRecord.setOnTouchListener(mVideoRecordTouchListener);
		mStartLiveRecord.setAlpha(VIEW_TRANSPARENT);
		mVideoRecording = (ImageView) findViewById(R.id.recording);
		mRecordTime = (TextView) findViewById(R.id.record_time);
		mRecordTime.setAlpha(VIEW_TRANSPARENT);
		mUid = getIntent().getStringExtra("uid");
		mPwd = getIntent().getStringExtra("pwd");
		mSurfaceView.getHolder().addCallback(this);
		mPD = new ProgressDialog(this);
		mPD.setCancelable(false);
		mPD.setOnKeyListener(this);
		PowerManager pm = (PowerManager) this
				.getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP
				| PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
		mWakeLock.acquire();
		mIOTCClient.registerHandler(mHandler);
		mContentResolver = getContentResolver();
	}

	private void showProgress(String msg) {
		if (mPD.isShowing()) {
			mPD.hide();
		}
		mPD.setMessage(msg);
		mPD.show();
	}

	private void hideProgress() {
		mPD.dismiss();
	}

	@Override
	public void onStart() {
		super.onStart();
		mIOTCClient.start(mUid, mPwd,
				getIntent().getIntExtra("clearity", IOTCClient.CAMERA_SET_LOW));
		mCameraSet = getIntent().getIntExtra("clearity",
				IOTCClient.CAMERA_SET_LOW);
		showProgress(getString(R.string.remote_live_msg_check_uid));
	}

	@Override
	public void onPause() {
		if (DEBUG)
			Log.i(TAG, "onPause");
		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
		if (DEBUG)
			Log.i(TAG, "onStop");
		if (mVideoRecordState == RECORD_ING) {
			stopVideoRecord();
		}
		mIOTCClient.stop();
		mPD.dismiss();
		finish();
	}

	@Override
	protected void onDestroy() {
		if (DEBUG)
			Log.i(TAG, "onDestroy");
		super.onDestroy();
		mPD = null;
		mWakeLock.release();
		mIOTCClient.unregisterHandler();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mIOTCClient.registerSurface(holder);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

	}

	@Override
	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			dialog.cancel();
			finish();
		}
		return false;
	}

	private OnTouchListener mRecordOnTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			Log.i(TAG, "onTouch = " + event.getAction());
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (RECODE_STATE != RECORD_ING) {
					RECODE_STATE = RECORD_ING;
					showVoiceDialog();
					mStartRecordTime = System.currentTimeMillis();
					mIOTCClient.startSpeaker();
				}

				break;
			case MotionEvent.ACTION_UP:
				if (RECODE_STATE == RECORD_ING) {
					RECODE_STATE = RECODE_ED;
					mIOTCClient.stopSpeaker();
					if (dialog.isShowing()) {
						dialog.dismiss();
					}
					Log.i(TAG, "startRecordTime = " + mStartRecordTime
							+ "----recordTime = "
							+ (System.currentTimeMillis() - mStartRecordTime)
							/ 1000);
					if (((System.currentTimeMillis() - mStartRecordTime) / 1000) < MIX_TIME) {
						showWarnToast(getString(R.string.remote_live_error_record_too_short));
						RECODE_STATE = RECORD_NO;
					}
				}

				break;
			}
			return true;
		}
	};

	private OnTouchListener mVideoRecordTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (mVideoRecordState != RECORD_ING) {
					Log.i(TAG, "--------11");
					generateVideoFilename();
					if ((mTmpVideoFileFullPath != null)
							&& (mVideoFileFullPath != null)) {
						Log.i(TAG, "mTmpVideoFileName = "
								+ mTmpVideoFileFullPath);
						Log.i(TAG, "mVideoFileName = " + mVideoFileFullPath);
						mVideoRecordStartTime = System.currentTimeMillis();
						int ret = mIOTCClient
								.setOutpuFileAndStartMP4Record(mTmpVideoFileFullPath);
						if (ret < 0) {
							deleteDatabase(mTmpVideoFileFullPath);
							Toast.makeText(CameraIOTCLiveActivity.this,
									R.string.remote_live_video_record_fail,
									Toast.LENGTH_SHORT).show();
						} else {
							mVideoRecordState = RECORD_ING;
							updateTimerView(true);
							mVideoRecording.setVisibility(View.VISIBLE);
							mRecordTime.setVisibility(View.VISIBLE);
						}
					}
				} else if (mVideoRecordState == RECORD_ING) {
					Log.i(TAG, "--------22");
					stopVideoRecord();
				}
				break;

			default:
				break;
			}
			return false;
		}
	};

	private String createName(long dateTaken) {
		Date date = new Date(dateTaken);
		String strFormat = null;
		strFormat = getString(R.string.video_file_name_format);
		SimpleDateFormat dateFormat = new SimpleDateFormat(strFormat, Locale.US);
		return dateFormat.format(date);
	}

	private void generateVideoFilename() {
		String recordDirectory = "/sdcard/IGlass/Videos";
		int videoFrameWidth = -1;
		int videoFrameHeight = -1;
		if (mCameraSet == IOTCClient.CAMERA_SET_LOW) {
			videoFrameWidth = 480;
			videoFrameHeight = 352;
		} else if (mCameraSet == IOTCClient.CAMERA_SET_MEDIUM) {
			videoFrameWidth = 768;
			videoFrameHeight = 432;
		} else if (mCameraSet == IOTCClient.CAMERA_SET_HIGH) {
			videoFrameWidth = 1280;
			videoFrameHeight = 720;
		} else {
			Log.e(TAG, "can't find right live resolution");
			mVideoFileFullPath = null;
			mTmpVideoFileFullPath = null;
			return;
		}
		long dateTaken = System.currentTimeMillis();
		String title = createName(dateTaken);
		String filename = title + ".mp4";
		String mime = ".mp4";
		File dir = new File(recordDirectory);
		if (!dir.exists())
			dir.mkdirs();

		mVideoFileFullPath = recordDirectory + '/' + filename;
		mTmpVideoFileFullPath = recordDirectory + '/' + title + ".tmp";
		mCurrentVideoValues = new ContentValues(7);
		mCurrentVideoValues.put(Video.Media.TITLE, title);
		mCurrentVideoValues.put(Video.Media.DISPLAY_NAME, filename);
		mCurrentVideoValues.put(Video.Media.DATE_TAKEN, dateTaken);
		mCurrentVideoValues.put(Video.Media.MIME_TYPE, mime);
		mCurrentVideoValues.put(Video.Media.DATA, mVideoFileFullPath);
		mCurrentVideoValues.put(
				Video.Media.RESOLUTION,
				Integer.toString(videoFrameWidth) + "x"
						+ Integer.toString(videoFrameHeight));
		if (DEBUG)
			Log.i(TAG, "New video filename: " + mVideoFileFullPath);
	}

	private void updateVideoFileName(String tmpFileName) {
		if (DEBUG)
			Log.i(TAG, "tmp fileName:" + tmpFileName);
		File f = new File(tmpFileName);
		if (f.exists()) {
			String fileName = tmpFileName.replace(".tmp", ".mp4");
			File newFile = new File(fileName);
			if (!f.renameTo(newFile)) {
				Log.e(TAG, "renameTo " + fileName + "failed!");
			}
		}
	}

	private void addVideoToMediaStore() {
		Uri videoTable = Uri.parse("content://media/external/video/media");
		mCurrentVideoValues.put(Video.Media.SIZE,
				new File(mVideoFileFullPath).length());
		long duration = System.currentTimeMillis() - mVideoRecordStartTime;
		if (duration > 0) {
			mCurrentVideoValues.put(Video.Media.DURATION, duration);
		} else {
			Log.w(TAG, "Video duration <= 0 : " + duration);
		}
		try {
			mCurrentVideoUri = mContentResolver.insert(videoTable,
					mCurrentVideoValues);
			sendBroadcast(new Intent(android.hardware.Camera.ACTION_NEW_VIDEO,
					mCurrentVideoUri));
		} catch (Exception e) {
			// We failed to insert into the database. This can happen if
			// the SD card is unmounted.
			Log.e(TAG, "insert db failed! ");
			mCurrentVideoUri = null;
		} finally {
			if (DEBUG)
				Log.i(TAG, "Current video URI: " + mCurrentVideoUri);
		}
		mCurrentVideoValues = null;
	}

	private void deleteVideoFile(String fileName) {
		if (DEBUG)
			Log.i(TAG, "Deleting video " + fileName);
		File f = new File(fileName);
		if (f.exists() && !f.delete()) {
			Log.w(TAG, "Could not delete " + fileName);
		}
	}

	private void showDialog(String title) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setOnKeyListener(this);
		builder.setTitle(title);
		builder.setNegativeButton(R.string.live_cancle,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						finish();
					}
				});
		builder.create().show();
	}

	private void showVoiceDialog() {
		dialog = new Dialog(this, R.style.DialogStyle);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		dialog.setContentView(R.layout.my_dialog);
		ImageView dialog_img = (ImageView) dialog.findViewById(R.id.dialog_img);
		AnimationDrawable animationDrawable = (AnimationDrawable) dialog_img
				.getDrawable();
		animationDrawable.start();
		dialog.show();
	}

	private void showWarnToast(String errorMsg) {
		Toast toast = new Toast(this);
		LinearLayout linearLayout = new LinearLayout(this);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		linearLayout.setPadding(20, 20, 20, 20);
		ImageView imageView = new ImageView(this);
		imageView.setImageResource(R.drawable.voice_to_short);
		imageView.setLayoutParams(new LayoutParams(128, 128));
		TextView mTv = new TextView(this);
		mTv.setText(errorMsg);
		mTv.setTextSize(14);
		mTv.setTextColor(Color.WHITE);
		mTv.setGravity(Gravity.CENTER);
		linearLayout.addView(imageView);
		linearLayout.addView(mTv);
		linearLayout.setGravity(Gravity.CENTER);
		linearLayout.setBackgroundResource(R.drawable.record_bg);
		toast.setView(linearLayout);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}

	private void stopVideoRecord() {
		mVideoRecordState = RECODE_ED;
		int ret = mIOTCClient.stopMP4Record();
		mVideoRecording.setVisibility(View.GONE);
		mRecordTime.setVisibility(View.GONE);
		mUpdateHandler.removeMessages(MSG_UPDATE_TIME);
		if (ret < 0) {
			Log.e(TAG, "Live Video Record Failed.");
			deleteDatabase(mTmpVideoFileFullPath);
			Toast.makeText(CameraIOTCLiveActivity.this,
					R.string.remote_live_video_save_fail, Toast.LENGTH_SHORT)
					.show();
		} else {
			if (((System.currentTimeMillis() - mVideoRecordStartTime) / 1000) < MIN_VIDEO_RECORD_TIME) {
				showWarnToast(getString(R.string.remote_live_error_videorecord_too_short));
				RECODE_STATE = RECORD_NO;
				deleteDatabase(mTmpVideoFileFullPath);
			} else {
				updateVideoFileName(mTmpVideoFileFullPath);
				addVideoToMediaStore();
			}
		}
	}

	private void updateTimerView(boolean begin) {
		Resources res = getResources();

		String timeStr = null;
		if (begin) {
			timeStr = String.format(
					getResources().getString(R.string.timer_format_minute), 0,
					0);
		} else {
			long time = (System.currentTimeMillis() - mVideoRecordStartTime) / 1000;
			if (time < 3600) {
				timeStr = String.format(
						getResources().getString(R.string.timer_format_minute),
						(time - time / 3600 * 3600) / 60, time % 60);
			} else {
				timeStr = String.format(
						getResources().getString(R.string.timer_format_hour),
						time / 3600, (time - time / 3600 * 3600) / 60,
						time % 60);
			}
		}
		mRecordTime.setText(timeStr);
		if (mVideoRecordState == RECORD_ING)
			mUpdateHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, 1000);
	}
}
