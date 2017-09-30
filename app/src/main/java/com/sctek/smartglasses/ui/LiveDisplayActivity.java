package com.sctek.smartglasses.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.AudioTrack;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;

import com.cn.zhongdun110.camlog.R;
import com.cn.zhongdun110.camlog.camera.LiveModule;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.sctek.smartglasses.utils.CamlogCmdChannel;
import com.sctek.smartglasses.utils.WifiUtils;
import com.sctek.smartglasses.utils.WifiUtils.WifiCipherType;

import java.nio.ByteBuffer;

import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.devicemanager.GlassDetect;
import cn.ingenic.glasssync.screen.live.RtspClient;

@SuppressLint("Wakelock")
public class LiveDisplayActivity extends BaseFragmentActivity implements
		RtspClient.OnRtspClientListener, SurfaceHolder.Callback,
		DialogInterface.OnKeyListener {
	private final String TAG = "LiveDisplayActivity";
	private final boolean DEBUG = false;
	private final int mDirectAudioBufferSize = 320;
	private LiveModule mLiveModule;
	private RtspClient mRtspClient;
	private SurfaceView mSurfaceView = null;
	private ProgressDialog mPD = null;
	private PowerManager.WakeLock mWakeLock;
	private boolean mGlassCameraClose = true;
	/* Audio Player */
	private boolean mReveivedAudioData = false;
	private final Object mLock = new Object();
	private byte[] mAudioData = new byte[mDirectAudioBufferSize];
	private ByteBuffer mDirectAudioBuffer = null;
	private boolean mAudioPlayThreadExit = false;
        private boolean mCloseMediaAudio = false;
	private AudioTrack mTrack;
        private WifiManager mWifiManager;
        private CamlogCmdChannel mCamlogCmdChannel;
        private AudioManager mAudioManager;
        private Context mContext;
        private boolean mRegistApStateBroadcastReceiver = false;
        private final static int MSG_RESEDN_CONNECT_WIFI = 1;
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (DEBUG)
				Log.i(TAG, "[ mHandler ] handle Message " + msg.what);
			switch (msg.what) {
			case LiveModule.MSG_LIVE_WIFI_UNCONNECTED:
				showDialog(getString(R.string.live_wifi_disconnect));
				break;
			case LiveModule.MSG_LIVE_MSG_LIVE_STATUS:
				showDialog((String) msg.obj);
				mGlassCameraClose = true;
				break;
			case LiveModule.MSG_LIVE_MSG_WIFI_CONNECTED:
				String url = (String) msg.obj;
				if (url != null) {
				    mRtspClient.start(url);
				    if (mDirectAudioBuffer != null)
					mRtspClient.setAudioDataBuf(mDirectAudioBuffer);
				}
				break;
			case CamlogCmdChannel.RECEIVE_MSG_FROM_GLASS:
			    Packet data = (Packet)msg.obj;
			    String glassIp = data.getString("ip");
			    if(null != glassIp && glassIp.length() != 0){
				mOwnHandler.removeMessages(MSG_RESEDN_CONNECT_WIFI);
				mCamlogCmdChannel.unregisterHandler("LiveDisplayActivity");
				showProgress(getString(R.string.live_loading));
				mLiveModule.sendLiveMessage(LiveModule.LIVE_MSG_CAMERA_LIVE);
				mLiveModule.registerHandler(mHandler);
				mGlassCameraClose = false;
			    }
			    break;
			}
		}
	};
        private Handler mOwnHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (DEBUG)
				Log.i(TAG, "[ mOwnHandler ] handle Message " + msg.what);
			switch (msg.what) {
			case MSG_RESEDN_CONNECT_WIFI:
			    sendApInfo();
				break;
			}
		}
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display);
		mContext = this;
		mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
		mDirectAudioBuffer = ByteBuffer.allocateDirect(mDirectAudioBufferSize);
		mSurfaceView.getHolder().addCallback(this);
		mRtspClient = new RtspClient(this);
		mRtspClient.setListener(this);
		mLiveModule = LiveModule.getInstance(this);
		mPD = new ProgressDialog(this);
		mPD.setCancelable(false);
		mPD.setOnKeyListener(this);
		AudioPlayThread audioPlayThread = new AudioPlayThread();
		audioPlayThread.start();
		PowerManager pm = (PowerManager) this
				.getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP
				| PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
		mWakeLock.acquire();
		mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		mCamlogCmdChannel = CamlogCmdChannel.getInstance(this);
		mCamlogCmdChannel.registerHandler("LiveDisplayActivity",mHandler);
                mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		if (mAudioManager.isMusicActive()){
		    mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
						    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
		}
		  //关闭媒体音频
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter.isEnabled()
		    && (adapter.getProfileConnectionState(BluetoothProfile.A2DP) 
			== BluetoothProfile.STATE_CONNECTED)){
		    GlassDetect.getInstance(this).set_a2dp_disconnect();
		    mCloseMediaAudio = true;
//		    Toast.makeText(this, R.string.turn_off_bluetooth_a2dp,
//							Toast.LENGTH_LONG).show();
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

	private void showProgress(String msg) {
		if (mPD.isShowing()) {
			mPD.hide();
		}
		mPD.setMessage(msg);
		mPD.show();
	}


        private void hideProgress(){
	    mPD.dismiss();
	}

	private boolean checkBTEnabled() {
		DefaultSyncManager manager = DefaultSyncManager.getDefault();
		if ((manager == null) || (!DefaultSyncManager.isConnect())) {
			Log.e(TAG, "Bluetooth unconnect : mManager " + manager);
			return false;
		}

		return true;
	}

	private void checkWifiState() {
		if (DEBUG)
			Log.i(TAG, "checkWifiState :: WifiUtils.getWifiAPState = "
					+ WifiUtils.getWifiAPState(mWifiManager));
		if (WifiUtils.getWifiAPState(mWifiManager) != WifiUtils.WIFI_AP_STATE_ENABLED) {
		    AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    builder.setOnKeyListener(this);
		    builder.setTitle(R.string.turn_wifi_ap_on);
		    builder.setMessage(R.string.live_wifi_ap_hint);
		    builder.setNegativeButton(R.string.cancel,
					      new DialogInterface.OnClickListener() {

						  @Override
						      public void onClick(DialogInterface dialog, int which) {
						      dialog.cancel();
						      finish();
						  }
					      });

		    builder.setPositiveButton(R.string.ok,
					      new DialogInterface.OnClickListener() {

						  @Override
						      public void onClick(DialogInterface dialog, int which) {
						      IntentFilter filter = new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
						      mContext.registerReceiver(mApStateBroadcastReceiver,filter);
						      mRegistApStateBroadcastReceiver = true;
						      WifiUtils.turnWifiApOn(LiveDisplayActivity.this
									     ,mWifiManager
									     ,WifiCipherType.WIFICIPHER_NOPASS); 
						      dialog.cancel();
						      showProgress(getString(R.string.waiting_for_glass_connect));
						  }
					      });

		    builder.create().show();
		} else {
		    sendApInfo();
		    showProgress(getString(R.string.waiting_for_glass_connect));
		}
	}

        private void sendApInfo(){
	    if (WifiUtils.getWifiAPState(mWifiManager) == WifiUtils.WIFI_AP_STATE_ENABLED) {
		Packet packet = mCamlogCmdChannel.createPacket();
		packet.putInt("type", CamlogCmdChannel.CONNET_WIFI_MSG);
			
		String ssid = WifiUtils.getValidSsid(this);
		String pw = WifiUtils.getValidPassword(this);
		String security = WifiUtils.getValidSecurity(this);
	    
		packet.putString("ssid", ssid);
		packet.putString("pw", pw);
		packet.putString("security", security);
		mCamlogCmdChannel.sendPacket(packet);
		mOwnHandler.sendEmptyMessageDelayed(MSG_RESEDN_CONNECT_WIFI, 5000);
	    }
	}
	@Override
	public void onStart() {
		super.onStart();
		if (checkBTEnabled()) {
			checkWifiState();
		} else
			showDialog(getString(R.string.bluetooth_error));
	}

	@Override
	public void onPause() {
		if (DEBUG)
			Log.i(TAG, "onPause");
		super.onPause();
		mLiveModule.unRegisterHandler();
	}

	@Override
	public void onStop() {
		if (DEBUG)
			Log.i(TAG, "onStop");
		if (!mGlassCameraClose)
			mLiveModule.sendLiveMessage(LiveModule.LIVE_MSG_CAMERA_LIVE);
		mLiveModule.unRegisterHandler();
		mRtspClient.setListener(null);
		mRtspClient.close();
		hideProgress();
		if (mTrack != null) {
			if (DEBUG)
				Log.i(TAG, "release Audio Track");
			mAudioPlayThreadExit = true;
			mTrack.release();
		}
		super.onStop();
		finish();
		mAudioPlayThreadExit = true;
	}

	@Override
	protected void onDestroy() {
		if (DEBUG)
			Log.i(TAG, "onDestroy");
		super.onDestroy();
		mOwnHandler.removeMessages(MSG_RESEDN_CONNECT_WIFI);
		if(mRegistApStateBroadcastReceiver)
		    mContext.unregisterReceiver(mApStateBroadcastReceiver);
		mPD = null;
		mWakeLock.release();
		mAudioManager.abandonAudioFocus(mAudioFocusListener);
		mCamlogCmdChannel.unregisterHandler("LiveDisplayActivity");
		if(mCloseMediaAudio){
		    GlassDetect.getInstance(this).set_a2dp_connect();
		}
	}

	// RTSPClient Listener function
	@Override
	public void onVideoSizeChanged(int width, int height) {
		if (DEBUG)
			Log.i(TAG, "[ onVideoSizeChanged ] width = " + width + " height = "
					+ height);
		LayoutParams lp = mSurfaceView.getLayoutParams();
		lp.width = width;
		lp.height = height;
		mSurfaceView.setLayoutParams(lp);
		mSurfaceView.requestLayout();
	}

	@Override
	public void onStreamDown() {
	    showDialog(getString(R.string.live_stream_down));
	}

	@Override
	public void onStreamDisconnect() {
		showDialog(getString(R.string.live_network_disconnect));
	}

	@Override
	public void onAudioStream() {
		synchronized (mLock) {
			mReveivedAudioData = true;
			mLock.notify();
		}
	}

	@Override
	public void onFrameState(int state) {
		switch (state) {
		case 0:
		    if (mGlassCameraClose) return;
		    showProgress(getString(R.string.live_wait_network_data));
		    break;
		case 1:
		    hideProgress();
		    break;
		case 2:
		    onStreamDisconnect();
		    break;
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mRtspClient.setSurface(holder);
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

	class AudioPlayThread extends Thread {
		public AudioPlayThread() {
			super();
			mTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
					AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT, 320, AudioTrack.MODE_STREAM);
		}

		@Override
		public void run() {
			super.run();
			while (!mAudioPlayThreadExit) {
				synchronized (mLock) {
					if (DEBUG)
						Log.i(TAG, "mReveivedAudioData = " + mReveivedAudioData);
					if (mReveivedAudioData) {
						if (DEBUG)
							Log.i(TAG, "Play Audio ....");
						try {
							if (mDirectAudioBuffer.hasArray()) {
								mAudioData = mDirectAudioBuffer.array();

								mTrack.write(mAudioData, 0, 320);
								mTrack.play();
								mTrack.stop();
							}
						} catch (Exception e) {
							Log.e(TAG, "Failed to write image", e);
						}

						mReveivedAudioData = false;
					} else {
						try {
							mLock.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

    private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
	    public void onAudioFocusChange(int focusChange) {
	    }
	};

    private BroadcastReceiver mApStateBroadcastReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(intent.getAction())) {
				int cstate = intent.getIntExtra("wifi_state", -1);
				Log.e(TAG, "WIFI_AP_STATE_CHANGED_ACTION:" + cstate);
				if(cstate == WifiUtils.WIFI_AP_STATE_ENABLED) {
					BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
					if(!adapter.isEnabled()) {
						adapter.enable();
					}
					sendApInfo();
					mRegistApStateBroadcastReceiver = false;
					mContext.unregisterReceiver(mApStateBroadcastReceiver);
				}
			}
		}
		
	};
}
