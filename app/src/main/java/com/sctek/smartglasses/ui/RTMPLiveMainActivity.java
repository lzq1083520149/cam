package com.sctek.smartglasses.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cn.zhongdun110.camlog.R;
import com.cn.zhongdun110.camlog.SyncApp;
import com.sctek.smartglasses.utils.Utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RTMPLiveMainActivity extends Activity implements OnItemClickListener {
	private static final String TAG = "RTMPLiveMainActivity";
	private boolean DEBUG = true;
	private final String URL_BASE = "http://glass.ingenic.com:5080/live/";
	private final String URL_GET_LIVE_LIST = URL_BASE + "getLivePushList";

	private final String PARAMETER_CLIENT_UID = "ClientUid";
	private final String PARAMETER_KEYLIST = "KeyList";
	private final String PARAMETER_CMP_UID = "CmpUid";
	private Context mContext;
	private String mClientUid;
	private ListView mLiveListView;
	private List<String> mLiveRoomList = new ArrayList<String>();
	private LiveRoomAdapter mLiveRoomAdapter;
	private ProgressDialog mProgressDialog;

	private	final int MSG_GET_LIVELIST_SUCCESS = 1;
	private	final int MSG_GET_LIVELIST_FAIL = 2;
	private	final int MSG_GET_LIVELIST_NONE = 3;
	private Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_GET_LIVELIST_SUCCESS:
					hideProgress();
					mLiveRoomAdapter.notifyDataSetChanged();
					break;
				case MSG_GET_LIVELIST_FAIL:
					mLiveRoomList.clear();
					mLiveRoomAdapter.notifyDataSetChanged();
					hideProgress();
					Toast.makeText(mContext, getString(R.string.rtmp_livelist_get_fail), Toast.LENGTH_LONG).show();
					break;
				case MSG_GET_LIVELIST_NONE:
					mLiveRoomAdapter.notifyDataSetChanged();
					hideProgress();
					Toast.makeText(mContext, getString(R.string.rtmp_livelist_none), Toast.LENGTH_LONG).show();
					break;
				default:
					break;
			}
		};
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rtmplive_main);
		mContext = this;
		mLiveRoomAdapter = new LiveRoomAdapter(this);
		mClientUid = Utils.getClientUid(mContext);
		Log.e(TAG, "clientuid="+mClientUid);
		initView();
		getLiveList();
		showProgress(getString(R.string.rtmp_livelist_getting));
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		getLiveList();
	}
	@Override
	protected void onStart() {
		super.onStart();
	}

	private void initView() {
		mLiveListView = (ListView)findViewById(R.id.live_listview);
		mLiveListView.setAdapter(mLiveRoomAdapter);
		mLiveListView.setOnItemClickListener(this);
		mProgressDialog = new ProgressDialog(this);
	}

	private void showProgress(String msg){
		mProgressDialog.setMessage(msg);
		mProgressDialog.show();
	}

	@Override
	protected void onPause() {
		super.onPause();
		hideProgress();
	}

	private void hideProgress(){
		if(mProgressDialog.isShowing())
			mProgressDialog.dismiss();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
							long arg3) {

		String name = mLiveRoomList.get(arg2);
		if(DEBUG)Log.d(TAG,"onItemClick name="+name);
		Intent intent = new Intent(RTMPLiveMainActivity.this,RTMPLivePlayActivity.class);
		intent.putExtra("room_name", name);
		startActivity(intent);
	}

	private void getLiveList() {
		if(DEBUG )Log.i(TAG, "getLiveList");
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					boolean getLiveListSuccess = false;
					StringBuilder getLiveUrl = new StringBuilder(
							URL_GET_LIVE_LIST + "?");
					getLiveUrl.append(PARAMETER_CLIENT_UID + "="+mClientUid);
					getLiveUrl.append("&" + PARAMETER_CMP_UID + "=" +SyncApp.COMPANY_UID);
					HttpGet httpGet = new HttpGet(getLiveUrl.toString());
					HttpClient httpClient = new DefaultHttpClient();
					HttpResponse httpResponse = httpClient.execute(httpGet);
					Log.i(TAG, "statusCode="
							+ httpResponse.getStatusLine().getStatusCode());
					if (httpResponse.getStatusLine().getStatusCode() == 200) {
						HttpEntity entity = httpResponse.getEntity();
						String response = EntityUtils.toString(entity, "utf-8");
						JSONObject resp = new JSONObject(response);
						JSONArray array = resp.getJSONArray(PARAMETER_KEYLIST);
						List<String> list = new ArrayList<String>();
						if (array != null) {
							for(int i =0;i<array.length();i++){
								if(DEBUG)Log.i(TAG,"i,"+array.getString(i));
								list.add(array.getString(i));
							}
							mLiveRoomList = list;
							if(mLiveRoomList.size() > 0)
								mHandler.sendEmptyMessage(MSG_GET_LIVELIST_SUCCESS);
							else
								mHandler.sendEmptyMessage(MSG_GET_LIVELIST_NONE);
						}else {
							mHandler.sendEmptyMessage(MSG_GET_LIVELIST_FAIL);
						}
					} else {
						mHandler.sendEmptyMessage(MSG_GET_LIVELIST_FAIL);
					}
				} catch (Exception e) {
					mHandler.sendEmptyMessage(MSG_GET_LIVELIST_FAIL);
					e.printStackTrace();
				}
			}
		}).start();
	}

	public class LiveRoomAdapter extends BaseAdapter{
		LayoutInflater inflater;
		public LiveRoomAdapter(Context context){
			this.inflater=LayoutInflater.from(context);
		}
		@Override
		public int getCount() {
			if(null == mLiveRoomList){
				return 0;
			}
			return mLiveRoomList.size();
		}
		@Override
		public Object getItem(int position) {
			return position;
		}
		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;
			final int mPosition = position ;
			if(convertView == null){
				holder=new ViewHolder();
				convertView = inflater.inflate(R.layout.rtmplive_list_item, null);
				holder.roomName = (TextView)convertView.findViewById(R.id.roomName);
				convertView.setTag(holder);
			}else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.roomName.setText(mLiveRoomList.get(position));
			return convertView;
		}
	}

	class ViewHolder {
		TextView roomName;
	}


}

