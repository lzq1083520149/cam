package com.sctek.smartglasses.utils;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.cn.zhongdun110.camlog.MediaSyncService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;

public class VideoSyncRunnable implements Runnable {
	
	private final static String TAG = "VideoSyncRunnable";
	public static final String VIDEO_DOWNLOAD_FOLDER = 
			Environment.getExternalStorageDirectory().toString()	+ "/Camlog/videos/";
	
	private ArrayList<MediaData> mVideos;
	private GlassImageDownloader mDownloader;
	private int doneCount = 0;
	private int failCount = 0;
	private int totalCount = 0;
	
	private boolean canceled = false;
	private boolean running = false;
	
	private Handler mHandler;
	
	private static VideoSyncRunnable instance = null;
	
	public static VideoSyncRunnable getInstance() {
		if(instance == null)
			instance = new VideoSyncRunnable();
		return instance;
	}
	
	private VideoSyncRunnable() {
		mVideos = new ArrayList<MediaData>();
		mDownloader =  new GlassImageDownloader();
	}
	
	public void setHandler(Handler handler) {
		mHandler = handler;
	}
	
	public void setData(ArrayList<MediaData> data) {
		mVideos = data;
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
		running = true;
		onTaskStart();
		for(MediaData data : mVideos) {
			
			long startPostion = 0;
			long videoLeng = 0;
			HttpURLConnection conn = null;
			Log.e(TAG, data.name);
			File file = new File(VIDEO_DOWNLOAD_FOLDER, data.name);
			conn = mDownloader.createConnection(data.url, 0);
			videoLeng = conn.getContentLength();
			if(file.exists()) {
				if(file.length() < videoLeng) {
					startPostion = file.length();
				}
				else {
					doneCount++;
					onUpdateCount(doneCount, data.name);
					conn.disconnect();
					continue;
				}
			}
			conn.disconnect();
			
			File dir = new File(VIDEO_DOWNLOAD_FOLDER);
			if(!dir.exists())
				dir.mkdirs();
			
			try {
				InputStream in = mDownloader.getInputStream(data.url, startPostion);
				
				FileOutputStream os = new FileOutputStream(file, true);
				byte[] buffer = new byte[4096];
				int len = 0;
				long downLoadedLength = startPostion;
				long tempDownLoadLength = 0;
				long delayTime = 0;
				long notifyTime = System.currentTimeMillis();
				while((len = in.read(buffer)) != -1 && !canceled) {
					Log.e(TAG, "" + len);
					downLoadedLength += len;
					tempDownLoadLength += len;
					os.write(buffer, 0, len);
					delayTime = System.currentTimeMillis() - notifyTime;
					if(delayTime > 2000) {
						onUpdateProgress(downLoadedLength, videoLeng, tempDownLoadLength/2);
						notifyTime = System.currentTimeMillis();
						tempDownLoadLength = 0;
					}
				}
				
				if(canceled) {
					running = false;
					mVideos.clear();
					return;
				}
				
				os.close();
				in.close();
				doneCount++;
				onUpdateCount(doneCount, data.name);
			} catch (Exception e) {
				failCount++;
				onUpdateProgress(0, 1, 0);
				e.printStackTrace();
			}
		}
		
		onTaskDone(); 
		mVideos.clear();
		running = false;
		
	}
	
	private void onTaskDone() {
		Log.e(TAG, "onTaskDone");
		Message msg = mHandler.obtainMessage(MediaSyncService.VIDEO_TASK_DONE, doneCount, failCount);
		msg.sendToTarget();
	}
	
	private void onUpdateCount(int count, String name) {
		Log.e(TAG, "onUPdateCount");
		Message msg = mHandler.obtainMessage(MediaSyncService.VIDEO_ONE_DONE, count, totalCount, name);
		msg.sendToTarget();
	}
	
	private void onTaskStart() {
		Log.e(TAG, "onTaskStart");
		Message msg = mHandler.obtainMessage(MediaSyncService.VIDEO_TASK_START, totalCount, 0);
		msg.sendToTarget();
	}
	
	private void onUpdateProgress(long progress, long total, long speed) {
		Log.e(TAG, "onProgressUpdateCount:" + progress + "/" + total);
		Message msg = mHandler.obtainMessage(MediaSyncService.VIDEO_PROGRESS_UPDATE, (int)progress, (int)total, speed);
		msg.sendToTarget();
	}
	
}
