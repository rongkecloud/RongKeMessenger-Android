package com.rongkecloud.test.db.table;

/**
 * 分组表
 */
public interface ContactGroupColumns {
	/**
	 * 表名
	 */
	public static final String TABLE_NAME = "contact_groups";
	/**
	 * 当前用户账号
	 */
	public static final String REG_ACCOUNT = "reg_account";
	
	/**
	 * 分组id
	 */
	public static final String GROUP_ID = "group_id";

	/**
	 * 分组名
	 */
	public static final String GROUP_NAME = "group_name";

	/**
	 * 定义删除表的SQL语句
	 */
	public static final String SQL_TABLE_DROP = String.format("DROP TABLE IF EXISTS %s;", TABLE_NAME);

	/**
	 * 定义创建表的SQL语句
	 */
	public static final String SQL_TABLE_CREATE = new StringBuilder().append("CREATE TABLE IF NOT EXISTS ").append(TABLE_NAME).append("(")
			.append(REG_ACCOUNT).append(" TEXT NOT NULL COLLATE NOCASE")
			.append(",").append(GROUP_ID).append(" INTEGER NOT NULL")
			.append(",").append(GROUP_NAME).append(" TEXT NOT NULL")
			.append(",").append("PRIMARY KEY(").append(REG_ACCOUNT).append(",").append(GROUP_ID).append(")")
			.append(");").toString();

}