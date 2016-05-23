package com.rongkecloud.test.http;

import java.util.concurrent.atomic.AtomicInteger;

public class HttpType{
	public static AtomicInteger base = new AtomicInteger(0);
	
	public static final int MODIFY_PWD = base.getAndIncrement();// 修改密码
	public static final int REGISTER = base.getAndIncrement();//注册
	public static final int lOGIN = base.getAndIncrement();// 登录
	
	public static final int OPERATION_PERSONAL_INFO = base.getAndIncrement();//操作个人信息
	public static final int UPLOAD_PERSONAL_AVATAR = base.getAndIncrement();//上传头像
	
	public static final int GET_GROUP_INFO = base.getAndIncrement();//获取分组信息
	public static final int GET_FRIENDS_INFO = base.getAndIncrement();//获取分组好友信息
	
	public static final int OPERATION_GROUPS = base.getAndIncrement();//操作分组
	
	public static final int ADD_FRIENDS = base.getAndIncrement();//添加好友
	public static final int CONFIRM_ADD_FRIEND = base.getAndIncrement();//确认添加好友
	public static final int DELETE_FRIEND = base.getAndIncrement();// 删除好友
	public static final int MODIFY_FRIENDS_GROUP = base.getAndIncrement();// 修改好友分组
	public static final int MODIFY_FRIEND_INFO = base.getAndIncrement();//修改好友信息
	
	public static final int SYNC_USERSINFO = base.getAndIncrement();// 同步用户信息
	public static final int GET_AVATAR_THUMB = base.getAndIncrement();//获取头像缩略图
	public static final int GET_AVATAR_BIG = base.getAndIncrement();//获取头像大图
	
	public static final int SEARCH_CONTACT_INFO = base.getAndIncrement();//搜索好友
	
	public static final int ADD_FEEDBACK = base.getAndIncrement();//添加意见反馈
	public static final int CHECK_UPDATE = base.getAndIncrement();// 检查更新
	public static final int DOWNLOAD_APK = base.getAndIncrement();// 下载apk
}
