package com.cn.zhongdun110.camlog;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothA2dp;
import android.util.Log;

import cn.ingenic.glasssync.DefaultSyncManager;

import com.sctek.smartglasses.biz.BLContacts;
import com.sctek.smartglasses.db.ContactsDBHelper;
import com.sctek.smartglasses.ui.BindCamlogActivity;
import com.sctek.smartglasses.ui.MainActivity;

import cn.ingenic.glasssync.devicemanager.GlassDetect;

public class DeviceReceiver extends DeviceAdminReceiver {
    private static final String TAG = "DeviceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equals(DefaultSyncManager.RECEIVER_REQUST_UNBIND)) {
            unBond(context);
        } else if (intent.getAction().equals(DefaultSyncManager.RECEIVER_ACTION_STATE_CHANGE)) {
            int state = intent.getIntExtra(DefaultSyncManager.EXTRA_STATE, DefaultSyncManager.IDLE);
            Log.w(TAG, "recever:state change state=" + state);
            if (state == DefaultSyncManager.CONNECTED)
                updateBluetoothHeadSet(context);
        } else if (intent.getAction().equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
            int connectState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
            DefaultSyncManager manager = DefaultSyncManager.getDefault();
            if (connectState == BluetoothProfile.STATE_CONNECTED &&
                    manager.getLockedAddress().equals("")) {
                GlassDetect glassDetect = (GlassDetect) GlassDetect.getInstance(context);
                glassDetect.set_audio_disconnect();
            }
        } else if (intent.getAction().equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
            int connectState = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1);
            DefaultSyncManager manager = DefaultSyncManager.getDefault();
            if (connectState == BluetoothProfile.STATE_CONNECTED &&
                    manager.getLockedAddress().equals("")) {
                GlassDetect glassDetect = (GlassDetect) GlassDetect.getInstance(context);
                glassDetect.set_a2dp_disconnect();
            }
        }
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        setAdminEnable(context, true);
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        setAdminEnable(context, false);
    }

    private void setAdminEnable(Context context, boolean enable) {
        SharedPreferences pref = context.getSharedPreferences("device_manager", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("lock_screen", enable);
        editor.commit();
    }

    private void unBond(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                BLContacts.getInstance(context).stopSyncContacts();
                ContactsDBHelper.getInstance(context, null).clearAllData();

                DefaultSyncManager manager = DefaultSyncManager.getDefault();
                manager.setLockedAddress("", false);
                Log.i(TAG, "unbind setlockeraddress ok");
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }

                GlassDetect glassDetect = (GlassDetect) GlassDetect.getInstance(context);
                glassDetect.set_audio_disconnect();
                glassDetect.set_a2dp_disconnect();
                manager.disconnect();

                if (MainActivity.getInstance() != null) {
                    Intent intent = new Intent(MainActivity.getInstance(), BindCamlogActivity.class);
                    MainActivity.getInstance().startActivity(intent);
                    MainActivity.getInstance().finish();
                }
                Log.i(TAG, "unBond out");
            }
        }).start();
    }

    private void updateBluetoothHeadSet(Context context) {
        SharedPreferences pref = context.getSharedPreferences(SyncApp.SHARED_FILE_NAME, Context.MODE_PRIVATE);
        Log.i(TAG, "get last_headset_state=" + pref.getBoolean("last_headset_state", false));
        Log.i(TAG, "get last_a2dp_state=" + pref.getBoolean("last_a2dp_state", false));
        if (pref.getBoolean("last_headset_state", false)) {
            GlassDetect gd = (GlassDetect) GlassDetect.getInstance(context);
            if (gd.getCurrentHeadSetState() == BluetoothProfile.STATE_DISCONNECTED) {
                gd.set_audio_connect();
            }
        }
        if (pref.getBoolean("last_a2dp_state", false)) {
            GlassDetect gd = (GlassDetect) GlassDetect.getInstance(context);
            if (gd.getCurrentA2dpState() == BluetoothProfile.STATE_DISCONNECTED) {
                gd.set_a2dp_connect();
            }
        }
    }

}
