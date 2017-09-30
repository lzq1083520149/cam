package com.ingenic.glass.api.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncException;
import cn.ingenic.glasssync.services.SyncModule;

/*
 * The data transfer bridge between two bluetooth connected(through GlassSync/GlassSyncMobile) devices.
 */
public class SyncChannel {

	private String TAG = "SyncChannel";

	public static SyncChannel create(String uuid, Context context,
			onChannelListener listener) {
		return new SyncChannel(uuid, context, listener);
	}

	public enum RESULT {
		SUCCESS, UNKNOWN_ERROR, SYNC_SERVICE_NOT_FOUND_ERROR, BT_DISCONNECTED_ERROR, NO_PAIRED_MODULE_ERROR, BAD_PACKET,
	}

	public enum CONNECTION_STATE {
		UNKNOWN, BLUETOOTH_CONNECTED, BLUETOOTH_DISCONNECTED, PAIRCHANNEL_CONNECTED, PAIRCHANNEL_DISCONNECTED,
	}

	private enum PACKET_STATE {
		UNKNOWN, OPERATE_IN_WRONG_STATE, SEND_INPROGRESS, SEND_SUCCESS, SEND_FAILED, RECEIVE_COMPLETED,
	}

	/*
	 * Payload carrying the data for transfer.
	 */
	public class Packet {

		private PACKET_STATE mState = PACKET_STATE.UNKNOWN;

		private SyncData mSyncData = null;

		private Packet() {
			mSyncData = new SyncData();
		}

		private Packet(SyncData data) {
			mSyncData = data;
		}

		public void putBoolean(String key, boolean b) {
			mSyncData.putBoolean(key, b);
		}

		public void putBooleanArray(String key, boolean[] array) {
			mSyncData.putBooleanArray(key, array);
		}

		public void putByte(String key, byte value) {
			mSyncData.putByte(key, value);
		}

		public void putByteArray(String key, byte[] value) {
			mSyncData.putByteArray(key, value);
		}

		public void putDouble(String key, double value) {
			mSyncData.putDouble(key, value);
		}

		public void putDoubleArray(String key, double[] value) {
			mSyncData.putDoubleArray(key, value);
		}

		public void putFloat(String key, float value) {
			mSyncData.putFloat(key, value);
		}

		public void putFloatArray(String key, float[] value) {
			mSyncData.putFloatArray(key, value);
		}

		public void putInt(String key, int value) {
			mSyncData.putInt(key, value);
		}

		public void putIntArray(String key, int[] value) {
			mSyncData.putIntArray(key, value);
		}

		public void putLong(String key, long value) {
			mSyncData.putLong(key, value);
		}

		public void putLongArray(String key, long[] value) {
			mSyncData.putLongArray(key, value);
		}

		public void putShort(String key, short value) {
			mSyncData.putShort(key, value);
		}

		public void putShortArray(String key, short[] value) {
			mSyncData.putShortArray(key, value);
		}

		public void putString(String key, String value) {
			mSyncData.putString(key, value);
		}

		public void putStringArray(String key, String[] value) {
			mSyncData.putStringArray(key, value);
		}

		public boolean getBoolean(String key) {
			return mSyncData.getBoolean(key, false);
		}

		public boolean[] getBooleanArray(String key) {
			return mSyncData.getBooleanArray(key);
		}

		public byte getByte(String key) {
			return mSyncData.getByte(key);
		}

		public byte[] getByteArray(String key) {
			return mSyncData.getByteArray(key);
		}

		public double getDouble(String key) {
			return mSyncData.getDouble(key);
		}

		public double[] getDoubleArray(String key) {
			return mSyncData.getDoubleArray(key);
		}

		public float getFloat(String key) {
			return mSyncData.getFloat(key);
		}

		public float[] getFloatArray(String key) {
			return mSyncData.getFloatArray(key);
		}

		public int getInt(String key) {
			return mSyncData.getInt(key);
		}

		public int[] getIntArray(String key) {
			return mSyncData.getIntArray(key);
		}

		public long getLong(String key) {
			return mSyncData.getLong(key);
		}

		public long[] getLongArray(String key) {
			return mSyncData.getLongArray(key);
		}

		public short getShort(String key) {
			return mSyncData.getShort(key);
		}

		public short[] getShortArray(String key) {
			return mSyncData.getShortArray(key);
		}

		public String getString(String key) {
			return mSyncData.getString(key);
		}

		public String[] getStringArray(String key) {
			return mSyncData.getStringArray(key);
		}
	}

	public Packet createPacket() {
		return new Packet();
	}

	public static final int MSG_SEND_PACKET = 0;
	public static final int MSG_ON_INIT = 1;
	public static final int MSG_ON_CONNECTION_STATE_CHANGED = 2;
	public static final int MSG_ON_RETRIEVED = 3;
	public static final int MSG_ON_SEND_CONFIG_CALLBACK = 4;
	public static final int MSG_CHECK_SEND_RESULT = 5;
        public static final int MSG_SERVICE_CONNECTED = 6;

	private onChannelListener mListener = null;

	/*
	 * listener of buffer sent and received.
	 */
	public interface onChannelListener {
		public void onSendCompleted(RESULT result, Packet packet);

		public void onReceive(RESULT result, Packet packet);

		public void onStateChanged(CONNECTION_STATE state);
	        
	        public void onServiceConnected();
	}

	private Handler mHandler = null;
	private MyHandlerThread mHandlerThread = null;

	private class MyHandlerThread extends HandlerThread implements Callback {
		public MyHandlerThread(String name) {
			super(name);
		}

		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_SEND_PACKET: {
				Packet packet = (Packet) msg.obj;

				if (packet.mState != PACKET_STATE.UNKNOWN) {
					packet.mState = PACKET_STATE.OPERATE_IN_WRONG_STATE;
					mListener.onSendCompleted(RESULT.BAD_PACKET, packet);
				} else {
					packet.mState = PACKET_STATE.SEND_INPROGRESS;
					SyncData data = packet.mSyncData;
					SyncData.Config config = new SyncData.Config();
					config.mmCallback = mHandler.obtainMessage();
					config.mmCallback.what = MSG_ON_SEND_CONFIG_CALLBACK;
					config.mmCallback.arg1 = -88;
					config.mmCallback.obj = packet;
					data.setConfig(config);

					try {
						Log.e(TAG, "MSG_SEND_PACKET 0");

						boolean res = mSyncImplement.send(data);
						if (mListener != null) {
							if (!res) {
								Log.e(TAG, "MSG_SEND_PACKET 1");
								packet.mState = PACKET_STATE.SEND_FAILED;
								mListener.onSendCompleted(
										RESULT.BT_DISCONNECTED_ERROR, packet);
							} else {
								Log.e(TAG, "MSG_SEND_PACKET 2");
								Message msg2 = mHandler.obtainMessage();
								msg2.what = MSG_CHECK_SEND_RESULT;
								msg2.obj = packet;
								mHandler.sendMessageDelayed(msg2, 5000);
							}
						}
					} catch (SyncException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Log.e(TAG, "MSG_SEND_PACKET 3");
						packet.mState = PACKET_STATE.SEND_FAILED;
						mListener.onSendCompleted(
								RESULT.SYNC_SERVICE_NOT_FOUND_ERROR, packet);
					}
				}
				break;
			}
			case MSG_ON_SEND_CONFIG_CALLBACK: {
				Log.e(TAG, "MSG_ON_SEND_CONFIG_CALLBACK 0:" + msg.arg1);
				Packet packet = (Packet) msg.obj;
				if (msg.arg1 == 0) {
					if (mListener != null) {
						Log.e(TAG, "MSG_ON_SEND_CONFIG_CALLBACK 1");
						packet.mState = PACKET_STATE.SEND_SUCCESS;
						mListener.onSendCompleted(RESULT.SUCCESS, packet);
					}
				} else {
					Log.e(TAG, "MSG_ON_SEND_CONFIG_CALLBACK 2");
					packet.mState = PACKET_STATE.SEND_FAILED;
					mListener.onSendCompleted(RESULT.BT_DISCONNECTED_ERROR,
							packet);
				}
				break;
			}
			case MSG_CHECK_SEND_RESULT: {
				Log.e(TAG, "MSG_CHECK_SEND_RESULT 0");
				Packet packet = (Packet) msg.obj;
				if (packet.mState == PACKET_STATE.SEND_INPROGRESS) {// timeout
																	// waiting
																	// SyncData.config
																	// callback.
					Log.e(TAG, "MSG_CHECK_SEND_RESULT 1");
					mListener.onSendCompleted(RESULT.BT_DISCONNECTED_ERROR,
							packet);
				}
				break;
			}
			case MSG_ON_RETRIEVED: {
				Log.e(TAG, "MSG_ON_RETRIEVED 0");
				SyncData data = (SyncData) msg.obj;
				Packet packet = new Packet(data);

				if (mListener != null) {
					Log.e(TAG, "MSG_ON_RETRIEVED 1");
					mListener.onReceive(RESULT.SUCCESS, packet);
				}

				break;
			}
			case MSG_ON_CONNECTION_STATE_CHANGED: {
				Log.e(TAG, "MSG_ON_CONNECTION_STATE_CHANGED 0" + msg.arg1);
				if (msg.arg1 == 1) {
					mListener
							.onStateChanged(CONNECTION_STATE.BLUETOOTH_CONNECTED);
				} else {
					mListener
							.onStateChanged(CONNECTION_STATE.BLUETOOTH_DISCONNECTED);
				}

				break;
			}
			case MSG_SERVICE_CONNECTED :
			    if (mListener != null)
				mListener.onServiceConnected();
			    break;
			}
			return true;
		}
	}

	/*
	 * send meta to the connected other Synccore.
	 */
	public void sendPacket(Packet packet) {
		Log.e(TAG, "sendPacket");
		Message msg = mHandler.obtainMessage();
		msg.what = MSG_SEND_PACKET;
		msg.obj = packet;
		mHandler.sendMessage(msg);
	}

	private SyncImplement mSyncImplement = null;

	private class SyncImplement extends SyncModule implements SyncModule.ISyncServiceListener{
		public SyncImplement(String name, Context context) {
			super(name, context);
		}

		public SyncImplement(String name, Context context, boolean autoBind) {
			super(name, context, autoBind);
		}

		@Override
		protected void onInit() {
			Message msg = mHandler.obtainMessage();
			msg.what = SyncChannel.MSG_ON_INIT;
			mHandler.sendMessage(msg);
		}

		@Override
		protected void onCreate() {
		    setISyncServiceListener(this);
		}

		@Override
		protected void onClear(String address) {
		}

		@Override
		protected void onConnectionStateChanged(boolean connect) {
			Log.e(TAG, "onConnectionStateChanged connect:" + connect);
			Message msg = mHandler.obtainMessage();
			msg.what = SyncChannel.MSG_ON_CONNECTION_STATE_CHANGED;
			msg.arg1 = connect ? 1 : 0;
			mHandler.sendMessage(msg);
		}

		@Override
		protected void onModeChanged(int mode) {
		}

		@Override
		protected void onRetrive(SyncData data) {
			Log.e(TAG, "onRetrive");
			Message msg = mHandler.obtainMessage();
			msg.what = SyncChannel.MSG_ON_RETRIEVED;
			msg.obj = data;
			mHandler.sendMessage(msg);
		}

		@Override
		protected void onFileSendComplete(String fileName, boolean success) {
		}

		@Override
		protected void onFileRetriveComplete(String fileName, boolean success) {
		}

	        @Override
                public void ISyncServiceReady(){
		    Log.e(TAG, "ISyncServiceReady");
		    mHandler.sendEmptyMessage(SyncChannel.MSG_SERVICE_CONNECTED);
		}
	        
	         
	}

	private SyncChannel(String uuid, Context context, onChannelListener listener) {
		mListener = listener;

		mHandlerThread = new MyHandlerThread("SyncCore HandlerThread");
		mHandlerThread.start();
		mHandler = new Handler(mHandlerThread.getLooper(), mHandlerThread);

		mSyncImplement = new SyncImplement(uuid, context);
	}

	public boolean isConnected() {
		try {
			return mSyncImplement.isConnected();
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}
}
