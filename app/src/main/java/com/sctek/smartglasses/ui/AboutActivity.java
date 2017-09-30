package com.sctek.smartglasses.ui;

import com.sctek.smartglasses.fragments.AboutFragment;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;

public class AboutActivity extends BaseFragmentActivity {

	private String TAG;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		new Handler().post(new Runnable() {
			
			@SuppressLint("NewApi")
			@Override
			public void run() {
				// TODO Auto-generated method stub
				TAG = AboutFragment.class.getName();
				AboutFragment aboutGF = (AboutFragment)getFragmentManager().findFragmentByTag(TAG);
				if(aboutGF == null) {
					aboutGF = new AboutFragment();
				}
				getFragmentManager().beginTransaction()
						.replace(android.R.id.content, aboutGF, TAG).commit();
			}
		});
	}
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		int stackCount = getFragmentManager().getBackStackEntryCount();
		if(stackCount != 0) {
			if(stackCount == 1)
				getFragmentManager().popBackStack();
		} 
		else {
			super.onBackPressed();
		}
	}

}
