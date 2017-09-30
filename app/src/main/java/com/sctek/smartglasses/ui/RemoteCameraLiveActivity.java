package com.sctek.smartglasses.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cn.zhongdun110.camlog.R;
import com.cn.zhongdun110.camlog.iotc.IOTCClient;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.sctek.smartglasses.db.DBRemoteLive;
import com.sctek.smartglasses.db.RemLiveSQLiteOpenHelper;
import com.sctek.smartglasses.entity.RemoteLiveBean;
import com.sctek.smartglasses.ui.RemoteLiveEdittext.OnScanDrawableClickListener;
import com.sctek.smartglasses.utils.CamlogCmdChannel;
import com.sctek.smartglasses.zxing.CaptureActivity;

public class RemoteCameraLiveActivity extends Activity implements
        OnScanDrawableClickListener, OnClickListener, OnCheckedChangeListener,
        TextWatcher {
    private final String TAG = "RemoteCameraLiveActi";
    private final String REMOTE_LIVE_SHARED_FILE_NAME = "remoteLive";
    private final String UUID = "uid";
    private final String PASSWORD = "pwd";
    private final String OLD_PASSWORD = "remotelive_oldpwd";
    private final String NEW_PASSWORD = "remotelive_newpwd";
    private final String CLEARITY = "clearity";
    private final String DEFAULT_PASSWORD = "888888";
    private final int MSG_MODIFY_TIME_OUT = 1;
    private final int DELAY_MILLS = 5000;
    RemoteLiveEdittext mUUID, mEdttName, mPassWord;
    TextView mModifyPwdView, mLoginView, mResetPwdView;
    private RelativeLayout mRlLayoutChangePwd;
    private RadioGroup mRGSelectClearity;
    private RadioButton mRBLowClearity, mRBStandardClearity, mRBHighClearity;
    private Editor mEditor;
    private int mClearitySelect = IOTCClient.CAMERA_SET_LOW;
    private ProgressDialog mProgressDialog;
    private CamlogCmdChannel mCamlogCmdChannel;
    private RemLiveSQLiteOpenHelper mRemLiveSQLiteOpenHelper;
    private DBRemoteLive mDBRemoteLive;
    private Button bt_show_pwd;
    private boolean hasshowpwd = false;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_MODIFY_TIME_OUT:
                    if (mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }
                    Toast.makeText(RemoteCameraLiveActivity.this,
                            R.string.remote_live_modify_error, Toast.LENGTH_LONG)
                            .show();
                    break;
                case CamlogCmdChannel.SET_REMOTELIVE_PASSWORD:
                    mHandler.removeMessages(MSG_MODIFY_TIME_OUT);
                    if (mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }
                    Packet data = (Packet) msg.obj;
                    int result = data.getInt(CamlogCmdChannel.REMOTE_LIVE_VERIFY_PASSWORD);
                    if (result == 1) {
                        Toast.makeText(RemoteCameraLiveActivity.this,
                                R.string.remote_live_uid_vertify_fail,
                                Toast.LENGTH_SHORT).show();
                    } else if (result == 2) {
                        Toast.makeText(RemoteCameraLiveActivity.this,
                                R.string.remote_live_verify_pwd_error,
                                Toast.LENGTH_SHORT).show();
                    } else if (result == 3) {
                        mPassWord.setText(data.getString(NEW_PASSWORD));
                        saveRemoteLive();
                        Toast.makeText(RemoteCameraLiveActivity.this,
                                R.string.remote_live_modify_success,
                                Toast.LENGTH_LONG).show();
                    }
                    break;
                default:
                    break;
            }
        }

        ;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_cameralive);
        setTitle(R.string.glass_live);
        mUUID = (RemoteLiveEdittext) findViewById(R.id.et_uuid);
        mEdttName = (RemoteLiveEdittext) findViewById(R.id.et_name);
        mPassWord = (RemoteLiveEdittext) findViewById(R.id.et_pwd);
        mLoginView = (TextView) findViewById(R.id.login);
        mModifyPwdView = (TextView) findViewById(R.id.modify_password);
        mResetPwdView = (TextView) findViewById(R.id.reset_password);
        mRGSelectClearity = (RadioGroup) findViewById(R.id.select_clarity);
        mRBLowClearity = (RadioButton) findViewById(R.id.low_clearity);
        mRBStandardClearity = (RadioButton) findViewById(R.id.standard_clearity);
        mRBHighClearity = (RadioButton) findViewById(R.id.high_clearity);
        mRlLayoutChangePwd = (RelativeLayout) findViewById(R.id.relayout_changepwd);
        bt_show_pwd = (Button) findViewById(R.id.bt_show_pwd);


        bt_show_pwd.setOnClickListener(this);
        mUUID.setOnLeftDrawableClickListner(this);
        mUUID.setOnClickListener(this);
        mUUID.addTextChangedListener(this);
        mEdttName.addTextChangedListener(this);
        mEdttName.setScanIconEnable(false);
        mPassWord.addTextChangedListener(this);
        mRGSelectClearity.setOnCheckedChangeListener(this);
        mLoginView.setOnClickListener(this);
        mModifyPwdView.setOnClickListener(this);
        mResetPwdView.setOnClickListener(this);
        mPassWord.setScanIconEnable(false);
        mRemLiveSQLiteOpenHelper = RemLiveSQLiteOpenHelper.getInstance(this);
        mDBRemoteLive = new DBRemoteLive(
                mRemLiveSQLiteOpenHelper.getReadableDatabase());
        RemoteLiveBean remoteliveBean = mDBRemoteLive.findRemoteLiveLatest();
        if (null != remoteliveBean) {
            refreshView(remoteliveBean);
        }
        if (getIntent().getBooleanExtra("unbind_state", false)) {
            mRlLayoutChangePwd.setVisibility(View.GONE);
        }
        mCamlogCmdChannel = CamlogCmdChannel.getInstance(this);
        mCamlogCmdChannel.registerHandler("RemoteCameraLiveActivity", mHandler);
        initProgressDialog();
    }

    private void initProgressDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getResources().getString(
                R.string.remote_live_modifying_pwd));
    }

    @Override
    public void onScanDrawableClick(View view) {
        Intent intent = new Intent(this, CaptureActivity.class);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCamlogCmdChannel.unregisterHandler("RemoteCameraLiveActivity");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "req:" + requestCode + " res:" + resultCode);
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                String scanResult = data.getStringExtra("name");
                String uuid = getUidFromQrResult(scanResult);
                if (uuid != null) {
                    mUUID.setText(uuid);
                }
            } else if (requestCode == 2) {
                String oldpwd = data.getStringExtra(OLD_PASSWORD);
                String newpwd = data.getStringExtra(NEW_PASSWORD);
                modifyPassword(oldpwd, newpwd);
            } else if (requestCode == 3) {
                String uid = data.getStringExtra(RemLiveSQLiteOpenHelper.COL_UID);
                if (null == uid || uid.length() == 0)
                    return;
                RemoteLiveBean relvBean = mDBRemoteLive.findRemoteLiveByUid(uid);
                if (null != relvBean) {
                    refreshView(relvBean);
                } else {
                    refreshView(new RemoteLiveBean());
                }
                mUUID.setText(uid);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.login:
                if (!checkNetworkState()) {
                    Toast.makeText(this, R.string.remote_live_no_network,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                String uid = mUUID.getText().toString();
                String pwd = mPassWord.getText().toString();
//                String pwd = DEFAULT_PASSWORD;
                if (uid.equals("")) {
                    Toast.makeText(this, R.string.remote_live_no_uid,
                            Toast.LENGTH_SHORT).show();
                    return;
                } else if (pwd.equals("")) {
                    Toast.makeText(this, R.string.remote_live_no_pwd,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                saveRemoteLive();
                Intent intent = new Intent(RemoteCameraLiveActivity.this,
                        CameraIOTCLiveActivity.class);
                intent.putExtra(UUID, uid);
                intent.putExtra(PASSWORD, pwd);
                intent.putExtra(CLEARITY, mClearitySelect);
                startActivity(intent);
                finish();
                break;
            case R.id.reset_password:
                modifyPassword("", DEFAULT_PASSWORD);
                break;
            case R.id.modify_password:
                Intent modifyIntet = new Intent(RemoteCameraLiveActivity.this, ModifyPasswordActivtiy.class);
                startActivityForResult(modifyIntet, 2);
                break;
            case R.id.et_uuid:
                Intent intetuid = new Intent(RemoteCameraLiveActivity.this, RemoteLiveSelectUIDActivity.class);
                startActivityForResult(intetuid, 3);
                break;
            case R.id.bt_show_pwd:
                if (hasshowpwd) {
                    mPassWord.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    hasshowpwd = false;
                } else {
                    mPassWord.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    hasshowpwd = true;
                }
                break;
        }
    }


    private boolean checkNetworkState() {
        boolean flag = false;
        // 得到网络连接信息
        ConnectivityManager manager = (ConnectivityManager) this
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        // 去进行判断网络是否连接
        if (manager.getActiveNetworkInfo() != null) {
            flag = manager.getActiveNetworkInfo().isAvailable();
        }
        return flag;
    }

    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.low_clearity:
                mClearitySelect = IOTCClient.CAMERA_SET_LOW;
                break;
            case R.id.standard_clearity:
                mClearitySelect = IOTCClient.CAMERA_SET_MEDIUM;
                break;
            case R.id.high_clearity:
                mClearitySelect = IOTCClient.CAMERA_SET_HIGH;
                break;
        }
    }

    /**
     * 当输入框里面内容发生变化的时候回调的方法
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
                                  int after) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (mUUID.getText().toString().length() > 0
                && mPassWord.getText().toString().length() > 0) {
            mLoginView.setEnabled(true);
        } else {
            mLoginView.setEnabled(false);
        }

        if (mPassWord.getText().toString().length() > 0) {
            bt_show_pwd.setVisibility(View.VISIBLE);
        } else {
            bt_show_pwd.setVisibility(View.GONE);
        }


    }

    private String getUidFromQrResult(String result) {
        Log.e(TAG, "result:" + result);
        if (result == null)
            return null;
        if (result.startsWith("UID=") || result.startsWith("uid=")) {
            int index = result.indexOf("=") + 1;
            String uid = result.substring(index);
            return uid;
        }
        return result;
    }

    private void modifyPassword(String oldPwd, String newPwd) {

        if (!mCamlogCmdChannel.isConnected()) {
            Toast.makeText(this, R.string.bluetooth_error, Toast.LENGTH_LONG)
                    .show();
            return;
        }
        String uid = mUUID.getText().toString();
//        String name = mPassWord.getText().toString();
        if (uid.length() == 0) {
            Toast.makeText(this, R.string.remote_live_no_uid, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
//        else if (name.length() == 0) {
//            Toast.makeText(this, R.string.remote_live_no_name, Toast.LENGTH_SHORT)
//                    .show();
//            return;
//        }
        mProgressDialog.show();
        Log.i(TAG, "oldPwd=" + oldPwd + " newpwd=" + newPwd);
        Packet modifyPassword = mCamlogCmdChannel.createPacket();
        modifyPassword.putInt("type", CamlogCmdChannel.SET_REMOTELIVE_PASSWORD);
        modifyPassword.putString(CamlogCmdChannel.KEY_REMOTELIVE_UID, uid);
        modifyPassword.putString(OLD_PASSWORD, oldPwd);
        modifyPassword.putString(NEW_PASSWORD, newPwd);
        mCamlogCmdChannel.sendPacket(modifyPassword);
        mHandler.sendEmptyMessageDelayed(MSG_MODIFY_TIME_OUT, DELAY_MILLS);
    }

    private void refreshView(RemoteLiveBean livebean) {
        mUUID.setText(livebean.getUid());
        mEdttName.setText(livebean.getName());
        mPassWord.setText(livebean.getPwd());
        int clearity = livebean.getClearity();
        if (clearity == IOTCClient.CAMERA_SET_MEDIUM) {
            mRBStandardClearity.setChecked(true);
        } else if (clearity == IOTCClient.CAMERA_SET_HIGH) {
            mRBHighClearity.setChecked(true);
        } else if (clearity == IOTCClient.CAMERA_SET_LOW) {
            mRBLowClearity.setChecked(true);
        }
    }

    private void saveRemoteLive() {
        String uid = mUUID.getText().toString();
        if (null == uid || uid.length() == 0)
            return;
        RemoteLiveBean remoteLiveBean = new RemoteLiveBean();
        remoteLiveBean.setUid(uid);
        remoteLiveBean.setName(mEdttName.getText().toString());
        remoteLiveBean.setPwd(mPassWord.getText().toString());
//        remoteLiveBean.setPwd(DEFAULT_PASSWORD);
        remoteLiveBean.setClearity(mClearitySelect);
        remoteLiveBean.setTime(System.currentTimeMillis());
        long count = mDBRemoteLive.getCount(RemLiveSQLiteOpenHelper.COL_UID
                + "=?", new String[]{uid});
        if (count == 0) {
            mDBRemoteLive.insertToRmlive(remoteLiveBean);
        } else {
            mDBRemoteLive.updateRmlive(remoteLiveBean);
        }

    }
}
