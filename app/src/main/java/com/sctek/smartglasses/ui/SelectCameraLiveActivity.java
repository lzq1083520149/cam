package com.sctek.smartglasses.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import cn.ingenic.glasssync.devicemanager.GlassDetect;

import com.cn.zhongdun110.camlog.R;
import com.cn.zhongdun110.camlog.SyncApp;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.sctek.smartglasses.fragments.SettingFragment;
import com.sctek.smartglasses.utils.CamlogCmdChannel;

public class SelectCameraLiveActivity extends Activity implements
		OnClickListener {
	private CamlogCmdChannel mHanLangCmdChannel;
	private TextView mAPCameraLive, mRemoteCameraLive, mTencentLive, see_qq_live;
	private BluetoothAdapter mAdapter;
	private GlassDetect mGlassDetect;
	private final int MSG_TENCENT_LIVE_ENABLE = 1;
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
				case MSG_TENCENT_LIVE_ENABLE:
					mTencentLive.setEnabled(true);
					break;
				default:
					break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select);
		setTitle(R.string.glass_live);
		mAPCameraLive = (TextView) findViewById(R.id.tv_menu);
		mRemoteCameraLive = (TextView) findViewById(R.id.tv_other_menu);
		mTencentLive = (TextView) findViewById(R.id.tv_rtmp);
		see_qq_live = (TextView) findViewById(R.id.see_qq_live);
		see_qq_live.setText(R.string.tencent_live);
		mRemoteCameraLive.setText(R.string.remote_live);
		mAPCameraLive.setText(R.string.native_live);
		mTencentLive.setText(R.string.rtmp_qq_live);
		mAPCameraLive.setOnClickListener(this);
		mRemoteCameraLive.setOnClickListener(this);
		mTencentLive.setOnClickListener(this);
		see_qq_live.setOnClickListener(this);
		mAPCameraLive.setTextColor(getResources().getColor(R.color.black));
		mRemoteCameraLive.setTextColor(getResources().getColor(R.color.black));
		mTencentLive.setTextColor(getResources().getColor(R.color.black));
		see_qq_live.setTextColor(getResources().getColor(R.color.black));
		mHanLangCmdChannel = CamlogCmdChannel.getInstance(getApplicationContext());
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mGlassDetect = (GlassDetect) GlassDetect.getInstance(getApplicationContext());
//		if(!SyncApp.TENCENT_LIVE)
//			mTencentLive.setVisibility(View.INVISIBLE);
		mHanLangCmdChannel = CamlogCmdChannel.getInstance(getApplicationContext());
	}

	private void startSilentLive() {
		Packet pk = mHanLangCmdChannel.createPacket();
		pk.putInt("type", SettingFragment.SET_LIVE_AUDIO);
		pk.putBoolean("audio", false);
		mHanLangCmdChannel.sendPacket(pk);
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		Editor editor = preferences.edit();
		editor.putBoolean("live_audio", false);
		editor.apply();
		Intent intent = new Intent(this, LiveDisplayActivity.class);
		startActivity(intent);
	}

	@SuppressLint("NewApi")
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.tv_menu:
				Intent intent = new Intent(this, LiveDisplayActivity.class);
				startActivity(intent);
				finish();
				break;
			case R.id.tv_other_menu:
				if (mAdapter.isEnabled()
						&& (mAdapter
						.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED)) {
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setMessage(R.string.live_audio_hint);
					builder.setNegativeButton(R.string.close_bluetooth_a2dp,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
													int which) {
									dialog.cancel();
									mGlassDetect.set_a2dp_disconnect();
									SharedPreferences pref =
											getSharedPreferences(SyncApp.SHARED_FILE_NAME,
													MODE_PRIVATE);
									Editor editor = pref.edit();
									editor.putBoolean("last_a2dp_state", false);
									editor.apply();
									mGlassDetect.set_a2dp_disconnect();
									startActivity(new Intent(SelectCameraLiveActivity.this,
											RemoteCameraLiveActivity.class));
									finish();
								}
							});
					builder.create().show();
				} else {
					startActivity(new Intent(SelectCameraLiveActivity.this,
							RemoteCameraLiveActivity.class));
					finish();
				}
				break;
			case R.id.tv_rtmp:
				if (mHanLangCmdChannel.isConnected()) {
					Packet pk = mHanLangCmdChannel.createPacket();
					pk.putInt("type", CamlogCmdChannel.TENCENT_LIVE);
					mHanLangCmdChannel.sendPacket(pk);
					mTencentLive.setEnabled(false);
					mHandler.sendEmptyMessageDelayed(MSG_TENCENT_LIVE_ENABLE, 2000);
				} else {
					Toast.makeText(this, R.string.bluetooth_error, Toast.LENGTH_LONG).show();
				}
				break;
			case R.id.see_qq_live:
//                startActivity(new Intent(this, QQLiveActivity.class));
				startActivity(new Intent(this, RTMPLiveMainActivity.class).putExtra("bind",true));
				//finish();
				break;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mHandler.removeMessages(MSG_TENCENT_LIVE_ENABLE);
	}
}
