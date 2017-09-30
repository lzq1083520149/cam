package com.sctek.smartglasses.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.sctek.smartglasses.entity.ContactsDto;
import com.sctek.smartglasses.entity.RemoteLiveBean;

public class DBRemoteLive {

	private static final String TAG = "DBRemoteLive";
	private static final boolean DEBUG = true;

	protected SQLiteDatabase mDB = null;

	public DBRemoteLive(SQLiteDatabase db) {
		if (null == db) {
			throw new NullPointerException("The db cannot be null.");
		}
		mDB = db;
	}

	public long insertToRmlive(RemoteLiveBean remoteLive) {
		ContentValues values = new ContentValues();
		values.put(RemLiveSQLiteOpenHelper.COL_UID, remoteLive.getUid());
		values.put(RemLiveSQLiteOpenHelper.COL_NAME, remoteLive.getName());
		values.put(RemLiveSQLiteOpenHelper.COL_PWD, remoteLive.getPwd());
		values.put(RemLiveSQLiteOpenHelper.COL_CLEARITY,
				remoteLive.getClearity());
		values.put(RemLiveSQLiteOpenHelper.COL_TIME, remoteLive.getTime());
		return mDB.insert(RemLiveSQLiteOpenHelper.DATABASE_TABLE_NAME, null,
				values);
	}

	public long updateRmlive(RemoteLiveBean remoteLive) {
		ContentValues values = new ContentValues();
		values.put(RemLiveSQLiteOpenHelper.COL_NAME, remoteLive.getName());
		values.put(RemLiveSQLiteOpenHelper.COL_PWD, remoteLive.getPwd());
		values.put(RemLiveSQLiteOpenHelper.COL_CLEARITY,
				remoteLive.getClearity());
		values.put(RemLiveSQLiteOpenHelper.COL_TIME, remoteLive.getTime());
		return mDB.update(RemLiveSQLiteOpenHelper.DATABASE_TABLE_NAME, values,
				"uid=?", new String[] { remoteLive.getUid() });
	}

	// clear remote live databases
	public long deleteAll() {
		return mDB.delete(RemLiveSQLiteOpenHelper.DATABASE_TABLE_NAME, null,
				null);
	}

	public List<RemoteLiveBean> findAll() {
		List<RemoteLiveBean> RemoteLiveBeanList = new ArrayList<RemoteLiveBean>();

		Cursor cursor = mDB.query(RemLiveSQLiteOpenHelper.DATABASE_TABLE_NAME,
				null, null, null, null, null, RemLiveSQLiteOpenHelper.COL_ID
						+ " desc");
		if (null != cursor) {
			while (cursor.moveToNext()) {
				RemoteLiveBean remoteLive = new RemoteLiveBean();
				remoteLive.set_id(cursor.getLong(cursor
						.getColumnIndex(RemLiveSQLiteOpenHelper.COL_ID)));
				remoteLive.setUid(cursor.getString(cursor
						.getColumnIndex(RemLiveSQLiteOpenHelper.COL_UID)));
				remoteLive.setName(cursor.getString(cursor
						.getColumnIndex(RemLiveSQLiteOpenHelper.COL_NAME)));
				remoteLive.setPwd(cursor.getString(cursor
						.getColumnIndex(RemLiveSQLiteOpenHelper.COL_PWD)));
				remoteLive.setClearity(cursor.getInt(cursor
						.getColumnIndex(RemLiveSQLiteOpenHelper.COL_CLEARITY)));
				remoteLive.setTime(cursor.getLong(cursor
						.getColumnIndex(RemLiveSQLiteOpenHelper.COL_TIME)));
				RemoteLiveBeanList.add(remoteLive);
			}
			cursor.close();
		}
		return RemoteLiveBeanList;
	}

	public long getCount(String conditions, String[] args) {
		long count = 0;
		if (TextUtils.isEmpty(conditions)) {
			conditions = " 1 = 1 ";
		}
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT COUNT(1) AS count FROM ");
		builder.append(RemLiveSQLiteOpenHelper.DATABASE_TABLE_NAME).append(" ");
		builder.append("WHERE ");
		builder.append(conditions);
		if (DEBUG)
			Log.d(TAG, "SQL: " + builder.toString());
		Cursor cursor = mDB.rawQuery(builder.toString(), args);
		if (null != cursor) {
			if (cursor.moveToNext()) {
				count = cursor.getLong(cursor.getColumnIndex("count"));
			}
			cursor.close();
		}
		return count;
	}

	public RemoteLiveBean findRemoteLiveLatest() {
		RemoteLiveBean remoteLive = null;
		Cursor cursor = mDB.query(RemLiveSQLiteOpenHelper.DATABASE_TABLE_NAME,
				null, null, null, null, null, RemLiveSQLiteOpenHelper.COL_TIME
						+ " desc");
		if (null != cursor) {
			if (cursor.moveToFirst()) {
				remoteLive = new RemoteLiveBean();
				remoteLive.set_id(cursor.getLong(cursor
						.getColumnIndex(RemLiveSQLiteOpenHelper.COL_ID)));
				remoteLive.setUid(cursor.getString(cursor
						.getColumnIndex(RemLiveSQLiteOpenHelper.COL_UID)));
				remoteLive.setName(cursor.getString(cursor
						.getColumnIndex(RemLiveSQLiteOpenHelper.COL_NAME)));
				remoteLive.setPwd(cursor.getString(cursor
						.getColumnIndex(RemLiveSQLiteOpenHelper.COL_PWD)));
				remoteLive.setClearity(cursor.getInt(cursor
						.getColumnIndex(RemLiveSQLiteOpenHelper.COL_CLEARITY)));
				remoteLive.setTime(cursor.getLong(cursor
						.getColumnIndex(RemLiveSQLiteOpenHelper.COL_TIME)));
			}
			cursor.close();
		}
		return remoteLive;
	}

	public RemoteLiveBean findRemoteLiveByUid(String uid) {
		RemoteLiveBean remoteLive = null;
		Cursor cursor = mDB.query(RemLiveSQLiteOpenHelper.DATABASE_TABLE_NAME,
				null, RemLiveSQLiteOpenHelper.COL_UID + "=?",
				new String[] { uid }, null, null, null);
		if (null != cursor) {
			if (cursor.moveToFirst()) {
				remoteLive = new RemoteLiveBean();
				remoteLive.set_id(cursor.getLong(cursor
						.getColumnIndex(RemLiveSQLiteOpenHelper.COL_ID)));
				remoteLive.setUid(cursor.getString(cursor
						.getColumnIndex(RemLiveSQLiteOpenHelper.COL_UID)));
				remoteLive.setName(cursor.getString(cursor
						.getColumnIndex(RemLiveSQLiteOpenHelper.COL_NAME)));
				remoteLive.setPwd(cursor.getString(cursor
						.getColumnIndex(RemLiveSQLiteOpenHelper.COL_PWD)));
				remoteLive.setClearity(cursor.getInt(cursor
						.getColumnIndex(RemLiveSQLiteOpenHelper.COL_CLEARITY)));
				remoteLive.setTime(cursor.getLong(cursor
						.getColumnIndex(RemLiveSQLiteOpenHelper.COL_TIME)));
			}
			cursor.close();
		}
		return remoteLive;
	}
}
