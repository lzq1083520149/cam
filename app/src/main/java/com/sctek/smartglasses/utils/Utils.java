package com.sctek.smartglasses.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class Utils {

    public static String getClientUid(Context context){
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
		String ClientUid = preference.getString("ClientUid", null);
		if(null == ClientUid || ClientUid.length() == 0){
			ClientUid = java.util.UUID.randomUUID().toString();
			Editor editor = preference.edit();
			editor.putString("ClientUid", ClientUid);
			editor.commit();
		}
		return ClientUid;
	}
}