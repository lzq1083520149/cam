package com.sctek.smartglasses.fragments;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.cn.zhongdun110.camlog.R;

import java.util.Locale;

@SuppressLint("NewApi")
public class GuideViewPagerFragment extends Fragment {
	
	public static final int FRAGMENT_INDEX = 0;
	private static final String TAG = GuideViewPagerFragment.class.getName();
	
	private ImageView dot1;
	private ImageView dot2;
	private ImageView dot3;
	private ImageView dot4;
	private ImageView dot5;
	
	private int guideIds[];
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e(TAG, "onCreate");
		
		getActivity().getActionBar().hide();
		
		Locale local = getActivity().getResources().getConfiguration().locale;
		if(local.getLanguage().contains("zh")) {
			guideIds = new int[]{ R.drawable.welcome};
		}
		else {
			guideIds = new int[] { R.drawable.welcome};
		}
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		Log.e(TAG, "onCreateView");
		View view = inflater.inflate(R.layout.user_guide_pager, container, false);
		
		ViewPager pager = (ViewPager) view.findViewById(R.id.pager);
		
//		dot1 = (ImageView)view.findViewById(R.id.dot_1);
//		dot2 = (ImageView)view.findViewById(R.id.dot_2);
//		dot3 = (ImageView)view.findViewById(R.id.dot_3);
//		dot4 = (ImageView)view.findViewById(R.id.dot_4);
//		dot5 = (ImageView)view.findViewById(R.id.dot_5);
		
		pager.setAdapter(new ImageAdapter());
//		dot1.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.dot_select));
//		pager.setOnPageChangeListener(new OnPageChangeListener() {
//			
//			private ImageView dot = dot1;
//			@Override
//			public void onPageSelected(int position) {
//				// TODO Auto-generated method stub
//				Log.e(TAG, "onPageSelected:" + position);
//				switch(position) {
//				case 0:
//					dot1.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.dot_select));
//					if(dot !=null)
//						dot.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.dot_normal));
//					dot = dot1;
//					break;
//				case 1:
//					dot2.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.dot_select));
//					if(dot !=null)
//						dot.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.dot_normal));
//					dot = dot2;
//					break;
//				case 2:
//					dot3.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.dot_select));
//					if(dot !=null)
//						dot.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.dot_normal));
//					dot = dot3;
//					break;
//				case 3:
//					dot4.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.dot_select));
//					if(dot !=null)
//						dot.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.dot_normal));
//					dot = dot4;
//					break;
//				case 4:
//					dot5.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.dot_select));
//					if(dot !=null)
//						dot.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.dot_normal));
//					dot = dot5;
//					break;
//			}
//			}
//			
//			@Override
//			public void onPageScrolled(int arg0, float arg1, int arg2) {
//				// TODO Auto-generated method stub
//				
//			}
//			
//			@Override
//			public void onPageScrollStateChanged(int arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//		});
		
		return view;
	}
	
	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onStart");
		super.onStart();
	}
	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onResume");
		
		super.onResume();
	}
	
	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onPause");
		super.onPause();
	}
	
	@SuppressLint("NewApi")
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
		super.onDestroyView();
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onDetach() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onDetach");
		super.onDetach();
	}
	
	private class ImageAdapter extends PagerAdapter {

		private ImageView dot;
		
		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((ImageView) object);
		}

		@Override
		public int getCount() {
			return guideIds.length;
		}

		@Override
		public Object instantiateItem(ViewGroup view, int position) {
			Log.e(TAG, "instantiateItem: " + position);
			
			ImageView guideView = new ImageView(getActivity());
			
			guideView.setImageDrawable(getActivity().getResources().getDrawable(guideIds[position]));
			
			view.addView(guideView, 0);
			return guideView;
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
