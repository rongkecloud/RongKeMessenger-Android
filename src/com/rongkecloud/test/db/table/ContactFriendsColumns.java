package com.rongkecloud.test.db.table;

/**
 * 分组表
 */
public interface ContactFriendsColumns {
	/**
	 * 表名
	 */
	public static final String TABLE_NAME = "contact_friends";
	/**
	 * 当前用户账号
	 */
	public static final String REG_ACCOUNT = "reg_account";
	
	/**
	 * 好友账号
	 */
	public static final String FRIEND_ACCOUNT = "friend_account";
	
	/**
	 * 分组id
	 */
	public static final String GROUP_ID = "group_id";
	/**
	 * 备注
	 */
	public static final String REMARK = "remark";
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
			.append(",").append(GROUP_ID).append(" INTEGER NOT NULL")
			.append(",").append(REMARK).append(" TEXT")
			.append(",").append("PRIMARY KEY(").append(REG_ACCOUNT).append(",").append(FRIEND_ACCOUNT).append(")")
			.append(");").toString();

}