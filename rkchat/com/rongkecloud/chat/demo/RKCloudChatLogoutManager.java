package com.rongkecloud.chat.demo;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import android.content.Context;

public class RKCloudChatLogoutManager {
	private static RKCloudChatLogoutManager mInstance;
	
	private Context mContext;
	private HashSet<WeakReference<RKCloudChatLogoutListener>> mLogoutListeners;
	private WriteLock mWriteLock;
	private ReadLock mReadLock;   
	    
	private RKCloudChatLogoutManager(Context context){
		mContext = context.getApplicationContext();
		mLogoutListeners = new HashSet<WeakReference<RKCloudChatLogoutListener>>();
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    	mReadLock = lock.readLock();
    	mWriteLock = lock.writeLock();
		
	}
	
	public static RKCloudChatLogoutManager getInstance(Context context){
		if(null == mInstance){
			mInstance = new RKCloudChatLogoutManager(context);
		}
		return mInstance;
	}
	
	/**
	 * 注册监听器
	 * @param listener
	 */
	public void registerLogoutListener(RKCloudChatLogoutListener listener){
		mWriteLock.lock();
		try {
			mLogoutListeners.add(new WeakReference<RKCloudChatLogoutListener>(listener));
		}finally{
			mWriteLock.unlock();
		}
	}
	
	/**
	 * 取消监听器
	 * @param listener
	 */
	public void cancelLogoutListener(RKCloudChatLogoutListener listener){
		mWriteLock.lock();
		try {
			if(null!=mLogoutListeners){
				Iterator<WeakReference<RKCloudChatLogoutListener>> iterables = mLogoutListeners.iterator();
				WeakReference<RKCloudChatLogoutListener> weakReference = null;
				RKCloudChatLogoutListener existListener = null;
				while (iterables.hasNext()) {
					weakReference = (WeakReference<RKCloudChatLogoutListener>) iterables.next();
					existListener = weakReference.get();
					
					if(null!= existListener && existListener.equals(listener)){
						iterables.remove();
					}
				}
			}
		}finally {
			mWriteLock.unlock();
		}
	}
	
	private void exitAllActivity(){
		// 回调实现退出时监听接口的操作
		mReadLock.lock();
		try{
			if(null != mLogoutListeners){
				RKCloudChatLogoutListener listener = null;
				for (WeakReference<RKCloudChatLogoutListener> item : mLogoutListeners) {
					listener = item.get();
					if(null != listener){
						listener.onRKCloudChatLogout();
					}
				}
			}
		}finally{
			mReadLock.unlock();
		}	
	}
	
	/**
	 * 退出操作
	 */
	public void logout(){
		// 退出所有Activity
		exitAllActivity();
		// 清除通知栏中所有会话内容
		RKCloudChatMmsManager.getInstance(mContext).cancelNotify(null);
	}
	
	/**
	 * 即时通信退出时的监听器
	 */
	public interface RKCloudChatLogoutListener {
		void onRKCloudChatLogout();

	}
}