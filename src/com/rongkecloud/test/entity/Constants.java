package com.rongkecloud.test.entity;

import com.rongkecloud.test.utility.SDCardUtil;

/**
 * 常量值定义
 */
public interface Constants {
	public static final String SUPPORT_QQ_GROUP = "389706118";
	public static final String SUPPORT_BUSINESS_EMAIL = "service@rongkecloud.com";
	public static final String SUPPORT_TECHNOLOGY_EMAIL = "support@rongkecloud.com";
	public static final String SUPPORT_WEBSITE = "www.rongkecloud.com";
	
	public static final String ROOT_PATH = String.format("%s%s", SDCardUtil.getESDString(), "/com.rongkecloud.demo/");// 根目录
	public static final String LOG_PATH = String.format("%slog/", ROOT_PATH);// 日志目录
	public static final String CRASH_PATH = String.format("%scrash/", ROOT_PATH);// 记录异常信息的目录
	public static final String UPGRADE_APK_PATH = String.format("%s%s", ROOT_PATH, "融科通.apk");
	public static final String TEMP_PATH = String.format("%stemp/", ROOT_PATH);//临时目录
	// 日志过期时间，默认为10天
	public static final int LOG_EXPIRED_TIME = 10;
	// 获取个人信息间隔时间，1分钟
	public static final int GET_PERSONAL_INFO_TIME = 1*60*1000;
	// 定期检查的时间，默认是24小时
	public static final long CHECK_TIME_INTERVAL = 24*3600*1000;	
	// 同步组信息的时间间隔，默认是30min
	public static final long SYNC_ALLGROUPINFOS_TIME_INTERVAL = 1*3600*1000/2;	
	
	public static final int USER_TYPE_ENTERPRISE = 2;//企业用户
	public static final int USER_TYPE_NORMAL = 1;//普通用户
	
	//设置头像压缩尺寸
	public static final int SETTING_AVATAR_WIDTH = 640;//长
	public static final int SETTING_AVATAR_HEIGHT = 480;//宽
	
	//性别
	public static final int SEX_MAN = 1;
	public static final int SEX_WOMAN = 2;	
	
	//操作分组
	public static final int OPERATION_GROUP_ADD = 1;//添加分组
	public static final int OPERATION_GROUP_MODIFY = 2;//修改分组
	public static final int OPERATION_GROUP_DELETE = 3;//删除分组
	
	//消息类型
	public static final String MESSGE_TYPE_ADD_REQUEST = "add_request";//添加好友请求
	public static final String MESSGE_TYPE_ADD_CONRIFM = "add_confirm";//确认添加好友
	public static final String MESSGE_TYPE_DELETE_FRIEND = "delete_friend";//删除好友
	
	// 好友通知类型
	public static final int FRIEND_NOTIFY_TYPE_VERIFY = 1;// 添加为好友时通过验证
	public static final int FRIEND_NOTIFY_TYPE_ADD = 2;// 添加好友
	
	// 好友通知状态
	public static final int FRIEND_NOTIFY_STATUS_VERIFY = 1;// type=1通过验证
	public static final int FRIEND_NOTIFY_STATUS_WAITVERIFY = 2;// type=2等待验证
	public static final int FRIEND_NOTIFY_STATUS_ADD = 3;// type=2添加好友
	public static final int FRIEND_NOTIFY_STATUS_HAS_FRIEND = 4;// type=1或2 已添加
	
	// 好友通知是否已读
	public static final int FRIEND_NOTIFY_READ_STATUS_YES = 1;// 已读
	public static final int FRIEND_NOTIFY_READ_STATUS_NO = 0;// 未读
	
	// 缩略图的宽度和高度
	public static final int AVATAR_THUMB_WIDTH = 100;
	public static final int AVATAR_THUMB_HEIGHT = 100;
	
	//加好友时是否需要验证
	public static final int ADD_FRIEND_PREMISSION_YES = 1;//需要验证
	public static final int ADD_FRIEND_PREMISSION_NO = 2;//不需要验证
	
	public static final String DOWNLOAD_APK = "download_apk";//下载apk
	
	//确认加好友类型
	public static final String ADD_FRIEND_TYPE_ISACTIVITED = "isActivited";//主动点击按钮确认
	public static final String ADD_FRIEND_TYPE_ISNOTACTIVITED = "isNotActivited";//不需要验证确认

	//选择铃声
	public static final int SOUND_SYSTEM = 1;//系统铃声
	public static final int SOUND_CUSTOM1 = 2;//自定义铃声1
}
