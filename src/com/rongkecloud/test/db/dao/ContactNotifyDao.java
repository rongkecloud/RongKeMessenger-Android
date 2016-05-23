package com.rongkecloud.test.db.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.rongkecloud.test.db.RKCloudDemoDb;
import com.rongkecloud.test.db.table.FriendsNotifyColumns;
import com.rongkecloud.test.entity.Constants;
import com.rongkecloud.test.entity.ContactInfo;
import com.rongkecloud.test.entity.FriendNotify;
import com.rongkecloud.test.system.ConfigKey;
import com.rongkecloud.test.system.RKCloudDemo;
import com.rongkecloud.test.utility.Print;

public class ContactNotifyDao {
private static final String TAG = ContactNotifyDao.class.getSimpleName();
	
	private SQLiteDatabase mDb;
	private PersonalDao mPersonDao;
	
	public ContactNotifyDao(){
		mDb = RKCloudDemoDb.getInstance().getSqlDateBase();		
		mPersonDao = new PersonalDao();
	}
	/**
	 * 获取所有的好友通知
	 * @param
	 * @return
	 */
	public List<FriendNotify> queryAllNotifys() {
		Cursor c = null;
		List<String> recordAccounts = null;
		List<FriendNotify> returnData = null;
		try {
			c = mDb.query(FriendsNotifyColumns.TABLE_NAME, null, String.format("%s=?", FriendsNotifyColumns.REG_ACCOUNT), new String[]{RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "")}, null, null, String.format("%s ASC", FriendsNotifyColumns.STATUS));
			if (null != c && c.getCount() > 0) {
				returnData = new ArrayList<FriendNotify>(c.getCount());
				recordAccounts = new ArrayList<String>(c.getCount());
				FriendNotify obj = null;
				boolean needGetIndex = true;
				Map<String, Integer> indexMaps = new HashMap<String, Integer>();
				for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
					if(needGetIndex){
						indexMaps.put(FriendsNotifyColumns.REG_ACCOUNT, c.getColumnIndex(FriendsNotifyColumns.REG_ACCOUNT));
						indexMaps.put(FriendsNotifyColumns.FRIEND_ACCOUNT, c.getColumnIndex(FriendsNotifyColumns.FRIEND_ACCOUNT));
						indexMaps.put(FriendsNotifyColumns.TYPE, c.getColumnIndex(FriendsNotifyColumns.TYPE));
						indexMaps.put(FriendsNotifyColumns.STATUS, c.getColumnIndex(FriendsNotifyColumns.STATUS));
						indexMaps.put(FriendsNotifyColumns.CONTENT, c.getColumnIndex(FriendsNotifyColumns.CONTENT));
						indexMaps.put(FriendsNotifyColumns.SYS_NAME, c.getColumnIndex(FriendsNotifyColumns.SYS_NAME));
						indexMaps.put(FriendsNotifyColumns.READ_STATUS, c.getColumnIndex(FriendsNotifyColumns.READ_STATUS));
						needGetIndex = false;
					}
					obj = new FriendNotify();
					obj.account = c.getString(indexMaps.get(FriendsNotifyColumns.FRIEND_ACCOUNT));
					obj.type = c.getString(indexMaps.get(FriendsNotifyColumns.TYPE));
					obj.status = c.getInt(indexMaps.get(FriendsNotifyColumns.STATUS));
					obj.content = c.getString(indexMaps.get(FriendsNotifyColumns.CONTENT));
					obj.sysName = c.getString(indexMaps.get(FriendsNotifyColumns.SYS_NAME));

					returnData.add(obj);
					if(!recordAccounts.contains(obj.account)){
						recordAccounts.add(obj.account);
					}
				}
				
				// 返回数据，并且过滤掉无个人信息的记录
				Map<String, ContactInfo> perInfos = mPersonDao.getContactsInfoByAccounts(recordAccounts);
				for(FriendNotify notifyObj : returnData){
					notifyObj.contactObj = perInfos.get(notifyObj.account);
				}
			}
		} catch (Exception e) {
			Print.w(TAG,"queryAllNotifys -- execute db error, info=" + e.getMessage());
		} finally {
			if (null != c) {
				c.close();
			}
		}
		
		if(null == returnData){
			returnData = new ArrayList<FriendNotify>();
		}

		return returnData;
	}

	/**
	 * 获取好友通知未读条数
	 * @param 
	 * @return
	 */
	public int getNotityCnt() {
		StringBuffer sb = new StringBuffer().append("SELECT count(*)")
				.append(" FROM ").append(FriendsNotifyColumns.TABLE_NAME)
				.append(" WHERE ").append(FriendsNotifyColumns.REG_ACCOUNT).append("=?")
				.append(" AND ").append(FriendsNotifyColumns.READ_STATUS).append("=").append(Constants.FRIEND_NOTIFY_READ_STATUS_NO)
				.append(";");
		Cursor c = null;
		int count = 0;
		try {
			c = mDb.rawQuery(sb.toString(), new String[]{RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "")});
			if (null != c && c.getCount() > 0) {
				c.moveToFirst();
				count = c.getInt(0);
			}
		} catch (Exception e) {
			Print.w(TAG,"getNotityCnt -- execute db error, info=" + e.getMessage());
		} finally {
			if (null != c) {
				c.close();
			}
		}

		return count;
	}
	
	/**
	 * 添加通知
	 * @param 
	 * @return
	 */
	public boolean addFriendNotify(FriendNotify data){
		if(null==mDb || null==data){
			return false;
		}
		ContentValues cv = new ContentValues();
		cv.put(FriendsNotifyColumns.REG_ACCOUNT, RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, ""));
		cv.put(FriendsNotifyColumns.FRIEND_ACCOUNT, data.account);
		cv.put(FriendsNotifyColumns.TYPE, data.type);
		cv.put(FriendsNotifyColumns.STATUS, data.status);
		cv.put(FriendsNotifyColumns.READ_STATUS, data.readStatus);
		if(!TextUtils.isEmpty(data.content)){
			cv.put(FriendsNotifyColumns.CONTENT, data.content);
		}
		if(!TextUtils.isEmpty(data.sysName)){
			cv.put(FriendsNotifyColumns.SYS_NAME, data.sysName);
		}
		boolean result = false;
		try{
			if(mDb.replace(FriendsNotifyColumns.TABLE_NAME, null, cv) > 0){
				result = true;
			}
		}catch(Exception e){
			Print.w(TAG,"addFriendNotify -- execute db error, info="+ e.getMessage());
		}
		return result;
	}
	
	/**
	 * 更新好友通知为已读状态
	 * @return
	 */
	public synchronized long updateNotifyReaded() {
		String where = String.format("%s=? AND %s=?",FriendsNotifyColumns.REG_ACCOUNT, FriendsNotifyColumns.READ_STATUS);
		String[] args = new String[]{RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, ""), String.valueOf(Constants.FRIEND_NOTIFY_READ_STATUS_NO)};
		
		ContentValues cv = new ContentValues();
		cv.put(FriendsNotifyColumns.READ_STATUS, Constants.FRIEND_NOTIFY_READ_STATUS_YES);
		long result = 0;
		try {
			result = mDb.update(FriendsNotifyColumns.TABLE_NAME, cv, where, args);
		} catch (Exception e) {
			Print.w(TAG, "updateNotifyReadStatus -- execute db error, info=" + e.getMessage());
		}
		return result;
	}
	
	/**
	 * 删除好友通知信息
	 * @param  
	 * @return
	 */
	public long delFriendsNotify(String account, String type) {
		if(null==mDb || null==account){
			return -1;
		}
		String where = String.format("%s=? AND %s=? AND %s=?",FriendsNotifyColumns.REG_ACCOUNT,FriendsNotifyColumns.FRIEND_ACCOUNT, FriendsNotifyColumns.TYPE);
		String[] args = new String[]{RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, ""), account, type};
		long result = 0;
		try {
			result = mDb.delete(FriendsNotifyColumns.TABLE_NAME, where, args);
		} catch (Exception e) {
			Log.w(TAG,"delFriendsNotify -- execute db error, info=" + e.getMessage());
			result = -1;
		}
		return result;
	}
	
	/**
	 * 删除好友通知信息
	 * @param  
	 * @return
	 */
	public long delFriendsNotify(String account) {
		if(null==mDb || null==account){
			return -1;
		}
		String where = String.format("%s=? AND %s=?",FriendsNotifyColumns.REG_ACCOUNT,FriendsNotifyColumns.FRIEND_ACCOUNT);
		String[] args = new String[]{RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, ""), account};
		long result = 0;
		try {
			result = mDb.delete(FriendsNotifyColumns.TABLE_NAME, where, args);
		} catch (Exception e) {
			Log.w(TAG,"delFriendsNotify -- execute db error, info=" + e.getMessage());
			result = -1;
		}
		return result;
	}
	
	/**
	 * 清空好友通知信息
	 * @param  
	 * @return
	 */
	public long delAllFriendsNotify() {
		if(null==mDb){
			return -1;
		}
		String where = String.format("%s=?",FriendsNotifyColumns.REG_ACCOUNT);
		String[] args = new String[]{RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "")};
		long result = 0;
		try {
			result = mDb.delete(FriendsNotifyColumns.TABLE_NAME, where, args);
		} catch (Exception e) {
			Log.w(TAG,"delFriendsNotify -- execute db error, info=" + e.getMessage());
			result = -1;
		}
		return result;
	}
	
	/**
	 * 更新通知
	 * @param data
	 * @return
	 */
	public boolean updateFriendNotify(FriendNotify data){
		if(null == mDb){
			return false;
		}
		String currAccount = RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "");
		String where = String.format("%s=? AND %s=?", FriendsNotifyColumns.REG_ACCOUNT, FriendsNotifyColumns.FRIEND_ACCOUNT);
		String[] args = new String[]{currAccount, String.valueOf(data.account)};
		ContentValues cv = new ContentValues();
		if(data.status != -1){
			cv.put(FriendsNotifyColumns.STATUS, data.status);
		}
		
		if(!TextUtils.isEmpty(data.content)){
			cv.put(FriendsNotifyColumns.CONTENT, data.content);
		}

		boolean result = true;
		try{
			mDb.update(FriendsNotifyColumns.TABLE_NAME, cv, where, args);
		}catch(Exception e){
			result = false;
			Print.w(TAG,"updateFriendNotify -- execute db error, info="+ e.getMessage());
		}
		return result;
	}
}
