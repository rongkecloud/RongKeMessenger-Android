package com.rongke.cloud.meeting.demo.entity;

import android.text.TextUtils;

import com.rongkecloud.multiVoice.RKCloudMeetingUserBean;

public class RKCloudMeetingUserInfo extends RKCloudMeetingUserBean{
	/**
	 * 账号在APP通讯录中显示的名称
	 */
	public String showName;
	/**
	 * 账号在APP通讯录中保存在SD卡的头像路径
	 */
	public String avatarPath;

	public RKCloudMeetingUserInfo(){
		super();
	}
	
	/**
	 * 获取参与者显示的名称，如果showName内容为空时返回参与者账号
	 * @return
	 */
	public String getShowName(){
		if(TextUtils.isEmpty(showName)){
			return getAttendeeAccount();
		}else{
			return showName;
		}
	}
	
	/**
	 * 拷贝数据，同一账号的参与者可以拷贝数据，反之拷贝失败
	 * @param obj {@link RKCloudMeetingUserInfo}对象
	 * @return boolean
	 */
	public boolean copyData(RKCloudMeetingUserInfo obj){
		if(null == obj || !obj.getAttendeeAccount().equalsIgnoreCase(getAttendeeAccount())){
			return false;
		}
		
		setAttendeeAccount(obj.getAttendeeAccount());
		setMute(obj.isMute());
		showName = obj.showName;
		avatarPath = obj.avatarPath;
		return true;
	}
}
