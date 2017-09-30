package com.sctek.smartglasses.language;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cn.zhongdun110.camlog.R;
import com.sctek.smartglasses.fragments.SettingFragment;

import java.util.Locale;

import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncException;
import cn.ingenic.glasssync.services.SyncModule;

public class LanguageModule extends SyncModule {
	private final String TAG = "LanguageModule";
	public static String SMS_NAME = "lang_module";
	private final String LANGUAGE_TYPE = "languageType";
        private final String SYNC_RESULT = "result";
        private final int COMPLETE = 1;
	private static LanguageModule mInstance;
        private Context mContext;

	public static LanguageModule getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new LanguageModule(context);
		}
		return mInstance;
	}

	private LanguageModule(Context context) {
		super(SMS_NAME, context);
		mContext  = context;
	}

        @Override
	protected void onCreate() {

	}


	public void sendSyncRequest(int languageType) {
		Log.i(TAG, "---sendSyncRequest :: languageType = "+ languageType);
		SyncData data = new SyncData();
		data.putInt(LANGUAGE_TYPE, languageType);
		try {
			send(data);
		} catch (SyncException e) {
			Log.e(TAG, "---send sync failed:" + e);
		}
	}

	public void initLanguage(){
		sendSyncRequest(checkLanguage());
	}

        @Override
	protected void onRetrive(SyncData data) {
	    int type = data.getInt("type");
	    int result = data.getInt("result");
	    int language = data.getInt("language");
	    Log.i(TAG,"onRetrive::type =" + type + " result = "+result + "language = " + language);
	    switch(type){
	    case COMPLETE :
		 if(result == 1){
			 setCurrentLanguage(language);	
		 }
		break;
	    }
	}


	private void setCurrentLanguage(int type){
		Log.e(TAG,"setCurrentLanguage ------------------");
		SharedPreferences pref = PreferenceManager
			.getDefaultSharedPreferences(mContext);
		Editor editor = pref.edit();
		switch(type){
		case SettingFragment.LANGUAGE_ZH:
			editor.putString("language", mContext.getString(R.string.language_zh));
			break;
		case SettingFragment.LANGUAGE_US:
			editor.putString("language", mContext.getString(R.string.language_us));
			break;
		case SettingFragment.LANGUAGE_FR:
			editor.putString("language", mContext.getString(R.string.language_fr));
			break;
		case SettingFragment.LANGUAGE_RU:
			editor.putString("language", mContext.getString(R.string.language_ru));
			break;
		case SettingFragment.LANGUAGE_DE:
			editor.putString("language", mContext.getString(R.string.language_de));
			break;
		case SettingFragment.LANGUAGE_TH:
			editor.putString("language", mContext.getString(R.string.language_th));
			break;
		case SettingFragment.LANGUAGE_FA:
			editor.putString("language", mContext.getString(R.string.language_fa));
			break;
		case SettingFragment.LANGUAGE_ES:
			editor.putString("language", mContext.getString(R.string.language_es));
			break;
		case SettingFragment.LANGUAGE_PT:
			editor.putString("language", mContext.getString(R.string.language_pt));
			break;
		case SettingFragment.LANGUAGE_AR:
			editor.putString("language", mContext.getString(R.string.language_ar));
			break;
		case SettingFragment.LANGUAGE_IT:
			editor.putString("language", mContext.getString(R.string.language_it));
			break;
		}
		editor.commit();
	}

	private int checkLanguage() {
		Locale locale = mContext.getResources().getConfiguration().locale;
		String language = locale.getLanguage();
		if (language.endsWith("zh"))
			return SettingFragment.LANGUAGE_ZH;
		else if (language.endsWith("en"))
			return SettingFragment.LANGUAGE_US;
		else if (language.endsWith("fr"))
			return SettingFragment.LANGUAGE_FR;
		else if (language.endsWith("ru"))
			return SettingFragment.LANGUAGE_RU;
		else if (language.endsWith("de"))
			return SettingFragment.LANGUAGE_DE;
		else if (language.endsWith("th"))
			return SettingFragment.LANGUAGE_TH;
		else if (language.endsWith("fa"))
			return SettingFragment.LANGUAGE_FA;
		else if (language.endsWith("es"))
			return SettingFragment.LANGUAGE_ES;
		else if (language.endsWith("pt"))
			return SettingFragment.LANGUAGE_PT;
		else if (language.endsWith("ar"))
			return SettingFragment.LANGUAGE_AR;
		else if (language.endsWith("it"))
			return SettingFragment.LANGUAGE_IT;
		return SettingFragment.LANGUAGE_ZH;
	}
}