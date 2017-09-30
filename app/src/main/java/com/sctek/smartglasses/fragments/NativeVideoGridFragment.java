package com.sctek.smartglasses.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

import com.cn.zhongdun110.camlog.R;
import com.sctek.smartglasses.utils.MediaData;
import com.sctek.smartglasses.utils.WifiUtils;

import java.util.ArrayList;


public class NativeVideoGridFragment extends BaseFragment {
	
	public static final int FRAGMENT_INDEX = 2;
	private static final String TAG = NativeVideoGridFragment.class.getName();
	private boolean onCreate;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.e(TAG, "onCreate");
		
		getActivity().getActionBar().show();
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
		getActivity().getActionBar().setHomeButtonEnabled(false);
		
		onCreate = true;
		getVideoPath();
		
		super.onCreate(savedInstanceState);
		
	}
	
	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onStart");
		super.onStart();
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onResume");
		getActivity().setTitle(R.string.native_video);
		
		if(!onCreate) {
			new Handler().post(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					getVideoPath();
					mImageAdapter.notifyDataSetChanged();
				}
			});
			
		}
		
		onCreate = false;
		
		super.onResume();
	}
	
	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onPause");
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onDestroy");
		super.onDestroy();
	}
	
	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onDestroyView");
		selectedMedias.clear();
		super.onDestroyView();
	}
	
	@Override
	public void onDetach() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onDetach");
		super.onDetach();
	}

	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		Log.e(TAG, "onCreateOptionsMenu");
		inflater.inflate(R.menu.native_video_fragment_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.e(TAG, "onOptionsItemSelected");
		switch (item.getItemId()) {
			case R.id.share_item:
				deleteView.setVisibility(View.VISIBLE);
				selectAllView.setVisibility(View.GONE);
				for(CheckBox cb : shareCheckBoxs) {
					cb.setVisibility(View.VISIBLE);
				}
				
				deleteTv.setText(R.string.share);
				deleteTv.setOnClickListener(onNativeVideoShareTvClickListener);
				return true;
			case R.id.glasses_item:
				showRemoteVideoFragment();
				return true;
			case R.id.native_video_delete_item:
				deleteView.setVisibility(View.VISIBLE);
				selectAllView.setVisibility(View.VISIBLE);
				cancelShareSelectView();
				for(CheckBox cb : checkBoxs) {
					cb.setVisibility(View.VISIBLE);
				}
				
				deleteTv.setText(R.string.delete);
				deleteTv.setOnClickListener(onNativeVideoDeleteTvClickListener);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@SuppressLint("NewApi")
	private void getVideoPath() {
		
		mediaList = new ArrayList<MediaData>();
		
		ContentResolver cr = getActivity().getContentResolver();
		Cursor cursor = cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
				new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME}, 
				MediaStore.MediaColumns.DATA + " like ?", 
				new String[]{"%/Camlog/videos%"}, MediaStore.Images.Media.DISPLAY_NAME+" desc");
		
		if(cursor == null || cursor.getCount() == 0){
			if(cursor == null) Log.w(TAG,"---cursor: "+cursor);
			Log.i(TAG,"---no videos find in Camlog/videos.");
			return;
		}
		while(cursor.moveToNext()) {
			
			MediaData md = new MediaData();
			md.setUrl("content://media/external/video/media/" + cursor.getInt(0));
			md.setName(cursor.getString(1));
			mediaList.add(md);
			
		}
		
		cursor.close();
	}
	
	@SuppressLint("NewApi")
	private void showRemoteVideoFragment() {
		
		FragmentManager fragManager = getActivity().getFragmentManager();
		FragmentTransaction transcaction = fragManager.beginTransaction();
		String tag = RemoteVideoGridFragment.class.getName();
		RemoteVideoGridFragment remoteVideoFm = (RemoteVideoGridFragment)fragManager.findFragmentByTag(tag);
		if(remoteVideoFm == null) {
			remoteVideoFm = new RemoteVideoGridFragment();
			Bundle bundle = new Bundle();
			bundle.putInt("index", RemoteVideoGridFragment.FRAGMENT_INDEX);
			bundle.putParcelableArrayList("videos", mediaList);
			remoteVideoFm.setArguments(bundle);
		}
		
		transcaction.replace(android.R.id.content, remoteVideoFm, tag);
		transcaction.addToBackStack(null);
		transcaction.commit();
	}
	
	private void onNativeVideoShareTvClicked() {
		
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.setType("video/mp4");
		
		shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(shareVideo.url));
		Intent selectIntent = Intent.createChooser(shareIntent, getResources().getText(R.string.share));
		startActivity(selectIntent);
		
		selectedCb.setChecked(false);
		onShareCancelTvClicked();
	}
	
	private void showTurnApOffWhenShareVideoDialog() {
		
		AlertDialog.Builder builder = new Builder(getActivity());
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
						WifiManager wifimanager = (WifiManager)getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
						WifiUtils.setWifiApEnabled(false, wifimanager);
						return null;
					}
				}.execute();
				
				
				dialog.cancel();
			}
		});
		
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
				
				dialog.cancel();
			}
		});
		
		AlertDialog dialog = builder.create();
		dialog.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				// TODO Auto-generated method stub
				onNativeVideoShareTvClicked();
				
			}
		});
		dialog.show();
	}
	
	private OnClickListener onNativeVideoDeleteTvClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(selectedMedias.size() != 0)
				showDeleteConfirmDialog();
			else
			 disCheckMedia();
		}
	};
	
	private OnClickListener onNativeVideoShareTvClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(shareVideo != null) {
				if(WifiUtils.getWifiAPState(mWifiManager) == WIFI_AP_STATE_ENABLED) 
					showTurnApOffWhenShareVideoDialog();
				else 
					onNativeVideoShareTvClicked();
			}else
				onShareCancelTvClicked();
		}
	};
	
	private void onShareCancelTvClicked() {
		
		deleteView.setVisibility(View.GONE);
		for(CheckBox cb : shareCheckBoxs) {
			cb.setVisibility(View.GONE);
		}
		
	}
	
	private void showDeleteConfirmDialog() {
		
		AlertDialog.Builder builder = new Builder(getActivity());
		builder.setTitle(R.string.delete);
		builder.setMessage(R.string.delete_message);
		builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				onNativeMediaDeleteTvClicked("videos");
				
				onCancelTvClicked();
			}
		});
		
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
			}
		});
		
		builder.create().show();
		
	}
	
}
