/*
 * Copyright (C) 2010-2013 The SINA WEIBO Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cn.zhongdun110.camlog.sinaapi;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cn.zhongdun110.camlog.R;

import java.io.File;

/**
 * 该类主要演示如何进行授权、SSO登陆。
 * 
 * @author SINA
 * @since 2013-09-29
 */
public class WBAuthAndShareActivity extends Activity {

	private static final String TAG = "WBAuthAndShareActivity";

	public static int GSMMD_PIC = 0x1;
	public static int GSMMD_VIDEO = 0x2;

	public static final String KEY_SHARE_CONTENT_TEXT = "key_share_content_text";
	public static final String KEY_SHARE_CONTENT_IMAGE = "key_share_content_image";
	public static final String KEY_SHARE_CONTENT_VIDEO = "key_share_content_video";
	public static final String KEY_SHARE_FROM_GLASSSYNC = "key_share_from_glasssync";

	private Object mAuthInfo;

	/** 封装了 "access_token"，"expires_in"，"refresh_token"，并提供了他们的管理功能 */
//	private Oauth2AccessToken mAccessToken;
//
//	/** 注意：SsoHandler 仅当 SDK 支持 SSO 时有效 */
//	private SsoHandler mSsoHandler;
//
//	private StatusesAPI mStatusesAPI;

	ProgressBar mProgressBar;
	TextView mContentTv;

	/**
	 * @see {@link Activity#onCreate}
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!getIntent().getBooleanExtra("fromGlassSync", false)) {
			finish();
			return;
		}

		setContentView(R.layout.activity_weibo);
		findView();

		// 创建微博实例
		// mWeiboAuth = new WeiboAuth(this, Constants.APP_KEY,
		// Constants.REDIRECT_URL, Constants.SCOPE);
		// 快速授权时，请不要传入 SCOPE，否则可能会授权不成功
//		mAuthInfo = new AuthInfo(this, Constants.APP_KEY,
//				Constants.REDIRECT_URL, Constants.SCOPE);
//		mSsoHandler = new SsoHandler(WBAuthAndShareActivity.this, mAuthInfo);
//
//		// 从 SharedPreferences 中读取上次已保存好 AccessToken 等信息，
//		// 第一次启动本应用，AccessToken 不可用
//		mAccessToken = AccessTokenKeeper.readAccessToken(this);
//		if (mAccessToken.isSessionValid()) {
//			Log.i(TAG, "已授权");
//			mStatusesAPI = new StatusesAPI(this, Constants.APP_KEY,
//					mAccessToken);
//			sendToWB();
//		} else {
//			Log.i(TAG, "未授权");
//			mSsoHandler.authorize(new AuthListener());
//		}
	}

	private void findView() {
		mProgressBar = (ProgressBar) findViewById(R.id.progress);
		Drawable progress = getResources().getDrawable(R.drawable.progress);
		mProgressBar.setIndeterminateDrawable(progress);
		mContentTv = (TextView) findViewById(R.id.tv_content);
	}

	private void sendToWB() {
		int fileType = getIntent().getIntExtra("type", 0);
		if (fileType == GSMMD_PIC) {
			// send pic
			String imgPath = getIntent()
					.getStringExtra(KEY_SHARE_CONTENT_IMAGE);
			File file = new File(imgPath); // file must exist.
			if (file.exists()) {
				// 发送一条带本地图片的微博
				Log.i(TAG, "----------pic -path=" + imgPath);
				Bitmap bmp = BitmapFactory.decodeFile(imgPath);
//				mStatusesAPI.upload("", bmp, null, null, mListener);
			} else
				Log.i(TAG, "----------pic not exist!----path=" + imgPath);
		} else if (fileType == GSMMD_VIDEO) {
			// send video
			String path = getIntent().getStringExtra(KEY_SHARE_CONTENT_VIDEO);
		} else {
			// send text
			String text = getIntent().getStringExtra(KEY_SHARE_CONTENT_TEXT);
			Log.i(TAG, "----------0000000--------text=" + text);
			if (text != null) {
				mProgressBar.setVisibility(View.VISIBLE);
				mProgressBar.setIndeterminate(true);

				mContentTv.setText(text);
//				mStatusesAPI.update(text, null, null, mListener);
			}
		}
	}

	/**
	 * 当 SSO 授权 Activity 退出时，该函数被调用。
	 * 
	 * @see {@link Activity#onActivityResult}
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// SSO 授权回调
		// 重要：发起 SSO 登陆的 Activity 必须重写 onActivityResult
//		if (mSsoHandler != null) {
//			mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
//		}
	}

	/**
	 * 微博认证授权回调类。 1. SSO 授权时，需要在 {@link #onActivityResult} 中调用
	 * {@link SsoHandler#authorizeCallBack} 后， 该回调才会被执行。 2. 非 SSO
	 * 授权时，当授权结束后，该回调就会被执行。 当授权成功后，请保存该 access_token、expires_in、uid 等信息到
	 * SharedPreferences 中。
	 */
//	class AuthListener implements WeiboAuthListener {
//
//		@Override
//		public void onComplete(Bundle values) {
//			// 从 Bundle 中解析 Token
//			mAccessToken = Oauth2AccessToken.parseAccessToken(values);
//			if (mAccessToken.isSessionValid()) {
//				// 保存 Token 到 SharedPreferences
//				AccessTokenKeeper.writeAccessToken(WBAuthAndShareActivity.this,
//						mAccessToken);
//				Toast.makeText(WBAuthAndShareActivity.this,
//						R.string.weibo_auth_success, Toast.LENGTH_SHORT).show();
//				Log.i(TAG, "onComplete weibosdk_demo_toast_auth_success");
//				mStatusesAPI = new StatusesAPI(WBAuthAndShareActivity.this,
//						Constants.APP_KEY, mAccessToken);
//				sendToWB();
//			} else {
//				// 以下几种情况，您会收到 Code：
//				// 1. 当您未在平台上注册的应用程序的包名与签名时；
//				// 2. 当您注册的应用程序包名与签名不正确时；
//				// 3. 当您在平台上注册的包名和签名与您当前测试的应用的包名和签名不匹配时。
//				String code = values.getString("code");
//				String message = getString(R.string.weibo_auth_failed);
//				if (!TextUtils.isEmpty(code)) {
//					message = message + "\nObtained the code: " + code;
//				}
//				Toast.makeText(WBAuthAndShareActivity.this, message,
//						Toast.LENGTH_LONG).show();
//				Log.i(TAG, "onComplete message:" + message);
//				finish();
//			}
//		}

//		@Override
//		public void onCancel() {
//			Toast.makeText(WBAuthAndShareActivity.this,
//					R.string.weibo_auth_canceled, Toast.LENGTH_LONG).show();
//			Log.i(TAG, "weibosdk_demo_toast_auth_canceled");
//			finish();
//		}
//
//		@Override
//		public void onWeiboException(WeiboException e) {
//			Toast.makeText(WBAuthAndShareActivity.this,
//					"Auth exception : " + e.getMessage(), Toast.LENGTH_LONG)
//					.show();
//			Log.e(TAG, "Auth exception : " + e.getMessage());
//			finish();
//		}
//	}

	/**
	 * 微博 OpenAPI 回调接口。
	 */
//	private RequestListener mListener = new RequestListener() {
//		@Override
//		public void onComplete(String response) {
//			if (!TextUtils.isEmpty(response)) {
//				Log.i(TAG, response);
//				if (response.startsWith("{\"created_at\"")) {
//					// 调用 Status#parse 解析字符串成微博对象
//					Status status = Status.parse(response);
//					Toast.makeText(WBAuthAndShareActivity.this,
//							R.string.weibo_share_success, Toast.LENGTH_LONG)
//							.show();
//				} else {
//					Toast.makeText(WBAuthAndShareActivity.this, response,
//							Toast.LENGTH_LONG).show();
//				}
//			}
//			finish();
//		}
//
//		@Override
//		public void onWeiboException(WeiboException e) {
//			Log.e(TAG, "onWeiboException " + e.getMessage());
//			ErrorInfo info = ErrorInfo.parse(e.getMessage());
//			Log.e(TAG, info.toString());
//			Toast.makeText(WBAuthAndShareActivity.this,
//					R.string.weibo_share_failed, Toast.LENGTH_LONG).show();
//			finish();
//		}
//	};
}