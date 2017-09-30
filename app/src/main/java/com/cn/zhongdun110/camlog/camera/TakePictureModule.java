package com.cn.zhongdun110.camlog.camera;

import android.content.Context;
import android.util.Log;
import android.os.Handler;

import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;

public class TakePictureModule extends SyncModule {
	private final String TAG = "TakePictureModule";
	private final static String MODULE_NAME = "picture_module";
	public static final int MSG_RECEIVE_PICTURE_DATA = 1001;
	private final int TYPE_SEND_PICTURE_DATA = 0x11;
	private final String KEY_TYPE = "type";
	private final String KEY_DATA = "pic_data";
	public static TakePictureModule sInstance;
	private Handler mHandler;

	private TakePictureModule(Context context) {
		super(MODULE_NAME, context);
		Log.e(TAG,"TakePictureModule --- init");
	}

	public static TakePictureModule getInstance(Context c) {
		if (null == sInstance)
			sInstance = new TakePictureModule(c);
		return sInstance;
	}

	@Override
	protected void onCreate() {
	    Log.e(TAG,"TakePictureModule --- onCreate");
	}

	@Override
	protected void onRetrive(SyncData data) {
		super.onRetrive(data);
		Log.i(TAG,"onRetrive in");
		int type = data.getInt(KEY_TYPE);
		switch (type) {
		case TYPE_SEND_PICTURE_DATA:
			byte[] picData = data.getByteArray(KEY_DATA);
			Log.i(TAG,"picData = " + picData.length + "--mHandler = " + mHandler);
			if (null != mHandler) {
				mHandler.obtainMessage(MSG_RECEIVE_PICTURE_DATA, picData).sendToTarget();
			}
			break;
		}
	}

	public void registerHandler(Handler handler) {
		mHandler = handler;
	}

	public void unRegisterHandler() {
		mHandler = null;
	}
}
