package com.rongkecloud.chat.demo.tools;

import android.util.Log;

public class RKCloudChatPrint {
	private static boolean ENABLE_V = false;
	private static boolean ENABLE_D = false;
	private static boolean ENABLE_I = false;
	private static boolean ENABLE_W = true;
	private static boolean ENABLE_E = true;
	
	/**
	 * 设置debug模式
	 * @param opeDebug
	 */
	public static void setDebugModel(boolean opeDebug){
		if(opeDebug){
			ENABLE_V = true;
			ENABLE_D = true;
			ENABLE_I = true;
		}else{
			ENABLE_V = false;
			ENABLE_D = false;
			ENABLE_I = false;
		}
	}

	public static void e(String tag, Object log) {
		if (ENABLE_E) {
			Log.e(tag, String.valueOf(log));
		}
	}

	public static void d(String tag, Object log) {
		if (ENABLE_D) {
			Log.d(tag, String.valueOf(log));
		}
	}

	public static void i(String tag, Object log) {
		if (ENABLE_I) {
			Log.i(tag, String.valueOf(log));
		}
	}

	public static void v(String tag, Object log) {
		if (ENABLE_V) {
			Log.v(tag, String.valueOf(log));
		}
	}

	public static void w(String tag, Object log) {
		if (ENABLE_W) {
			Log.w(tag, String.valueOf(log));
		}

	}

	public static void e(String tag, Object log, Throwable tr) {
		if (ENABLE_E) {
			Log.e(tag, String.valueOf(log), tr);
		}
	}

	public static void d(String tag, Object log, Throwable tr) {
		if (ENABLE_D) {
			Log.d(tag, String.valueOf(log), tr);
		}
	}

	public static void i(String tag, Object log, Throwable tr) {
		if (ENABLE_I) {
			Log.i(tag, String.valueOf(log), tr);
		}
	}

	public static void v(String tag, Object log, Throwable tr) {
		if (ENABLE_V) {
			Log.v(tag, String.valueOf(log), tr);
		}
	}

	public static void w(String tag, Object log, Throwable tr) {
		if (ENABLE_W) {
			Log.w(tag, String.valueOf(log), tr);
		}
	}

}
