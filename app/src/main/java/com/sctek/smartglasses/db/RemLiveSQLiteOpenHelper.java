package com.sctek.smartglasses.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class RemLiveSQLiteOpenHelper extends SQLiteOpenHelper {

	private static String REMOTE_LIVE_DATABASE_NAME = "remotelive.db";
	private static int version = 1;
	public static final String DATABASE_TABLE_NAME = "rmlive";
	public static final String COL_ID = "_id";
	public static final String COL_UID = "uid";
	public static final String COL_NAME = "name";
	public static final String COL_PWD = "pwd";
	public static final String COL_TIME = "time";// modify time
	public static final String COL_CLEARITY = "clearity";
	private final String REMOTE_LIVE_DATABASE_CREATE ="create table IF NOT EXISTS "+DATABASE_TABLE_NAME+"("+
			COL_ID +" integer primary key autoincrement,"+
			COL_UID +" text," + COL_NAME +" text," +COL_PWD +" text,"+COL_CLEARITY +" integer,"+
			COL_TIME+" integer)";
	private static RemLiveSQLiteOpenHelper mInstance = null;
	private static Context mContext;

	public static RemLiveSQLiteOpenHelper getInstance(Context context) {
		if (null == mInstance) {
			mInstance = new RemLiveSQLiteOpenHelper(context);
			mContext = context;
		}
		return mInstance;
	}

	private RemLiveSQLiteOpenHelper(Context context) {
		super(context, REMOTE_LIVE_DATABASE_NAME, null, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(REMOTE_LIVE_DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {

	}

}
