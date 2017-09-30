package com.sctek.smartglasses.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.sctek.smartglasses.entity.ContactsDto;

public class DBContactsBase {

        private static final String TAG = "DBContactsBase";
        private static final boolean DEBUG = true;

        protected SQLiteDatabase mDB = null;

        public DBContactsBase (SQLiteDatabase db) {
                if (null == db) {
                        throw new NullPointerException("The db cannot be null.");
                }
                mDB = db;
        }

        protected long _insert (String tableName, ContactsDto contactsDto) {
                ContentValues values = new ContentValues();
                values.put(ContactsDBHelper.COL_CONTACTS_DISPLAYNAME, contactsDto.getDisplayName());
                return mDB.insert(tableName, null, values);
        }

        public long insert (ContactsDto contactsDto) {
                return _insert(ContactsDBHelper.TABLE_CONTACTS, contactsDto);
        }

        public void insertAll (List<ContactsDto> contactsDtoList) {
                for (int i = 0, j = contactsDtoList.size(); i < j; i++) {
                        _insert(ContactsDBHelper.TABLE_CONTACTS, contactsDtoList.get(i));
                }
        }

        public ContactsDto findByPrimaryKey (long id) {
                ContactsDto contactsDto = null;

                StringBuilder builder = new StringBuilder("SELECT ");
                builder.append(ContactsDBHelper.COL_CONTACTS_ID).append(", ");
                builder.append(ContactsDBHelper.COL_CONTACTS_DISPLAYNAME).append(" ");
                builder.append("FROM ").append(ContactsDBHelper.TABLE_CONTACTS).append(" ");
                builder.append("WHERE ");
                builder.append(ContactsDBHelper.COL_CONTACTS_ID).append(" = ? ");

                if (DEBUG) Log.d(TAG, "SQL: " + builder.toString());
                Cursor cursor = mDB.rawQuery(builder.toString(), new String [] { String.valueOf(id) });
                if (null != cursor) {
                        if (cursor.moveToNext()) {
                                contactsDto = new ContactsDto();
                                contactsDto.set_id(cursor.getLong(cursor.getColumnIndex(ContactsDBHelper.COL_CONTACTS_ID)));
                                contactsDto.setDisplayName(cursor.getString(cursor.getColumnIndex(ContactsDBHelper.COL_CONTACTS_DISPLAYNAME)));
                        }
                        cursor.close();
                }

                return contactsDto;
        }

        public List<ContactsDto> findByConditions (String selection, String [] selectionArgs
                        , String groupBy, String having, String orderBy) {
                List<ContactsDto> contactsDtoList = new ArrayList<ContactsDto>();

                Cursor cursor = mDB.query(ContactsDBHelper.TABLE_CONTACTS
                                , new String [] { ContactsDBHelper.COL_CONTACTS_ID
                                        , ContactsDBHelper.COL_CONTACTS_DISPLAYNAME }
                                , selection, selectionArgs, groupBy, having, orderBy);
                if (null != cursor) {
                        while (cursor.moveToNext()) {
                                ContactsDto contactsDto = new ContactsDto();
                                contactsDto.set_id(cursor.getLong(cursor.getColumnIndex(ContactsDBHelper.COL_CONTACTS_ID)));
                                contactsDto.setDisplayName(cursor.getString(cursor.getColumnIndex(ContactsDBHelper.COL_CONTACTS_DISPLAYNAME)));
                                contactsDtoList.add(contactsDto);
                        }
                        cursor.close();
                }

                return contactsDtoList;
        }

        public int updateByPrimaryKey (long id, ContactsDto contactsDto) {
                ContentValues values = new ContentValues();
                values.put(ContactsDBHelper.COL_CONTACTS_DISPLAYNAME, contactsDto.getDisplayName());
                return mDB.update(ContactsDBHelper.TABLE_CONTACTS
                                , values
                                , ContactsDBHelper.COL_CONTACTS_ID + " = ? "
                                , new String [] { String.valueOf(id) });
        }

        public int updateByConditions (ContactsDto contactsDto, String conditions, String [] args) {
                ContentValues values = new ContentValues();
                values.put(ContactsDBHelper.COL_CONTACTS_DISPLAYNAME, contactsDto.getDisplayName());
                return mDB.update(ContactsDBHelper.TABLE_CONTACTS, values, conditions, args);
        }

        public int deleteByPrimaryKey (long id) {
                return mDB.delete(ContactsDBHelper.TABLE_CONTACTS
                                , ContactsDBHelper.COL_CONTACTS_ID + " = ? "
                                , new String [] { String.valueOf(id) });
        }

        public int deleteByConditions (String conditions, String [] args) {
                return mDB.delete(ContactsDBHelper.TABLE_CONTACTS, conditions, args);
        }

        public long getCount (String conditions, String [] args) {
                long count = 0;

                if (TextUtils.isEmpty(conditions)) {
                        conditions = " 1 = 1 ";
                }

                StringBuilder builder = new StringBuilder();
                builder.append("SELECT COUNT(1) AS count FROM ");
                builder.append(ContactsDBHelper.TABLE_CONTACTS).append(" ");
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
