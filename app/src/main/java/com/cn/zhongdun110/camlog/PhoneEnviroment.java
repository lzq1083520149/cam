package com.cn.zhongdun110.camlog;

import java.util.UUID;

import cn.ingenic.glasssync.DefaultSyncManager;
import cn.ingenic.glasssync.Enviroment;
import cn.ingenic.glasssync.transport.BluetoothChannel;
import cn.ingenic.glasssync.transport.TransportManager;

import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class PhoneEnviroment extends Enviroment {
//	private static final String TAG = "PhoneEnv";

    PhoneEnviroment(Context context) {
        super(context);
        mResMgr = new ResourceManager(context) {

            @SuppressWarnings("deprecation")
            @Override
            public Notification getRetryFailedNotification() {
//				Notification noti = new Notification();
//				noti.flags |= Notification.FLAG_ONGOING_EVENT;
//				noti.icon = android.R.drawable.ic_dialog_alert;
//				noti.vibrate = new long[]{0, 500, 500, 500};
//				noti.setLatestEventInfo(mContext, mContext.getString(R.string.timout_noti_title), mContext.getString(R.string.timout_noti_content), null);


                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(mContext)
                                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                                .setContentTitle(mContext.getString(R.string.timout_noti_title))
                                .setContentText(mContext.getString(R.string.timout_noti_content))
                                .setVibrate(new long[]{0, 500, 500, 500})
                                .setOngoing(true);

//				Notification.Builder build = new Notification.Builder(mContext);
//				build.setOngoing(true);
//				build.setContentTitle("Glasssync Timeout");
//				build.setContentText("Glasssync running without BT connectivity for more than two hours");
////				build.setContentInfo("touch me to connect");
//				build.setTicker("Glasssync disable auto connect");
//				build.setSmallIcon(android.R.drawable.ic_dialog_alert);
//				build.setVibrate(new long[]{500, 500, 500, 500});
//				return build.getNotification();
                return builder.build();
            }

            @Override
            public Toast getRetryToast(int reason) {
                String str = "UNKNOW";
                switch (reason) {
                    case DefaultSyncManager.CONNECTING:
                        str = mContext.getString(R.string.retry_connect);
                        break;
                    case DefaultSyncManager.SUCCESS:
                        str = mContext.getString(R.string.retry_connect_success);
                        break;
                    case DefaultSyncManager.NO_CONNECTIVITY:
                        str = mContext.getString(R.string.retry_connect_failed);
                        break;
                    case DefaultSyncManager.FEATURE_DISABLED:
                        throw new IllegalArgumentException("System module should ever not be disable.");
                    case DefaultSyncManager.NO_LOCKED_ADDRESS:
                        str = mContext.getString(R.string.retry_connect_failed_with_no_address);
                        break;
                }
                return Toast.makeText(mContext, str, Toast.LENGTH_SHORT);
            }

        };
    }

    @Override
    public boolean isWatch() {
        return false;
    }

//    @Override
//    public void processBondRequest(String address) {
//        String title = mContext.getString(R.string.bond_request);
//        String name = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address).getName();
//        String msg = mContext.getString(R.string.bond_request_message, name);
//        Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
//        v.vibrate(500);
//    }

//    @Override
//    public void processBondResponse(boolean success) {
//        TransportManager tm = TransportManager.getDefault();
//        tm.sendBondResponse(success);
//    }

    @Override
    public UUID getUUID(int type, boolean remote) {
        switch (type) {
            case BluetoothChannel.CUSTOM:
                return remote ? BluetoothChannel.W_CUSTOM_UUID
                        : BluetoothChannel.CUSTOM_UUID;
            case BluetoothChannel.SERVICE:
                return remote ? BluetoothChannel.W_SERVICE_UUID
                        : BluetoothChannel.SERVICE_UUID;
            default:
                return null;
        }
    }

}
