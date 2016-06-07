package com.rongkecloud.chat.demo.entity;

public class RKCloudChatSelectFileItem implements Comparable<RKCloudChatSelectFileItem> {
	
	private String mText;
	private int mIcon;
	
	private boolean mSelectable = true;
	private boolean mIsBackItem = false;

	public RKCloudChatSelectFileItem(String text, int icon) {
		mIcon = icon;
		mText = text;
	}

	public boolean isSelectable() {
		return mSelectable;
	}

	public void setSelectable(boolean selectable) {
		mSelectable = selectable;
	}

	public String getText() {
		return mText;
	}

	public int getIcon() {
		return mIcon;
	}

	public int compareTo(RKCloudChatSelectFileItem other) {
		if (mIsBackItem) {
			return Integer.MIN_VALUE;
		} else {
			if (other.isBackItem()) {
				return Integer.MAX_VALUE;
			}
		}
		if (mText != null) {
			return mText.compareTo(other.getText());
		} else {
			throw new IllegalArgumentException();
		}
	}

	public void setBackItem(boolean isBackItem) {
		mIsBackItem = isBackItem;
	}

	public boolean isBackItem() {
		return mIsBackItem;
	}
}
