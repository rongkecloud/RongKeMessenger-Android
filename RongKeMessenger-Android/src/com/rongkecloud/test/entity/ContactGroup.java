package com.rongkecloud.test.entity;

import java.util.ArrayList;
import java.util.List;

public class ContactGroup{
	public int mGroupId;//组id
	public String mGroupName;//组名称
	public int mGroupCount;//组人数
	public List<ContactInfo> mContactFriendInfo = new ArrayList<ContactInfo>();//组成员
}
