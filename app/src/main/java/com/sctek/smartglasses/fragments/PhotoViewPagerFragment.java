package com.sctek.smartglasses.fragments;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cn.zhongdun110.camlog.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.photoedit.demo.PhotoEditActivity;
import com.sctek.smartglasses.ui.TouchImageView;
import com.sctek.smartglasses.utils.MediaData;

import java.util.ArrayList;

@SuppressLint("NewApi")
public class PhotoViewPagerFragment extends Fragment {
	
	public static final int FRAGMENT_INDEX = 0;
	private static final String TAG = PhotoViewPagerFragment.class.getName();
	
	private ArrayList<MediaData> mediaList;
	private DisplayImageOptions options;
	private TouchImageView imageView;
	private String type;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e(TAG, "onCreate");
		
		getActivity().getActionBar().hide();
		
		options = new DisplayImageOptions.Builder()
		.showImageOnLoading(R.drawable.ic_stub)
		.resetViewBeforeLoading(true)
		.cacheOnDisk(true)
		.imageScaleType(ImageScaleType.EXACTLY)
		.bitmapConfig(Bitmap.Config.RGB_565)
		.considerExifParams(true)
		.displayer(new FadeInBitmapDisplayer(300))
		.build();
		
		mediaList = getArguments().getParcelableArrayList("data");
		type = getArguments().getString("type");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.e(TAG, "onCreateView");
		View view = inflater.inflate(R.layout.fragment_image_pager, container, false);

		final ViewPager pager = (ViewPager) view.findViewById(R.id.pager);
		pager.setAdapter(new ImageAdapter());

		Button edit_imageview = (Button) view.findViewById(R.id.edit_imageview);

		if (type.equals("native")) {
			edit_imageview.setVisibility(View.VISIBLE);
		} else if (type.equals("remote")) {
			edit_imageview.setVisibility(View.GONE);
		}

		edit_imageview.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						String path = Environment.getExternalStorageDirectory().toString() + "/Camlog/photos/" + mediaList.get(pager.getCurrentItem()).getName();
						Intent it = new Intent(getActivity(), PhotoEditActivity.class);
						it.putExtra("photo_path", path);
						it.putExtra("photo", true);
						startActivity(it);
						break;
				}
				return true;
			}
		});
		
		int position = getArguments().getInt("position");
		pager.setCurrentItem(position);
		
		return view;
	}
	
	@Override
	public void onStart() {
		Log.e(TAG, "onStart");
		super.onStart();
	}
	
	@Override
	public void onResume() {
		Log.e(TAG, "onResume");
		super.onResume();
	}
	
	@Override
	public void onPause() {
		Log.e(TAG, "onPause");
		super.onPause();
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onDestroy() {
		Log.e(TAG, "onDestroy");
		ImageLoader.getInstance().cancelDisplayTask(imageView);
		getActivity().getActionBar().show();
		super.onDestroy();
	}
	
	@Override
	public void onDestroyView() {
		Log.e(TAG, "onDestroyView");
		super.onDestroyView();
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onDetach() {
		Log.e(TAG, "onDetach");
		super.onDetach();
	}
	
	private class ImageAdapter extends PagerAdapter {

		private LayoutInflater inflater;

		ImageAdapter() {
			inflater = LayoutInflater.from(getActivity());
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}

		@Override
		public int getCount() {
			return mediaList.size();
		}

		@Override
		public Object instantiateItem(ViewGroup view, int position) {
			Log.e(TAG, "instantiateItem");
			
			final int mPosition = position;
			View imageLayout = inflater.inflate(R.layout.item_pager_image, view, false);
			assert imageLayout != null;
			imageView = (TouchImageView) imageLayout.findViewById(R.id.image);
			final ProgressBar spinner = (ProgressBar) imageLayout.findViewById(R.id.loading);
			final TextView imageName = (TextView) imageLayout.findViewById(R.id.image_name_tv);
			try{
			ImageLoader.getInstance().displayImage(mediaList.get(position).url, imageView, options, new SimpleImageLoadingListener() {
				@Override
				public void onLoadingStarted(String imageUri, View view) {
					spinner.setVisibility(View.VISIBLE);
					imageName.setVisibility(View.GONE);
				}

				@Override
				public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
					String message = null;
					switch (failReason.getType()) {
						case IO_ERROR:
							message = "Input/Output error";
							break;
						case DECODING_ERROR:
							message = "Image can't be decoded";
							break;
						case NETWORK_DENIED:
							message = "Downloads are denied";
							break;
						case OUT_OF_MEMORY:
							message = "Out Of Memory error";
							break;
						case UNKNOWN:
							message = "Unknown error";
							break;
					}
					Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();

					spinner.setVisibility(View.GONE);
				}

				@Override
				public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
					spinner.setVisibility(View.GONE);
					imageName.setVisibility(View.VISIBLE);
					imageName.setText(mediaList.get(mPosition).name);
				}
			});

			view.addView(imageLayout, 0);
			} catch (Exception e){
				e.printStackTrace();
			}
			return imageLayout;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view.equals(object);
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {
		}

		@Override
		public Parcelable saveState() {
			return null;
		}
	}
	
}
