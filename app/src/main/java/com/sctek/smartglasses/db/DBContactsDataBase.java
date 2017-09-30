package com.sctek.smartglasses.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.sctek.smartglasses.entity.ContactsDataDto;

public class DBContactsDataBase {

        private static final String TAG = "DBContactsDataBase";
        private static final boolean DEBUG = true;

        protected SQLiteDatabase mDB = null;

        public DBContactsDataBase (SQLiteDatabase db) {
                if (null == db) {
                        throw new NullPointerException("The db cannot be null.");
                }
                mDB = db;
        }

        protected long _insert (String tableName, ContactsDataDto dataDto) {
                ContentValues values = new ContentValues();
                values.put(ContactsDBHelper.COL_DATA_CONTACTID, dataDto.getContactId());
                values.put(ContactsDBHelper.COL_DATA_TYPE, dataDto.getType());
                values.put(ContactsDBHelper.COL_DATA_DATA, dataDto.getData());

                return mDB.insert(tableName, null, values);
        }

        public long insert (ContactsDataDto dataDto) {
                return _insert(ContactsDBHelper.TABLE_DATA, dataDto);
        }

        public void insertAll (List<ContactsDataDto> dataDtoList) {
                for (int i = 0, j = dataDtoList.size(); i < j; i++) {
                        _insert(ContactsDBHelper.TABLE_DATA, dataDtoList.get(i));
                }
        }

        public ContactsDataDto findByPrimaryKey (long id) {
                ContactsDataDto dataDto = null;

                Cursor cursor = mDB.query(ContactsDBHelper.TABLE_DATA
                                , new String [] { ContactsDBHelper.COL_DATA_ID
                                        , ContactsDBHelper.COL_DATA_CONTACTID
                                        , ContactsDBHelper.COL_DATA_TYPE
                                        , ContactsDBHelper.COL_DATA_DATA }
                                , ContactsDBHelper.COL_DATA_ID + " = ? "
                                , new String [] { String.valueOf(id) }
                                , null, null, null);
                if (null != cursor) {
                        if (cursor.moveToNext()) {
                                dataDto = new ContactsDataDto();
                                dataDto.set_id(cursor.getLong(cursor.getColumnIndex(ContactsDBHelper.COL_DATA_ID)));
                                dataDto.setContactId(cursor.getLong(cursor.getColumnIndex(ContactsDBHelper.COL_DATA_CONTACTID)));
                                dataDto.setType(cursor.getString(cursor.getColumnIndex(ContactsDBHelper.COL_DATA_TYPE)));
                                dataDto.setData(cursor.getString(cursor.getColumnIndex(ContactsDBHelper.COL_DATA_DATA)));
                        }
                        cursor.close();
                }

                return dataDto;
        }

        public List<ContactsDataDto> findByConditions (String selection, String [] selectionArgs
                        , String groupBy, String having, String orderBy) {
                List<ContactsDataDto> dataDtoList = new ArrayList<ContactsDataDto>();

                Cursor cursor = mDB.query(ContactsDBHelper.TABLE_DATA
                                , new String [] { ContactsDBHelper.COL_DATA_ID
                                        , ContactsDBHelper.COL_DATA_CONTACTID
                                        , ContactsDBHelper.COL_DATA_TYPE
                                        , ContactsDBHelper.COL_DATA_DATA }
                                , selection, selectionArgs, groupBy, having, orderBy);
                if (null != cursor) {
                        while (cursor.moveToNext()) {
                                ContactsDataDto dataDto = new ContactsDataDto();
                                dataDto.set_id(cursor.getLong(cursor.getColumnIndex(ContactsDBHelper.COL_DATA_ID)));
                                dataDto.setContactId(cursor.getLong(cursor.getColumnIndex(ContactsDBHelper.COL_DATA_CONTACTID)));
                                dataDto.setType(cursor.getString(cursor.getColumnIndex(ContactsDBHelper.COL_DATA_TYPE)));
                                dataDto.setData(cursor.getString(cursor.getColumnIndex(ContactsDBHelper.COL_DATA_DATA)));
                                dataDtoList.add(dataDto);
                        }
                        cursor.close();
                }

                return dataDtoList;
        }

        public int updateByPrimaryKey (long id, ContactsDataDto dataDto) {
                ContentValues values = new ContentValues();
                values.put(ContactsDBHelper.COL_DATA_CONTACTID, dataDto.getContactId());
                values.put(ContactsDBHelper.COL_DATA_TYPE, dataDto.getType());
                values.put(ContactsDBHelper.COL_DATA_DATA, dataDto.getData());
                return mDB.update(ContactsDBHelper.TABLE_DATA
                                , values
                                , ContactsDBHelper.COL_DATA_ID + " = ? "
                                , new String [] { String.valueOf(id) });
        }

        public int updateByConditions (ContactsDataDto dataDto, String conditions, String [] args) {
                ContentValues values = new ContentValues();
                values.put(ContactsDBHelper.COL_DATA_CONTACTID, dataDto.getContactId());
                values.put(ContactsDBHelper.COL_DATA_TYPE, dataDto.getType());
                values.put(ContactsDBHelper.COL_DATA_DATA, dataDto.getData());
                return mDB.update(ContactsDBHelper.TABLE_DATA, values, conditions, args);
        }

        public int deleteByPrimaryKey (long id) {
                return mDB.delete(ContactsDBHelper.TABLE_DATA
                                , ContactsDBHelper.COL_DATA_ID + " = ? "
                                , new String [] { String.valueOf(id) });
        }

        protected int _deleteByConditions (String tableName, String conditions, String [] args) {
                return mDB.delete(tableName, conditions, args);
        }

        public int deleteByConditions (String conditions, String [] args) {
                return _deleteByConditions(ContactsDBHelper.TABLE_DATA, conditions, args);
        }

        public long getCount (String conditions, String [] args) {
                long count = 0;

                if (TextUtils.isEmpty(conditions)) {
                        conditions = " 1 = 1 ";
                }

                StringBuilder builder = new StringBuilder("");
                builder.append("SELECT COUNT(1) AS count FROM ");
                builder.append(ContactsDBHelper.TABLE_DATA).append(" ");
                builder.append("WHERE ");
                builder.append(conditions);

                if (DEBUG) Log.d(TAG, "SQL: " + builder.toString());
                Cursor cursor = mDB.rawQuery(builder.toString(), args);
                if (null != cursor) {
                        if (cursor.moveToNext()) {
                                count = cursor.getLong(cursor.getColumnIndex("count"));
                        }
                        cursor.close();
                }

                return count;
        }
}
