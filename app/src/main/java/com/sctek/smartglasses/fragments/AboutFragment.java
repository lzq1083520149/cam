package com.sctek.smartglasses.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.cn.zhongdun110.camlog.R;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.sctek.smartglasses.ui.WifiListActivity;
import com.sctek.smartglasses.utils.CamlogCmdChannel;

@SuppressLint("NewApi")
public class AboutFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {
	
	private final static String TAG = AboutFragment.class.getName();
	
	private final static int GET_POWER_LEVEL = 13;
	private final static int GET_STORAGE_INFO = 14;
	private final static int GET_UP_TIME = 16;
	private final static int GET_GLASS_INFO = 17;
	private final static int GET_STATE = 19;
	
	private final static int GET_POWER_TIMEOUT = 1;
	private final static int GET_STORAGE_TIMEOUT = 2;
	private final static int GET_UPTIME_TIMEOUT = 3;
	private final static int GET_STATE_TIMEOUT = 4;
        private final static int MSG_CHECK_UPDATE_TIMEOUT = 5;
	private CamlogCmdChannel mCamlogCmdChannel;
	private BluetoothAdapter mBluetoothAdapter;
	
	private Preference mCpuPreference;
	private Preference mRamPrefrence;
	private Preference mVersionPreference;
//	private Preference mSerialPreference;
	private Preference mPowerPreference;
	private Preference mStoragePreference;
	private Preference mUptimePreference;
	private Preference mStatePreference;
	private Preference mMediaPathPreference;
	private Preference mGuidePreference;
	private Preference mAppVersionPreference;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.about_preference);
		
		getActivity().getActionBar().show();
		getActivity().setTitle(R.string.about_glasses);
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
		getActivity().getActionBar().setHomeButtonEnabled(false);
		
		mCamlogCmdChannel = CamlogCmdChannel.getInstance(getActivity().getApplicationContext());
		mCamlogCmdChannel.registerHandler("AboutFragment",mChannelHandler);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if(!mBluetoothAdapter.isEnabled())
			mBluetoothAdapter.enable();
		
		initPrefereceView();
	}
	
	@Override
	public void onDestroy() {
		mCamlogCmdChannel.unregisterHandler("AboutFragment");
		super.onDestroy();
	}
	private Handler mChannelHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if(msg.what == CamlogCmdChannel.RECEIVE_MSG_FROM_GLASS) {
				Packet data = (Packet)msg.obj;
				int type = data.getInt("type");
				switch (type) {
				case GET_POWER_LEVEL:
					int level = data.getInt("power");
					String text1 = String.format((String)getResources().getText(R.string.camlog_power_state), level);
					mPowerPreference.setSummary(text1 + "%");
					removeMessages(GET_POWER_TIMEOUT);
					break;
				case GET_STORAGE_INFO:
					String total = data.getString("total");
					double totalStorage = Double.parseDouble(total.substring(0,total.length()-2));
					if(totalStorage <= 4.00)
					    totalStorage = 4.00;
					else if(totalStorage <= 8.00)
					    totalStorage = 8.00;
					else 
					    totalStorage = 16.00;
					String available = data.getString("available");
					double usedStorage;
					double avlbStorage = Double.parseDouble(available.substring(0,available.length()-2));
					if(available.endsWith("GB")){
						usedStorage = totalStorage - avlbStorage;
					}else if(available.endsWith("MB")){
						usedStorage = totalStorage - avlbStorage/1024;
					}else{
						usedStorage = totalStorage;
					}			   
					String text2 = String.format((String)getResources().getText(R.string.camlog_storage_state), totalStorage,usedStorage,available);
					mStoragePreference.setSummary(text2); 
					removeMessages(GET_STORAGE_TIMEOUT);
					break;
				case GET_UP_TIME:
					long upTime = data.getLong("uptime");
					mUptimePreference.setSummary(parseUpTime(upTime));
					removeMessages(GET_UPTIME_TIMEOUT);
					break;
				case GET_STATE:
					int state = data.getInt("state");
					mStatePreference.setSummary(parseState(state));
					removeMessages(GET_STATE_TIMEOUT);
					break;
				}
			}

			else if(msg.what == GET_POWER_TIMEOUT) {
				mPowerPreference.setSummary(R.string.get_power_timeout);
			}
			else if(msg.what == GET_STORAGE_TIMEOUT) {
				mStoragePreference.setSummary(R.string.get_storage_timeout);
			}
			else if(msg.what == GET_UPTIME_TIMEOUT) {
				mUptimePreference.setSummary(R.string.get_uptime_timeout);
			}
			else if(msg.what == GET_STATE_TIMEOUT) {
				mStatePreference.setSummary(R.string.get_state_timeout);
			}

		}
	};
	
	private void initPrefereceView() {
		
		mCpuPreference = findPreference("cpu");
		mRamPrefrence = findPreference("ram");
		mVersionPreference = findPreference("version");
//		mSerialPreference = findPreference("serial");
		mPowerPreference = findPreference("power");
		mStoragePreference = findPreference("storage");
		mUptimePreference = findPreference("uptime");
		mStatePreference = findPreference("state");
		mMediaPathPreference = findPreference("media_path");
		mGuidePreference = findPreference("guide");

		getPreferenceScreen().removePreference(mGuidePreference);

		mAppVersionPreference = findPreference("app_version");
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mCpuPreference.setSummary(sharedPreferences.getString("cpu", "Ingenic Xburst V4.15"));
		mRamPrefrence.setSummary(sharedPreferences.getString("ram", "512M"));
		mVersionPreference.setSummary("Camlog_v2.0");
		mMediaPathPreference.setSummary(Environment.getExternalStorageDirectory().toString() + "/Camlog");
		mAppVersionPreference.setSummary(getAppVersion());

		mGuidePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				FragmentManager fragManager = getActivity().getFragmentManager();
				FragmentTransaction transcaction = fragManager.beginTransaction();
				String tag = GuideViewPagerFragment.class.getName();
				GuideViewPagerFragment photoFm = (GuideViewPagerFragment)fragManager.findFragmentByTag(tag);
				if(photoFm == null)
					photoFm = new GuideViewPagerFragment();
				
				transcaction.replace(android.R.id.content, photoFm, tag);
				transcaction.addToBackStack(null);
				transcaction.commit();
				return false;
			}
		});
		
		getPower();
		
		mChannelHandler.sendEmptyMessageDelayed(GET_POWER_TIMEOUT, 5000);
		
		getStorage();
		
		mChannelHandler.sendEmptyMessageDelayed(GET_STORAGE_TIMEOUT, 5000);
		
		getUptime();
		
		mChannelHandler.sendEmptyMessageDelayed(GET_UPTIME_TIMEOUT, 5000);
		
		getState();
		
		mChannelHandler.sendEmptyMessageDelayed(GET_STATE_TIMEOUT, 5000);
	}
	
	private String parseUpTime(long time) {
		
		long totalSecond = time/1000;
		int upSecond = (int) (totalSecond%60);
		int upMinute = (int) ((totalSecond/60)%60);
		int upHour = (int) ((totalSecond/60/60));
		
		String result = null;
		if(upHour > 0 ) {
			result = String.format((String)getResources().getText(R.string.camlog_on_time_hour), upHour, upMinute, upSecond);
		}
		else if(upMinute > 0) {
			result = String.format((String)getResources().getText(R.string.camlog_on_time_minute), upMinute, upSecond);
		}
		else {
			result = String.format((String)getResources().getText(R.string.camlog_on_time_second), upSecond);
		}
		return result;
		
	}
	
	private String parseState(int state) {
		
		String result = null;
		if(state == 0)
			result = getActivity().getResources().getString(R.string.idle);
		else {
			long totalSecond = state/1000;
			int upSecond = (int) (totalSecond%60);
			int upMinute = (int) ((totalSecond/60)%60);
			int upHour = (int) ((totalSecond/60/60));
			
			if(upHour > 0 ) {
				result = String.format((String)getResources().getText(R.string.camlog_video_state_hour), upHour, upMinute, upSecond);
			}
			else if(upMinute > 0) {
				result = String.format((String)getResources().getText(R.string.camlog_video_state_minute), upMinute, upSecond);
			}
			else {
				result = String.format((String)getResources().getText(R.string.camlog_video_state_second), upSecond);
			}
		}
		return result;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		// TODO Auto-generated method stub
		String key = preference.getKey();
		if(!mCamlogCmdChannel.isConnected()) {
			Toast.makeText(getActivity(), R.string.bluetooth_error, Toast.LENGTH_LONG).show();
			return true;
		}

		return true;
	}
	
	private void getPower() {
		Packet pk = mCamlogCmdChannel.createPacket();
		pk.putInt("type", GET_POWER_LEVEL);
		mCamlogCmdChannel.sendPacket(pk);
	}
	
	private void getStorage() {
		Packet pk = mCamlogCmdChannel.createPacket();
		pk.putInt("type", GET_STORAGE_INFO);
		mCamlogCmdChannel.sendPacket(pk);
	}
	
	private void getUptime() {
		Packet pk = mCamlogCmdChannel.createPacket();
		pk.putInt("type", GET_UP_TIME);
		mCamlogCmdChannel.sendPacket(pk);
	}
	
	private void getState() {
		Packet pk = mCamlogCmdChannel.createPacket();
		pk.putInt("type", GET_STATE);
		mCamlogCmdChannel.sendPacket(pk);
	}
	
	private String getAppVersion() {
		try {
			PackageManager pm = getActivity().getPackageManager();
			PackageInfo pi = pm.getPackageInfo(getActivity().getPackageName(), 0);
			return pi.versionName;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

        private void showUpdateConfirmDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.software_updates);
		builder.setMessage(R.string.updates_note);
		builder.setNegativeButton(R.string.update_later, new DialogInterface.OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});		
		builder.setPositiveButton(R.string.update_now, new DialogInterface.OnClickListener() {	
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				Intent intent =new Intent(getActivity(),WifiListActivity.class);
				intent.putExtra("wifi_type",WifiListActivity.TYPE_UPDATE);
				startActivity(intent);
			}
		});		
		builder.create().show();
	}
    
}
