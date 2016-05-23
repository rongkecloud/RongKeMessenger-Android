package com.rongkecloud.chat.demo.tools;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

/**
 * 屏幕相关的工具类
 */
public class RKCloudChatScreenTools {
	private static final String TAG = RKCloudChatScreenTools.class.getSimpleName();
	
	private static RKCloudChatScreenTools mInstance = null;
	private WakeLock mWakeLock = null;
	private PowerManager mPowerManager;
	private Context mContext;
	private int mScreenWidth;
	private int mScreenHeight;
	
	private RKCloudChatScreenTools(Context context){
		mContext = context;
		mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		WindowManager windowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics metric = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(metric);
		mScreenWidth = metric.widthPixels;
		mScreenHeight = metric.heightPixels;	
	}
	
	public static RKCloudChatScreenTools getInstance(Context context) {
		if(null == mInstance){
			mInstance = new RKCloudChatScreenTools(context);
		}
		return mInstance;
	}
	
	/**
	 * 点亮屏幕 
	 */
	public void screenOn() {
		mWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
				| PowerManager.ON_AFTER_RELEASE, TAG);
		mWakeLock.acquire();
	}
	
	/**
	 * 关闭屏幕
	 */
	public void screenOff() {
		try {
			if (mWakeLock != null && mWakeLock.isHeld()) {
				mWakeLock.release();
				mWakeLock = null;
			}

		} catch (RuntimeException e) {
			Log.i(TAG, "wake lock count < 1");
		}

		mWakeLock = null;
	}
	
	/**
	 * 获取屏幕的宽度
	 * @return
	 */
	public int getScreenWidth() {	
		return mScreenWidth;
	}

	/**
	 * 获取屏幕的高度
	 * @return
	 */
	public int getScreenHeight() {
		return mScreenHeight;
	}

}
