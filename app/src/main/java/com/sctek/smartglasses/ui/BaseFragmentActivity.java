package com.sctek.smartglasses.ui;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import com.cn.zhongdun110.camlog.SyncApp;

public class BaseFragmentActivity extends FragmentActivity {
	private String TAG = "BaseFragmentActivity";
	private AudioManager mAudioManager;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onStart(){
		super.onStart();
		mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		Settings.System.putInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 0);
		mAudioManager.unloadSoundEffects();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG,"base onStop");
		if (!isAppOnForeground()) {
		    Log.i(TAG,"app is on background");
		      //app 进入后台
		      //全局变量 记录当前已经进入后台
		    if(SyncApp.getInstance().isOpenTouchAudio()){
			Settings.System.putInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 1);
			mAudioManager.loadSoundEffects();
		    }
		}
	}
	
	/**
     * 程序是否在前台运行
     * 
     * @return
     */
    public boolean isAppOnForeground() {
            // Returns a list of application processes that are running on the
            // device             
            ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            String packageName = getApplicationContext().getPackageName();

            List<RunningAppProcessInfo> appProcesses = activityManager
                            .getRunningAppProcesses();
            if (appProcesses == null)
                    return false;
            for (RunningAppProcessInfo appProcess : appProcesses) {
                    // The name of the process that this object is associated with.
                    if (appProcess.processName.equals(packageName)
                                    && appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                            return true;
                    }
            }
            return false;
    }
}
