package com.sctek.smartglasses.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.cn.zhongdun110.camlog.SyncApp;
import com.fota.iport.ICheckVersionCallback;
import com.fota.iport.MobAgentPolicy;
import com.fota.iport.config.MobileParamInfo;
import com.ingenic.glass.api.sync.SyncChannel;
import com.ingenic.glass.api.sync.SyncChannel.CONNECTION_STATE;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.ingenic.glass.api.sync.SyncChannel.RESULT;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cn.ingenic.glasssync.devicemanager.GlassDetect;

public class CamlogCmdChannel {

	private static final String TAG = "CamlogCmdChannel";

	private static final String CMD_CHANNEL_NAME = "cmdchannel";

	private static final String CAMLOG_FOTA_TOKE = "fb5c379aeed5277fdf4b89c797af1bcd";

	private SyncChannel mChannel;

	private static CamlogCmdChannel instance;
	private HashMap<String, Handler> mHashMapHandler = new HashMap<String, Handler>();
	public static final int CHECK_UPDATE_ERROR = 99;
	public static final int CHECK_UPDATE_SUCCESS = 100;
	public static final int RECEIVE_MSG_FROM_GLASS = 101;

	public final static int CONNET_WIFI_MSG = 1;
	public final static int CHECK_UPDATE_GLASS = 17;
	public final static int TURN_WIFI_OFF = 18;
	public final static int UPDATE_CONNECT_WIFI_MSG =20;
	public final static int SET_DEFAULT_INFO = 26;
	public final static int GET_WIFI_CONNECT_STATE = 28;
	public final static int RECEIVE_A2DP_STATE = 29;
	public final static int SET_REMOTELIVE_PASSWORD = 30;
	public static final int TYPE_SYNC_TIME = 31;
	public static final int TYPE_SET_LIVERECORD = 33;
	public static final int TENCENT_LIVE = 34;
	public static final String GLASS_A2DP_STATE = "glass_a2dp_state";
	public static final String KEY_REMOTELIVE_UID = "remotelive_uid";
	public static final String REMOTE_LIVE_VERIFY_PASSWORD = "remotelive_result";
	private Context mContext;
	private WifiApStateBroadcastReceiver nReceiver;
	private CamlogCmdChannel(Context context) {

		mChannel = SyncChannel.create(CMD_CHANNEL_NAME, context, mOnSyncListener);
		mContext = context;

		nReceiver = new WifiApStateBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(WIFI_AP_STATE_CHANGED_ACTION);
		context.registerReceiver(nReceiver,filter);
	}

	protected void finalize() {
		mContext.unregisterReceiver(nReceiver);
	}

	public static CamlogCmdChannel getInstance(Context context	) {
		if(instance == null)
			instance = new CamlogCmdChannel(context);
		return instance;
	}

	public Packet createPacket() {
		return mChannel.createPacket();
	}

	public void sendPacket(Packet pk) {
		mChannel.sendPacket(pk);
	}

	public boolean isConnected() {
		return mChannel.isConnected();
	}

	public void sendInt (String key, int value) {
		Packet packet = mChannel.createPacket();
		packet.putInt(key, value);
		mChannel.sendPacket(packet);
	}

	public void sendBoolean (String key, boolean value) {
		Packet packet = mChannel.createPacket();
		packet.putBoolean(key, value);
		mChannel.sendPacket(packet);
	}

	public void sendString (String key, String value) {
		Packet packet = mChannel.createPacket();
		packet.putString(key, value);
		mChannel.sendPacket(packet);
	}

	public void sendFloat (String key, float value) {
		Packet packet = mChannel.createPacket();
		packet.putFloat(key, value);
		mChannel.sendPacket(packet);
	}

	public void registerHandler(String key,Handler handler){
		mHashMapHandler.put(key, handler);
	}
	public void unregisterHandler(String key){
		mHashMapHandler.remove(key);
	}

	public void sendSyncTime() {
		String time = System.currentTimeMillis() + "";
		String timezoneId = java.util.TimeZone.getDefault().getID();
		Packet packet = mChannel.createPacket();
		packet.putInt("type", TYPE_SYNC_TIME);
		packet.putString("time", time + "," + timezoneId);
		sendPacket(packet);
	}

	private MyOnSyncListener mOnSyncListener = new MyOnSyncListener();
	private class MyOnSyncListener implements SyncChannel.onChannelListener {
		@Override
		public void onServiceConnected() {
			Log.i(TAG, "onServiceConnected ");
		}

		@Override
		public void onReceive(RESULT arg0, Packet data) {
			// TODO Auto-generated method stub
			int type = data.getInt("type");
			Log.i(TAG, "Channel onReceive type="+type);
			if(SET_DEFAULT_INFO == type) {
				saveGlassInfo(data);
			} else if(CHECK_UPDATE_GLASS == type){
				checkDeviceVersion(data);
				saveGlassInfo(data);
			} else if(RECEIVE_A2DP_STATE == type){
				boolean a2dpState = data.getBoolean(GLASS_A2DP_STATE);
				setA2DPState(a2dpState);
				if(a2dpState == true){
					GlassDetect.getInstance(mContext).set_a2dp_connect();
				}
			}else if (SET_REMOTELIVE_PASSWORD == type){
				sendMsgToUser(SET_REMOTELIVE_PASSWORD,0,data);
			}else {
				Log.i(TAG,"---onReceive RECEIVE_MSG_FROM_GLASS---");
				sendMsgToUser(RECEIVE_MSG_FROM_GLASS,0,data);
			}
		}

		@Override
		public void onSendCompleted(RESULT result, Packet arg1) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onSendCompleted:" + result.name());
		}

		@Override
		public void onStateChanged(CONNECTION_STATE arg0) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onStateChanged:" + arg0.name());
		}
	}

	private void checkDeviceVersion(Packet pk) {
		MobAgentPolicy.initConfig(mContext.getApplicationContext());
		Log.e(TAG, "checkDeviceVersion");
		//检测版本所需参数
		final MobileParamInfo mobileParamInfo = new MobileParamInfo();
		mobileParamInfo.mid = pk.getString("serial");
		mobileParamInfo.version = pk.getString("version");
		mobileParamInfo.oem = pk.getString("oem");
		mobileParamInfo.models = pk.getString("models");
		mobileParamInfo.token = pk.getString("token").equals("") ? CAMLOG_FOTA_TOKE : pk.getString("token");
		mobileParamInfo.platform = pk.getString("platform");
		mobileParamInfo.deviceType = pk.getString("deviceType");
		Log.e(TAG, "mid:" + mobileParamInfo.mid + " version" + mobileParamInfo.version +
				" oem:" + mobileParamInfo.oem + " models" + mobileParamInfo.models +
				" token:" + mobileParamInfo.token + " platform:" + mobileParamInfo.platform +
				" deviceType:" + mobileParamInfo.deviceType);

		if (!isValidToDownload(mobileParamInfo)) {
			return;
		}

		//检测版本
		MobAgentPolicy.checkVersion(mContext, mobileParamInfo, new ICheckVersionCallback() {
			@Override
			public void onCheckSuccess(int status) {
				Log.e(TAG, "==================status" + status);
				sendMsgToUser(CHECK_UPDATE_SUCCESS,0,null);
			}

			@Override
			public void onCheckFail(final int status, final String errorMsg) {
				Log.e(TAG, "status="+status+"errormsg"+errorMsg);
				sendMsgToUser(CHECK_UPDATE_ERROR,status,null);
			}

			@Override
			public void onInvalidDate() {
				Log.e(TAG, "Remote respond invalid message");
				sendMsgToUser(CHECK_UPDATE_ERROR,0,null);
			}
		});
	}

	private boolean isValidToDownload(MobileParamInfo mobileParamInfo) {
		if (TextUtils.isEmpty(mobileParamInfo.mid)
				|| TextUtils.isEmpty(mobileParamInfo.version)
				|| TextUtils.isEmpty(mobileParamInfo.oem)
				|| TextUtils.isEmpty(mobileParamInfo.models)
				|| TextUtils.isEmpty(mobileParamInfo.token)
				|| TextUtils.isEmpty(mobileParamInfo.platform)
				|| TextUtils.isEmpty(mobileParamInfo.deviceType)
				) {
			return false;
		}
		return true;
	}

	private void saveGlassInfo(Packet data){
		String model = data.getString("model");
		String cpu = data.getString("cpu");
		String version = data.getString("version");
		String serial = data.getString("serial");
		int volume = data.getInt("volume");
		boolean round = data.getBoolean("round");
		String duration = data.getString("duration");
		boolean picture = data.getBoolean("picture");
		boolean video = data.getBoolean("video");
		boolean audio = data.getBoolean("audio");
		boolean voice_recog_enabled = data.getBoolean("voice_recog");
		Log.e(TAG, "volume:" + volume + " round:" + round + "duration:" + duration +
				"serial:" + serial + " picture = " + picture + " video = " + video);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		Editor editor = preferences.edit();
		editor.putString("model", model);
		editor.putString("cpu", cpu);
		editor.putString("version", version);
		editor.putString("serial", serial);
		editor.putInt("volume", volume);
		editor.putBoolean("round_video", round);
		editor.putString("duration", duration);
		editor.putBoolean("picture",picture);
		editor.putBoolean("video",video);
		editor.putBoolean("live_audio",audio);
		editor.putBoolean("voice_recog",voice_recog_enabled);
		editor.commit();
	}

	public static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";

	class WifiApStateBroadcastReceiver extends BroadcastReceiver {

		private static final String TAG = "WifiApStateBroadcastReceiver";

		public static final String EXTRA_WIFI_AP_STATE = "wifi_state";
		public static final String EXTRA_PREVIOUS_WIFI_AP_STATE = "previous_wifi_state";

		protected static final int WIFI_AP_STATE_DISABLED = 11;
		protected static final int WIFI_AP_STATE_ENABLED = 13;

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (WIFI_AP_STATE_CHANGED_ACTION.equals(intent.getAction())) {
				int cstate = intent.getIntExtra(EXTRA_WIFI_AP_STATE, -1);
				int pstate = intent.getIntExtra(EXTRA_PREVIOUS_WIFI_AP_STATE, -1);
				Log.e(TAG, WIFI_AP_STATE_CHANGED_ACTION + ", current state:" + cstate + ",previous state:" + pstate);

				if (cstate == WIFI_AP_STATE_DISABLED) {
					// don't need send TURN_WIFI_OFF msg, WifiAdmin auto close wifi when wifi has been disconnected for 3 minutes.
					// CamlogCmdChannel channel = CamlogCmdChannel.getInstance(context);
					// Packet packet = channel.createPacket();
					// packet.putInt("type", CamlogCmdChannel.TURN_WIFI_OFF);
					// channel.sendPacket(packet);
				} else if(cstate == WIFI_AP_STATE_ENABLED){
					String ssid = WifiUtils.getValidSsid(context);
					String pw = WifiUtils.getValidPassword(context);
					String security = WifiUtils.getValidSecurity(context);

					CamlogCmdChannel channel = CamlogCmdChannel.getInstance(context);
					Packet packet = channel.createPacket();
					packet.putInt("type", CamlogCmdChannel.CONNET_WIFI_MSG);
					packet.putString("ssid", ssid);
					packet.putString("pw", pw);
					packet.putString("security", security);
					//channel.sendPacket(packet);
				}
			}
		}
	}

	private void setA2DPState(boolean state){
		SharedPreferences headsetPreferences = mContext.
				getSharedPreferences(SyncApp.SHARED_FILE_NAME, Context.MODE_PRIVATE);
		Editor editorOn = headsetPreferences.edit();
		editorOn.putBoolean("last_a2dp_state", state);
		editorOn.commit();
	}

	private void sendMsgToUser(int what,int arg1, Object obj) {
		Iterator iterator = mHashMapHandler.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry entry = (Map.Entry) iterator.next();
			Handler handler = (Handler) entry.getValue();
			Message msg = handler.obtainMessage();
			msg.what = what;
			msg.arg1 = arg1;
			msg.obj = obj;
			handler.sendMessage(msg);
		}
	}
}
