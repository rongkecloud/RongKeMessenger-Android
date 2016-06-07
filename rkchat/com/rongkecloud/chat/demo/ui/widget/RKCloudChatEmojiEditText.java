package com.rongkecloud.chat.demo.ui.widget;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.widget.EditText;

import com.rongkecloud.chat.RKCloudChatMessageManager;
import com.rongkecloud.chat.demo.entity.RKCloudChatEmojiRes;
import com.rongkecloud.chat.demo.tools.RKCloudChatTools;
import com.rongkecloud.test.R;

/**
 * 带表情图标的输入框
 */
public class RKCloudChatEmojiEditText extends EditText{
	private Context mContext;
	private ClipboardManager mClipboardManager;	

	public RKCloudChatEmojiEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		mClipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
	}

	/**
	 * 向文本框中插入表情图标
	 * @param emojiRegx
	 */
	public void insertIcon(String emojiRegx) {
		if(TextUtils.isEmpty(emojiRegx)){
			return;
		}
		int insertLegth = RKCloudChatMessageManager.getInstance(mContext).getTextMaxLength() - getText().toString().length();
		if (insertLegth < emojiRegx.length()) {
			RKCloudChatTools.showToastText(getContext(), getContext().getString(R.string.rkcloud_chat_emojiedittext_full));	
			return;
		}	

		String[] emojiRegxs = RKCloudChatEmojiRes.EMOJI_REGX;
		int[] emojiResIds = RKCloudChatEmojiRes.EMOJI_RESIDS;

		int len = emojiRegxs.length;
		int index = -1;
		for (int i = 0; i < len; i++) {
			if (emojiRegx.equals(emojiRegxs[i])) {
				index = i;
				break;
			}
		}
		
		int start = getSelectionStart();
		Spannable ss = getText().insert(start, emojiRegx);
		if(-1!=index && index<len){
			Drawable d = getResources().getDrawable(emojiResIds[index]);
			if(null != d){
				int imageWidth = (int)getResources().getDimension(R.dimen.rkcloud_chat_emoji_inedittext_width);
				int imageHeight = (int)getResources().getDimension(R.dimen.rkcloud_chat_emoji_inedittext_height);
				d.setBounds(0, 0, imageWidth, imageHeight);
				ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
				ss.setSpan(span, start, start + emojiRegx.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
		
		setText(ss);
		setSelection(start + emojiRegx.length());		
	}

	@Override
	public boolean onTextContextMenuItem(int id) {
		if(id == android.R.id.paste){
			String oldContent = getText().toString();
			if(mClipboardManager.hasPrimaryClip()){
				ClipData clipData = mClipboardManager.getPrimaryClip();
				int count = clipData.getItemCount();
				// 取最后一项
				ClipData.Item item = clipData.getItemAt(count-1);
				String insertContent = item.coerceToText(mContext).toString().trim();
				processPaste(oldContent, insertContent, getSelectionEnd());	
			}
			return true;
		}
		
		return super.onTextContextMenuItem(id);
	}
	
	private void processPaste(String oldContent, String insertContent, int location){		
		int insertLength = insertContent.length();
		int realInsertLength = RKCloudChatMessageManager.getInstance(mContext).getTextMaxLength() - oldContent.length();// 实际可以插入的内容长度
		String realInsertContent = null;// 实际可以插入的内容
		if(realInsertLength >= insertLength){
			realInsertContent = insertContent;
		}else{
			// 如果最后面为表情时，需要去掉
			Matcher m = Pattern.compile(RKCloudChatEmojiRes.EMOJI_REGP_RULE).matcher(insertContent);			
			while (m.find()) {
				if(m.start() >= realInsertLength-(RKCloudChatEmojiRes.EMOJI_REGP_LENGTH-1)){
					if(m.start()<=realInsertLength+(RKCloudChatEmojiRes.EMOJI_REGP_LENGTH-1)){
						realInsertLength = m.start();
					}
					break;
				}							
			}
			realInsertContent = insertContent.substring(0, realInsertLength);	
		}
		
		StringBuffer buffer = new StringBuffer().append(oldContent.substring(0, location)).append(realInsertContent).append(oldContent.substring(location));
		
		// 转成带表情的内容
		CharSequence currentContent = RKCloudChatTools.parseMsgFace(mContext, buffer.toString(), 0, 3);
		setText(currentContent);
		setSelection(currentContent.length() - (oldContent.length() - location));
		
		if(insertLength > realInsertLength){
			RKCloudChatTools.showToastText(getContext(), getContext().getString(R.string.rkcloud_chat_emojiedittext_full));		
		}
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
	}
}