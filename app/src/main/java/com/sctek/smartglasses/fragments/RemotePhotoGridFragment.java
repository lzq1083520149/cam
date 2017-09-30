package com.sctek.smartglasses.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.cn.zhongdun110.camlog.MediaSyncService;
import com.cn.zhongdun110.camlog.R;
import com.sctek.smartglasses.ui.PhotoActivity;
import com.sctek.smartglasses.utils.CustomHttpClient;
import com.sctek.smartglasses.utils.DownloadUtil;
import com.sctek.smartglasses.utils.GlassImageDownloader;
import com.sctek.smartglasses.utils.MediaData;
import com.sctek.smartglasses.utils.RemoteMediaDeleteTask;
import com.sctek.smartglasses.utils.WifiUtils;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import cn.ingenic.glasssync.devicemanager.WifiManagerApi;

public class RemotePhotoGridFragment extends BaseFragment {

    public static final int FRAGMENT_INDEX = 3;
    public static final int PHOTO_NOTIFICATION_ID = 0;

    private static final String TAG = RemotePhotoGridFragment.class.getName();
    private static final String REMOTE_PHOTO_URL = "http://192.168.5.122:7766/sdcard/DCIM/Camera/";

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");

        mediaList = new ArrayList<MediaData>();
        nativeMediaList = getArguments().getParcelableArrayList("photos");
        preApState = WifiUtils.getWifiAPState(mWifiManager);
        // mWifiATask = new SetWifiAPTask(true, false);

        getActivity().setTitle(R.string.remote_photo);

        IntentFilter filter = new IntentFilter(WIFI_AP_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mApStateBroadcastReceiver, filter);

        initProgressDialog();

        if (preApState == WIFI_AP_STATE_ENABLED)
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    // if(WifiUtils.needTurnWifiApOff(getActivity())) {
                    // 	mWifiATask.execute(true);

                    // }
                    // else {
                    sendApInfoToGlass();
                    // }
                }
            }, 0);

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

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        Log.e(TAG, "onDestroy");

        mContext.unregisterReceiver(mApStateBroadcastReceiver);

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

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.e(TAG, "onCreateOptionsMenu");
        inflater.inflate(R.menu.remote_photo_fragment_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.e(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
            case R.id.download_item:
                deleteView.setVisibility(View.VISIBLE);
                selectAllView.setVisibility(View.VISIBLE);

                if (mediaList.size() > 0) {
                    for (CheckBox cb : checkBoxs) {
                        cb.setVisibility(View.VISIBLE);
                    }
                }

                deleteTv.setText(R.string.download);
                deleteTv.setOnClickListener(onPhotoDownloadClickListener);
                return true;
            case R.id.remote_photo_delete_item:

                deleteView.setVisibility(View.VISIBLE);
                selectAllView.setVisibility(View.VISIBLE);

                for (CheckBox cb : checkBoxs) {
                    cb.setVisibility(View.VISIBLE);
                }
                deleteTv.setText(R.string.delete);
                deleteTv.setOnClickListener(onRemotePhotoDeleteClickListener);
                return true;
            default:
                return true;
        }
    }

//	private void getImagePath(final String ip) {
//		
//		String uri = "http://" + ip + "/data/apache/cgi-bin/listfiles";
//		final HttpClient httpClient = CustomHttpClient.getHttpClient();
//		final HttpGet httpGet = new HttpGet(uri);
//		new Thread(new Runnable() {
//			
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				String result = httpRequestExecute(httpClient, httpGet);
//				
//				if(result != null) {
//					ArrayList<String> names = getImageNames(result);
//					imagesName = new String[names.size()];
//					names.toArray(imagesName);
//					for(int i = 0; i<names.size(); i++) {
//						String url = "http://" + ip + "/data/" + names.get(i);
//						imageUrls.add(url);
//					}
//					getActivity().sendBroadcast(new Intent(GET_IMAGESURL_DONE));
//				}
//			}
//		}).start();
//		
//	}
//	

//	
//	private ArrayList<String> getImageNames(String result) {
//		try {
//			XmlContentHandler xmlHandler = new XmlContentHandler();
//			StringReader reader = new StringReader(result);
//			SAXParserFactory factory = SAXParserFactory.newInstance();    
//			SAXParser parser = factory.newSAXParser();    
//			XMLReader xmlReader = parser.getXMLReader(); 
//			xmlReader.setContentHandler(xmlHandler);
//			xmlReader.parse(new InputSource(reader));
//			
//			return xmlHandler.getNames();
//		} catch (Exception e) {
//			e.printStackTrace();
//			return null;
//		}
//	}

    private OnClickListener onPhotoDownloadClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (selectedMedias.size() != 0)
                onPhotoDownloadTvClicked();

            disCheckMedia();
            onCancelTvClicked();
        }
    };

    private OnClickListener onRemotePhotoDeleteClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (selectedMedias.size() != 0) {
                showDeleteConfirmDialog();
            }

        }
    };

    private BroadcastReceiver mApStateBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if (WIFI_AP_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                int cstate = intent.getIntExtra(EXTRA_WIFI_AP_STATE, -1);
                Log.e(TAG, WIFI_AP_STATE_CHANGED_ACTION + ":" + cstate);
                if (cstate == WIFI_AP_STATE_ENABLED
                        && preApState != WIFI_AP_STATE_ENABLED) {

                    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                    if (!adapter.isEnabled()) {
                        adapter.enable();
                    }
                    WifiManagerApi mWifiManagerApi = new WifiManagerApi(getActivity());
                    WifiConfiguration mWifiConfiguration = mWifiManagerApi.getWifiApConfiguration();
                    Log.e(TAG, "ssid:" + mWifiConfiguration.SSID + "password:" + mWifiConfiguration.preSharedKey);
                    sendApInfoToGlass();
                }
                preApState = cstate;
            }
        }

    };

    public void onPhotoDownloadTvClicked() {

        //new PhotoDownloadTask().execute();
        //getActivity().startService(new Intent(getActivity(), MediaSyncService.class));
        final ArrayList<MediaData> data = (ArrayList<MediaData>) selectedMedias.clone();
        final ArrayList<Integer> selectedPossition = (ArrayList<Integer>) selectedPosition.clone();
        //((PhotoActivity)getActivity()).startPhotoSync(data);
        new Thread(new DownloadThread(data, selectedPossition,"photos")).start();
    }


    public void onPhotoDeleteTvClicked() {
        ArrayList<MediaData> data = (ArrayList<MediaData>) selectedMedias.clone();
        new RemoteMediaDeleteTask(getActivity(),
                mediaList, data, mImageAdapter).execute(new String[]{"photos", glassIp});
    }

    private class PhotoDownloadTask extends AsyncTask<String, Integer, Void> {

        private ProgressDialog progressDialog;
        private int totalcount;
        private int downloadcount;
        private GlassImageDownloader imageDownloader;

        private NotificationManager notificationManager;
        private Notification notification;

        @SuppressLint("NewApi")
        public PhotoDownloadTask() {

            progressDialog = new ProgressDialog(getActivity());
            totalcount = selectedMedias.size();
            downloadcount = 0;

            imageDownloader = new GlassImageDownloader();

            notificationManager = (NotificationManager) (getActivity().getSystemService(mContext.NOTIFICATION_SERVICE));
            notification = new Notification();

        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();

            String msg = String.format("照片同步中(%d/%d)...", downloadcount, totalcount);

            notification.contentView = new RemoteViews(mContext.getPackageName(), R.layout.notification_view);
            notification.icon = R.drawable.ic_stub;
            notification.contentView.setProgressBar(R.id.donwload_progress, 100, 100, true);
            notification.contentView.setTextViewText(R.id.download_lable_tv, msg);
            notificationManager.notify(PHOTO_NOTIFICATION_ID, notification);


            progressDialog.setMessage(msg);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            progressDialog.dismiss();

            String msg = String.format("同步完成(%d/%d)", downloadcount, totalcount);
            notification.contentView.setTextViewText(R.id.download_lable_tv, msg);
            notification.vibrate = new long[]{0, 100, 200, 300};
            notificationManager.notify(PHOTO_NOTIFICATION_ID, notification);

            if (downloadcount != 0) {
                refreshGallery("photos");
            }
            disCheckMedia();
//			selectedMedias.clear();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // TODO Auto-generated method stub
            super.onProgressUpdate(values);
            String msg = String.format("照片同步中(%d/%d)...", downloadcount, totalcount);
            notification.contentView.setTextViewText(R.id.download_lable_tv, msg);
            notificationManager.notify(PHOTO_NOTIFICATION_ID, notification);
            progressDialog.setMessage(msg);
        }

        @Override
        protected Void doInBackground(String... params) {
            Log.e(TAG, "PhotoDownloadTask");
            for (int i = 0; i < selectedMedias.size(); i++) {
                MediaData data = selectedMedias.get(i);

                try {

                    InputStream in = imageDownloader.getInputStream(data.url, 0);

                    File dir = new File(PHOTO_DOWNLOAD_FOLDER);
                    if (!dir.exists())
                        dir.mkdirs();

                    File file = new File(PHOTO_DOWNLOAD_FOLDER, data.name);
                    if (file.exists()) {
                        downloadcount++;
                        publishProgress();
                        in.close();
                        continue;
                    }

                    byte[] buffer = new byte[1024];
                    int len = 0;

                    FileOutputStream os = new FileOutputStream(file);
                    while ((len = in.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                    downloadcount++;
                    publishProgress();

                    os.flush();
                    os.close();
                    in.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

    }

    private class PhotoDeleteTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            mDeleteProgressDialog.setMessage(getActivity().getResources().getText(R.string.deleting));
            mDeleteProgressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onPostExecute");
            if (mDeleteProgressDialog.isShowing())
                mDeleteProgressDialog.cancel();
            super.onPostExecute(result);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // TODO Auto-generated method stub
            super.onProgressUpdate(values);
            if (values[1] == 0) {
                Toast.makeText(mContext, R.string.connect_error, Toast.LENGTH_LONG).show();
                disCheckMedia();
            } else {
                onMediaDeleted();
                if (values[0] == 0)
                    Toast.makeText(mContext, R.string.delete_ok, Toast.LENGTH_LONG).show();
                else {
                    String msg = String.format((String) getActivity().getResources().getText(R.string.delete_fail_count), values[0]);
                    Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            // TODO Auto-generated method stub
            Iterator<MediaData> mediaIterator = selectedMedias.iterator();
            int errors = 0;
            int dones = 0;
            while (mediaIterator.hasNext()) {

                String urlPref = String.format("http://%s/cgi-bin/deletefiles?", glassIp);
                StringBuffer urlBuffer = new StringBuffer(1024);
                urlBuffer.append(urlPref);
                urlBuffer.append("photos");

                int i = 0;
                for (; i < 200 && mediaIterator.hasNext(); i++) {
                    MediaData data = mediaIterator.next();
                    urlBuffer.append("&" + data.name);
                }

                Log.e(TAG, "delete url " + urlBuffer.toString());

                HttpClient httpClient = CustomHttpClient.getHttpClient();
                HttpGet httpGet = new HttpGet(urlBuffer.toString());
                int error = GlassImageDownloader.deleteRequestExecute(httpClient, httpGet);

                if (error < 0) {
                    errors += i;
                } else {
                    errors += error;
                    dones += (i - error);
                }

            }
            Log.e(TAG, "=======errors:" + errors + " =======dones:" + dones);
            publishProgress(errors, dones);
            return null;
        }

    }

    public void initProgressDialog() {

        mConnectProgressDialog = new ProgressDialog(getActivity());
        mConnectProgressDialog.setTitle(R.string.remote_photo);
        mConnectProgressDialog.setCancelable(false);
        mConnectProgressDialog.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                // TODO Auto-generated method stub
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.cancel();
                    getActivity().onBackPressed();
                }
                return false;
            }
        });
    }

    private void showDeleteConfirmDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.delete);
        builder.setMessage(R.string.delete_message);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                onPhotoDeleteTvClicked();

                disCheckMedia();
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
