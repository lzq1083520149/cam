package com.sctek.smartglasses.utils;

//MediaMetadataRetriever mMRetriever = new MediaMetadataRetriever();
//mMRetriever.setDataSource(url[0], new HashMap<String, String>());
//		
// bm = mMRetriever.getFrameAtTime();
//
//bm = ThumbnailUtils.extractThumbnail(bm, 100, 100
//		, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

public class GetRemoteVideoThumbWorks {
	
	private static final String TAG = "GetRemotevideoThumbTask";
	
	private static final int MAX_CACHE_SIZE = 20;
	private static final int THREAD_POOL_SIZE = 5;
	private static final int MAX_MEMORY_SIZE = (int) (Runtime.getRuntime().maxMemory()/(1024*8));
	
	public int position;
	
//	private Map<String, SoftReference<Bitmap>> thumbCache;
	private LruCache<String, Bitmap> thumbCache;
	private ExecutorService mExecutorService;
	private boolean paused;
	private Handler handler;
	
	private static volatile GetRemoteVideoThumbWorks instance = null;
	
	private GetRemoteVideoThumbWorks() {
		
//		thumbCache = new HashMap<String, SoftReference<Bitmap>>();
		thumbCache = new LruCache<String, Bitmap>(MAX_CACHE_SIZE);
		mExecutorService = Executors.newFixedThreadPool(5);
		paused = false;
		handler = new Handler();
	}
	
	public static synchronized GetRemoteVideoThumbWorks getInstance() {
		if(instance == null)
			instance = new GetRemoteVideoThumbWorks();
		return instance;
	}
	
	public void pause() {
		paused = true;
	}
	
	public void resume() {
		paused = false;
	}
	
	public void stop() {
		mExecutorService.shutdownNow();
	}
	
	public void getRemoteVideoThumb(final String url, final GetRemoteVideoThumbListener listener) {
		
		if(mExecutorService.isShutdown())
			mExecutorService = Executors.newFixedThreadPool(5);
		
		mExecutorService.submit(new Runnable() {
			
			@SuppressLint("NewApi")
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if(paused)
					return;
				
				Bitmap temp = thumbCache.get(url);
				
				if(temp == null) {
					
					MediaMetadataRetriever mMRetriever = new MediaMetadataRetriever();
					mMRetriever.setDataSource(url, new HashMap<String, String>());
					
					
					temp = mMRetriever.getFrameAtTime(-1);
					
					temp = ThumbnailUtils.extractThumbnail(temp, 100, 100
							, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
					
					thumbCache.put(url, temp);
					
				}
				final Bitmap bm = temp;
				handler.post(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						listener.onGetRemoteVideoThumbDone(bm);
					}
				});
			}
		});
	}
	
	public static interface GetRemoteVideoThumbListener {
		void onGetRemoteVideoThumbDone(Bitmap bitmap);
	}

}
