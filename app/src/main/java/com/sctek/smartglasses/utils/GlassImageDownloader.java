package com.sctek.smartglasses.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import com.nostra13.universalimageloader.utils.IoUtils;

import android.net.Uri;
import android.util.Log;

public class GlassImageDownloader {
	
	public static final String TAG = "GlassImageDownloader";
	
	public static final int DEFAULT_HTTP_CONNECT_TIMEOUT = 20 * 1000; // milliseconds
	
	public static final int DEFAULT_HTTP_READ_TIMEOUT = 60 * 1000; // milliseconds
	
	protected static final String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%";
	
	protected static final int MAX_REDIRECT_COUNT = 5;
	
	public InputStream getInputStream(String uri, long startP) {
		Log.e(TAG, "getInputStream");
		HttpURLConnection conn = null;
		conn = createConnection(uri, startP);
		Log.e(TAG, "leng:" + conn.getContentLength());
//		int redirectCount = 0;
//		while (conn.getResponseCode() / 100 == 3 && redirectCount < MAX_REDIRECT_COUNT) {
//			conn = createConnection(conn.getHeaderField("Location"));
//			redirectCount++;
//		}
		
		InputStream imageStream = null;
		int retry = 0;
		while(retry < 3) {
			try {
				imageStream = conn.getInputStream();
				break;
			} catch (IOException e) {
				Log.e(TAG, "ioException");
				e.printStackTrace();
			}
			retry++;
		}

		return imageStream;
		
	}
	
	public HttpURLConnection createConnection(String url, long startP) {
		Log.e(TAG, "createConnection");
		HttpURLConnection conn = null;
		try {
			
			String encodedUrl = Uri.encode(url, ALLOWED_URI_CHARS);
			conn = (HttpURLConnection) new URL(encodedUrl).openConnection();
			conn.setConnectTimeout(DEFAULT_HTTP_CONNECT_TIMEOUT);
			conn.setReadTimeout(DEFAULT_HTTP_READ_TIMEOUT);
//			conn.setRequestMethod("GET");
//			conn.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
//			conn.setRequestProperty("Accept-Language", "zh-CN");
//			conn.setRequestProperty("Referer", url); 
//			conn.setRequestProperty("Charset", "UTF-8");
//			conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
			conn.setRequestProperty("Range", "bytes=" + startP + "-");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return conn;
		
	}
	
	public static int deleteRequestExecute(HttpClient httpclient, HttpGet httpget) {
		
		BufferedReader in = null;
		int delcount = 0;
		
		try{
			
			HttpResponse response = httpclient.execute(httpget);
			in = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
			
			StringBuffer result = new StringBuffer();
			String line;
			
			while((line = in.readLine()) != null){
				result.append(line);
			}
			in.close();
			if(result != null) {
				
				int start = result.indexOf("<result>");
				int end = result.indexOf("</result>");
				
				if(start > 0 && end >0)
					delcount = Integer.parseInt(result.substring(start + 8, end));
			}
				Log.e(TAG, result.toString());
			return delcount;
		}catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

}