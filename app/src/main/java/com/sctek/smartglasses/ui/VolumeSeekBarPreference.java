package com.sctek.smartglasses.ui;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;

import com.cn.zhongdun110.camlog.R;

public class VolumeSeekBarPreference extends DialogPreference{

	private Context mContext;
	private SeekBar seekbar;
	public VolumeSeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		mContext = context;
	}
	
	@Override
	protected View onCreateDialogView() {
		// TODO Auto-generated method stub
		View view = LayoutInflater.from(mContext).inflate(R.layout.volume_seekbar, null);
		seekbar = (SeekBar)view.findViewById(R.id.volume_seekbar);
		
		int progress = getSharedPreferences().getInt("volume", 7);
		seekbar.setProgress(progress);
		
		return view;
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		// TODO Auto-generated method stub
		if(positiveResult) {
			int progress = seekbar.getProgress();
			if(callChangeListener(progress))
				setValue(progress);
		}
		super.onDialogClosed(positiveResult);
	}
	
	public void setValue(int progress) {
		
		int oldProg = getSharedPreferences().getInt("volume", 7);
		
		Editor editor = getEditor();
		editor.putInt("volume", progress);
		editor.commit();
		
		if(progress != oldProg)
			notifyChanged();
		
	}
	
	

}
