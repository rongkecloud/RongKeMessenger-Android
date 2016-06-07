package com.rongkecloud.chat.demo.ui.loadimages;

import android.graphics.drawable.Drawable;

import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest.IMAGE_REQUEST_TYPE;

public class RKCloudChatImageResult {
	public IMAGE_REQUEST_TYPE type;// 对应ImageRequest类中的type属性
	public String requester;// 对应ImageRequest类中的requester属性
	public String key;// 对应ImageRequest类中的key属性
	public boolean isSquare = true;// 对应ImageRequest类中的isSquare属性
	
	public Drawable resource;// 图片资源
	protected boolean isExpired = true;// 标注该cache是否已经过期
}