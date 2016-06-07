package com.rongkecloud.test.manager.uihandlermsg;

/**
 * 设置需要用到的handler消息类型
 */
public class SettingUiMessage {
	private static final int MSG_BASE = 30000;
	
	public static final int RESPONSE_MODIFY_SELFINFO = MSG_BASE + 1;//操作个人信息
	public static final int RESPONSE_UPLOAD_AVATAR = MSG_BASE + 2;//上传头像
	public static final int RESPONSE_MODIFY_PWD = MSG_BASE + 3;//修改密码
	public static final int RESPONSE_ADD_FEEDBACK = MSG_BASE + 4;//添加意见反馈
	public static final int RESPONSE_CHECK_UPDATE = MSG_BASE + 5;//检查更新
	public static final int RESPONSE_DOWNLOAD_APK = MSG_BASE + 6;//下载apk
	public static final int RESPONSE_UPDATE_DOWNLOAD_PROGRESS = MSG_BASE + 7;//下载apk进度
}
