package com.sctek.smartglasses.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.cn.zhongdun110.camlog.R;

public class ModifyPasswordActivtiy extends Activity implements OnClickListener,TextWatcher
											{
		
	private String TAG = "ModifyPasswordActivtiy";
	private RemoteLiveEdittext mOldPwdEdTt,mNewPwdEdTt,mEnsurePwdEdTt;
	private TextView mEnsureModifyTv;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_remlive_modifypwd);
		initView();
	}
	
	private void initView(){
		mOldPwdEdTt = (RemoteLiveEdittext)findViewById(R.id.et_oldpwd);
		mNewPwdEdTt = (RemoteLiveEdittext)findViewById(R.id.et_newpwd);
		mEnsurePwdEdTt = (RemoteLiveEdittext)findViewById(R.id.et_ensurepwd);
		mEnsureModifyTv = (TextView)findViewById(R.id.ensure_modify);
		mEnsureModifyTv.setOnClickListener(this);
		mEnsureModifyTv.setEnabled(false);
		mOldPwdEdTt.setScanIconEnable(false);
		mNewPwdEdTt.setScanIconEnable(false);
		mEnsurePwdEdTt.setScanIconEnable(false);
		mOldPwdEdTt.addTextChangedListener(this);
		mNewPwdEdTt.addTextChangedListener(this);
		mEnsurePwdEdTt.addTextChangedListener(this);
	}

	@Override
	public void onClick(View arg0) {
		switch(arg0.getId()){
		case R.id.ensure_modify:
			String newPwd = mNewPwdEdTt.getText().toString();
			String ensurePwd = mEnsurePwdEdTt.getText().toString();
			if(!newPwd.equals(ensurePwd)){
				Toast.makeText(ModifyPasswordActivtiy.this, R.string.remote_live_pwd_inconsistent, Toast.LENGTH_LONG).show();
				return;
			}
			Intent intent = new Intent();
		    intent.putExtra("remotelive_oldpwd", mOldPwdEdTt.getText().toString());
		    intent.putExtra("remotelive_newpwd", mNewPwdEdTt.getText().toString());
		    setResult(RESULT_OK, intent);
		    finish();
			break;
		}
	}
	
	@Override
	  public boolean onKeyDown(int keyCode, KeyEvent event) {
	    switch (keyCode) {
	      case KeyEvent.KEYCODE_BACK:
	    	  	setResult(RESULT_CANCELED, new Intent());
	    	  	finish();
	    	  	return true;
	    }
	    return super.onKeyDown(keyCode, event);
	  }

	@Override
	public void afterTextChanged(Editable arg0) {
		Log.i(TAG,"text change oldpwd="+mNewPwdEdTt.getText()+" newpwd="+mOldPwdEdTt.getText());
		if(mOldPwdEdTt.getText().toString().length()>=6 && mOldPwdEdTt.getText().toString().length()<=32
				&& mNewPwdEdTt.getText().toString().length()>=6 && mNewPwdEdTt.getText().toString().length()<=32
				&& mEnsurePwdEdTt.getText().toString().length()>=6 && mEnsurePwdEdTt.getText().toString().length()<=32){
			mEnsureModifyTv.setEnabled(true);
		    }else {
			mEnsureModifyTv.setEnabled(false);
		    }
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
			int arg3) {
	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		
      	}
}
