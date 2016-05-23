package com.rongkecloud.test.manager.uihandlermsg;

/**
 * 圈需要用到的handler消息类型
 */
public class AccountUiMessage {
	private static final int MSG_BASE = 10000;
	
	public static final int RESPONSE_REGISTER = MSG_BASE + 1;//注册
	public static final int RESPONSE_LOGIN = MSG_BASE + 2;//登录
	
	public static final int SDK_INIT_FINISHED =  MSG_BASE + 3;//sdk初始化完成
}
