package com.rongkecloud.test.ui.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.rongkecloud.test.R;
import com.rongkecloud.test.utility.NetworkUtil;

/**
 * 重写TextView组件，主要用于UI上网络中断时的信息提示
 * 
 * @author jessica.yang
 * 
 */
public class NoNetworkHintView extends TextView {
	private BroadcastReceiver mNetWorkBroadReciver;// 监听网络连接状态的广播接收器对象
	private Context mContext;

	private class NetWorkBroadCastReciver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			updateView();
		}
	}

	/**
	 * 构造函数
	 * @param context
	 * @param attrs
	 */
	public NoNetworkHintView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		initShowStyle();
	}

	/**
	 * 构造函数
	 * @param context
	 */
	public NoNetworkHintView(Context context) {
		super(context);
		mContext = context;
		initShowStyle();
	}

	private void initShowStyle(){
		setBackgroundResource(R.drawable.chatdemo_network_offline_hint_bg);
		setText(mContext.getString(R.string.rkcloud_chat_network_off));
		setTextColor(android.graphics.Color.WHITE);		
		setPadding(9, 0, 9, 0);
		setGravity(Gravity.CENTER);
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		init(mContext);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (null != mNetWorkBroadReciver) {
			mContext.unregisterReceiver(mNetWorkBroadReciver);
			mNetWorkBroadReciver = null;
		}
	}

	/*
	 * 初始化操作
	 */
	private void init(Context context) {
		updateView();
		if (null != mNetWorkBroadReciver) {
			return;
		}

		mNetWorkBroadReciver = new NetWorkBroadCastReciver();
		context.registerReceiver(mNetWorkBroadReciver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	/*
	 * 更新view，如果当前网络可用，则隐藏不显示；反之，网络不可用时显示提示信息
	 */
	private void updateView() {
		if (NetworkUtil.isNetworkAvaliable()) {
			setVisibility(View.GONE);
		} else {
			setVisibility(View.VISIBLE);
		}
	}
}