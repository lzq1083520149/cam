package com.sctek.smartglasses.db;

import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.sctek.smartglasses.entity.ContactsDto;

public class DBContacts extends DBContactsBase {

        private static final String TAG = "DBContacts";
        private static final boolean DEBUG = true;

        public DBContacts(SQLiteDatabase db) {
                super(db);
        }

        public long insertToSys (ContactsDto contactsDto) {
                return _insert(ContactsDBHelper.TABLE_CONTACTS_SYS, contactsDto);
        }

        public void insertAllToSys (List<ContactsDto> contactsDtoList) {
                for (int i = 0, j = contactsDtoList.size(); i < j; i++) {
                        insertToSys(contactsDtoList.get(i));
                }
        }

        public int deleteSysByConditions (String conditions, String [] args) {
                return mDB.delete(ContactsDBHelper.TABLE_CONTACTS_SYS, conditions, args);
        }

        public ContactsDto findContactsByName (String name) {
                List<ContactsDto> contactsDtoList =  findByConditions(
                                ContactsDBHelper.COL_CONTACTS_DISPLAYNAME + " = ? "
                                , new String [] { name }
                                , null, null, null);
                if (contactsDtoList.size() > 0) {
                        return contactsDtoList.get(0);
                }
                return null;
        }

        public List<ContactsDto> queryObsoleteContacts () {
                List<ContactsDto> contactsDtoList = new ArrayList<ContactsDto>();
                StringBuilder builder = new StringBuilder("SELECT ");
                builder.append("t.").append(ContactsDBHelper.COL_CONTACTS_ID).append(", ");
                builder.append("t.").append(ContactsDBHelper.COL_CONTACTS_DISPLAYNAME).append(" ");
                builder.append("FROM ").append(ContactsDBHelper.TABLE_CONTACTS).append(" t ");
                builder.append("LEFT JOIN ").append(ContactsDBHelper.TABLE_CONTACTS_SYS).append(" s ");
                builder.append("ON (t.").append(ContactsDBHelper.COL_CONTACTS_DISPLAYNAME).append(" = ");
                builder.append("s.").append(ContactsDBHelper.COL_CONTACTS_DISPLAYNAME).append(") ");
                builder.append("WHERE ");
                builder.append("s.").append(ContactsDBHelper.COL_CONTACTS_DISPLAYNAME).append(" IS NULL ");

                if (DEBUG) Log.d(TAG, "SQL: " + builder.toString());
                Cursor cursor = mDB.rawQuery(builder.toString(), null);
                if (null != cursor) {
                        while (cursor.moveToNext()) {
                                ContactsDto contactsDto = new ContactsDto();
                                contactsDto.set_id(cursor.getLong(
                                                cursor.getColumnIndex(ContactsDBHelper.COL_CONTACTS_ID)));
                                contactsDto.setDisplayName(cursor.getString(
                                                cursor.getColumnIndex(ContactsDBHelper.COL_CONTACTS_DISPLAYNAME)));
                                contactsDtoList.add(contactsDto);
                        }
                        cursor.close();
                }
                return contactsDtoList;
        }

}
