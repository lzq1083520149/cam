package com.sctek.smartglasses.utils;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cn.zhongdun110.camlog.R;

public class MyDialog extends Dialog implements
		android.view.View.OnClickListener {
	Context context;
	TextView mTv_message, mDialog_cancle_one, mDialog_cancle_two,
			mDialog_ok;
	String mMessage, mCancle_one,mOK;
	RelativeLayout mLayout_onebtn, mLayout_twobtn;
	private LeaveMeetingDialogListener listener;

	public interface LeaveMeetingDialogListener {
		public void onClick(View view);
	}

	public MyDialog(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		this.context = context;
	}

	public MyDialog(Context context, int theme, String message,
			String cancleText, LeaveMeetingDialogListener listener) {
		super(context, theme);
		this.context = context;
		this.mMessage = message;

		this.mCancle_one = cancleText;
		this.listener = listener;
	}

	public MyDialog(Context context, int theme, String message,
			String cancleText, String okText,
			LeaveMeetingDialogListener listener) {
		super(context, theme);
		this.context = context;
		this.mMessage = message;
		this.mCancle_one = cancleText;
		this.mOK = okText;
		this.listener = listener;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.dialog_item);
		mTv_message = (TextView) findViewById(R.id.tv_message);
		mDialog_cancle_one = (TextView) findViewById(R.id.dialog_tv_cancel_one);
		mDialog_cancle_two = (TextView) findViewById(R.id.dialog_tv_cancel_two);
		mDialog_ok = (TextView) findViewById(R.id.dialog_tv_ok);
		mLayout_onebtn = (RelativeLayout) findViewById(R.id.layout_onebtn);
		mLayout_twobtn = (RelativeLayout) findViewById(R.id.layout_twobtn);
		mDialog_cancle_one.setOnClickListener(this);
		mDialog_cancle_two.setOnClickListener(this);
		mDialog_ok.setOnClickListener(this);
		mTv_message.setText(mMessage);
		if (mOK != null) {
			mLayout_twobtn.setVisibility(View.VISIBLE);
			mLayout_onebtn.setVisibility(View.GONE);
			Log.d("Tag", mOK+"======="+mDialog_cancle_two.getText()+"mDialog_cancle_two");
			mDialog_cancle_two.setText(mCancle_one);
			mDialog_ok.setText(mOK);
			mTv_message.setText(mMessage);
		} else {
			mDialog_cancle_one.setText(mCancle_one);
			mTv_message.setText(mMessage);
		}

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		listener.onClick(v);
	}
	
}
