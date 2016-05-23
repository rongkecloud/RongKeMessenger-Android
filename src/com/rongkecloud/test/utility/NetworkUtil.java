package com.rongkecloud.test.utility;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.rongkecloud.test.system.RKCloudDemo;


public class NetworkUtil {
	
	private static String TAG = NetworkUtil.class.getSimpleName();
	/**
	 * 判断网络是否可用
	 * @param context
	 * @return
	 */
	public static synchronized boolean isNetworkAvaliable() {
		ConnectivityManager cm = (ConnectivityManager) RKCloudDemo.context.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetInfo = cm.getActiveNetworkInfo();
		if (null == activeNetInfo || !activeNetInfo.isAvailable() || !activeNetInfo.isConnected()) {
			Log.i(TAG, "NetWork is unavailable");
			return false;
		}
		return true;
	}
	
	/**
	 * wifi是否打开
	 * @return
	 */
	public static boolean isWiFiEnabled(){
		ConnectivityManager cm = (ConnectivityManager) RKCloudDemo.context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		return info!=null && info.getType()==ConnectivityManager.TYPE_WIFI;
	}
	
	/**
	 * 3G是否打开
	 * @return
	 */
	public static boolean is3GEnabled(){
		ConnectivityManager cm = (ConnectivityManager) RKCloudDemo.context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		return info!=null && info.getType()==ConnectivityManager.TYPE_MOBILE;
	}
}