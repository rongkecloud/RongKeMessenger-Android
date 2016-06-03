package com.rongke.cloud.meeting.demo;

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

import com.rongke.cloud.meeting.demo.entity.RKCloudMeetingUserInfo;
import com.rongkecloud.chat.demo.tools.RKCloudChatSDCardTools;
import com.rongkecloud.test.entity.Constants;
import com.rongkecloud.test.entity.ContactInfo;
import com.rongkecloud.test.manager.PersonalManager;
import com.rongkecloud.test.ui.contact.ContactDetailInfoActivity;

public class RKCloudMeetingContactManager{
	private static final int CONTACTSINFO_CHANGED = 1;// 联系人信息有变化
	private static final int CONTACT_HEADERIMAGE_CHANGED = 1005;// 联系人头像有变化

	
	private static RKCloudMeetingContactManager mInstance;
	private Context mContext;
	private Handler mUiHandler;
	
	private RKCloudMeetingContactManager(Context context){
		mContext = context;
	}
	
	public static RKCloudMeetingContactManager getInstance(Context context){
		if(null == mInstance){
			mInstance = new RKCloudMeetingContactManager(context);
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
			msg.what = RKCloudMeetingUiHandlerMessage.MSG_WHAT_MEETING_CONTACTSINFO_CHANGED;
			msg.obj = lowerAccounts;
			msg.sendToTarget();
		}
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
	
	/**
	 * 账户的头像有变化
	 * @param account
	 */
	public void onAccountHeaderImageChanged(String account){
		if(null!=mUiHandler && !TextUtils.isEmpty(account)){
			Message msg = mUiHandler.obtainMessage();
			msg.what = RKCloudMeetingUiHandlerMessage.MSG_WHAT_MEETING_CONTACT_HEADERIMAGE_CHANGED;
			msg.obj = account.toLowerCase();
			msg.sendToTarget();
		}
	}

	public RKCloudMeetingUserInfo getContactInfo(String account){
		RKCloudMeetingUserInfo info = new RKCloudMeetingUserInfo();
		info.setAttendeeAccount(account);
		info.showName = account;
		// TODO 完善用户信息
		ContactInfo mContactInfo = PersonalManager.getInstance().getContactInfo(account);
		if(null != mContactInfo){
			info.showName = mContactInfo.getShowName();
			info.avatarPath = mContactInfo.mThumbPath;
		}

		return info;
	}
	
	public Map<String, RKCloudMeetingUserInfo> getContactInfos(List<String> accounts){
		Map<String, RKCloudMeetingUserInfo> datas = new HashMap<String, RKCloudMeetingUserInfo>();
		// TODO 完善用户信息
		Map<String, ContactInfo> contacts = PersonalManager.getInstance().getContactsInfoByAccounts(accounts);
		if(null!=contacts && 0 != contacts.size()){
			boolean sdAvailable = RKCloudChatSDCardTools.diskSpaceAvailable();
			RKCloudMeetingUserInfo meetingContactInfo;
			for(ContactInfo obj : contacts.values()){
				// 下载头像的缩略图
				if(obj.mAvatarServerVersion>0 && sdAvailable){
					if(TextUtils.isEmpty(obj.mThumbPath) 
							|| !new File(obj.mThumbPath).exists() 
							|| obj.mAvatarServerVersion>obj.mAvatarClientThumbVersion){
						PersonalManager.getInstance().getAvatarThumb(obj.mAccount, obj.mAvatarServerVersion);
					}
				}
				
				meetingContactInfo = new RKCloudMeetingUserInfo();
				meetingContactInfo.setAttendeeAccount(obj.mAccount);
				meetingContactInfo.showName = obj.getShowName();
				meetingContactInfo.avatarPath = obj.mThumbPath;
				datas.put(obj.mAccount.toLowerCase(), meetingContactInfo);
			}
		}
		return datas;
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
}
