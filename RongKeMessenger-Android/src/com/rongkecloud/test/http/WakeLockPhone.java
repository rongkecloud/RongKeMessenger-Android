package com.rongkecloud.test.http;

import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

/**
 * 唤醒手机辅助类
 * 
 * @author zhao
 * 
 */
class WakeLockPhone {
	private PowerManager.WakeLock sWakeLock;
	private String TAG = WakeLockPhone.class.getSimpleName();

	private static final AtomicInteger mCount = new AtomicInteger(1);

	private int mLeve = PowerManager.PARTIAL_WAKE_LOCK;
	private Context mContext;

	/**
	 * 默认构造方法，默认只是强制唤醒CPU，不带其他的作用
	 */
	public WakeLockPhone(Context mContext) {
		this.mContext = mContext;
	}

	/***
	 * 根据制定的LEVE，来唤醒手机
	 * 
	 * @param wakeLeve
	 *            强制唤醒的级别
	 * 
	 * @see android.os.PowerManager#PARTIAL_WAKE_LOCK
	 * @see android.os.PowerManager#FULL_WAKE_LOCK
	 * @see android.os.PowerManager#SCREEN_BRIGHT_WAKE_LOCK
	 * @see android.os.PowerManager#SCREEN_DIM_WAKE_LOCK
	 */
	public WakeLockPhone(int wakeLeve) {
		mLeve = wakeLeve;
	}

	/**
	 * 唤醒
	 */
	public void wake() {
		synchronized (this) {
			if (null == sWakeLock) {
				PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
				sWakeLock = pm.newWakeLock(mLeve, TAG + mCount.getAndIncrement());
			}
			if (!sWakeLock.isHeld()) {
				sWakeLock.acquire();
			} else {
				Log.w(TAG, "WakeLock was locked, so wake do nothing.");
			}
		}
	}

	/**
	 * 释放
	 */
	public void release() {
		synchronized (this) {
			if (null != sWakeLock) {
				if (sWakeLock.isHeld()) {
					// 判断锁子 是不是被锁住了
					sWakeLock.release();
				}
			}
		}
	}

}