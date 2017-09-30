package com.sctek.smartglasses.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ContactsDBHelper extends SQLiteOpenHelper {

        private static final String TAG = "ContactsDBHelper";
        private static final boolean DEBUG = true;

        private static final String DB_NAME = "contacts.db";
        public static final int DB_VERSION = 1;

        protected static final String TABLE_CONTACTS = "contacts_ard";
        protected static final String TABLE_CONTACTS_SYS = "contacts_sys";
        protected static final String TABLE_DATA = "data_ard";
        protected static final String TABLE_DATA_SYS = "data_sys";

        protected static final String COL_CONTACTS_ID = "_id";
        protected static final String COL_CONTACTS_DISPLAYNAME = "display_name";

        protected static final String COL_DATA_ID = "_id";
        protected static final String COL_DATA_CONTACTID = "contact_id";
        protected static final String COL_DATA_TYPE = "type";
        protected static final String COL_DATA_DATA = "data";

        public static final String MIMETYPE_DIR = "vnd.android.cursor.dir/";
        public static final String MIMETYPE_ITEM = "vnd.android.cursor.item/";

        public static final String DATA_TYPE_PHONE = "phone"; // phone number

        // Create contacts table.
        private static final String SQL_CREATE_TABLE_CONTACTS = "CREATE TABLE " + TABLE_CONTACTS + " ("
                        + COL_CONTACTS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COL_CONTACTS_DISPLAYNAME + " TEXT) ";

        // Create system contacts table.
        private static final String SQL_CREATE_TABLE_CONTACTSSYS = "CREATE TABLE " + TABLE_CONTACTS_SYS + " ("
                        + COL_CONTACTS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COL_CONTACTS_DISPLAYNAME + " TEXT) ";

        // Create data table.
        private static final String SQL_CREATE_TABLE_DATA = "CREATE TABLE " + TABLE_DATA + " ("
                        + COL_DATA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COL_DATA_CONTACTID + " INTEGER NOT NULL, "
                        + COL_DATA_TYPE + " INTEGER, "
                        + COL_DATA_DATA + " TEXT) ";

        // Create system data table.
        private static final String SQL_CREATE_TABLE_DATASYS = "CREATE TABLE " + TABLE_DATA_SYS + " ("
                        + COL_DATA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COL_DATA_CONTACTID + " INTEGER NOT NULL, "
                        + COL_DATA_TYPE + " INTEGER, "
                        + COL_DATA_DATA + " TEXT) ";

        private static final String SQL_CLEAN_CONTACTS = "DELETE FROM " + TABLE_CONTACTS + " ";
        private static final String SQL_CLEAN_CONTACTSSYS = "DELETE FROM " + TABLE_CONTACTS_SYS + " ";
        private static final String SQL_CLEAN_DATA = "DELETE FROM " + TABLE_DATA + " ";
        private static final String SQL_CLEAN_DATASYS = "DELETE FROM " + TABLE_DATA_SYS + " ";

        private static ContactsDBHelper instance = null;

        public static ContactsDBHelper getInstance (Context context, CursorFactory factory) {
                if (null == instance) {
                        instance = new ContactsDBHelper(context, DB_NAME, factory, ContactsDBHelper.DB_VERSION);
                }
                return instance;
        }

        private ContactsDBHelper(Context context, String name,
                        CursorFactory factory, int version) {
                super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
                if (DEBUG) Log.i(TAG, "onCreate() in");
                db.execSQL(SQL_CREATE_TABLE_CONTACTS);
                db.execSQL(SQL_CREATE_TABLE_CONTACTSSYS);
                db.execSQL(SQL_CREATE_TABLE_DATA);
                db.execSQL(SQL_CREATE_TABLE_DATASYS);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                if (DEBUG) Log.i(TAG, "onUpgrade() in");
        }

        public void clearAllData () {
                if (DEBUG) Log.i(TAG, "clearAllData() in");
                SQLiteDatabase db = this.getWritableDatabase();
                db.execSQL(SQL_CLEAN_CONTACTS);
                db.execSQL(SQL_CLEAN_CONTACTSSYS);
                db.execSQL(SQL_CLEAN_DATA);
                db.execSQL(SQL_CLEAN_DATASYS);
        }
}
