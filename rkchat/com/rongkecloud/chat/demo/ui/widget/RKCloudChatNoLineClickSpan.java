package com.rongkecloud.chat.demo.ui.widget;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Patterns;
import android.view.View;

/**
 * 定义带长点击的RKCloudChatLongClickableSpan
 */
abstract class RKCloudChatLongClickableSpan extends ClickableSpan {
	abstract public void onLongClick(View view);
}

public class RKCloudChatNoLineClickSpan extends RKCloudChatLongClickableSpan {
	private static final int URL = 0;
	private static final int EMAIL = 1;
	private static final int PHONE = 2;

	private String mStr;
	private int mType;

	public RKCloudChatNoLineClickSpan(SpannableString spStr) {
		Pattern pattern = Patterns.WEB_URL;
		Matcher matcher = pattern.matcher(spStr);
		while (matcher.find()) {
			String strBuffer = matcher.group();
			int start = spStr.toString().indexOf(strBuffer);
			String EmailUrl = "@" + strBuffer;
			int emailIndex = spStr.toString().indexOf(EmailUrl);
			if (start - emailIndex != 1 || start == 0) {
				ClickableSpan clickSpan = new RKCloudChatNoLineClickSpan(strBuffer, URL); // 设置超链接
				spStr.setSpan(clickSpan, start, start + strBuffer.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			}
		}

		Pattern patternEmail = Patterns.EMAIL_ADDRESS;
		Matcher matcherEmail = patternEmail.matcher(spStr);
		while (matcherEmail.find()) {
			String strBuffer = matcherEmail.group();
			int start = spStr.toString().indexOf(strBuffer);
			ClickableSpan clickSpan = new RKCloudChatNoLineClickSpan(strBuffer, EMAIL); // 设置超链接
			spStr.setSpan(clickSpan, start, start + strBuffer.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		}

		Pattern patternPhone = Patterns.PHONE;
		Matcher matcherPhone = patternPhone.matcher(spStr);
		while (matcherPhone.find()) {
			String strBuffer = matcherPhone.group();
			if(strBuffer.length() >= 7){
				int start = spStr.toString().indexOf(strBuffer);
				ClickableSpan clickSpan = new RKCloudChatNoLineClickSpan(strBuffer, PHONE); // 设置超链接
				spStr.setSpan(clickSpan, start, start + strBuffer.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			}
		}
	}

	private RKCloudChatNoLineClickSpan(String str, int type) {
		super();
		mStr = str;
		mType = type;
	}

	@Override
	public void updateDrawState(TextPaint ds) {
		ds.setColor(ds.linkColor);
		ds.setUnderlineText(true);
	}

	@Override
	public void onClick(View widget) {
		switch (mType) {
		case URL:
			if (!mStr.startsWith("http://")) {
				mStr = "http://" + mStr;
			}
			Uri content_url = Uri.parse(mStr);
			Intent intent = new Intent(Intent.ACTION_VIEW, content_url);
			try {
				widget.getContext().startActivity(intent);
			} catch (Exception e) {
				e.printStackTrace();
			}

			break;
		case PHONE:
			Intent intentPhone = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + mStr));
			try {
				widget.getContext().startActivity(intentPhone);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		case EMAIL:
			String[] email = { mStr };
			Intent intentEmail = new Intent(Intent.ACTION_SEND);
			intentEmail.setType("message/rfc822");
			intentEmail.putExtra(Intent.EXTRA_EMAIL, email);
			intentEmail.putExtra(Intent.EXTRA_CC, email);
			try {
				widget.getContext().startActivity(Intent.createChooser(intentEmail, null));
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
			
		default:
			break;
		}
	}

	@Override
	public void onLongClick(View view) {
	}
}
