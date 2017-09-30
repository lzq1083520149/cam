package com.sctek.smartglasses.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class RTMPLivePush {
	private static final String TAG = "RTMPLivePush";
	private static final boolean DEBUG = true;
	
	static {
		System.loadLibrary("rtmppush_jni");
	}
	private int mNativeContext = 0;

	private static EventHandler mEventHandler;
	
	public static final int MSG_RECEIVE_AUDIO_DATA = 1;
	public static final int MSG_RECEIVE_STOP = 2;
	
	private native final void native_startTalk(String url);

	private native final void native_stopTalk();

	private native final void native_write_aac_data(byte[] aac, int len, long pts);

	public RTMPLivePush(Context context) {
		Looper looper;
		if ((looper = Looper.myLooper()) != null) {
		    mEventHandler = new EventHandler(this, looper);
		} else if ((looper = Looper.getMainLooper()) != null) {
		    mEventHandler = new EventHandler(this, looper);
		} else {
		    mEventHandler = null;
		}
	}
	
	public void start(String url) {
		if (DEBUG)
			Log.i(TAG, "RTMPLivePush start");
		native_startTalk(url);
	}

	public void stop() {
		if (DEBUG)
			Log.i(TAG, "RTMPLivePush stop");
		native_stopTalk();
	}

	public void setAACData(byte[] aac, int len, long pts) {
		native_write_aac_data(aac, len, pts);
		
	}
	
	static void receiveState(int state){
		Log.i(TAG, "RTMPLivePush receiveState="+state);
		if(mEventHandler != null){
			mEventHandler.sendEmptyMessage(state);
		}
	}

	private class EventHandler extends Handler {
        public EventHandler(RTMPLivePush c, Looper looper) {
            super(looper);
        }
        
        @Override
        public void handleMessage(Message msg) {
        	switch (msg.what) {
		
			default:
				break;
			}
        	
        }
	}
	
}
