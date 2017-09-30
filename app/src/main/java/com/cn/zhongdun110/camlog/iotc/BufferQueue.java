package com.cn.zhongdun110.camlog.iotc;

import java.util.ArrayList;

public class BufferQueue {
	public class MediaBuffer {
		public byte[] mData;
		public int mFrameSize;
		public int mTimeStamp;

		public MediaBuffer() {
			mData = null;
			mFrameSize = 0;
			mTimeStamp = 0;
		}
	}

	private final Object mLock = new Object();
	public int mBufCount = 0;
	public int mBufSize = 0;
	public ArrayList<MediaBuffer> mFreeBufList = new ArrayList<MediaBuffer>();
	public ArrayList<MediaBuffer> mBusyBufList = new ArrayList<MediaBuffer>();

	public BufferQueue(final int bufCount, final int bufSize) {
		mBufCount = bufCount;
		mBufSize = bufSize;

		for (int i = 0; i < mBufCount; i++) {
			MediaBuffer media = new MediaBuffer();
			media.mData = new byte[mBufSize];
			mFreeBufList.add(i, media);
		}
	}

	public MediaBuffer getFreeBuffer(long millis) {
		synchronized (mLock) {
			if (mFreeBufList.isEmpty()) {
				try {
					mLock.wait(millis);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				MediaBuffer media = mFreeBufList.get(0);
				mFreeBufList.remove(0);
				return media;
			}
			return null;
		}
	}

	public void putFreeBuffer(MediaBuffer media) {
		synchronized (mLock) {
			mBusyBufList.add(media);
			mLock.notify();
		}
	}

	public void undoFreeBuffer(MediaBuffer media) {
		synchronized (mLock) {
			mFreeBufList.add(0, media);
		}
	}

	public MediaBuffer getBusyBuffer(long millis) {
		synchronized (mLock) {
			if (mBusyBufList.isEmpty()) {
				try {
					mLock.wait(millis);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				MediaBuffer media = mBusyBufList.get(0);
				mBusyBufList.remove(0);
				return media;
			}
			return null;
		}
	}

	public void putBusyBuffer(MediaBuffer media) {
		synchronized (mLock) {
			media.mFrameSize = 0;
			media.mTimeStamp = 0;
			mFreeBufList.add(media);
			mLock.notify();
		}
	}

	public void undoBusyBuffer(MediaBuffer media) {
		synchronized (mLock) {
			mBusyBufList.add(0, media);
		}
	}

	public int getBusyBufferCount() {
		int count = 0;
		synchronized (mLock) {
			count = mBusyBufList.size();
		}
		return count;
	}
}