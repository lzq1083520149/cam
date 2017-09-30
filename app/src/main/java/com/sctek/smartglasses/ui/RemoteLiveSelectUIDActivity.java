package com.sctek.smartglasses.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cn.zhongdun110.camlog.R;
import com.sctek.smartglasses.db.DBRemoteLive;
import com.sctek.smartglasses.db.RemLiveSQLiteOpenHelper;
import com.sctek.smartglasses.entity.RemoteLiveBean;

import java.util.List;

public class RemoteLiveSelectUIDActivity extends Activity implements
		OnClickListener, TextWatcher, OnItemClickListener {
	private static final String TAG = "RemoteLiveSelectUIDActivity";
	private EditText mEdTtUid;
        private TextView mTvEnsure,mTvClear;
	private ListView mListViewUid;
	private RemLiveSQLiteOpenHelper mRemLiveSQLiteOpenHelper;
	private DBRemoteLive mDBRemoteLive;
	private List<RemoteLiveBean> mRemoteLiveBeanList;
	private RemoteLiveBean mRemoteLiveBeanSelect;
	private MyAdapter mAdapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_remlive_selectuid);
		setTitle(R.string.glass_live);
		mRemLiveSQLiteOpenHelper = RemLiveSQLiteOpenHelper.getInstance(this);
		mDBRemoteLive = new DBRemoteLive(mRemLiveSQLiteOpenHelper.getReadableDatabase());
		mRemoteLiveBeanList = mDBRemoteLive.findAll();
		initView();
	}

	private void initView() {
		mEdTtUid = (EditText)findViewById(R.id.et_uid);
		mEdTtUid.addTextChangedListener(this);
		mTvEnsure = (TextView)findViewById(R.id.tv_ensure);
		mTvEnsure.setOnClickListener(this);
		mListViewUid = (ListView)findViewById(R.id.listview_uid);
		mAdapter = new MyAdapter(this);
		mListViewUid.setAdapter(mAdapter);
		mListViewUid.setOnItemClickListener(this);
		mTvClear = (TextView)findViewById(R.id.tv_clear_history);
		mTvClear.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
		case R.id.tv_ensure:
			if(mEdTtUid.getText().toString().length()==0){
				Toast.makeText(this, R.string.please_input,
						Toast.LENGTH_SHORT).show();
			}else{
				Intent intent = new Intent();
			    intent.putExtra(RemLiveSQLiteOpenHelper.COL_UID, mEdTtUid.getText().toString());
			    setResult(RESULT_OK, intent);
				finish();
			}
			break;
		case R.id.tv_clear_history:
		        mDBRemoteLive.deleteAll();
		        mRemoteLiveBeanList = mDBRemoteLive.findAll();
		        mAdapter.notifyDataSetChanged();
		        break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    switch (keyCode) {
	      case KeyEvent.KEYCODE_BACK:
	    	  	setResult(RESULT_CANCELED, new Intent());
	    	  	finish();
	    	  	return true;
	    }
	    return super.onKeyDown(keyCode, event);
	  }
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		mRemoteLiveBeanSelect = mRemoteLiveBeanList.get(arg2);
		mEdTtUid.setText(mRemoteLiveBeanSelect.getUid());
	}
	
	@Override
	public void afterTextChanged(Editable arg0) {
		
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
			int arg3) {
		
	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		
	}

	public class MyAdapter extends BaseAdapter{
		LayoutInflater inflater;
		public MyAdapter(Context context){
			this.inflater=LayoutInflater.from(context);
		}
		@Override
		public int getCount() {
		    if(null == mRemoteLiveBeanList){
			return 0;
		    }
			return mRemoteLiveBeanList.size();
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
		    	convertView = inflater.inflate(R.layout.wifi_scan_item, null);
		    	holder.remoteliveName = (TextView)convertView.findViewById(R.id.wifiName);
		    	holder.remoteliveUid = (TextView)convertView.findViewById(R.id.wifiSecurity);
		    	convertView.setTag(holder);
		    }else {
		    	holder = (ViewHolder) convertView.getTag();
		    }
		    holder.remoteliveName.setText(mRemoteLiveBeanList.get(position).getName());
			holder.remoteliveUid.setText(mRemoteLiveBeanList.get(position).getUid());
			 	return convertView;
		}
	}
		
        class ViewHolder {
	        TextView remoteliveName,remoteliveUid;
	}

}
