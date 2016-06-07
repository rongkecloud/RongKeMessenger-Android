package com.rongkecloud.test.db.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.rongkecloud.test.db.RKCloudDemoDb;
import com.rongkecloud.test.db.table.ContactFriendsColumns;
import com.rongkecloud.test.db.table.ContactGroupColumns;
import com.rongkecloud.test.entity.ContactGroup;
import com.rongkecloud.test.entity.ContactInfo;
import com.rongkecloud.test.system.ConfigKey;
import com.rongkecloud.test.system.RKCloudDemo;

public class ContactsDao {
	private static final String TAG = ContactsDao.class.getSimpleName();
	
	private SQLiteDatabase mDb;
	
	public ContactsDao(){
		mDb = RKCloudDemoDb.getInstance().getSqlDateBase();		
	}
	
	/**
	 * 读取数据库的ContactGroupColumns.TABLE_NAME表中的所有项目
	 */
	public List<ContactGroup> queryAllGroupInfos() {
		if (null == mDb) {
			return new ArrayList<ContactGroup>();
		}
		
		List<ContactGroup> results = null;
		Cursor c = null;
		try{
			c = mDb.query(ContactGroupColumns.TABLE_NAME, null, String.format("%s=?", ContactGroupColumns.REG_ACCOUNT), new String[]{RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "")}, null, null, null);
			if (null != c && c.getCount() > 0) {
				results = new ArrayList<ContactGroup>(c.getCount());
				ContactGroup obj = null;
				
				boolean needGetIndex = true;
				Map<String, Integer> indexMaps = new HashMap<String, Integer>();
				for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
					if(needGetIndex){
						indexMaps.put(ContactGroupColumns.GROUP_ID, c.getColumnIndex(ContactGroupColumns.GROUP_ID));
						indexMaps.put(ContactGroupColumns.GROUP_NAME, c.getColumnIndex(ContactGroupColumns.GROUP_NAME));
						needGetIndex = false;
					}
					
					obj = new ContactGroup();
					obj.mGroupId = c.getInt(indexMaps.get(ContactGroupColumns.GROUP_ID));
					obj.mGroupName = c.getString(indexMaps.get(ContactGroupColumns.GROUP_NAME));
					results.add(obj);
				}
			}
			
		}catch(Exception e){
			Log.e(TAG, "queryAllGroupInfos--query db error, info="+e.getMessage());
		}finally{
			if(null != c){
				c.close();
			}
		}
		
		if(null == results){
			results = new ArrayList<ContactGroup>();
		}
		
		return results;
	}
	
	/**
	 * 获取对应分组下面的好友账号
	 * @param gid
	 * @return
	 */
	public List<String> queryFriendAccountsByGroupId(int gid) {
		if (null == mDb) {
			return new ArrayList<String>();
		}
		
		String where = String.format("%s=? AND %s=?", ContactFriendsColumns.REG_ACCOUNT, ContactFriendsColumns.GROUP_ID); 
		String[] args = new String[]{RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, ""), String.valueOf(gid)};
		List<String> results = null;
		Cursor c = null;
		try{
			c = mDb.query(ContactFriendsColumns.TABLE_NAME, new String[]{ContactFriendsColumns.FRIEND_ACCOUNT}, where, args, null, null, null);
			if (null != c && c.getCount() > 0) {
				results = new ArrayList<String>(c.getCount());
				for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
					results.add(c.getString(0));
				}
			}
			
		}catch(Exception e){
			Log.e(TAG, "queryFriendAccountsByGroupId--query db error, info="+e.getMessage());
		}finally{
			if(null != c){
				c.close();
			}
		}
		
		if(null == results){
			results = new ArrayList<String>();
		}
		
		return results;
	}

	/**
	 * 根据分组名称读取数据库的ContactGroupColumns.TABLE_NAME表中的所有项目
	 */
	public ContactGroup queryGroupInfo(int groupId) {
		if (null == mDb) {
			return null;
		}
		
		String where = String.format("%s=? AND %s=?", ContactGroupColumns.REG_ACCOUNT, ContactGroupColumns.GROUP_ID);
		String[] args = new String[]{RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, ""), String.valueOf(groupId)};
		
		ContactGroup result = null;
		Cursor c = null;
		try{
			c = mDb.query(ContactGroupColumns.TABLE_NAME, null, where, args, null,null, null, null);
			if (null != c && c.getCount() > 0) {
				c.moveToFirst();
				result = new ContactGroup();
				result.mGroupId = c.getInt(c.getColumnIndex(ContactGroupColumns.GROUP_ID));
				result.mGroupName = c.getString(c.getColumnIndex(ContactGroupColumns.GROUP_NAME));
			}
		}catch(Exception e){
			Log.e(TAG, "queryGroupInfo--query db error, info="+e.getMessage());
		}finally{
			if(null != c){
				c.close();
			}
		}
		
		return result;
	}
	
	
	/**
	 * 根据分组名称读取数据库的ContactGroupColumns.TABLE_NAME表中的所有项目
	 */
	public ContactGroup queryGroupInfo(String gname) {
		if (null == mDb) {
			return null;
		}
		
		String where = String.format("%s=? AND %s=?", ContactGroupColumns.REG_ACCOUNT, ContactGroupColumns.GROUP_NAME);
		String[] args = new String[]{RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, ""), gname};
		
		ContactGroup result = null;
		Cursor c = null;
		try{
			c = mDb.query(ContactGroupColumns.TABLE_NAME, null, where, args, null,null, null, null);
			if (null != c && c.getCount() > 0) {
				c.moveToFirst();
				result = new ContactGroup();
				result.mGroupId = c.getInt(c.getColumnIndex(ContactGroupColumns.GROUP_ID));
				result.mGroupName = c.getString(c.getColumnIndex(ContactGroupColumns.GROUP_NAME));
			}
		}catch(Exception e){
			Log.e(TAG, "queryGroupInfo--query db error, info="+e.getMessage());
		}finally{
			if(null != c){
				c.close();
			}
		}
		
		return result;
	}
	
	/**
	 * 将分组信息批量写入表ContactGroupColumns.TABLE_NAME中
	 */
	public boolean batchInsertGroups(List<ContactGroup> group) {
		if (null==mDb || 0==group.size()) {
			return false;
		}
		String currAccount = RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "");
		
		boolean result = false;
		mDb.beginTransaction();
		try{
			mDb.delete(ContactGroupColumns.TABLE_NAME, String.format("%s=?", ContactGroupColumns.REG_ACCOUNT), new String[]{currAccount});
			for(ContactGroup obj : group){
				// 插入会话信息，存在时忽略
				StringBuffer insertSql = new StringBuffer().append("INSERT INTO ").append(ContactGroupColumns.TABLE_NAME).append("(")
						.append(ContactGroupColumns.REG_ACCOUNT)
						.append(",").append(ContactGroupColumns.GROUP_ID)	
						.append(",").append(ContactGroupColumns.GROUP_NAME)						
						.append(") VALUES(?,?,?);");
				mDb.execSQL(insertSql.toString(), new String[]{currAccount, String.valueOf(obj.mGroupId), obj.mGroupName});
			}
			
			mDb.setTransactionSuccessful();
			
			result = true;
		}catch(Exception e){
			Log.e(TAG, "batchInsertGroups--execute db error, info="+e.getMessage());
		}finally{
			mDb.endTransaction();
		}
		
		return result;
	}
	
	public boolean insertGroup(int groupId, String groupName){
		if (null==mDb) {
			return false;
		}
		ContentValues cv = new ContentValues();
		cv.put(ContactGroupColumns.REG_ACCOUNT, RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, ""));
		cv.put(ContactGroupColumns.GROUP_ID, groupId);
		cv.put(ContactGroupColumns.GROUP_NAME, groupName);
		boolean result = false;
		try {
			if(mDb.insert(ContactGroupColumns.TABLE_NAME, null, cv)>0){
				result = true;
			}
		} catch (Exception e) {
			Log.e(TAG, "insertGroup--execute db error, info="+e.getMessage());
		}
		return result;
	}
	
	/**
	 * 更新数据库ContactGroupColumns.TABLE_NAME表中group_id=oldID的信息
	 */
	public boolean updateGroup(int groupId, String groupName) {
		if (null==mDb || null==groupName){
			return false;
		}
		
		String where = String.format("%s=? AND %s=?", ContactGroupColumns.REG_ACCOUNT, ContactGroupColumns.GROUP_ID);
		String[] args = new String[]{RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, ""), String.valueOf(groupId)};
		
		boolean result = false;
		
		ContentValues cv = new ContentValues();
		cv.put(ContactGroupColumns.GROUP_NAME, groupName);
		
		try {
			if(mDb.update(ContactGroupColumns.TABLE_NAME, cv, where, args)>0){
				result = true;
			}
		} catch (Exception e) {
			Log.e(TAG, "updateGroup--execute db error, info="+e.getMessage());
		}
		return result;
	}
	
	/**
	 * 删除数据库ContactGroupColumns.TABLE_NAME表中的符合group的项目
	 */
	public boolean deleteGroup(int groupId) {
		if(null == mDb){
			return false;
		}
		
		String currAccount = RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "");

		boolean result = false;
		mDb.beginTransaction();
		StringBuffer sb = new StringBuffer().append("UPDATE ").append(ContactFriendsColumns.TABLE_NAME)
				.append(" SET ").append(ContactFriendsColumns.GROUP_ID).append("=0")
				.append(" WHERE ").append(ContactFriendsColumns.REG_ACCOUNT).append("=?")
				.append(" AND ").append(ContactFriendsColumns.GROUP_ID).append("=?;");
		try{
			mDb.execSQL(sb.toString(), new String[]{currAccount, String.valueOf(groupId)});
			mDb.delete(ContactGroupColumns.TABLE_NAME, String.format("%s=? AND %s=?", ContactGroupColumns.REG_ACCOUNT, ContactGroupColumns.GROUP_ID), new String[]{currAccount, String.valueOf(groupId)});
			mDb.setTransactionSuccessful();
			result = true;
		}catch(Exception e){
			Log.e(TAG, "deleteGroup--execute db error, info="+e.getMessage());
		}finally{
			mDb.endTransaction();
		}
		return result;
	}
	
	/**
	 * 读取数据库的ContactGroupColumns.TABLE_NAME表中的所有项目
	 */
	public Map<String, Integer> getGroupIdsByAccounts() {
		if (null == mDb) {
			return new HashMap<String, Integer>();
		}
		
		String where = String.format("%s=?", ContactFriendsColumns.REG_ACCOUNT);
		String[] args = new String[]{RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "")};
		
		Map<String, Integer> results = null;
		Cursor c = null;
		try{
			c = mDb.query(ContactFriendsColumns.TABLE_NAME, new String[]{ContactFriendsColumns.FRIEND_ACCOUNT, ContactFriendsColumns.GROUP_ID}, where, args, null, null, null);
			if (null != c && c.getCount() > 0) {
				results = new HashMap<String, Integer>(c.getCount());				
				for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
					results.put(c.getString(0), c.getInt(1));
				}
			}
			
		}catch(Exception e){
			Log.e(TAG, "getFriendsGroup--query db error, info="+e.getMessage());
		}finally{
			if(null != c){
				c.close();
			}
		}
		
		if(null == results){
			results = new HashMap<String, Integer>();
		}
		
		return results;
	}	
	
	/**
	 * 获取好友所在的分组信息
	 * @return
	 */
	public int queryGroupId(String account){
		if(null==mDb || null==account){
			return -1;
		}
		String where = String.format("%s=? AND %s=?", ContactFriendsColumns.REG_ACCOUNT, ContactFriendsColumns.FRIEND_ACCOUNT);
		String[] args = new String[]{RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, ""), account};
		int groupId = -1;
		Cursor c = null;
		try{
			c = mDb.query(ContactFriendsColumns.TABLE_NAME, new String[]{ContactFriendsColumns.GROUP_ID}, where, args, null, null, null);
			if(null!=c && c.getCount()>0){
				c.moveToFirst();
				groupId = c.getInt(0);
			}
		}catch(Exception e){
			Log.d(TAG, "queryGroupIdByAccount--execute db error, info="+e.getMessage());
			
		}finally{
			if(null != c){
				c.close();
			}
			
		}
		return groupId;
	}	
	
	/**
	 * 批量插入好友信息在表ContactFriendsColumns.TABLE_NAME中
	 */
	public boolean batchInsertFriends(List<ContactInfo> friends) {
		if (null==mDb || null==friends || 0==friends.size()) {
			return false;
		}
		
		String currAccount = RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "");

		boolean result = false;
		mDb.beginTransaction();
		try{
			mDb.delete(ContactFriendsColumns.TABLE_NAME, String.format("%s=?", ContactFriendsColumns.REG_ACCOUNT), new String[]{currAccount});
			for(ContactInfo info : friends){
				ContentValues cv = new ContentValues();
				cv.put(ContactFriendsColumns.REG_ACCOUNT, currAccount);
				cv.put(ContactFriendsColumns.GROUP_ID, info.mGroupId);
				cv.put(ContactFriendsColumns.FRIEND_ACCOUNT, info.mAccount);
				cv.put(ContactFriendsColumns.REMARK, info.mRemark);
				mDb.insert(ContactFriendsColumns.TABLE_NAME, null, cv);
			}
			
			mDb.setTransactionSuccessful();
			result = true;
		}catch(Exception e){
			Log.e(TAG, "batchInsertFriends--execute db error, info="+e.getMessage());
		}finally{
			mDb.endTransaction();
		}
		
		return result;
	}
		
	public boolean insertFriend(int groupId, String account) {
		if(null == mDb){
			return false;
		}
		boolean result = false;
		ContentValues cv = new ContentValues();
		cv.put(ContactFriendsColumns.REG_ACCOUNT, RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, ""));
		cv.put(ContactFriendsColumns.GROUP_ID, groupId);
		cv.put(ContactFriendsColumns.FRIEND_ACCOUNT, account);
		try{
			if(mDb.insert(ContactFriendsColumns.TABLE_NAME, null, cv)>0){
				result = true;
			}
			
		}catch(Exception e){
			Log.e(TAG, "insertGroupFriend--execute db error, info="+e.getMessage());
		}
		return result;
	}
	
	/**
	 * 修改好友备注信息
	 * @param account
	 * @param remark
	 * @return
	 */
	public boolean modifyFriendRemark(String account, String remark){
		if(null == mDb){
			return false;
		}
		
		ContentValues cv = new ContentValues();
		cv.put(ContactFriendsColumns.REMARK, remark);
		boolean result = false;
		try {
			if(mDb.update(ContactFriendsColumns.TABLE_NAME, cv, 
					String.format("%s=? AND %s=?", ContactFriendsColumns.REG_ACCOUNT, ContactFriendsColumns.FRIEND_ACCOUNT)
					, new String[]{RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, ""), account})>0){
				result = true;
			}
		} catch (Exception e) {
			Log.e(TAG, "modifyFriendRemark--execute db error, info="+e.getMessage());
		}
		return result;
	}
	
	/**
	 * 根据好友id，分组id更新数据库ContactFriendsColumns.TABLE_NAME表中的项目
	 */
	public boolean updateGroupIdByAccounts(int groupId, List<String> accounts) {
		if(null == mDb || null==accounts || accounts.size()<=0){
			return false;
		}
		
		String currAccount = RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "");
		
		StringBuffer sql = new StringBuffer().append("UPDATE ").append(ContactFriendsColumns.TABLE_NAME)
				.append(" SET ").append(ContactFriendsColumns.GROUP_ID).append("=").append(groupId)
				.append(" WHERE ").append(ContactFriendsColumns.REG_ACCOUNT).append("=?")
				.append(" AND ").append(ContactFriendsColumns.FRIEND_ACCOUNT).append(" IN(");
		String[] args = new String[accounts.size()+1];
		int index = 0;
		args[index++] = currAccount;
		for(String account : accounts){
			sql.append("?,");
			args[index++] = account;
		}
		sql.deleteCharAt(sql.length()-1).append(");");
		boolean result = false;
		try {
			mDb.execSQL(sql.toString(), args);
			mDb.setTransactionSuccessful();
			result = true;
		} catch (Exception e) {
			Log.e(TAG, "updateGroupIdByAccounts--execute db error, info="+e.getMessage());
		}
		return result;
	}	
	
	/**
	 * 根据好友id删除数据库ContactFriendsColumns.TABLE_NAME表中对应的项目
	 */
	public boolean deleteFriend(String account) {
		if(null == mDb){
			return false;
		}
		
		boolean result = false;
		try{
			if(mDb.delete(ContactFriendsColumns.TABLE_NAME, 
					String.format("%s=? AND %s=?", ContactFriendsColumns.REG_ACCOUNT, ContactFriendsColumns.FRIEND_ACCOUNT), 
					new String[]{RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, ""), account})>0){
				result = true;
			}
		}catch(Exception e){
			Log.e(TAG, "deleteFriend--execute db error, info="+e.getMessage());
		}
		return result;
	}
}
