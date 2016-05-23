package com.rongkecloud.chat.demo.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore.Images.Media;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.Gravity;
import android.widget.Toast;

import com.rongkecloud.chat.RKCloudChatBaseMessage.MSG_STATUS;
import com.rongkecloud.chat.demo.RKCloudChatConstants;
import com.rongkecloud.chat.demo.entity.RKCloudChatEmojiRes;
import com.rongkecloud.test.R;

/**
 * 会话相关的操作内容
 */
public class RKCloudChatTools {	
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
	private static SimpleDateFormat showMsgTimeDateFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());
	private static SimpleDateFormat showMsgTimeTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
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
	 * 判断sd卡是否有效
	 * @return
	 */
	public static boolean isSDCardValid() {
		return Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
	}
	
	/**
	 * 获取sd卡文件路径
	 * @return
	 */
	public static File getSDCardRootDirectory() {
		if(isSDCardValid()){
			return Environment.getExternalStorageDirectory();
		}else{
			return Environment.getRootDirectory();
		}
	}
	
	/**
	 * 拷贝数据
	 */
	public static boolean copyFile(File srcFile, File destFile){
		boolean result = false;
		FileInputStream input = null;
		FileOutputStream output = null;
		try {
			input = new FileInputStream(srcFile);
			output = new FileOutputStream(destFile);
			byte[] buffer = new byte[4096];
            int n = 0;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }
            result = true;
		} catch (Exception e) {
			e.getStackTrace();
		}finally{
			if(null != input){
				try{
					input.close();
				}catch(Exception e){
					e.getStackTrace();
				}
			}
			if(null != output){
				try{
					output.close();
				}catch(Exception e){
					e.getStackTrace();
				}
			}
		}
		
		return result;
	}
	
	/**
	 * getFileName 根据文件路径获取文件名称
	 * @param filePath String 完整的文件路径及名称
	 * @return null 参数错误、或者最后一个分隔符获取位置错误、或者文件名称为空 String 文件名称
	 */
	public static String getFileName(String filePath) {
		// 参数校验
		if (TextUtils.isEmpty(filePath)) {
			return null;
		}
		// 获取文件路径中最后一个分隔符的位置
		int position = filePath.lastIndexOf(File.separator);
		if (position == -1) {
			return null;
		}
		// 获取文件名称
		String filename = filePath.substring(position + 1);
		// 如果文件内容为空，则返回null
		if (TextUtils.isEmpty(filename)) {
			return null;
		}
		return filename;
	}

	
	/**
	 * 文件排序
	 * @param fileDirectory 文件目录
	 * @param type 排序字段 1：文件最后修改时间  2：文件大小 3：文件名称
	 * @param downSort true:降序  false:升序
	 */
	public static List<File> orderByLastModifyTime(String fileDirectory, final int type, final boolean downSort) {
		File dir = new File(fileDirectory);
		if(!dir.exists() || !dir.isDirectory()){
			return null;
		}
		
		List<File> files = Arrays.asList(dir.listFiles());
		Collections.sort(files, new Comparator<File>() {
			public int compare(File f1, File f2) {
				long diff = 0;
				switch(type){
				case 1:
					diff = f1.lastModified() - f2.lastModified();
					break;
					
				case 2:
					diff = f1.length() - f2.length();
					break;
					
				case 3:
					diff = f1.getName().compareTo(f2.getName());
					break;
				}
				
				if(0 == diff){
					return 0;
				}else{
					if(downSort){
						return diff > 0 ? 1 : -1;
					}else{
						return diff > 0 ? -1 : 1;
					}
				}
			}

			public boolean equals(Object obj) {
				return true;
			}
		});
		
		return files;
	}
	
	/**
	 * @function parseMsgFaceToAlias 主要用于通知栏中消息的显示，并且把表情图标用别名代替
	 * @param context Context对象
	 * @param content 文本内容
	 * @return null 参数为null或为空 CharSequence 替换后的内容
	 */
	public static CharSequence parseMsgFaceToAlias(Context context, CharSequence content) {
		if (null==context && TextUtils.isEmpty(content)) {
			return null;
		}
		String[] emojiRegxs = RKCloudChatEmojiRes.EMOJI_REGX;// 获取表情的文本内容
		String[] emojiAlias = RKCloudChatEmojiRes.EMOJI_ALIAS;// 获取表情对应的别名
		int emojiCount = emojiRegxs.length;

		StringBuffer sb = new StringBuffer();// 用于存放返回的内容
		Matcher m = Pattern.compile(RKCloudChatEmojiRes.EMOJI_REGP_RULE).matcher(content);
		int tempIndex = -1;// 临时索引号
		// 通过循环遍历的方式，把查找出来的符合正则表达式的文本内容替换成对应的别名
		while (m.find()) {
			// 获取表情文本在数组中的索引号
			tempIndex = getLocation(emojiRegxs, m.group());
			if (tempIndex >= 0 && tempIndex < emojiCount) {
				// 用别名替代表情文本
				m.appendReplacement(sb, emojiAlias[tempIndex]);
			}
		}

		m.appendTail(sb);

		return sb.toString();
	}
	
	/*
	 * 获取索引位置
	 */
	private static int getLocation(String[] arr, String findStr) {
		int index = -1;
		// 如果数组内容为空或者查找的字符串为空，则返回-1
		if (null == arr || arr.length == 0 || TextUtils.isEmpty(findStr)) {
			return index;
		}
		int len = arr.length;
		for (int i = 0; i < len; i++) {
			if (arr[i].equals(findStr)) {
				index = i;
				break;
			}
		}
		return index;

	}

	/**
	 * @function parseMsgFace 解析文本内容，把带有表情的文本转换成对应的表情图标
	 * @param context Context对象
	 * @param content 文本内容
	 * @param limit int 用于控制长度 -1：表示不受限制
	 * @param type int 1:会话列表 2：消息列表中的文本内容 3：消息列表页面中的输入框
	 * @return null 部分参数值为null或为空 CharSequence 该类的一个对象，即替换后的内容
	 */
	public static CharSequence parseMsgFace(Context context, CharSequence content, int limit, int type) {
		if (null == context || TextUtils.isEmpty(content)) {
			return null;
		}

		String[] emojiRegxs = RKCloudChatEmojiRes.EMOJI_REGX;// 获取表情的文本内容
		int[] emojiResIds = RKCloudChatEmojiRes.EMOJI_RESIDS;// 获取表情图的资源Id
		int emojiCount = emojiRegxs.length;

		SpannableString sb = new SpannableString(content);// 可扩展的字符串
		Matcher m = Pattern.compile(RKCloudChatEmojiRes.EMOJI_REGP_RULE).matcher(content);

		// 定义临时变量
		int tempIndex = -1;// 临时索引号
		int tempStart = 0;// 记录表情的开始位置
		int tempEnd = 0;// 记录表情的结束位置
		Drawable tempDraw = null;// 对应的表情图片
		ImageSpan tempImageSpan = null;// 用于在TextView中显示图片

		Drawable emojiDrawable = context.getResources().getDrawable(emojiResIds[0]);// 先获取一个emoji图片用来定义大小
		int imageWidth = emojiDrawable.getIntrinsicWidth();
		int imageHeight = emojiDrawable.getIntrinsicHeight();
		switch(type){
		case 1:
			imageWidth = (int)context.getResources().getDimension(R.dimen.rkcloud_chat_emoji_inchatlist_width);
			imageHeight = (int)context.getResources().getDimension(R.dimen.rkcloud_chat_emoji_inchatlist_height);
			break;			
		case 2:
			imageWidth = (int)context.getResources().getDimension(R.dimen.rkcloud_chat_emoji_inmsglist_width);
			imageHeight = (int)context.getResources().getDimension(R.dimen.rkcloud_chat_emoji_inmsglist_height);
			break;
			
		case 3:
			imageWidth = (int)context.getResources().getDimension(R.dimen.rkcloud_chat_emoji_inedittext_width);
			imageHeight = (int)context.getResources().getDimension(R.dimen.rkcloud_chat_emoji_inedittext_height);
			break;
		}
		// 通过循环遍历的方式，把查找出来的符合正则表达式的文本内容替换成对应的表情图片
		while (m.find()) {
			tempStart = m.start();
			tempEnd = m.end();
			// 获取表情文本在数组中的索引号
			tempIndex = getLocation(emojiRegxs, m.group());
			
			if (tempIndex >= 0 && tempIndex < emojiCount) {
				// 生成表情对应的图片，并且为null时继续执行下一轮的替换
				tempDraw = context.getResources().getDrawable(emojiResIds[tempIndex]);
				if (null == tempDraw) {
					continue;
				}
				// 设置图片样式
				tempDraw.setBounds(0, 0, imageWidth, imageHeight);
				// 生成插入TextView的图片对象
				tempImageSpan = new ImageSpan(tempDraw, ImageSpan.ALIGN_BOTTOM);
				// 替换表情文本为图片
				sb.setSpan(tempImageSpan, tempStart, tempEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}

		return sb;
	}

	/**
	 * @function getFileSize 根据文件路径获取文件大小，单位：字节
	 * @param filePath 完整的文件路径及名称
	 * @return 
	 * 		>0 文件大小值 
	 * 		-1 文件不存在或者压根不是文件
	 */
	public static long getFileSize(String filePath) {
		File file = new File(filePath);
		long size = 0;
		// 文件存在并且确实是一个文件，则获取文件的实际大小值；反之，返回-2
		if (file.exists() && file.isFile()) {
			size = file.length();
		} else {
			size = -1;
		}

		return size;
	}
	
	/**
	 * @function 文件大小的格式转换(带有单位)
	 * @param fileSize long 文件大小，单位：字节
	 * @return String 转换后的字符串
	 */
	public static String formatFileSize(long fileSize) {
		long kb = 1024;
		long mb = 1024 * kb;
		long gb = 1024 * mb;
		float floatFileSize = (float)fileSize;

		if (fileSize < kb) {
			float value = floatFileSize / kb;
			if(value < 0.01f){
				return "0.01KB";
			}else{
				return String.format(Locale.getDefault(), "%.2fKB", value);
			}
		} else if (fileSize < mb) {
			return String.format(Locale.getDefault(), "%.2fKB", floatFileSize/kb);
		} else if (fileSize < gb) {
			return String.format(Locale.getDefault(), "%.2fMB", floatFileSize/mb);
		} else {
			return String.format(Locale.getDefault(), "%.3fGB", floatFileSize/gb);
		}
	}
	
	/**
	 * 播放时长的格式转换
	 * @param duration
	 * @return
	 */
	public static String formatDuration(int totalSeconds){
		if(totalSeconds <= 0){
			return "00:00s";
		}
		
		int hour = totalSeconds/3600;
		int minute = (totalSeconds-hour*3600)/60;
		int second = totalSeconds-hour*3600-minute*60;
		StringBuffer sb = new StringBuffer();
		if(hour > 0){
			if(hour < 10){
				sb.append(0);
			}
			sb.append(hour).append(":");
		}
		
		if(minute < 10){
			sb.append(0);
		}
		sb.append(minute).append(":");
		
		if(second < 10){
			sb.append(0);
		}
		sb.append(second);
		return sb.toString();
	}
	
	
	/**
	 * 获取图片选择功能的路径
	 */
	public static String getChoosePicturePath(Context context, Uri uri) {
		if (null==context || null==uri || uri.getPath().length()==0) {
			return null;
		}

		String picPath = null;
		String scheme = uri.getScheme();
		// 抓取图片路径
		if ("file".equals(scheme)) {
			picPath = uri.getSchemeSpecificPart();
		} else if ("content".equals(scheme)) {
			String[] projection = new String[] { Media.DATA };
			Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
			if (cursor.moveToFirst()) {
				picPath = cursor.getString(0);
			}
			cursor.close();
		}
		return picPath;
	}	
	
	/*
	 * 创建目录
	 */
	public static File createDirectory(String directoryPath) {
		File file = new File(directoryPath);
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}
	
	/**
	 * 打开文件
	 * @param context
	 * @param msgFilePath
	 * @return 是否打开成功 true:打开成功
	 */
	public static boolean openFile(Context context, String msgFilePath){	
		if (TextUtils.isEmpty(msgFilePath)) {		
			return false;
		}

		File filePath = new File(msgFilePath);
		// 文件不存在时，给出提示
		if (!filePath.exists()) {
			showToastText(context, context.getString(R.string.rkcloud_chat_unfound_resource));
			return false;
		}

		File file = new File(filePath.getAbsolutePath());// 文件绝对路径
		String fileName = file.getName();// 文件名称
		int index = fileName.lastIndexOf(".");
		// 文件扩展名
		String extension = index > 0 ? fileName.substring(index, fileName.length()).toLowerCase(Locale.getDefault()) : null;
		if (TextUtils.isEmpty(extension)) {
			// 无文件后缀时，给出提示：文件保存在：*******
			showToastText(context, context.getString(R.string.rkcloud_chat_file_savepath, filePath.getAbsolutePath()));
			return false;
		}

		// 获取文件的mime类型
		String type = getAttachmentMime(extension);// 文件的mime类型		

		if (TextUtils.isEmpty(type)) {
			// 文件类型不存在时，给出提示：文件保存在：******
			showToastText(context, context.getString(R.string.rkcloud_chat_file_savepath, filePath.getAbsolutePath()));
			return false;
		}
		try {
			// 打开文件
			Intent intent = new Intent();
			intent.setAction(android.content.Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(file), type);
			context.startActivity(intent);	
			return true;
		} catch (Exception e) {
			Toast.makeText(context, "Your device does not support this operation.", Toast.LENGTH_SHORT).show();			
		}	
		
		return false;
	}

	public static String getAttachmentMime(String extension){
		Map<String, String> mimeCollections = new HashMap<String, String>();
		mimeCollections.put(".3gp", "video/3gpp");
		mimeCollections.put(".apk", "application/vnd.android.package-archive" );
		mimeCollections.put(".asf", "video/x-ms-asf" );
		mimeCollections.put(".avi", "video/x-msvideo" );
		mimeCollections.put(".bin", "application/octet-stream" );
		mimeCollections.put(".bmp", "image/bmp" );
		mimeCollections.put(".c", "text/plain" );
		mimeCollections.put(".class", "application/octet-stream" );
		mimeCollections.put(".conf", "text/plain" );
		mimeCollections.put(".cpp", "text/plain" );
		mimeCollections.put(".doc", "application/msword" );
		mimeCollections.put(".exe", "application/octet-stream" );
		mimeCollections.put(".gif", "image/gif" );
		mimeCollections.put(".gtar", "application/x-gtar" );
		mimeCollections.put(".h", "text/plain" ); 
		mimeCollections.put(".htm", "text/html" ); 
		mimeCollections.put(".html", "text/html" ); 
		mimeCollections.put(".jar", "application/java-archive" );
		mimeCollections.put(".java", "text/plain" ); 
		mimeCollections.put(".jpeg", "image/jpeg" ); 
		mimeCollections.put(".jpg", "image/jpeg" );
		mimeCollections.put(".js", "application/x-javascript" ); 
		mimeCollections.put(".log", "text/plain" ); 
		mimeCollections.put(".m3u", "audio/x-mpegurl" );
		mimeCollections.put(".m4a", "audio/mp4a-latm" ); 
		mimeCollections.put(".m4b", "audio/mp4a-latm" ); 
		mimeCollections.put(".m4p", "audio/mp4a-latm" );
		mimeCollections.put(".amr", "audio/amr" ); 
		mimeCollections.put(".m4u", "video/vnd.mpegurl" ); 
		mimeCollections.put(".m4v", "video/x-m4v" ); 
		mimeCollections.put(".mov", "video/quicktime" );
		mimeCollections.put(".mp2", "audio/x-mpeg" ); 
		mimeCollections.put(".mp3", "audio/x-mpeg" ); 
		mimeCollections.put(".mp4", "video/mp4" );
		mimeCollections.put(".mpc", "application/vnd.mpohun.certificate" ); 
		mimeCollections.put(".mpe", "video/mpeg" ); 
		mimeCollections.put(".mpeg", "video/mpeg" );
		mimeCollections.put(".mpg", "video/mpeg" ); 
		mimeCollections.put(".mpg4", "video/mp4" ); 
		mimeCollections.put(".mpga", "audio/mpeg" );
		mimeCollections.put(".msg", "application/vnd.ms-outlook" ); 
		mimeCollections.put(".ogg", "audio/ogg" ); 
		mimeCollections.put(".pdf", "application/pdf" );
		mimeCollections.put(".png", "image/png" ); 
		mimeCollections.put(".pps", "application/vnd.ms-powerpoint" ); 
		mimeCollections.put(".ppt", "application/vnd.ms-powerpoint" );
		mimeCollections.put(".prop", "text/plain" ); 
		mimeCollections.put(".rar", "application/x-rar-compressed" ); 
		mimeCollections.put(".rc", "text/plain" );
		mimeCollections.put(".rmvb", "audio/x-pn-realaudio" ); 
		mimeCollections.put(".rtf", "application/rtf" );
		mimeCollections.put(".sh", "text/plain" );
		mimeCollections.put(".tgz", "application/x-compressed" ); 
		mimeCollections.put(".txt", "text/plain" ); 
		mimeCollections.put(".wav", "audio/x-wav" );
		mimeCollections.put(".wma", "audio/x-ms-wma" ); 
		mimeCollections.put(".wmv", "audio/x-ms-wmv" ); 
		mimeCollections.put(".wps", "application/vnd.ms-works" );
		mimeCollections.put(".xml", "text/plain" ); 
		mimeCollections.put(".z", "application/x-compress" ); 
		mimeCollections.put(".zip", "application/zip" ); 
//		MIME_COLLECTIONS.put(".gz","application/x-gzip");
//		MIME_COLLECTIONS.put(".tar","application/x-tar");				

		return mimeCollections.get(extension);
	}
	
	/**
	 * @function getMsgShowTime 获取显示的日期，格式有三种：今天 、昨天、月/日
	 * @param timestamp long 毫秒级的时间戳
	 * @return String
	 */
	public static String getShowDate(long timestamp, Context context) {
		if (timestamp <= 0 || null == context) {
			return null;
		}

		SimpleDateFormat defaultDateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());// 默认的日期格式
		SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
		// 获取今天的日期
		Calendar calendar = Calendar.getInstance();
		String today = dayFormat.format(calendar.getTime());

		// 获取昨天的日期
		calendar.add(Calendar.DATE, -1);// 日期减一
		String yesterday = dayFormat.format(calendar.getTime());

		// 获取给定时间戳的日期
		calendar.setTimeInMillis(timestamp);
		String day = dayFormat.format(calendar.getTime());

		if (day.equals(today)) {
			return context.getString(R.string.rkcloud_chat_chatlist_today);
		} else if (day.equals(yesterday)) {
			return context.getString(R.string.rkcloud_chat_chatlist_yesterday);
		} else {
			return defaultDateFormat.format(calendar.getTime());
		}

	}

	/**
	 * @function getShowTime 获取显示的时间，格式为：时:分
	 * @param timestamp long 毫秒级的时间戳
	 * @return String
	 */
	public static String getShowTime(long timestamp) {
		if (timestamp <= 0) {
			return null;
		}

		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.US);
		Date curDate = new Date(timestamp);
		return formatter.format(curDate);
	}
	
	/*
	 * 分隔字符串
	 */
	public static List<String> splitStrings(String accounts){
		List<String> results = null;
		if(!TextUtils.isEmpty(accounts)){		
			String[] arr = accounts.split(",");
			results = new ArrayList<String>(arr.length);
			for(String str : arr){
				str = str.trim();
				if(!TextUtils.isEmpty(str) && !results.contains(str)){
					results.add(str);
				}
			}
		}else{
			results = new ArrayList<String>();
		}
		return results;
	}
	
	/**
	 * 连接字符串
	 * @param accounts
	 * @return
	 */
	public static String joinStrings(List<String> accounts){
		if(null==accounts || 0==accounts.size()){
			return "";
		}
		StringBuffer sb = new StringBuffer();
		for(String account : accounts){
			sb.append(account).append(",");
		}
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}
	
	/**
	 * 获取发送消息的状态
	 * @return
	 */
	public static String getSendMsgStatus(Context context, MSG_STATUS status){
		int resid = 0;
		switch (status) {
			case SEND_SENDED :
				resid = R.string.rkcloud_chat_msgstatus_send_sended;
				break;
				
			case SEND_ARRIVED :
				resid = R.string.rkcloud_chat_msgstatus_send_arrived;
				break;
				
			case READED :
				resid = R.string.rkcloud_chat_msgstatus_send_readed;
				break;
			default:
				break;
		}
		if(resid > 0){
			return context.getString(resid);
		}
		return null;
	}
	
	/**
	 * 是否添加时间提示条
	 * @param oldDate
	 * @param newDate
	 * @return
	 */
	public static boolean isAddTimeTip(long oldDate, long newDate){		
		if(0==oldDate || !dateFormat.format(newDate).equals(dateFormat.format(oldDate)) || Math.abs(newDate-oldDate)>RKCloudChatConstants.MSG_TIMETIP_INTERVAL){
			return true;
		}
		return false;
	}
	
	/**
	 * 获取消息显示的时间
	 * @param msgTime
	 * @return
	 */
	public static String getMsgShowTime(long msgTime){
		String date = showMsgTimeDateFormat.format(new Date(msgTime));
		String time = showMsgTimeTimeFormat.format(new Date(msgTime));
		
		String content = time;
		if(!showMsgTimeDateFormat.format(new Date()).equals(date)){
			content = String.format("%s %s", date, time);
		}
		
		return content;
	}
	
}
