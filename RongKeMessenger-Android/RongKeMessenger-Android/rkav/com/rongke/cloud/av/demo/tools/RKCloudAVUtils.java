package com.rongke.cloud.av.demo.tools;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

public class RKCloudAVUtils {
	/**
	 * @function getShowTime 获取显示的时间，格式为：yyyy-MM-dd hh:mm:ss
	 * @param timestamp long 毫秒级的时间戳
	 * @return String
	 */
	public static String getTimeExactMinute(long timestamp) {
		if (timestamp <= 0) {	
			return null;
		}

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		Date curDate = new Date(timestamp);
		return formatter.format(curDate);
	}
		
	/**
	 * 把秒数转化为时间格式的内容
	 * @param content 
	 * @return String 转换后的时间格式为：ii:ss
	 */
	public static String secondConvertToTime(long content){
		long realValue = content;
		
		long hours = realValue/3600;
		long minutes = (realValue-hours*3600)/60;
		long seconds = realValue-hours*3600-minutes*60;
		String strHour, strMinute, strSecond;
		// 转化为小时
		if(hours<10){
			strHour = "0"+hours;
		}else if(hours >24){
			strHour = "00";
		}else{
			strHour = String.valueOf(hours);
		}
		// 转化为分钟
		if(minutes<10){
			strMinute = "0"+minutes;
		}else if(minutes >59){
			strMinute = "00";
		}else{
			strMinute = String.valueOf(minutes);
		}
		// 转化为秒数
		if(seconds<10){
			strSecond = "0"+seconds;
		}else if(seconds >59){
			strSecond = "00";
		}else{
			strSecond = String.valueOf(seconds);
		}
		if(hours > 0){
			return String.format("%s:%s:%s", strHour, strMinute, strSecond);
		}
		return String.format("%s:%s", strMinute, strSecond);
	}
	
	/**
	 * 转换日期为毫秒级的时间戳
	 * @param dateTime 格式：yyyy-MM-dd HH:mm:ss
	 * @return
	 */
	public static long parseDateTime(String dateTime){
		if(TextUtils.isEmpty(dateTime)){
			return 0;
		}
		
		long time = 0;
		try {
			Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateTime);
			if(null != date){
				time = date.getTime();
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return time;
	}
	
	/**
	 * 当前是否有GSM电话
	 * @param context
	 * @return
	 */
	public static boolean isInSystemCall(Context context) {
		TelephonyManager telManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		if (telManager == null)
			return false;
		return (telManager.getCallState() != TelephonyManager.CALL_STATE_IDLE);
	}
	
	/**
	 * 获取通知栏高度
	 * @param context
	 * @return
	 */
	public static int getStatusBarHeight(Context context){
        int statusBarHeight = 0;
        try {
        	Class<?> c = Class.forName("com.android.internal.R$dimen");
        	Object obj = c.newInstance();
        	Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
        }
        return statusBarHeight;
    }
}
