package com.rongkecloud.test.system;

/**
 * 定义所有访问sharedpreference的key
 * @version 1.0.1
 * <P>版本1.0.2中更新内容包括：
 * <br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1. 去除GET_CONTACT_GROUP_KEY和GET_CONTACT_FRIEND_KEY
 * <br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2. 添加SYNC_ALLGROUPS_LASTTIME

 */
public interface ConfigKey {
	/**
	 * 创建快捷方式的标志位
	 * 类型：boolean
	 * 值：true--已创建   false:未创建
	 */
	public static final String KEY_CREATE_SHORTCUT = "key.create.shortcut";	
	/**
	 * sp信息是否升级完成
	 * 类型：boolean
	 * 值：true--完成   false:未完成
	 */
	public static final String UPGRADE_AP = "upgrade.sp";
	
	/**
	 * 当前登录帐户的基本信息
	 * 
	 */
	public static final String LAST_LOGIN_NAME = "last.login.name";// String类型
	public static final String LOGIN_NAME = "login.name";// String类型
	public static final String LOGIN_PWD = "login.pwd";// String类型
	public static final String LOGIN_RKCLOUD_PWD = "login.rkcloud.pwd";// 云视互动密码，String类型
	public static final String LOGIN_ACCOUNT_SESSION = "login.account.session";// String类型
	public static final String LOGIN_USER_TYPE = "login.user.type";// String类型
	public static final String LOGIN_ADD_FRIEND_PERMISSION = "login.add.friend.permission";// String类型
	/**
	 * 获取分组信息记录值：0：未获取，1.已获取
	 */
//	public static final String GET_CONTACT_GROUP_KEY = "get_contact_group_key";
	/**
	 * 获取好友信息记录值：0：未获取，1.已获取
	 */
//	public static final String GET_CONTACT_FRIEND_KEY = "get_contact_friend_key";
	/**
	 * 最后一次检查更新的时间，单位：毫秒级时间戳
	 * 类型：long
	 */
	public static final String UPGRADE_LASTTIME = "upgrade.lasttime";
	/**
	 * 当前点击的主界面tab项
	 * 类型：int
	 */	
	public static final String LAST_SELECTED_TAB = "last_selected_tab";
	/**
	 * 账号是否不存在
	 * 类型：boolean true:不存在  false:存在，默认是存在
	 */
	public static final String ACCOUNT_HAS_NOT_EXIST = "account_has_not_exist";
	/**
	 * 同步组信息的最后一次更新时间，单位：毫秒级时间戳
	 * 类型：long
	 */
	public static final String SYNC_ALLGROUPS_LASTTIME = "sync_allgroups_lasttime";
    /**
     * 是否需要展示引导页的条件，
     * 类型：boolean true：不展示 false：展示，默认是展示
     * 每次引导页图片改变时，key值需要改变
     * 110*38 6PX 17SP BLOD WHITE
     * */
    public static final String SP_GUIDEPAGES_SHOW = "SP_GuidePages_Show";
}
