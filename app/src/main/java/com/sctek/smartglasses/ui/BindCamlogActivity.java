package com.sctek.smartglasses.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.cn.zhongdun110.camlog.R;
import com.cn.zhongdun110.camlog.SyncApp;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.sctek.smartglasses.language.LanguageModule;
import com.sctek.smartglasses.utils.CamlogCmdChannel;
import com.sctek.smartglasses.utils.RadarScanView;
import com.sctek.smartglasses.utils.RandomTextView;
import com.sctek.smartglasses.zxing.CaptureActivity;

import java.lang.reflect.Method;
import java.util.List;

import cn.ingenic.glasssync.DefaultSyncManager;


public class BindCamlogActivity extends BaseFragmentActivity {
    private final String TAG = "BindGlassActivity";
    public final static int BIND_TIMEOUT = 2;
    public final static int REQUEST_CONNECT = 3;
    public final static int REQUEST_PAIR = 4;
    public final static int REQUEST_SCAN_DEVICE = 5;

    public final static int REQUEST_CANCEL_SCAN_DEVICE = 6;
    public final static int BT_BOND_FAILED = 7;
    public final static int BIND_FAIL = 8;

    private int BIND_TIMEOUT_DELAY = 20 * 1000;
    private final static int CANCEL_SCAN_DELAY_TIME = 15 * 1000;

    private static final boolean DEBUG = true;
    private DefaultSyncManager mManager;
    private BluetoothAdapter mAdapter;
    private static final String CAMLOG_BT_NAME = "CAMLOG";
    //      private static final String CAMLOG_BT_NAME = "CLIQUE";
    private Context mContext;


    private Button mBindHanLangBt;
    private Button mScanQrCodeBt;
    private TextView mBindHintTv;
    private RadarScanView bind_radarScanView;
    private RandomTextView randomTextView;
    private String deviceName;
    private final static int ACCESS_BLUETOOTH = 233;
    private BluetoothGatt mGatt;

    private ScanCallback mScanCallback;
    private BluetoothLeScanner leScanner;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // mHandler.removeMessages(BIND_TIMEOUT);
            switch (msg.what) {
                // case BIND_TIMEOUT:
                // 	 bindHanLangFail(R.string.operation_timeout);
                //     break;
                case REQUEST_CONNECT:
                    mAdapter.cancelDiscovery();

                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    Log.i(TAG, "--REQUEST_CONNECT bond state=" + device.getBondState());
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        try {
                            mBindHintTv.setText(R.string.binding_camlog);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                leScanner.stopScan(mScanCallback);
                                connectToDevice(device);
                            }
                            mManager.connect(device.getAddress());

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Message msgB = mHandler.obtainMessage();
                        msgB.what = BIND_FAIL;
                        mHandler.sendMessageDelayed(msgB, BIND_TIMEOUT_DELAY);

                    } else {
                        mBindHintTv.setText(R.string.bluetooth_paring);
                        try {
                            Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                            createBondMethod.invoke(device);

//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
////                                connectToDevice(device);
//                                mManager.connect(device.getAddress());
//                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                leScanner.stopScan(mScanCallback);
                                connectToDevice(device);
                            }

                            // mPairingDevice = device;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Message msgB = mHandler.obtainMessage();
                        msgB.what = BT_BOND_FAILED;
                        mHandler.sendMessageDelayed(msgB, BIND_TIMEOUT_DELAY);
                    }
                    break;
                case REQUEST_CANCEL_SCAN_DEVICE:
                    mAdapter.cancelDiscovery();
                    bindHanLangFail(R.string.scan_device_timeout);
                    break;
                case BT_BOND_FAILED:
                    bindHanLangFail(R.string.pair_fail);
                    restartBluetooth();
                    break;
                case BIND_FAIL:
                    bindHanLangFail(R.string.bind_fail);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_bind_camlog);
        mContext = this;

        mBindHanLangBt = (Button) findViewById(R.id.bind_camlog_bt);
        mScanQrCodeBt = (Button) findViewById(R.id.scan_qrcode_bt);
        mBindHintTv = (TextView) findViewById(R.id.bind_camlog_hint_tv);
        bind_radarScanView = (RadarScanView) findViewById(R.id.bind_radarScanView);

        randomTextView = (RandomTextView) findViewById(R.id.bind_randomTextView);

        mManager = DefaultSyncManager.getDefault();
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        Button remoteCameraLive = (Button) findViewById(R.id.remote_live);
        if (SyncApp.REMOTE_CAMERA_LIVE) {
            remoteCameraLive.setVisibility(View.VISIBLE);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!mAdapter.isEnabled())
            mAdapter.enable();
    }

    public void onBindHanLangButtonClicked(View view) {
        if (view != null)
            deviceName = null;
        mHandler.sendEmptyMessageDelayed(REQUEST_CANCEL_SCAN_DEVICE, CANCEL_SCAN_DELAY_TIME);
        mBindHanLangBt.setEnabled(false);
        mScanQrCodeBt.setEnabled(false);
        mBindHintTv.setText(R.string.serching_camlog);
        bind_radarScanView.startAnimation();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startBuletooth();
        } else {
            mAdapter.startDiscovery();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startBuletooth() {
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice btDevice = result.getDevice();
                final String name = btDevice.getName();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        randomTextView.addKeyWord(name);
                        randomTextView.show();
                    }
                }, 0);

                if (name != null && name.toUpperCase().startsWith(CAMLOG_BT_NAME)) {
                    leScanner.stopScan(mScanCallback);
                    connectToDevice(btDevice);
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
            }

            @Override
            public void onScanFailed(int errorCode) {
            }
        };

        if (!mAdapter.isEnabled())
            mAdapter.enable();
        leScanner = mAdapter.getBluetoothLeScanner();
        leScanner.startScan(mScanCallback);
    }

    /*
    绑定蓝牙设备 最低api 18
     */
    public void connectToDevice(BluetoothDevice device) {
        mGatt = device.connectGatt(this, true, gattCallback);
        mManager.connect(device.getAddress());
    }

    /*
    绑定蓝牙回调
     */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    mManager.setLockedAddress(gatt.getDevice().getAddress());
                    //unregisterReceiver(mBluetoothReceiver);
                    //mHandler.removeMessages(BIND_TIMEOUT);
                    setGlassInfo();
                    leScanner.stopScan(mScanCallback);
                    Intent bind_intent = new Intent(BindCamlogActivity.this,
                            MainActivity.class);
                    bind_intent.putExtra("first_bind", true);
                    startActivity(bind_intent);
                    finish();
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
            gatt.disconnect();
        }
    };


    public void onRemoteCameraLiveButtonClicked(View view) {

        startActivity(new Intent(this, RTMPLiveMainActivity.class).putExtra("bind",false));

//        Intent intent = new Intent(this, RemoteCameraLiveActivity.class);
//        intent.putExtra("unbind_state", true);
//        startActivity(intent);
    }


    public void onBindScanQrCodeClicked(View view) {
        try {
            Intent intent = new Intent(this, CaptureActivity.class);
            startActivityForResult(intent, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "rcv " + intent.getAction());
            if (DefaultSyncManager.RECEIVER_ACTION_DISCONNECTED.equals(intent.getAction())) {
                //mHandler.removeMessages(BIND_TIMEOUT);
                mHandler.removeMessages(BIND_FAIL);
                mHandler.sendEmptyMessage(BIND_FAIL);
            }
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {

                BluetoothDevice scanDevice = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (scanDevice == null || scanDevice.getName() == null) return;
                Log.i(TAG, "name=" + scanDevice.getName() + "address=" + scanDevice.getAddress() + "--Build.BOARD=" + Build.BOARD);
                final String name = scanDevice.getName();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        randomTextView.addKeyWord(name);
                        randomTextView.show();
                    }
                }, 0);

                if (name != null &&name.toUpperCase().startsWith(CAMLOG_BT_NAME))
                    connectDevice(scanDevice);
                else if (name != null && startsWithAry(name))
                    connectDevice(scanDevice);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent
                    .getAction())) {

                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (DEBUG) Log.i(TAG, device.getBondState() + "Other activity===bonded");
                String name = device.getName();
                Log.i(TAG, "ACTION_BOND_STATE_CHANGED device name=" + name + " bondstate=" + device.getBondState());
                if ((deviceName == null && name.toUpperCase().startsWith(CAMLOG_BT_NAME) && !name.endsWith("nc") && SyncApp.BINE_SINGLE_BTNAME) ||
                        (deviceName == null && startsWithAry(name) && !name.endsWith("nc") && !SyncApp.BINE_SINGLE_BTNAME) ||
                        (deviceName != null && name.toUpperCase().equals(deviceName))) {
                    mHandler.removeMessages(BT_BOND_FAILED);
                    switch (device.getBondState()) {
                        case BluetoothDevice.BOND_BONDED:
                            // if(device.getAddress().equals(mPairingDevice.getAddress()))
                            mHandler.removeMessages(REQUEST_CANCEL_SCAN_DEVICE);
                            Message requestBindMsg = mHandler.obtainMessage();
                            requestBindMsg.what = REQUEST_CONNECT;
                            requestBindMsg.obj = device;
                            mHandler.sendMessage(requestBindMsg);
                            break;
                        case BluetoothDevice.BOND_NONE:
                            mHandler.sendEmptyMessage(BT_BOND_FAILED);
                            break;
                    }
                }
            } else if (DefaultSyncManager.RECEIVER_ACTION_STATE_CHANGE
                    .equals(intent.getAction())) {
                int state = intent.getIntExtra(DefaultSyncManager.EXTRA_STATE,
                        DefaultSyncManager.IDLE);
                boolean isConnect = (state == DefaultSyncManager.CONNECTED);
                Log.e(TAG, DefaultSyncManager.RECEIVER_ACTION_STATE_CHANGE + ":" + isConnect);
                if (DEBUG) Log.e(TAG, isConnect + "    isConnect");
                mHandler.removeMessages(BIND_FAIL);
                if (isConnect) {
                    String addr = mManager.getLockedAddress();
                    if (addr.equals("")) {
                        //local has disconnect last,but remote not get notification
                        //notify again
                        Log.w(TAG, "local has disconnect,but remote not get notificaton.notify again!");
                        mManager.disconnect();
                    } else {
                        mManager.setLockedAddress(addr);
                        //unregisterReceiver(mBluetoothReceiver);
                        //mHandler.removeMessages(BIND_TIMEOUT);
                        setGlassInfo();
                        Intent bind_intent = new Intent(BindCamlogActivity.this,
                                MainActivity.class);
                        bind_intent.putExtra("first_bind", true);
                        startActivity(bind_intent);
                        finish();
                    }
                } else {
                    mHandler.sendEmptyMessage(BIND_FAIL);
                }
            }
//            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
//
//
//            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (DEBUG) {
            Log.i(TAG, "onStart in");
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(DefaultSyncManager.RECEIVER_ACTION_STATE_CHANGE);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(DefaultSyncManager.RECEIVER_ACTION_DISCONNECTED);
        registerReceiver(mBluetoothReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop in");
        unregisterReceiver(mBluetoothReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (mGatt != null) {
                mGatt.close();
                mGatt = null;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && leScanner != null) {
            leScanner.stopScan(mScanCallback);
        }

        if (DEBUG) {
            Log.i(TAG, "onDestroy in");
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(TAG, "onActivityResult");
        Log.e(TAG, "scan result:" + data.getStringExtra("name"));
        if (resultCode == RESULT_OK) {
            String resultString = data.getStringExtra("name");
            deviceName = getNameFromQrResult(resultString);
            if (deviceName == null) {
                Toast.makeText(this, R.string.not_camlog_glass, Toast.LENGTH_SHORT).show();
                return;
            }
            onBindHanLangButtonClicked(null);
//    		mBindHanLangBt.setEnabled(false);
//        	mScanQrCodeBt.setEnabled(false);
//        	mBindProgressBar.setIndeterminate(true);
//        	mBindProgressBar.startAnimation();
//    		BluetoothDevice device = mAdapter.getRemoteDevice(mac);
//    		Message requestpairMsg = mHandler.obtainMessage();
//	    	requestpairMsg.what = REQUEST_CONNECT;
//	    	requestpairMsg.obj = device;
//			mHandler.sendMessage(requestpairMsg);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setListViewHeightBasedOnChildren(ListView listView) {

        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();

        if (listAdapter.getCount() == 0) {
            params.height = 0;
            listView.setLayoutParams(params);
        } else {
            View listItem = listAdapter.getView(0, null, listView);
            if (listItem == null) return;
            listItem.measure(0, 0);
            int height = listItem.getMeasuredHeight();
            params.height = (height + listView.getDividerHeight()) * listAdapter.getCount();
            listView.setLayoutParams(params);
        }
    }

    private void bindHanLangFail(int resId) {

        mBindHintTv.setText(resId);

        mBindHanLangBt.setEnabled(true);
        mScanQrCodeBt.setEnabled(true);

//    	mBindProgressBar.stopAnimation();
//    	mBindProgressBar.setIndeterminate(false);
        bind_radarScanView.stopAnimation();
        randomTextView.removeAllKeyWord();
    }

    private String getNameFromQrResult(String result) {
        Log.e(TAG, "result:" + result);
        if (result == null)
            return null;

        result = result.toUpperCase();
        String strings[] = result.split("#");

        if (strings.length < 2)
            return null;
        if (SyncApp.BINE_SINGLE_BTNAME) {
            if (strings[1].startsWith("CAMLOG_")) {
                return strings[1];
            }
        } else {
            if (startsWithAry(strings[1]))
                return strings[1];
        }
        return null;
    }

    private void restartBluetooth() {
        Log.i(TAG, "restartBluetooth");
    }

    private void setGlassInfo() {
        CamlogCmdChannel channel = CamlogCmdChannel.getInstance(getApplicationContext());
        Packet checkUpdate = channel.createPacket();
        checkUpdate.putInt("type", CamlogCmdChannel.CHECK_UPDATE_GLASS);
        channel.sendPacket(checkUpdate);
        Packet pk = channel.createPacket();
        pk.putInt("type", CamlogCmdChannel.SET_DEFAULT_INFO);
        pk.putString("duration", "0");
        pk.putBoolean("round", false);
        pk.putBoolean("picture", true);
        pk.putBoolean("video", true);
        pk.putBoolean("audio", false);
        pk.putBoolean("voice_recog", true);
        pk.putBoolean("live_record", true);
        channel.sendPacket(pk);
        LanguageModule langModule = LanguageModule.getInstance(this);
        langModule.initLanguage();
    }

    private void connectDevice(BluetoothDevice scanDevice) {
        mHandler.removeMessages(REQUEST_CANCEL_SCAN_DEVICE);
        Message requestpairMsg = mHandler.obtainMessage();
        requestpairMsg.what = REQUEST_CONNECT;
        requestpairMsg.obj = scanDevice;
        mHandler.sendMessage(requestpairMsg);
    }

    private boolean startsWithAry(String name) {
        for (String prefix : SyncApp.BT_NAME_ARY) {
            if (name.toUpperCase().startsWith(prefix))
                return true;
        }
        return false;
    }
}
