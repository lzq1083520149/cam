package com.cn.zhongdun110.camlog.multimedia;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.net.Uri;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;

public class MultiMediaScanner implements MediaScannerConnectionClient {
    private static final String TAG = "MultiMediaScanner";

    private MediaScannerConnection mConnection;
    private Context mContext;
    private Handler mCallback;

    private String mFilepath;

    public MultiMediaScanner(Context context, String filepath, String filetype) {
	mContext = context;
	mCallback = mHandler;
	mFilepath = filepath;
	mConnection = new MediaScannerConnection(mContext, this);
	mConnection.connect();
    }

    public void onMediaScannerConnected() {
	mConnection.scanFile(mFilepath, null);
    }

    public void onScanCompleted(String path, Uri uri) {
	mConnection.disconnect();
	Intent sync_intent = new Intent("sync_file_finish");
	sync_intent.putExtra("path",path);
	mContext.sendBroadcast(sync_intent);
    }

    private static Handler mHandler = new Handler() {  
	    public void handleMessage(Message msg) {  
		switch (msg.what) {  
		default:  
		    break;  
		}  
	    }  
	}; 
}
