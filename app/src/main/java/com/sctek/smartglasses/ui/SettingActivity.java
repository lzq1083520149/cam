package com.sctek.smartglasses.ui;

import com.sctek.smartglasses.fragments.SettingFragment;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;

import com.cn.zhongdun110.camlog.SyncApp;
public class SettingActivity extends BaseFragmentActivity {

	private String TAG;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SyncApp.getInstance().addActivity(this);
		new Handler().post(new Runnable() {
			
			@SuppressLint("NewApi")
			@Override
			public void run() {
				TAG = SettingFragment.class.getName();
				SettingFragment settingGF = (SettingFragment)getFragmentManager().findFragmentByTag(TAG);
				if(settingGF == null) {
					settingGF = new SettingFragment();
				}
				getFragmentManager().beginTransaction()
						.replace(android.R.id.content, settingGF, TAG).commit();
			}
		});
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		SyncApp.getInstance().removeActivity(this);
	}
}
