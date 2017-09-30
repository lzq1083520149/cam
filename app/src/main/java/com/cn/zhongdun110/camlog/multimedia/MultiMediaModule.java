package com.cn.zhongdun110.camlog.multimedia;

import java.io.File;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.Environment;
import android.content.SharedPreferences;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.SyncException;

public class MultiMediaModule extends SyncModule {
    private static final String TAG = "MultiMediaModule";
    private static final String LETAG = "GSMMM";

    private static final String GSMMD_CMD = "gsmmd_cmd";
    private static final String GSMMD_ASK = "gsmmd_ask";
    private static final String GSMMD_rqst = "gsmmd_rqst";
    private static final String GSMMD_FINISH = "gsmmd_finish";
    private static final String GSMMD_DELETE = "gsmmd_delete";
    private static final String GSMMD_DELFNS = "gsmmd_delfns";
    private static final String GSMMD_FFAIL = "gsmmd_ffail";
    private static final String GSMMD_FFACK = "gsmmd_ffack";
    private static final String GSMMD_IMAGESYNC = "gsmmd_imgsync";
    private static final String GSMMD_VIDEOSYNC = "gsmmd_vidsync";

    private static final String GSMMD_NULL = "gsmmd_null";

    private static final String GSMMD_NAME = "gsmmd_name";

    private static final String GSMMD_TYPE = "gsmmd_type";
    public static int GSMMD_NONE = 0x0;
    public static int GSMMD_PIC = 0x1;
    public static int GSMMD_VIDEO = 0x2;
    public static int GSMMD_SINGLE_FILE = 0x3;
    public static int GSMMD_IMG_THUMB = 0x4;
    public static int GSMMD_VID_THUMB = 0x5;

    public static int GSMMD_PICS = 0x6;
    public static int GSMMD_VIDS = 0x7;
    public static int GSMMD_ALL = 0x10;

    private static final String GSMMD_ACT = "gsmmd_act";
    public static int GSMMD_EXIST = 0x1;
    public static int GSMMD_NOEXIST = 0x2;

    private static final String GSMMD_TSP = "gsmmd_tsp";
    private int ASK_TSP = 0;

    private static final String GSMMD_SRST = "gsmmd_srst";

    private static final String GSMMD_STASM = "gsmmd_stasm";

    private static final String GSMMD_SINGLE_FILE_NAME = "gsmmd_single_file_name";
    private static final String GSMMD_SINGLE_FILE_TYPE = "gsmmd_single_file_type";
    public static int SINGLE_FILE_TYPE_PIC = 0x1;
    public static int SINGLE_FILE_TYPE_VIDEO = 0x2;


    private Context mContext;
    private static MultiMediaModule sInstance;
    private String mSingleFileName;

    private MultiMediaModule(Context context){
	super(LETAG, context);
	mContext = context;
	Log.e(TAG, "MultiMediaModule");
    }

    public static MultiMediaModule getInstance(Context c) {
	if (null == sInstance)
	    sInstance = new MultiMediaModule(c);
	return sInstance;
    }

    @Override
    protected void onCreate() {
    }

    @Override
    protected void onRetrive(SyncData data) {
	Log.e(TAG, "onRetrive");

	String cmd = data.getString(GSMMD_CMD);
	if (cmd.equals(GSMMD_ASK)){
	    Log.e(TAG, "GSMMD_ASK " + data.getString(GSMMD_NAME));
	    int type = data.getInt(GSMMD_TYPE);
	    ASK_TSP = data.getInt(GSMMD_TSP);
	    String name = data.getString(GSMMD_NAME);
	    int exist = check_exist(name, type);
	    sync_request(name, type, exist);
	}else if (cmd.equals(GSMMD_DELFNS)){
	    Log.e(TAG, "GSMMD_DELFNS " + data.getString(GSMMD_NAME));
	    // MultiMediaObserver m = MultiMediaObserver.getInstance(mContext);
	    // m.delete_finish(data.getString(GSMMD_NAME), data.getInt(GSMMD_TYPE));
	}else if (cmd.equals(GSMMD_FFAIL)){
	    Log.e(TAG, "GSMMD_FFAIL " + data.getString(GSMMD_NAME));
	    // MultiMediaObserver m = MultiMediaObserver.getInstance(mContext);
	    fileFail(data.getString(GSMMD_NAME), data.getInt(GSMMD_TYPE));
	}else if (cmd.equals(GSMMD_NULL)){
	    Log.e(TAG, "GSMMD_NULL ");
	    Intent i = new Intent(OpenFileActivity.NODATA);
	    mContext.sendBroadcast(i);
	}
    }

    public void sync_request(String name, int type, int act){	
	SyncData data = new SyncData();

	data.putString(GSMMD_CMD, GSMMD_rqst);
	data.putInt(GSMMD_TYPE, type);
	data.putInt(GSMMD_ACT, act);
	data.putString(GSMMD_NAME, name);
	data.putInt(GSMMD_TSP, ASK_TSP);

	try {
	    Log.e(TAG, "sync_request");
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }

    public void mul_request_single_file(String name,int type){
	SyncData data = new SyncData();

	data.putString(GSMMD_CMD, GSMMD_rqst);
	data.putInt(GSMMD_TYPE, GSMMD_SINGLE_FILE);

	data.putString(GSMMD_SINGLE_FILE_NAME, name);
	data.putInt(GSMMD_SINGLE_FILE_TYPE, type);
        mSingleFileName = name;
	try {
	    Log.e(TAG, "mul_request_single_file");
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }

    public void delete_request(String fileName, int type){
	SyncData data = new SyncData();

	data.putString(GSMMD_CMD, GSMMD_DELETE);
	data.putInt(GSMMD_TYPE, type);
	data.putString(GSMMD_NAME, fileName);

	try {
	    Log.e(TAG, "delete_request");
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }

    public void reply_ffail(String fileName, int type){
	SyncData data = new SyncData();

	data.putString(GSMMD_CMD, GSMMD_FFACK);
	data.putInt(GSMMD_TYPE, type);
	data.putString(GSMMD_NAME, fileName);

	try {
	    Log.e(TAG, "reply_ffail");
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }

    public void mul_request(){
	SharedPreferences sp = mContext.getSharedPreferences(LETAG, Context.MODE_PRIVATE);
	boolean imagesync = sp.getBoolean("imagesync", false);
	boolean videosync = sp.getBoolean("videosync", false);

	if(imagesync){
	    SyncData data = new SyncData(); 
	    data.putString(GSMMD_CMD, GSMMD_rqst);
	    data.putInt(GSMMD_TYPE, GSMMD_PICS);
	    
	    try {
		send(data);
	    } catch (SyncException e) {
		Log.e(TAG, "" + e);
	    }
	}

	if(videosync){
	    SyncData data = new SyncData(); 
	    data.putString(GSMMD_CMD, GSMMD_rqst);
	    data.putInt(GSMMD_TYPE, GSMMD_VIDS);
	    
	    try {
		send(data);
	    } catch (SyncException e) {
		Log.e(TAG, "" + e);
	    }
	}

    }
    public void setImageAutoSync(boolean val){
	SharedPreferences sp = mContext.getSharedPreferences(LETAG, Context.MODE_PRIVATE);
	SharedPreferences.Editor editor = sp.edit();

	boolean videosync = sp.getBoolean("videosync", false);

	editor.putBoolean("imagesync", val);
	editor.putBoolean("videosync", videosync);
	editor.commit();

	SyncData data = new SyncData();

	data.putString(GSMMD_CMD, GSMMD_IMAGESYNC);
	data.putBoolean(GSMMD_STASM, val);

	try {
	    Log.e(TAG, "setimageSync");
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }

    public void setVideoAutoSync(boolean val){
	SharedPreferences sp = mContext.getSharedPreferences(LETAG, Context.MODE_PRIVATE);
	SharedPreferences.Editor editor = sp.edit();

	boolean imagesync = sp.getBoolean("imagesync", false);

	editor.putBoolean("imagesync", imagesync);
	editor.putBoolean("videosync", val);
	editor.commit();

	SyncData data = new SyncData();

	data.putString(GSMMD_CMD, GSMMD_VIDEOSYNC);
	data.putBoolean(GSMMD_STASM, val);

	try {
	    Log.e(TAG, "setvideoSync");
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}
    }

    @Override
    public void onFileRetriveComplete(String fileName, boolean success){
	Log.e(TAG, "onFileRetriveComplete " + fileName + " " + success);

	SyncData data = new SyncData();
	
	data.putString(GSMMD_CMD, GSMMD_FINISH);
	data.putBoolean(GSMMD_SRST, success);

	try {
	    Log.e(TAG, "sync finish");
	    send(data);
	} catch (SyncException e) {
	    Log.e(TAG, "" + e);
	}

	if (success == true){
	    MultiMediaScanner m = new MultiMediaScanner(mContext, fileName, null);
	    // MultiMediaObserver mmo = MultiMediaObserver.getInstance(mContext);
	    // mmo.addPath(fileName);
	    
	    String file = fileName.substring(fileName.lastIndexOf("/")+1,fileName.length());
	    Log.e(TAG, "mSingleFileName"+mSingleFileName +"--"+file);
	    if(file.equals(mSingleFileName)){
		Intent i = new Intent("cn.ingenic.glasssync.share.REQUEST_FILE_OK");
		mContext.sendBroadcast(i);
		}
	}
    }

    private static int check_exist(String name, int type){
	String dirpath;
	if (type == MultiMediaModule.GSMMD_PIC){
	    dirpath = "/IGlass/Pictures/";
	}else if (type == MultiMediaModule.GSMMD_VIDEO){
	    dirpath = "/IGlass/Video/";
	}else if (type == MultiMediaModule.GSMMD_IMG_THUMB || type == MultiMediaModule.GSMMD_VID_THUMB){
	    dirpath = "/IGlass/Thumbnails/";
	}else{
	    dirpath = "/IGlass/data/";
	}

	File f = new File(Environment.getExternalStorageDirectory() + dirpath + name);
	Log.e(TAG, "File " + f.getPath());
	if (f.exists()){
	    Log.e(TAG, "GSMMD_EXIST");
	    return MultiMediaModule.GSMMD_EXIST;
	}else{
	    Log.e(TAG, "GSMMD_NOEXIST");
	    return MultiMediaModule.GSMMD_NOEXIST;
	}
    }
    private void fileFail(String fileName, int type){
	if (check_exist(fileName, type) == MultiMediaModule.GSMMD_EXIST){
	    deleteFile(fileName, type);
	}

	// MultiMediaModule m = MultiMediaModule.getInstance(mContext);
        reply_ffail(fileName, type);
    }

    private void deleteFile(String fileName, int type){
	String dirpath;
	if (type == MultiMediaModule.GSMMD_PIC){
	    dirpath = "/IGlass/Pictures/";
	}else if (type == MultiMediaModule.GSMMD_VIDEO){
	    dirpath = "/IGlass/Video/";
	}else if (type == MultiMediaModule.GSMMD_IMG_THUMB || type == MultiMediaModule.GSMMD_VID_THUMB){
	    dirpath = "/IGlass/Thumbnails/";
	}else{
	    dirpath = "/IGlass/data/";
	}

	File f = new File(Environment.getExternalStorageDirectory() + dirpath + fileName);
	f.delete();
    }
}