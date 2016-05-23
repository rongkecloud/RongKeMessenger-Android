package com.rongkecloud.test.utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;

import com.rongkecloud.test.entity.Constants;

public class FileLog {
	private static final String TAG = FileLog.class.getSimpleName();	
	private static final String LOGFILE_PREFIX = "";// 文件前缀
	
	private static final ReentrantLock mLock = new ReentrantLock(true);
	private static boolean mIntialized = false;
	
	private static FileOutputStream mSystemLog = null;	
	
	public synchronized static void v(String tag, String str) {
		str = __ML__() + " : " + str;
		Log.v(tag, str);
		log(tag,str);
	}

	public synchronized static void d(String tag, String str) {		
		str = __ML__() + " : " + str;
		Log.d(tag, str);
		log(tag,str);
	}

	public synchronized static void i(String tag, String str) {
		str = __ML__() + " : " + str;
		Log.i(tag, str);
		log(tag,str);
	}
	
	public synchronized static void e(String tag, String str) {
		str = __ML__() + " : " + str;
		Log.e(tag, str);
		log(tag,str);
	}
	

	public synchronized static void w(String tag, String str) {
		str = __ML__() + " : " + str;
		Log.w(tag, str);
		log(tag,str);
	}
	
	public synchronized static void log(String tag, String str) {
		log(String.format("[%s] (%s): %s\n", new SimpleDateFormat("MM-dd HH:mm:ss.SSS").format(new Date()), tag, str).getBytes());// 月-日 时:分:秒.毫秒
	}

	
	private static void log(byte[] buffer) {		
		if (!mIntialized) {
			try {
				intialize();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		if (null == mSystemLog){
			mIntialized = false;
			return;
		}
		
		FileLock lock = null;
		mLock.lock();

		try {
			lock = mSystemLog.getChannel().lock();
			// ///所有的线程 在这里排队等待锁//////
			if (null == mSystemLog) {
				mIntialized = false;
				return;
			}
			
			mSystemLog.write(buffer);
		} catch (Exception e) {
			e.printStackTrace();
			close();
		}finally{
			if (null != lock) {
				try {
					lock.release();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			mLock.unlock();
		}
	}
	
	private static void intialize() throws IOException {
		try {	
			String fileName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
			File logFile = new File(String.format("%s/%s%s.log", Constants.LOG_PATH, LOGFILE_PREFIX, fileName));		
			if (!logFile.exists()) {
				logFile.getParentFile().mkdirs();
				logFile.createNewFile();
				deleteOlderLogFile();
			}
			mSystemLog = new FileOutputStream(logFile, true);
			mIntialized = true;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void deleteOlderLogFile() {
		File logFileDir = new File(Constants.LOG_PATH);
		if (!logFileDir.exists()) {
			return;
		}

		File[] logs = logFileDir.listFiles();
		if (null!=logs && logs.length>0) {
			// 获取离现在之前的过期日期
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, -Constants.LOG_EXPIRED_TIME);// 日期减
			String expire = String.format("%s%s.log", LOGFILE_PREFIX, new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime()));
			String fullFileName = null;
			for (File file : logs) {
				if(!file.isFile()){
					continue;
				}
				fullFileName = file.getName();
				if(fullFileName.compareToIgnoreCase(expire) <= 0){
					file.delete();
				}
			}
		}
	}
	
	private static void close() {
		mIntialized = false;
		if(null != mSystemLog){
			try {
				mSystemLog.close();
			} catch (IOException e) {				
				e.printStackTrace();
			}
		}		
		mSystemLog = null;	
	}
	
	private static String __ML__(){
		return Thread.currentThread().getStackTrace()[4].getMethodName() + "[" + Thread.currentThread().getStackTrace()[4].getLineNumber() + "]";
	}
}
