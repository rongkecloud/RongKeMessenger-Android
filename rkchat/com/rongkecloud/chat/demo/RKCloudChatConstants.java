package com.rongkecloud.chat.demo;

import com.rongkecloud.chat.demo.tools.RKCloudChatTools;

/**
 * 会话消息使用的常量值定义
 * @author jessica.yang
 */
public class RKCloudChatConstants {		
	public static final String ROOT_PATH = String.format("%s%s", RKCloudChatTools.getSDCardRootDirectory().getAbsolutePath(), "/com.rongkecloud.sdk/");// 根目录
	public static final String MMS_TEMP_PATH = String.format("%stemp/", ROOT_PATH);// 媒体消息临时存放目录
	
	// 图片默认的宽度和高度
	public static final int IMAGE_DEFAULT_WIDTH = 800;
	public static final int IMAGE_DEFAULT_HEIGHT = 800;	
	
	public static final int LOAD_MSG_DEFAULT_COUNT = 20;// 聊天页面默认加载消息的条数
	public static final int MSG_TIMETIP_INTERVAL = 120000;// 聊天页面时间分段的间隔时间，默认2分钟，单位：毫秒
	
	public static final String FLAG_AVCALL_IS_VIDEO = "call_video";// 音视频通话-视频通话
	public static final String FLAG_AVCALL_IS_AUDIO = "call_audio";// 音视频通话-语音通话
	public static final String FLAG_MEETING_MUTLIMEETING = "meeting_mutlimeeting";// 多人语音标识
	public static final String FLAG_LOCAL_TIPMESSAGE = "local_tipmsg";// 本地提示类型的消息标识
	public static final String FLAG_ADD_FRIEND_SUCCESS = "flag_addfriend_success";// 添加好友成功的标识

	public static final String KEY_GROUP_ALL = "all";//@所有成员的key
}
