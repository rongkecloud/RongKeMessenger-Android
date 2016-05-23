package com.rongkecloud.test.db.table;

/**
 * 个人信息表
 */
public interface PersonInfoColumns {
	/**
	 * 表名
	 */
	public static final String TABLE_NAME = "user_info";
	/**
	 * 用户名
	 */
	public static final String ACCOUNT = "account";
	/**
	 * 姓名
	 */
	public static final String NAME = "name";
	/**
	 * 住址
	 */
	public static final String ADDRESS = "address";
	/**
	 * 用户类型
	 */
	public static final String USER_TYPE = "user_type";
	/**
	 * 性别
	 */
	public static final String SEX = "sex";	
	/**
	 * 手机号码
	 */
	public static final String MOBILE = "mobile";
	/**
	 * 邮箱
	 */
	public static final String EMAIL = "email";
	/**
	 * 头像大图路径
	 */
	public static final String AVATAR_PATH = "avatar_path";
	/**
	 * 头像缩略图路径
	 */
	public static final String AVATAR_THUMB = "avatar_thumb";	
	/**
	 * 上次更新时间
	 */
	public static final String INFO_SYNC_LASTTIME = "info_sync_lasttime";
	/**
	 * 用户信息客户端版本号
	 */
	public static final String INFO_CLIENT_VERSION = "info_client_version";
	/**
	 * 用户信息服务器版本号
	 */
	public static final String INFO_SERVER_VERSION = "info_server_version";	
	/**
	 * 头像缩略图版本号
	 */
	public static final String AVATAR_CLIENT_THUMB_VERSION = "avatar_client_thumb_version";
	/**
	 * 头像大图版本号
	 */
	public static final String AVATAR_CLIENT_VERSION = "avatar_client_version";
	/**
	 * 头像服务器版本号
	 */
	public static final String AVATAR_SERVER_VERSION = "avatar_server_version";
	/**
	 * 定义删除表的SQL语句
	 */
	public static final String SQL_TABLE_DROP = String.format("DROP TABLE IF EXISTS %s;", TABLE_NAME);

	/**
	 * 定义创建表的SQL语句
	 */
	public static final String SQL_TABLE_CREATE = new StringBuilder().append("CREATE TABLE IF NOT EXISTS ").append(TABLE_NAME).append("(")
			.append(ACCOUNT).append(" TEXT COLLATE NOCASE PRIMARY KEY")
			.append(",").append(NAME).append(" TEXT")
			.append(",").append(ADDRESS).append(" TEXT")
			.append(",").append(USER_TYPE).append(" INTEGER")
			.append(",").append(MOBILE).append(" TEXT")
			.append(",").append(SEX).append(" INTEGER")
			.append(",").append(EMAIL).append(" TEXT COLLATE NOCASE")
			.append(",").append(AVATAR_PATH).append(" TEXT")
			.append(",").append(AVATAR_THUMB).append(" TEXT")
			.append(",").append(INFO_SYNC_LASTTIME).append(" INTEGER DEFAULT 0")
			.append(",").append(INFO_CLIENT_VERSION).append(" INTEGER DEFAULT 0")
			.append(",").append(INFO_SERVER_VERSION).append(" INTEGER DEFAULT 0")
			.append(",").append(AVATAR_CLIENT_THUMB_VERSION).append(" INTEGER DEFAULT 0")
			.append(",").append(AVATAR_CLIENT_VERSION).append(" INTEGER DEFAULT 0")
			.append(",").append(AVATAR_SERVER_VERSION).append(" INTEGER DEFAULT 0")
			.append(");").toString();

}