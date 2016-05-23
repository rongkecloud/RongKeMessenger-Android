package com.rongke.cloud.av.demo;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.rongke.cloud.av.demo.entity.RKCloudAVContact;
import com.rongkecloud.test.entity.Constants;
import com.rongkecloud.test.entity.ContactInfo;
import com.rongkecloud.test.manager.PersonalManager;

public class RKCloudAVContactManager {
	private static RKCloudAVContactManager mInstance;
	private Context mContext;
	private Handler mUiHandler;
	
	private RKCloudAVContactManager(Context context){
		mContext = context;
	}
	
	public static RKCloudAVContactManager getInstance(Context context){
		if(null == mInstance){
			mInstance = new RKCloudAVContactManager(context);
		}
		return mInstance;
	}
	
	/**
	 * 获取联系人信息
	 * @param rkAccount
	 * @return
	 */
	public RKCloudAVContact getContactInfo(String rkAccount){
		RKCloudAVContact obj = new RKCloudAVContact();
		obj.account = rkAccount;
		// TODO 完善信息
		ContactInfo contact = PersonalManager.getInstance().getContactInfo(rkAccount); 
		if(null != contact){
			obj.showName = contact.getShowName();
			obj.thumbPath = contact.mThumbPath;
		}else{
			obj.showName = rkAccount;
		}
		return obj;
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
			msg.what = RKCloudAVUiHandlerMessage.MSG_WHAT_MEETING_CONTACTSINFO_CHANGED;
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
			msg.what = RKCloudAVUiHandlerMessage.MSG_WHAT_MEETING_CONTACT_HEADERIMAGE_CHANGED;
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
}
