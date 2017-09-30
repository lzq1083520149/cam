package com.cn.zhongdun110.camlog.contactslite;

import org.json.JSONObject;
import android.content.Context;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncException;
import cn.ingenic.glasssync.services.SyncModule;
import com.sctek.smartglasses.biz.BLContacts;

public class ContactsModule extends SyncModule {

        private static final String TAG = "ContactsModule";
        private static final boolean DEBUG = true;

        private static final String MODULE_NAME = "contacts_module";

        private final String KEY_TYPE = "type";
        private final String KEY_SYNCCMD = "cmd";
        private final String KEY_SYNCNAME = "name";
        private final String KEY_SYNCPHONE = "phone";
        private final String KEY_SYNCALL = "alljson";
        private final String KEY_CONTACTSLEN = "contacts_len";

        private final int TYPE_SYNC = 0;

        private final int CMD_ADD = 0;
        private final int CMD_DELNAME = 1;
        private final int CMD_DELPHONE = 2;
        private final int CMD_CLEARALL = 3;
        private final int CMD_SYNCALLLEN = 4;
        private final int CMD_SYNCALL = 5;
        private final int CMD_RESYNCALL = 6; // Retry sync all contacts.

        private static ContactsModule instance = null;

        private Context mContext = null;

        private ContactsModule(String name, Context context) {
                super(name, context);
                mContext = context;
        }

        public static ContactsModule getInstance (Context context) {
                if (null == instance) {
                        instance = new ContactsModule(MODULE_NAME, context);
                }
                return instance;
        }

        @Override
        protected void onCreate() {
        }

        @Override
        protected void onRetrive(SyncData data) {
                super.onRetrive(data);
                int cmd = data.getInt(KEY_SYNCCMD);
                if (DEBUG) Log.w(TAG, "onRetrive() in, cmd = " + cmd);
                switch (cmd) {
                case CMD_RESYNCALL:
                        BLContacts.getInstance(mContext).syncContacts(true, false);
                        break;
                }
        }

        public void sendAllContactsJson (JSONObject allContactsJsonObj) {
                if (DEBUG) Log.w(TAG, "sendAllContactsJson() in");
                SyncData data = null;
                String str = allContactsJsonObj.toString();
                try {
                        // Send contacts package length.
                        data = new SyncData();
                        data.putInt(KEY_TYPE, TYPE_SYNC);
                        data.putInt(KEY_SYNCCMD, CMD_SYNCALLLEN);
                        data.putLong(KEY_CONTACTSLEN, str.length());
                        send(data);

                        // Send contacts package.
                        data = new SyncData();
                        data.putInt(KEY_TYPE, TYPE_SYNC);
                        data.putInt(KEY_SYNCCMD, CMD_SYNCALL);
                        data.putString(KEY_SYNCALL, str);
                        send(data);
                } catch (SyncException e) {
                        e.printStackTrace();
                }
        }

        public void sendSyncAdd (String name, String phone) {
                if (DEBUG) Log.w(TAG, "sendSyncAdd(" + name + ", " + phone + ") in");
                SyncData data = new SyncData();
                data.putInt(KEY_TYPE, TYPE_SYNC);
                data.putInt(KEY_SYNCCMD, CMD_ADD);
                data.putString(KEY_SYNCNAME, name);
                data.putString(KEY_SYNCPHONE, phone);
                try {
                        send(data);
                } catch (SyncException e) {
                        e.printStackTrace();
                }
        }

        public void sendSyncDelName (String name) {
                if (DEBUG) Log.w(TAG, "sendSyncDelName(" + name + ") in");
                SyncData data = new SyncData();
                data.putInt(KEY_TYPE, TYPE_SYNC);
                data.putInt(KEY_SYNCCMD, CMD_DELNAME);
                data.putString(KEY_SYNCNAME, name);
                try {
                        send(data);
                } catch (SyncException e) {
                        e.printStackTrace();
                }
        }

        public void sendSyncDelData (String name, String phone) {
                if (DEBUG) Log.w(TAG, "sendSyncDelData(" + name + ", " + phone + ") in");
                SyncData data = new SyncData();
                data.putInt(KEY_TYPE, TYPE_SYNC);
                data.putInt(KEY_SYNCCMD, CMD_DELPHONE);
                data.putString(KEY_SYNCNAME, name);
                data.putString(KEY_SYNCPHONE, phone);
                try {
                        send(data);
                } catch (SyncException e) {
                        e.printStackTrace();
                }
        }

        public void sendClearAll () {
                if (DEBUG) Log.w(TAG, "sendClearAll() in");
                SyncData data = new SyncData();
                data.putInt(KEY_TYPE, TYPE_SYNC);
                data.putInt(KEY_SYNCCMD, CMD_CLEARALL);
                try {
                        send(data);
                } catch (SyncException e) {
                        e.printStackTrace();
                }
        }

}
