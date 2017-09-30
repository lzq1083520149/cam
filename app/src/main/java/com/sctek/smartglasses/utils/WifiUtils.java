package com.sctek.smartglasses.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.cn.zhongdun110.camlog.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WifiUtils {

    private static final String TAG = "WifiUtils";

    protected static final int WIFI_AP_STATE_UNKNOWN = -1;
    protected static final int WIFI_AP_STATE_DISABLING = 10;
    protected static final int WIFI_AP_STATE_DISABLED = 11;
    protected static final int WIFI_AP_STATE_ENABLING = 12;
    public static final int WIFI_AP_STATE_ENABLED = 13;
    protected static final int WIFI_AP_STATE_FAILED = 14;

    public static final int SECURITY_NONE = 0;
    public static final int SECURITY_WEP = 1;
    public static final int SECURITY_PSK = 2;
    public static final int SECURITY_EAP = 3;
    public static final String WIFI_AP_PASSWORD = "12345678";

    private enum PskType {
        UNKNOWN,
        WPA,
        WPA2,
        WPA_WPA2
    }

    private static PskType pskType = PskType.UNKNOWN;

    public static void turnWifiApOn(Context mContext, WifiManager mWifiManager, WifiCipherType Type) {
        Log.w(TAG, "turnWifiApOn type =" + Type);
        String ssid = getDefaultApSsid(mContext);

        String pw = PreferenceManager.
                getDefaultSharedPreferences(mContext).getString("pw", WIFI_AP_PASSWORD);

        WifiConfiguration wcfg = new WifiConfiguration();
        wcfg.SSID = new String(ssid);
        wcfg.networkId = 1;
        wcfg.allowedAuthAlgorithms.clear();
        wcfg.allowedGroupCiphers.clear();
        wcfg.allowedKeyManagement.clear();
        wcfg.allowedPairwiseCiphers.clear();
        wcfg.allowedProtocols.clear();

        if (Type == WifiCipherType.WIFICIPHER_NOPASS) {
            wcfg.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN, true);
            wcfg.wepKeys[0] = "";
            wcfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wcfg.wepTxKeyIndex = 0;
        }

        if (Type == WifiCipherType.WIFICIPHER_WPA2) {
            wcfg.preSharedKey = pw;
            wcfg.hiddenSSID = true;
            wcfg.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wcfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wcfg.allowedKeyManagement.set(4);//4 means WPA2_PSK
            wcfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wcfg.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wcfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wcfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        }

        try {
            Method method = mWifiManager.getClass().getMethod("setWifiApConfiguration", wcfg.getClass());

            Boolean rt = (Boolean) method.invoke(mWifiManager, wcfg);
            Log.i("setconfig", " " + rt);
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            Log.i("setconfig", " no method");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            Log.i("setconfig", " illegeal argument");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            Log.i("setconfig", " illegal access");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            Log.i("setconfig", " invocation failed");
            e.printStackTrace();
        }
        toggleWifi(mContext, mWifiManager);
    }

    public static void toggleWifi(Context mContext, WifiManager mWifiManager) {
        boolean wifiApIsOn = getWifiAPState(mWifiManager) == WIFI_AP_STATE_ENABLED || getWifiAPState(mWifiManager) == WIFI_AP_STATE_ENABLING;
//        new SetWifiAPTask(mContext, mWifiManager, !wifiApIsOn, false).execute();
        setWifiApEnabled(!wifiApIsOn, mWifiManager);

    }

    public static int getWifiAPState(WifiManager mWifiManager) {
        int state = WIFI_AP_STATE_UNKNOWN;
        try {
            Method method2 = mWifiManager.getClass().getMethod("getWifiApState");
            state = (Integer) method2.invoke(mWifiManager);
        } catch (Exception e) {
        }
        Log.i("WifiAP", "getWifiAPState.state " + state);
        return state;
    }

    public static int setWifiApEnabled(boolean enabled, WifiManager mWifiManager) {

        Log.i("WifiAP", "*** setWifiApEnabled CALLED **** " + enabled);
        if (enabled && mWifiManager.getConnectionInfo() != null) {
            mWifiManager.setWifiEnabled(false);

            try {
                Thread.sleep(1500);
            } catch (Exception e) {
            }
        }

        int state = WIFI_AP_STATE_UNKNOWN;
        try {
            mWifiManager.setWifiEnabled(false);
            Method method1 = mWifiManager.getClass().getMethod("setWifiApEnabled",
                    WifiConfiguration.class, boolean.class);
            method1.invoke(mWifiManager, null, enabled); // true
            Method method2 = mWifiManager.getClass().getMethod("getWifiApState");
            state = (Integer) method2.invoke(mWifiManager);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        if (!enabled) {
            int loopMax = 10;
            while (loopMax > 0 && (getWifiAPState(mWifiManager) == WIFI_AP_STATE_DISABLING
                    || getWifiAPState(mWifiManager) == WIFI_AP_STATE_ENABLED
                    || getWifiAPState(mWifiManager) == WIFI_AP_STATE_FAILED)) {
                try {
                    Thread.sleep(500);
                    loopMax--;
                } catch (Exception e) {
                }
            }
            mWifiManager.setWifiEnabled(true);
        } else if (enabled) {
            int loopMax = 10;
            while (loopMax > 0 && (getWifiAPState(mWifiManager) == WIFI_AP_STATE_ENABLING
                    || getWifiAPState(mWifiManager) == WIFI_AP_STATE_DISABLED
                    || getWifiAPState(mWifiManager) == WIFI_AP_STATE_FAILED)) {
                try {
                    Thread.sleep(500);
                    loopMax--;
                } catch (Exception e) {
                }
            }
        }
        return state;
    }

    public enum WifiCipherType {
        WIFICIPHER_WEP, WIFICIPHER_WPA2, WIFICIPHER_NOPASS, WIFICIPHER_INVALID
    }

    private static String getDefaultApSsid(Context mContext) {
//		String deviceId = ((TelephonyManager)mContext
//				.getSystemService(mContext.TELEPHONY_SERVICE)).getDeviceId();
//
//		String defaultSsid = "CAMLOG" + deviceId.substring(deviceId.length()-5, deviceId.length());
//		String ssid = PreferenceManager.
//				getDefaultSharedPreferences(mContext).getString("ssid", defaultSsid);
//		return ssid;

        String ssid;
        String imei = ((TelephonyManager) mContext.getSystemService(mContext.TELEPHONY_SERVICE)).getDeviceId();
        String android_id = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);

        if (imei == null || imei.length() < 6) {
            ssid = "CAMLOG" + android_id.substring(0, 5);
        } else {
            ssid = "CAMLOG" + imei.substring(0, 5);
        }

        return ssid;
    }

    public static String getValidSsid(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration configuration = (WifiConfiguration) method.invoke(wifiManager);
            return configuration.SSID;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    public static String getValidPassword(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration configuration = (WifiConfiguration) method.invoke(wifiManager);
            return configuration.preSharedKey;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }

    }

    public static String getValidSecurity(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration configuration = null;
        try {
            Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
            configuration = (WifiConfiguration) method.invoke(wifiManager);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        Log.i(TAG, "getSecurity security=" + configuration.allowedKeyManagement);
        if (configuration.allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
            return "WPA_PSK";
        } else if (configuration.allowedKeyManagement.get(4)) { //4 means WPA2_PSK
            return "WPA2_PSK";
        } else if (configuration.allowedKeyManagement.get(KeyMgmt.WPA_EAP)) {
            return "WPA_EAP";
        } else if (configuration.allowedKeyManagement.get(KeyMgmt.IEEE8021X)) {
            return "IEEE8021X";
        }
        return "NONE";

    }

    private static int getSecurity(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return SECURITY_WEP;
        } else if (result.capabilities.contains("PSK")) {
            return SECURITY_PSK;
        } else if (result.capabilities.contains("EAP")) {
            return SECURITY_EAP;
        }
        return SECURITY_NONE;
    }

    public static ArrayList<Map<String, Object>> scanWifiResult(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
       /*make sure close wifi-ap*/
        try {
            Method method1 = wifiManager.getClass().getMethod("setWifiApEnabled",
                    WifiConfiguration.class, boolean.class);
            method1.invoke(wifiManager, null, false);
            while (getWifiAPState(wifiManager) != WIFI_AP_STATE_DISABLED) {
                Thread.sleep(200);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        wifiManager.startScan();
        ArrayList<Map<String, Object>> mWifiList = new ArrayList<Map<String, Object>>();
        List<ScanResult> results = wifiManager.getScanResults();
        if (results != null) {
            for (ScanResult result : results) {
                if (result.SSID == null || result.SSID.length() == 0
                        || result.capabilities.contains("[IBSS]")) {
                    continue;
                }
                boolean found = false;
                int security = getSecurity(result);
                for (Map<String, Object> item : mWifiList) {
                    if (item.get("SSID").equals(result.SSID.toString()) && Integer.parseInt(item.get("SECURITY_INT").toString()) == security) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("SSID", result.SSID.toString());
                    map.put("SECURITY_INT", security);
                    map.put("SECURITY_STRING", getSecurityString(security, result, context));
                    mWifiList.add(map);
                }
            }
            return mWifiList;
        }
        return null;
    }

    private static String getSecurityString(int security, ScanResult result, Context context) {
        pskType = getPskType(result);
        String SecurityString = "";
        switch (security) {
            case SECURITY_EAP:
                SecurityString = context.getString(R.string.wifi_security_short_eap);
                break;
            case SECURITY_PSK:
                switch (pskType) {
                    case WPA:
                        SecurityString = context.getString(R.string.wifi_security_short_wpa);
                        break;
                    case WPA2:
                        SecurityString = context.getString(R.string.wifi_security_short_wpa2);
                        break;
                    case WPA_WPA2:
                        SecurityString = context.getString(R.string.wifi_security_short_wpa_wpa2);
                        break;
                    case UNKNOWN:
                    default:
                        SecurityString = context.getString(R.string.wifi_security_short_psk_generic);
                        break;
                }
                break;
            case SECURITY_WEP:
                SecurityString = context.getString(R.string.wifi_security_short_wep);
                break;
            case SECURITY_NONE:
                SecurityString = context.getString(R.string.wifi_free);
                break;
            default:
                break;
        }
        String securityStrFormat = context.getString(R.string.wifi_security_none);
        if (security != SECURITY_NONE) {
            securityStrFormat = context.getString(R.string.wifi_secured_first_item);
        } else {
            return SecurityString;
        }
        StringBuilder summary = new StringBuilder();
        summary.append(String.format(securityStrFormat, SecurityString));
        return summary.toString();
    }

    private static PskType getPskType(ScanResult result) {
        boolean wpa = result.capabilities.contains("WPA-PSK");
        boolean wpa2 = result.capabilities.contains("WPA2-PSK");
        if (wpa2 && wpa) {
            return PskType.WPA_WPA2;
        } else if (wpa2) {
            return PskType.WPA2;
        } else if (wpa) {
            return PskType.WPA;
        } else {
            return PskType.UNKNOWN;
        }
    }

    //Network is available or not.
    public static boolean isNetAvailable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        return (info != null && info.isAvailable());
    }

    public static boolean isMoblieNetworkConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (null != info && info.isConnected()) {

            return (info.getType() == ConnectivityManager.TYPE_MOBILE);
        }
        return false;
    }
}
