package com.rongkecloud.test.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.rongkecloud.chat.demo.entity.RKCloudChatContact;
import com.rongkecloud.test.manager.AccountManager;

public class ContactInfo extends RKCloudChatContact implements Parcelable {
	
	public int mGroupId = -1;// 组ID
	public String mRemark;// 备注
	
	public String mAccount;// 用户账号
	public String mRealName;// 姓名	
	public int mUserType;// 用户类型
	public String mAddress;// 住址
	public String mMobile;// 电话
	public int mSex;// 性别
	public String mEmail;// 邮箱
	public String mThumbPath;// 缩略图路径
	public String mPath;//头像路径
	
	public long mInfoLastGetTime;//上次获取个人信息时间
	
	public int mInfoClientVersion;// 个人信息客户端版本号
	public int mInfoServerVersion;// 个人信息服务器版本号
	
	public int mAvatarClientThumbVersion;//客户端缩略图版本号
	public int mAvatarClientVersion;//客户端大图版本号
	public int mAvatarServerVersion;//服务器版本号	

	@Override
	public String getShowName() {
		if (!TextUtils.isEmpty(mRemark)) {
			return mRemark;
		} else if(!TextUtils.isEmpty(mRealName)){
			return mRealName;
		}else if(!TextUtils.isEmpty(mAccount)){
			String currAccount = AccountManager.getInstance().getAccount();
			if(mAccount.equalsIgnoreCase(currAccount)){
				return currAccount;
			}else{
				return mAccount;
			}
		}else{
			String currAccount = AccountManager.getInstance().getAccount();
			if(rkAccount.equalsIgnoreCase(currAccount)){
				return currAccount;
			}else{
				return rkAccount;
			}
		}
	}
	
	@Override
	public String getHeaderThumbImagePath() {
		return mThumbPath;
	}
	
	@Override
	public String getSortKey() {
		if (!TextUtils.isEmpty(mRemark)) {
			return mRemark;
		} else if(!TextUtils.isEmpty(mRealName)){
			return mRealName;
		}else if(!TextUtils.isEmpty(mAccount)){
			String currAccount = AccountManager.getInstance().getAccount();
			if(mAccount.equalsIgnoreCase(currAccount)){
				return currAccount;
			}else{
				return mAccount;
			}
		}else{
			String currAccount = AccountManager.getInstance().getAccount();
			if(rkAccount.equalsIgnoreCase(currAccount)){
				return currAccount;
			}else{
				return rkAccount;
			}
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mGroupId);
		dest.writeString(mRemark);
		
		dest.writeString(mAccount);
		dest.writeString(mRealName);
		dest.writeInt(mUserType);
		dest.writeString(mAddress);
		dest.writeString(mMobile);
		dest.writeInt(mSex);
		dest.writeString(mEmail);
		dest.writeString(mThumbPath);
		dest.writeString(mPath);
		dest.writeLong(mInfoLastGetTime);
		dest.writeInt(mInfoClientVersion);
		dest.writeInt(mInfoServerVersion);
		dest.writeInt(mAvatarClientThumbVersion);
		dest.writeInt(mAvatarClientVersion);
		dest.writeInt(mAvatarServerVersion);
	}

	public static final Parcelable.Creator<ContactInfo> CREATOR = new Creator<ContactInfo>() {
		@Override
		public ContactInfo[] newArray(int size) {
			return new ContactInfo[size];
		}

		@Override
		public ContactInfo createFromParcel(Parcel source) {
			ContactInfo obj = new ContactInfo();
			obj.mGroupId = source.readInt();
			obj.mRemark = source.readString();
			
			obj.mAccount = source.readString();
			obj.rkAccount = obj.mAccount;
			obj.mRealName = source.readString();
			obj.mUserType = source.readInt();
			obj.mAddress = source.readString();
			obj.mMobile = source.readString();
			obj.mSex = source.readInt();
			obj.mEmail = source.readString();
			obj.mThumbPath = source.readString();
			obj.mPath = source.readString();
			obj.mInfoLastGetTime = source.readLong();
			obj.mInfoClientVersion = source.readInt();
			obj.mInfoServerVersion = source.readInt();			
			obj.mAvatarClientThumbVersion = source.readInt();
			obj.mAvatarClientVersion = source.readInt();
			obj.mAvatarServerVersion = source.readInt();
			return obj;
		}
	};
	
	public boolean copyData(ContactInfo obj){
		if(null == obj){
			return false;
		}
		mGroupId = obj.mGroupId;
		mRemark = obj.mRemark;

		mAccount = obj.mAccount;
		rkAccount = mAccount;
		mRealName = obj.mRealName;
		mUserType = obj.mUserType;
		mAddress = obj.mAddress;
		mMobile = obj.mMobile;
		mSex = obj.mSex;
		mEmail = obj.mEmail;
		mThumbPath = obj.mThumbPath;
		mPath = obj.mPath;
		mInfoLastGetTime = obj.mInfoLastGetTime;
		mInfoClientVersion = obj.mInfoClientVersion;
		mInfoServerVersion = obj.mInfoServerVersion;			
		mAvatarClientThumbVersion = obj.mAvatarClientThumbVersion;
		mAvatarClientVersion = obj.mAvatarClientVersion;
		mAvatarServerVersion = obj.mAvatarServerVersion;
		return true;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer().append("ContactInfo[")
				.append("mAccount=").append(mAccount)
				.append(", rkAccount=").append(rkAccount)
				.append(", mRealName=").append(mRealName)
				.append(", mRemark=").append(mRemark)
				.append("]");
		return sb.toString();
	}
}
