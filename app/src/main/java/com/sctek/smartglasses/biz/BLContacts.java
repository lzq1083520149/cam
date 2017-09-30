package com.sctek.smartglasses.biz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.util.Log;
import com.cn.zhongdun110.camlog.SyncApp;
import com.cn.zhongdun110.camlog.contactslite.ContactsModule;

import com.sctek.smartglasses.db.ContactsDBHelper;
import com.sctek.smartglasses.db.DBContacts;
import com.sctek.smartglasses.db.DBContactsData;
import com.sctek.smartglasses.entity.ContactsDataDto;
import com.sctek.smartglasses.entity.ContactsDto;

public class BLContacts {

        private static final String TAG = "BLContacts";
        private static final boolean DEBUG = true;

        private static final String JKEY_ADD = "add";
        private static final String JKEY_DEL_NAME = "del_name";
        private static final String JKEY_DEL_DATA = "del_data";
        private static final String JKEY_NAME = "display_name";
        private static final String JKEY_DATA = "data";

        private Context mContext = null;
        private ContactsDBHelper mDBHelper = null;
        private static BLContacts mInstance = null;
        private Handler mHandler = null;

        private Thread mSyncThread = null;

        private ContactObserver mContactObserver = null;

        public synchronized static BLContacts getInstance (Context context) {
                if (null == mInstance) {
                        mInstance = new BLContacts(context);
                }
                return mInstance;
        }

        private BLContacts (Context context) {
                mContext = context;
                mDBHelper = ContactsDBHelper.getInstance(context, null);
                mHandler = new ContactsHandler(mContext.getMainLooper());

                mContactObserver = new ContactObserver(mHandler);
                mContext.getContentResolver().registerContentObserver(
                                Phone.CONTENT_URI, false, mContactObserver);
        }

        public void syncContacts(boolean isRetry, boolean isFirstBind) {
                SharedPreferences preferences = mContext.getApplicationContext()
                                .getSharedPreferences(SyncApp.SHARED_FILE_NAME, Context.MODE_PRIVATE);
                String last_sync_contact_state = preferences.getString("last_sync_contact_state", "open");
                if (DEBUG) Log.i(TAG, "[last_sync_contact_state] " + last_sync_contact_state);
                if (!"open".equals(last_sync_contact_state)) {
                        return ;
                }
                if (isRetry) {
                        stopSyncContacts();
                }
                if ((null == mSyncThread) || (!mSyncThread.isAlive()) || isRetry) {
                        SyncContactsThread syncContactsThread = new SyncContactsThread();
                        syncContactsThread.setRetry(isRetry);
                        syncContactsThread.setFirstBind(isFirstBind);
                        mSyncThread = new Thread(syncContactsThread);
                        mSyncThread.start();
                }
        }

        public void stopSyncContacts () {
                if (DEBUG) Log.w(TAG, "stopSyncContacts() in");
                if ((null != mSyncThread) && (mSyncThread.isAlive())) {
                        mSyncThread.interrupt();
                        try {
                              mSyncThread.join(1000);
                      } catch (InterruptedException e) {
                              e.printStackTrace();
                      }
                }
        }

        private class SyncContactsThread implements Runnable {
                private boolean isRetry = false;
                private boolean isFirstBind = false;

                public boolean isRetry() {
                        return isRetry;
                }

                public void setRetry(boolean isRetry) {
                        this.isRetry = isRetry;
                }

                public boolean isFirstBind() {
                        return isFirstBind;
                }

                public void setFirstBind(boolean isFirstBind) {
                        this.isFirstBind = isFirstBind;
                }

                @Override
                public void run() {
                        SQLiteDatabase db = mDBHelper.getReadableDatabase();
                        DBContacts dbContacts = new DBContacts(db);
                        DBContactsData dbContactsData = new DBContactsData(db);
                        ContentResolver resolver = mContext.getContentResolver();
                        ContactsModule module = ContactsModule.getInstance(mContext);
                        Map<String, ContactsDto> addContactsMap = new HashMap<String, ContactsDto>();
                        Map<String, ContactsDto> mobileContactsMap = new HashMap<String, ContactsDto>();

                        /* Json data format:
                               {
                                       "add": [
                                           {"display_name": "zhangsan", "data": "13100001111"}
                                           , ...
                                       ], 
                                       "del_name": [
                                           {"display_name": "lisi"}
                                           , ...
                                       ], 
                                       "del_data": [
                                           {"display_name": "wangwu", "data": "13100002222"}
                                           , ...
                                       ]
                                }
                       */
                        JSONObject allContactsJsonObj = new JSONObject();
                        JSONArray addContactsJsonAry = new JSONArray();
                        JSONArray delContactsJsonAry = new JSONArray();
                        JSONArray delDataJsonAry = new JSONArray();

                        if (isRetry() || isFirstBind()) {
                                module.sendClearAll();
                                mDBHelper.clearAllData();
                        }

                        // Get all phone numbers.
                        Cursor cursor = resolver.query(Phone.CONTENT_URI
                                        , new String [] { Phone.CONTACT_ID, Phone.DISPLAY_NAME, Phone.NUMBER }
                                        , Phone.NUMBER + " IS NOT NULL", null, null);
                        if ((null == cursor) || (0 == cursor.getCount())) {
                                return ;
                        }
                        db.beginTransaction();
                        while (cursor.moveToNext() && (!Thread.currentThread().isInterrupted())) {
//                                long contactId = cursor.getLong(cursor.getColumnIndex(Phone.CONTACT_ID));
                                String displayName = cursor.getString(cursor.getColumnIndex(Phone.DISPLAY_NAME));
                                String phone = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
                                if (DEBUG) Log.w(TAG, "displayName = " + displayName + ", phone = " + phone);

                                // Exclude empty phone number.
                                if (TextUtils.isEmpty(phone)) {
                                        continue;
                                }
                                if (TextUtils.isEmpty(displayName)) {
                                        continue;
                                }

                                phone = phone.trim().replaceAll(" ", "").replaceAll("-", "");

                                ContactsDataDto dataDto = new ContactsDataDto();
                                dataDto.setContactId(-1);
                                dataDto.setType(ContactsDBHelper.DATA_TYPE_PHONE);
                                dataDto.setData(phone);

                                ContactsDto contactsDto = null;
                                ContactsDto mobileContactsDto = null;
                                ContactsDto contactsDto_db = null;
                                try {
                                        contactsDto_db = dbContacts.findContactsByName(displayName);
                                } catch (Exception e) {
                                        e.printStackTrace();
                                        continue;
                                }
                                if (null == contactsDto_db) { // The contacts is not exists in DB.
                                        if (DEBUG) Log.w(TAG, displayName + " is not exists in DB.");
                                        if (!addContactsMap.containsKey(displayName)) {
                                                if (DEBUG) Log.w(TAG, displayName + " is not exists in Map.");
                                                contactsDto_db = new ContactsDto();
                                                contactsDto_db.set_id(-1);
                                                contactsDto_db.setDisplayName(displayName);
                                                List<ContactsDataDto> dataDtoList = new ArrayList<ContactsDataDto>();
                                                contactsDto_db.setContactsDataDtoList(dataDtoList);
                                                addContactsMap.put(displayName, contactsDto_db);
                                                contactsDto = contactsDto_db;
                                        } else {
                                                if (DEBUG) Log.w(TAG, displayName + " is exists in Map.");
                                                contactsDto = addContactsMap.get(displayName);
                                        }
                                        mobileContactsDto = contactsDto;
                                } else { // The contacts is exists in DB.
                                        if (DEBUG) Log.w(TAG, displayName + " is exists in DB.");
                                        if (!addContactsMap.containsKey(displayName)) {
                                                if (DEBUG) Log.w(TAG, displayName + " is not exists in Map.");
                                                List<ContactsDataDto> dataDtoList = new ArrayList<ContactsDataDto>();
                                                contactsDto_db.setContactsDataDtoList(dataDtoList);
                                                addContactsMap.put(displayName, contactsDto_db);
                                                contactsDto = contactsDto_db;
                                        } else {
                                                if (DEBUG) Log.w(TAG, displayName + " is exists in Map.");
                                                contactsDto = addContactsMap.get(displayName);
                                        }
                                }

                                if (mobileContactsMap.containsKey(displayName)) {
                                        mobileContactsDto = mobileContactsMap.get(displayName);
                                } else {
                                        try {
                                                mobileContactsDto = (ContactsDto) contactsDto.clone();
                                        } catch (CloneNotSupportedException e) {
                                                e.printStackTrace();
                                        }
                                }
                                dataDto.setContactId(contactsDto.get_id());
                                long countData = 0;
                                try {
                                        countData = dbContactsData.getCountByPhone(contactsDto.get_id(), phone);
                                        if (DEBUG) Log.w(TAG, phone + ", getCount() = " + countData);
                                } catch (Exception e) {
                                        e.printStackTrace();
                                }
                                if (0 == countData) { // The contacts data is changed.
                                        contactsDto.getContactsDataDtoList().add(dataDto);
                                }
                                mobileContactsDto.getContactsDataDtoList().add(dataDto);

                                mobileContactsMap.put(displayName, mobileContactsDto);
                        }
                        db.endTransaction();
                        cursor.close();

                        if (Thread.currentThread().isInterrupted()) {
                                return ;
                        }

                        /* Sync new contacts and data. */
                        {
                                Set<String> nameSet = addContactsMap.keySet();
                                for (Iterator<String> it = nameSet.iterator(); it.hasNext() && (!Thread.currentThread().isInterrupted()); ) {
                                        String name = it.next();
                                        ContactsDto contactsDto = addContactsMap.get(name);
                                        long contactId = contactsDto.get_id();
                                        // Insert the contacts if it's new contacts.
                                        if (contactsDto.get_id() < 0) {
                                                contactId = dbContacts.insert(contactsDto);
                                        }
                                        List<ContactsDataDto> dataDtoList = contactsDto.getContactsDataDtoList();
                                        for (int k = 0, p = dataDtoList.size(); (k < p) && (!Thread.currentThread().isInterrupted()); k++) {
                                                ContactsDataDto dataDto = dataDtoList.get(k);
                                                dataDto.setContactId(contactId);
                                                if (DEBUG) Log.w(TAG, "==ADD==" + contactsDto.getDisplayName() + ", " + dataDto.getData());
                                                dbContactsData.insert(dataDto);
                                                try {
                                                        JSONObject contactsJsonObj = new JSONObject();
                                                        contactsJsonObj.put(JKEY_NAME, contactsDto.getDisplayName());
                                                        contactsJsonObj.put(JKEY_DATA, dataDto.getData());
                                                        addContactsJsonAry.put(contactsJsonObj);
                                                } catch (JSONException e) {
                                                        e.printStackTrace();
                                                }
                                        }
                                }
                        }

                        if (Thread.currentThread().isInterrupted()) {
                                return ;
                        }

                        /* Clear and Insert mobile contacts to sys table */
                        db.beginTransaction();
                        try {
                                dbContacts.deleteSysByConditions(null, null);
                                db.setTransactionSuccessful();
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                        db.endTransaction();

                        if (Thread.currentThread().isInterrupted()) {
                                return ;
                        }

                        db.beginTransaction();
                        try {
                                dbContacts.insertAllToSys(new ArrayList<ContactsDto>(mobileContactsMap.values()));
                                db.setTransactionSuccessful();
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                        db.endTransaction();

                        if (Thread.currentThread().isInterrupted()) {
                                return ;
                        }

                        db.beginTransaction();
                        try {
                                dbContactsData.deleteSysByCondtiions(null, null);
                                db.setTransactionSuccessful();
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                        db.endTransaction();

                        if (Thread.currentThread().isInterrupted()) {
                                return ;
                        }

                        db.beginTransaction();
                        try {
                                Set<String> nameSet = mobileContactsMap.keySet();
                                for (Iterator<String> it = nameSet.iterator(); it.hasNext() && (!Thread.currentThread().isInterrupted()); ) {
                                        String name = it.next();
                                        ContactsDto contactsDto_db = dbContacts.findContactsByName(name);
                                        if (null == contactsDto_db) {
                                                continue;
                                        }
                                        ContactsDto contactsDto = mobileContactsMap.get(name);
                                        List<ContactsDataDto> dataDtoList = contactsDto.getContactsDataDtoList();
                                        for (int i = 0, j = dataDtoList.size(); (i < j) && (!Thread.currentThread().isInterrupted()); i++) {
                                                ContactsDataDto dataDto = dataDtoList.get(i);
                                                dataDto.setContactId(contactsDto_db.get_id());
                                                dbContactsData.insertToSys(dataDto);
                                        }
                                }
                                db.setTransactionSuccessful();
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                        db.endTransaction();

                        if (Thread.currentThread().isInterrupted()) {
                                return ;
                        }

                        // Compare contacts between CONTACTS_ARD and CONTACTS_SYS. [deprecated contacts]
                        List<ContactsDto> obsoleteContactsList = dbContacts.queryObsoleteContacts();
                        for (int i = 0, j = obsoleteContactsList.size(); (i < j) && (!Thread.currentThread().isInterrupted()); i++) {
                                ContactsDto contactsDto = obsoleteContactsList.get(i);
                                if (DEBUG) Log.w(TAG, "==DELNAME==" + contactsDto.getDisplayName());
                                long contactId = contactsDto.get_id();
                                db.beginTransaction();
                                try {
                                        dbContacts.deleteByPrimaryKey(contactId);
                                        dbContactsData.deleteByContactId(contactId);
                                        db.setTransactionSuccessful();

                                        JSONObject contactsJsonObj = new JSONObject();
                                        contactsJsonObj.put(JKEY_NAME, contactsDto.getDisplayName());
                                        delContactsJsonAry.put(contactsJsonObj);
                                } catch (Exception e) {
                                        e.printStackTrace();
                                }
                                db.endTransaction();
                        }

                        if (Thread.currentThread().isInterrupted()) {
                                return ;
                        }

                        // Compare data between DATA_ARD and DATA_SYS. [deprecated contacts data]
                        List<ContactsDataDto> obsoleteDataList = dbContactsData.queryObsoleteData();
                        for (int i = 0, j = obsoleteDataList.size(); (i < j) && (!Thread.currentThread().isInterrupted()); i++) {
                                ContactsDataDto dataDto = obsoleteDataList.get(i);
                                db.beginTransaction();
                                ContactsDto contactsDto = null;
                                try {
                                        contactsDto = dbContacts.findByPrimaryKey(dataDto.getContactId());
                                        if (null == contactsDto) {
                                                continue;
                                        }
                                        dbContactsData.deleteByPrimaryKey(dataDto.get_id());
                                        db.setTransactionSuccessful();

                                        JSONObject dataJsonObj = new JSONObject();
                                        dataJsonObj.put(JKEY_NAME, contactsDto.getDisplayName());
                                        dataJsonObj.put(JKEY_DATA, dataDto.getData());
                                        delDataJsonAry.put(dataJsonObj);
                                } catch (Exception e) {
                                        e.printStackTrace();
                                } finally {
                                        db.endTransaction();
                                }
                                if (DEBUG) Log.w(TAG, "==DELDATA==" + contactsDto.getDisplayName() + ", " + dataDto.getData());
                        }

                        if ((addContactsJsonAry.length() > 0)
                                        || (delContactsJsonAry.length() > 0)
                                        || (delDataJsonAry.length() > 0)) {
                                try {
                                        allContactsJsonObj.put(JKEY_ADD, addContactsJsonAry);
                                        allContactsJsonObj.put(JKEY_DEL_NAME, delContactsJsonAry);
                                        allContactsJsonObj.put(JKEY_DEL_DATA, delDataJsonAry);
                                } catch (JSONException e) {
                                        e.printStackTrace();
                                }
                                if (Thread.currentThread().isInterrupted()) {
                                        return ;
                                }
                                if (DEBUG) Log.w(TAG, allContactsJsonObj.toString());
                                module.sendAllContactsJson(allContactsJsonObj);
                        }
                }
        };

        class ContactsHandler extends Handler {

                public ContactsHandler (Looper looper) {
                        super(looper);
                }

                @Override
                public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                }

        }

        class ContactObserver extends ContentObserver {

                public ContactObserver(Handler handler) {
                        super(handler);
                }

                @Override
                public void onChange(boolean selfChange, Uri uri) {
                        if (DEBUG) Log.i(TAG, "Contacts is changed.");
                        super.onChange(selfChange, uri);
                        syncContacts(false, false);
                }

        }
}
