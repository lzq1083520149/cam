package com.sctek.smartglasses.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cn.zhongdun110.camlog.MediaSyncService;
import com.cn.zhongdun110.camlog.R;
import com.cn.zhongdun110.camlog.multimedia.MultiMedia;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.sctek.smartglasses.control.RestartApacheModule;
import com.sctek.smartglasses.ui.PhotoActivity;
import com.sctek.smartglasses.ui.VideoActivity;
import com.sctek.smartglasses.utils.CamlogCmdChannel;
import com.sctek.smartglasses.utils.CustomHttpClient;
import com.sctek.smartglasses.utils.DownloadUtil;
import com.sctek.smartglasses.utils.MediaData;
import com.sctek.smartglasses.utils.MultiMediaScanner;
import com.sctek.smartglasses.utils.PhotosSyncRunnable;
import com.sctek.smartglasses.utils.RemoteMediaDeleteTask;
import com.sctek.smartglasses.utils.WifiUtils;
import com.sctek.smartglasses.utils.WifiUtils.WifiCipherType;
import com.sctek.smartglasses.utils.XmlContentHandler;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

@SuppressLint("NewApi")
public class BaseFragment extends Fragment {

    public static final String TAG = BaseFragment.class.getName();
    public static final boolean DEBUG = true;
    public static final String URL_PREFIX = "http://192.168.5.122/";

    public static final String PHOTO_DOWNLOAD_FOLDER =
            Environment.getExternalStorageDirectory().toString() + "/Camlog/photos/";
    public static final String VIDEO_DOWNLOAD_FOLDER =
            Environment.getExternalStorageDirectory().toString() + "/Camlog/videos";

    public static final String EXTERNEL_DIRCTORY_PATH =
            Environment.getExternalStorageDirectory() + "/Camlog/photos/";

    protected static final int WIFI_AP_STATE_DISABLED = 11;
    protected static final int WIFI_AP_STATE_ENABLED = 13;

    public static final int RESEDN_CONNECT_WIFI_MSG = 5;

    public static final String WIFI_AP_STATE_CHANGED_ACTION =
            "android.net.wifi.WIFI_AP_STATE_CHANGED";
    public static final String EXTRA_WIFI_AP_STATE = "wifi_state";
    public static final String EXTRA_PREVIOUS_WIFI_AP_STATE = "previous_wifi_state";

    private static int failedMultimediaSyncCount = 0;
    private static Object failedMultimediaSyncCount_Lock = new Object();

    public ArrayList<MediaData> mediaList;

    public ArrayList<MediaData> selectedMedias;

    public ArrayList<MediaData> nativeMediaList;

    public ArrayList<CheckBox> checkBoxs;

    private DisplayImageOptions options;

    public View deleteView;
    public View selectAllView;

    public boolean showImageCheckBox;
    public boolean wifi_msg_received = false;

    public ImageAdapter mImageAdapter;

    public TextView deleteTv;
    public TextView cancelTv;
    protected CheckBox selectAllCb;

    private int childIndex;
    private int selectMediasSize;

    protected WifiManager mWifiManager;
    public CamlogCmdChannel mCamlogCmdChannel;

    public Context mContext;
    protected int preApState;
    protected SetWifiAPTask mWifiATask;
    private GetRemoteMediaUrlTask mMediaUrlTask;

    public ProgressDialog mConnectProgressDialog;
    public ProgressDialog mDeleteProgressDialog;
    public String glassIp;

    public MediaData shareVideo;
    public CheckBox selectedCb;
    public ArrayList<CheckBox> shareCheckBoxs;


    private GridView grid;

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");
        setHasOptionsMenu(true);

        mContext = (Context) getActivity().getApplicationContext();
        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.ic_stub)
                .resetViewBeforeLoading(true)
                .cacheOnDisk(true)
                .imageScaleType(ImageScaleType.EXACTLY)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .considerExifParams(true)
                .displayer(new FadeInBitmapDisplayer(300))
                .build();

        childIndex = getArguments().getInt("index");
        selectedMedias = new ArrayList<MediaData>();
        showImageCheckBox = false;
        mImageAdapter = new ImageAdapter();
        checkBoxs = new ArrayList<CheckBox>();
        shareCheckBoxs = new ArrayList<CheckBox>();


        mMediaUrlTask = new GetRemoteMediaUrlTask();
        mDeleteProgressDialog = new ProgressDialog(getActivity());

        mWifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiATask = new SetWifiAPTask(true, false);
        mCamlogCmdChannel = CamlogCmdChannel.getInstance(mContext);


    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.e(TAG, "onCreateView");

        mCamlogCmdChannel.registerHandler("BaseFragment", mChannelHandler);
        View view = inflater.inflate(R.layout.fragment_image_grid, container, false);

        selectAllView = view.findViewById(R.id.select_all_lo);
        deleteView = view.findViewById(R.id.delete_bt_lo);
        deleteTv = (TextView) view.findViewById(R.id.delete_tv);
        cancelTv = (TextView) view.findViewById(R.id.cancel_tv);
        selectAllCb = (CheckBox) view.findViewById(R.id.select_all_cb);

        cancelTv.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onCancelTvClicked();
                disCheckMedia();

                cancelShareSelectView();

            }
        });

        selectAllCb.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    selectedPosition.clear();
                    int i =0;
                    for (MediaData md : mediaList) {
                        selectedPosition.add(i++);
                        if (!selectedMedias.contains(md))
                            selectedMedias.add(md);
                    }
                    selectMediasSize = selectedMedias.size();
                    for (CheckBox cb : checkBoxs) {
                        cb.setChecked(true);
                    }
                } else {
                    if (selectedMedias.size() == selectMediasSize) {
                        for (CheckBox cb : checkBoxs)
                            cb.setChecked(false);
                        selectedPosition.clear();
                        selectedMedias.clear();
                    }
                }
            }
        });

        grid = (GridView) view.findViewById(R.id.grid);
        grid.setAdapter(mImageAdapter);

        switch (childIndex) {

            case NativePhotoGridFragment.FRAGMENT_INDEX:
                grid.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), true, false));
                grid.setOnItemClickListener(onPhotoImageClickedListener);
                break;
            case RemotePhotoGridFragment.FRAGMENT_INDEX:
                grid.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), true, false));
                grid.setOnItemClickListener(onPhotoImageClickedListener);
                if (WIFI_AP_STATE_ENABLED != WifiUtils.getWifiAPState(mWifiManager))
                    showTurnWifiApOnDialog();
                break;
            case NativeVideoGridFragment.FRAGMENT_INDEX:
                grid.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), true, false));
                grid.setOnItemClickListener(onVideoImageClickedListener);
                break;
            case RemoteVideoGridFragment.FRAGMENT_INDEX:
                grid.setOnItemClickListener(onVideoImageClickedListener);
                if (WIFI_AP_STATE_ENABLED != WifiUtils.getWifiAPState(mWifiManager))
                    showTurnWifiApOnDialog();
                break;
        }

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
        try {
            super.onResume();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        mMediaUrlTask.cancel(true);
        mDialogHandler.removeMessages(RESEDN_CONNECT_WIFI_MSG);
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        // TODO Auto-generated method stub
        Log.e(TAG, "onDestroyView");
        mCamlogCmdChannel.unregisterHandler("BaseFragment");
        checkBoxs.clear();
        shareCheckBoxs.clear();
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        // TODO Auto-generated method stub
        Log.e(TAG, "onDetach");
        super.onDetach();
    }


    public class ImageAdapter extends BaseAdapter {

        private LayoutInflater inflater;
        int i = 0;
        int j = 0;

        ImageAdapter() {
            inflater = LayoutInflater.from(mContext);
        }

        @Override
        public int getCount() {
            return mediaList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //Log.e(TAG, "getView");
            ViewHolder holder = new ViewHolder();
            final int mPositoin = position;
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.image_grid_item, parent, false);
                view.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                assert view != null;
                holder.imageView = (ImageView) view.findViewById(R.id.image);
                holder.progressBar = (ProgressBar) view.findViewById(R.id.progress);
                holder.imageName = (TextView) view.findViewById(R.id.image_name_tv);
                holder.imageCb = (CheckBox) view.findViewById(R.id.image_select_cb);
                holder.shareCb = (CheckBox) view.findViewById(R.id.video_share_cb);
                holder.downloadedTv = (TextView) view.findViewById(R.id.downloaded_tv);
                holder.rl_back = (RelativeLayout) view.findViewById(R.id.rl_back);
                holder.tv_download = (TextView) view.findViewById(R.id.xia_zai_zhuang_tai);
                holder.download_pgb = (ProgressBar) view.findViewById(R.id.download_pgb);


                view.setTag(holder);
                Log.e(TAG, "getView: setTag  " + position + "   " + i++);

                checkBoxs.add(holder.imageCb);
                shareCheckBoxs.add(holder.shareCb);
            } else {
                holder = (ViewHolder) view.getTag();
                Log.e(TAG, "getView: getTag  " + position + "   " + j++);
            }

            holder.shareCb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                private int mediaIndex = mPositoin;

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // TODO Auto-generated method stub
                    if (isChecked) {
                        shareVideo = mediaList.get(mediaIndex);
                        if (selectedCb != null)
                            selectedCb.setChecked(false);
                        selectedCb = (CheckBox) buttonView;
                    } else {
                        if (shareVideo == mediaList.get(mediaIndex)) {
                            shareVideo = null;
                            selectedCb = null;
                        }
                    }
                }
            });

            holder.imageCb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                private int imageIndex = mPositoin;

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // TODO Auto-generated method stub
                    try {
                        if (isChecked) {
                            if (!selectedMedias.contains(mediaList.get(imageIndex))) {
                                selectedMedias.add(mediaList.get(imageIndex));
                                selectedPosition.add(imageIndex);
                            }

                            if (selectedMedias.size() == mediaList.size()) {
                                selectAllCb.setChecked(true);
                            }
                            Log.e(TAG, "onCheckedChanged: position: " + imageIndex);
                        } else {
                            selectedPosition.remove(imageIndex);
                            selectedMedias.remove(mediaList.get(imageIndex));
                            if (selectAllCb.isChecked()) {
                                selectAllCb.setChecked(false);
                            }
                        }
                    } catch (IndexOutOfBoundsException expected) {

                    }
                }
            });

            if (selectedMedias.contains(mediaList.get(mPositoin))) {
                holder.imageCb.setChecked(true);
            } else {
                holder.imageCb.setChecked(false);
            }

            if (mediaList.get(mPositoin).equals(shareVideo)) {
                holder.shareCb.setChecked(true);
            } else {
                holder.shareCb.setChecked(false);
            }

            if (childIndex == RemotePhotoGridFragment.FRAGMENT_INDEX ||
                    childIndex == RemoteVideoGridFragment.FRAGMENT_INDEX) {

                if (!mediaList.get(position).mdownload.equals("null") ||
                        isDownloaded(mediaList.get(position).getName())) {
                    holder.downloadedTv.setText(R.string.media_downloaded);
                } else {
                    holder.downloadedTv.setText("");
                }

            }

            String url = getImageLoadUrl(position);
            // Log.e(TAG, "getView: url : "+url );
            final ViewHolder finalHolder = holder;
            final ViewHolder finalHolder1 = holder;
            ImageLoader.getInstance()
                    .displayImage(url, holder.imageView, options, new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingStarted(String imageUri, View view) {
                            finalHolder.progressBar.setProgress(0);
                            finalHolder.progressBar.setVisibility(View.VISIBLE);
                            finalHolder.imageName.setVisibility(View.GONE);
                        }

                        @Override
                        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                            try {
                                finalHolder.progressBar.setVisibility(View.GONE);
                                finalHolder.imageName.setVisibility(View.VISIBLE);
                                finalHolder.imageName.setText(mediaList.get(mPositoin).name);
                            } catch (IndexOutOfBoundsException e) {
                            }
                        }

                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            try {
                                finalHolder.progressBar.setVisibility(View.GONE);
                                finalHolder.imageName.setVisibility(View.VISIBLE);
                                finalHolder.imageName.setText(mediaList.get(mPositoin).name);
                            } catch (IndexOutOfBoundsException e) {
                            }
                        }
                    }, new ImageLoadingProgressListener() {
                        @Override
                        public void onProgressUpdate(String imageUri, View view, int current, int total) {
                            finalHolder1.progressBar.setProgress(Math.round(100.0f * current / total));
                        }
                    });

            return view;
        }
    }


    private String getImageLoadUrl(int position) {
        if (childIndex != RemoteVideoGridFragment.FRAGMENT_INDEX)
            return mediaList.get(position).url;
        else
            return mediaList.get(position).url.replace("videos", ".videothumbnails");
    }

    static class ViewHolder {
        ImageView imageView;
        ProgressBar progressBar;
        TextView imageName;
        CheckBox imageCb;
        CheckBox shareCb;
        TextView downloadedTv;
        RelativeLayout rl_back;
        TextView tv_download;
        ProgressBar download_pgb;
    }

    private OnItemClickListener onPhotoImageClickedListener = new OnItemClickListener() {

        @SuppressLint("NewApi")
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            // TODO Auto-generated method stub
            if (childIndex == NativePhotoGridFragment.FRAGMENT_INDEX) {
                shownOnNativePhotoClickedDialog(position);
            } else if (childIndex == RemotePhotoGridFragment.FRAGMENT_INDEX) {

                shownOnRemotePhotoClickedDialog(position);
            }
        }

    };

    private OnItemClickListener onVideoImageClickedListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            // TODO Auto-generated method stub
            if (childIndex == NativeVideoGridFragment.FRAGMENT_INDEX) {
                ShowOnNativeVideoClickedDialog(position);
            } else if (childIndex == RemoteVideoGridFragment.FRAGMENT_INDEX) {
                ShowOnRemoteVideoClickedDialog(position);
            }
        }
    };

    public class SetWifiAPTask extends AsyncTask<Boolean, Void, Void> {

        private boolean mMode;
        private boolean mFinish;

        public SetWifiAPTask(boolean mode, boolean finish) {
            mMode = mode;
            mFinish = finish;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (DEBUG) Log.i(TAG, "-----SetWifiAPTask onPreExecute------");
            mConnectProgressDialog.setMessage(getResources().getText(R.string.turning_wifi_ap_on));
            mConnectProgressDialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (DEBUG) Log.i(TAG, "-----SetWifiAPTask onPostExecute------");
            //updateStatusDisplay();
//			if (mFinish) mContext.finish();
        }

        @Override
        protected Void doInBackground(Boolean... off) {
            Log.e(TAG, "1234");
            try {
                if (off[0])
                    WifiUtils.toggleWifi(getActivity(), mWifiManager);
                WifiUtils.turnWifiApOn(getActivity(), mWifiManager, WifiCipherType.WIFICIPHER_NOPASS);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public void onCancelTvClicked() {

        deleteView.setVisibility(View.GONE);
        selectAllView.setVisibility(View.GONE);
        for (CheckBox cb : checkBoxs) {
            cb.setVisibility(View.GONE);
        }

    }

    public void cancelShareSelectView() {

        if (selectedCb != null)
            selectedCb.setChecked(false);

        for (CheckBox cb : shareCheckBoxs) {
            cb.setVisibility(View.GONE);
        }
    }

    public void disCheckMedia() {
        selectAllCb.setChecked(false);
        selectedPosition.clear();
        selectedMedias.clear();
    }


    public void onNativeMediaDeleteTvClicked(String type) {

        String imagesPath[] = getMediaPath(type);

        for (String path : imagesPath) {
            File file = new File(path);
            if (file.exists())
                file.delete();
        }

        MultiMediaScanner scanner = new MultiMediaScanner(mContext, imagesPath, null, mDialogHandler);
        scanner.connect();

        onMediaDeleted();

    }

    public Handler mDialogHandler = new Handler() {

        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            switch (msg.what) {
                case 0:
                    String msg0 = String.format(getResources().getString(R.string.delete_progress), msg.arg1, msg.arg2);
                    mDeleteProgressDialog.setMessage(msg0);
                    mDeleteProgressDialog.show();
                    break;
                case 1:
                    String msg1 = String.format(getResources().getString(R.string.delete_progress), msg.arg1, msg.arg2);
                    mDeleteProgressDialog.setMessage(msg1);
                    break;
                case 2:
                    if (mDeleteProgressDialog.isShowing())
                        mDeleteProgressDialog.cancel();
                    break;
                case 3:
                    mConnectProgressDialog.setMessage(getResources().getText(R.string.get_file_list));
                    break;
                case 4:
                    if (mConnectProgressDialog.isShowing())
                        mConnectProgressDialog.cancel();
                    break;
                case RESEDN_CONNECT_WIFI_MSG:
                    sendApInfoToGlass();
                    break;
            }
        }
    };

    private Handler mChannelHandler = new Handler() {
        private boolean connected = false;

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CamlogCmdChannel.RECEIVE_MSG_FROM_GLASS) {
                Packet data = (Packet) msg.obj;
                glassIp = data.getString("ip");
                if (DEBUG) Log.i(TAG, "---glassIp: " + glassIp);
                if (glassIp != null && glassIp.length() != 0 && !connected) {

                    connected = true;
                    mDialogHandler.removeMessages(RESEDN_CONNECT_WIFI_MSG);

                    if (childIndex == RemotePhotoGridFragment.FRAGMENT_INDEX)
                        mMediaUrlTask.execute(new String[]{glassIp, "photos"});
                    else if (childIndex == RemoteVideoGridFragment.FRAGMENT_INDEX)
                        mMediaUrlTask.execute(new String[]{glassIp, "videos"});
                }
            }
        }

    };

    public void onMediaDeleted() {
        ArrayList<MediaData> tmp = new ArrayList<MediaData>(selectedMedias);

        disCheckMedia();
        for (MediaData md : tmp) {
            Log.e(TAG, md.name);
            int i = mediaList.indexOf(md);
            if (i != -1)
                mediaList.remove(i);
        }
        tmp.clear();
        mImageAdapter.notifyDataSetChanged();
    }

    public void onRemotePhotoDeleteTvClicked() {

        DownloadManager mDownloadManager = (DownloadManager) mContext
                .getSystemService(mContext.DOWNLOAD_SERVICE);
//		DownloadManager.Request request = new DownloadManager.Request(uri)
    }

    private String[] getMediaPath(String type) {

        String paths[] = new String[selectedMedias.size()];
        String dirPath = Environment.getExternalStorageDirectory().toString()
                + "/Camlog/" + type + "/";

        for (int i = 0; i < selectedMedias.size(); i++) {

            MediaData data = selectedMedias.get(i);
            paths[i] = dirPath + data.name;
        }
        return paths;
    }

    private String[] getImagesId(ArrayList<String> imagesUrl) {
        String ids[] = new String[imagesUrl.size()];
        int i = 0;
        for (String url : imagesUrl) {
            int idIndex = url.lastIndexOf("/");
            ids[i++] = url.substring(idIndex + 1);
            Log.e(TAG, ids[i - 1]);
        }
        return ids;
    }

    class GetRemoteMediaUrlTask extends AsyncTask<String, Integer, String> {

        public GetRemoteMediaUrlTask() {
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            if (DEBUG) Log.i(TAG, "----GetRemoteMediaUrlTask onPreExecute.");
            mDialogHandler.sendEmptyMessage(3);
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            if (DEBUG) Log.i(TAG, "---GetRemoteMediaUrlTask onPostExecute.");
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... type) {
            // TODO Auto-generated method stub
            if (!getMediaUrl(type[0], type[1])) {
                publishProgress(2);
            } else if (!isCancelled())
                publishProgress(1);
            return "";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // TODO Auto-generated method stub
            mDialogHandler.sendEmptyMessage(4);
            if (DEBUG)
                Log.i(TAG, "---GetRemoteMediaUrlTask onProgressUpdate values[0]: " + values[0]);
            switch (values[0]) {
                case 1:
                    mImageAdapter.notifyDataSetChanged();
                    synchronized (failedMultimediaSyncCount_Lock) {
                        failedMultimediaSyncCount = 0;
                    }
                    break;
                case 2:
                    Toast.makeText(getActivity(), R.string.connect_error, Toast.LENGTH_LONG).show();
                    getActivity().onBackPressed();
                    // restart glass apache service
                    synchronized (failedMultimediaSyncCount_Lock) {
                        if (failedMultimediaSyncCount < 1) {
                            failedMultimediaSyncCount++;
                        } else {
                            failedMultimediaSyncCount = 0;
                            RestartApacheModule.getInstance(mContext).sendRestartApache();
                        }
                    }
                    break;
            }
            super.onProgressUpdate(values);
        }

    }

    private boolean getMediaUrl(final String ip, String type) {

        String uri = String.format("http://" + ip + "/cgi-bin/listfiles?%s", type);
        if (DEBUG) Log.i(TAG, "---uri: " + uri);
        final HttpClient httpClient = CustomHttpClient.getHttpClient();
        final HttpGet httpGet = new HttpGet(uri);
        boolean ok = false;
        ok = httpRequestExecute(httpClient, httpGet);
        if (ok) {
            mediaList = getMediaData(ip);
        }
        if (mediaList == null || !ok) {
            return false;
        }
        //Adjust the document order, in descending order by name.
        Comparator<MediaData> comparator = new Comparator<MediaData>() {
            public int compare(MediaData s1, MediaData s2) {
                return s1.name.compareTo(s2.name);
            }
        };
        Collections.sort(mediaList, comparator);
        Collections.reverse(mediaList);
        return true;
    }

    public void refreshGallery(String type) {
//		Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//		mediaScanIntent.setData(Uri.fromFile(file));
//		getActivity().getApplicationContext().sendBroadcast(mediaScanIntent);
        String path[] = getMediaPath(type);
        MultiMediaScanner scanner = new MultiMediaScanner(mContext, path, null, null);
        scanner.connect();
    }

    private ArrayList<MediaData> getMediaData(String ip) {
        try {
            File xmlFile = new File(getActivity().getCacheDir(), "medianame.xml");
            XmlContentHandler xmlHandler = new XmlContentHandler(ip);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            XMLReader xmlReader = parser.getXMLReader();
            xmlReader.setContentHandler(xmlHandler);
            xmlReader.parse(new InputSource(new FileInputStream(xmlFile)));

            return xmlHandler.getMedias();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean httpRequestExecute(HttpClient httpclient, HttpGet httpget) {

        InputStream in = null;
        int retry = 3;
        while (retry-- != 0) {
            Log.e(TAG, "123");
            try {
                HttpResponse response = httpclient.execute(httpget);
                in = response.getEntity().getContent();

                byte[] buffer = new byte[4096];

                File cacheDir = getActivity().getCacheDir();
                File xmlFile = new File(cacheDir, "medianame.xml");
                FileOutputStream fo = new FileOutputStream(xmlFile, false);

                int n = 0;
                while ((n = in.read(buffer)) != -1) {
                    fo.write(buffer, 0, n);
                }

                fo.close();
                in.close();

                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void sendApInfoToGlass() {

        if (mCamlogCmdChannel.isConnected()) {

            mConnectProgressDialog.setMessage(getResources().getText(R.string.wait_device_connect));
            if (!mConnectProgressDialog.isShowing())
                mConnectProgressDialog.show();

            Packet packet = mCamlogCmdChannel.createPacket();
            packet.putInt("type", CamlogCmdChannel.CONNET_WIFI_MSG);

            String ssid = WifiUtils.getValidSsid(mContext);
            String pw = WifiUtils.getValidPassword(mContext);
            String security = WifiUtils.getValidSecurity(mContext);

            packet.putString("ssid", ssid);
            packet.putString("pw", pw);
            packet.putString("security", security);
            mCamlogCmdChannel.sendPacket(packet);
            if (DEBUG)
                Log.i(TAG, "---sendApInfoToGlass ssid: " + ssid + " pw: " + pw + " security: " + security);
            mDialogHandler.sendEmptyMessageDelayed(RESEDN_CONNECT_WIFI_MSG, 5000);
        } else {
            if (mConnectProgressDialog.isShowing())
                mConnectProgressDialog.dismiss();
            Toast.makeText(getActivity(), R.string.bluetooth_error, Toast.LENGTH_LONG).show();
            getActivity().onBackPressed();
        }
    }

    public void showTurnWifiApOnDialog() {
        Log.w(TAG, "showTurnWifiApOnDialog in");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.turn_wifi_ap_on);
        builder.setMessage(R.string.wifi_ap_hint);
        builder.setPositiveButton(R.string.turn_wifi_ap_on_now, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                mWifiATask.execute(false);
                dialog.cancel();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                dialog.cancel();
                getActivity().onBackPressed();
            }
        });

        builder.setCancelable(false);
        builder.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                // TODO Auto-generated method stub
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    getActivity().onBackPressed();
                    dialog.cancel();
                }
                return false;
            }
        });

        builder.create().show();
    }

    public boolean isDownloaded(String name) {
        if (nativeMediaList != null) {
            for (MediaData md : nativeMediaList) {
                if (md.name.equals(name))
                    return true;
            }

        }
        return false;
    }

    public void ShowOnRemoteVideoClickedDialog(final int mPosition) {

        View view = getActivity().getLayoutInflater().inflate(R.layout.remote_video_dialog, null);
        ListView listView = (ListView) view.findViewById(R.id.option_lv);
        String[] options1 = getActivity().getResources().getStringArray(R.array.remote_video_option_1);
        String[] options2 = getActivity().getResources().getStringArray(R.array.remote_video_option_2);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(mediaList.get(mPosition).name);
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        dialog.show();

        if (isDownloaded(mediaList.get(mPosition).name)||!mediaList.get(mPosition).mdownload.equals("null")) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                    R.layout.remote_video_option_item, R.id.option_tv, options2);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    switch (position) {
                        case 0:
                            String path = VIDEO_DOWNLOAD_FOLDER + "/" + mediaList.get(mPosition).name;
                            Uri uri = Uri.fromFile(new File(path));
                            Log.e(TAG, uri.toString());
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(uri, "video/mp4");
//						startActivity(intent);
                            if (intent.resolveActivity(getActivity().getPackageManager()) != null)
                                startActivity(intent);
                            else
                                Toast.makeText(getActivity(), R.string.no_available_player, Toast.LENGTH_LONG).show();

                            break;
                        case 1:
                            showDeleteConfirmDialog(childIndex, mPosition);
                            break;
                        default:
                            break;
                    }
                    dialog.dismiss();
                }
            });
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                    R.layout.remote_video_option_item, R.id.option_tv, options1);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    // TODO Auto-generated method stub
                    ArrayList<MediaData> temp = new ArrayList<MediaData>();
                    temp.add(mediaList.get(mPosition));
                    switch (position) {
                        case 0:
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.e(TAG, "onDownloadSuccess: 正在下载   position: " + mPosition);
                                    if (isCurrentGridViewItemVisible(mPosition)) {
                                        ViewHolder holder = getViewHolder(mPosition);
                                        holder.rl_back.setVisibility(View.VISIBLE);
                                        holder.tv_download.setText(R.string.zhengzaixiazai);
                                    }

                                }
                            });
                            final ArrayList<MediaData> temp1 = new ArrayList<MediaData>();
                            temp1.add(mediaList.get(mPosition));

//                       // Log.e(TAG, "onItemClick : url : "+temp1.get(0).url+"  path : " +Environment.getExternalStorageDirectory().toString() + "/Camlog/photos/"+temp1.get(0).name);
                            DownloadUtil.get().download(temp1.get(0).url, "Camlog/videos", new DownloadUtil.OnDownloadListener() {

                                @Override
                                public void onDownloadSuccess() {
                                    Log.e(TAG, "onDownloadSuccess : ");
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            //更新系统图库使同步
                                            updataMeid(temp1.get(0),"videos");
                                            Log.e(TAG, "onDownloadSuccess: 已下载");
                                            if (isCurrentGridViewItemVisible(mPosition)) {
                                                ViewHolder holder = getViewHolder(mPosition);
                                                holder.rl_back.setVisibility(View.GONE);
                                                holder.downloadedTv.setText(R.string.media_downloaded);
                                                mediaList.get(mPosition).setMdownload(getResources().getString(R.string.media_downloaded));
                                            }
                                        }
                                    });

                                }

                                @Override
                                public void onDownloading(int progress) {

                                    Log.e(TAG, "onDownloading: " + progress);
                                    if (isCurrentGridViewItemVisible(mPosition)) {
                                        ViewHolder holder = getViewHolder(mPosition);
                                        holder.download_pgb.setProgress(progress);
                                    }

                                }

                                @Override
                                public void onDownloadFailed() {
                                    Log.e(TAG, "onDownloadFailed: ");
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (isCurrentGridViewItemVisible(mPosition)) {
                                                ViewHolder holder = getViewHolder(mPosition);
                                                holder.tv_download.setText(R.string.xiazaishibai);
                                            }

                                            new Handler().postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (isCurrentGridViewItemVisible(mPosition)) {
                                                        ViewHolder holder = getViewHolder(mPosition);
                                                        holder.rl_back.setVisibility(View.GONE);
                                                    }

                                                }
                                            }, 1000);
                                        }
                                    });
                                }
                            });
                           // ((VideoActivity) getActivity()).startVideoSync(temp);
                            break;
                        case 1:
                            showDeleteConfirmDialog(childIndex, mPosition);
                            break;
                        default:
                            break;
                    }
                    dialog.dismiss();
                }
            });
        }

    }

    public void ShowOnNativeVideoClickedDialog(final int mPosition) {

        View view = getActivity().getLayoutInflater().inflate(R.layout.remote_video_dialog, null);
        ListView listView = (ListView) view.findViewById(R.id.option_lv);
        String[] options = getActivity().getResources().getStringArray(R.array.native_video_option);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(mediaList.get(mPosition).name);
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        dialog.show();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                R.layout.remote_video_option_item, R.id.option_tv, options);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // TODO Auto-generated method stub
                switch (position) {
                    case 0:
                        String path = VIDEO_DOWNLOAD_FOLDER + "/" + mediaList.get(mPosition).name;
                        Uri uri = Uri.fromFile(new File(path));
                        Log.e(TAG, uri.toString());
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, "video/mp4");
//					startActivity(intent);
                        if (intent.resolveActivity(getActivity().getPackageManager()) != null)
                            startActivity(intent);
                        else
                            Toast.makeText(getActivity(), R.string.no_available_player, Toast.LENGTH_LONG).show();
                        break;
                    case 1:
                        if (WifiUtils.getWifiAPState(mWifiManager) == WIFI_AP_STATE_ENABLED)
                            showTurnApOffWhenShareDialog(childIndex, mPosition);
                        else
                            shareMedia(childIndex, mPosition);
                        break;
                    case 2:
                        showDeleteConfirmDialog(childIndex, mPosition);
                        break;
                    default:
                        break;
                }
                dialog.dismiss();
            }
        });

    }

    public void viewPhotos(int position, String type) {

        FragmentManager fragManager = getActivity().getFragmentManager();
        FragmentTransaction transcaction = fragManager.beginTransaction();
        String tag = PhotoViewPagerFragment.class.getName();
        PhotoViewPagerFragment photoFm = (PhotoViewPagerFragment) fragManager.findFragmentByTag(tag);
        if (photoFm == null)
            photoFm = new PhotoViewPagerFragment();

        Bundle bundle = new Bundle();
        bundle.putInt("position", position);
        bundle.putString("type", type);
        bundle.putParcelableArrayList("data", mediaList);
        photoFm.setArguments(bundle);

        transcaction.replace(android.R.id.content, photoFm, tag);
        transcaction.addToBackStack(null);
        transcaction.commit();

    }

    public void shownOnNativePhotoClickedDialog(final int mPosition) {

        View view = getActivity().getLayoutInflater().inflate(R.layout.remote_video_dialog, null);
        ListView listView = (ListView) view.findViewById(R.id.option_lv);
        String[] options = getActivity().getResources().getStringArray(R.array.native_photo_option);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(mediaList.get(mPosition).name);
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        dialog.show();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                R.layout.remote_video_option_item, R.id.option_tv, options);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                switch (position) {
                    case 0:
                        viewPhotos(mPosition, "native");
                        break;
                    case 1:
                        if (WifiUtils.getWifiAPState(mWifiManager) == WIFI_AP_STATE_ENABLED)
                            showTurnApOffWhenShareDialog(childIndex, mPosition);
                        else
                            shareMedia(childIndex, mPosition);
                        break;
                    case 2:
                        showDeleteConfirmDialog(childIndex, mPosition);
                        break;
                    default:
                        break;
                }
                dialog.dismiss();
            }
        });

    }

    public void shownOnRemotePhotoClickedDialog(final int mPosition) {

        View view = getActivity().getLayoutInflater().inflate(R.layout.remote_video_dialog, null);
        ListView listView = (ListView) view.findViewById(R.id.option_lv);
        String[] options = getActivity().getResources().getStringArray(R.array.remote_photo_option);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(mediaList.get(mPosition).name);
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        dialog.show();

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                R.layout.remote_video_option_item, R.id.option_tv, options);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    final int position, long id) {
                switch (position) {
                    case 0:
                        viewPhotos(mPosition, "remote");
                        break;
                    case 1:

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.e(TAG, "onDownloadSuccess: 正在下载   position: " + mPosition);
                                if (isCurrentGridViewItemVisible(mPosition)) {
                                    ViewHolder holder = getViewHolder(mPosition);
                                    holder.rl_back.setVisibility(View.VISIBLE);
                                    holder.tv_download.setText(R.string.zhengzaixiazai);
                                }

                            }
                        });
                        final ArrayList<MediaData> temp1 = new ArrayList<MediaData>();
                        temp1.add(mediaList.get(mPosition));

//                       // Log.e(TAG, "onItemClick : url : "+temp1.get(0).url+"  path : " +Environment.getExternalStorageDirectory().toString() + "/Camlog/photos/"+temp1.get(0).name);
                        DownloadUtil.get().download(temp1.get(0).url, "Camlog/photos", new DownloadUtil.OnDownloadListener() {

                            @Override
                            public void onDownloadSuccess() {
                                Log.e(TAG, "onDownloadSuccess : ");
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //更新系统图库使同步
                                        updataMeid(temp1.get(0),"photos");
                                        Log.e(TAG, "onDownloadSuccess: 已下载");
                                        if (isCurrentGridViewItemVisible(mPosition)) {
                                            ViewHolder holder = getViewHolder(mPosition);
                                            holder.rl_back.setVisibility(View.GONE);
                                            holder.downloadedTv.setText(R.string.media_downloaded);
                                            mediaList.get(mPosition).setMdownload(getResources().getString(R.string.media_downloaded));
                                        }
                                    }
                                });

                            }

                            @Override
                            public void onDownloading(int progress) {

                                Log.e(TAG, "onDownloading: " + progress);
                                if (isCurrentGridViewItemVisible(mPosition)) {
                                    ViewHolder holder = getViewHolder(mPosition);
                                    holder.download_pgb.setProgress(progress);
                                }

                            }

                            @Override
                            public void onDownloadFailed() {
                                Log.e(TAG, "onDownloadFailed: ");
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (isCurrentGridViewItemVisible(mPosition)) {
                                            ViewHolder holder = getViewHolder(mPosition);
                                            holder.tv_download.setText(R.string.xiazaishibai);
                                        }

                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (isCurrentGridViewItemVisible(mPosition)) {
                                                    ViewHolder holder = getViewHolder(mPosition);
                                                    holder.rl_back.setVisibility(View.GONE);
                                                }

                                            }
                                        }, 1000);
                                    }
                                });
                            }
                        });

                        //((PhotoActivity) getActivity()).startPhotoSync(temp1);
                        break;
                    case 2:
                        showDeleteConfirmDialog(childIndex, mPosition);
                        break;
                    default:
                        break;
                }
                dialog.dismiss();
            }
        });
    }

    //判断item是否在可见范围内
    private boolean isCurrentGridViewItemVisible(int position) {
        int first = grid.getFirstVisiblePosition();
        int last = grid.getLastVisiblePosition();
        return first <= position && position <= last;
    }

    //获取某个item的holder
    private ViewHolder getViewHolder(int position) {
        int childPosition = position - grid.getFirstVisiblePosition();
        View view = grid.getChildAt(childPosition);
        return (ViewHolder) view.getTag();
    }


    private void updataMeid(MediaData temp,String path) {
        //这段代码是用于更新sd卡下的文件内容，如果不刷新则不能获取到下载完成的文件
        String photoPath = Environment.getExternalStorageDirectory().toString()
                + "/Camlog/"+path+"/" + temp.name;
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(new File(photoPath)));
        getActivity().sendBroadcast(mediaScanIntent);
    }


    private void showDeleteConfirmDialog(final int type, final int position) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.delete);
        builder.setMessage(R.string.delete_message);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                switch (type) {
                    case NativePhotoGridFragment.FRAGMENT_INDEX:
                        selectedMedias.add(mediaList.get(position));
                        onNativeMediaDeleteTvClicked("photos");
                        break;
                    case NativeVideoGridFragment.FRAGMENT_INDEX:
                        selectedMedias.add(mediaList.get(position));
                        onNativeMediaDeleteTvClicked("videos");
                        break;
                    case RemotePhotoGridFragment.FRAGMENT_INDEX:
                        ArrayList<MediaData> temp2 = new ArrayList<MediaData>();
                        temp2.add(mediaList.get(position));
                        new RemoteMediaDeleteTask(getActivity(),
                                mediaList, temp2, mImageAdapter).execute(new String[]{"photos", glassIp});
                        break;
                    case RemoteVideoGridFragment.FRAGMENT_INDEX:
                        ArrayList<MediaData> temp = new ArrayList<MediaData>();
                        temp.add(mediaList.get(position));
                        new RemoteMediaDeleteTask(getActivity(),
                                mediaList, temp, mImageAdapter).execute(new String[]{"videos", glassIp});
                        break;
                }

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

    private void showTurnApOffWhenShareDialog(final int type, final int mPosition) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
                        WifiManager wifimanager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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
                shareMedia(type, mPosition);

            }
        });
        dialog.show();
    }

    private void shareMedia(int type, int position) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        if (type == NativePhotoGridFragment.FRAGMENT_INDEX)
            shareIntent.setType("image/jpeg");
        else if (type == NativeVideoGridFragment.FRAGMENT_INDEX)
            shareIntent.setType("video/mp4");

        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(mediaList.get(position).url));
        Intent selectIntent = Intent.createChooser(shareIntent, getResources().getText(R.string.share));
        startActivity(selectIntent);
    }


    public ArrayList<Integer> selectedPosition = new ArrayList<>();


    class DownloadThread implements Runnable {
        List<MediaData> mediaDatas;
        List<Integer> selectedPosition;
        String path;

        public DownloadThread(final List<MediaData> mediaDatas, final List<Integer> selectedPosition,String path) {
            this.mediaDatas = mediaDatas;
            this.selectedPosition = selectedPosition;
            this.path = path;
        }

        @Override
        public void run() {
            if (mediaDatas.size() != 0 && selectedPosition.size() != 0) {
                for (int i = 0; i < mediaDatas.size(); i++) {
                    try {
                        final int finalI = i;
                        final int j = selectedPosition.get(finalI);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                Log.e(TAG, "onDownloadSuccess: 正在下载   position: " + j);
                                if (isCurrentGridViewItemVisible(j)) {
                                    ViewHolder holder = getViewHolder(j);
                                    holder.rl_back.setVisibility(View.VISIBLE);
                                    holder.tv_download.setText(R.string.zhengzaixiazai);
                                }

                            }
                        });
                        DownloadUtil.get().download(mediaDatas.get(i).url, "Camlog/"+path, new DownloadUtil.OnDownloadListener() {

                            @Override
                            public void onDownloadSuccess() {
                                Log.e(TAG, "onDownloadSuccess : ");

                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //更新系统图库使同步
                                        updataMeid(mediaDatas.get(finalI),path);
                                        Log.e(TAG, "onDownloadSuccess: 已下载");
                                        //int j = selectedPosition.get(finalI);
                                        if (isCurrentGridViewItemVisible(j)) {
                                            ViewHolder viewHolder = getViewHolder(j);
                                            viewHolder.rl_back.setVisibility(View.GONE);
                                            viewHolder.downloadedTv.setText(R.string.media_downloaded);
                                            mediaList.get(selectedPosition.get(finalI)).setMdownload(getResources().getString(R.string.media_downloaded));
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onDownloading(int progress) {

                                Log.e(TAG, "onDownloading: " + progress);
                                //int j = selectedPosition.get(finalI);
                                if (isCurrentGridViewItemVisible(j)) {
                                    ViewHolder viewHolder = getViewHolder(j);
                                    viewHolder.download_pgb.setProgress(progress);
                                }

                            }

                            @Override
                            public void onDownloadFailed() {
                                Log.e(TAG, "onDownloadFailed: ");
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //int j = selectedPosition.get(finalI);
                                    if (isCurrentGridViewItemVisible(j)) {
                                        ViewHolder viewHolder = getViewHolder(j);
                                        viewHolder.tv_download.setText(R.string.xiazaishibai);
                                    }

                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            //int j = selectedPosition.get(finalI);
                                            if (isCurrentGridViewItemVisible(j)) {
                                                ViewHolder viewHolder = getViewHolder(j);
                                                viewHolder.rl_back.setVisibility(View.GONE);
                                            }

                                        }
                                    }, 1000);
                                }
                            });
                            }
                        });
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

}
