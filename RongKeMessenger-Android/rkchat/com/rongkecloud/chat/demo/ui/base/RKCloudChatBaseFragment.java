package com.rongkecloud.chat.demo.ui.base;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;

public abstract class RKCloudChatBaseFragment extends Fragment{
	public UiHandler mUiHandler;
	protected boolean mShow;// 是否可见，true:可见 false:不可见
	
	// 等待框使用的属性
	private ProgressDialog mProgressDialog;
	private String mDefaultTitle = "提示";
	private String mDefaultContent = "操作处理中，请稍候...";
	
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mUiHandler = new UiHandler(getActivity());	
		mShow = true;
	};
		
	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		mShow = !hidden;
		if(!mShow){
			closeProgressDialog();
			onDestroyOptionsMenu();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mShow = true;
	}
	
	@Override
	public void onStop() {
		super.onStop();
		mShow = false;		
		closeProgressDialog();
		onDestroyOptionsMenu();
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}
	
	/**
	 * 显示一个加载框,其中标题和显示的内容为默认值
	 */
	public void showProgressDialog(){
		if(null != mProgressDialog && mProgressDialog.isShowing()){
			return;
		}
		mProgressDialog = ProgressDialog.show(getActivity(), mDefaultTitle, mDefaultContent);		
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
		mProgressDialog = ProgressDialog.show(getActivity(), title, message);		
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
	 * 刷新处理
	 */
	abstract public void refresh();
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
