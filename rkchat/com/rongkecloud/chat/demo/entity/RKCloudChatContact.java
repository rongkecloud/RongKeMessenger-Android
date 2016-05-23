package com.rongkecloud.chat.demo.entity;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;

public class RKCloudChatContact{
	public String rkAccount;// 云视互动账号				
	public SpannableStringBuilder highLightName;// 高亮显示的名称
		
	public String getShowName(){
		// TODO 完善UI显示的名称
		return rkAccount;
	}
	
	/**
	 * 获取头像缩略图路径
	 * @return
	 */
	public String getHeaderThumbImagePath(){
		// TODO 完善头像路径 
		return "";
	}
	
	/**
	 * 排序时使用
	 * @return
	 */
	public String getSortKey(){
		// TODO 完善排序字段
		return rkAccount;
	}
	
	/**
	 * 使用搜索条件匹配名字
	 */
	public void matchName(String filter, BackgroundColorSpan backgroundColorSpan) {
		int start=0, end=0;			
		// 名字高亮字段
		String upperCaseName = getShowName().toUpperCase();
		String upperCaseFilter = filter.toUpperCase();
		// 直接使用filter搜索
		if ((start=upperCaseName.indexOf(upperCaseFilter)) != -1) {
			end = start + filter.length();
			highLightName = new SpannableStringBuilder(getShowName());
			if(null != backgroundColorSpan){
				highLightName.setSpan(backgroundColorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}else{
			highLightName = null;
		}
	}
}
