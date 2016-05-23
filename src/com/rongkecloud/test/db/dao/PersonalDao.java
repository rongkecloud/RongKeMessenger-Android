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
import com.rongkecloud.test.db.table.ContactFriendsColumns;
import com.rongkecloud.test.db.table.ContactGroupColumns;
import com.rongkecloud.test.db.table.FriendsNotifyColumns;
import com.rongkecloud.test.db.table.PersonInfoColumns;
import com.rongkecloud.test.entity.ContactInfo;
import com.rongkecloud.test.system.ConfigKey;
import com.rongkecloud.test.system.RKCloudDemo;

public class PersonalDao {
	private static final String TAG = PersonalDao.class.getSimpleName();
	
	private SQLiteDatabase mDb;
	
	public PersonalDao(){
		mDb = RKCloudDemoDb.getInstance().getSqlDateBase();		
	}
	
	public List<ContactInfo> queryAllFriends() {
		if (null == mDb) {
			return new ArrayList<ContactInfo>();
		}
		
		String currAccount = RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "");

		StringBuffer sb = new StringBuffer().append("SELECT a.").append(ContactFriendsColumns.GROUP_ID)
						.append(",a.").append(ContactFriendsColumns.FRIEND_ACCOUNT)
						.append(",a.").append(ContactFriendsColumns.REMARK)
						.append(",b.*")
						.append(" FROM ").append(ContactFriendsColumns.TABLE_NAME).append(" a")
						.append(" LEFT JOIN ").append(PersonInfoColumns.TABLE_NAME).append(" b")
						.append(" ON a.").append(ContactFriendsColumns.FRIEND_ACCOUNT).append("=b.").append(PersonInfoColumns.ACCOUNT)
						.append(" WHERE a.").append(ContactFriendsColumns.REG_ACCOUNT).append("=?")
						.append(" ORDER BY ").append(ContactFriendsColumns.FRIEND_ACCOUNT)
						.append(";");
		
		List<ContactInfo> results = null;
		Cursor c = null;
		try{
			c = mDb.rawQuery(sb.toString(), new String[]{currAccount});
			if(null!=c && c.getCount()>0){
				results = new ArrayList<ContactInfo>(c.getCount());
				ContactInfo obj = null;
				boolean needGetIndex = true;
				Map<String, Integer> indexs = null;
				
				for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
					if(needGetIndex){
						indexs = getContactIndex(c);
						indexs.put(ContactFriendsColumns.GROUP_ID, c.getColumnIndex(ContactFriendsColumns.GROUP_ID));
						indexs.put(ContactFriendsColumns.FRIEND_ACCOUNT, c.getColumnIndex(ContactFriendsColumns.FRIEND_ACCOUNT));
						indexs.put(ContactFriendsColumns.REMARK, c.getColumnIndex(ContactFriendsColumns.REMARK));
						needGetIndex = false;
					}
					
					obj = new ContactInfo();
					obj.mAccount = c.getString(indexs.get(ContactFriendsColumns.FRIEND_ACCOUNT));
					obj.mGroupId = c.getInt(indexs.get(ContactFriendsColumns.GROUP_ID));
					obj.mRemark = c.getString(indexs.get(ContactFriendsColumns.REMARK));
					obj.rkAccount = obj.mAccount;
					
					obj.mRealName = c.getString(indexs.get(PersonInfoColumns.NAME));
					obj.mAddress = c.getString(indexs.get(PersonInfoColumns.ADDRESS));
					obj.mUserType = c.getInt(indexs.get(PersonInfoColumns.USER_TYPE));
					obj.mMobile = c.getString(indexs.get(PersonInfoColumns.MOBILE));
					obj.mSex = c.getInt(indexs.get(PersonInfoColumns.SEX));
					obj.mEmail = c.getString(indexs.get(PersonInfoColumns.EMAIL));
					obj.mPath = c.getString(indexs.get(PersonInfoColumns.AVATAR_PATH));
					obj.mThumbPath = c.getString(indexs.get(PersonInfoColumns.AVATAR_THUMB));
					obj.mInfoLastGetTime = c.getLong(indexs.get(PersonInfoColumns.INFO_SYNC_LASTTIME));
					obj.mInfoClientVersion = c.getInt(indexs.get(PersonInfoColumns.INFO_CLIENT_VERSION));
					obj.mInfoServerVersion = c.getInt(indexs.get(PersonInfoColumns.INFO_SERVER_VERSION));
					obj.mAvatarClientThumbVersion = c.getInt(indexs.get(PersonInfoColumns.AVATAR_CLIENT_THUMB_VERSION));
					obj.mAvatarClientVersion = c.getInt(indexs.get(PersonInfoColumns.AVATAR_CLIENT_VERSION));
					obj.mAvatarServerVersion = c.getInt(indexs.get(PersonInfoColumns.AVATAR_SERVER_VERSION));
					results.add(obj);
				}
			}
			
		}catch(Exception e){
			Log.e(TAG, "queryAllFriends--execute db error, info="+e.getMessage());
		}finally{
			if(null != c){
				c.close();
			}
		}

		if(null == results){
			results = new ArrayList<ContactInfo>();
		}
		return results;
	}
	
	/**
	 * 根据分组id读取数据库的ContactInfoColumns.TABLE_NAME表中的所有项目，并且对应在表ContactInfoColumns.TABLE_NAME中查出详细信息
	 */
	public List<ContactInfo> queryFriendsByGroupId(int groupId) {
		if (null == mDb) {
			return new ArrayList<ContactInfo>();
		}
		
		String currAccount = RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "");

		StringBuffer sb = new StringBuffer().append("SELECT a.").append(ContactFriendsColumns.FRIEND_ACCOUNT)
						.append(",a.").append(ContactFriendsColumns.REMARK)
						.append(",b.*")
						.append(" FROM ").append(ContactFriendsColumns.TABLE_NAME).append(" a")
						.append(" LEFT JOIN ").append(PersonInfoColumns.TABLE_NAME).append(" b")
						.append(" ON a.").append(ContactFriendsColumns.FRIEND_ACCOUNT).append("=b.").append(PersonInfoColumns.ACCOUNT)
						.append(" WHERE a.").append(ContactFriendsColumns.REG_ACCOUNT).append("=?")
						.append(" AND a.").append(ContactFriendsColumns.GROUP_ID).append("=?")
						.append(" ORDER BY ").append(ContactFriendsColumns.FRIEND_ACCOUNT)
						.append(";");
		
		List<ContactInfo> results = null;
		Cursor c = null;
		try{
			c = mDb.rawQuery(sb.toString(), new String[]{currAccount, String.valueOf(groupId)});
			if(null!=c && c.getCount()>0){
				results = new ArrayList<ContactInfo>(c.getCount());
				ContactInfo obj = null;
				boolean needGetIndex = true;
				Map<String, Integer> indexs = null;
				
				for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
					if(needGetIndex){
						indexs = getContactIndex(c);
						indexs.put(ContactFriendsColumns.FRIEND_ACCOUNT, c.getColumnIndex(ContactFriendsColumns.FRIEND_ACCOUNT));
						indexs.put(ContactFriendsColumns.REMARK, c.getColumnIndex(ContactFriendsColumns.REMARK));
						needGetIndex = false;
					}
					
					obj = new ContactInfo();
					obj.mGroupId = groupId;
					obj.mAccount = c.getString(indexs.get(ContactFriendsColumns.FRIEND_ACCOUNT));
					obj.mRemark = c.getString(indexs.get(ContactFriendsColumns.REMARK));
					obj.rkAccount = obj.mAccount;
					
					obj.mRealName = c.getString(indexs.get(PersonInfoColumns.NAME));
					obj.mAddress = c.getString(indexs.get(PersonInfoColumns.ADDRESS));
					obj.mUserType = c.getInt(indexs.get(PersonInfoColumns.USER_TYPE));
					obj.mMobile = c.getString(indexs.get(PersonInfoColumns.MOBILE));
					obj.mSex = c.getInt(indexs.get(PersonInfoColumns.SEX));
					obj.mEmail = c.getString(indexs.get(PersonInfoColumns.EMAIL));
					obj.mPath = c.getString(indexs.get(PersonInfoColumns.AVATAR_PATH));
					obj.mThumbPath = c.getString(indexs.get(PersonInfoColumns.AVATAR_THUMB));
					obj.mInfoLastGetTime = c.getLong(indexs.get(PersonInfoColumns.INFO_SYNC_LASTTIME));
					obj.mInfoClientVersion = c.getInt(indexs.get(PersonInfoColumns.INFO_CLIENT_VERSION));
					obj.mInfoServerVersion = c.getInt(indexs.get(PersonInfoColumns.INFO_SERVER_VERSION));
					obj.mAvatarClientThumbVersion = c.getInt(indexs.get(PersonInfoColumns.AVATAR_CLIENT_THUMB_VERSION));
					obj.mAvatarClientVersion = c.getInt(indexs.get(PersonInfoColumns.AVATAR_CLIENT_VERSION));
					obj.mAvatarServerVersion = c.getInt(indexs.get(PersonInfoColumns.AVATAR_SERVER_VERSION));
					results.add(obj);
				}
			}
			
		}catch(Exception e){
			Log.e(TAG, "queryFriendsByGroupId--execute db error, info="+e.getMessage());
		}finally{
			if(null != c){
				c.close();
			}
		}

		if(null == results){
			results = new ArrayList<ContactInfo>();
		}
	return results;
	}
	
	/**
	 * 根据用户名读取数据库的ContactInfoColumns.TABLE_NAME表中的项目
	 */
	public ContactInfo getContactInfo(String account) {
		if (null == mDb) {
			return new ContactInfo();
		}
		
		StringBuffer sb = new StringBuffer().append("SELECT a.*")
				.append(", b.").append(ContactFriendsColumns.GROUP_ID)
				.append(", b.").append(ContactFriendsColumns.FRIEND_ACCOUNT)
				.append(", b.").append(ContactFriendsColumns.REMARK)
				.append(" FROM ").append(PersonInfoColumns.TABLE_NAME).append(" a")
				.append(" LEFT JOIN ").append(ContactFriendsColumns.TABLE_NAME).append(" b")
				.append(" ON a.").append(PersonInfoColumns.ACCOUNT).append("=b.").append(ContactFriendsColumns.FRIEND_ACCOUNT)
				.append(" AND b.").append(ContactFriendsColumns.REG_ACCOUNT).append("=?")
				.append(" WHERE ").append(PersonInfoColumns.ACCOUNT).append("=?");
		
		ContactInfo result = null;
		Cursor c = null;
		try{
			c = mDb.rawQuery(sb.toString(), new String[]{RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, ""), account});
			if(null!=c && c.getCount()>0){
				c.moveToFirst();
				result = getContactInfo(c, getContactIndex(c));
				if(!TextUtils.isEmpty(c.getString(c.getColumnIndex(ContactFriendsColumns.FRIEND_ACCOUNT)))){
					result.mGroupId = c.getInt(c.getColumnIndex(ContactFriendsColumns.GROUP_ID));
					result.mRemark = c.getString(c.getColumnIndex(ContactFriendsColumns.REMARK));
				}
			}
		}catch(Exception e){
			Log.d(TAG, "getContactInfo--execute db error, info="+e.getMessage());
		}finally{
			if(null != c){
				c.close();
			}
		}

		return result;
	}
	
	/**
	 * 根据用户名读取数据库的ContactInfoColumns.TABLE_NAME表中的项目
	 */
	public Map<String, ContactInfo> getContactsInfoByAccounts(List<String> queryAccounts) {		
		if (null==mDb || null==queryAccounts || 0==queryAccounts.size()) {
			return new HashMap<String, ContactInfo>();
		}
		
		List<String> accounts = new ArrayList<String>();
		accounts.addAll(queryAccounts);
		if(0 == accounts.size()) {
			return new HashMap<String, ContactInfo>();
		}
		
		StringBuffer sb = new StringBuffer().append("SELECT a.*")
				.append(", b.").append(ContactFriendsColumns.GROUP_ID)
				.append(", b.").append(ContactFriendsColumns.FRIEND_ACCOUNT)
				.append(", b.").append(ContactFriendsColumns.REMARK)
				.append(" FROM ").append(PersonInfoColumns.TABLE_NAME).append(" a")
				.append(" LEFT JOIN ").append(ContactFriendsColumns.TABLE_NAME).append(" b")
				.append(" ON a.").append(PersonInfoColumns.ACCOUNT).append("=b.").append(ContactFriendsColumns.FRIEND_ACCOUNT)
				.append(" AND b.").append(ContactFriendsColumns.REG_ACCOUNT).append("=?")
				.append(" WHERE ").append(PersonInfoColumns.ACCOUNT).append(" IN(");
		String[] args = new String[accounts.size()+1];
		int index = 0;
		args[index++] = RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "");
		for(String account : accounts){
			sb.append("?,");
			args[index++] = account;
		}
		sb.deleteCharAt(sb.length()-1).append(");");
		
		Map<String, ContactInfo> results = null;
		Cursor c = null;
		try{
			c = mDb.rawQuery(sb.toString(), args);
			if(null!=c && c.getCount()>0){
				results = new HashMap<String, ContactInfo>(c.getCount());
				boolean needGetIndex = true;
				Map<String, Integer> indexsMap = null;
				ContactInfo obj = null;
				for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
					if(needGetIndex){
						indexsMap = getContactIndex(c);
						indexsMap.put(ContactFriendsColumns.GROUP_ID, c.getColumnIndex(ContactFriendsColumns.GROUP_ID));
						indexsMap.put(ContactFriendsColumns.FRIEND_ACCOUNT, c.getColumnIndex(ContactFriendsColumns.FRIEND_ACCOUNT));
						indexsMap.put(ContactFriendsColumns.REMARK, c.getColumnIndex(ContactFriendsColumns.REMARK));
						needGetIndex = false;
					}
					
					obj = getContactInfo(c, indexsMap);
					if(!TextUtils.isEmpty(c.getString(indexsMap.get(ContactFriendsColumns.FRIEND_ACCOUNT)))){
						obj.mGroupId = c.getInt(indexsMap.get(ContactFriendsColumns.GROUP_ID));
						obj.mRemark = c.getString(indexsMap.get(ContactFriendsColumns.REMARK));
					}
					results.put(obj.mAccount, obj);
				}
			}
		}catch(Exception e){
			Log.d(TAG, "getContactsInfoByAccounts--execute db error, info="+e.getMessage());
		}finally{
			if(null != c){
				c.close();
			}
		}

		if(null == results){
			results = new HashMap<String, ContactInfo>();
		}
		return results;
	}
		
	public boolean batchInsertContactInfos(List<ContactInfo> insertDatas){
		if(null==mDb || null==insertDatas || 0==insertDatas.size()){
			return false;
		}
		
		List<ContactInfo> datas = new ArrayList<ContactInfo>();
		datas.addAll(insertDatas);
		if(0 == datas.size()){
			return false;
		}
		
		boolean result = false;
		// 查询已存在的用户信息
		StringBuffer existSB = new StringBuffer().append("SELECT ").append(PersonInfoColumns.ACCOUNT)
				.append(" FROM ").append(PersonInfoColumns.TABLE_NAME)
				.append(" WHERE ").append(PersonInfoColumns.ACCOUNT).append(" IN(");
		String[] existArgs = new String[datas.size()];
		int index = 0;
		for(ContactInfo data : datas){
			existSB.append("?,");
			existArgs[index++] = data.mAccount;
		}
		existSB.deleteCharAt(existSB.length()-1).append(");");
		Cursor c = null;
		List<String> existAccounts = null;
		try{
			c = mDb.rawQuery(existSB.toString(), existArgs);
			if(null!=c && c.getCount()>0){
				existAccounts = new ArrayList<String>(c.getCount());
				for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
					existAccounts.add(c.getString(0));
				}
			}
		}catch(Exception e){
			
		}finally{
			if(null != c){
				c.close();
			}
		}
		if(null == existAccounts){
			existAccounts = new ArrayList<String>();
		}
		
		mDb.beginTransaction();
		try{
			ContentValues cv = null;
			for(ContactInfo data : datas){
				cv = getContactInfoCV(data);				
				if(existAccounts.contains(data.mAccount)){
					// 更新
					mDb.update(PersonInfoColumns.TABLE_NAME, cv, String.format("%s=?", PersonInfoColumns.ACCOUNT), new String[]{data.mAccount});
				}else{ 
					// 插入
					mDb.insert(PersonInfoColumns.TABLE_NAME, null, cv);
				}
			}
			mDb.setTransactionSuccessful();
			result = true;
			
		}catch(Exception e){
			
		}finally{
			mDb.endTransaction();
		}
		
		return result;
	}
	
	/**
	 * 插入数据库ContactInfoColumns.TABLE_NAME表中user_id=oldID的项目
	 */
	public boolean insertContactInfo(ContentValues cv) {
		if(null==mDb || null==cv){
			return false;
		}
		boolean result = false;
		try{
			if(mDb.insert(PersonInfoColumns.TABLE_NAME, null, cv) > 0){
				result = true;
			}
		}catch(Exception e){
			
		}
		return result;
	}
	
	public boolean updateContactInfo(String account, ContentValues cv){
		if(null==mDb || null==account || null==cv){
			return false;
		}
		boolean result = false;
		try{
			if(mDb.update(PersonInfoColumns.TABLE_NAME, cv, String.format("%s=?", PersonInfoColumns.ACCOUNT), new String[]{account}) > 0){
				result = true;
			}
		}catch(Exception e){
			
		}
		return result;
	}	
	
	/**
	 * 删除数据库ContactInfoColumns.TABLE_NAME表中的所有的项目
	 */
	public boolean deleteAllDatas() {
		if(null == mDb){
			return false;
		}
		boolean result = false;
		mDb.beginTransaction();
		try{
			mDb.delete(FriendsNotifyColumns.TABLE_NAME, null, null);
			mDb.delete(ContactFriendsColumns.TABLE_NAME, null, null);
			mDb.delete(ContactGroupColumns.TABLE_NAME, null, null);
			mDb.delete(PersonInfoColumns.TABLE_NAME, null, null);
			mDb.setTransactionSuccessful();
			result = true;
		}catch(Exception e){
			Log.d(TAG, "deleteAllContactInfo--execute db error, info="+e.getMessage());
		}finally{
			mDb.endTransaction();
		}
		return result;
	}
	
	private ContentValues getContactInfoCV(ContactInfo obj){
		ContentValues cv = new ContentValues();
		if(null != obj.mAccount){
			cv.put(PersonInfoColumns.ACCOUNT, obj.mAccount);
		}
		if(null != obj.mRealName){
			cv.put(PersonInfoColumns.NAME, obj.mRealName);
		}
		if(null != obj.mAddress){
			cv.put(PersonInfoColumns.ADDRESS, obj.mAddress);
		}
		if(obj.mUserType > 0){
			cv.put(PersonInfoColumns.USER_TYPE, obj.mUserType);
		}
		if(null != obj.mMobile){
			cv.put(PersonInfoColumns.MOBILE, obj.mMobile);
		}
		if(null != obj.mEmail){
			cv.put(PersonInfoColumns.EMAIL, obj.mEmail);
		}
		if(obj.mSex > 0){
			cv.put(PersonInfoColumns.SEX, obj.mSex);
		}
		if(null != obj.mPath){
			cv.put(PersonInfoColumns.AVATAR_PATH, obj.mPath);
		}
		if(null != obj.mThumbPath){
			cv.put(PersonInfoColumns.AVATAR_THUMB, obj.mThumbPath);
		}
		if(obj.mInfoLastGetTime > 0){
			cv.put(PersonInfoColumns.INFO_SYNC_LASTTIME, obj.mInfoLastGetTime);
		}
		if(obj.mInfoClientVersion > 0){
			cv.put(PersonInfoColumns.INFO_CLIENT_VERSION, obj.mInfoClientVersion);
		}
		if(obj.mInfoServerVersion > 0){
			cv.put(PersonInfoColumns.INFO_SERVER_VERSION, obj.mInfoServerVersion);
		}
		if(obj.mAvatarClientThumbVersion > 0){
			cv.put(PersonInfoColumns.AVATAR_CLIENT_THUMB_VERSION, obj.mAvatarClientThumbVersion);
		}
		if(obj.mAvatarClientVersion > 0){
			cv.put(PersonInfoColumns.AVATAR_CLIENT_VERSION, obj.mAvatarClientVersion);
		}
		if(obj.mAvatarServerVersion > 0){
			cv.put(PersonInfoColumns.AVATAR_SERVER_VERSION, obj.mAvatarServerVersion);
		}
		return cv;
	}

	private Map<String, Integer> getContactIndex(Cursor c){
		Map<String, Integer> indexs = new HashMap<String, Integer>();
		
		indexs.put(PersonInfoColumns.ACCOUNT, c.getColumnIndex(PersonInfoColumns.ACCOUNT));
		indexs.put(PersonInfoColumns.NAME, c.getColumnIndex(PersonInfoColumns.NAME));
		indexs.put(PersonInfoColumns.ADDRESS, c.getColumnIndex(PersonInfoColumns.ADDRESS));
		indexs.put(PersonInfoColumns.USER_TYPE, c.getColumnIndex(PersonInfoColumns.USER_TYPE));
		indexs.put(PersonInfoColumns.MOBILE, c.getColumnIndex(PersonInfoColumns.MOBILE));
		indexs.put(PersonInfoColumns.SEX, c.getColumnIndex(PersonInfoColumns.SEX));
		indexs.put(PersonInfoColumns.EMAIL, c.getColumnIndex(PersonInfoColumns.EMAIL));
		indexs.put(PersonInfoColumns.AVATAR_PATH, c.getColumnIndex(PersonInfoColumns.AVATAR_PATH));
		indexs.put(PersonInfoColumns.AVATAR_THUMB, c.getColumnIndex(PersonInfoColumns.AVATAR_THUMB));
		indexs.put(PersonInfoColumns.INFO_SYNC_LASTTIME, c.getColumnIndex(PersonInfoColumns.INFO_SYNC_LASTTIME));
		indexs.put(PersonInfoColumns.INFO_CLIENT_VERSION, c.getColumnIndex(PersonInfoColumns.INFO_CLIENT_VERSION));
		indexs.put(PersonInfoColumns.INFO_SERVER_VERSION, c.getColumnIndex(PersonInfoColumns.INFO_SERVER_VERSION));
		indexs.put(PersonInfoColumns.AVATAR_CLIENT_THUMB_VERSION, c.getColumnIndex(PersonInfoColumns.AVATAR_CLIENT_THUMB_VERSION));
		indexs.put(PersonInfoColumns.AVATAR_CLIENT_VERSION, c.getColumnIndex(PersonInfoColumns.AVATAR_CLIENT_VERSION));
		indexs.put(PersonInfoColumns.AVATAR_SERVER_VERSION, c.getColumnIndex(PersonInfoColumns.AVATAR_SERVER_VERSION));
		
		return indexs;
	}
	
	private ContactInfo getContactInfo(Cursor c, Map<String, Integer> indexs){
		ContactInfo obj = new ContactInfo();
		obj.mAccount = c.getString(indexs.get(PersonInfoColumns.ACCOUNT));
		obj.rkAccount = obj.mAccount;
		obj.mRealName = c.getString(indexs.get(PersonInfoColumns.NAME));
		obj.mAddress = c.getString(indexs.get(PersonInfoColumns.ADDRESS));
		obj.mUserType = c.getInt(indexs.get(PersonInfoColumns.USER_TYPE));
		obj.mMobile = c.getString(indexs.get(PersonInfoColumns.MOBILE));
		obj.mSex = c.getInt(indexs.get(PersonInfoColumns.SEX));
		obj.mEmail = c.getString(indexs.get(PersonInfoColumns.EMAIL));
		obj.mPath = c.getString(indexs.get(PersonInfoColumns.AVATAR_PATH));
		obj.mThumbPath = c.getString(indexs.get(PersonInfoColumns.AVATAR_THUMB));
		obj.mInfoLastGetTime = c.getLong(indexs.get(PersonInfoColumns.INFO_SYNC_LASTTIME));
		obj.mInfoClientVersion = c.getInt(indexs.get(PersonInfoColumns.INFO_CLIENT_VERSION));
		obj.mInfoServerVersion = c.getInt(indexs.get(PersonInfoColumns.INFO_SERVER_VERSION));
		obj.mAvatarClientThumbVersion = c.getInt(indexs.get(PersonInfoColumns.AVATAR_CLIENT_THUMB_VERSION));
		obj.mAvatarClientVersion = c.getInt(indexs.get(PersonInfoColumns.AVATAR_CLIENT_VERSION));
		obj.mAvatarServerVersion = c.getInt(indexs.get(PersonInfoColumns.AVATAR_SERVER_VERSION));
		return obj;
	}
}
