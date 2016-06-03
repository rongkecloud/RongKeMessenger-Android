package com.rongke.cloud.meeting.demo;

public class RKCloudMeetingUiHandlerMessage {
	
	/////////////////多人语音发送handler消息时使用的what类型值/////////////////////
	public static final int MSG_WHAT_MEETING_CALLSTATUSCHANGED = 1;// 用户的通话状态有变化
	public static final int MSG_WHAT_MEETING_MEETINGSTATUSGINFO = 2;// 会议状态有变化
	public static final int MSG_WHAT_MEETING_USERINFOSCHANGED = 3;// 参与人员信息有变化
	public static final int MSG_WHAT_MEETING_SYNCMEETINGINFO = 4;// 同步会议信息完成
	public static final int MSG_WHAT_MEETING_UPDATETIME = 5;// 更新通话时长
	

	/////////////////联系人相关的what类型值/////////////////////
	public static final int MSG_WHAT_MEETING_CONTACTSINFO_CHANGED = 11;// 联系人信息有变化
	public static final int MSG_WHAT_MEETING_CONTACT_HEADERIMAGE_CHANGED = 12;// 联系人头像有变化
	
	public static final int HANDLER_MSG_WHAT_SENSOR = 21;// 距离感应使用的what类型
}
