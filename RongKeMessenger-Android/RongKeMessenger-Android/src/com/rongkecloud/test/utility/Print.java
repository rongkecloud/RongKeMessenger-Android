package com.rongkecloud.test.utility;

import android.util.Log;

import com.rongkecloud.test.system.RKCloudDemo;

public class Print {
	private static final boolean ENABLE_V = RKCloudDemo.debugModel;
	private static final boolean ENABLE_D = RKCloudDemo.debugModel;
	private static final boolean ENABLE_I = RKCloudDemo.debugModel;
	private static final boolean ENABLE_W = RKCloudDemo.debugModel;
	private static final boolean ENABLE_E = true;

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
