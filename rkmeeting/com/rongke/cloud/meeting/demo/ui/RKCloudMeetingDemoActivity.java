package com.rongke.cloud.meeting.demo.ui;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rongke.cloud.meeting.demo.RKCloudMeetingContactManager;
import com.rongke.cloud.meeting.demo.RKCloudMeetingDemoManager;
import com.rongke.cloud.meeting.demo.RKCloudMeetingUiHandlerMessage;
import com.rongke.cloud.meeting.demo.entity.RKCloudMeetingUserInfo;
import com.rongke.cloud.meeting.demo.ui.adapter.RKCloudMeetingUserAdapter;
import com.rongkecloud.multiVoice.RKCloudMeetingCallState;
import com.rongkecloud.multiVoice.RKCloudMeetingInfo;
import com.rongkecloud.multiVoice.RKCloudMeetingUserState;
import com.rongkecloud.sdkbase.RKCloud;
import com.rongkecloud.test.R;

public class RKCloudMeetingDemoActivity extends Activity implements OnClickListener, SensorEventListener{
	private static final int DELAY_EXECUTE_DIMSCREEN_TIME = 1000;// 1s	
	// UI组件
	private View mRootView;// 根布局
	private View mContentBgView;
	private TextView mTitleTipTV;
	private TextView mUserNumTV;//人数显示 
	// 免提
	private LinearLayout mHandsFreeLayout;
	private ImageView mHandsFreeImg;
	private TextView mHandsFreeTV;
	// 静音
	private LinearLayout mMuteLayout;
	private ImageView mMuteImg;
	private TextView mMuteTV;
	// 挂断
	private ImageView mHandupImg;
	// 显示成员信息组件
	private GridView mUserGridView;
	// 成员变量
	private List<RKCloudMeetingUserInfo> mDatas;
	private RKCloudMeetingUserAdapter mAdapter;
	private RKCloudMeetingDemoManager mMeetingManager;
	private Handler mUiHandler;
	private String meetingTime = "";//通话时长;
	
	// 距离感应相关的内容
	private SensorManager mSensorManager;
	private Sensor mSensor = null;
	private float mDistance;
	private boolean mIsSensorTimerRunning = false;// 距离感应使用的定时器是否在运行 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rkcloud_meeting_main);
		
		initViewAndSetListener();
		resolveIntent(getIntent());
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		resolveIntent(intent);
	}

	private void initViewAndSetListener(){	
		mRootView = findViewById(R.id.layout_meetingroot);
		mContentBgView = findViewById(R.id.layout_content);
		mTitleTipTV = (TextView) findViewById(R.id.title_tip);
		mUserNumTV = (TextView) findViewById(R.id.tv_mettingusernum);
		
		mHandsFreeLayout = (LinearLayout) findViewById(R.id.layout_handsfree);
		mHandsFreeImg = (ImageView) findViewById(R.id.multimeeting_handsfree);
		mHandsFreeTV = (TextView) findViewById(R.id.multimeeting_handsfree_text);
		
		mMuteLayout = (LinearLayout) findViewById(R.id.layout_mute);
		mMuteImg = (ImageView) findViewById(R.id.multimeeting_mute);
		mMuteTV = (TextView) findViewById(R.id.multimeeting_mute_text);
		
		mHandupImg = (ImageView) findViewById(R.id.multimeeting_handup);
		
		mUserGridView = (GridView) findViewById(R.id.gridview_userinfo);
		
		mContentBgView.setOnClickListener(this);
		mHandsFreeLayout.setOnClickListener(this);
		mMuteLayout.setOnClickListener(this);
		mHandupImg.setOnClickListener(this);
		
		mUserGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				RKCloudMeetingUserInfo info = mDatas.get(position);
				RKCloudMeetingContactManager.getInstance(RKCloudMeetingDemoActivity.this).jumpContactDetailInfoUI(RKCloudMeetingDemoActivity.this, info.getAttendeeAccount());
			}
		});
	}
	
	private void resolveIntent(Intent intent){				
		mMeetingManager = RKCloudMeetingDemoManager.getInstance(this);
		
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);	

		mUiHandler = new UiHandler(this);
		mDatas = new ArrayList<RKCloudMeetingUserInfo>();
		mAdapter = new RKCloudMeetingUserAdapter(getBaseContext(), mDatas);
		mUserGridView.setAdapter(mAdapter);
		
		RKCloudMeetingInfo meetingInfo = mMeetingManager.getMeetingInfo();
		
		// 无会议或空闲等状态时结束
		if(null==meetingInfo || RKCloudMeetingCallState.MEETING_CALL_IDLE==meetingInfo.callState
				|| RKCloudMeetingCallState.MEETING_CALL_PREPARING==meetingInfo.callState){
			mMeetingManager.cancelNotification();
			finish();
			return;
		}
		
		// 设置音量键调节的音量类型
		setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
		// 数据初始化
		showWidgets(meetingInfo.callState, 0);
		// 同步成员数据
		refreshAttendeeInfo();
		// 个人操作同步
		showSelfInfo();
	}

	@Override
	protected void onResume() {
		super.onResume();		
		mMeetingManager.bindUiHandler(mUiHandler);
		RKCloudMeetingContactManager.getInstance(this).bindUiHandler(mUiHandler);
		mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		mIsSensorTimerRunning = false;
	}
	
	@Override
	public void onClick(View v) {		
		switch (v.getId()) {
		case R.id.layout_handsfree://免提
			boolean isHandFree = mMeetingManager.isCurrMeetingHandFree();
			mMeetingManager.handFree(!isHandFree);
			showSelfInfo();
			break;
			
        case R.id.multimeeting_handup://挂断
        	mMeetingManager.handup();
        	mUiHandler.removeMessages(RKCloudMeetingUiHandlerMessage.MSG_WHAT_MEETING_UPDATETIME);
			break;
			
        case R.id.layout_mute://静音
        	boolean isMute = mMeetingManager.isCurrMute();
        	mMeetingManager.mute(!isMute);
        	RKCloudMeetingUserInfo selfInfo = getUserInfoByAccount(RKCloud.getUserName());
        	if(null != selfInfo){
        		selfInfo.setMute(!isMute);
        		mAdapter.notifyDataSetChanged();
            	showSelfInfo();
        	}
        	break;
		}
	}
	

	@Override
	public void onSensorChanged(SensorEvent event) {
		mDistance = event.values[0];
		if(mDistance < mSensor.getMaximumRange()){
			if(!mIsSensorTimerRunning){
				mUiHandler.sendEmptyMessageDelayed(RKCloudMeetingUiHandlerMessage.HANDLER_MSG_WHAT_SENSOR, DELAY_EXECUTE_DIMSCREEN_TIME);				
				mIsSensorTimerRunning = true;
			}
			
		}else{
			lightScreen();
		}		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {		
	}
	
	private void lightScreen() {
		WindowManager.LayoutParams attrs = getWindow().getAttributes();
		attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
		mContentBgView.setVisibility(View.VISIBLE);
		mContentBgView.setClickable(true);
		mRootView.setBackgroundColor(Color.TRANSPARENT);
		getWindow().setAttributes(attrs);
	}
	

	private void dimScreen() {
		WindowManager.LayoutParams attrs = getWindow().getAttributes();
		attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
		mContentBgView.setVisibility(View.INVISIBLE);
		mContentBgView.setClickable(false);
		mRootView.setBackgroundColor(Color.BLACK);
		getWindow().setAttributes(attrs);
	}

	
	private RKCloudMeetingUserInfo getUserInfoByAccount(String account){
		for(RKCloudMeetingUserInfo user : mDatas){
			if(account.equalsIgnoreCase(user.getAttendeeAccount())){
				return user;
			}
		}
		return null;
	}
	
	/*
	 * 组件显示
	 */
	private void showWidgets(int state, int stateReason){
		switch(state){
		case RKCloudMeetingCallState.MEETING_CALL_ANSWER:// 呼叫已经接听
//			if(mDatas.size() > 1){
////				mTitleTipTV.setText(R.string.rkcloud_meeting_title);
////				
//			}else{
////				mTitleTipTV.setText(R.string.rkcloud_meeting_wait);
//			}
			
			mUiHandler.sendEmptyMessage(RKCloudMeetingUiHandlerMessage.MSG_WHAT_MEETING_UPDATETIME);
			break;
			
		case RKCloudMeetingCallState.MEETING_CALL_HANGUP:// 挂断
			finish();
			break;
		}
	}
	
	private void showSelfInfo(){
		boolean isMute = mMeetingManager.isCurrMute();
		if(isMute){
			mMuteImg.setImageResource(R.drawable.rkcloud_meeting_mute_on);
    		mMuteTV.setSelected(true);
    	}else{
    		mMuteImg.setImageResource(R.drawable.rkcloud_meeting_mute_off);
    		mMuteTV.setSelected(false);
    	}
    	
		boolean isHandFree = mMeetingManager.isCurrMeetingHandFree();
		if(isHandFree){ 
			mHandsFreeImg.setImageResource(R.drawable.rkcloud_meeting_handsfree_on);
			mHandsFreeTV.setSelected(true);
		}else{
			mHandsFreeImg.setImageResource(R.drawable.rkcloud_meeting_handsfree_off);
			mHandsFreeTV.setSelected(false);
		}
	}
	
	/*
	 * 刷新用户数据
	 */
	private synchronized void refreshAttendeeInfo(){		
		Map<String, RKCloudMeetingUserInfo> syncDatas = mMeetingManager.getMeetingMembers();	
		List<RKCloudMeetingUserInfo> deleteIndexs = new ArrayList<RKCloudMeetingUserInfo>();
		for(RKCloudMeetingUserInfo bean : mDatas){
			RKCloudMeetingUserInfo syncBean = syncDatas.remove(bean.getAttendeeAccount());
			if(null == syncBean){
				// 表示不存在的需要删除
				deleteIndexs.add(bean);
			}else{
				// 同步数据
				bean.copyData(syncBean);
			}
		}
		// 删除需要删除的用户
		for(RKCloudMeetingUserInfo bean : deleteIndexs){
			mDatas.remove(bean);
		}
		// 添加新增的用户
		for(String account : syncDatas.keySet()){
			if(account.equalsIgnoreCase(RKCloud.getUserName())){
				mDatas.add(0, syncDatas.get(account));
			}else{
				mDatas.add(syncDatas.get(account));
			}
		}
		
		mAdapter.notifyDataSetChanged();
		
		showSelfInfo();
		if(mDatas.size() > 1){
			mUserNumTV.setVisibility(View.VISIBLE);
			mUserNumTV.setText(getString(R.string.rkcloud_meeting_user_num, mDatas.size()));//人数显示
		}else{
			mUserNumTV.setVisibility(View.INVISIBLE);
		}
		
		
		// title内容的显示
		if(mDatas.size() > 1){
//			mTitleTipTV.setText(R.string.rkcloud_meeting_title);
//			mUiHandler.sendEmptyMessage(RKCloudMeetingUiHandlerMessage.MSG_WHAT_MEETING_UPDATETIME);
			
		}else{
//			mTitleTipTV.setText(R.string.rkcloud_meeting_wait);
//			mUiHandler.sendEmptyMessage(RKCloudMeetingUiHandlerMessage.MSG_WHAT_MEETING_UPDATETIME);
		}
	}
	/**
	 * 更新时间
	 */
	private void updateTime(){
		meetingTime = mMeetingManager.getMeetingProgressTime();
		mTitleTipTV.setText(meetingTime);
		mUiHandler.sendEmptyMessageDelayed(RKCloudMeetingUiHandlerMessage.MSG_WHAT_MEETING_UPDATETIME, 1000);
	}
	
	private void processResult(Message msg) {
		if(RKCloudMeetingUiHandlerMessage.HANDLER_MSG_WHAT_SENSOR == msg.what){
			if(mDistance <mSensor.getMaximumRange()){
				dimScreen();
			}
			mIsSensorTimerRunning = false;
			
		}else if(RKCloudMeetingUiHandlerMessage.MSG_WHAT_MEETING_CALLSTATUSCHANGED == msg.what){
			showWidgets(msg.arg1, msg.arg2);
			
		}else if(RKCloudMeetingUiHandlerMessage.MSG_WHAT_MEETING_USERINFOSCHANGED == msg.what){
			String account = (String)msg.obj;
			String showName = null;
			if(account.equalsIgnoreCase(RKCloud.getUserName())){
				showName = getString(R.string.rkcloud_meeting_me);
			}else{
				RKCloudMeetingUserInfo userBean = RKCloudMeetingContactManager.getInstance(this).getContactInfo(account);
				showName = null!=userBean ? userBean.getShowName() : account;
			}
			switch (msg.arg1) {
			case RKCloudMeetingUserState.MEETING_USER_STATE_IN:// 用户进来
				if(!account.equalsIgnoreCase(RKCloud.getUserName())){
					mMeetingManager.showToastText(this, getString(R.string.rkcloud_meeting_user_in, showName));
				}
				break;
				
			case RKCloudMeetingUserState.MEETING_USER_STATE_OUT://用户退出
				mMeetingManager.showToastText(this, getString(R.string.rkcloud_meeting_user_out, showName));
			   break;
			}
			refreshAttendeeInfo();
			
		}else if(RKCloudMeetingUiHandlerMessage.MSG_WHAT_MEETING_SYNCMEETINGINFO == msg.what){ // 同步会议信息
			refreshAttendeeInfo();
		}else if(RKCloudMeetingUiHandlerMessage.MSG_WHAT_MEETING_CONTACTSINFO_CHANGED == msg.what){ // 联系人信息有变更
			List<String> accounts = (List<String>)msg.obj;
			if(null!=accounts && accounts.size()>0){
				for(RKCloudMeetingUserInfo bean : mDatas){
					if(accounts.contains(bean.getAttendeeAccount())){
						bean.copyData(RKCloudMeetingContactManager.getInstance(this).getContactInfo(bean.getAttendeeAccount()));
					}
				}
				mAdapter.notifyDataSetChanged();
			}
		}else if(RKCloudMeetingUiHandlerMessage.MSG_WHAT_MEETING_CONTACT_HEADERIMAGE_CHANGED == msg.what){ // 联系人头像有变更
			String account = (String)msg.obj;
			for(RKCloudMeetingUserInfo bean : mDatas){
				if(account.contains(bean.getAttendeeAccount())){
					bean.copyData(RKCloudMeetingContactManager.getInstance(this).getContactInfo(bean.getAttendeeAccount()));
					mAdapter.notifyDataSetChanged();
					break;
				}
			}
		}else if(RKCloudMeetingUiHandlerMessage.MSG_WHAT_MEETING_UPDATETIME == msg.what){//联系人时间
			updateTime();
		}
	}
	
	private class UiHandler extends Handler{
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
