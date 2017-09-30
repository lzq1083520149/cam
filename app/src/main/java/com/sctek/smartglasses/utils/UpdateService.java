package com.sctek.smartglasses.utils;

import java.io.File;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

public class UpdateService extends Service {
	
	private DownloadManager mDownloadManager;
	private long mDownloadId;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
		IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		registerReceiver(mDownloadReceiver, filter);
		
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		String url = intent.getStringExtra("url");
		String name = intent.getStringExtra("name");
		Request request = new Request(Uri.parse(url));
		request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name + ".apk");
		request.setNotificationVisibility(Request.VISIBILITY_VISIBLE);
		request.setMimeType("application/vnd.android.package-archive");
		mDownloadId = mDownloadManager.enqueue(request);
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		unregisterReceiver(mDownloadReceiver);
		super.onDestroy();
	}
	
	private BroadcastReceiver mDownloadReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
			if(id == mDownloadId) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				Log.e("UpdateService", mDownloadManager.getUriForDownloadedFile(id).toString());
				i.setDataAndType(mDownloadManager.getUriForDownloadedFile(id),	"application/vnd.android.package-archive");
				startActivity(i);
			}
			stopSelf();
		}
	};
	
}
