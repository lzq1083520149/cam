package com.sctek.smartglasses.ui;

import java.io.File;
import java.util.ArrayList;

import com.cn.zhongdun110.camlog.MediaSyncService;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.utils.StorageUtils;
import com.sctek.smartglasses.fragments.NativeVideoGridFragment;
import com.sctek.smartglasses.utils.MediaData;

import android.util.Log;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;

public class VideoActivity extends BaseFragmentActivity {

	private String TAG;
	private MediaSyncService mMediaSyncService;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		SyncApp.getInstance().addActivity(this);
		initImageLoader(getApplicationContext());
		
		new Handler().post(new Runnable() {
			
			@SuppressLint("NewApi")
			@Override
			public void run() {
				// TODO Auto-generated method stub
				TAG = NativeVideoGridFragment.class.getName();
				NativeVideoGridFragment VideoGF = (NativeVideoGridFragment)getFragmentManager().findFragmentByTag(TAG);
				if(VideoGF == null) {
					VideoGF = new NativeVideoGridFragment();
					
					Bundle vBundle = new Bundle();
					vBundle.putInt("index", NativeVideoGridFragment.FRAGMENT_INDEX);
					VideoGF.setArguments(vBundle);
					getFragmentManager().beginTransaction()
							.replace(android.R.id.content, VideoGF, TAG).commit();
				}
			}
		});
		
		bindService(new Intent(this, MediaSyncService.class), mConnection, BIND_AUTO_CREATE);
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		int stackCount = getFragmentManager().getBackStackEntryCount();
		if(stackCount != 0) {
			getFragmentManager().popBackStack();
		}
		else 
			super.onBackPressed();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		unbindService(mConnection);
//		SyncApp.getInstance().removeActivity(this);
		super.onDestroy();
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onServiceConnected");
			mMediaSyncService = ((MediaSyncService.ServiceBinder)service).getService();
		}
	};
	
	public void startVideoSync(ArrayList<MediaData> data)  {
		Log.e(TAG, "startPhotoSync");
		mMediaSyncService.startVideoSync(data);
	}
	
	public static void initImageLoader(Context context) {
		// This configuration tuning is custom. You can tune every option, you may tune some of them,
		// or you can create default configuration by
		//  ImageLoaderConfiguration.createDefault(this);
		// method.
		String cacheDir = Environment.getExternalStorageDirectory().getAbsolutePath()
				+ "/.glasses_image_cache";
		File cacheFile = StorageUtils.getOwnCacheDirectory(context, cacheDir);
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
				.threadPriority(Thread.NORM_PRIORITY - 2)
				.threadPoolSize(3)
				.denyCacheImageMultipleSizesInMemory()
				.diskCacheFileNameGenerator(new Md5FileNameGenerator())
				.diskCacheSize(50 * 1024 * 1024) // 50 Mb
				.diskCache(new UnlimitedDiskCache(cacheFile))
				.tasksProcessingOrder(QueueProcessingType.LIFO)
				.writeDebugLogs() // Remove for release app
				.diskCacheExtraOptions(480, 320, null)
				.build();
		// Initialize ImageLoader with configuration.
		
		if(!ImageLoader.getInstance().isInited())
			ImageLoader.getInstance().init(config);
	}

}
