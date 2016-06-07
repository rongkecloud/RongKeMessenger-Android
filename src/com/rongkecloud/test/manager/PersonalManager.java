package com.rongkecloud.test.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentValues;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.rongke.cloud.av.demo.RKCloudAVContactManager;
import com.rongke.cloud.meeting.demo.RKCloudMeetingContactManager;
import com.rongkecloud.chat.demo.RKCloudChatContactManager;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageAsyncLoader;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest.IMAGE_REQUEST_TYPE;
import com.rongkecloud.test.db.dao.PersonalDao;
import com.rongkecloud.test.db.table.PersonInfoColumns;
import com.rongkecloud.test.entity.Constants;
import com.rongkecloud.test.entity.ContactInfo;
import com.rongkecloud.test.http.HttpCallback;
import com.rongkecloud.test.http.HttpResponseCode;
import com.rongkecloud.test.http.HttpTools;
import com.rongkecloud.test.http.HttpType;
import com.rongkecloud.test.http.Progress;
import com.rongkecloud.test.http.Request;
import com.rongkecloud.test.http.Request.RequestType;
import com.rongkecloud.test.http.Result;
import com.rongkecloud.test.manager.uihandlermsg.ContactUiMessage;
import com.rongkecloud.test.system.ConfigKey;
import com.rongkecloud.test.system.RKCloudDemo;

public class PersonalManager{
	
	private static PersonalManager mInstance = null;
	private PersonalDao mPersonalDao;
	private AccountManager mAccountManager;
	private Handler mUiHandler;
	private Map<String, Long> mRecordSyncUserInfos;// 记录同步的用户信息，key为用户账号,value为时间
	private Map<String, Long> mRecordDownThumbs;// key为用户账号， value为时间
	
	private PersonalManager(){
		mPersonalDao = new PersonalDao();
		mAccountManager = AccountManager.getInstance();
		mRecordSyncUserInfos = new HashMap<String, Long>();
		mRecordDownThumbs = new HashMap<String, Long>();
	}
	
	public static PersonalManager getInstance(){
		if(null == mInstance){
			mInstance = new PersonalManager();
		}
		return mInstance;
	}
	
	public void bindUiHandler(Handler handler) {
		mUiHandler = handler;
	}
	
	//////////////////////////////////////////////db操作  begin//////////////////////////////////////////////////	
	//获取当前用户所有好友
	public List<ContactInfo> queryAllFriends(){		
		return mPersonalDao.queryAllFriends();
	}
	
	// 获取对应分组所拥有的好友
	public List<ContactInfo> queryContactFriendsByGroupId(int gid){
		return mPersonalDao.queryFriendsByGroupId(gid);
	}

	//根据用户名获取用户信息
	public ContactInfo getContactInfo(String account){
		return mPersonalDao.getContactInfo(account);
	}
	
	// 批量获取用户信息
	public Map<String, ContactInfo> getContactsInfoByAccounts(List<String> accounts){
		return mPersonalDao.getContactsInfoByAccounts(accounts);
	}
	
	public synchronized boolean batchInsertContactInfos(List<ContactInfo> datas){
		return mPersonalDao.batchInsertContactInfos(datas);
	}
	
	//插入个人信息
	public boolean insertContactInfo(ContentValues cv){
		return mPersonalDao.insertContactInfo(cv);
	}	
		
	//更新个人信息
	public boolean updateContactInfo(String account, ContentValues cv){
		return mPersonalDao.updateContactInfo(account, cv);
	}
	
	// 删除所有用户信息
	public boolean deleteAllDatas(){
		return mPersonalDao.deleteAllDatas();
	}
	
	//////////////////////////////////////////////db操作 end////////////////////////////////////////////////////
	/**
	 * 同步用户信息
	 * @param 
	 */
	public void syncUserInfo(String account){
		long time = mRecordSyncUserInfos.containsKey(account) ? mRecordSyncUserInfos.get(account) : 0;
		if(System.currentTimeMillis()-time < 120000){
			return;
		}
		realSyncUserInfos(account);		
	}
	
	public void syncUserInfoByAccounts(List<String> accounts){
		StringBuffer sb = new StringBuffer();
		for(String account : accounts){
			long time = mRecordSyncUserInfos.containsKey(account) ? mRecordSyncUserInfos.get(account) : 0;
			if(System.currentTimeMillis()-time < 120000){
				continue;
			}
			sb.append(account).append(",");
		}
		if(sb.length() > 0){
			sb.deleteCharAt(sb.length()-1);
			realSyncUserInfos(sb.toString());
		}
	}
	
	/*
	 * 同步用户信息
	 * @param 
	 */
	private void realSyncUserInfos(final String content){
		String session= mAccountManager.getSession();
		if(TextUtils.isEmpty(session)){
			return;
		}		
		
		Request request = new Request(HttpType.SYNC_USERSINFO, RKCloudDemo.kit.getHttpHost(HttpTools.HTTPHOST_TYPE_ROOT), HttpTools.SYNC_USERSINFO_URL);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("ss", session);
		params.put("accounts", content);
		request.params = params;
		request.mHttpCallback = new HttpCallback() {
			@Override
			public void onThreadResponse(Result result) {
				for(String account : content.split(",")){
					mRecordSyncUserInfos.remove(account);
				}

				if(HttpResponseCode.OK == result.opCode){
					List<String> syncAccounts = null;
					try {
						JSONArray jsonArray = new JSONArray(result.values.get("result"));
						if(null==jsonArray || 0==jsonArray.length()){
							return;
						}
						List<ContactInfo> contactFriendsList = new ArrayList<ContactInfo>(jsonArray.length());
						syncAccounts = new ArrayList<String>(jsonArray.length());
						JSONObject jsonObj = null;
						ContactInfo obj = null;
						for (int i = 0; i < jsonArray.length(); i++) {
							jsonObj = new JSONObject(jsonArray.get(i).toString());
							obj = new ContactInfo();
							obj.mAccount = jsonObj.getString("account");
							obj.mRealName = jsonObj.getString("name");
							obj.mAddress = jsonObj.getString("address");
							obj.mUserType = jsonObj.getInt("type");
							obj.mMobile = jsonObj.getString("mobile");
							obj.mEmail = jsonObj.getString("email");
							obj.mSex = jsonObj.getInt("sex");
							obj.mInfoServerVersion = jsonObj.getInt("info_version");
							obj.mAvatarServerVersion = jsonObj.getInt("avatar_version");
							obj.mInfoLastGetTime = System.currentTimeMillis();
							
							syncAccounts.add(obj.mAccount);
							contactFriendsList.add(obj);
						}
						
						mPersonalDao.batchInsertContactInfos(contactFriendsList);
						
						if(null != syncAccounts){
							if(null!=mUiHandler){
								Message msg = mUiHandler.obtainMessage();
								msg.what = ContactUiMessage.SYNC_PERSON_INFOS;
								msg.obj = syncAccounts;
								msg.arg1 = result.opCode;
								msg.sendToTarget();
							}
						}
						
						// 通知rkchat包联系人信息有变化
						RKCloudChatContactManager.getInstance(RKCloudDemo.context).onAccountInfoChanged(syncAccounts);
						// 通知rkav包联系人信息有变化
						RKCloudAVContactManager.getInstance(RKCloudDemo.context).onAccountInfoChanged(syncAccounts);
						// 通知rkmeeting包联系人信息有变化
						RKCloudMeetingContactManager.getInstance(RKCloudDemo.context).onAccountInfoChanged(syncAccounts); 
						
					} catch (Exception e) {
						e.printStackTrace();
					}
				}else{
					if(null!=mUiHandler){
						Message msg = mUiHandler.obtainMessage();
						msg.what = ContactUiMessage.SYNC_PERSON_INFOS;
						msg.arg1 = result.opCode;
						msg.sendToTarget();
					}
				}
			}
			
			@Override
			public void onThreadProgress(Progress progress) {
			}
		};
		RKCloudDemo.kit.execute(request);
		for(String account : content.split(",")){
			mRecordSyncUserInfos.put(account, System.currentTimeMillis());
		}
	}
	
	//获取头像缩略图
	public void getAvatarThumb(final String account, final int serverVersion){
		long time = mRecordDownThumbs.containsKey(account) ? mRecordDownThumbs.get(account) : 0;
		if(System.currentTimeMillis()-time < 120000){
			return;
		}
		
		String session= mAccountManager.getSession();
		if(TextUtils.isEmpty(session)){
			return;
		}
		
		String currLoginAccount = RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "");
		if(!TextUtils.isEmpty(currLoginAccount)){
			currLoginAccount += "/";
		}
		final String filePath = String.format("%savatars/%s%s_%d_thumb", Constants.ROOT_PATH, currLoginAccount, account, serverVersion);
		
		Request request = new Request(HttpType.GET_AVATAR_THUMB, RKCloudDemo.kit.getHttpHost(HttpTools.HTTPHOST_TYPE_ROOT), HttpTools.GET_AVATAR_URL, RequestType.FILE);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("ss", session);
		params.put("account", account);
		params.put("type", "1");
		request.params = params;
		request.filePath = filePath;
		
		request.mHttpCallback = new HttpCallback() {
			@Override
			public void onThreadResponse(Result result) {
				mRecordDownThumbs.remove(account);
				if(HttpResponseCode.OK == result.opCode){
					ContentValues cv = new ContentValues();
					cv.put(PersonInfoColumns.AVATAR_THUMB, filePath);
					cv.put(PersonInfoColumns.AVATAR_CLIENT_THUMB_VERSION, serverVersion);
								
					mPersonalDao.updateContactInfo(account, cv);
					RKCloudChatImageAsyncLoader.getInstance(RKCloudDemo.context).removeImagesByType(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG, account);
					if(null != mUiHandler){
						Message msg = mUiHandler.obtainMessage();
						msg.what = ContactUiMessage.RESPONSE_GET_AVATAR_THUMB;
						msg.obj = account;
						msg.sendToTarget();
					}
					// 通知rkchat包联系人头像有变化
					RKCloudChatContactManager.getInstance(RKCloudDemo.context).onAccountHeaderImageChanged(account);
					// 通知rkav包联系人头像信息有变化
					RKCloudAVContactManager.getInstance(RKCloudDemo.context).onAccountHeaderImageChanged(account);
					// 通知rkmeeting包联系人头像信息有变化
					RKCloudMeetingContactManager.getInstance(RKCloudDemo.context).onAccountHeaderImageChanged(account);
				}
			}
			
			@Override
			public void onThreadProgress(Progress progress) {
			}
		};
		RKCloudDemo.kit.execute(request);
		mRecordDownThumbs.put(account, System.currentTimeMillis());
	}
	
	//获取头像大图
	public void getAvatarBig(final String account, final int serverVersion){
		String session= mAccountManager.getSession();
		if(TextUtils.isEmpty(session)){
			return;
		}
		
		String currLoginAccount = RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "");
		if(!TextUtils.isEmpty(currLoginAccount)){
			currLoginAccount += "/";
		}
		final String filePath = String.format("%savatars/%s%s_%d", Constants.ROOT_PATH, currLoginAccount, account, serverVersion);
				
		Request request = new Request(HttpType.GET_AVATAR_BIG, RKCloudDemo.kit.getHttpHost(HttpTools.HTTPHOST_TYPE_ROOT), HttpTools.GET_AVATAR_URL, RequestType.FILE);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("ss", session);
		params.put("account", account);
		params.put("type", "2");
		request.params = params;
		
		request.filePath = filePath;
		request.mHttpCallback = new HttpCallback() {			
			@Override
			public void onThreadResponse(Result result) {
				if(HttpResponseCode.OK == result.opCode){	
					ContentValues cv = new ContentValues();
					cv.put(PersonInfoColumns.AVATAR_PATH, filePath);
					cv.put(PersonInfoColumns.AVATAR_CLIENT_VERSION, serverVersion);
					mPersonalDao.updateContactInfo(account, cv);
					// 同步个人信息
					syncUserInfo(account);
				}
				if(null != mUiHandler){
					Message msg = mUiHandler.obtainMessage();
					msg.what = ContactUiMessage.RESPONSE_GET_AVATAR_BIG;
					msg.arg1 = result.opCode;
					msg.obj = account;
					msg.sendToTarget();
				}
			}
			
			@Override
			public void onThreadProgress(Progress progress) {
			}
		};
		RKCloudDemo.kit.execute(request);
	}
}