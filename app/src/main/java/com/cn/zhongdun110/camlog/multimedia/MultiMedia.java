package com.cn.zhongdun110.camlog.multimedia;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.cn.zhongdun110.camlog.R;

import java.util.ArrayList;
import java.util.HashMap;

public class MultiMedia extends Activity{
    private static final String TAG = "MultiMedia";

    private MultiMediaAdapter mMyAdapter;
    private ListView mListView;
    private int mWidth;
    private int mWidthHalf;
    private Context mContext;
    ArrayList<HashMap<String, Object>>mListItem;

    String[] picColu = new String[] {
	MediaStore.Images.Media._ID,
	MediaStore.Images.Media.DATA
    };

    @Override
    protected void onCreate (Bundle savedInstanceState){
	Log.e(TAG, "onCreate");
	super.onCreate(savedInstanceState);

	DisplayMetrics metric = new DisplayMetrics();
	getWindowManager().getDefaultDisplay().getMetrics(metric);
	mWidth = metric.widthPixels;
	int height = metric.heightPixels;
	mWidthHalf = (mWidth/2);
	Log.e(TAG, "width:" + mWidth + " height:" + height);

        mContext = getApplicationContext();

	setContentView(R.layout.multimedia);
	setupViews();
	openFile();
    }

    public void setupViews() {
        mMyAdapter = new MultiMediaAdapter();
        mListView = (ListView) findViewById(R.id.mmlv);
	mListItem = new ArrayList<HashMap<String,Object>>();
        mListView.setAdapter(mMyAdapter);
    }

    private void openFile(){

	StringBuilder where = new StringBuilder();
	where.append(MediaStore.Images.Media.DATA + " like ?");
	String whereVal[] = {"%" + "IGlass/Thumbnails" + "%"};

	Cursor cs = this.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, picColu, where.toString(), whereVal, null);

	int id_idx = cs.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
	int path_idx = cs.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

	if (cs.moveToFirst()){
	    do {
		Long id = cs.getLong(id_idx);
		String path = cs.getString(path_idx);
		Log.e(TAG, "id:" + id + " path:" + path);

		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("id", id);
		map.put("path", path);
		mListItem.add(map);
		//Bitmap btmp = thumbBitmap(false, id, 0);
		
	    }while (cs.moveToNext());
	}
	cs.close();
    }

    public void startShowPic(Intent i){
	this.startActivity(i);
    }

    class MultiMediaAdapter extends BaseAdapter{

	@Override
	public int getCount() {
	    // TODO Auto-generated method stub
	    return (mListItem.size() + 1) / 2;
	    //return 1;
	}

	@Override
	public Object getItem(int arg0) {
	    // TODO Auto-generated method stub
	    return arg0;
	}

	@Override
	public long getItemId(int position) {
	    // TODO Auto-generated method stub
	    return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	    convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.mulstyle,null);

	      /*left*/
	    HashMap hm1 = mListItem.get(position * 2);
	    String path1 = (String)hm1.get((Object)"path");
	    Bitmap btmp1 = BitmapFactory.decodeFile(path1);
            ImageView mImageView1 = new ImageView(getApplicationContext());

	     mImageView1.setTag(path1);
	     mImageView1.setPadding(10,5,5,5);//l t r b
	     mImageView1.setImageBitmap(btmp1);
	     mImageView1.setScaleType(ImageView.ScaleType.FIT_XY);
	     mImageView1.setLayoutParams(new LayoutParams(mWidthHalf,mWidthHalf*3/4));
	     mImageView1.setOnClickListener(picOnclick);

	     if(check_video(path1)){
		 ImageView iv_tag = new ImageView(getApplicationContext());
		 iv_tag.setImageResource(R.drawable.ic_video_tag);
		 iv_tag.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
		 iv_tag.setScaleType(ImageView.ScaleType.CENTER);

		 FrameLayout fl= new FrameLayout(getApplicationContext());
		 fl.setLayoutParams(new LayoutParams(mWidthHalf,mWidthHalf*3/4));
		 fl.addView(mImageView1);
		 fl.addView(iv_tag);
		 ((LinearLayout)convertView).addView(fl);
	     }else{
		 ((LinearLayout)convertView).addView(mImageView1);
	     }

	    if (mListItem.size() < position * 2 + 2){
		return convertView;
	    }

	      /*right*/
	    HashMap hm2 = mListItem.get(position * 2 + 1);
	    String path2 = (String)hm2.get((Object)"path");
	    Bitmap btmp2 = BitmapFactory.decodeFile(path2);
            ImageView mImageView2 = new ImageView(getApplicationContext());

	    mImageView2.setTag(path2);
	    mImageView2.setPadding(5,5,10,5); //ltrb
	    mImageView2.setImageBitmap(btmp2);
	    mImageView2.setScaleType(ImageView.ScaleType.FIT_XY);
	    mImageView2.setLayoutParams(new LayoutParams(mWidthHalf,mWidthHalf*3/4));
	    mImageView2.setOnClickListener(picOnclick);

	    if(check_video(path2)){
		 ImageView iv_tag = new ImageView(getApplicationContext());
		 iv_tag.setImageResource(R.drawable.ic_video_tag);
		 iv_tag.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
		 iv_tag.setScaleType(ImageView.ScaleType.CENTER);

		 FrameLayout fl= new FrameLayout(getApplicationContext());
		 fl.setLayoutParams(new LayoutParams(mWidthHalf,mWidthHalf*3/4));
		 fl.addView(mImageView2);
		 fl.addView(iv_tag);
		 ((LinearLayout)convertView).addView(fl);
	     }else{
		 ((LinearLayout)convertView).addView(mImageView2);
	     }

	    Log.e(TAG, "--path1:" + path1+" --path2:"+path2);
	    return convertView;
	}

	public OnClickListener picOnclick = new OnClickListener(){
		@Override
		public void onClick(View v) {
		    String picPath = (String)v.getTag();
		    Log.e(TAG, "picPath:" + picPath);
			Intent intent = new Intent(mContext, OpenFileActivity.class);
			intent.putExtra("path", picPath);
			startActivity(intent);		    
		}
	    };

	private boolean check_video(String path){
	    if(path.toLowerCase().endsWith("_mp4_thumb.jpg")
	       || path.toLowerCase().endsWith("_3gp_thumb.jpg")
	       || path.toLowerCase().endsWith("_mkv_thumb.jpg")
	       || path.toLowerCase().endsWith("_avi_thumb.jpg")
	       || path.toLowerCase().endsWith("_mov_thumb.jpg")
	       || path.toLowerCase().endsWith("_wmv_thumb.jpg"))
		return true;
	    return false;
	}

    }
}