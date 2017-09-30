package com.cn.zhongdun110.camlog.camera;

import android.content.Context;
import android.util.Log;

import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;

public class PhotoModule extends SyncModule {
    private static final String PHOTO = "photo_module";
    private static final String TAG = "PhotoModule";
    private static final boolean DEBUG = true;
    private Context mContext = null;
    private String mCameraState;

    private static final String CAMERA_TYPE = "camera_type";
    private static final int TAKE_PHOTO = 1;
    private static final int RECORD = 2;
    private static final int CAMERA_STATE = 3;
    
    private static PhotoModule sInstance = null;
    private PhotoModule(Context context){
	super(PHOTO, context);
	mContext = context;

    }

    public static PhotoModule getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new PhotoModule(c);
	return sInstance;
    }

    @Override
    protected void onCreate() {
    }
    
    
    public String getCameraState() {
	if(DEBUG)Log.i(TAG,"getCameraState mCameraState="+mCameraState);
		return mCameraState;
	}

	@Override
    protected void onRetrive(SyncData data) {
    	int type = data.getInt("type"); 
		switch (type){
		case CAMERA_STATE:
		    if(DEBUG)Log.i(TAG,"onRetrive camera state="+data.getString("glass_camera_state"));
			mCameraState = data.getString("glass_camera_state");
			break;
		default:
		    break;
		}
    }
    
    public void send_take_photo(){
	SyncData data = new SyncData();
	data.putInt(CAMERA_TYPE, TAKE_PHOTO);
	if(DEBUG)Log.i(TAG,"send_take_photo");
	try {
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }
    
    public void send_record(){
	SyncData data = new SyncData();
	data.putInt(CAMERA_TYPE, RECORD);
	if(DEBUG)Log.i(TAG,"send_start_record");
	try {
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }
    
    public void requestCameraState(){
		SyncData data = new SyncData();
		data.putInt(CAMERA_TYPE, CAMERA_STATE);
		try {
		    send(data);
		} catch (SyncException e) {
		    Log.e(TAG, "" + e);
		}
	}
}