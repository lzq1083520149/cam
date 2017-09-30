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
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;

import com.cn.zhongdun110.camlog.R;
import com.photoedit.demo.PhotoEditActivity;
import com.sctek.smartglasses.utils.MediaData;
import com.sctek.smartglasses.utils.WifiUtils;

import java.util.ArrayList;

public class NativePhotoGridFragment extends BaseFragment {
	
	public static final int FRAGMENT_INDEX = 1;
	private static final String TAG = NativePhotoGridFragment.class.getName();
	private boolean onCreate;
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e(TAG, "onCreate");
		
		getActivity().getActionBar().show();
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
		getActivity().getActionBar().setHomeButtonEnabled(false);
		
		onCreate = true;
		getImagePath();
		
	}
	
	@Override
	public void onStart() {
		Log.e(TAG, "onStart");
		super.onStart();
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onResume() {
		Log.e(TAG, "onResume");
		getActivity().setTitle(R.string.native_photo);
		
		if(!onCreate) {
			new Handler().post(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					getImagePath();
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
		inflater.inflate(R.menu.native_photo_fragment_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.e(TAG, "onOptionsItemSelected checkBoxs size= " +checkBoxs.size());
		switch (item.getItemId()) {
			case R.id.share_item:
				deleteView.setVisibility(View.VISIBLE);
				selectAllView.setVisibility(View.VISIBLE);
				
				for(CheckBox cb : checkBoxs) {
					cb.setVisibility(View.VISIBLE);
				}
				
				deleteTv.setText(R.string.share);
				deleteTv.setOnClickListener(onNativePhotoShareTvClickListener);
				return true;
			case R.id.glasses_item:
				showRemotePhotoFragment();
				return true;
			case R.id.native_photo_delete_item:
				deleteView.setVisibility(View.VISIBLE);
				selectAllView.setVisibility(View.VISIBLE);
				
				for(CheckBox cb : checkBoxs) {
					cb.setVisibility(View.VISIBLE);
				}
				
				deleteTv.setText(R.string.delete);
				deleteTv.setOnClickListener(onNativePhotoDeleteTvClickListener);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private void getImagePath() {
		mediaList = new ArrayList<>();
		
		ContentResolver cr = getActivity().getContentResolver();
		Cursor cursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATA}, 
				MediaStore.MediaColumns.DATA + " like ?", 
				new String[]{"%/Camlog/photos%"}, MediaStore.Images.Media.DISPLAY_NAME+" desc");
		
		if(cursor == null || cursor.getCount() == 0){
			if(cursor == null) Log.w(TAG,"---cursor: "+cursor);
			Log.i(TAG,"---no photos find in Camlog/photos.");
			return;
		}
		while(cursor.moveToNext()) {
			
			Log.e(TAG, cursor.getString(2));
			MediaData md = new MediaData();
			md.setUrl("content://media/external/images/media/" + cursor.getInt(0));
			md.setName(cursor.getString(1));
            md.setMdownload(getResources().getString(R.string.media_downloaded));
			mediaList.add(md);
			
		}
        cursor.close();
	}
	
	private void showRemotePhotoFragment() {
		
		FragmentManager fragManager = getActivity().getFragmentManager();
		FragmentTransaction transcaction = fragManager.beginTransaction();
		String tag = RemotePhotoGridFragment.class.getName();
		RemotePhotoGridFragment remotePhotoFm = (RemotePhotoGridFragment)fragManager.findFragmentByTag(tag);
		if(remotePhotoFm == null) {
			
			remotePhotoFm = new RemotePhotoGridFragment();
			Bundle bundle = new Bundle();
			bundle.putInt("index", RemotePhotoGridFragment.FRAGMENT_INDEX);
			bundle.putParcelableArrayList("photos", mediaList);
			remotePhotoFm.setArguments(bundle);
		}
		
		transcaction.replace(android.R.id.content, remotePhotoFm, tag);
		transcaction.addToBackStack(null);
		transcaction.commit();
	}
	
	private void onNativePhotoShareTvClicked() {
		
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
		shareIntent.setType("image/jpeg");
		ArrayList<Uri> photoUris = new ArrayList<Uri>();
		
		for(MediaData dd: selectedMedias) {
			photoUris.add(Uri.parse(dd.url));
		}
		
		shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, photoUris);
		startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share)));
		disCheckMedia();
		
	}
	
	private void showTurnApOffWhenSharePhotosDialog() {
		
		AlertDialog.Builder builder = new Builder(getActivity());
		builder.setTitle(R.string.turn_wifi_ap_off);
		builder.setMessage(R.string.wifi_ap_hint_off);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
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

				
				dialog.cancel();
			}
		});
		
		AlertDialog dialog = builder.create();
		dialog.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				onNativePhotoShareTvClicked();
				
			}
		});
		dialog.show();
	}
	
	private OnClickListener onNativePhotoDeleteTvClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(selectedMedias.size() != 0)
				showDeleteConfirmDialog();
			else
				disCheckMedia();
		}
	};
	
	private OnClickListener onNativePhotoShareTvClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(selectedMedias.size() != 0) {
				if(WifiUtils.getWifiAPState(mWifiManager) == WIFI_AP_STATE_ENABLED)
					showTurnApOffWhenSharePhotosDialog();
				else 
					onNativePhotoShareTvClicked();
			} else
				disCheckMedia();
			onCancelTvClicked();
		}
	};
	
	private void showDeleteConfirmDialog() {
		
		AlertDialog.Builder builder = new Builder(getActivity());
		builder.setTitle(R.string.delete);
		builder.setMessage(R.string.delete_message);
		builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				onNativeMediaDeleteTvClicked("photos");
				
				onCancelTvClicked();
			}
		});
		
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		});
		
		builder.create().show();
		
	}


	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
//
//		Button button = (Button) view.findViewById(R.id.bt);
//		button.setVisibility(View.VISIBLE);
//		button.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
////				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
////				intent.setType("image/jpeg");
////				intent.addCategory(Intent.CATEGORY_OPENABLE);
//				startActivity(new Intent(getActivity(), PhotoEditActivity.class));
//			}
//		});
	}

//
//	@Override
//	public void onActivityResult(int requestCode, int resultCode, Intent data) {
//		super.onActivityResult(requestCode, resultCode, data);
//		if (resultCode == Activity.RESULT_OK && requestCode == 1) {//是否选择，没选择就不会继续
//			Uri uri = data.getData();//得到uri，后面就是将uri转化成file的过程。
//			String[] proj = {MediaStore.Images.Media.DATA};
//			Cursor actualimagecursor = getActivity().managedQuery(uri, proj, null, null, null);
//			int actual_image_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
//			actualimagecursor.moveToFirst();
//			String img_path = actualimagecursor.getString(actual_image_column_index);
//			File file = new File(img_path);
//
//			if (img_path.endsWith("jpg") || img_path.endsWith("jpeg") || img_path.endsWith("png")) {
//				Intent it = new Intent(getActivity(), PhotoEditActivity.class);
//				it.putExtra(PhotoEditActivity.INTENT_FILE_TYPE, file.getAbsolutePath());
//				startActivity(it);
//			} else {
//				Toast.makeText(getActivity(), "Please choose a picture", Toast.LENGTH_SHORT).show();
//			}
//		}
//	}

}
