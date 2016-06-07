package com.rongkecloud.chat.demo.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/**
 * 重新定义FrameLayout组件
 */
public class RKCloudChatSameSizeFrameLayout extends FrameLayout {
	public static final String TAG = RKCloudChatSameSizeFrameLayout.class.getSimpleName();

	private int mFirstChildWidth;
	private int mFirstChildHeight;

	public RKCloudChatSameSizeFrameLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public RKCloudChatSameSizeFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public RKCloudChatSameSizeFrameLayout(Context context) {
		super(context);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int mCount = getChildCount();
		if (mCount > 1) {
			View firstView = getChildAt(0);
			mFirstChildWidth = firstView.getMeasuredWidth();
			mFirstChildHeight = firstView.getMeasuredHeight();
			if(mFirstChildWidth>0 && mFirstChildHeight>0){
				for (int i = 1; i < mCount; i++) {
					View nextView = getChildAt(i);
					nextView.setMinimumHeight(mFirstChildHeight);
					nextView.setMinimumWidth(mFirstChildWidth);
				}
			}
		}

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
	}
}
