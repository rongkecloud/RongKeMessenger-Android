package com.rongkecloud.chat.demo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.rongkecloud.chat.demo.entity.RKCloudChatContact;
import com.rongkecloud.chat.demo.tools.RKCloudChatSDCardTools;
import com.rongkecloud.chat.interfaces.RKCloudChatContactCallBack;
import com.rongkecloud.test.entity.Constants;
import com.rongkecloud.test.entity.ContactInfo;
import com.rongkecloud.test.manager.PersonalManager;
import com.rongkecloud.test.ui.contact.ContactDetailInfoActivity;
import com.rongkecloud.test.utility.SDCardUtil;

/**
 * Demo中使用的通讯录，需要应用App自己完善用户信息
 */
public class RKCloudChatContactManager implements RKCloudChatContactCallBack{
	private static RKCloudChatContactManager mInstance;
	private Context mContext;
	private Handler mUiHandler;
	
	private RKCloudChatContactManager(Context context){
		mContext = context;
	}
	
	public static RKCloudChatContactManager getInstance(Context context){
		if(null == mInstance){
			mInstance = new RKCloudChatContactManager(context);
		}
		return mInstance;
	}
	
	public void bindUiHandler(Handler handler){
		mUiHandler = handler;
	}
	
	/**
	 * 账户信息有变化
	 * @param accounts
	 */
	public void onAccountInfoChanged(List<String> accounts){
		if(null!=mUiHandler && null!=accounts && accounts.size()>0){
			List<String> lowerAccounts = new ArrayList<String>(accounts.size());
			for(String acc : accounts){
				lowerAccounts.add(acc);
			}
			Message msg = mUiHandler.obtainMessage();
			msg.what = RKCloudChatUiHandlerMessage.CONTACTSINFO_CHANGED;
			msg.obj = lowerAccounts;
			msg.sendToTarget();
		}
	}
	
	/**
	 * 账户的头像有变化
	 * @param account
	 */
	public void onAccountHeaderImageChanged(String account){
		if(null!=mUiHandler && !TextUtils.isEmpty(account)){
			Message msg = mUiHandler.obtainMessage();
			msg.what = RKCloudChatUiHandlerMessage.CONTACT_HEADERIMAGE_CHANGED;
			msg.obj = account.toLowerCase();
			msg.sendToTarget();
		}
	}	
	
	/**
	 * 同步用户信息
	 * @param rkAccounts
	 */
	public void syncContactInfo(String rkAccount){
		// TODO 完善用户同步操作
		ContactInfo info = PersonalManager.getInstance().getContactInfo(rkAccount);
		if(null==info || System.currentTimeMillis() - info.mInfoLastGetTime >= Constants.GET_PERSONAL_INFO_TIME){
			PersonalManager.getInstance().syncUserInfo(rkAccount);
		}	
	}
	
	/**
	 * 同步用户信息
	 * @param rkAccounts
	 */
	public void syncContactsInfo(List<String> rkAccounts){
		// TODO 完善用户同步操作
		List<String> realSyncAccounts = new ArrayList<String>(rkAccounts.size());
		Map<String, ContactInfo> contacts = PersonalManager.getInstance().getContactsInfoByAccounts(rkAccounts);
		for(String account : rkAccounts){
			ContactInfo contact = contacts.get(account);
			if(null==contact || System.currentTimeMillis() - contact.mInfoLastGetTime >= Constants.GET_PERSONAL_INFO_TIME){
				realSyncAccounts.add(account);
			}
		}
		
		PersonalManager.getInstance().syncUserInfoByAccounts(realSyncAccounts);
	}
	
	/**
	 * 获取所有联系人信息，此处需要关联应用App自己的用户数据
	 * @return
	 */
	public List<RKCloudChatContact> getAllContacts(){
		List<RKCloudChatContact> datas = new ArrayList<RKCloudChatContact>();
		// TODO 完善通讯录内容
		List<ContactInfo> friends = PersonalManager.getInstance().queryAllFriends();
		if(null!=friends && friends.size()>0){
			boolean sdAvailable = SDCardUtil.diskSpaceAvailable();
			for(ContactInfo obj : friends){
				if(obj.mAvatarServerVersion>0 && sdAvailable){
					if(TextUtils.isEmpty(obj.mThumbPath) 
							|| !new File(obj.mThumbPath).exists() 
							|| obj.mAvatarServerVersion>obj.mAvatarClientThumbVersion){
						PersonalManager.getInstance().getAvatarThumb(obj.mAccount, obj.mAvatarServerVersion);
					}
				}
				datas.add(obj);
			}
		}
		return datas;
	}
	
	/**
	 * 通过云视互动账号获取对应App中的用户信息
	 * @param rkAccount
	 * @return
	 */
	public RKCloudChatContact getContactInfo(String rkAccount){
		// TODO 完善内容
		return PersonalManager.getInstance().getContactInfo(rkAccount);
	}
	
	/**
	 * 通过云视互动账号获取对应App中的用户信息，其中key为云视互动账号，value为对应的联系人对象
	 * @param rkAccounts
	 * @return
	 */
	public Map<String, RKCloudChatContact> getContactInfos(List<String> rkAccounts){
		Map<String, RKCloudChatContact> datas = new HashMap<String, RKCloudChatContact>();
		// TODO 完善内容
		Map<String, ContactInfo> contacts = PersonalManager.getInstance().getContactsInfoByAccounts(rkAccounts);
		if(null!=contacts && 0 != contacts.size()){
			boolean sdAvailable = RKCloudChatSDCardTools.diskSpaceAvailable();
			for(ContactInfo obj : contacts.values()){
				// 下载头像的缩略图
				if(obj.mAvatarServerVersion>0 && sdAvailable){
					if(TextUtils.isEmpty(obj.mThumbPath) 
							|| !new File(obj.mThumbPath).exists() 
							|| obj.mAvatarServerVersion>obj.mAvatarClientThumbVersion){
						PersonalManager.getInstance().getAvatarThumb(obj.mAccount, obj.mAvatarServerVersion);
					}
				}
				
				datas.put(obj.rkAccount.toLowerCase(), obj);
			}
		}
		return datas;
	}
	
	/**
	 * 跳转到用户详情页面
	 */
	public void jumpContactDetailInfoUI(Context context, String rkAccount){
		// TODO 引用好友详情的activity
		Intent intent = new Intent(context, ContactDetailInfoActivity.class);
		intent.putExtra(ContactDetailInfoActivity.INTENT_CONTACT_ACCOUNT, rkAccount);
		context.startActivity(intent);
	}
	
	@Override
	public Map<String, String> getContactNames(List<String> rkAccounts) {
		Map<String, String> names = new HashMap<String, String>();		
		// TODO 完善显示名称的获取
		Map<String, ContactInfo> contacts = PersonalManager.getInstance().getContactsInfoByAccounts(rkAccounts);
		if(null!=contacts && contacts.size()>0){
			for(ContactInfo obj : contacts.values()){
				names.put(obj.rkAccount, obj.getShowName());
			}
		}
		return names;
	}

	@Override
	public String getContactName(String rkAccount) {
		// TODO 完善显示名称的获取
		ContactInfo contactObj = PersonalManager.getInstance().getContactInfo(rkAccount);
		if(null != contactObj){
			return contactObj.getShowName();
		}
		return rkAccount;
	}

	@Override
	public Map<String, String> getContactHeaderPhotos(List<String> rkAccounts) {
		Map<String, String> headers = new HashMap<String, String>();
		// TODO 完善头像的获取
		Map<String, ContactInfo> contacts = PersonalManager.getInstance().getContactsInfoByAccounts(rkAccounts);
		if(null!=contacts && contacts.size()>0){
			for(ContactInfo obj : contacts.values()){
				if(!TextUtils.isEmpty(obj.mThumbPath)){
					headers.put(obj.rkAccount, obj.mThumbPath);
				}
			}
		}
		return headers;
	}

	@Override
	public String getContactHeaderPhoto(String rkAccount) {
		// TODO 完善头像的获取		
		ContactInfo contactObj = PersonalManager.getInstance().getContactInfo(rkAccount);
		if(null!=contactObj){
			return contactObj.mThumbPath;
		}
		
		return null;
	}
}
