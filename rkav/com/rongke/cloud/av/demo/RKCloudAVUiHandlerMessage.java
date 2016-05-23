package com.rongke.cloud.av.demo;

public class RKCloudAVUiHandlerMessage {
	
	/////////////////音视频互动使用的what类型值/////////////////////
	public static final int HANDLER_MSG_WHAT_SENSOR = 1;// 距离感应使用的what类型
	public static final int HANDLER_MSG_WHAT_AV = 2;// 音视频通话使用的what类型
	public static final int HANDLER_MSG_WHAT_UPDATETIME = 3;// 音视频通话使用的what类型:更新通话时长


	/////////////////联系人相关的what类型值/////////////////////
	public static final int MSG_WHAT_MEETING_CONTACTSINFO_CHANGED = 11;// 联系人信息有变化
	public static final int MSG_WHAT_MEETING_CONTACT_HEADERIMAGE_CHANGED = 12;// 联系人头像有变化
}
