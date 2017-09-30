package com.cn.zhongdun110.camlog;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import com.sctek.smartglasses.utils.CustomHttpClient;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LocationReportService extends Service {
	
	private final static String TAG = "LocationReportService";
	private final static String REPORT_LOCATION_URL_1 = "http://www.wear0309.com/location.php";
	private final static String REPORT_LOCATION_URL_2 = "http://www.sctek.cn:8080/location";
	
	private LocationManager mLocationManager;
	
	private LocationListener mLocationListener = new LocationListener() {
		
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
		}
		
		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			reportGpsLocation(location);
			mLocationManager.removeUpdates(this);
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		mLocationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
		super.onCreate();
	}
	
	@SuppressLint("NewApi")
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		reportBSLocation();
		
		if(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
			mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocationListener, null);
		else if(mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
			mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, mLocationListener, null);
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	private void reportBSLocation() {
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				int mmc;
				int mnc;
				int lac;
				int cid;
				String serial = PreferenceManager.getDefaultSharedPreferences(LocationReportService.this).getString("serial", "000000000");
				
				TelephonyManager mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
				String operator = mTelephonyManager.getNetworkOperator();
				
				CellLocation cellLocation = mTelephonyManager.getCellLocation();
				
				if(operator == null ||operator.length() < 5 || cellLocation == null)
					return;
				
				mmc = Integer.parseInt(operator.substring(0, 3));
				mnc = Integer.parseInt(operator.substring(3));
				
				if(mnc == 2) {
					lac = ((CdmaCellLocation)cellLocation).getNetworkId();
					cid = ((CdmaCellLocation)cellLocation).getBaseStationId();
				}
				else {
					lac = ((GsmCellLocation)cellLocation).getLac();
					cid = ((GsmCellLocation)cellLocation).getCid();
				}
				
				Log.e(TAG, "mmc:" + mmc + " mnc:" + mnc + " lac:" + lac + " cid:" + cid); 
				
				HttpClient client = CustomHttpClient.getHttpClient();
				HttpPost httpPost_1 = new HttpPost(REPORT_LOCATION_URL_1);
				HttpPost httpPost_2 = new HttpPost(REPORT_LOCATION_URL_2);
				
				List<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("serial", serial));
				params.add(new BasicNameValuePair("type", "bs"));
				params.add(new BasicNameValuePair("mmc", String.valueOf(mmc)));
				params.add(new BasicNameValuePair("mnc", String.valueOf(mnc)));
				params.add(new BasicNameValuePair("lac", String.valueOf(lac)));
				params.add(new BasicNameValuePair("cid", String.valueOf(cid)));
				
				long time = System.currentTimeMillis();
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				String date = format.format(new Date(time));
				params.add(new BasicNameValuePair("time", date));
				
				try {
					httpPost_1.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
					client.execute(httpPost_1);
					
					httpPost_2.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
					client.execute(httpPost_2);
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	private void reportGpsLocation(final Location location){
		
		new Thread(new Runnable() {
					
					@Override
					public void run() {
						
						int mmc;
						int mnc;
						int lac;
						int cid;
						
						String serial = PreferenceManager.getDefaultSharedPreferences(LocationReportService.this).getString("serial", "000000000");
						
						HttpClient client = CustomHttpClient.getHttpClient();
						HttpPost httpPost_1 = new HttpPost(REPORT_LOCATION_URL_1);
						HttpPost httpPost_2 = new HttpPost(REPORT_LOCATION_URL_2);
						
						List<NameValuePair> params = new ArrayList<NameValuePair>();
						params.add(new BasicNameValuePair("serial", serial));
						params.add(new BasicNameValuePair("type", "gps"));
						params.add(new BasicNameValuePair("latitude", String.valueOf(location.getLatitude())));
						params.add(new BasicNameValuePair("longitude", String.valueOf(location.getLongitude())));
						
						long time = System.currentTimeMillis();
						SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
						String date = format.format(new Date(time));
						params.add(new BasicNameValuePair("time", date));
						
						try {
							httpPost_1.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
							client.execute(httpPost_1);
							
							httpPost_2.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
							client.execute(httpPost_2);
							
						} catch (ClientProtocolException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}).start();

	}
}
