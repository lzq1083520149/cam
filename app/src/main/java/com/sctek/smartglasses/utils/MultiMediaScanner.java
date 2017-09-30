package com.sctek.smartglasses.utils;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

public class MultiMediaScanner implements MediaScannerConnectionClient {
    private static final String TAG = "MultiMediaManager";

    private MediaScannerConnection mConnection;
    private Context mContext;
    private Handler mHandler;

    private String[] mFilepath;
    private int totalCount;
    private int deletedCount;

    public MultiMediaScanner(Context context, String[] filepath, String filetype, Handler handler) {
		mContext = context;
		mFilepath = filepath;
		mConnection = new MediaScannerConnection(mContext, this);
		totalCount = filepath.length;
		deletedCount = 0;
		mHandler = handler;
    }
    
    public void connect() {
    	mConnection.connect();
    }

    public void onMediaScannerConnected() {
		Log.e(TAG, "onMediaScannerConnected");
	//	mConnection.scanFile(mFilepath, null);
		mConnection.scanFile(mContext, mFilepath, null, this);
		if(mHandler != null)
			mHandler.sendMessage(mHandler.obtainMessage(0, deletedCount, totalCount));
    }

    public void onScanCompleted(String path, Uri uri) {
		Log.e(TAG, "onScanCompleted");
		deletedCount++;
		if(mHandler != null)
			mHandler.sendMessage(mHandler.obtainMessage(1, deletedCount, totalCount));
		if(deletedCount == totalCount) {
			if(mHandler != null)
				mHandler.sendMessage(mHandler.obtainMessage(2, deletedCount, totalCount));
			mConnection.disconnect();
		}
    }
}