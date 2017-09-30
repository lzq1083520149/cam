package com.sctek.smartglasses.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.cn.zhongdun110.camlog.R;
import com.cn.zhongdun110.camlog.SyncApp;
import com.cn.zhongdun110.camlog.contactslite.ContactsLiteModule;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.sctek.smartglasses.biz.BLContacts;
import com.sctek.smartglasses.language.LanguageModule;
import com.sctek.smartglasses.ui.WifiListActivity;
import com.sctek.smartglasses.utils.CamlogCmdChannel;

import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.devicemanager.GlassDetect;

@SuppressLint("NewApi")
public class SettingFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener
							,Preference.OnPreferenceClickListener {
	
	private final static String TAG = SettingFragment.class.getName();
	
	public final static int SET_PHOTO_PIXEL = 2;
	public final static int SET_VEDIO_PIXEL = 3;
	public final static int SET_VEDIO_DURATION = 4;
	public final static int SWITCH_GLASSES = 5;
        public final static int WIFI_CONNECT_STATE_CHANGED = 15;
	public final static int SWITCH_ANTI_SHAKE = 16;
	public final static int SWITCH_TIME_STAMP = 17;
	public final static int TURN_WIFI_OFF = 18;
	public final static int SET_VOLUME = 8;
	public final static int SWITCH_ROUND_VIDEO = 12;
        public final static int LANGUAGE_SETTING = 13;
	public final static int SET_LIVE_AUDIO = 25;
	public final static int SET_VOICE_RECOG = 27;
	public final static int GET_WIFI_CONNECT_STATE = 28;

	public final static int MSG_SEND_FINISH = 1;
	public final static int CONTACT_READABLE = 103;
	public final static int CONNECT_WIFI_TIMEOUT = 104;
	public final static int GET_WIFI_CONNECT_STATE_TIMEOUT = 105;

	public final static int SETTING_DELAY_TIME = 3000;
        public final static int SYNCWIFI_DELAYTIMES = 20*1000; //20s 
	public final int PHONE_AUDIO_CONNECT = 6;
	public final int PHONE_AUDIO_DISCONNECT = 7;
	public final int PHONE_A2DP_CONNECT = 8;
	public final int PHONE_A2DP_DISCONNECT = 9;
    
        // language setting
        public static final int LANGUAGE_ZH = 0;
        public static final int LANGUAGE_US = 1;
        public static final int LANGUAGE_FR = 2;
        public static final int LANGUAGE_RU = 3;
        public static final int LANGUAGE_DE = 4;
        public static final int LANGUAGE_TH = 5;
        public static final int LANGUAGE_FA = 6;
        public static final int LANGUAGE_ES = 7;
        public static final int LANGUAGE_PT = 8;
        public static final int LANGUAGE_AR = 9;
	public static final int LANGUAGE_IT = 10;
	
	private boolean contactReadable = false;
	private boolean syncContactToGlass = false;
        private int mIsDefaultLanguage = 0;
	
	private static final String[] lables = {"pixel", "pixel", "pixel", "duration", "sw", "sw", "sw", "volume", "ssid", "pw", "NULL", "sw" };
	private static final String[] keys = {"NULL", "photo_pixel", "vedio_pixel", "duration", 
		"default_switch", "anti_shake", "timestamp"};
	
        private ListPreference mVedioDurationPreference,mLanguagePreference;
    //	private VolumeSeekBarPreference mVolumeSeekBarPreference;
        private Preference mWifiSyncPreference,mWaterMarkPreference;
	private CheckBoxPreference mBluetoothPhonePreference;
	private CheckBoxPreference mBluetoothA2dpPreference;
	private CheckBoxPreference mRoundVideoPreference;
	private CheckBoxPreference mSyncContactPreference , mLiveAudioPreference;
	private CheckBoxPreference mVoiceRecogPreference;
        private CheckBoxPreference mLiveRecordPreference;
	private SharedPreferences mHeadsetPreferences;
	
	private BluetoothAdapter mBluetoothAdapter;
	
	private boolean setBack = false;
	
	private GlassDetect mGlassDetect;
	
	private ProgressDialog mProgressDialog;
	
	private CamlogCmdChannel mCamlogCmdChannel;
	private ProgressDialog mWifiSyncProgressDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.setting_preference);
		
		getActivity().getActionBar().show();
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
		getActivity().getActionBar().setHomeButtonEnabled(false);
		getActivity().setTitle(R.string.setting);
		
		mHeadsetPreferences = getActivity().getApplicationContext().
				getSharedPreferences(SyncApp.SHARED_FILE_NAME, Context.MODE_PRIVATE);
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		mGlassDetect = (GlassDetect)GlassDetect.getInstance(getActivity());
		String addr = DefaultSyncManager.getDefault().getLockedAddress();
		mGlassDetect.setLockedAddress(addr);
		mProgressDialog = new ProgressDialog(getActivity());
		mCamlogCmdChannel = CamlogCmdChannel.getInstance(getActivity().getApplicationContext());
		mCamlogCmdChannel.registerHandler("SettingFragment",handler);
//		mGlassDetect.setCallBack(handler);
		
		IntentFilter filter = new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
		filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
		getActivity().registerReceiver(mBroadcastReceiver, filter);
		
		if(!mBluetoothAdapter.isEnabled())
			mBluetoothAdapter.enable();
		
		checkContactReadable();
		
		initPrefereceView();
		initProgressDialog();
		getWifiConnectState();
	}

        @Override
	public void onResume() {
	    super.onResume();
	    Log.i(TAG,"onResume in--");
	    if (mGlassDetect.getCurrentHeadSetState() == BluetoothProfile.STATE_CONNECTED)
		mBluetoothPhonePreference.setChecked(true);
	    else mBluetoothPhonePreference.setChecked(false);

	    if (mGlassDetect.getCurrentA2dpState() == BluetoothProfile.STATE_CONNECTED)
		mBluetoothA2dpPreference.setChecked(true);
	    else mBluetoothA2dpPreference.setChecked(false);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View view = super.onCreateView(inflater, container, savedInstanceState);
		return view;
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		getActivity().unregisterReceiver(mBroadcastReceiver);
		handler.removeCallbacksAndMessages(null);
		mCamlogCmdChannel.unregisterHandler("SettingFragment");
		super.onDestroy();
	}
	
	private void initPrefereceView() {
		
		mVedioDurationPreference = (ListPreference)findPreference("duration");
		mLanguagePreference = (ListPreference)findPreference("language");
//		mVolumeSeekBarPreference = (VolumeSeekBarPreference)findPreference("volume");
		mWaterMarkPreference = (Preference)findPreference("water_mark");
		mBluetoothPhonePreference = (CheckBoxPreference)findPreference("phone_on");
		mBluetoothA2dpPreference = (CheckBoxPreference)findPreference("a2dp_on");
		mRoundVideoPreference = (CheckBoxPreference)findPreference("round_video");
		mSyncContactPreference = (CheckBoxPreference)findPreference("sync_contact");
		mLiveAudioPreference = (CheckBoxPreference)findPreference("live_audio");
		mWifiSyncPreference = (Preference)findPreference("wifi_sync");
		mVoiceRecogPreference = (CheckBoxPreference)findPreference("voice_recog");
		mLiveRecordPreference = (CheckBoxPreference)findPreference("live_record");
		SharedPreferences preferences = PreferenceManager.
			getDefaultSharedPreferences(getActivity().getApplicationContext());
		mVoiceRecogPreference.setChecked(preferences.getBoolean("voice_recog", true));
		mLiveRecordPreference.setChecked(preferences.getBoolean("live_record", true));
	      	try {
			mVedioDurationPreference.setOnPreferenceChangeListener(this);
			mLanguagePreference.setOnPreferenceChangeListener(this);
//			mVolumeSeekBarPreference.setOnPreferenceChangeListener(this);
			mBluetoothPhonePreference.setOnPreferenceChangeListener(this);
			mBluetoothA2dpPreference.setOnPreferenceChangeListener(this);
			mRoundVideoPreference.setOnPreferenceChangeListener(this);
			mSyncContactPreference.setOnPreferenceChangeListener(this);
			mLiveAudioPreference.setOnPreferenceChangeListener(this);
			mWaterMarkPreference.setOnPreferenceClickListener(this);
			mWifiSyncPreference.setOnPreferenceClickListener(this);
			mVoiceRecogPreference.setOnPreferenceChangeListener(this);
			mLiveRecordPreference.setOnPreferenceChangeListener(this);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void onPreferenceChanged(Preference preference, Object value) {
		
		String key = preference.getKey();
		Packet pk = mCamlogCmdChannel.createPacket();
		Log.e(TAG, "" + key);
		if("duration".equals(key)) {
			
			String duration = (String)value;
			pk.putInt("type", SET_VEDIO_DURATION);
			pk.putString("duration", duration);
			
		}
		else if("volume".equals(key)) {
			int volume = (Integer)value;
			pk.putInt("type", SET_VOLUME);
			pk.putInt("volume", volume);
		}
		else if("phone_on".equals(key)) {
			boolean on = (Boolean)value;
			if(on) {
				Log.e(TAG, "set phone on");
				mProgressDialog.setMessage(getActivity().getResources().getText(R.string.turning_bluetoothheadset_on));
				mProgressDialog.show();
				mGlassDetect.set_audio_connect();
			}
			else {
				Log.e(TAG, "set phone off");
				mProgressDialog.setMessage(getActivity().getResources().getText(R.string.turning_bluetoothheadset_off));
				mProgressDialog.show();
				mGlassDetect.set_audio_disconnect();
			}
			return;
		}
		else if("a2dp_on".equals(key)) {
			boolean on = (Boolean)value;
			if(on) {
				Log.i(TAG, "set a2dp on");
				mProgressDialog.setMessage(getActivity().getResources().getText(R.string.turning_bluetoothheadset_on));
				mProgressDialog.show();
				mGlassDetect.set_a2dp_connect();
			}
			else {
				Log.i(TAG, "set a2dp off");
				mProgressDialog.setMessage(getActivity().getResources().getText(R.string.turning_bluetoothheadset_off));
				mProgressDialog.show();
				mGlassDetect.set_a2dp_disconnect();
			}
			return;
		}
		else if("round_video".equals(key)) {
			boolean sw = (Boolean)value;
			pk.putInt("type", SWITCH_ROUND_VIDEO);
			pk.putBoolean("sw", sw);
		}
		else if("sync_contact".equals(key)) {
			syncContactToGlass = (Boolean)value;
			if(syncContactToGlass) {
				if(contactReadable) {
					syncContactToGlass(syncContactToGlass);
				}
				else {
					checkContactReadable();
					Toast.makeText(getActivity(), R.string.no_read_contact_permission, Toast.LENGTH_SHORT).show();
				}
			}
			else {
				syncContactToGlass(syncContactToGlass);
			}
			return ;
		}
		
		else if("language".equals(key)){
		    String type = (String)value;
		    chooseLanguage(type);
		    return ;	
		}else if("live_audio".equals(key)){
			boolean audio = (Boolean)value;
			Log.i(TAG, "onPreferenceChanged :: audio =" +audio );
			pk.putInt("type", SET_LIVE_AUDIO);
			pk.putBoolean("audio", audio);
		}else if("voice_recog".equals(key)){
			boolean voice_recog_enabled = (Boolean)value;
			Log.i(TAG, "onPreferenceChanged :: voice_recog_enabled =" + voice_recog_enabled);
			pk.putInt("type", SET_VOICE_RECOG);
			pk.putBoolean("voice_recog", voice_recog_enabled);
		}else if("live_record".equals(key)){
		        boolean live_record_enabled = (Boolean)value;
			Log.i(TAG, "onPreferenceChanged :: live_record_enabled =" + live_record_enabled);
			pk.putInt("type", CamlogCmdChannel.TYPE_SET_LIVERECORD);
			pk.putBoolean("live_record", live_record_enabled);
		}		
		mCamlogCmdChannel.sendPacket(pk);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// TODO Auto-generated method stub
		
		if(setBack) {
			Log.e(TAG, "set back true");
			setBack = false;
			handler.removeCallbacks(disableSetBackRunnable);
			return true;
		}
		Log.e(TAG, "set back false");
		String key = preference.getKey();
		if(mCamlogCmdChannel.isConnected()) {
			onPreferenceChanged(preference, newValue);
			return false;
		}
		else {
			Toast.makeText(getActivity(), R.string.bluetooth_error, Toast.LENGTH_LONG).show();
			return false;
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		// TODO Auto-generated method stub
		Log.e(TAG, "onPreferenceClick");
		String key = preference.getKey();
		Log.e(TAG, key);
		if("wifi_sync".equals(key)) {
		    Intent intent =new Intent(getActivity(),WifiListActivity.class);
		    intent.putExtra("wifi_type",WifiListActivity.TYPE_CONNECT);
		    startActivityForResult(intent,0);
		}else if("water_mark".equals(key)){
		    Intent intent = new Intent(getActivity(),WaterMarkActivity.class);
		    startActivity(intent);
		}
		return true;
	}
	
	private Runnable disableSetBackRunnable = new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.e(TAG, "disableSetBackRunnable");
			setBack = false;
		}
	};
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			
			if(msg.what == CamlogCmdChannel.RECEIVE_MSG_FROM_GLASS) {
				
				Packet data = (Packet)msg.obj;
				int type = data.getInt("type");
				String sValue = null;
				boolean bValue = false;
				
				switch (type) {
				
					case SET_VEDIO_DURATION:
						setBack = true;
						sValue = data.getString(lables[type -1]);
						mVedioDurationPreference.setValue(sValue);
						break;
//					case SET_VOLUME:
//						int volume = data.getInt(lables[type -1]);
//						mVolumeSeekBarPreference.setValue(volume);
//						break;
					case SWITCH_ROUND_VIDEO:
						setBack = true;
						bValue = data.getBoolean(lables[type -1]);
						mRoundVideoPreference.setChecked(bValue);
						break;
				        case WIFI_CONNECT_STATE_CHANGED:
					    String connected_ssid = data.getString("connected_ssid");
					    Log.i(TAG,"wifi connect state---connected_ssid = "+connected_ssid);
					    if (connected_ssid.equals("")) {
						    // wifi is disconnected now
						    mWifiSyncPreference.setSummary(R.string.wifi_is_closed);
					    } else {
						    // connect ssid ok
						    handler.removeMessages(CONNECT_WIFI_TIMEOUT);
						    String sync_info = connected_ssid + 
							    getString(R.string.wifi_sync_success);
						    mWifiSyncPreference.setSummary(sync_info);
						    if (mWifiSyncProgressDialog.isShowing()) {
							    mWifiSyncProgressDialog.dismiss();
						    }
					    }
					    break;
				           case SET_LIVE_AUDIO:
						   setBack = true;
						   boolean audio = data.getBoolean("audio");
						   mLiveAudioPreference.setChecked(audio);
						   break;
				           case SET_VOICE_RECOG:
						   setBack = true;
						   boolean voice_recog_enabled = data.getBoolean("voice_recog");
						   mVoiceRecogPreference.setChecked(voice_recog_enabled);
						   break;
				           case GET_WIFI_CONNECT_STATE:
						   handler.removeMessages(GET_WIFI_CONNECT_STATE_TIMEOUT);
						   String ssid = data.getString("connected_ssid");
						   Log.i(TAG,"getWifiConnectState ok---ssid="+ssid);
						   if (ssid.equals("")) {
							   // disconnected state
							   mWifiSyncPreference.setSummary(R.string.wifi_is_closed);
						   } else {
							   // connected state
							   String sync_info = ssid + getString(R.string.wifi_sync_success);
							   mWifiSyncPreference.setSummary(sync_info);
						   }
						   break;
				             case CamlogCmdChannel.TYPE_SET_LIVERECORD:
						 setBack = true;
						 boolean liveRecord = data.getBoolean("live_record");
						 mLiveRecordPreference.setChecked(liveRecord);
						 break;
				           default:
							break;
				}
			}
			else if(msg.what == PHONE_AUDIO_CONNECT) {
				setBack = true;
				Log.e(TAG, "phone_on1");
				mBluetoothPhonePreference.setChecked(true);
				if(mProgressDialog.isShowing())
					mProgressDialog.dismiss();
				
				Editor editorOn = mHeadsetPreferences.edit();
				editorOn.putBoolean("last_headset_state", true);
				editorOn.commit();
			}
			else if(msg.what == PHONE_AUDIO_DISCONNECT) {
				setBack = true;
				Log.e(TAG, "phone_off1");
				mBluetoothPhonePreference.setChecked(false);
				if(mProgressDialog.isShowing())
					mProgressDialog.dismiss();
				if (mCamlogCmdChannel.isConnected()){
				    Editor editorOff = mHeadsetPreferences.edit();
				    editorOff.putBoolean("last_headset_state", false);
				    editorOff.commit();
				}
			}
			else if(msg.what == PHONE_A2DP_CONNECT) {
				setBack = true;
				Log.i(TAG, "a2dp on");
				mBluetoothA2dpPreference.setChecked(true);
				if(mProgressDialog.isShowing())
					mProgressDialog.dismiss();
				
				Editor editorOn = mHeadsetPreferences.edit();
				editorOn.putBoolean("last_a2dp_state", true);
				editorOn.commit();
			}
			else if(msg.what == PHONE_A2DP_DISCONNECT) {
				setBack = true;
				Log.i(TAG, "a2dp off");
				mBluetoothA2dpPreference.setChecked(false);
				if(mProgressDialog.isShowing())
					mProgressDialog.dismiss();
				if (mCamlogCmdChannel.isConnected()){
				    Editor editorOff = mHeadsetPreferences.edit();
				    editorOff.putBoolean("last_a2dp_state", false);
				    editorOff.commit();
				}
			}
			else if(msg.what == MSG_SEND_FINISH) {
				setBack = true;
				mSyncContactPreference.setChecked(syncContactToGlass);

				Editor contactEditor = mHeadsetPreferences.edit();
				if(syncContactToGlass){
					contactEditor.putString("last_sync_contact_state", "open");	
				}else{
					contactEditor.putString("last_sync_contact_state", "off");
				}
				contactEditor.commit();
			}
			else if(msg.what == CONTACT_READABLE) {
				String checkState = mHeadsetPreferences.getString("last_sync_contact_state", "other");
				if("open".equals(checkState)){
					setBack = true;
					mSyncContactPreference.setChecked(true);
				}else if("off".equals(checkState)){
					setBack = true;				
					mSyncContactPreference.setChecked(false);
				}else{
					setBack = true;				
					mSyncContactPreference.setChecked((Boolean)msg.obj);
				}
			}
			else if(msg.what == CONNECT_WIFI_TIMEOUT) {
			    Log.i(TAG,"wifi sync timeout---");
			    String ssid = (String)msg.obj;
			    String sync_info = ssid + getString(R.string.wifi_sync_fail);
			    mWifiSyncPreference.setSummary(sync_info);
			    if (mWifiSyncProgressDialog.isShowing()) {
				    mWifiSyncProgressDialog.dismiss();
			    }
			    return;
			} else if (msg.what == GET_WIFI_CONNECT_STATE_TIMEOUT) {
			    Log.i(TAG,"getWifiConnectState timeout---");
			    mWifiSyncPreference.setSummary(R.string.get_wifi_state_timeout);	
			    return;
			}
			handler.postDelayed(disableSetBackRunnable, 200);
		}
	};
	private BroadcastReceiver mBroadcastReceiver =  new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			Log.e(TAG, intent.getAction());
			if (intent.getAction().equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
				int state=intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTED);
				Log.e(TAG, "state:" + state);
				if(state == BluetoothProfile.STATE_CONNECTED) {
					handler.sendEmptyMessage(PHONE_AUDIO_CONNECT);
				}
				if(state == BluetoothProfile.STATE_DISCONNECTED) {
					handler.sendEmptyMessage(PHONE_AUDIO_DISCONNECT);
				}
			}
			if (intent.getAction().equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
				int state=intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_CONNECTED);
				Log.e(TAG, "a2dp state:" + state);
				if(state == BluetoothProfile.STATE_CONNECTED) {
					handler.sendEmptyMessage(PHONE_A2DP_CONNECT);
				}
				if(state == BluetoothProfile.STATE_DISCONNECTED) {
					handler.sendEmptyMessage(PHONE_A2DP_DISCONNECT);
				}
			}
		}
	};
	
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    if(requestCode==0){
		if (resultCode == Activity.RESULT_OK) {
		    Log.i(TAG,"wifi syncing----");
		    String ssid = data.getStringExtra("sync_ssid");
		    String sync_info = String.format(getString(R.string.wifi_syncing), ssid);
		    mWifiSyncPreference.setSummary(sync_info);
		    mWifiSyncProgressDialog.setMessage(sync_info);
		    mWifiSyncProgressDialog.show();
		    Message msg = handler.obtainMessage(CONNECT_WIFI_TIMEOUT, ssid);
		    handler.sendMessageDelayed(msg, SYNCWIFI_DELAYTIMES);
		} else {
		    getWifiConnectState();	
		}
	    }
	}

	private void syncContactToGlass(boolean on){
		ContactsLiteModule clm = (ContactsLiteModule) ContactsLiteModule.getInstance(getActivity().getApplicationContext());
		clm.sendSyncRequest(on,handler);
		clm.setSyncEnable(false);
		BLContacts blContacts = BLContacts.getInstance(getActivity());
		if (on) {
		        blContacts.syncContacts(false, false);
		} else {
		        blContacts.stopSyncContacts();
		}
	}
	
	private void checkContactReadable() {
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Cursor cursor = null;
				cursor = getActivity().getApplicationContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,null,null,null);
				if (cursor != null && cursor.getCount() != 0) {
					contactReadable = true;
				}
				else {
					contactReadable = false;
				}
				
				handler.obtainMessage(CONTACT_READABLE, contactReadable).sendToTarget();
				cursor.close();
			}
		}).start();
	}

     private void setLanguageForGlass(int languageType){
	 LanguageModule langModule = LanguageModule.getInstance(getActivity());
	 langModule.sendSyncRequest(languageType);
     }

     private void chooseLanguage(String type){			 
	 if (type.equals(getString(R.string.language_zh))){
	     mIsDefaultLanguage = LANGUAGE_ZH;		
	 }else if(type.equals(getString(R.string.language_us))){
	     mIsDefaultLanguage = LANGUAGE_US;
	 }else if(type.equals(getString(R.string.language_fr))){
	     mIsDefaultLanguage = LANGUAGE_FR;
	 }else if(type.equals(getString(R.string.language_ru))){
	     mIsDefaultLanguage = LANGUAGE_RU;
	 }else if(type.equals(getString(R.string.language_de))){
	     mIsDefaultLanguage = LANGUAGE_DE;
	 }else if(type.equals(getString(R.string.language_th))){
	     mIsDefaultLanguage = LANGUAGE_TH;
	 }else if(type.equals(getString(R.string.language_fa))){
	     mIsDefaultLanguage = LANGUAGE_FA;
	 }else if(type.equals(getString(R.string.language_es))){
	     mIsDefaultLanguage = LANGUAGE_ES;
	 }else if(type.equals(getString(R.string.language_pt))){
	     mIsDefaultLanguage = LANGUAGE_PT;
	 }else if(type.equals(getString(R.string.language_ar))){
	     mIsDefaultLanguage = LANGUAGE_AR;
	 }else if(type.equals(getString(R.string.language_it))){
	     mIsDefaultLanguage = LANGUAGE_IT;
	 }
	 mLanguagePreference.setValue(type);
	 setLanguageForGlass(mIsDefaultLanguage);	 
     }

	private void getWifiConnectState() {
		mWifiSyncPreference.setSummary(R.string.get_wifi_state);
		Packet pk = mCamlogCmdChannel.createPacket();
		pk.putInt("type", GET_WIFI_CONNECT_STATE);
		mCamlogCmdChannel.sendPacket(pk);
		Message msg = handler.obtainMessage(GET_WIFI_CONNECT_STATE_TIMEOUT);
		handler.sendMessageDelayed(msg, 5000);
		Log.i(TAG,"getWifiConnectState sendMessageDelayed 5000---");
	}

	public void initProgressDialog() {
		mWifiSyncProgressDialog  = new ProgressDialog(getActivity());
		mWifiSyncProgressDialog.setTitle(getString(R.string.wlan_sync_state_title));
		mWifiSyncProgressDialog.setCancelable(false);
		mWifiSyncProgressDialog.setOnKeyListener(new OnKeyListener() {
				@Override
				public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_BACK) {
						dialog.cancel();
					}
					return false;
				}
			});
	}
}