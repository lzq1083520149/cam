package com.cn.zhongdun110.camlog.camera;

import android.content.Context;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncException;
import cn.ingenic.glasssync.services.SyncModule;


public class LiveModule extends SyncModule {
    private static final String TAG = "LiveModule";
    private static final String MODULE_NAME = "live_module";
    private Boolean DEBUG = true;

    // key-value pairs
    private final String LIVE_MESSAGE = "live_message";
    private final String LIVE_STATUS = "live_status";
    private final String LIVE_RTSP_URL = "live_rtsp_url";
    private final String STATUS_LIVE_NOT_FINISH = "camera_live_not_finish";

    // glass message receiver
    public static final int LIVE_MSG_CAMERA_LIVE = 0;
    public static final int MSG_LIVE_WIFI_UNCONNECTED = 1;;
    public static final int MSG_LIVE_MSG_LIVE_STATUS = 2;
    public static final int MSG_LIVE_MSG_WIFI_CONNECTED = 3;
    private final int MSG_LIVE_NOT_FINISH = 4;
    // mobile message receiver
    private final int LIVE_MSG_WIFI_CONNECTED = 1000;
    private final int LIVE_MSG_WIFI_UNCONNECTED = 1001;
    private final int LIVE_MSG_LIVE_STATUS = 1002;

	private static LiveModule sInstance;
	private Handler mHandler;

    private Handler mCameraLiveHandler = new  Handler(){
	    @Override
		public void handleMessage(Message msg) {
		super.handleMessage(msg);
		switch (msg.what) {
		case MSG_LIVE_NOT_FINISH :
		    sendLiveMessage(LiveModule.LIVE_MSG_CAMERA_LIVE);
		    break;
		}
	    }
	};

	private LiveModule(Context context) {
		super(MODULE_NAME, context);
	}

	public static LiveModule getInstance(Context c) {
		if (null == sInstance) {
			sInstance = new LiveModule(c);
		}
		return sInstance;
	}

	@Override
	protected void onCreate() {
	}

	@Override
	protected void onRetrive(SyncData data) {
		if (DEBUG)
			Log.i(TAG, "Mobile onRetrive mHandler = " + mHandler);
		if (mHandler == null)
			return;

		int message = data.getInt(LIVE_MESSAGE);
		if (DEBUG)
			Log.i(TAG, "message = " + message);
		mCameraLiveHandler.removeMessages(MSG_LIVE_NOT_FINISH);
		switch (message) {
		case LIVE_MSG_WIFI_CONNECTED:
			Message Message = mHandler.obtainMessage();
			Message.obj = data.getString(LIVE_RTSP_URL);
			Message.what = MSG_LIVE_MSG_WIFI_CONNECTED;
			mHandler.sendMessage(Message);
			break;
		case LIVE_MSG_WIFI_UNCONNECTED:
			mHandler.sendEmptyMessage(MSG_LIVE_WIFI_UNCONNECTED);
			break;
		case LIVE_MSG_LIVE_STATUS:
			String status = data.getString(LIVE_STATUS);
			if (DEBUG)
			    Log.i(TAG,"status = " + status);
			if (status != null){
			    if (status.equals(STATUS_LIVE_NOT_FINISH)){
				mCameraLiveHandler.sendMessageDelayed(mCameraLiveHandler
								      .obtainMessage(MSG_LIVE_NOT_FINISH),1000);
			    }else{
				Message msg = mHandler.obtainMessage();
				msg.obj = status;
				msg.what = MSG_LIVE_MSG_LIVE_STATUS;
				mHandler.sendMessage(msg);
			    }
			}
			break;
		}
	}

	public void sendLiveMessage(int type) {
		if (DEBUG)
			Log.i(TAG, "sendLiveMessage type = " + type);
		SyncData data = new SyncData();
		data.putInt(LIVE_MESSAGE, type);
		try {
			send(data);
		} catch (SyncException e) {
			Log.e(TAG, "send file sync failed:" + e);
		}
	}

	public void registerHandler(Handler handler) {
		mHandler = handler;
	}

	public void unRegisterHandler() {
	    if (null != mHandler)
		mHandler = null;
	}
}
