package com.sctek.smartglasses.utils;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.cn.zhongdun110.camlog.MediaSyncService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class PhotosSyncRunnable implements Runnable {
	
	private final static String TAG = "PhotosSyncRunnable";
	public static final String PHOTO_DOWNLOAD_FOLDER = 
			Environment.getExternalStorageDirectory().toString()	+ "/Camlog/photos/";
	
	private ArrayList<MediaData> mPhotos;
	private GlassImageDownloader mDownloader;
	private int doneCount = 0;
	private int failCount = 0;
	private int totalCount = 0;
	
	private boolean canceled = false;
	private boolean running = false;
	
	private Handler mHandler;
	
	private static PhotosSyncRunnable instance = null; 
	
	private PhotosSyncRunnable() {
		mPhotos =  new ArrayList<MediaData>();
		mDownloader =  new GlassImageDownloader();
	}
	
	public static PhotosSyncRunnable getInstance() {
		
		if(instance == null) {
			instance = new PhotosSyncRunnable();
		}
		return instance;
	}
	
	public void setHandler(Handler handler) {
		mHandler = handler;
	}
	
	public void setData(ArrayList<MediaData> data) {
		mPhotos = data;
		totalCount = data.size();
		failCount = 0;
		doneCount = 0;
		canceled = false;
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void cancel() {
		canceled = true;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		onTaskStart();
		for(MediaData data : mPhotos) {
			
			File file = new File(PHOTO_DOWNLOAD_FOLDER, data.name);
			if(file.exists()) {
				doneCount++;
				onUpdateCount(doneCount, data.name);
				continue;
			}
			
			File dir = new File(PHOTO_DOWNLOAD_FOLDER);
			if(!dir.exists())
				dir.mkdirs();
			
			try {
				
				InputStream in = mDownloader.getInputStream(data.url, 0);
				
				FileOutputStream os = new FileOutputStream(file);
				Log.e(TAG, "run: file: "+file.getAbsolutePath() );
				byte[] buffer = new byte[4096];
				int len = 0;
				while((len = in.read(buffer)) != -1 && !canceled) {
					Log.e(TAG, "" + len);
					os.write(buffer, 0, len);
					
				}
				
				if(canceled) {
					running = false;
					mPhotos.clear();
					return;
				}
				
				os.close();
				in.close();
				doneCount++;
				onUpdateCount(doneCount, data.name);
			} catch (Exception e) {
				e.printStackTrace();
				failCount++;
				if(file.exists())
					file.delete();
			}
		}
		
		onTaskDone(); 
		mPhotos.clear();
		running = false;
		
	}
	
	private void onUpdateCount(int count, String name) {
		Message msg = mHandler.obtainMessage(MediaSyncService.PHOTO_ONE_DONE, count, totalCount, name);
		msg.sendToTarget();
	}
	
	private void onTaskStart() {
		Message msg = mHandler.obtainMessage(MediaSyncService.PHOTO_TASK_START, totalCount, 0);
		msg.sendToTarget();
	}
	
	private void onTaskDone() {
		Log.e(TAG, "onTaskDone");
		Message msg = mHandler.obtainMessage(MediaSyncService.PHOTO_TASK_DONE, doneCount, failCount);
		msg.sendToTarget();
	}
	
}
