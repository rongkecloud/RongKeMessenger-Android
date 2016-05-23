package com.rongkecloud.chat.demo.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.TextView;

import com.rongkecloud.test.R;

/**
 * 按屏幕宽度百分比限制最大宽度的TextView
 */
public class RKCloudChatResizeTextView extends TextView {
	private static final String TAG = RKCloudChatResizeTextView.class.getSimpleName();

	private Context mContext;
	private static final float DEFAULT_WIDTH_SCALE = 0.55F;
	private float mWidthScale = DEFAULT_WIDTH_SCALE;

	public RKCloudChatResizeTextView(Context context) {
		super(context);
		mContext = context;
	}

	public RKCloudChatResizeTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init(attrs);
	}

	public RKCloudChatResizeTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		init(attrs);
	}

	private void init(AttributeSet attrs) {
		TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.RKCloud_Chat_Attrs);
		float l_widthScale = a.getFloat(R.styleable.RKCloud_Chat_Attrs_width_scale, 0);
		a.recycle();
		if (l_widthScale > 0.0 && l_widthScale <= 1.0) {
			mWidthScale = l_widthScale;
		}
	}

	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = (int) (findScrollWidthPixels() * mWidthScale);
		setMaxWidth(width);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	private int findScrollWidthPixels() {
		DisplayMetrics dm = new DisplayMetrics();
		WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(dm);
		return dm.widthPixels;
	}

	public float getWidthScale() {
		return mWidthScale;
	}

	/**
	 * 控件最大宽度占屏幕宽度百分比
	 * 
	 * @param widthMeasureScale
	 *            有效范围(0.0, 1.0]
	 */
	public void setWidthScale(float widthMeasureScale) {
		if (widthMeasureScale > 0.0 && widthMeasureScale <= 1.0) {
			this.mWidthScale = widthMeasureScale;
		}
	}
}
