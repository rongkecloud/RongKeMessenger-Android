package com.rongkecloud.test.utility;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.rongkecloud.test.R;
import com.rongkecloud.test.system.RKCloudDemo;

/**
 * 获得软件的一些基本信息如版本等信息和手机当前的语言等信息
 */
public class SystemInfo {
	private static final String TAG = SystemInfo.class.getSimpleName();
	
	/**
	 * 获取客户端名称
	 * @return
	 */
	public static String getClientName() {
		return RKCloudDemo.context.getString(R.string.app_name);
	}
	
	/**
	 * 获取客户端版本号
	 * @return
	 */
	public static String getClientVersion(){
		return getVersionName();
	}
	
	/**
	 * 获得版本全称
	 */
	public static String getVersionName() {	 
		PackageManager manager = RKCloudDemo.context.getPackageManager();
		String verName = null;
		try {
			PackageInfo info = manager.getPackageInfo(RKCloudDemo.context.getPackageName(), 0);
			verName = info.versionName;
		} catch (NameNotFoundException e) {
			Print.w(TAG, "getVersionName -- exception, info="+e.getMessage());
			e.getStackTrace();
		}

		return verName;
	}

	/**
	 * 获得版本号
	 */
	public static int getVersionCode() {
		PackageManager manager = RKCloudDemo.context.getPackageManager();
		int verCode = 0;

		try {
			PackageInfo info = manager.getPackageInfo(RKCloudDemo.context.getPackageName(), 0);
			verCode = info.versionCode;
		} catch (NameNotFoundException e) {
			Print.w(TAG, "getVersionCode -- exception, info="+e.getMessage());
			e.getStackTrace();
		}
		return verCode;
	}

	/**
	 * 获取手机的操作系统名称
	 */
	public static String getOsName() {
		return "android";
	}
	
	/**
	 * 获得当前手机的系统版本
	 * @return
	 */
	public static String getOsVersion() {
		return Build.VERSION.RELEASE;
	}
	
	/**
	 * 获取手机的制造商
	 * @return
	 */
	public static String getManufacturer() {
		return Build.MANUFACTURER;
	}
	
	/** 
	 * The end-user-visible name for the end product. 
	 */
	public static String getDeviceModel() {
		return Build.MODEL;
	}
	
	/**
	 * 获取手机号码
	 * @return
	 */
	public static String getSimNumber(){
		TelephonyManager telManager = (TelephonyManager)RKCloudDemo.context.getSystemService(Context.TELEPHONY_SERVICE);
		String simNumber = telManager.getLine1Number();
		if(!TextUtils.isEmpty(simNumber)){
			simNumber = RegularCheckTools.filterMobile(simNumber);
		}
		return simNumber;
	}
	
	/**
	 * 获取IMSI号码(国际移动用户识别码)
	 */
	public static String getImsi(){
		TelephonyManager telManager = (TelephonyManager)RKCloudDemo.context.getSystemService(Context.TELEPHONY_SERVICE);
		String imsi = telManager.getSubscriberId();
		if(TextUtils.isEmpty(imsi)){
			imsi = "";
		}
		return imsi;
	}
	
	/**
	 * 获取IMEI号码，是国际移动设备身份码
	 */
	public static String getImei(){
		TelephonyManager telManager = (TelephonyManager)RKCloudDemo.context.getSystemService(Context.TELEPHONY_SERVICE);
		String imei = telManager.getDeviceId();
		if(TextUtils.isEmpty(imei)){
			imei = "";
		}
		return imei;
	}
	
	public static boolean isForceUpgrade(String minVer) {
		String curVer = SystemInfo.getVersionName();

		if (getVersionNumber(curVer) < getVersionNumber(minVer))
			return true;
		return false;
	}
	
	/*
	 * @param ver
	 * @return
	 */
	private static long getVersionNumber(String ver) {
		String[] array = ver.split("\\.");
		long n = 0;

		for (String str : array) {
			try {
				n = 10*n + Integer.parseInt(str);
			} catch (NumberFormatException e) {
				e.getStackTrace();
			}
		}
		return n;
	}
}
