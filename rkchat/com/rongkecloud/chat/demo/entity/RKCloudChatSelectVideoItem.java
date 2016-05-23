package com.rongkecloud.chat.demo.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class RKCloudChatSelectVideoItem implements Parcelable{
	private static final String TAG = RKCloudChatSelectVideoItem.class.getSimpleName();
	private int mId;
	private String mTitle;// 文件名称
	private String mFilePath;// 文件路径
	private long mFileSize;// 文件大小，单位：字节
	private int mDuration;// 时长，单位：秒
	
	public int getId() {
		return mId;
	}
	public void setId(int id) {
		mId = id;
	}
	public String getTitle() {
		return mTitle;
	}
	public void setTitle(String title) {
		mTitle = title;
	}
	public String getFilePath() {
		return mFilePath;
	}
	public void setFilePath(String filePath) {
		mFilePath = filePath;
	}
	public long getFileSize() {
		return mFileSize;
	}
	public void setFileSize(long size) {
		mFileSize = size;
	}
	public int getDuration() {
		return mDuration;
	}
	/**
	 * 设置时长，单位：毫秒
	 */
	public void setDuration(long duration) {		
		mDuration = (int)Math.floor(duration/1000.0f);
	}
	
	public static final Parcelable.Creator<RKCloudChatSelectVideoItem> CREATOR = new Creator<RKCloudChatSelectVideoItem>() {
		@Override
		public RKCloudChatSelectVideoItem[] newArray(int size) {
			return new RKCloudChatSelectVideoItem[size];
		}
		
		@Override
		public RKCloudChatSelectVideoItem createFromParcel(Parcel source) {
			RKCloudChatSelectVideoItem obj = new RKCloudChatSelectVideoItem();
			obj.mId = source.readInt();
			obj.mTitle = source.readString();
			obj.mFilePath = source.readString();
			obj.mFileSize = source.readLong();
			obj.mDuration = source.readInt();
			return obj;
		}
	};
	
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mId);
		dest.writeString(mTitle);
		dest.writeString(mFilePath);
		dest.writeLong(mFileSize);
		dest.writeInt(mDuration);
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer().append(TAG).append("[")
				.append("id=").append(mId)
				.append(", title=").append(mTitle)
				.append(", filePath=").append(mFilePath)
				.append(", fileSize=").append(mFileSize)
				.append(", duration=").append(mDuration)
				.append("]");
		return sb.toString();
	}
}
