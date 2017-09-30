package com.sctek.smartglasses.control;

import android.content.Context;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncException;
import cn.ingenic.glasssync.services.SyncModule;

public class RestartApacheModule extends SyncModule {

        private static RestartApacheModule instance = null;

        private static final String TAG = "RestartApacheModule";
        private static final String MODULE_NAME = "ctrl_module";

        private final String KEY_TYPE = "type";

        private final int T_REST_APACHE = 0; // restart apache

        private RestartApacheModule(String name, Context context) {
                super(name, context);
        }

        public static RestartApacheModule getInstance (Context context) {
                if (null == instance) {
                        instance = new RestartApacheModule(MODULE_NAME, context);
                }
                return instance;
        }

        @Override
        protected void onRetrive(SyncData data) {
                super.onRetrive(data);
                Log.d(TAG, "onRetrive() in");
        }

        @Override
        protected void onCreate() {
        }

        public void sendRestartApache () {
                SyncData data = new SyncData();
                data.putInt(KEY_TYPE, T_REST_APACHE);
                try {
                        super.send(data);
                } catch (SyncException e) {
                        e.printStackTrace();
                }
        }
}
