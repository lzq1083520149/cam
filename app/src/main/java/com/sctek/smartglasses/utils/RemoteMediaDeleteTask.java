package com.sctek.smartglasses.utils;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.cn.zhongdun110.camlog.R;
import com.sctek.smartglasses.fragments.BaseFragment.ImageAdapter;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.util.ArrayList;
import java.util.Iterator;

public class RemoteMediaDeleteTask extends AsyncTask<String, Integer, Void> {

	private final static String TAG = "RemoteMediaDeleteTask";
	private ProgressDialog mProgressDialog;
	private Context mContext;
	private ArrayList<MediaData> mMedias;
	private ArrayList<MediaData> mSelectedMedias;
	private ImageAdapter mAdapter;
	
	@SuppressLint("NewApi")
	public RemoteMediaDeleteTask (Context context, ArrayList<MediaData> medias, ArrayList<MediaData> selectMedias, ImageAdapter adapter) {
		mProgressDialog = new ProgressDialog(context);
		mContext = context;
		mMedias = medias;
		mSelectedMedias = selectMedias;
		mAdapter = adapter;
	}
	@SuppressLint("NewApi")
	@Override
	protected void onPreExecute() {
		// TODO Auto-generated method stub
		mProgressDialog.setMessage(mContext.getResources().getText(R.string.deleting));
		mProgressDialog.show();
		super.onPreExecute();
	}
	
	@Override
	protected void onPostExecute(Void result) {
		// TODO Auto-generated method stub
		mProgressDialog.dismiss();
		super.onPostExecute(result);
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		// TODO Auto-generated method stub
		if(values[1] == 0) {
			Toast.makeText(mContext, R.string.delete_error, Toast.LENGTH_LONG).show();
		}
		else
		{
			onMediaDeleted();
			if(values[0] == 0)
				Toast.makeText(mContext, R.string.delete_ok, Toast.LENGTH_LONG).show();
			else {
				String msg = String.format((String)mContext.getResources().getText(R.string.delete_fail_count), values[0]);
				Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
			}
		}
		super.onProgressUpdate(values);
	}
	
	@Override
	protected Void doInBackground(String... params) {
		// TODO Auto-generated method stub
		Iterator<MediaData> mediaIterator = mSelectedMedias.iterator();
		int errors = 0;
		int dones = 0;
		while(mediaIterator.hasNext()) {
			
			String urlPref = String.format("http://%s/cgi-bin/deletefiles?", params[1]);
			StringBuffer urlBuffer = new StringBuffer(1024);
			urlBuffer.append(urlPref);
			urlBuffer.append(params[0]);
			
			int i = 0;
			for(; i<100&&mediaIterator.hasNext(); i++) {
				MediaData data = mediaIterator.next();
				urlBuffer.append("&" + data.name);
			}
			
			Log.e(TAG, "delete url " + urlBuffer.toString());
			try {
				
				HttpClient httpClient = CustomHttpClient.getHttpClient();
				HttpGet httpGet = new HttpGet(urlBuffer.toString());
				int delcount = GlassImageDownloader.deleteRequestExecute(httpClient, httpGet);
				
				if(delcount < 0) {
					errors += i;
				}
				else {
					errors += i - delcount;
					dones += delcount;
				}
				
			} catch (Exception e) {
				e.printStackTrace();
//				publishProgress(-1);
//				return null;
			}
			
		}
		
		Log.e(TAG, "=======errors:" + errors + " =======dones:" + dones);
		publishProgress(errors, dones);
		return null;
	}
	
	
	public void onMediaDeleted() {
		
		for(MediaData md : mSelectedMedias) {
			int i = mMedias.indexOf(md);
			if(i != -1)
				mMedias.remove(i);
		}
		
		mSelectedMedias.clear();
		mAdapter.notifyDataSetChanged();
	}
	
}
