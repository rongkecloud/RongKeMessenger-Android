package com.rongkecloud.test.entity;

import com.rongkecloud.test.db.table.FriendsNotifyColumns;

public class FriendNotify{
	/**
	 * @see FriendsNotifyColumns#FRIEND_ACCOUNT
	 */
	public String account;
	/**
	 * @see FriendsNotifyColumns#TYPE
	 */
	public String type = "";
	/**
	 * @see FriendsNotifyColumns#STATUS
	 */
	public int status = -1;
	
	/**
	 * @see FriendsNotifyColumns#CONTENT
	 */
	public String content;
	/**
	 * @see FriendsNotifyColumns#SYS_NAME
	 */
	public String sysName;
	/**
	 * @see FriendsNotifyColumns#READ_STATUS
	 */
	public int readStatus;
	/**
	 * 个人的基本信息
	 */
	public ContactInfo contactObj;
	
	public String getDisplayName(){
		if(null != contactObj){
			return contactObj.getShowName();
		}
		return account;
	}
}
