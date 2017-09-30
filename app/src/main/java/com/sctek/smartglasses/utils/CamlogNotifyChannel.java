package com.sctek.smartglasses.utils;

import com.ingenic.glass.api.sync.SyncChannel;
import com.ingenic.glass.api.sync.SyncChannel.CONNECTION_STATE;
import com.ingenic.glass.api.sync.SyncChannel.Packet;
import com.ingenic.glass.api.sync.SyncChannel.RESULT;
import com.ingenic.glass.api.sync.SyncChannel.onChannelListener;

import com.cn.zhongdun110.camlog.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.util.Log;

public class CamlogNotifyChannel {
	
	private final static String TAG = "CamlogNotifyChannel";

	private static final String NTF_CHANNEL_NAME = "ntfchannel";
	
	public final static int MSG_TYPE_POWER_CHANGE = 3;
	private final static int MSG_TYPE_LOW_POWER = 2;
	private final static int MSG_TYPE_PHONE = 1;
	private final static int REPORT_UPDATE_STATE = 21;
	private static final int REPORT_DOWNLOAD_PROGRESS = 32;

	public final static int UPDATE_TRY_CONNECT_WIFI = 0;
	public final static int UPDATE_CONNECTI_WIFI_TIMEOUT = 1;
	public final static int UPDATE_START_DOWNLOAD = 2;
	public final static int UPDATE_DOWNLOAD_ERROR = 3;
	public final static int UPDATE_INVALID_PACKAGE = 4;
	public final static int UPDATE_START = 5;
	public final static int UPDATE_SUCCESS = 6;
	public final static int UPDATE_FAILE = 7;
	
	private final static int updateMsgs[] = {R.string.update_try_connect_wifi, R.string.update_connect_wifi_timeout, R.string.update_start_download,
																										R.string.update_donwload_error, R.string.update_invalid_package, R.string.update_start,
																										R.string.update_success, R.string.update_fail, R.string.update_power_shortage, R.string.update_storage_shortage};
	
	private SyncChannel mChannel;
	
	private static CamlogNotifyChannel instance;
	
	private Context mContext;
	
	private CamlogNotifyChannel(Context context) {
		
		mChannel = SyncChannel.create(NTF_CHANNEL_NAME, context, mOnSyncListener);
		mContext = context;
	}
	
	public static CamlogNotifyChannel getInstance(Context context	) {
		if(instance == null)
			instance = new CamlogNotifyChannel(context);
		return instance;
	}
	
        public Packet createPacket() {
	    return mChannel.createPacket();
	}
	
        public void sendPacket(Packet pk) {
	    mChannel.sendPacket(pk);
	}
	private onChannelListener mOnSyncListener = new onChannelListener() {
		
		@Override
		public void onStateChanged(CONNECTION_STATE state) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onStateChanged:" + state.toString());
		}
		
		@Override
		public void onServiceConnected() {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onSendCompleted(RESULT result, Packet packet) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onSendCompleted:" + result.toString());
		}
		
		@Override
		public void onReceive(RESULT result, Packet data) {
			// TODO Auto-generated method stub
			Log.e(TAG, "Channel onReceive");
			int type = data.getInt("type");
			int state = data.getInt("state");
			switch (type) {
			case MSG_TYPE_LOW_POWER:
				NotificationManager notificationManager =  
				(NotificationManager)(mContext.getSystemService(Context.NOTIFICATION_SERVICE));
				Notification.Builder builder= new Notification.Builder(mContext)
				.setContentTitle(mContext.getResources().getString(R.string.low_power))
				.setContentText(mContext.getResources().getString(R.string.low_power_msg))
				.setVibrate(new long[]{0,100,200,300})
				.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(),R.drawable.ic_notification))
				.setSmallIcon(R.drawable.ic_launcher);
				Notification notification = builder.build();
				notificationManager.notify(3, notification);
				break;
			case MSG_TYPE_POWER_CHANGE:
				int powerLevel=data.getInt("CURRENT_POWER");
				showBetteryNotifi(powerLevel);
				break;
			case REPORT_UPDATE_STATE:
				showUpdateNotification(state);
				break;
			case REPORT_DOWNLOAD_PROGRESS:
			        int progress = data.getInt("progress");
				showDownloadProgressNotifi(progress);
				break;
			}
		}
	};
	
	private void showUpdateNotification(int state) {
		NotificationManager notificationManager =  
				(NotificationManager)(mContext.getSystemService(Context.NOTIFICATION_SERVICE));
				Notification.Builder builder= new Notification.Builder(mContext);
				builder.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(),R.drawable.ic_notification));
				builder.setSmallIcon(R.drawable.ic_launcher);
				builder.setContentTitle(mContext.getText(R.string.camlog_update));
				builder.setContentText(mContext.getText(updateMsgs[state]));
				Notification notification = builder.getNotification();
				notificationManager.notify(4, notification);
	}
	
        private void showDownloadProgressNotifi(int progress) {
	        NotificationManager notificationManager =  
				(NotificationManager)(mContext.getSystemService(Context.NOTIFICATION_SERVICE));
				Notification.Builder builder= new Notification.Builder(mContext);
				builder.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(),R.drawable.ic_notification));
				builder.setSmallIcon(R.drawable.ic_launcher);
				builder.setContentTitle(mContext.getText(R.string.camlog_update));
				String strProgress = mContext.getResources().getString(R.string.update_download_progress)+progress+"%";
				builder.setContentText(strProgress);
				Notification notification = builder.getNotification();
				notificationManager.notify(4, notification);
	}
	private void showBetteryNotifi(int powerLevel){
		NotificationManager notificationManager = (NotificationManager)    
		    mContext.getSystemService(android.content.Context.NOTIFICATION_SERVICE);   
		String currentBettery=mContext.getResources().getString(R.string.current_power)+":"+powerLevel;
		Notification.Builder builder= new Notification.Builder(mContext);
		CharSequence contentTitle = mContext.getResources().getString(R.string.current_power);
		CharSequence contentText = currentBettery;
		builder.setContentTitle(contentTitle);
		builder.setContentText(currentBettery);
		builder.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(),R.drawable.ic_notification));
		if (powerLevel >= 95)
		    builder.setSmallIcon(R.drawable.battery_100);
		else if (powerLevel >= 85)
		    builder.setSmallIcon(R.drawable.battery_90);
		else if (powerLevel >= 75)
		    builder.setSmallIcon(R.drawable.battery_80);
		else if (powerLevel >= 65)
		    builder.setSmallIcon(R.drawable.battery_70);
		else if (powerLevel >= 55)
		    builder.setSmallIcon(R.drawable.battery_60);
		else if (powerLevel >= 45)
		    builder.setSmallIcon(R.drawable.battery_50);
		else if (powerLevel >= 35)
		    builder.setSmallIcon(R.drawable.battery_40);
		else if (powerLevel >= 25)
		    builder.setSmallIcon(R.drawable.battery_30);
		else if (powerLevel >= 15)
		    builder.setSmallIcon(R.drawable.battery_20);
		else if (powerLevel >= 7)
		    builder.setSmallIcon(R.drawable.battery_10);
		else 
		    builder.setSmallIcon(R.drawable.battery_5);
		// 设置通知的事件消息   	
		Intent notificationIntent =new Intent();
		notificationIntent.setAction("WelcomeActivity");
		PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent,0);
		builder.setContentIntent(contentIntent);
		Notification notification = builder.getNotification();
		notification.flags |= Notification.FLAG_SHOW_LIGHTS; 
		notification.flags |= Notification.FLAG_ONGOING_EVENT; 
		// 表明在点击了通知栏中的"清除通知"后，此通知不清除， 经常与FLAG_ONGOING_EVENT一起使用  
		notification.flags |= Notification.FLAG_NO_CLEAR;    
		notification.defaults = Notification.DEFAULT_LIGHTS; 
		notification.ledARGB = 0xff0000ff;
		notification.ledOnMS =5000; //5 s
		/* id :0,1,3,4,102,121,122,0x123 has been used*/
   		notificationManager.notify(100, notification);   
	}
}
