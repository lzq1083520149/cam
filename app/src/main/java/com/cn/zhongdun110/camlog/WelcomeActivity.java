package com.cn.zhongdun110.camlog;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.baidu.android.bba.common.util.Util;
import com.sctek.smartglasses.ui.BindCamlogActivity;
import com.sctek.smartglasses.ui.MainActivity;
import com.sctek.smartglasses.utils.Utils;

import cn.ingenic.glasssync.DefaultSyncManager;

public class WelcomeActivity extends Activity {
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1122;
    private BluetoothAdapter mAdapter;
    //    private boolean mFirst = true;
    private final String TAG = "WelcomeActivity";

    //自定义的打开 Bluetooth 的请求码，与 onActivityResult 中返回的 requestCode 匹配。
    private static final int REQUEST_CODE_BLUETOOTH_ON = 1313;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.welcome_activity);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothReceiver, filter);
        mAdapter = BluetoothAdapter.getDefaultAdapter();
    }


    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    startActivity();
                }
            }
        }
    };

     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_CANCELED) {
            if (mAdapter.isEnabled()) {
                startActivity();
            }else {
                mAdapter.enable();
            }
        }
    }


    private void startActivity() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

//                SharedPreferences sharedPreferences = getSharedPreferences("start", MODE_PRIVATE);
//                Boolean b = sharedPreferences.getBoolean("first_start", true);
//                if (b) {
//                    Intent intent = new Intent(WelcomeActivity.this, GuideActivity.cl ass);
//                    startActivity(intent);
//                    finish();
//                } else {
                DefaultSyncManager mManager = DefaultSyncManager.getDefault();
                if (!mManager.getLockedAddress().equals("")) {
                    Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(WelcomeActivity.this, BindCamlogActivity.class);
                    startActivity(intent);
                    finish();
                }
//                }
            }
        }, 1500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        unregisterReceiver(mBluetoothReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //checkBluetoothPermission();
        if (mAdapter.isEnabled()) {
            startActivity();
        }else {
            // 请求打开 Bluetooth
//            Intent requestBluetoothOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(requestBluetoothOn, REQUEST_CODE_BLUETOOTH_ON);
            mAdapter.enable();
        }

    }


    /*
       校验蓝牙权限
      */
    private void checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            //校验是否已具有模糊定位权限
            if (ContextCompat.checkSelfPermission(WelcomeActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(WelcomeActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.BLUETOOTH
                                , Manifest.permission.BLUETOOTH_ADMIN},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            } else {
                //具有权限
                startActivity();
            }
        } else {
            //系统不高于6.0直接执行
            startActivity();
        }
    }
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        doNext(requestCode, grantResults);
//    }


//    private void doNext(int requestCode, int[] grantResults) {
//        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION) {
//            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                //同意权限
//                startActivity();
//            } else {
//                // 权限拒绝
//                // 下面的方法最好写一个跳转，可以直接跳转到权限设置页面，方便用户
//                denyPermission();
//            }
//        }
//    }

    private void denyPermission() {
        Toast.makeText(this, "请将蓝牙打开", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intent);
    }


}
