package com.sctek.smartglasses.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.cn.zhongdun110.camlog.R;
import com.cn.zhongdun110.camlog.SyncApp;
import com.cn.zhongdun110.camlog.camera.PhotoModule;
import com.cn.zhongdun110.camlog.camera.TakePictureModule;
import com.cn.zhongdun110.camlog.contactslite.ContactsLiteModule;
import com.ingenic.glass.api.sync.SyncChannel;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.utils.StorageUtils;
import com.sctek.smartglasses.biz.BLContacts;
import com.sctek.smartglasses.db.ContactsDBHelper;
import com.sctek.smartglasses.fragments.SettingFragment;
import com.sctek.smartglasses.utils.CamlogCmdChannel;
import com.sctek.smartglasses.utils.CamlogNotifyChannel;
import com.sctek.smartglasses.utils.PhotosSyncRunnable;
import com.sctek.smartglasses.utils.VideoSyncRunnable;
import com.sctek.smartglasses.utils.WifiUtils;
import com.sctek.smartglasses.utils.WifiUtils.WifiCipherType;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.devicemanager.GlassDetect;
import cn.ingenic.glasssync.devicemanager.WifiManagerApi;


public class MainActivity extends BaseFragmentActivity {

    private String TAG = "PhotoEditActivity";
    private final String DIRECTORY = Environment.getExternalStorageDirectory()
            .toString();
    private ImageButton takePhotoBt;
    private ImageButton takeVideoBt;
    //    private Button mSyncHotspotBt;
    private DefaultSyncManager mSyncManager;
    private CamlogCmdChannel mCamlogCmdChannel;
    protected SetWifiAPTask mWifiATask;
    protected WifiManager mWifiManager;
    public ProgressDialog mConnectProgressDialog;
    private boolean mRegistApStateBroadcastReceiver = false;
    private Context mContext;
    private static final int MESSAGE_UNBIND_START = 1;
    private static final int MESSAGE_UNBIND_FINISH = 2;
    public static final int MESSAGE_UPDATE_GLASS = 3;
    public static final int MSG_RESEDN_CONNECT_WIFI = 4;
    private GlassDetect mGlassDetect;
    private static MainActivity mInstance = null;

    private final static int GET_POWER_LEVEL = 13;
    private final static int GET_STORAGE_INFO = 14;
    private final static int GET_POWER_TIMEOUT = 1;

    private BluetoothAdapter mAdapter;

    private DrawerLayout drawer;
    private CircularProgressBar circularProgressBar;//表盘最外圈
    private CircularProgressBar circularProgressBar1;//表盘中圈
    private CircularProgressBar circularProgressBar2;//表盘内圈
    private TextView tv1, tv2, tv3;//电量，空间使用量，null
    private ImageButton liveBt;


    public static MainActivity getInstance() {
        return mInstance;
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_drawer);
        mInstance = this;
        SyncApp.getInstance().addActivity(this);


        mAdapter = BluetoothAdapter.getDefaultAdapter();

        mSyncManager = DefaultSyncManager.getDefault();
        initImageLoader(getApplicationContext());

        initView();

        dialog = new ProgressDialog(MainActivity.this);

        mGlassDetect = GlassDetect.getInstance(getApplicationContext());
        mGlassDetect.setLockedAddress(mSyncManager.getLockedAddress());

        mCamlogCmdChannel = CamlogCmdChannel.getInstance(getApplicationContext());
        initSyncHotspot();

        SharedPreferences pref = getSharedPreferences(SyncApp.SHARED_FILE_NAME, Context.MODE_PRIVATE);

        boolean firstBind = getIntent().getBooleanExtra("first_bind", false);
        syncContactToGlass(false, firstBind);
        if (firstBind) {

            mGlassDetect.set_audio_connect();
            mGlassDetect.set_a2dp_connect();
            Editor editor = pref.edit();
            editor.putBoolean("last_headset_state", true);
            editor.putBoolean("last_a2dp_state", true);
            editor.apply();

            //默认打开直播音频
//            SyncChannel.Packet pk = mCamlogCmdChannel.createPacket();
//            pk.putInt("type", 25);
//            pk.putBoolean("audio", true);
//            mCamlogCmdChannel.sendPacket(pk);

        } else if (pref.getBoolean("last_headset_state", false) &&
                (mGlassDetect.getCurrentHeadSetState() == BluetoothProfile.STATE_DISCONNECTED)) {
            mGlassDetect.set_audio_connect();
        } else if (pref.getBoolean("last_a2dp_state", false) &&
                (mGlassDetect.getCurrentA2dpState() == BluetoothProfile.STATE_DISCONNECTED)) {
            mGlassDetect.set_a2dp_connect();
        }
        mCamlogCmdChannel.sendSyncTime();
        TakePictureModule module = TakePictureModule.getInstance(this);
        module.registerHandler(handler);
        getData();

    }


    private void initView() {


        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    drawer.openDrawer(GravityCompat.START);
                }
            }
        });


        findViewById(R.id.setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingActivity.class));
            }
        });


        takePhotoBt = (ImageButton) findViewById(R.id.take_photo);
        takeVideoBt = (ImageButton) findViewById(R.id.recorder);
        liveBt = (ImageButton) findViewById(R.id.location_live);

        takePhotoBt.setOnClickListener(mClickedListener);
        takeVideoBt.setOnClickListener(mClickedListener);
        liveBt.setOnClickListener(mClickedListener);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View drawview = navigationView.inflateHeaderView(R.layout.nav_header_main);

        drawview.findViewById(R.id.sync_photo_view).setOnClickListener(mClickedListener);
        drawview.findViewById(R.id.sync_video_view).setOnClickListener(mClickedListener);
        drawview.findViewById(R.id.live).setOnClickListener(mClickedListener);
        drawview.findViewById(R.id.remote_live_view).setOnClickListener(mClickedListener);
        drawview.findViewById(R.id.about_view).setOnClickListener(mClickedListener);
        drawview.findViewById(R.id.unbind_view).setOnClickListener(mClickedListener);


        circularProgressBar = (CircularProgressBar) findViewById(R.id.circularProgressbar);
        circularProgressBar.setColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        circularProgressBar.setBackgroundColor(ContextCompat.getColor(this, R.color.circular_pb_background));
        circularProgressBar.setProgressBarWidth(getResources().getDimension(R.dimen.layout_margin));
        circularProgressBar.setBackgroundProgressBarWidth(getResources().getDimension(R.dimen.layout_margin));


        circularProgressBar1 = (CircularProgressBar) findViewById(R.id.circularProgressbar1);
        circularProgressBar1.setColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
        circularProgressBar1.setBackgroundColor(ContextCompat.getColor(this, R.color.circular_pb_background));
        circularProgressBar1.setProgressBarWidth(getResources().getDimension(R.dimen.layout_margin));
        circularProgressBar1.setBackgroundProgressBarWidth(getResources().getDimension(R.dimen.layout_margin));

        circularProgressBar2 = (CircularProgressBar) findViewById(R.id.circularProgressbar2);
        circularProgressBar2.setColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        circularProgressBar2.setBackgroundColor(ContextCompat.getColor(this, R.color.circular_pb_background));
        circularProgressBar2.setProgressBarWidth(getResources().getDimension(R.dimen.layout_margin));
        circularProgressBar2.setBackgroundProgressBarWidth(getResources().getDimension(R.dimen.layout_margin));

        tv1 = (TextView) findViewById(R.id.tv1);
        tv2 = (TextView) findViewById(R.id.tv2);
        tv3 = (TextView) findViewById(R.id.tv3);

    }


    /*
获取电量信息、存储空间占用信息
 */
    private void getData() {
        //获取电量
        SyncChannel.Packet pk = mCamlogCmdChannel.createPacket();
        pk.putInt("type", GET_POWER_LEVEL);
        mCamlogCmdChannel.sendPacket(pk);

        //获取空间

        SyncChannel.Packet pk1 = mCamlogCmdChannel.createPacket();
        pk1.putInt("type", GET_STORAGE_INFO);
        mCamlogCmdChannel.sendPacket(pk1);

        mCmdHandler.sendEmptyMessageDelayed(GET_POWER_TIMEOUT, 15000);
    }


    private void initSyncHotspot() {
        mContext = this;
        mCamlogCmdChannel.registerHandler("PhotoEditActivity", mCmdHandler);
        mConnectProgressDialog = new ProgressDialog(mContext);
        mConnectProgressDialog.setTitle(R.string.sync_phone_wifi_hotspot);
        mConnectProgressDialog.setCancelable(false);
        mConnectProgressDialog.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.cancel();
                }
                return false;
            }
        });
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    private void syncContactToGlass(Boolean value, boolean firstBind) {
        ContactsLiteModule clm = (ContactsLiteModule) ContactsLiteModule.getInstance(getApplicationContext());
        clm.sendSyncRequest(value, null);
        clm.setSyncEnable(value);
        BLContacts.getInstance(getApplicationContext()).syncContacts(false, firstBind);
    }

    private long currentTime = System.currentTimeMillis();

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            long tempTime = System.currentTimeMillis();
            long interTime = tempTime - currentTime;
            if (interTime > 2000) {

                Locale local = getResources().getConfiguration().locale;

                if (local.getLanguage().contains("zh")) {
                    Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                }
                currentTime = tempTime;
                return;
            }
//			turnApOff();
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mAdapter.isEnabled())
            mAdapter.enable();

        PhotoModule.getInstance(getApplicationContext()).requestCameraState();
        CamlogNotifyChannel notifyChannel = CamlogNotifyChannel.getInstance(this);
        Packet getPower = notifyChannel.createPacket();
        getPower.putInt("type", CamlogNotifyChannel.MSG_TYPE_POWER_CHANGE);
        notifyChannel.sendPacket(getPower);
        getWifiConnectState();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public static void initImageLoader(Context context) {
        // This configuration tuning is custom. You can tune every option, you may tune some of them,
        // or you can create default configuration by
        //  ImageLoaderConfiguration.createDefault(this);
        // method.
        String cacheDir = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/.glasses_image_cache";
        File cacheFile = StorageUtils.getOwnCacheDirectory(context, cacheDir);
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .threadPoolSize(3)
                .denyCacheImageMultipleSizesInMemory()
                .diskCacheFileNameGenerator(new Md5FileNameGenerator())
                .diskCacheSize(50 * 1024 * 1024) // 50 Mb
                .diskCache(new UnlimitedDiskCache(cacheFile))
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                .writeDebugLogs() // Remove for release app
                .diskCacheExtraOptions(480, 320, null)
                .build();
        // Initialize ImageLoader with configuration.

        if (!ImageLoader.getInstance().isInited())
            ImageLoader.getInstance().init(config);
    }

    private OnClickListener mClickedListener = new OnClickListener() {

        @SuppressLint("NewApi")
        @Override
        public void onClick(View v) {
            switch (v.getId()) {

                case R.id.sync_photo_view:
                    startActivity(new Intent(MainActivity.this, PhotoActivity.class));
                    break;
                case R.id.sync_video_view:
                    startActivity(new Intent(MainActivity.this, VideoActivity.class));
                    break;
                case R.id.live:
                    Intent intent = new Intent(MainActivity.this, LiveDisplayActivity.class);
                    startActivity(intent);
                    break;
                case R.id.remote_live_view:
                    if (mAdapter.isEnabled()
                            && (mAdapter
                            .getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED)) {
                        mGlassDetect.set_a2dp_disconnect();
                        SharedPreferences pref =
                                getSharedPreferences(SyncApp.SHARED_FILE_NAME,
                                        MODE_PRIVATE);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putBoolean("last_a2dp_state", false);
                        editor.apply();
                        mGlassDetect.set_a2dp_disconnect();
                    }

                    startActivity(new Intent(MainActivity.this,
                            SelectCameraLiveActivity.class));

//                    startActivity(new Intent(MainActivity.this,SelectCameraLiveActivity.class));

                    break;
                case R.id.live_tv:
                    startActivity(new Intent(MainActivity.this, SelectCameraLiveActivity.class));
                    break;
                case R.id.unbind_view:
                    showUbindDialog();
                    break;
                case R.id.about_view:
                    startActivity(new Intent(MainActivity.this, AboutActivity.class));
                    break;
                case R.id.take_photo:
                    if (mCamlogCmdChannel.isConnected()) {//bluetooth is connected or not.
                        PhotoModule m = PhotoModule.getInstance(getApplicationContext());
                        m.send_take_photo();
                        takePhotoBt.setEnabled(false);
                        handler.postDelayed(takePhotoRunnable, 2000);
                    } else {
                        Toast.makeText(MainActivity.this, R.string.bluetooth_error, Toast.LENGTH_LONG).show();
                    }
                    break;
                case R.id.location_live:
                    Intent intentLive = new Intent(MainActivity.this, LiveDisplayActivity.class);
                    startActivity(intentLive);
                    break;
                case R.id.recorder:
                    if (mCamlogCmdChannel.isConnected()) {
                        PhotoModule.getInstance(getApplicationContext()).send_record();
                        takeVideoBt.setEnabled(false);
                        handler.postDelayed(takeVideoRunnable, 2000);
                    } else {
                        Toast.makeText(MainActivity.this, R.string.bluetooth_error, Toast.LENGTH_LONG).show();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public void showUbindDialog() {

        AlertDialog.Builder builder = new Builder(this);
        builder.setTitle(R.string.unbind);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    GlassDetect glassDetect = GlassDetect.getInstance(getApplicationContext());
                    glassDetect.set_audio_disconnect();
                    glassDetect.set_a2dp_disconnect();

                    disableLocalData();
                    unBond();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        builder.create().show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy in");
        mInstance = null;
        handler.removeMessages(MSG_RESEDN_CONNECT_WIFI);
        SyncApp.getInstance().exitAllActivity();
        ImageLoader.getInstance().clearDiskCache();
        ImageLoader.getInstance().clearMemoryCache();
        ImageLoader.getInstance().destroy();
        mCamlogCmdChannel.unregisterHandler("PhotoEditActivity");
        if (mRegistApStateBroadcastReceiver)
            mContext.unregisterReceiver(mApStateBroadcastReceiver);
    }

    ProgressDialog dialog;
    Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MESSAGE_UNBIND_START:
                    dialog.setMessage(getResources().getText(R.string.unbonding));
                    dialog.show();
                    break;
                case MESSAGE_UNBIND_FINISH:
                    if (dialog.isShowing())
                        dialog.cancel();
                    break;
                case TakePictureModule.MSG_RECEIVE_PICTURE_DATA:
                    byte[] picData = (byte[]) msg.obj;
                    addImage(picData);
                    Toast.makeText(MainActivity.this, "接受图片成功", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_RESEDN_CONNECT_WIFI:
                    sendApInfoToGlass();
                    break;
            }
        }
    };
    Handler mCmdHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case CamlogCmdChannel.CHECK_UPDATE_SUCCESS:
                    showUpdateConfirmDialog();
                    break;
                case CamlogCmdChannel.RECEIVE_MSG_FROM_GLASS:
                    Packet data = (Packet) msg.obj;
                    int type = data.getInt("type");
                    if (type == GET_POWER_LEVEL) {
                        int level = data.getInt("power");
                        circularProgressBar.setProgressWithAnimation(level, 2000);
                        removeMessages(GET_POWER_TIMEOUT);
                        tv1.setText(getResources().getString(R.string.power) + level + "%");
                    } else if (type == GET_STORAGE_INFO) {
                        String total = data.getString("total");
                        double totalStorage = Double.parseDouble(total.substring(0, total.length() - 2));
                        if (totalStorage <= 4.00)
                            totalStorage = 4.00;
                        else if (totalStorage <= 8.00)
                            totalStorage = 8.00;
                        else
                            totalStorage = 16.00;
                        String available = data.getString("available");
                        double usedStorage;
                        double avlbStorage = Double.parseDouble(available.substring(0, available.length() - 2));
                        if (available.endsWith("GB")) {
                            usedStorage = totalStorage - avlbStorage;
                        } else if (available.endsWith("MB")) {
                            usedStorage = totalStorage - avlbStorage / 1024;
                        } else {
                            usedStorage = totalStorage;
                        }
                        String userd = Double.toString(usedStorage);
                        circularProgressBar1.setProgressWithAnimation((float) (usedStorage / totalStorage) * 100, 2000);
                        tv2.setText(getResources().getString(R.string.rom) + String.valueOf((float) (usedStorage / totalStorage) * 100).substring(0, 4) + "%");
                        tv3.setText(getResources().getString(R.string.ram) + "0.0%" + "");
                    }
                    break;

                case GET_POWER_TIMEOUT:

                    break;
                default:
                    break;


            }
        }
    };

    private void addImage(byte[] jpeg) {
        File dir = new File(DIRECTORY);
        if (!dir.exists())
            dir.mkdirs();
        String path = DIRECTORY + '/'
                + generateName(System.currentTimeMillis()) + ".jpg";
        Log.i(TAG, "path  = " + path + "--jpeg.length = " + jpeg.length);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            out.write(jpeg);
        } catch (Exception e) {
            Log.e(TAG, "Failed to write image", e);
        } finally {
            try {
                out.close();
                Intent ti = new Intent();
                ti.setAction("cn.ingenic.kx.sendpic");
                ti.putExtra("kx.pic.path", path);
                getApplicationContext().sendBroadcast(ti);
            } catch (Exception e) {
            }

        }
    }

    private String generateName(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat format = new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss",
                Locale.US);
        String result = format.format(date);
        return result;
    }

    private void unBond() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                handler.sendEmptyMessage(MESSAGE_UNBIND_START);
                BLContacts.getInstance(getApplicationContext()).stopSyncContacts();
                ContactsDBHelper.getInstance(getApplicationContext(), null).clearAllData();
                try {
                    mSyncManager.setLockedAddress("", true);
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }
                    mSyncManager.disconnect();
                    handler.sendEmptyMessage(MESSAGE_UNBIND_FINISH);
                    clearBetteryNotifi();
                    Intent intent = new Intent(MainActivity.this, BindCamlogActivity.class);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private Runnable takePhotoRunnable = new Runnable() {

        @Override
        public void run() {
            takePhotoBt.setEnabled(true);
            //takePhotoBt.setAlpha(255);
        }
    };

    private Runnable takeVideoRunnable = new Runnable() {

        @Override
        public void run() {
            takeVideoBt.setEnabled(true);
            //takeVideoBt.setAlpha(255);
        }
    };

    private void disableLocalData() {
        SharedPreferences sp = getSharedPreferences(SyncApp.SHARED_FILE_NAME
                , MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.clear();
        editor.commit();

        SharedPreferences defaultSp = PreferenceManager.getDefaultSharedPreferences(this);
        Editor defaultEditor = defaultSp.edit();
        defaultEditor.clear();
        defaultEditor.commit();
    }

    private void turnApOff() {
        PhotosSyncRunnable photosSyncRunnable = PhotosSyncRunnable.getInstance();
        VideoSyncRunnable videoSyncRunnable = VideoSyncRunnable.getInstance();
        WifiManager wifimanager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!photosSyncRunnable.isRunning() && !videoSyncRunnable.isRunning() &&
                WifiUtils.getWifiAPState(wifimanager) == 13) {
            showTurnApOffDialog();
        } else {
            quit();
        }
    }

    private void showTurnApOffDialog() {

        AlertDialog.Builder builder = new Builder(this);
        builder.setTitle(R.string.turn_wifi_ap_off);
        builder.setMessage(R.string.wifi_ap_hint_off);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        // TODO Auto-generated method stub
                        WifiManager wifimanager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        WifiUtils.setWifiApEnabled(false, wifimanager);
                        return null;
                    }
                }.execute();

                dialog.cancel();
                quit();
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                dialog.cancel();
                quit();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                // TODO Auto-generated method stub
                quit();
            }
        });
        dialog.show();
    }

    private void quit() {
        super.onBackPressed();
    }

    private void showUpdateConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
                Intent intent = new Intent(MainActivity.this, WifiListActivity.class);
                intent.putExtra("wifi_type", WifiListActivity.TYPE_UPDATE);
                startActivity(intent);
            }
        });

        builder.create().show();
    }

    private void clearBetteryNotifi() {
        // 删除通知
        NotificationManager notificationManager = (NotificationManager) this
                .getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(100);
    }

    private void startSilentLive() {
        Packet pk = mCamlogCmdChannel.createPacket();
        pk.putInt("type", SettingFragment.SET_LIVE_AUDIO);
        pk.putBoolean("audio", false);
        mCamlogCmdChannel.sendPacket(pk);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        Editor editor = preferences.edit();
        editor.putBoolean("live_audio", false);
        editor.commit();
        Intent intent = new Intent(MainActivity.this, LiveDisplayActivity.class);
        startActivity(intent);
    }

    public class SetWifiAPTask extends AsyncTask<Boolean, Void, Void> {

        private boolean mMode;
        private boolean mFinish;

        public SetWifiAPTask(boolean mode, boolean finish) {
            mMode = mode;
            mFinish = finish;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i(TAG, "-----SetWifiAPTask onPreExecute------");
            mConnectProgressDialog.setMessage(getResources().getText(R.string.turning_wifi_ap_on));
            mConnectProgressDialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.i(TAG, "-----SetWifiAPTask onPostExecute------");
            //updateStatusDisplay();
//			if (mFinish) mContext.finish();
        }

        @Override
        protected Void doInBackground(Boolean... off) {
            Log.i(TAG, "doInBackground");
            try {
                if (off[0])
                    WifiUtils.toggleWifi(mContext, mWifiManager);
                WifiUtils.turnWifiApOn(mContext, mWifiManager, WifiCipherType.WIFICIPHER_NOPASS);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private BroadcastReceiver mApStateBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(intent.getAction())) {
                int cstate = intent.getIntExtra("wifi_state", -1);
                Log.e(TAG, "WIFI_AP_STATE_CHANGED_ACTION:" + cstate);
                if (cstate == WifiUtils.WIFI_AP_STATE_ENABLED
                        ) {

                    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                    if (!adapter.isEnabled()) {
                        adapter.enable();
                    }
                    WifiManagerApi mWifiManagerApi = new WifiManagerApi(mContext);
                    WifiConfiguration mWifiConfiguration = mWifiManagerApi.getWifiApConfiguration();
                    Log.e(TAG, "ssid:" + mWifiConfiguration.SSID + "password:" + mWifiConfiguration.preSharedKey);
                    setProgressDialog();
                    sendApInfoToGlass();
                    mRegistApStateBroadcastReceiver = false;
                    mContext.unregisterReceiver(mApStateBroadcastReceiver);
                }
            }
        }

    };

    public void sendApInfoToGlass() {

        if (mCamlogCmdChannel.isConnected()) {
            Packet packet = mCamlogCmdChannel.createPacket();
            packet.putInt("type", CamlogCmdChannel.CONNET_WIFI_MSG);

            String ssid = WifiUtils.getValidSsid(mContext);
            String pw = WifiUtils.getValidPassword(mContext);
            String security = WifiUtils.getValidSecurity(mContext);

            packet.putString("ssid", ssid);
            packet.putString("pw", pw);
            packet.putString("security", security);
            mCamlogCmdChannel.sendPacket(packet);
            Log.i(TAG, "---sendApInfoToGlass ssid: " + ssid + " pw: " + pw + " security: " + security);
            handler.sendEmptyMessageDelayed(MSG_RESEDN_CONNECT_WIFI, 5000);
        } else {
            if (mConnectProgressDialog.isShowing())
                mConnectProgressDialog.dismiss();
            Toast.makeText(mContext, R.string.bluetooth_error, Toast.LENGTH_LONG).show();
        }
    }

    private void setProgressDialog() {
        if (mCamlogCmdChannel.isConnected()) {
            mConnectProgressDialog.setMessage(getResources().getText(R.string.wait_device_connect));
            if (!mConnectProgressDialog.isShowing())
                mConnectProgressDialog.show();
        }
    }

    private void getWifiConnectState() {
        //phone wifi ap opened
        if (WifiUtils.getWifiAPState(mWifiManager) == WifiUtils.WIFI_AP_STATE_ENABLED && mCamlogCmdChannel.isConnected()) {
            Log.i(TAG, "getWifiConnectState ");
            Packet pk = mCamlogCmdChannel.createPacket();
            pk.putInt("type", 28);
            mCamlogCmdChannel.sendPacket(pk);
        }
//        else {
//            mSyncHotspotBt.setTextColor(mContext.getResources().getColor(R.color.black));
//            mSyncHotspotBt.setText(mContext.getResources().getString(R.string.sync_phone_wifi_hotspot));
//        }
    }
}
