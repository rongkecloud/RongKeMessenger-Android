package com.rongkecloud.chat.demo.entity;

import com.rongkecloud.test.R;

public class RKCloudChatEmojiRes {
	/**
	 * 每个表情图标正则表达式对应的字符个数
	 */
	public static final int EMOJI_REGP_LENGTH = 5;
	/**
	 * 表情图标的正则表达式规则
	 */
	public static final String EMOJI_REGP_RULE = "~:.{2}~";

	/**
	 * 所有表情，聊天页面表情区域展示使用
	 */
	public static final int[] EMOJI_RESIDS = new int[] { 
		R.drawable.rkcloud_chat_emoji_01, R.drawable.rkcloud_chat_emoji_02, R.drawable.rkcloud_chat_emoji_03, R.drawable.rkcloud_chat_emoji_04, 
		R.drawable.rkcloud_chat_emoji_05, R.drawable.rkcloud_chat_emoji_06, R.drawable.rkcloud_chat_emoji_07, R.drawable.rkcloud_chat_emoji_08, 
		R.drawable.rkcloud_chat_emoji_09, R.drawable.rkcloud_chat_emoji_10, R.drawable.rkcloud_chat_emoji_11, R.drawable.rkcloud_chat_emoji_12, 
		R.drawable.rkcloud_chat_emoji_13, R.drawable.rkcloud_chat_emoji_14, R.drawable.rkcloud_chat_emoji_15, R.drawable.rkcloud_chat_emoji_16, 
		R.drawable.rkcloud_chat_emoji_17, R.drawable.rkcloud_chat_emoji_18, R.drawable.rkcloud_chat_emoji_19,	R.drawable.rkcloud_chat_emoji_20, 
		R.drawable.rkcloud_chat_emoji_21, R.drawable.rkcloud_chat_emoji_22, R.drawable.rkcloud_chat_emoji_23, R.drawable.rkcloud_chat_emoji_24};
	
	/**
	 * 表情对应的正则表达式
	 */
	public static final String[] EMOJI_REGX = new String[]{		
		"~:a0~", "~:a1~", "~:a2~", "~:a3~", 
		"~:a4~", "~:a5~", "~:a6~", "~:a7~", 
		"~:a8~", "~:a9~", "~:b0~", "~:b1~", 
		"~:b2~", "~:b3~", "~:b4~", "~:b5~", 
		"~:b6~", "~:b7~", "~:b8~", "~:b9~",
		"~:c0~", "~:c1~", "~:c2~", "~:c3~"};
	
	/**
	 * 表情对应的别名
	 */
	public static final String[] EMOJI_ALIAS = new String[]{
		"[微笑]", "[开怀大笑]", "[目瞪口呆]", "[难过]", 
		"[无言]", "[抽烟]", "[小天使]", "[疑问]", 
		"[流汗]", "[惊恐]", "[偷笑]", "[吐舌头]", 
		"[愤怒]", "[想睡觉]", "[大哭]", "[扁嘴]", 
		"[感动]", "[接吻]", "[病恹恹]", "[再见]", 
		"[赞(向上)]", "[赞(向下)]", "[OK]", "[胜利]"};
}
