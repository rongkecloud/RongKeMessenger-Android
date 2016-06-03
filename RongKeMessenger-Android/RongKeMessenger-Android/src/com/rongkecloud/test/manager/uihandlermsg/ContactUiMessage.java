package com.rongkecloud.test.manager.uihandlermsg;

/**
 * 好友需要用到的handler消息类型
 */
public class ContactUiMessage {
	private static final int MSG_BASE = 20000;
				
	public static final int SYNC_GROUP_INFOS = MSG_BASE + 1;// 同步组信息完成 
	public static final int SYNC_FRIEND_INFOS = MSG_BASE + 2;// 同步好友信息完成 	
	
	public static final int SYNC_PERSON_INFOS = MSG_BASE + 10;// 同步用户信息
	public static final int RESPONSE_GET_AVATAR_THUMB = MSG_BASE + 11;// 获取缩略图
	public static final int RESPONSE_GET_AVATAR_BIG = MSG_BASE + 12;// 获取大图
	
	public static final int RESPONSE_OPERATION_GROUP = MSG_BASE + 20;// 操作分组
	
	public static final int RESPONSE_ADDFRIEND = MSG_BASE + 40;//添加好友
	public static final int RESPONSE_MODIFY_FRIENDS_GROUP = MSG_BASE + 41;//修改好友所属分组
	public static final int RESPONSE_MODIFY_FRIENDINFO = MSG_BASE + 42;//修改好友信息
	public static final int RESPONSE_DELFRIEND = MSG_BASE + 43;//删除好友	
	public static final int RESPONSE_CONFIRM_ADDFRIEND = MSG_BASE + 44;// 通过好友验证
	
	public static final int RECEIVED_FRIEND_ADDREQUEST = MSG_BASE + 60;// 收到请求加好友消息
	public static final int RECEIVED_FRIEND_ADDCONFIRM = MSG_BASE + 61;// 收到好友确认通过消息
	public static final int RECEIVED_FRIEND_DELETED = MSG_BASE + 62;// 收到删除好友消息
	
	public static final int CLEAR_FRIEND_NOTIFY_FINISHED = MSG_BASE + 80;// 清空好友通知完成
	
	public static final int SEARCH_CONTACT_INFO = MSG_BASE + 100;//搜索好友

}
