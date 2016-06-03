package com.rongkecloud.test.db.table;

/**
 * 好友通知表
 */
public interface FriendsNotifyColumns{
	/**
	 * 表名
	 */
	public static final String TABLE_NAME = "friends_notify";

	/**
	 * 当前注册用户的account
	 */
	public static final String REG_ACCOUNT = "reg_account";	
	/**
	 * 好友账号
	 */
	public static final String FRIEND_ACCOUNT = "friend_account";
	/**
	 * 请求类别
	 * <p>1--添加好友时需要通过验证
	 * <p>2--加为好友
	 * <p>3--删除好友
	 */
	public static final String TYPE = "type";
	/**
	 * 状态
	 * <p>1--通过验证(type=1时使用)
	 * <p>2--等待验证(type=2时使用)
	 * <p>3--添加好友(type=2时使用)
	 * <p>4--已添加(type=1或2时使用)
	 */
	public static final String STATUS = "status";
	/**
	 * 添加好友时用户填写的请求验证内容
	 */
	public static final String CONTENT = "content";
	/**
	 * 添加好友时手机联系人显示的系统名称
	 */
	public static final String SYS_NAME = "sys_name";
	/**
	 * 通知是否已读
	 * <p>0--未读
	 * <p>1--已读
	 */
	public static final String READ_STATUS = "read_status";

	/**
	 * 定义删除表的SQL语句
	 */
	public static final String SQL_TABLE_DROP = String.format("DROP TABLE IF EXISTS %s;", TABLE_NAME);
	/**
	 * 定义创建表的SQL语句
	 */
	public static final String SQL_TABLE_CREATE = new StringBuilder().append("CREATE TABLE IF NOT EXISTS ").append(TABLE_NAME).append("(")
			.append(REG_ACCOUNT).append(" TEXT NOT NULL COLLATE NOCASE")
			.append(",").append(FRIEND_ACCOUNT).append(" TEXT NOT NULL COLLATE NOCASE")			
			.append(",").append(TYPE).append(" INTEGER")	
			.append(",").append(STATUS).append(" INTEGER")	
			.append(",").append(CONTENT).append(" TEXT")
			.append(",").append(SYS_NAME).append(" TEXT")
			.append(",").append(READ_STATUS).append(" INTEGER DEFAULT 0")
			.append(",").append("PRIMARY KEY(").append(REG_ACCOUNT).append(",").append(FRIEND_ACCOUNT).append(",").append(TYPE).append(")")
			.append(");").toString();	
}
