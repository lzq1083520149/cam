package com.cn.zhongdun110.camlog;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.cn.zhongdun110.camlog.camera.LiveModule;
import com.cn.zhongdun110.camlog.camera.PhotoModule;
import com.cn.zhongdun110.camlog.camera.TakePictureModule;
import com.cn.zhongdun110.camlog.contactslite.ContactsModule;
import com.sctek.smartglasses.control.RestartApacheModule;
import com.sctek.smartglasses.language.LanguageModule;
import com.sctek.smartglasses.utils.CamlogCmdChannel;
import com.sctek.smartglasses.utils.CamlogNotifyChannel;
import com.sctek.smartglasses.utils.CrashHandler;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.LinkedList;
import java.util.List;

import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.Enviroment;
import cn.ingenic.glasssync.LogTag;
import cn.ingenic.glasssync.SystemModule;
import cn.ingenic.glasssync.devicemanager.GlassDetect;

public class SyncApp extends Application implements
        Enviroment.EnviromentCallback, ActivityLifecycleCallbacks {
	private String TAG = "SyncApp";
	public static final String SHARED_FILE_NAME = "Install";
	public static final boolean REMOTE_CAMERA_LIVE = true;
	public static final boolean BINE_SINGLE_BTNAME = false;

    //public static final String[] BT_NAME_ARY = {"WEAR","COLDWAVEIPAL","CAMLOG","MARKEN","ZBR","RAYPAI","CLIQUE","TSYC","G1","COOLGLASS"};
    public static final String[] BT_NAME_ARY = {"CAMLOG"};
	public final static String COMPANY_UID = "0c790f12-dcc2-4733-83b6-873be1334ac2";
	private List<Activity> mActivityList = new LinkedList<Activity>();
	public static SyncApp mInstance;
	private int mTouchAudioState;

	@Override
	public void onCreate() {
		super.onCreate();
		mInstance = this;
		if (LogTag.V) {
			Log.i(LogTag.APP, "Sync App created.");
		}
		String processName = getProcessName(this, android.os.Process.myPid());
		if (processName != null) {
			boolean defaultProcess = processName.equals("com.cn.zhongdun110.camlog");
			if (!defaultProcess)
				return;
		}
		Enviroment.init(this);
		DefaultSyncManager manager = DefaultSyncManager.init(this);
		SystemModule systemModule = new SystemModule();
		try {
			mTouchAudioState = Settings.System.getInt(getContentResolver(),
					Settings.System.SOUND_EFFECTS_ENABLED);
			Log.i(TAG, "mTouchAudioState = " + mTouchAudioState);
		} catch (Settings.SettingNotFoundException exception) {
			Log.e(LogTag.APP, "----SettingNotFoundException");
		}

		PhotoModule.getInstance(this);

		if (manager.registModule(systemModule)) {
			Log.i(LogTag.APP, "SystemModule is registed.");
		}

		LiveModule.getInstance(this);
		LanguageModule.getInstance(this);
		GlassDetect gdt = GlassDetect.getInstance(this);

		CamlogCmdChannel.getInstance(this);
		CamlogNotifyChannel.getInstance(this);
		startService(new Intent(this, MediaSyncService.class));

		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread thread, Throwable ex) {
				ex.printStackTrace();
			}
		});

		RestartApacheModule.getInstance(this);
		ContactsModule.getInstance(this);
		registerActivityLifecycleCallbacks(this);
		TakePictureModule.getInstance(this);

		CrashHandler crashHandler = CrashHandler.getInstance();
		crashHandler.init(this);

	}

	@Override
	public Enviroment createEnviroment() {
		return new PhoneEnviroment(this);
	}

	private String getProcessName(Context cxt, int pid) {
		ActivityManager am = (ActivityManager) cxt
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
		if (runningApps == null) {
			return null;
		}
		for (RunningAppProcessInfo procInfo : runningApps) {
			if (procInfo.pid == pid) {
				return procInfo.processName;
			}
		}
		return null;
	}

	public static SyncApp getInstance() {
		return mInstance;
	}

	public void addActivity(Activity activity) {
		mActivityList.add(activity);
	}

	public void removeActivity(Activity activity) {
		mActivityList.remove(activity);
	}

	public void exitAllActivity() {
		try {
			for (Activity activity : mActivityList) {
				if (activity != null)
					activity.finish();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// System.exit(0);
		}
	}

	public boolean isOpenTouchAudio() {
		Log.i(TAG, "return mTouchAudioState = " + mTouchAudioState);
		return (mTouchAudioState == 1 ? true : false);
	}

	@Override
	public void onActivityCreated(Activity arg0, Bundle arg1) {

	}

	@Override
	public void onActivityDestroyed(Activity arg0) {

	}

	@Override
	public void onActivityPaused(Activity arg0) {

	}

	@Override
	public void onActivityResumed(Activity arg0) {
		if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.dialog_title);
			builder.setNegativeButton(R.string.dialog_cancle,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							System.exit(0);
						}
					});

			builder.setPositiveButton(R.string.dialog_ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivity(intent);
						}
					});

			AlertDialog dialog = builder.create();
			dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
			dialog.setCanceledOnTouchOutside(false);
			dialog.setOnKeyListener(keylistener);
			dialog.show();
		}
	}

	@Override
	public void onActivitySaveInstanceState(Activity arg0, Bundle arg1) {

	}

	@Override
	public void onActivityStarted(Activity arg0) {

	}

	@Override
	public void onActivityStopped(Activity arg0) {

	}

	OnKeyListener keylistener = new DialogInterface.OnKeyListener() {

		@Override
		public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
			if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
				return true;
			} else {
				return false;
			}
		}
	};
}
