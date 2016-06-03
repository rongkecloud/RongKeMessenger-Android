package com.rongke.cloud.av.demo.tools;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

/**
 * 手机屏幕工具类，功能包括： 点亮屏幕、关闭屏幕
 */
public class RKCloudAVScreenUtils {
	private static final String TAG = RKCloudAVScreenUtils.class.getSimpleName();
	
	private static RKCloudAVScreenUtils mInstance = null;	
	private Context mContext;
	private WakeLock mWakeLock = null;
	
	public static RKCloudAVScreenUtils getInstance(Context context) {
		if (null == mInstance) {
			mInstance = new RKCloudAVScreenUtils(context);
		}
		return mInstance;
	}

	private RKCloudAVScreenUtils(Context context) {
		mContext = context;
	}

	/**
	 * 点亮屏幕
	 */
	public void screenOn() {
		PowerManager pwrMgr = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		mWakeLock = pwrMgr.newWakeLock(PowerManager.ON_AFTER_RELEASE | PowerManager.ACQUIRE_CAUSES_WAKEUP
				| PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
		mWakeLock.setReferenceCounted(false);
		mWakeLock.acquire();
	}

	/**
	 * 关闭屏幕
	 */
	public void screenOff() {
		try {
			if (mWakeLock != null) {
				mWakeLock.release();
				mWakeLock.setReferenceCounted(true);
			}

		}catch(RuntimeException e) {
		}

		mWakeLock = null;
	}
}
