package com.rongkecloud.chat.demo.ui.base;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;

import com.rongkecloud.chat.demo.RKCloudChatLogoutManager;
import com.rongkecloud.chat.demo.RKCloudChatLogoutManager.RKCloudChatLogoutListener;

public abstract class RKCloudChatBaseActivity extends Activity implements RKCloudChatLogoutListener{
	public UiHandler mUiHandler;
	// 等待框使用的属性
	private ProgressDialog mProgressDialog;
	private String mDefaultTitle = "提示";
	private String mDefaultContent = "操作处理中，请稍候...";
	
	// 关闭当前页面的监听事件
	protected OnClickListener mExitListener = new OnClickListener(){
		@Override
		public void onClick(View v) {
			finish();
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		RKCloudChatLogoutManager.getInstance(this).registerLogoutListener(this);
		mUiHandler = new UiHandler(this);	
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		closeProgressDialog();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {		
		RKCloudChatLogoutManager.getInstance(this).cancelLogoutListener(this);
		super.onDestroy();
	}
	
	@Override
	public void onRKCloudChatLogout() {
		if(!isFinishing()){
			finish();
		}
	}
	
	/**
	 * 显示一个加载框,其中标题和显示的内容为默认值
	 */
	public void showProgressDialog(){
		if(null != mProgressDialog && mProgressDialog.isShowing()){
			return;
		}
		mProgressDialog = ProgressDialog.show(this, mDefaultTitle, mDefaultContent);		
	}
	
	/**
	 * 显示一个加载框,其中标题和显示的内容可以自定义
	 * @param title
	 * @param message
	 */
	public void showProgressDialog(String title, String message){
		if(null != mProgressDialog && mProgressDialog.isShowing()){
			return;
		}
		mProgressDialog = ProgressDialog.show(this, title, message);		
	}	
	
	/**
	 * 关闭等待框
	 */
	public void closeProgressDialog(){
		if(null != mProgressDialog && mProgressDialog.isShowing()){
			mProgressDialog.dismiss();
		}	
	}
	
	/**
	 * 处理Http请求的回调
	 * @param msg
	 */
	abstract public void processResult(Message msg);
	
	/**
	 * Http回调时绑定的Handler
	 * @author lion
	 *
	 */
	public class UiHandler extends Handler{
		private WeakReference<Activity> mContext;
		
		public UiHandler(Activity context){
			mContext = new WeakReference<Activity>(context);
		}
		
		public void handleMessage(Message msg) {
			super.handleMessage(msg);						
			if(null == mContext || null == mContext.get()){
				return;
			}			
			processResult(msg);
		}
	}
}
