package com.cn.zhongdun110.camlog.multimedia;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.cn.zhongdun110.camlog.R;

import java.io.File;

public class OpenFileActivity extends Activity {
    private static final String TAG = "OpenFileActivity";
    private final String THUMB_TAG = "_thumb.jpg";
    public final static String NODATA = "nodata";
    public final static String REQUEST_FILE_OK = "request_file_ok";
    
    private final int NO_FLAG = 0;
    private final int IMG_FLAG = 1;
    private final int VIDEO_FLAG = 2;

    private final int WAIT_TIMEOUT = 10;
    private final int WAIT_RIGHT = 11;
    private final int WAIT_NULL = 12;
    private final int WAIT_ProgressBar = 13;

    private View mProgressBarLayout;
    //private boolean mIsSyncFailed = false;
    
    private String mOriginalFileName;
    private String mOriginalFilePath;
    private Context mContext;
      //   private MultiMediaReceiver nReceiver;
    private WaitThread mThread;
    private final Handler mHandler = new MainHandler();
    private OpenFileActivityReceiver nReceiver;
    private boolean mRequestFileOk_Flag = false;

    // This Handler is used to post message back onto the main thread of the
    // application
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
	        case WAIT_TIMEOUT:{
		    showErrorView();
		    break;
		}
	        case WAIT_ProgressBar:{
		    mProgressBarLayout.setVisibility(View.VISIBLE);
		    break;
		}
	        case WAIT_RIGHT:{
		    if(checkFileType(mOriginalFileName) == IMG_FLAG)
			showImageView(mOriginalFilePath);
		    else
			videoPlay(mOriginalFilePath);			

		    mThread.exit = true;  // 终止线程thread 
		    try {   
			mThread.join(); 
		    } catch (InterruptedException e) {   
			   mThread.interrupt();   
		    }
		}
		case WAIT_NULL:{
		
		    finish();
		}   
                default:
                    Log.v(TAG, "Unhandled message: " + msg.what);
                    break;
            }
        }
    }

    class OpenFileActivityReceiver extends BroadcastReceiver{
	private String TAG = "OpenFileActivityReceiver";
	
        @Override
	    public void onReceive(Context context, Intent intent) {
	    //Log.e(TAG, "onReceive " + intent.getAction());
	    String action = intent.getAction();
	    if (action.equals(NODATA)){
		Toast.makeText(OpenFileActivity.this, "眼镜找不到该文件了", Toast.LENGTH_SHORT).show();
		mHandler.sendEmptyMessageDelayed(WAIT_NULL, 1000);
	    }else if(action.equals(REQUEST_FILE_OK)){
		mRequestFileOk_Flag = true;
	    }
        }
    }

    @Override
    protected void onCreate (Bundle savedInstanceState){
	super.onCreate(savedInstanceState);

	mContext = this;
	setContentView(R.layout.openfile);

	  //processing
	mProgressBarLayout = findViewById(R.id.layout_processBar);
	mProgressBarLayout.setVisibility(View.VISIBLE);

	String path = getIntent().getStringExtra("path");
	if(!path.toLowerCase().endsWith(THUMB_TAG))
	    return;

	String tempPath = path.replace(THUMB_TAG,""); 
	int fileTypePos = tempPath.lastIndexOf("_");

	tempPath = tempPath.substring(0,fileTypePos) + "." 
	    + tempPath.substring(fileTypePos+1,tempPath.length());
	
	mOriginalFileName = tempPath.substring(tempPath.lastIndexOf("/")+1,tempPath.length());

	int fileType = checkFileType(tempPath);

	if(fileType == IMG_FLAG){
	    mOriginalFilePath = tempPath.replace("/Thumbnails/","/Pictures/");
	    if ((new File(mOriginalFilePath)).exists()){
		showImageView(mOriginalFilePath);
		Log.i(TAG,"showImageView----mOriginalFilePath="+mOriginalFilePath);
		}else{
		notifySyncFile(mOriginalFileName,fileType);
	    }
	}else if (fileType == VIDEO_FLAG){
	    mOriginalFilePath = tempPath.replace("/Thumbnails/","/Video/");
	    if ((new File(mOriginalFilePath)).exists()){
		videoPlay(mOriginalFilePath);
	    }else{
		notifySyncFile(mOriginalFileName,fileType);
	    }
	}
	init_receiver(this);
    }
    
    private void init_receiver(Context c){
        nReceiver = new OpenFileActivityReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(NODATA);
	filter.addAction(REQUEST_FILE_OK);
        c.registerReceiver(nReceiver,filter);
    }

    private void notifySyncFile(String fileName,int fileType){
	Intent i = new Intent("cn.ingenic.glasssync.mulmedia.REQUEST_MULMEDIA");
	i.putExtra("file_name",fileName); 
	if(fileType ==IMG_FLAG)
	    i.putExtra("file_type",MultiMediaModule.GSMMD_PIC); 
	else
	    i.putExtra("file_type",MultiMediaModule.GSMMD_VIDEO); 
	
	mContext.sendBroadcast(i);	    

	mThread = new WaitThread(fileType);
	mThread.start();  
    }

      /*wait 5s for sync file,
       *then show image thumbnails or video sync fails info when timeout
       */
    public class WaitThread extends Thread {
	public volatile boolean exit = false; 
	private int type;
	
	public WaitThread(int fileType){
	    type = fileType;
	}

	@Override 
	public void run() {
	    int times=0;
	    int stopTime=0;
	    	
	    if(type == IMG_FLAG){
		stopTime = 5;
	    }else {
		stopTime = 59;
	    }
	    while(times < stopTime && !exit){
		times++;		
		if ((new File(mOriginalFilePath)).exists())
		    if(mRequestFileOk_Flag){			
			mHandler.sendEmptyMessageDelayed(WAIT_RIGHT, 0);
			// mRequestFileOk_Flag = false;
		    }
		else 
		    mHandler.sendEmptyMessageDelayed(WAIT_ProgressBar, 0);
		try {   
		    Thread.sleep(1000);//1s
		} catch (InterruptedException e) {   
		    Thread.currentThread().interrupt();   
		}		
	    }
	    mHandler.sendEmptyMessageDelayed(WAIT_ProgressBar, 0);
	    mHandler.sendEmptyMessageDelayed(WAIT_TIMEOUT, 0);		
	}	
    }


    private int checkFileType(String path){
	    if(path.toLowerCase().endsWith(".mp4")
	       || path.toLowerCase().endsWith(".3gp") 
	       || path.toLowerCase().endsWith(".mkv") 
	       || path.toLowerCase().endsWith(".avi") 
	       || path.toLowerCase().endsWith(".mov") 
	       || path.toLowerCase().endsWith(".wmv"))
		return VIDEO_FLAG;

	    if(path.toLowerCase().endsWith(".jpg") 
	       || path.toLowerCase().endsWith(".png") 
	       || path.toLowerCase().endsWith(".bmp") 
	       || path.toLowerCase().endsWith(".jpeg") 
	       || path.toLowerCase().endsWith(".gif"))
		return IMG_FLAG;
	    return NO_FLAG;
    }

    private void showErrorView(){
	if(checkFileType(mOriginalFileName) == IMG_FLAG){
	    String path = getIntent().getStringExtra("path");
	    showImageView(path);
	}else{ 
	     //video
	    mProgressBarLayout.setVisibility(View.INVISIBLE);
	    Intent intent = new Intent();
	    setResult(1, intent);
	    finish();
	    Log.i(TAG,"--finish()--");
	}
    }

    private void showImageView(String filePath){
	// Bitmap btmp = BitmapFactory.decodeFile(filePath);    
	// ViewGroup root  = (ViewGroup) findViewById(R.id.main);
	// ImageView iv = new ImageView(this);

	// iv.setImageBitmap(btmp);
	// iv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));

	// root.addView(iv);
        // mProcessView.setVisibility(View.INVISIBLE);
	mProgressBarLayout.setVisibility(View.INVISIBLE);
	//mIsSyncFailed = false;
	
	Intent intent = new Intent("android.intent.action.VIEW");
	intent.addCategory("android.intent.category.DEFAULT");
	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	Uri uri = Uri.fromFile(new File(filePath));
	intent.setDataAndType(uri, "image/*");
	this.startActivity(intent);
	finish();
    }

    private void videoPlay(String filePath){
    mProgressBarLayout.setVisibility(View.INVISIBLE);
    //mIsSyncFailed = false;
	try {
		Intent intent = new Intent(Intent.ACTION_VIEW);  
		intent.setDataAndType(Uri.parse(filePath), "video/*");
		startActivity(intent);    
		finish();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		Toast.makeText(this,R.string.play_error,Toast.LENGTH_LONG).show();
		finish();
		e.printStackTrace();
	}
    }

    @Override
    protected void onResume(){
	super.onResume();
    }

    @Override  
    protected void onDestroy() {
    super.onDestroy();
    unregisterReceiver(nReceiver);
	Log.i(TAG, "--onDestory called.");  
    }  

}