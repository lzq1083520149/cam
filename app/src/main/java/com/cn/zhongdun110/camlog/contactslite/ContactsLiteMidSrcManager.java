package com.cn.zhongdun110.camlog.contactslite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContactsEntity;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Identity;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.util.Log;
import cn.ingenic.contactslite.common.ContactPacket;
import cn.ingenic.contactslite.common.ContactPacketUtils;
import cn.ingenic.contactslite.common.ContactPacket.DataEntity;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.mid.Column;
import cn.ingenic.glasssync.services.mid.DefaultColumn;
import cn.ingenic.glasssync.services.mid.KeyColumn;
import cn.ingenic.glasssync.services.mid.MidException;
import cn.ingenic.glasssync.services.mid.MidTableManager;
import cn.ingenic.glasssync.services.mid.SimpleMidSrcManager;


public class ContactsLiteMidSrcManager extends SimpleMidSrcManager {
	private static MidTableManager sInstance;

	private Context mContext;

	private static final String TAG = "ContactsLiteMidSrcManager";
	
	private ContactsLiteMidSrcManager(Context context, SyncModule module) {
		super(context, module);
		mContext = context;
	}

	public synchronized static MidTableManager getInstance(Context context, SyncModule module) {
		if (sInstance == null) {
			sInstance = new ContactsLiteMidSrcManager(context, module);
		}
		return sInstance;
	}
	
	@Override
	protected Uri[] getSrcObservedUris() {
		Log.i(TAG, "getSrcObservedUris");
		return new Uri[]{ContactsContract.AUTHORITY_URI};
	}

	@Override
	protected List<Column> getSrcColumnList() {
		Log.i(TAG, "getSrcColumnList");
		List<Column> list = new ArrayList<Column>();
		list.add(new DefaultColumn(ContactsLiteMidTable.VERSION, Column.STRING));
		return list;
	}

	private class ContactsLiteMidTable {
		public static final String LOOKUP_KEY = ContactsContract.Contacts.LOOKUP_KEY;
		public static final String VERSION = ContactsContract.RawContacts.VERSION;
	}
	
	/**
	 * cache the query contact datas
	 */
	private Map<String, ContactPacket> mCacheContacts = new HashMap<String, ContactPacket>();
	
	/**
	 * @param keys changed contacts' lookup keys or null which means all contacts
	 * @return cursor this implementation return a cursor contains changed contact's id
	 */
	@Override
	protected Cursor getSrcDataCursor(Set keys) {
		Log.i(TAG, "getSrcDataCursor");
		mCacheContacts = readContacts(keys);
		String[] columnNames = new String[] {Contacts.LOOKUP_KEY, Contacts._ID, RawContacts.VERSION};
		MatrixCursor matrixCursor = new MatrixCursor(columnNames, mCacheContacts.size());
		for (String contactId : mCacheContacts.keySet()) {
			String lookupKey = mCacheContacts.get(contactId).getLookupKey();
			String version = mCacheContacts.get(contactId).getVersion();
			matrixCursor.addRow(new String[]{lookupKey, contactId, version});
		}
		return matrixCursor;
	}
	
	
//	private static final String[] CONTACTS_MINIMUM_PROJECTION = new String[] {
//		Contacts._ID, 
//		Contacts.LOOKUP_KEY,
//		Contacts.DISPLAY_NAME, 
//		//TODO replace with older sdk's API
//		Contacts.SORT_KEY_PRIMARY,
//	};
	private static final String[] CONTACT_PROJECTION= new String[] {
        Contacts._ID,                           // 0
        Contacts.DISPLAY_NAME_PRIMARY,          // 1
//        Contacts.CONTACT_PRESENCE,              // 2
//        Contacts.CONTACT_STATUS,                // 3
        Contacts.PHOTO_ID,                      // 4
        //Contacts.PHOTO_THUMBNAIL_URI,           // 5
        Contacts.LOOKUP_KEY,                    // 6
        Contacts.SORT_KEY_PRIMARY,               // 7
    };
	
	private static final String[] RAWCONTACTS_MINIMUM_PROJECTION = new String[] {
		RawContacts.VERSION, 
		RawContactsEntity.MIMETYPE, 
		RawContactsEntity.CONTACT_ID, 
		RawContactsEntity._ID,
		RawContactsEntity.DATA1, 
		RawContactsEntity.DATA2, 
		RawContactsEntity.DATA3, 
		RawContactsEntity.DATA4, 
		RawContactsEntity.DATA5, 
		RawContactsEntity.DATA6, 
		RawContactsEntity.DATA7, 
		RawContactsEntity.DATA8, 
		RawContactsEntity.DATA9, 
		RawContactsEntity.DATA10, 
		RawContactsEntity.DATA11, 
		RawContactsEntity.DATA12, 
		RawContactsEntity.DATA13, 
		RawContactsEntity.DATA14, 
		RawContactsEntity.DATA15, 
	};
	
	private synchronized Map<String, ContactPacket> readContacts(Collection<String> lookupKeyList) {
		Map<String, ContactPacket> contacts = new HashMap<String, ContactPacket>();
		//1. read contactId, name_raw_contact_id, lookup
		ContentResolver resolver = mContext.getContentResolver();
		String selection = buildSelectionByLookupKeyList(lookupKeyList);
		Cursor cursor = resolver.query(Contacts.CONTENT_URI, 
				CONTACT_PROJECTION, selection, null, null);
		if (cursor == null || cursor.getCount() == 0) return contacts;
		while (cursor.moveToNext()) {
			String id = cursor.getString(cursor.getColumnIndex(Contacts._ID));
			String lookupKey = cursor.getString(cursor.getColumnIndex(Contacts.LOOKUP_KEY));
			String displayName = cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME_PRIMARY));
			String sortKey = cursor.getString(cursor.getColumnIndex(Contacts.SORT_KEY_PRIMARY));
			int phoneId=cursor.getInt(cursor.getColumnIndex(Contacts.PHOTO_ID));
//			String phoneUri=cursor.getString(cursor.getColumnIndex(Contacts.PHOTO_THUMBNAIL_URI));
			ContactPacket packet = new ContactPacket(lookupKey);
			packet.mDisplayName = displayName;
			packet.mSortKey = sortKey;
			packet.mPhoneId=phoneId;
//			packet.mPhoneUri=phoneUri;
			contacts.put(id, packet);
		}
		cursor.close();
		//2. version, all data
		List<String> idList = new ArrayList<String>(contacts.keySet());
		String idSelection = buildSelectionByIdList(RawContactsEntity.CONTACT_ID, idList);
		if (idSelection == null) {
			//didn't find any 
			return contacts;
		}
	    
		Cursor dataCursor = resolver.query(RawContactsEntity.CONTENT_URI, 
				RAWCONTACTS_MINIMUM_PROJECTION, idSelection, null, null);
		if(dataCursor==null){
			dataCursor.close();
			return contacts;
		}
		while (dataCursor.moveToNext()) {
			String contactId = dataCursor.getString(dataCursor.getColumnIndex(RawContactsEntity.CONTACT_ID));
			String rawContactId = dataCursor.getString(dataCursor.getColumnIndex(RawContactsEntity._ID));
			String version = dataCursor.getString(dataCursor.getColumnIndex(RawContacts.VERSION));
			DataEntity dataEntity = getDataEntity(dataCursor);
			ContactPacket packet = contacts.get(contactId);
			if (packet != null) {
				packet.appendVersion(rawContactId, version);
				packet.addDataEntity(dataEntity);
				idList.remove(contactId);
			} else {
				Log.e(TAG, "can't find contact id " + contactId + " in maps.");
			}
		}
		dataCursor.close();
		for(String id:idList){
			contacts.remove(id);
		}
		
		return contacts;
	}
	
	//TODO move to common library
	private static String buildSelectionByIdList(String columnName, List<String> idList) {
		if (idList == null 
				|| idList.size() == 0) {
			return null;
		}
		
		StringBuilder inBuilder = new StringBuilder();
		int index = 0;
		for (String id : idList) {
			if (index == 0) {
				inBuilder.append("(");
			} else {
				inBuilder.append(",");
			}
			inBuilder.append(id);
			index++;
		}
		inBuilder.append(')');
		return columnName + " IN " + inBuilder.toString();
	}
	
	private DataEntity getDataEntity(Cursor cursor) {
		Resources res = mContext.getResources();
		String mimeType = cursor.getString(cursor.getColumnIndex(RawContactsEntity.MIMETYPE));
		String data = cursor.getString(cursor.getColumnIndex(Data.DATA1));
		String label = null;
		int type = -1;
		if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
			type = cursor.getInt(cursor.getColumnIndex(Phone.TYPE));
			label = cursor.getString(cursor.getColumnIndex(Phone.LABEL));
		} else if (Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
			type = cursor.getInt(cursor.getColumnIndex(Email.TYPE));
			label = cursor.getString(cursor.getColumnIndex(Email.LABEL));
		} else if (StructuredPostal.CONTENT_ITEM_TYPE.equals(mimeType)) {
			type = cursor.getInt(cursor.getColumnIndex(StructuredPostal.TYPE));
			label = cursor.getString(cursor.getColumnIndex(StructuredPostal.LABEL));
		} else if (Organization.CONTENT_ITEM_TYPE.equals(mimeType)) {
			label = cursor.getString(cursor.getColumnIndex(Organization.TITLE));
		} else if (Im.CONTENT_ITEM_TYPE.equals(mimeType)) {
			type = cursor.getInt(cursor.getColumnIndex(Im.PROTOCOL));
			label = cursor.getString(cursor.getColumnIndex(Im.CUSTOM_PROTOCOL));
		}else if(Event.CONTENT_ITEM_TYPE.equals(mimeType)){
			type = cursor.getInt(cursor.getColumnIndex(Event.DATA2));
		}else if (Note.CONTENT_ITEM_TYPE.equals(mimeType)) {
			//ignore no properties of this type
		} else if (SipAddress.CONTENT_ITEM_TYPE.equals(mimeType)) {
			//ignore
		} else if (Nickname.CONTENT_ITEM_TYPE.equals(mimeType)) {
			//ignore
		} else if (Website.CONTENT_ITEM_TYPE.equals(mimeType)) {
			//ignore
		} else if (Identity.CONTENT_ITEM_TYPE.equals(mimeType)) {
			//ignore
		}
		return new DataEntity(mimeType, data, type, label);
	}
	
	//TODO move to common library
	private static String buildSelectionByLookupKeyList(Collection<String> lookupKeyList) {
		if (lookupKeyList == null 
				|| lookupKeyList.size() == 0) {
			return null;
		}
		StringBuilder inBuilder = new StringBuilder();
		int index = 0;
		for (String lookupKey : lookupKeyList) {
			if (index == 0) {
				inBuilder.append("(");
			} else {
				inBuilder.append(",");
			}
			inBuilder.append( "'" + lookupKey + "'" );
			index++;
		}
		inBuilder.append(')');
		return Data.LOOKUP_KEY + " IN " + inBuilder.toString();
	}
	
	private static final String[] CONTACTS_MINIMUM_DATA = new String[] {
		Data.LOOKUP_KEY, RawContacts.VERSION,
		Data.DISPLAY_NAME, Data.SORT_KEY_PRIMARY,
		
		Data.MIMETYPE, 
		Data.DATA1, Data.DATA2, Data.DATA3, Data.DATA4, Data.DATA5, 
		Data.DATA6,	Data.DATA7, Data.DATA8, Data.DATA9,	Data.DATA10, 
		Data.DATA11, Data.DATA12, Data.DATA13, Data.DATA14, Data.DATA15,
	};
	
	/*
	 * how many contacts changed, how many times it will be call
	 */
	@Override
	protected void fillSrcSyncData(SyncData data, Cursor cursor)
			throws MidException {
//		Log.i(TAG, "fillSrcSyncData");
		String contactId = cursor.getString(cursor.getColumnIndex(Contacts._ID));
		ContactPacket contact = mCacheContacts.get(contactId);
		ContactPacketUtils.fillWith(data, contact);
//		Log.i(TAG, contact.toString());
	}

	@Override
	protected KeyColumn getSrcKeyColumn() {
		Log.i(TAG, "getSrcKeyColumn");
		//TODO CHECK
		return new KeyColumn(ContactsLiteMidTable.LOOKUP_KEY, Column.STRING);
	}
	
	@Override
	protected SyncData[] appendSrcSyncData(Set<Integer> positons, Cursor source)
			throws MidException {
		//TODO
		return null;
	}

	@Override
	protected String getMidAuthorityName() {
	    return mContext.getPackageName()+"CONTACTS_AUTHORITY";
	      //return mContext.getString(R.string.contactslite_midtable_authority);
	}
}
