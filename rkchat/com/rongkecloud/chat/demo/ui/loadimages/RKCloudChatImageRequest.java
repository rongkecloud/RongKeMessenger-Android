package com.rongkecloud.chat.demo.ui.loadimages;

import java.util.Locale;

public class RKCloudChatImageRequest {
	// 图片请求的类型定义
	public enum IMAGE_REQUEST_TYPE{
		GET_MSG_THUMBNAIL,// 获取消息图片中的缩略图
		GET_MSG_VIDEO_THUMBNAIL, // 获取消息中视频文件的缩略图
		GET_CONTACT_HEADERIMG // 获取联系人头像
	}
	
	private IMAGE_REQUEST_TYPE type;// 发起请求的类型
	private String requester;// 谁发起的图片加载请求，一般为msgSerialNum等
	/**
	 * 请求的key值
	 * TYPE_REQ_MSG_THUMBNAIL -- file path of sended msg or thumbnail path of received msg
	 * GET_VIDEO_THUMBNAIL -- video file path
	 * GET_CONTACT_HEADERIMG -- header image path
	 */
	private String key;
	
	public boolean isSquare = true;// 是否是直角图片，直角为true， 圆角为false
	protected boolean isReload = false;// 是否需要重新加载
	// 图片宽度和高度
	public int mWidth, mHeight;
	
	private RKCloudChatImageRequest(){
		
	}
	
	/**
	 * 实例化
	 * @param type 请求类型
	 * @param key 请求的key值，可能为文件路径等
	 */
	public RKCloudChatImageRequest(IMAGE_REQUEST_TYPE type, String key){
		this.type =type;  
		this.key = key;
	}
	
	/**
	 * 实例化
	 * @param type 请求类型
	 * @param key 请求的key值，可能为文件路径等
	 * @param requester 谁发起的图片加载请求，一般为msgSerialNum、 rkaccount等
	 */
	public RKCloudChatImageRequest(IMAGE_REQUEST_TYPE type, String key, String requester){
		this(type, key);
		setRequester(requester);
	}
	
	public void setRequester(String requester){
		this.requester = requester;
	}
	
	public IMAGE_REQUEST_TYPE getType(){
		return type;
	}
	
	public String getKey(){
		return key;
	}
	
	public String getRequester(){
		return requester;
	}

	/**
	 * cache 内部使用每个Cache唯一索引
	 * @return
	 */
	protected String getUniqueKey() {
		return String.format("%s-%s-%d", type.name(), key, isSquare ? 1 : 0);
	}

	@Override
	public String toString() {
		return String.format(Locale.getDefault(), "type=%s, requester=%s, key=%s", type.name(), requester, key);
	}
}
