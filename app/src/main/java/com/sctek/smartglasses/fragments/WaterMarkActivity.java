package com.sctek.smartglasses.fragments;

import android.util.Log;
import android.widget.Toast;
import android.view.KeyEvent;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.CheckBoxPreference ;
import android.preference.PreferenceActivity;


import com.cn.zhongdun110.camlog.R;
import com.sctek.smartglasses.utils.CamlogCmdChannel;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
public class WaterMarkActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    private final String TAG = "WaterMarkActivity";
    private CheckBoxPreference  mPicturePreference,mVideoPreference;
    public final int PIC_WATER_MARK = 23;
    public final int VIDEO_WATER_MARK = 24;
    private final int MSG_SET_TIME_OUT = 1;
    private CamlogCmdChannel mCamlogCmdChannel;
    private boolean mSetBack;
    private Handler mHandler = new Handler(){
	@Override
	public void handleMessage(Message msg) {
	    switch(msg.what){
	    case CamlogCmdChannel.RECEIVE_MSG_FROM_GLASS:
		Packet data = (Packet)msg.obj;
		int type = data.getInt("type");
		Log.i(TAG,"data = " + data + "type = " + type);
		switch (type){
		case PIC_WATER_MARK:
		    mSetBack = true;
		    boolean pic_enable = data.getBoolean("enable");
		    Log.i(TAG,"pic_enable = " + pic_enable);
		    mPicturePreference.setChecked(pic_enable);
		    break;
		case VIDEO_WATER_MARK:
		    mSetBack = true;
		    boolean video_enable = data.getBoolean("enable");
		    mVideoPreference.setChecked(video_enable);
		    break;   
		}
		break;
	    case MSG_SET_TIME_OUT:
		mSetBack = false;
		break;
	    }
	    
	    mHandler.sendEmptyMessageDelayed(MSG_SET_TIME_OUT, 200);
				   
	}
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	addPreferencesFromResource(R.xml.water_mark);
	mPicturePreference = (CheckBoxPreference)findPreference("picture");
	mVideoPreference = (CheckBoxPreference)findPreference("video");
	mPicturePreference.setOnPreferenceChangeListener(this);
	mVideoPreference.setOnPreferenceChangeListener(this);
	mCamlogCmdChannel = CamlogCmdChannel.getInstance(this);
	mCamlogCmdChannel.registerHandler("WaterMarkActivity",mHandler);
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
	
	if(mSetBack) {
	    Log.e(TAG, "set back true");
	    mSetBack = false;
	    mHandler.removeMessages(MSG_SET_TIME_OUT);
	    return true;
	}

	if(mCamlogCmdChannel.isConnected()) {
	    String key = preference.getKey();
	    Packet pk = mCamlogCmdChannel.createPacket();
	    if("picture".equals(key)) {
		boolean enable = (Boolean)newValue;
		Log.i(TAG,"enable = " + enable);
		pk.putInt("type", PIC_WATER_MARK);
		pk.putBoolean("enable", enable);		
	    }
	    else if("video".equals(key)) {
		boolean enable = (Boolean)newValue;
		pk.putInt("type", VIDEO_WATER_MARK);
		pk.putBoolean("enable", enable);
	    }
	    mCamlogCmdChannel.sendPacket(pk);
	    return false;
	}else {
	    Toast.makeText(this, R.string.bluetooth_error, Toast.LENGTH_LONG).show();
	    return false;
	}
	
    }

    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	finish();
	return true;
    }

    public void onDestroy(){
	super.onDestroy();
	mCamlogCmdChannel.unregisterHandler("WaterMarkActivity");
    }
}
