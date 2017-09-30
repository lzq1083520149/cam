package com.cn.zhongdun110.camlog.contactslite;

import android.content.Context;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.mid.MidTableManager;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncException;
import android.util.Log;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
public class ContactsLiteModule extends SyncModule {
	public static final String MODULE_NAME = "CONTACTS";
	private static final String TAG = MODULE_NAME;
	private static SyncModule sInstance = null;
    private int MSG_SEND_FINISH = 1;
    private int MSG_SEND_FAIL = 2;
	private Context mContext;
    private boolean mSyncEnabled = false;
        private static final String SYNC_REQUEST = "sync_request"; //The length of not more than 15
        private CallBackHandler mCallBackHandler;
        private	Handler mHandler;
        private ContactsLiteModule(Context context) {
		super(MODULE_NAME, context, true);
		mContext = context;
		mSyncEnabled = false;
		HandlerThread ht = new HandlerThread("app_manager_call_back");
		ht.start();
		mCallBackHandler = new CallBackHandler(ht.getLooper());
	}
	
	public synchronized static SyncModule getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new ContactsLiteModule(context);
		}
		return sInstance;
	}
	
    public void sendSyncRequest(boolean enabled, Handler handler ) {
	    Log.i(TAG, "---sendSyncRequest");
	    SyncData data = new SyncData();
	    data.putBoolean(SYNC_REQUEST, enabled);
	    mHandler=handler;
	    SyncData.Config config = new SyncData.Config();
	    Message m = mCallBackHandler.obtainMessage();		
	    m.what = 1;
	    config.mmCallback = m;
	    data.setConfig(config);
	    try {
		send(data);
	    } catch (SyncException e) {
		Log.e(TAG, "---send sync failed:" + e);
	    }
	}

	@Override
	protected void onCreate() {
		
	}
        @Override
	    public void setSyncEnable(boolean enabled){
	    mSyncEnabled = enabled;
	}

        @Override
	    public boolean getSyncEnable(){
	    return mSyncEnabled;
	}
	
	@Override
	public MidTableManager getMidTableManager() {
		return ContactsLiteMidSrcManager.getInstance(getcontext(), this);
	}

	private Context getcontext() {
		return mContext;
	}
	private class CallBackHandler extends Handler {
		public CallBackHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if(mHandler == null) return;
			switch (msg.what) {
			case 1 :
			    Log.i(TAG,"success"+msg.arg1);
			    if(msg.arg1 == 0){
				Message message= mHandler.obtainMessage();
				message.obj = MODULE_NAME;
			        message.what = MSG_SEND_FINISH;
			        message.sendToTarget();
				Log.i(TAG, "msg.sendToTarget():"+"msg.obj="+ MODULE_NAME );
			    }else{
				Message message = mHandler.obtainMessage();
				message.obj = MODULE_NAME;
			        message.what = MSG_SEND_FAIL;
			        message.sendToTarget();
				Log.i(TAG, "msg.sendToTarget():"+"msg.obj="+MODULE_NAME);
			    }

			}

		}

	}
}
