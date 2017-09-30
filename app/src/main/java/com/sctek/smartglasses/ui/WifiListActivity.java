package com.sctek.smartglasses.ui;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cn.zhongdun110.camlog.R;
import com.fota.iport.MobAgentPolicy;
import com.fota.iport.config.VersionInfo;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.sctek.smartglasses.fragments.SettingFragment;
import com.sctek.smartglasses.utils.CamlogCmdChannel;
import com.sctek.smartglasses.utils.WifiUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import cn.ingenic.glasssync.LogTag;

public class WifiListActivity extends BaseFragmentActivity implements OnItemClickListener{
	protected static final String TAG = "WifiListActivity";
	private final static int MSG_SCAN_WIFI = 1;
	private final static int MSG_CLOSE_WLAN_TIMEOUT = 2;
	private final static int CLOSE_WLAN_TIMEOUT = 5000;
        public static final int TYPE_CONNECT = 1;
        public static final int TYPE_UPDATE = 2;
	private ListView mWifiList;
        private TextView mTvTitle;
	private ArrayList <Map<String,Object>> mWifiListResults;
	private MyAdapter mAdatper;
	private final Timer mTimer = new Timer();  
	private TimerTask mTask;
        private int mSyncWifiType;
	private Button mCloseWLANBt;

	private Handler mHandler = new Handler() {  
	    @Override  
	    public void handleMessage(Message msg) {  
	        super.handleMessage(msg); 
	        switch(msg.what){
	        case MSG_SCAN_WIFI:{
		        ArrayList scanWifi =WifiUtils.scanWifiResult(WifiListActivity.this);
			if(null ==scanWifi || scanWifi.size() == 0){
			    return;
			}
			mTvTitle.setText(getString(R.string.wifi_connect));
	        	mWifiListResults = scanWifi;
	        	mAdatper.notifyDataSetChanged();
	        	break;
		}
		case MSG_CLOSE_WLAN_TIMEOUT:
			Log.i(TAG, "close wlan timeout----");
			break;
		case CamlogCmdChannel.RECEIVE_MSG_FROM_GLASS:
			Log.i(TAG, "RECEIVE_MSG_FROM_GLASS----");
			mHandler.removeMessages(MSG_CLOSE_WLAN_TIMEOUT);
			Packet data = (Packet)msg.obj;
			int type = data.getInt("type");
			if (type == SettingFragment.TURN_WIFI_OFF) {
				Log.i(TAG, "close wlan ok----");
				showWLANClosedDialog();
			}
			break;
	        default:	
	        	break;
	        }
	    }  
	};  
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wifi_scan_list);
		mSyncWifiType = getIntent().getIntExtra("wifi_type",TYPE_CONNECT);
		mTvTitle  = (TextView)findViewById(R.id.wifi_title);
		mWifiList = (ListView)findViewById(R.id.scan_list);
		mCloseWLANBt = (Button)findViewById(R.id.close_wlan_bt);
		mAdatper = new MyAdapter(this);
		mWifiList.setAdapter(mAdatper);
		mWifiList.setOnItemClickListener(this);
		CamlogCmdChannel.getInstance(this).registerHandler("WifiListActivity",mHandler);
		mCloseWLANBt.setOnClickListener(mClickedListener);
		mTask = new TimerTask() {  
		    @Override  
		    public void run() {  
		        mHandler.sendEmptyMessage(MSG_SCAN_WIFI); 
		    }  
		}; 
		mTimer.schedule(mTask, 0, 5000);
		showTipDialog();
	}
	
	private void showWifiConnectDialog(String str,int security_type){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		View view = LayoutInflater.from(this).inflate(R.layout.config_wifi, null);
		final String ssid = str;
		final int securityType = security_type;
		String title = getResources().getString(R.string.wifi_dialog_title);  
		String dialogTitle = String.format(title,ssid);
		final EditText pwEt = (EditText)view.findViewById(R.id.ap_pw_et);
		final SharedPreferences preferences=getSharedPreferences("wifi_password",Context.MODE_PRIVATE);
		if("" != preferences.getString(ssid, ""))
		    pwEt.setText(preferences.getString(ssid, ""));

		builder.setView(view);
		builder.setTitle(dialogTitle);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String pw = pwEt.getText().toString();
				dialog.cancel();
				if(null == pw || pw.length() < 8){
				        Toast.makeText(WifiListActivity.this, R.string.pw_too_short, Toast.LENGTH_SHORT).show();
					return;    
				}
				Editor editor=preferences.edit();
				editor.putString(ssid, pw);
				editor.commit();
				syncWifiInfo(ssid,pw,securityType);
			}
		});		
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});		
		builder.create().show();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG,"onDestroy"); 
		mTask.cancel();
		mHandler.removeMessages(MSG_SCAN_WIFI);
		CamlogCmdChannel.getInstance(WifiListActivity.this).unregisterHandler("WifiListActivity");
	}
	
        @Override 
	public void onBackPressed() { 
	    super.onBackPressed();
		Log.i(TAG,"onBackPressed"); 
	    finish();       
	} 
    
        @Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
						long arg3) {
	    String ssid = mWifiListResults.get(arg2).get("SSID").toString();
	    int security_type = Integer.parseInt(mWifiListResults.get(arg2).get("SECURITY_INT").toString());
	    if(security_type == WifiUtils.SECURITY_NONE){
		syncWifiInfo(ssid,null,security_type);	    
	    }else {
		showWifiConnectDialog(ssid,security_type);
	    }
	}
    
        private void syncWifiInfo(String ssid, String pw, int security_type){
	        CamlogCmdChannel channel = CamlogCmdChannel.getInstance(WifiListActivity.this);
		if(!channel.isConnected()) {
		    Toast.makeText(WifiListActivity.this, R.string.bluetooth_error, Toast.LENGTH_SHORT).show();
		    return;
		}
		final VersionInfo vi = MobAgentPolicy.getVersionInfo();
		Packet pk = channel.createPacket();
		pk.putString("ssid", ssid);
		pk.putString("pw", pw);
		pk.putString("security", getStringSecurity(security_type));
		//pk.putString("security", "WPA2_PSK");
		if(mSyncWifiType == TYPE_UPDATE){
		    pk.putInt("type", CamlogCmdChannel.UPDATE_CONNECT_WIFI_MSG);
		    pk.putString("url", vi.deltaUrl);
		    pk.putString("deltaid", vi.deltaID);
		    pk.putString("md5", vi.md5sum);
		    pk.putInt("size", vi.fileSize);
		    pk.putString("vname", vi.versionName);
		    Log.i(TAG, "url:" + vi.deltaUrl + "deltaid:" + vi.deltaID + "md5:" + vi.md5sum + "size:" + vi.fileSize + "vname:" + vi.versionName);
		}else if(mSyncWifiType == TYPE_CONNECT){
		    pk.putInt("type", CamlogCmdChannel.CONNET_WIFI_MSG);
		    Intent intent = new Intent();
		    intent.putExtra("sync_ssid", ssid);
		    setResult(RESULT_OK, intent);
		}
		channel.sendPacket(pk);
		finish();
        }

	public class MyAdapter extends BaseAdapter{
		LayoutInflater inflater;
		public MyAdapter(Context context){
			this.inflater=LayoutInflater.from(context);
		}
		@Override
		public int getCount() {
		    if(null == mWifiListResults){
			return 0;
		    }
			return mWifiListResults.size();
		}
		@Override
		public Object getItem(int position) {
			return position;
		}
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;
		    final int mPosition = position ;
		    if(convertView == null){
		    	holder=new ViewHolder();
		    	convertView = inflater.inflate(R.layout.wifi_scan_item, null);
		    	holder.wifiNameTV = (TextView)convertView.findViewById(R.id.wifiName);
		    	holder.wifiSecurityTV = (TextView)convertView.findViewById(R.id.wifiSecurity);
		    	convertView.setTag(holder);
		    }else {
		    	holder = (ViewHolder) convertView.getTag();
		    }
		    holder.wifiNameTV.setText(mWifiListResults.get(position).get("SSID").toString());
			holder.wifiSecurityTV.setText(mWifiListResults.get(position).get("SECURITY_STRING").toString());
			 return convertView;
		}
	}
		
        class ViewHolder {
	        TextView wifiNameTV,wifiSecurityTV;
	}

        private static String getStringSecurity(int security){
	    if(security == WifiUtils.SECURITY_NONE){
		return "NONE";
	    }else if (security == WifiUtils.SECURITY_WEP){
		return "WPA_EAP"; 
	    }else {
		return "WPA_PSK";
	    }
	}

	private OnClickListener mClickedListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.close_wlan_bt:
				requestCloseWLAN();
				break;    
			default:
				break;
			}
		}
	};

	private void requestCloseWLAN() {
	        CamlogCmdChannel channel = CamlogCmdChannel.getInstance(WifiListActivity.this);
		if(!channel.isConnected()) {
			Toast.makeText(WifiListActivity.this, R.string.bluetooth_error, Toast.LENGTH_SHORT).show();
			return;
		}
		Log.i(TAG, "request close wlan----");
		Packet pk = channel.createPacket();
		pk.putInt("type", SettingFragment.TURN_WIFI_OFF);
		channel.sendPacket(pk);
		Message msg = mHandler.obtainMessage(MSG_CLOSE_WLAN_TIMEOUT);
		mHandler.sendMessageDelayed(msg, CLOSE_WLAN_TIMEOUT);
        }

	public void showTipDialog() {
		AlertDialog.Builder builder = new Builder(this);
		builder.setTitle(R.string.wlan_dialog_tip_title);
		builder.setMessage(R.string.wlan_dialog_tip);
		builder.setPositiveButton(R.string.wifi_dialog_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if(keyCode == KeyEvent.KEYCODE_BACK) {
					dialog.cancel();
				}
				return false;
			}
		});
		builder.create().show();
	}

	public void showWLANClosedDialog() {
		Log.i(TAG, "showWLANClosedDialog----");
		AlertDialog.Builder builder = new Builder(this);
		builder.setTitle(R.string.wifi_is_closed);
		builder.setPositiveButton(R.string.wifi_dialog_ok, new DialogInterface.OnClickListener() {
				@Override
					public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
		builder.setOnKeyListener(new OnKeyListener() {
				@Override
					public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
					if(keyCode == KeyEvent.KEYCODE_BACK) {
						dialog.cancel();
					}
					return false;
				}
			});
		builder.create().show();
	}
}