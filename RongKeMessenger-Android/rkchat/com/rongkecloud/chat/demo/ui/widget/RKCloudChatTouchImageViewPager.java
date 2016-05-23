package com.rongkecloud.chat.demo.ui.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class RKCloudChatTouchImageViewPager extends ViewPager {
	private MotionEvent lastMotionEvent = null;
	private RKCloudChatTouchImageViewListener mListener = null;

	public RKCloudChatTouchImageViewPager(Context context) {
		super(context);
	}

	public RKCloudChatTouchImageViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setOnRKCloudChatTouchImageViewListener(RKCloudChatTouchImageViewListener listener) {
		mListener = listener;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent arg0) {
		if (mListener != null) {
			int currIdx = getCurrentItem();

			View v = mListener.onGetCurrTouchImageView(currIdx);
			if (v != null && v instanceof RKCloudChatTouchImageView) {
				RKCloudChatTouchImageView tiv = (RKCloudChatTouchImageView) v;
				if (MotionEvent.ACTION_MOVE == arg0.getAction() && tiv.isScaleAndNotAtSide()) {
					lastMotionEvent = arg0;
					return false;
				}
			}
		}

		boolean tmpb = false;
		if (lastMotionEvent != null) {
			arg0.setAction(MotionEvent.ACTION_DOWN);
			try {
				tmpb = super.onInterceptTouchEvent(arg0);
			} catch (IllegalArgumentException e) {
			}
			lastMotionEvent = null;
			return tmpb;
		}
		
		try {
			tmpb = super.onInterceptTouchEvent(arg0);
		} catch (IllegalArgumentException e) {
		}
		
		return tmpb;
	}

	public interface RKCloudChatTouchImageViewListener {
		public View onGetCurrTouchImageView(int pos);
	}
}
