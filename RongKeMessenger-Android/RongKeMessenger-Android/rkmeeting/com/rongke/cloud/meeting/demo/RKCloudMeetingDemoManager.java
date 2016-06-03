package com.rongke.cloud.meeting.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Toast;

import com.rongke.cloud.meeting.demo.entity.RKCloudMeetingUserInfo;
import com.rongke.cloud.meeting.demo.ui.RKCloudMeetingDemoActivity;
import com.rongkecloud.chat.GroupChat;
import com.rongkecloud.chat.LocalMessage;
import com.rongkecloud.chat.RKCloudChatBaseChat;
import com.rongkecloud.chat.RKCloudChatBaseMessage.MSG_STATUS;
import com.rongkecloud.chat.demo.RKCloudChatConstants;
import com.rongkecloud.chat.demo.RKCloudChatMmsManager;
import com.rongkecloud.multiVoice.RKCloudMeeting;
import com.rongkecloud.multiVoice.RKCloudMeetingCallState;
import com.rongkecloud.multiVoice.RKCloudMeetingErrorCode;
import com.rongkecloud.multiVoice.RKCloudMeetingInfo;
import com.rongkecloud.multiVoice.RKCloudMeetingInvitedInfoBean;
import com.rongkecloud.multiVoice.RKCloudMeetingUserBean;
import com.rongkecloud.multiVoice.RKCloudMeetingUserState;
import com.rongkecloud.multiVoice.interfaces.RKCloudMeetingInviteCallBack;
import com.rongkecloud.multiVoice.interfaces.RKCloudMeetingRequestCallBack;
import com.rongkecloud.multiVoice.interfaces.RKCloudMeetingStateCallBack;
import com.rongkecloud.sdkbase.RKCloud;
import com.rongkecloud.test.R;

public class RKCloudMeetingDemoManager implements RKCloudMeetingInviteCallBack{		
	// 通知栏显示的ID类型值
	private static final int NOTIFICATION_MUTLIMEETING_ID = 10;
	
	private static RKCloudMeetingDemoManager mInstance;
	
	private NotificationManager mNotificationManager;	
	private AudioManager mAudioManager;
	private Context mContext;
	private Handler mUiHandler;
	
	private RKCloudMeetingInfo mMeetingInfo;// 会议信息
	private String mGroupId;// 扩展信息，也作为唯一标识使用，此处为即时通信的群ID
	private boolean mCurrHandFree = false;// 是否免提
	private boolean mCurrMute = false;// 是否静音
	private boolean isReceiver = false;//是否是接受者
	private long mStartTime = 0;
	
	private long mLastEnterTime = 0;//记录上次发起多人语音的时间，单位：毫秒
	private long mLastJoinTime = 0;//记录上次加入多人语音的时间，单位：毫秒
	private long mTime = 1 * 60 * 1000;//配置dail 上次点击和当前时间的差值；
	
	private RKCloudMeetingContactManager mRkCloudMeetingContactManager;
	
	private RKCloudMeetingDemoManager(Context context){
		mContext = context.getApplicationContext();
		mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
		mRkCloudMeetingContactManager = RKCloudMeetingContactManager.getInstance(context);
	}
	
	public static RKCloudMeetingDemoManager getInstance(Context context){
		if(null == mInstance){
			mInstance = new RKCloudMeetingDemoManager(context);
		}
		return mInstance;
	}
	
	public void bindUiHandler(Handler uiHandler){
		mUiHandler = uiHandler;
	}
	/**
	 * 转换时间戳
	 * @param content
	 * @return
	 */
	public static String secondConvertToTime(long content){
		long realValue = content;
		if(content == 0){
			return "";
		}
		
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

	@Override
	public void onInviteToMeeting(ArrayList<RKCloudMeetingInvitedInfoBean> invitations) {
//		// 弹出邀请提示框
//		Intent intent = new Intent();
//		intent.setAction(RKCloudMeetingDemoInvitedActivity.INVITED_BROADCAST);
//		intent.putParcelableArrayListExtra(RKCloudMeetingDemoInvitedActivity.INVITED_BROADCAST_DATAS, invitations);
//		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//		mContext.startActivity(intent);
		
		// TODO 以下是与即时通信结合使用的内容
		if(null!=invitations && invitations.size()>0){
			for(RKCloudMeetingInvitedInfoBean info : invitations){
				if(!TextUtils.isEmpty(info.getExtension()) /*&& !info.getExtension().equals(mGroupId)*/){
					LocalMessage msgObj = LocalMessage.buildReceivedMsg(info.getExtension(), mContext.getString(R.string.rkcloud_meeting_startmeeting), info.getInvitorAccount());
					msgObj.setMsgTime(info.getTime());
					msgObj.setStatus(MSG_STATUS.RECEIVE_RECEIVED);
					msgObj.setExtension(RKCloudChatConstants.FLAG_MEETING_MUTLIMEETING+","+info.toJson());
					addLocalMsg(msgObj, true);
				}
			}
		}
	}
	
	/**
	 * 进入多人语音会议室
	 * @param context 
	 * @param groupId 会议室号码，不允许为空，与即时通信的群结合使用，此处为群ID
	 */
	public void enterMutliMeeting(final Context context, final String groupId){
		if(TextUtils.isEmpty(groupId)){
			return;
		}
		if(null == RKCloudMeeting.rkCloudMeetingManager){
			showToastText(context, context.getString(R.string.rkcloud_meeting_sdk_uninit));
			return;
		}
		if(null!=mMeetingInfo && RKCloudMeetingCallState.MEETING_CALL_IDLE!=mMeetingInfo.callState){
			// 有会议进行时如果不是同一个会议则给出提示
			if(!groupId.equals(mGroupId)){
				showToastText(context, context.getString(R.string.rkcloud_meeting_fail_hasmeeting));
				return;
			}
			// 打开UI进入多人语音会议
			Intent intent = new Intent(context, RKCloudMeetingDemoActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
			
		}else{
			if(System.currentTimeMillis() - mLastEnterTime < mTime){
				showToastText(context,context.getString(R.string.rkcloud_meeting_failed_frequent_operation));
				return;
			}
			mLastEnterTime = System.currentTimeMillis();
			// 无会议时直接发起会议		
			RKCloudMeeting.rkCloudMeetingManager.dial(groupId, new RKCloudMeetingStateCallBack(){		
				@Override
				public void onConfStateSYNCallBack() {
					mMeetingInfo = RKCloudMeeting.rkCloudMeetingManager.getMeetingInfo();
					if(null != mUiHandler){
						Message msg = mUiHandler.obtainMessage();
						msg.what = RKCloudMeetingUiHandlerMessage.MSG_WHAT_MEETING_SYNCMEETINGINFO;
						msg.sendToTarget();
					}
				}
	
				@Override
				public void onCallStateCallback(int state, int stateReason) {
					String mmsContent = null;
					switch(state){
					case RKCloudMeetingCallState.MEETING_CALL_RINGBACK:// 等待中
						mMeetingInfo = RKCloudMeeting.rkCloudMeetingManager.getMeetingInfo();
						// 进入多人语音页面
						Intent intent = new Intent(context, RKCloudMeetingDemoActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						context.startActivity(intent);
						// 通知栏显示
						showNotification(groupId); 
						break;
						
					case RKCloudMeetingCallState.MEETING_CALL_ANSWER:// 应答
						mStartTime = System.currentTimeMillis();
						isReceiver = false;
						mMeetingInfo = RKCloudMeeting.rkCloudMeetingManager.getMeetingInfo();
						mmsContent = context.getString(R.string.rkcloud_meeting_startmeeting);
						// 邀请成员
						List<String> groupUsers = RKCloudChatMmsManager.getInstance(context).queryGroupUsers(groupId);// 获取成员
						if(null!=groupUsers && groupUsers.size()>0){
							if(groupUsers.contains(RKCloud.getUserName())){
								groupUsers.remove(RKCloud.getUserName());
							}
							inviteAttendees(context, groupUsers);
						}
						break;
						
					case RKCloudMeetingCallState.MEETING_CALL_HANGUP:// 挂断
						String showContent = context.getString(R.string.rkcloud_meeting_fail);
						switch(stateReason){
						case RKCloudMeetingErrorCode.RK_PARAMS_ERROR:
							showContent = context.getString(R.string.rkcloud_meeting_fail_paramerror);
							break;
						case RKCloudMeetingErrorCode.RK_SDK_UNINIT:
							showContent = context.getString(R.string.rkcloud_meeting_fail_sdkuninit);
							break;
						case RKCloudMeetingErrorCode.RK_NONSUPPORT_MEETING:
							showContent = context.getString(R.string.rkcloud_meeting_fail_unsupport);
							break;
						case RKCloudMeetingErrorCode.RK_EXIST_MEETINGING:
							showContent = context.getString(R.string.rkcloud_meeting_fail_hasmeeting);
							break;
						case RKCloudMeetingErrorCode.RK_EXIST_AV_CALLING:
							showContent = context.getString(R.string.rkcloud_meeting_fail_hasavcall);
							break;
						case RKCloudMeetingErrorCode.MEETING_CONF_DIAL_TIMEOUT:
							showContent = context.getString(R.string.rkcloud_meeting_fail_dail_timeout);
							break;
						case RKCloudMeetingErrorCode.RK_SUCCESS:
							if(!TextUtils.isEmpty(getMeetingProgressTime())){
								showContent = context.getString(R.string.rkcloud_meeting_user_out_time, context.getString(R.string.rkcloud_meeting_me),getMeetingProgressTime());
							}else{
								showContent = context.getString(R.string.rkcloud_meeting_hangup);
							}
							
							mmsContent = context.getString(R.string.rkcloud_meeting_user_out, context.getString(R.string.rkcloud_meeting_me));
							break;
						}
						showToastText(context, showContent);
						if(RKCloudMeetingErrorCode.RK_EXIST_MEETINGING != stateReason){
							switchToIdle();
						}
						break;
					}
					if(!TextUtils.isEmpty(mmsContent)){
						// TODO 向聊天页面写入消息
						LocalMessage msgObj = LocalMessage.buildSendMsg(groupId, mmsContent, RKCloud.getUserName());
						msgObj.setExtension(RKCloudChatConstants.FLAG_LOCAL_TIPMESSAGE);
						addLocalMsg(msgObj, false);
					}
					if(null != mUiHandler){
						Message msg = mUiHandler.obtainMessage();
						msg.what = RKCloudMeetingUiHandlerMessage.MSG_WHAT_MEETING_CALLSTATUSCHANGED;
						msg.arg1 = state;
						msg.arg2 = stateReason;
						msg.obj = groupId;
						msg.sendToTarget();
					}
				}

				@Override
				public void onConfMemberStateChangeCallBack(String userName, int state) {
					processUserChanged(userName, state);
				}

				@Override
				public void onConfStateChangeCallBack() {
					if(null != mUiHandler){
						mUiHandler.sendEmptyMessage(RKCloudMeetingUiHandlerMessage.MSG_WHAT_MEETING_MEETINGSTATUSGINFO);
					}
				}
			});
			
			// 修改状态值
			mGroupId = groupId;
		}
	}
	
	/**
	 * 邀请成员
	 * @param context Context对象
	 * @param attendees String列表对象  被邀请者账号
	 * @param extension 扩展参数，此处可以为即时通信的群Id
	 */
	public void inviteAttendees(final Context context, List<String> attendees){
		// 会议未接通时邀请失败
		if(null==mMeetingInfo || RKCloudMeetingCallState.MEETING_CALL_ANSWER!=mMeetingInfo.callState
				|| null==attendees || attendees.size()<=0){
			return;
		}
		RKCloudMeeting.rkCloudMeetingManager.inviteAttendees(attendees, mGroupId, new RKCloudMeetingRequestCallBack(){
			@Override
			public void onFailed(int errorCode) {
				switch(errorCode){
				case RKCloudMeetingErrorCode.RK_PARAMS_ERROR:
					showToastText(context, context.getString(R.string.rkcloud_meeting_invitefailed_paramerror));
					break;
					
				case RKCloudMeetingErrorCode.MEETING_CONF_CANNOT_INVITE_OWN:
					showToastText(context, context.getString(R.string.rkcloud_meeting_invitefailed_forbidyourself));
					break;
					
				case RKCloudMeetingErrorCode.MEETING_CONF_NOT_EXIST:
					showToastText(context, context.getString(R.string.rkcloud_meeting_invitefailed_unhavemeeting));
					break;
					
				case RKCloudMeetingErrorCode.RK_INVALID_USER:
					showToastText(context, context.getString(R.string.rkcloud_meeting_invitefailed_illegalerreceiver));
					break;
					
				default:
					showToastText(context, context.getString(R.string.rkcloud_meeting_invitefailed));
					break;
				}
			}

			@Override
			public void onSuccess(Object arg0) {
			}
		});
	}		
	
	/**
	 * 加入多人语音会议
	 */
	public void joinMeeting(final Context context, RKCloudMeetingInvitedInfoBean invitedInfo){
		if(null==RKCloudMeeting.rkCloudMeetingManager){
			showToastText(context, context.getString(R.string.rkcloud_meeting_sdk_uninit));
			return;
		}
		if(null == invitedInfo){
			showToastText(context, context.getString(R.string.rkcloud_meeting_fail_paramerror));
			return;
		}
		// 解析扩展参数
		final String groupId = invitedInfo.getExtension();
		if(null == groupId){
			showToastText(context, context.getString(R.string.rkcloud_meeting_fail_paramerror));
			return;
		}
		// 有会议进行时，除非进的是同一个群，否则发起失败
		if(null!=mMeetingInfo && RKCloudMeetingCallState.MEETING_CALL_IDLE!=mMeetingInfo.callState){
			if(mGroupId.equals(groupId)){
				// 直接进入会议室
				Intent intent = new Intent(context, RKCloudMeetingDemoActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent);
			}else{
				showToastText(context, context.getString(R.string.rkcloud_meeting_fail_hasmeeting));
			}
		}else{
			if(System.currentTimeMillis() - mLastJoinTime < mTime){
				showToastText(context,context.getString(R.string.rkcloud_meeting_failed_frequent_operation));
				return;
			}
			mLastJoinTime = System.currentTimeMillis();
			// 接受邀请操作
			RKCloudMeeting.rkCloudMeetingManager.joinMeeting(invitedInfo, new RKCloudMeetingStateCallBack(){
				@Override
				public void onConfStateChangeCallBack() {
				}
				
				@Override
				public void onConfStateSYNCallBack() {
					mMeetingInfo = RKCloudMeeting.rkCloudMeetingManager.getMeetingInfo();
					if(null != mUiHandler){
						Message msg = mUiHandler.obtainMessage();
						msg.what = RKCloudMeetingUiHandlerMessage.MSG_WHAT_MEETING_SYNCMEETINGINFO;
						msg.sendToTarget();
					}
					// 同步参与者信息
					Map<String, RKCloudMeetingUserBean> attendees = RKCloudMeeting.rkCloudMeetingManager.getAttendeeInfos();	
					if(null!=attendees && attendees.size()>0){
						List<String> accounts = new ArrayList<String>(attendees.size());
						for(String account : attendees.keySet()){
							if(account.equalsIgnoreCase(RKCloud.getUserName())){
								accounts.add(account);
							}
						}
						mRkCloudMeetingContactManager.syncContactsInfo(accounts);
					}
				}
	
				@Override
				public void onCallStateCallback(int state, int stateReason) {
					String mmsContent = null;
					switch(state){
					case RKCloudMeetingCallState.MEETING_CALL_INVITING:// 加入中
						mMeetingInfo = RKCloudMeeting.rkCloudMeetingManager.getMeetingInfo();
						// 进入多人语音页面
						Intent intent = new Intent(context, RKCloudMeetingDemoActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						context.startActivity(intent);
						// 通知栏显示
						showNotification(groupId);
						break;
						
					case RKCloudMeetingCallState.MEETING_CALL_ANSWER:// 已应答
						isReceiver = true;
						mStartTime = System.currentTimeMillis();
						mMeetingInfo = RKCloudMeeting.rkCloudMeetingManager.getMeetingInfo();
//						mmsContent = context.getString(R.string.rkcloud_meeting_user_in, context.getString(R.string.rkcloud_meeting_me));
						break;
						
					case RKCloudMeetingCallState.MEETING_CALL_HANGUP://挂断
						String showContent = context.getString(R.string.rkcloud_meeting_fail);
						switch(stateReason){
						case RKCloudMeetingErrorCode.RK_PARAMS_ERROR:
							showContent = context.getString(R.string.rkcloud_meeting_fail_paramerror);
							break;
						case RKCloudMeetingErrorCode.RK_SDK_UNINIT:
							showContent = context.getString(R.string.rkcloud_meeting_fail_sdkuninit);
							break;	
						case RKCloudMeetingErrorCode.RK_EXIST_MEETINGING:
							showContent = context.getString(R.string.rkcloud_meeting_fail_hasmeeting);
							break;	
						case RKCloudMeetingErrorCode.RK_EXIST_AV_CALLING:
							showContent = context.getString(R.string.rkcloud_meeting_fail_hasavcall);
							break;	
						case RKCloudMeetingErrorCode.MEETING_CONF_DIAL_TIMEOUT:
							showContent = context.getString(R.string.rkcloud_meeting_fail_dail_timeout);
							break; 
						case RKCloudMeetingErrorCode.RK_SUCCESS:
							if(!TextUtils.isEmpty(getMeetingProgressTime())){
								showContent = context.getString(R.string.rkcloud_meeting_user_out_time, context.getString(R.string.rkcloud_meeting_me),getMeetingProgressTime());
							}else{
								showContent = context.getString(R.string.rkcloud_meeting_hangup);
							}
							mmsContent = context.getString(R.string.rkcloud_meeting_user_out, context.getString(R.string.rkcloud_meeting_me));
							break;
						}
						showToastText(context, showContent);
						if(RKCloudMeetingErrorCode.RK_EXIST_MEETINGING != stateReason){
							switchToIdle();
						}
						break;
					}
					
					if(!TextUtils.isEmpty(mmsContent)){
						// TODO 向聊天页面写入消息
						LocalMessage msgObj = LocalMessage.buildSendMsg(groupId, mmsContent, RKCloud.getUserName());
						msgObj.setExtension(RKCloudChatConstants.FLAG_LOCAL_TIPMESSAGE);
						addLocalMsg(msgObj, false);
					}
					if(null != mUiHandler){
						Message msg = mUiHandler.obtainMessage();
						msg.what = RKCloudMeetingUiHandlerMessage.MSG_WHAT_MEETING_CALLSTATUSCHANGED;
						msg.arg1 = state;
						msg.arg2 = stateReason;
						msg.obj = groupId;
						msg.sendToTarget();
					}
				}

				@Override
				public void onConfMemberStateChangeCallBack(String userName, int state) {
					processUserChanged(userName, state);
				}
			});
			
			// 修改状态值
			mGroupId = groupId;
		}
	}
	
	/**
	 * 退出时清除通知栏图标
	 */
	public void logout(){
		mNotificationManager.cancel(NOTIFICATION_MUTLIMEETING_ID);
	}
	
	public void handup(){
		if(null != RKCloudMeeting.rkCloudMeetingManager){
			RKCloudMeeting.rkCloudMeetingManager.hangup();
		}
	}
	
	public void mute(boolean isMute){
		if(null != RKCloudMeeting.rkCloudMeetingManager){
			RKCloudMeeting.rkCloudMeetingManager.mute(isMute);
		}
	}
	
	public boolean isCurrMute(){
		return mCurrMute;
	}
	
	/**
	 * 是否开启免提
	 * @param handFree true:免提  false:关闭免提
	 */
	public void handFree(boolean handFree){		
		mAudioManager.setMode(AudioManager.ROUTE_SPEAKER);
		mAudioManager.setSpeakerphoneOn(handFree);
		mCurrHandFree = handFree;
	}
	
	public boolean isCurrMeetingHandFree(){
		return mCurrHandFree;
	}
	
	/*
	 * 处理用户信息有变化的操作
	 */
	private synchronized void processUserChanged(String account, int state){
		if(RKCloudMeetingUserState.MEETING_USER_STATE_IN==state 
				|| RKCloudMeetingUserState.MEETING_USER_STATE_OUT==state){
			String showName = null;
			if(account.equalsIgnoreCase(RKCloud.getUserName())){
				showName = mContext.getString(R.string.rkcloud_meeting_me);
			}else{
				RKCloudMeetingUserInfo userBean = mRkCloudMeetingContactManager.getContactInfo(account);
				showName = null!=userBean ? userBean.getShowName() : account;
			}
			
			String content = null;
			if(RKCloudMeetingUserState.MEETING_USER_STATE_IN == state){
				if(account.equalsIgnoreCase(RKCloud.getUserName())){
					if(isReceiver){
						content = mContext.getString(R.string.rkcloud_meeting_user_in, showName);
					}
				}else{
					content = mContext.getString(R.string.rkcloud_meeting_user_in, showName);
				}
				
			}else{
				content = mContext.getString(R.string.rkcloud_meeting_user_out, showName);
			}
			
			// TODO 向聊天页面写入消息
			if(!TextUtils.isEmpty(content)){
				LocalMessage msgObj = LocalMessage.buildSendMsg(mGroupId, content, RKCloud.getUserName());
				msgObj.setExtension(RKCloudChatConstants.FLAG_LOCAL_TIPMESSAGE);
				addLocalMsg(msgObj, false);
			}
			// 同步用户信息
			if(RKCloudMeetingUserState.MEETING_USER_STATE_IN==state && !account.equalsIgnoreCase(RKCloud.getUserName())){
				mRkCloudMeetingContactManager.syncContactInfo(account);
			}
			
		}else if(RKCloudMeetingUserState.MEETING_USER_STATE_MUTE==state 
				|| RKCloudMeetingUserState.MEETING_USER_STATE_UNMUTE==state){
			if(account.equalsIgnoreCase(RKCloud.getUserName())){
				mCurrMute = state== RKCloudMeetingUserState.MEETING_USER_STATE_MUTE;
			}
		}
		if(null != mUiHandler){
			Message msg = mUiHandler.obtainMessage();
			msg.what = RKCloudMeetingUiHandlerMessage.MSG_WHAT_MEETING_USERINFOSCHANGED;
			msg.arg1 = state;
			msg.obj = account;
			msg.sendToTarget();
		}
	}
	
	/**
	 * 获取会议进行时间
	 */
	public String  getMeetingProgressTime(){
		if(null == mMeetingInfo){
			return "";
		}
		if(RKCloudMeetingCallState.MEETING_CALL_IDLE == mMeetingInfo.callState){
			return "";
		}
		
		if(0 == mStartTime){
			return "";
		}
		return secondConvertToTime((System.currentTimeMillis()-mStartTime)/1000);
	}
	
	/**
	 * 获取会议信息
	 * @return
	 */
	public RKCloudMeetingInfo getMeetingInfo(){
		return mMeetingInfo;
	}

	/**
	 * 获取多人语音会议的扩展信息
	 * @return
	 */
	public String getMeetingExtensionInfo(){
		return mGroupId;
	}
	
	/**
	 * 获取会议参与人员
	 * @return
	 */
	public Map<String, RKCloudMeetingUserInfo> getMeetingMembers(){
		if(null == RKCloudMeeting.rkCloudMeetingManager){
			return new HashMap<String, RKCloudMeetingUserInfo>();
		}
		Map<String, RKCloudMeetingUserInfo> returnDatas = new HashMap<String, RKCloudMeetingUserInfo>();
		
		Map<String, RKCloudMeetingUserBean> attendees = RKCloudMeeting.rkCloudMeetingManager.getAttendeeInfos();	
		if(null!=attendees && attendees.size()>0){
			// 先获取本地联系人的信息
			List<String> accounts = new ArrayList<String>(attendees.size());
			for(String account : attendees.keySet()){
				accounts.add(account);
			}
			if(accounts.size() > 0){
				Map<String, RKCloudMeetingUserInfo> localContactDatas = mRkCloudMeetingContactManager.getContactInfos(accounts);
				
				RKCloudMeetingUserInfo userInfo = null;
				RKCloudMeetingUserBean obj = null;
				RKCloudMeetingUserInfo localObj = null;
				for(String account : attendees.keySet()){
					obj = attendees.get(account);
					localObj = localContactDatas.get(account);
					
					userInfo = new RKCloudMeetingUserInfo();
					userInfo.setAttendeeAccount(account);
					userInfo.setMute(obj.isMute());
					
					if(null != localObj){
						userInfo.showName = localObj.showName;
						userInfo.avatarPath = localObj.avatarPath;
					}
					
					returnDatas.put(userInfo.getAttendeeAccount(), userInfo);
				}
			}
		}
		
		return returnDatas;
	}
	
	/*
	 * 结束多人会议之后转换为空闲状态
	 */
	private void switchToIdle(){
		mMeetingInfo = null;
		mGroupId = null;
		mCurrMute = false;
		mCurrHandFree = false;
		mStartTime = 0;
		cancelNotification();
		mLastEnterTime = 0;
		mLastJoinTime = 0;
	}
	
	/**
	 * 弹出提示
	 * @param context
	 * @param msg
	 */
	public void showToastText(Context context, String msg){
		Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 100);
		toast.show();		
	}
	
	/*
	 * 在通知栏中显示图标
	 */
	private void showNotification(String chatId){
		Intent intent = new Intent(mContext, RKCloudMeetingDemoActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		// 构建通知中使用的信息
		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
		// 通知栏中显示的标题
		builder.setContentTitle(mContext.getString(R.string.rkcloud_meeting_notification_title));
		// 通知栏中显示的内容
		builder.setContentText(mContext.getString(R.string.rkcloud_meeting_notification_content));
		// 通知栏中显示的icon图标
		builder.setSmallIcon(R.drawable.rkcloud_meeting_icon_mutlimeeting);
		// 通知栏中右下角显示的小图标
//		builder.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.rkcloud_meeting_icon_mutlimeeting));		
		// 通知栏中点击后的响应处理
		builder.setContentIntent(pendingIntent);
		// 构建通知对象
		Notification notification = builder.build();
		notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		mNotificationManager.notify(NOTIFICATION_MUTLIMEETING_ID, notification);
	}
	
	/**
	 * 取消通知栏提示
	 */
	public void cancelNotification(){
		mNotificationManager.cancel(NOTIFICATION_MUTLIMEETING_ID);
	}
	
	private void addLocalMsg(LocalMessage msgObj, boolean isNotify){
		RKCloudChatMmsManager mmsManager = RKCloudChatMmsManager.getInstance(mContext);
		RKCloudChatBaseChat chatObj = mmsManager.queryChat(msgObj.getChatId());
		if(null!=chatObj && chatObj instanceof GroupChat){
			if(mmsManager.addLocalMsg(msgObj, GroupChat.class) > 0){
				// 向通知栏中发送消息
				if(isNotify){
					mmsManager.onReceivedMsg(mmsManager.queryChat(msgObj.getChatId()), msgObj);
				}
			}
		}
	}
}
