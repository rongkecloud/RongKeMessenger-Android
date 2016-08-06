package com.rongke.cloud.av.demo;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.rongke.cloud.av.demo.entity.RKCloudAVContact;
import com.rongke.cloud.av.demo.tools.RKCloudAVUtils;
import com.rongke.cloud.av.demo.ui.RKCloudAVCallLogListActivity;
import com.rongke.cloud.av.demo.ui.RKCloudAVDemoActivity;
import com.rongkecloud.av.*;
import com.rongkecloud.av.interfaces.RKCloudAVNewCallCallBack;
import com.rongkecloud.av.interfaces.RKCloudAVStateCallBack;
import com.rongkecloud.chat.LocalMessage;
import com.rongkecloud.chat.RKCloudChatBaseMessage.MSG_STATUS;
import com.rongkecloud.chat.SingleChat;
import com.rongkecloud.chat.demo.RKCloudChatConstants;
import com.rongkecloud.chat.demo.RKCloudChatMmsManager;
import com.rongkecloud.sdkbase.RKCloud;
import com.rongkecloud.sdkbase.RKCloudLog;
import com.rongkecloud.test.R;

import java.util.ArrayList;
import java.util.List;

public class RKCloudAVDemoManager implements RKCloudAVNewCallCallBack{
	private static final String TAG = RKCloudAVDemoManager.class.getSimpleName();	
	
	// 通知栏显示的ID类型值
	private static final int NOTIFICATION_ID_MISSED = 1;// 未接
	private static final int NOTIFICATION_ID_OUT = 2;// 呼出
	private static final int NOTIFICATION_ID_IN = 3;// 来电
	
	private static RKCloudAVDemoManager mInstance = null;
	
	private Context mContext;
	private NotificationManager mNotificationManager;		
	private AudioManager mAudioManager;	
	
	private Handler mUiHandler;
	private boolean mIsCallActivityShow = true;// 音视频通话的窗口是否为可见状态 true：可见  false:不可见
	private boolean mFloatDefaultShowSelf = true;// 悬浮窗中是否默认显示用户自己的图像 true:表示默认显示自己的图像
	
	private RKCloudAVCallInfo mCurrCallInfo;
	private boolean mHandFreeStatus = false;// 是否为免提模式，true:免提模式 false:听筒模式
	private boolean mMuteStatus = false;// 是否为静音状态，true:静音状态  false:发言状态
	private boolean mSwitchCameraStatus = false;// 切换摄像头按钮的按下状态 true:表示为按下状态 false:正常状态
	private int mCameraId = -1;// 摄像头ID
	
	// 悬浮窗使用的内容
	private WindowManager mWindowManager;
	private WindowManager.LayoutParams mWinManagerParams;
	private int mFloatWinWidth,mFloatWinHeight,mFloatWinMarginTop,mFloatWinMarginRight;
	private View mFloatLayout;	
	private RelativeLayout mCallUiVideoLayout;// 通话页面展示视频图像的区域
	private RelativeLayout mFloatBigImageLayout, mFloatSmallImageLayout;
	private SurfaceView mLocalSurfaceView, mRemoteSurfaceView;
	private int mLastX=0, mLastY=0;
	private int mStartX=0, mStartY=0;	
	private boolean mFloatWinClickToEnterCallUi = false;// 是否允许点击悬浮窗跳转到通话页面，true:允许
	
	private long mLastDailTime = 0;//记录上次dail的时间，单位：毫秒
	private long mTime = 1 * 60 * 1000;//配置dail 上次点击和当前时间的差值；
	
	private RKCloudAVContactManager mAvContactManager;
	
	private RKCloudAVDemoManager(Context context){
		mContext = context;
		mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		// 获取的是LocalWindowManager对象
		mWindowManager = (WindowManager) mContext.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics metric = new DisplayMetrics();
		mWindowManager.getDefaultDisplay().getMetrics(metric);
		int screenWidth = metric.widthPixels;
		
		mFloatWinWidth = (int)(screenWidth*0.8/3);
		mFloatWinHeight = mFloatWinWidth*4/3; 
		mFloatWinMarginTop = (int) mContext.getResources().getDimension(R.dimen.rkcloud_av_floatwin_margintop);
		mFloatWinMarginRight = (int) mContext.getResources().getDimension(R.dimen.rkcloud_av_floatwin_marginright);    
		mAvContactManager = RKCloudAVContactManager.getInstance(context);
	}
	
	public static RKCloudAVDemoManager getInstance(Context context){
		if(null == mInstance){
			mInstance = new RKCloudAVDemoManager(context);
		}
		
		return mInstance;
	}
	
	public void bindUiHandler(Handler uiHandler){
		mUiHandler = uiHandler;
	}
	
	/**
	 * 退出时的操作
	 */
	public void logout(){
		// 取消通知栏所有通话的通知
		mNotificationManager.cancel(NOTIFICATION_ID_OUT);
		mNotificationManager.cancel(NOTIFICATION_ID_IN);
		mNotificationManager.cancel(NOTIFICATION_ID_MISSED);
	}
	
	
	/**
	 * 打开通话记录界面
	 * @param mContext
	 */
	public void openCallLog(Context mContext) {
		Intent intent = new Intent(mContext, RKCloudAVCallLogListActivity.class);	
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(intent);		
	}
	
	@Override
	public void onMissedCall(String callerAccount, boolean isVideo, long callTime) {
		// 在通知栏中显示未接来电
//		showMissedCallNotification(callerAccount);
		
		// TODO 向即时通信消息聊天页面添加记录		
		LocalMessage msgObj = LocalMessage.buildReceivedMsg(callerAccount, mContext.getString(R.string.rkcloud_av_msg_callmissed), callerAccount);
		msgObj.setMsgTime(callTime);
		msgObj.setStatus(MSG_STATUS.RECEIVE_RECEIVED);
		addCallMsg(msgObj, isVideo, true);
		
		// 同步用户信息
		mAvContactManager.syncContactInfo(callerAccount);
	};
	
	@Override
	public void onNewCall(final String callerAccount, final boolean isVideo) {
		if(TextUtils.isEmpty(callerAccount)){
			return;
		}
		
		RKCloudAV.setRKCloudAVStateCallBack(new RKCloudAVStateCallBack() {			
			@Override
			public void onStateCallBack(int state, int stateReason) {
				RKCloudLog.d(TAG, String.format("state=%s stateReason=%s", state,stateReason));		
				if(RKCloudAVCallState.AV_CALL_STATE_RINGIN == state){
					mCurrCallInfo = RKCloudAV.rkCloudAVManager.getAVCallInfo();
					
				}else if(RKCloudAVCallState.AV_CALL_STATE_ANSWER == state){
					mCurrCallInfo = RKCloudAV.rkCloudAVManager.getAVCallInfo();
					
				}else if(RKCloudAVCallState.AV_CALL_STATE_VIDEO_INIT == state){
					initVideoInfo();
					controllShowVideos();
					
				}else if(RKCloudAVCallState.AV_CALL_STATE_VIDEO_START == state){
					mCurrCallInfo = RKCloudAV.rkCloudAVManager.getAVCallInfo();
					
				}else if(RKCloudAVCallState.AV_CALL_STATE_VIDEO_STOP == state){
					mCurrCallInfo = RKCloudAV.rkCloudAVManager.getAVCallInfo();
					remoteFloatWin();
					
				}else if(RKCloudAVCallState.AV_CALL_STATE_HANGUP == state){
					String content = mContext.getString(R.string.rkcloud_av_hangup);
					String mmsContent = null;
					boolean isNotify = false;// 是否提醒
					switch(stateReason){
					case RKCloudAVErrorCode.AV_CALLEE_REJECT:
						content = mContext.getString(R.string.rkcloud_av_callfailed_calleereject);
						mmsContent = mContext.getString(R.string.rkcloud_av_msg_callee_calleereject);
						break;
						
					case RKCloudAVErrorCode.AV_CALLEE_ANSWER_TIMEOUT:						
						mmsContent = mContext.getString(R.string.rkcloud_av_msg_callmissed);
						isNotify = true;
						break;
                    case RKCloudAVErrorCode.AV_CALL_OTHER_FAIL:
                        mmsContent =  mContext.getString(R.string.rkcloud_av_msg_callmissed);
                        break;

					case RKCloudAVErrorCode.AV_CALLER_CANCEL:
						content = mContext.getString(R.string.rkcloud_av_callfailed_cancel);
						mmsContent = mContext.getString(R.string.rkcloud_av_msg_callmissed);
						isNotify = true;
						break;
						
					case RKCloudAVErrorCode.AV_NO_REASON:
						if(null!=mCurrCallInfo && mCurrCallInfo.callAnswerTime>0){
							long duration = (long)Math.ceil((System.currentTimeMillis()-mCurrCallInfo.callAnswerTime)/1000);
							mmsContent = mContext.getString(R.string.rkcloud_av_msg_callduration, RKCloudAVUtils.secondConvertToTime(duration));
						}
						break;
					}
					if(null!=mmsContent){
						// TODO 向即时通信消息表中插入记录
						LocalMessage msgObj = LocalMessage.buildReceivedMsg(callerAccount, mmsContent, callerAccount);
						if(isNotify){
							msgObj.setStatus(MSG_STATUS.RECEIVE_RECEIVED);
						}
						addCallMsg(msgObj, isVideo, isNotify);
					}
					
					showToastText(content);		
					if(RKCloudAVErrorCode.RK_EXIST_AV_CALLING != stateReason){
						hideInCallNotification();
						switchToIdle();
					}
				}
				
				if(null != mUiHandler){
					Message msg = mUiHandler.obtainMessage();
					msg.what = RKCloudAVUiHandlerMessage.HANDLER_MSG_WHAT_AV;
					msg.arg1 = state;
					msg.arg2 = stateReason;
					msg.sendToTarget();
				}
			}
		});	
		
		showInCallNotification(callerAccount, System.currentTimeMillis());
		initCallData(callerAccount, false, isVideo);
		
		Intent intent = new Intent(mContext, RKCloudAVDemoActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(RKCloudAVDemoActivity.INTENT_KEY_INCALL_ACCOUNT, callerAccount);
		mContext.startActivity(intent);	
		// 同步用户信息
		mAvContactManager.syncContactInfo(callerAccount);
	}
	
	/**
	 * 打开呼叫界面，并发起呼叫
	 * @param context
	 * @param calleeAccount
	 * @param isVideo 是否为视频通话  true:视频通话 false:语音通话
	 */
	public void dial(final Context context, final String calleeAccount, final boolean isVideo){		
		// 条件判断
		if(null == RKCloudAV.rkCloudAVManager){
			showToastText(context.getString(R.string.rkcloud_av_callfailed_sdkfailed));
			return;
		}
		// 有GSM电话时禁止呼出
		if(RKCloudAVUtils.isInSystemCall(mContext)){
			showToastText(context.getString(R.string.rkcloud_av_callfailed_curr_hasgsmcall));
			return;
		}
		
		if(System.currentTimeMillis() - mLastDailTime < mTime){
			showToastText(context.getString(R.string.rkcloud_av_callfailed_frequent_operation));
			return;
		}
		mLastDailTime = System.currentTimeMillis();
		RKCloudAV.rkCloudAVManager.dial(calleeAccount, isVideo, new RKCloudAVStateCallBack() {
			@Override
			public void onStateCallBack(int state, int stateReason) {
				RKCloudLog.d(TAG, String.format("dial--statecallback--state=%s stateReason=%s", state,stateReason));
				if(RKCloudAVCallState.AV_CALL_STATE_PREPARING == state){
					//准备状态进入通话页面但是没有铃声；
					showOutCallNotification(calleeAccount, System.currentTimeMillis());
					initCallData(calleeAccount, true, isVideo);
					
					Intent intent = new Intent(context, RKCloudAVDemoActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.putExtra(RKCloudAVDemoActivity.INTENT_KEY_OUTCALL_ACCOUNT, calleeAccount);
					context.startActivity(intent);	
					// 同步通话信息
					mCurrCallInfo = RKCloudAV.rkCloudAVManager.getAVCallInfo();
				}else if(RKCloudAVCallState.AV_CALL_STATE_RINGBACK == state){
					// 同步通话信息
					mCurrCallInfo = RKCloudAV.rkCloudAVManager.getAVCallInfo();
					
				}else if(RKCloudAVCallState.AV_CALL_STATE_ANSWER == state){
					// 同步通话信息
					mCurrCallInfo = RKCloudAV.rkCloudAVManager.getAVCallInfo();
					
				}else if(RKCloudAVCallState.AV_CALL_STATE_VIDEO_INIT == state){
					initVideoInfo();
					controllShowVideos();
					
				}else if(RKCloudAVCallState.AV_CALL_STATE_VIDEO_START == state){
					// 同步通话信息
					mCurrCallInfo = RKCloudAV.rkCloudAVManager.getAVCallInfo();
					
				}else if(RKCloudAVCallState.AV_CALL_STATE_VIDEO_STOP == state){
					// 同步通话信息
					mCurrCallInfo = RKCloudAV.rkCloudAVManager.getAVCallInfo();
					remoteFloatWin();
					
				}else if(RKCloudAVCallState.AV_CALL_STATE_HANGUP == state){					
					// 弹出提示的消息内容
					String content = context.getString(R.string.rkcloud_av_hangup);
					// 向即时通信中写入消息的内容
					String mmsContent = null;
					switch(stateReason){
					case RKCloudAVErrorCode.RK_PARAMS_ERROR:
						content = context.getString(R.string.rkcloud_av_callfailed_paramerror);
						break;
						
					case RKCloudAVErrorCode.RK_SDK_UNINIT:
						content = context.getString(R.string.rkcloud_av_callfailed_sdkfailed);
						break;
						
					case RKCloudAVErrorCode.RK_EXIST_AV_CALLING:
						content = context.getString(R.string.rkcloud_av_callfailed_curr_hascall);
						break;
						
					case RKCloudAVErrorCode.RK_EXIST_MEETINGING:
						content = context.getString(R.string.rkcloud_av_callfailed_curr_hasmeeting);
						break;
						
					case RKCloudAVErrorCode.AV_CANNOT_CALL_OWN:
						content = context.getString(R.string.rkcloud_av_callfailed_forbid_toyourself);
						break;
						
					case RKCloudAVErrorCode.AV_CALL_OTHER_FAIL:
						content = context.getString(R.string.rkcloud_av_callfailed_otherreason);
						mmsContent = context.getString(R.string.rkcloud_av_msg_caller_callfailed);
						break;	
					
					case RKCloudAVErrorCode.RK_NONSUPPORT_AVCALL:
						content = context.getString(R.string.rkcloud_av_callfailed_unsupport);
						break;	
						
					case RKCloudAVErrorCode.AV_CALLEE_UNLINE:
						content = context.getString(R.string.rkcloud_av_callfailed_unline);
						mmsContent = context.getString(R.string.rkcloud_av_msg_caller_calleeunline);
						break;	
						
					case RKCloudAVErrorCode.AV_CALLEE_NO_ANSWER:
						content = context.getString(R.string.rkcloud_av_callfailed_noanswer);
						mmsContent = context.getString(R.string.rkcloud_av_msg_caller_calleenoanswer);
						break;
						
					case RKCloudAVErrorCode.AV_CALLEE_REJECT:
						content = context.getString(R.string.rkcloud_av_callfailed_reject);
						mmsContent = context.getString(R.string.rkcloud_av_msg_caller_calleereject);
						break;	

                    case RKCloudAVErrorCode.AV_CALLEE_ANSWER_TIMEOUT:
					case RKCloudAVErrorCode.AV_CALLER_CANCEL:
						content = context.getString(R.string.rkcloud_av_callfailed_cancel);
						mmsContent = context.getString(R.string.rkcloud_av_msg_caller_callcanel);
						break;	
						
					case RKCloudAVErrorCode.AV_CALLEE_BUSY:
						content = context.getString(R.string.rkcloud_av_callfailed_calling);
						mmsContent = context.getString(R.string.rkcloud_av_msg_caller_calleecalling);
						break;
						
					case RKCloudAVErrorCode.AV_NO_REASON:
						if(null!=mCurrCallInfo && mCurrCallInfo.callAnswerTime>0){
							long duration = (long)Math.ceil((System.currentTimeMillis()-mCurrCallInfo.callAnswerTime)/1000);
							mmsContent = context.getString(R.string.rkcloud_av_msg_callduration, RKCloudAVUtils.secondConvertToTime(duration));
						}
						break;
					}
					
					if(!TextUtils.isEmpty(mmsContent)){
						// TODO 向即时通信消息聊天页面添加记录
						LocalMessage msgObj = LocalMessage.buildSendMsg(calleeAccount, mmsContent, RKCloud.getUserName());
						addCallMsg(msgObj, isVideo, false);
					}
					showToastText(content);	
					
					if(RKCloudAVErrorCode.RK_EXIST_AV_CALLING != stateReason){
						// 取消通知栏中的电话图标
						hideOutCallNotification();
						switchToIdle();
					}
				}
				
				if(null != mUiHandler){
					Message msg = mUiHandler.obtainMessage();
					msg.what = RKCloudAVUiHandlerMessage.HANDLER_MSG_WHAT_AV;
					msg.arg1 = state;
					msg.arg2 = stateReason;
					msg.sendToTarget();
				}
			}
		});			
	}
	
	/*
	 * 初始化通话数据
	 */
	private void initCallData(String account, boolean isCaller, boolean isVideoCall){
		mHandFreeStatus = false;
		mMuteStatus = false;
		mSwitchCameraStatus = false;
		
		if(isVideoCall){
			if(checkCameraHardware()){
				mCameraId = enableSwitchCamera() ? CameraInfo.CAMERA_FACING_FRONT : CameraInfo.CAMERA_FACING_BACK;
			}else{
				mCameraId = -1;
			}
		}
	}
	
	/*
	 * 重置数据
	 */
	private void switchToIdle(){
		mCurrCallInfo = null;
		mHandFreeStatus = false;
		mMuteStatus = false;
		mSwitchCameraStatus = false;
		mCameraId = -1;
		
		mFloatDefaultShowSelf = true;
		remoteFloatWin();
		mLastDailTime = 0;
	}
	
	/*
	 * 移除悬浮窗
	 */
	private void remoteFloatWin(){
		if(null != mFloatLayout){
			mWindowManager.removeView(mFloatLayout);
			mFloatLayout = null;
			mFloatBigImageLayout = null;
			mFloatSmallImageLayout = null;
			mLocalSurfaceView = null;
			mRemoteSurfaceView = null;
		}
		mWinManagerParams = null;
		mLastX = 0;
		mLastY = 0;
		mStartX = 0;
		mStartY = 0;
	}
	
	/*
	 * 向即时通信中添加消息记录
	 */
	private void addCallMsg(LocalMessage msgObj, boolean isVideo, boolean isNotify){
		msgObj.setExtension(isVideo ? RKCloudChatConstants.FLAG_AVCALL_IS_VIDEO : RKCloudChatConstants.FLAG_AVCALL_IS_AUDIO);
		RKCloudChatMmsManager mmsManager = RKCloudChatMmsManager.getInstance(mContext);
		mmsManager.addLocalMsg(msgObj, SingleChat.class);
		if(isNotify){
			// 向通知栏中发送消息
			mmsManager.onReceivedMsg(mmsManager.queryChat(msgObj.getChatId()), msgObj);
		}
	}
	
	/**
	 * 获取所有的通话记录
	 * @return
	 */
	public List<RKCloudAVCallLog> getAllCallLogs(){
		return null!=RKCloudAV.rkCloudAVManager ? RKCloudAV.rkCloudAVManager.getAllCallLogs() : new ArrayList<RKCloudAVCallLog>();
	}
	
	/**
	 * 删除通话记录
	 */
	public boolean delCallLog(long callId){
		return null!=RKCloudAV.rkCloudAVManager ? RKCloudAV.rkCloudAVManager.delCallLogById(callId) : false;
	}
	
	/**
	 * 删除通话记录
	 */
	public boolean delCallLogByAccount(String account){
		return null!=RKCloudAV.rkCloudAVManager ? RKCloudAV.rkCloudAVManager.delCallLogByAccount(account) : false;
	}	
	
	/**
	 * 清空通话记录
	 */
	public boolean clearCallLogs(){
		return null!=RKCloudAV.rkCloudAVManager ? RKCloudAV.rkCloudAVManager.delAllCallLog() : false;
	}
	
	/**
	 * 获取通话信息
	 * @return
	 */
	public RKCloudAVCallInfo getAVCallInfo(){
		return mCurrCallInfo;
	}
	
	/** 
	 * 检查设备是否提供摄像头 
	 * @return true:摄像头存在  false:摄像头不存在 
	 */ 
	public boolean checkCameraHardware(){
		if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){ 
	        return true; 
	    } else { 
	        return false; 
	    } 
	}
	
	/**
	 * 是否有切换前置/后置摄像头的能力
	 * @return
	 */
	@SuppressLint("NewApi")
	public boolean enableSwitchCamera(){
		if(!checkCameraHardware()){
			return false;
		}
		return Camera.getNumberOfCameras() > 1;
	}
	
	/**
	 * 设置通话页面显示视频图像的组件
	 * @param layout
	 */
	public void setCallUiShowVideoLayout(RelativeLayout layout){
		mCallUiVideoLayout = layout;
	}
	
	/*
	 * 初始化视频相关的信息
	 */
	private void initVideoInfo(){
		// 非视频通话时返回
		if(null==RKCloudAV.rkCloudAVManager || null==mCurrCallInfo || !mCurrCallInfo.isCurrVideoOpen){
			return;
		}
		
		RKCloudAV.rkCloudAVManager.setOrientation(true);
		RKCloudAV.rkCloudAVManager.initVideoInfo();
		if(-1 != mCameraId){
			RKCloudAV.rkCloudAVManager.setCamera(mCameraId);
		}

		mLocalSurfaceView = RKCloudAV.rkCloudAVManager.getLocalRenderer();
		mRemoteSurfaceView = RKCloudAV.rkCloudAVManager.getRemoteRenderer();
		// 创建悬浮窗
		createFloatView();
	}
	
	/**
	 * 设置通话页面是否为显示状态
	 * @param isShow
	 */
	public synchronized void setCallUiIsShow(boolean isShow){
		mIsCallActivityShow = isShow;
		if(null!=mCurrCallInfo && mCurrCallInfo.callState==RKCloudAVCallState.AV_CALL_STATE_ANSWER){
			if(isShow){
				mFloatDefaultShowSelf = true;
				
			}else{
				if(null != mFloatSmallImageLayout){
					mFloatSmallImageLayout.setVisibility(View.VISIBLE);
				}
				mFloatWinClickToEnterCallUi = true;
			}
			
			if(mCurrCallInfo.isCurrVideoOpen){
				controllShowVideos();
				// 控制悬浮窗的显示位置
				mWinManagerParams.x = mFloatWinMarginRight;
				mWinManagerParams.y = mFloatWinMarginTop;
				mWinManagerParams.width = mFloatWinWidth;
				mWinManagerParams.height = mFloatWinHeight;
				mWindowManager.updateViewLayout(mFloatLayout, mWinManagerParams);
			}
		}
	}
	
	/*
	 * 控制视频的显示
	 */
	private synchronized void controllShowVideos(){
		if(null!=mCurrCallInfo && mCurrCallInfo.isCurrVideoOpen){
			mCallUiVideoLayout.removeAllViews();
			mFloatBigImageLayout.removeAllViews();
			mFloatSmallImageLayout.removeAllViews();
			
			if(mIsCallActivityShow){
				mFloatSmallImageLayout.setVisibility(View.GONE);	
				if(mFloatDefaultShowSelf){
					mCallUiVideoLayout.addView(mRemoteSurfaceView);
					mFloatBigImageLayout.addView(mLocalSurfaceView);
					
				}else{
					mCallUiVideoLayout.addView(mLocalSurfaceView);
					mFloatBigImageLayout.addView(mRemoteSurfaceView);
				}
							 
			}else{
				mFloatSmallImageLayout.setVisibility(View.VISIBLE);	
				mFloatSmallImageLayout.addView(mLocalSurfaceView);
				mLocalSurfaceView.setZOrderMediaOverlay(true);
				mFloatBigImageLayout.addView(mRemoteSurfaceView);				
			}
		}
	}
	
	/**
	 * 被叫应答电话
	 */
	public void answer(){
		RKCloudAV.rkCloudAVManager.answer();
	}
	
	/**
	 * 控制免提的打开与关闭操作
	 * @param handfree
	 */
	public void handFree(boolean handfree){
		mHandFreeStatus = handfree;
		mAudioManager.setSpeakerphoneOn(mHandFreeStatus);	
	}
	
	/**
	 * 静音操作
	 */
	public void mute(boolean isMute){
		mMuteStatus = isMute;
		RKCloudAV.rkCloudAVManager.mute(mMuteStatus);
	}
	
	/**
	 * 切换前置/后置摄像头
	 */
	@SuppressLint("InlinedApi")
	public void switchCamera(boolean status){
		mSwitchCameraStatus = status;
		if(mCameraId == CameraInfo.CAMERA_FACING_FRONT){
			mCameraId = CameraInfo.CAMERA_FACING_BACK;
		}else{
			mCameraId = CameraInfo.CAMERA_FACING_FRONT;
		}
		RKCloudAV.rkCloudAVManager.setCamera(mCameraId);
	}
	
	/**
	 * 切换为语音模式
	 * @param 
	 */
	public void switchToAudioCall(){
		if(null!=mCurrCallInfo){
			RKCloudAV.rkCloudAVManager.stopVideo();
			mCurrCallInfo = RKCloudAV.rkCloudAVManager.getAVCallInfo();
			remoteFloatWin();
		}
	}
	
	/**
	 * 挂断
	 */
	public void hangup(){
		RKCloudAV.rkCloudAVManager.hangup();
	}
	
	/**
	 * 获取免提是否被按下的状态
	 * @return
	 */
	public boolean getHandFreeStatus(){
		return mHandFreeStatus;
	}
	
	/**
	 * 获取转换摄像头是否被按下的状态
	 * @return
	 */
	public boolean getSwitchCameraStatus(){
		return mSwitchCameraStatus;
	}
	
	/**
	 * 获取静音按钮是否按下的状态
	 * @return
	 */
	public boolean getMuteStatus(){
		return mMuteStatus;
	}
	
	/*
	 * 创建悬浮窗
	 */
	public void createFloatView(){			
		// 获取LayoutParams对象
		mWinManagerParams = new WindowManager.LayoutParams();
		// 确定爱悬浮窗类型，表示在所有应用程序之上，但在状态栏之下
		mWinManagerParams.type = WindowManager.LayoutParams.TYPE_PHONE;
		mWinManagerParams.format = PixelFormat.RGBA_8888;
		// 确定悬浮窗的行为，表示不可聚焦、非模态对话框
		mWinManagerParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL; 
		// 悬浮窗的对齐方式
		mWinManagerParams.gravity = Gravity.RIGHT | Gravity.TOP;
		// 悬浮窗的位置
		mWinManagerParams.x = mFloatWinMarginRight;
		mWinManagerParams.y = mFloatWinMarginTop;
		mWinManagerParams.width = mFloatWinWidth;
		mWinManagerParams.height = mFloatWinHeight;
		
		
		mFloatLayout = LayoutInflater.from(mContext).inflate(R.layout.rkcloud_av_float_layout, null);
		mFloatBigImageLayout = (RelativeLayout)mFloatLayout.findViewById(R.id.float_bigvideo);
		mFloatSmallImageLayout = (RelativeLayout)mFloatLayout.findViewById(R.id.float_smallvideo);
		
		mWindowManager.addView(mFloatLayout, mWinManagerParams);		
		mFloatBigImageLayout.setOnTouchListener(new OnTouchListener() {			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				if(MotionEvent.ACTION_DOWN == action){
					mStartX = mLastX = (int) event.getRawX();  
					mStartY = mLastY = (int) event.getRawY();  
					
				}else if(MotionEvent.ACTION_UP == action){
					int dx = (int) event.getRawX()-mStartX;
                    int dy = (int) event.getRawY()-mStartY;
                    if(Math.abs(dx)>5 || Math.abs(dy)>5){
                    	return true;
                    }
					
				}else if(MotionEvent.ACTION_MOVE == action){
					int dx = (int) event.getRawX()-mLastX;
                    int dy = (int) event.getRawY()-mLastY;
                    mWinManagerParams.x =  mWinManagerParams.x - dx;  
                    mWinManagerParams.y =  mWinManagerParams.y + dy;  
                    mWindowManager.updateViewLayout(mFloatLayout, mWinManagerParams);
                    mLastX = (int) event.getRawX();  
					mLastY = (int) event.getRawY();
				}
				return false;
			}
		});
		
		mFloatBigImageLayout.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				if(mIsCallActivityShow){
					// 通话页面表示切换大小图的操作
					mFloatDefaultShowSelf = !mFloatDefaultShowSelf;
					controllShowVideos();
				}else{
					if(null != mCurrCallInfo){
						if(mFloatWinClickToEnterCallUi){
							// 表示要进入通话页面
							Intent intent = new Intent(mContext, RKCloudAVDemoActivity.class);
							if(mCurrCallInfo.isCaller){
								intent.putExtra(RKCloudAVDemoActivity.INTENT_KEY_OUTCALL_ACCOUNT, mCurrCallInfo.peerAccount);
							}else{
								intent.putExtra(RKCloudAVDemoActivity.INTENT_KEY_INCALL_ACCOUNT, mCurrCallInfo.peerAccount);
							}
							
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							mContext.startActivity(intent);
							
							mFloatWinClickToEnterCallUi = false;
						}else{
							showToastText(mContext.getString(R.string.rkcloud_av_floatwin_click_frequently));
						}
					}else{
						remoteFloatWin();
					}
				}
			}
		});
	}
	
	/**
	 * 弹出提示
	 * @param msg
	 */
	public void showToastText(String msg){
		Toast toast = Toast.makeText(mContext, msg, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 100);
		toast.show();		
	}
	
	/*
	 * 在通知栏中显示呼出的电话
	 */
	public void showOutCallNotification(String calleeAccount, long time){
		RKCloudAVContact contactObj = mAvContactManager.getContactInfo(calleeAccount);
		String showName = null!=contactObj && null!=contactObj.showName ? contactObj.showName : calleeAccount;
		
		Intent intent = new Intent(mContext, RKCloudAVDemoActivity.class);
		intent.putExtra(RKCloudAVDemoActivity.INTENT_KEY_OUTCALL_ACCOUNT, calleeAccount);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		// 构建通知中使用的信息
		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
		// 通知栏中显示的标题
		builder.setContentTitle(mContext.getString(R.string.rkcloud_av_notification_title_out));
		// 通知栏中显示的内容
		builder.setContentText(showName);
		// 通知栏中显示的icon图标
		builder.setSmallIcon(R.drawable.rkcloud_av_icon_call);
		// 通知栏中右下角显示的小图标
//		builder.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.rkcloud_av_icon_call));		
		// 通知栏中点击后的响应处理
		builder.setContentIntent(pendingIntent);
		// 构建通知对象
		Notification notification = builder.build();
		notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		mNotificationManager.notify(NOTIFICATION_ID_OUT, notification);
	}	
	
	/*
	 * 在通知栏中显示呼入的电话
	 */
	public void showInCallNotification(String callerAccount, long time){
		RKCloudAVContact contactObj = mAvContactManager.getContactInfo(callerAccount);
		String showName = null!=contactObj && null!=contactObj.showName ? contactObj.showName : callerAccount;
		
		Intent intent = new Intent(mContext, RKCloudAVDemoActivity.class);
		intent.putExtra(RKCloudAVDemoActivity.INTENT_KEY_INCALL_ACCOUNT, callerAccount);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		// 构建通知中使用的信息
		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
		// 通知栏中显示的标题
		builder.setContentTitle(mContext.getString(R.string.rkcloud_av_notification_title_in));
		// 通知栏中显示的内容
		builder.setContentText(showName);
		// 通知栏中显示的icon图标
		builder.setSmallIcon(R.drawable.rkcloud_av_icon_call);
		// 通知栏中右下角显示的小图标
//		builder.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.rkcloud_av_icon_call));		
		// 通知栏中点击后的响应处理
		builder.setContentIntent(pendingIntent);
		// 构建通知对象
		Notification notification = builder.build();
		notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		mNotificationManager.notify(NOTIFICATION_ID_IN, notification);
	}
	
	/*
	 * 在通知栏中显示未接电话
	 */
	public void showMissedCallNotification(String callerAccount){
		RKCloudAVContact contactObj = mAvContactManager.getContactInfo(callerAccount);
		String showName = null!=contactObj && null!=contactObj.showName ? contactObj.showName : callerAccount;
		
		Intent intent = new Intent(mContext, RKCloudAVCallLogListActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		// 构建通知中使用的信息
		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
		// 通知栏中显示的标题
		builder.setContentTitle(mContext.getString(R.string.rkcloud_av_notification_title_missed));
		// 通知栏中显示的内容
		builder.setContentText(showName);
		// 通知栏中显示的icon图标
		builder.setSmallIcon(R.drawable.rkcloud_av_icon_call);
		// 通知栏中右下角显示的小图标
//		builder.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.rkcloud_av_icon_call));		
		// 通知栏中点击后的响应处理
		builder.setContentIntent(pendingIntent);
		// 构建通知对象
		Notification notification = builder.build();
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		mNotificationManager.notify(NOTIFICATION_ID_MISSED, notification);
	}
	
	/**
	 * 隐藏通知栏中的呼出电话
	 */
	public void hideOutCallNotification(){
		mNotificationManager.cancel(NOTIFICATION_ID_OUT);
	}
	
	/**
	 * 隐藏通知栏中的新电话
	 */
	public void hideInCallNotification(){
		mNotificationManager.cancel(NOTIFICATION_ID_IN);
	}
	
	/**
	 * 隐藏通知栏中的未接电话
	 */
	public void hideMissedCallNotification(){
		mNotificationManager.cancel(NOTIFICATION_ID_MISSED);
	}
}
