package com.rongkecloud.test.utility;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.Gravity;
import android.widget.Toast;

import com.rongkecloud.test.R;
import com.rongkecloud.test.system.ConfigKey;
import com.rongkecloud.test.system.RKCloudDemo;
import com.rongkecloud.test.ui.WelcomeActivity;

public class OtherUtilities {
	/**
	 * 弹出提示
	 * @param context
	 * @param msg
	 */
	public static void showToastText(Context context, String msg){
		Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 100);
		toast.show();		
	}
	
	/**
	 * 在主界面提示用是否需要添加快捷方式的dialog
	 * @param context
	 */
	public static void createShortcut(Context context) {
		if(RKCloudDemo.config.getBoolean(ConfigKey.KEY_CREATE_SHORTCUT, false)){
			return;
		}
		
		RKCloudDemo.config.put(ConfigKey.KEY_CREATE_SHORTCUT, true);
		Intent shortcutIntent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
		// 不允许重复创建
		shortcutIntent.putExtra("duplicate", false);
		// 快捷方式的名称
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, context.getString(R.string.app_name));
		// 快捷方式的图标
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, R.drawable.logo));
		// 点击快捷图片，运行的程序主入口
		Intent enterIntent = new Intent(Intent.ACTION_MAIN);
		enterIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		enterIntent.setComponent(new ComponentName(context, WelcomeActivity.class));
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, enterIntent);
		context.sendBroadcast(shortcutIntent);
	}
	
	/*
	 * 是否已创建快捷方式
	 */
	private static boolean hasShortcut(Context context) {
		String AUTHORITY = "com.android.launcher.settings";
		Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/favorites?notify=true");
		String[] columns = new String[] { "title", "iconResource" };
		String[] args = new String[] {context.getString(R.string.app_name)};
		
		Cursor c = null;
		boolean isInstallShortcut = false;
		try{
			c = context.getContentResolver().query(CONTENT_URI, columns, "title=?", args,null);
			if (null!=c && c.getCount()>0) {
				isInstallShortcut = true;
			}
		}catch(Exception e){
			
		}finally{
			if(null != c){
				c.close();
			}
		}
		
		return isInstallShortcut;
	}
}
