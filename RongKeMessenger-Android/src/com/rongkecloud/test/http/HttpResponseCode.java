package com.rongkecloud.test.http;

/**
 * http响应结果错误码
 */
public class HttpResponseCode{
	public static final int OK                                		= 0;    // 成功	
	public static final int NO_NETWORK                        		= 1;    // 无网络
	public static final int CLIENT_HTTP_RESOLVING_ERROR       		= 2;    // 客户端http解析错误
	public static final int CLIENT_PROTOCOL_ERROR             		= 3;    // 客户端协议错误
	
	public static final int SERVER_SYSTEM_ERROR                     = 9998; // server端系统错误
	public static final int SERVER_PARAM_ERROR                      = 9999; // 参数错误
	public static final int BANNED_USER						    	= 9997;	// 被禁用户
	
	public static final int INVALID_SESSION						    = 1001;	// 无效的session
	
	public static final int ACCOUNT_PWD_ERROR						= 1002;	// 账号密码错误	
	public static final int ACCOUNT_NOT_EXISTS						= 1003;	// 账号不存在
	public static final int ACCOUNT_EXIST                           = 1004; // 账号已存在
	public static final int PWD_ERROR 								= 1005; // 密码错误
	public static final int NO_CHECK_UPDATE 						= 1006; // 没有发现更新
			
	public static final int UPLOAD_IMAGE_ERROR					    = 1010; // 图片上传失败
	public static final int GET_AVATAR_FAIL                         = 1011; // 图像下载失败
	
	public static final int FRIEND_EXIST					    	= 1020; // 好友已存在		
	public static final int ADDFRIEND_NEEDVERIFY                    = 1021; // 添加好友需要验证信息
	public static final int ADDFRIEND_WAITVERIFY 					= 1022; // 添加好友等待对端验证
	public static final int ADDFRIEND_FORBIDYOURSELF 				= 1023; // 添加好友禁止添加自己
	
	public static final int GROPNAME_HASEXIST 						= 1030; // 组名已存在
}