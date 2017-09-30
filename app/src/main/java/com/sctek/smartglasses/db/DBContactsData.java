package com.sctek.smartglasses.db;

import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.sctek.smartglasses.entity.ContactsDataDto;

public class DBContactsData extends DBContactsDataBase {

        private static final String TAG = "DBContactsData";
        private static final boolean DEBUG = true;

        public DBContactsData(SQLiteDatabase db) {
                super(db);
        }

        public long insertToSys (ContactsDataDto dataDto) {
                return _insert(ContactsDBHelper.TABLE_DATA_SYS, dataDto);
        }

        public int deleteSysByCondtiions (String conditions, String [] args) {
                return mDB.delete(ContactsDBHelper.TABLE_DATA_SYS, conditions, args);
        }

        public long getCountByPhone (long contactId, String phone) {
                return getCount(ContactsDBHelper.COL_DATA_CONTACTID + " = ? AND "
                                + ContactsDBHelper.COL_DATA_TYPE + " = ? AND "
                                + ContactsDBHelper.COL_DATA_DATA + " = ? "
                            , new String [] { String.valueOf(contactId), ContactsDBHelper.DATA_TYPE_PHONE, phone });
        }

        private int _deleteByContactId (String tableName, long contactId) {
                return _deleteByConditions(tableName
                                , ContactsDBHelper.COL_DATA_CONTACTID + " = ? "
                                , new String [] { String.valueOf(contactId) });
        }

        public int deleteByContactId (long contactId) {
                return _deleteByContactId(ContactsDBHelper.TABLE_DATA, contactId);
        }

        public List<ContactsDataDto> queryObsoleteData () {
                List<ContactsDataDto> dataDtoList = new ArrayList<ContactsDataDto>();
                StringBuilder builder = new StringBuilder("SELECT ");
                builder.append("t.").append(ContactsDBHelper.COL_DATA_ID).append(", ");
                builder.append("t.").append(ContactsDBHelper.COL_DATA_CONTACTID).append(", ");
                builder.append("t.").append(ContactsDBHelper.COL_DATA_TYPE).append(", ");
                builder.append("t.").append(ContactsDBHelper.COL_DATA_DATA).append(" ");
                builder.append("FROM ").append(ContactsDBHelper.TABLE_DATA).append(" t ");
                builder.append("LEFT JOIN ").append(ContactsDBHelper.TABLE_DATA_SYS).append(" s ");
                builder.append("ON (t.").append(ContactsDBHelper.COL_DATA_CONTACTID).append(" = ");
                builder.append("s.").append(ContactsDBHelper.COL_DATA_CONTACTID).append(" ");
                builder.append("AND t.").append(ContactsDBHelper.COL_DATA_TYPE).append(" = ");
                builder.append("s.").append(ContactsDBHelper.COL_DATA_TYPE).append(" ");
                builder.append("AND t.").append(ContactsDBHelper.COL_DATA_DATA).append(" = ");
                builder.append("s.").append(ContactsDBHelper.COL_DATA_DATA).append(") ");
                builder.append("WHERE ");
                builder.append("s.").append(ContactsDBHelper.COL_DATA_DATA).append(" IS NULL ");

                if (DEBUG) Log.d(TAG, "SQL: " + builder.toString());
                Cursor cursor = mDB.rawQuery(builder.toString(), null);
                if (null != cursor) {
                        while (cursor.moveToNext()) {
                                ContactsDataDto dataDto = new ContactsDataDto();
                                dataDto.set_id(cursor.getLong(
                                                cursor.getColumnIndex(ContactsDBHelper.COL_DATA_ID)));
                                dataDto.setContactId(cursor.getLong(
                                                cursor.getColumnIndex(ContactsDBHelper.COL_DATA_CONTACTID)));
                                dataDto.setType(cursor.getString(
                                                cursor.getColumnIndex(ContactsDBHelper.COL_DATA_TYPE)));
                                dataDto.setData(cursor.getString(
                                                cursor.getColumnIndex(ContactsDBHelper.COL_DATA_DATA)));
                                dataDtoList.add(dataDto);
                        }
                        cursor.close();
                }
                return dataDtoList;
        }

}
