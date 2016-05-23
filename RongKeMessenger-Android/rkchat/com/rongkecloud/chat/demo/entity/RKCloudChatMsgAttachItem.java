package com.rongkecloud.chat.demo.entity;

/**
 * 消息聊天页面更多功能区域底端项的Bean定义 
 */
public class RKCloudChatMsgAttachItem {
	public enum ATTACH_TYPE{
		SELECTLOCALIMAGE,	// 选择本地图片
		TAKEPHOTO, 			// 拍照
		RECORDVIDEO,		// 小视频
		FILE,				// 文件
		VIDEOCALL,			// 视频通话
		AUDIOCALL,			// 语音通话
        MULTIMEETING        // 多人语音
	}
	
	public ATTACH_TYPE type;// 类型
	public int drawableResId;// 图片对应的资源ID
	public String content;// 显示的内容
	
	public RKCloudChatMsgAttachItem(ATTACH_TYPE type, int resId, String content){
		this.type = type;
		this.drawableResId = resId;
		this.content = content;
	}
}
