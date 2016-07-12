package com.rongkecloud.test.http;

interface HttpApi {
    // api对应的ip和端口
//	public static final String ROOT_HOST_NAME = "demo.rongkecloud.com";
//	public static final int ROOT_HOST_PORT = 80;

	/**集测*/
	public static final String ROOT_HOST_NAME = "101.200.143.20";
	public static final int ROOT_HOST_PORT = 8083;
	/**内网*/
//  public static final String ROOT_HOST_NAME = "192.168.1.163";
//  public static final int ROOT_HOST_PORT = 8083;

	public static final String API_PATH = "/rkdemo/";
	// 修改密码
	public static final String MODIFY_PWD_URL = API_PATH + "modify_pwd.php";
	// 注册
	public static final String REGISTER_URL = API_PATH + "register.php";
	// 登录
	public static final String lOGIN_URL = API_PATH + "login.php";
	
	// 操作个人信息
	public static final String OPERATION_PERSONAL_INFO_URL = API_PATH + "operation_personal_info.php";
	// 上传头像信息
	public static final String UPLOAD_PERSONAL_AVATAR_URL = API_PATH + "upload_personal_avatar.php";
	
	// 获取用户分组信息
	public static final String GET_GROUP_INFO_URL = API_PATH + "get_group_infos.php";
	// 获取好友信息
	public static final String GET_FRIENDS_INFO_URL = API_PATH + "get_friend_infos.php";
	
	// 操作分组
	public static final String OPERATIPN_GROUP_URL = API_PATH + "operation_group.php";
		
	// 添加好友
	public static final String ADD_FRIENDS_URL = API_PATH + "add_friend.php";	
	// 确认增加好友
	public static final String CONFIRM_ADD_FRIEND_URL = API_PATH + "confirm_add_friend.php";
	// 删除好友
	public static final String DELETE_FRIEND_URL = API_PATH + "del_friend.php";
	// 修改好友分组
	public static final String MODIFY_FRIENDS_GROUP_URL = API_PATH + "operation_group_members.php";
	// 修改好友信息
	public static final String MODIFY_FRIEND_INFO_URL = API_PATH + "modify_friend_info.php";

	// 同步用户信息
	public static final String SYNC_USERSINFO_URL = API_PATH + "get_personal_infos.php";	
	// 获取头像
	public static final String GET_AVATAR_URL = API_PATH + "get_avatar.php";
	
	// 搜索好友
	public static final String SEARCH_CONTACT_INFO_URL = API_PATH + "search_contact_infos.php";
		
	// 意见反馈
	public static final String ADD_FEEDBACK_URL = API_PATH + "add_feedback.php";
	// 意见反馈
	public static final String CHECK_UPDATE_URL = API_PATH + "check_update.php";
}

