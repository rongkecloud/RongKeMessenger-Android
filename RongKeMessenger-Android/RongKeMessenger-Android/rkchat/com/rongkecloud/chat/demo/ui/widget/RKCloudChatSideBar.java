package com.rongkecloud.chat.demo.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.rongkecloud.test.R;

public class RKCloudChatSideBar extends View {
	// 显示的字符
	private static final char[] SHOW_CHARS = new char[] {'#','A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
	private SectionIndexer mSectionIndexter = null;
	private ListView mList;
	private TextView mDialogText;
	
	private boolean hasClickedDown = false;
	private Paint paint = new Paint();
	private Paint mBgPaint = new Paint();
	private RectF mBgRcf = null;
	
	private float mFontSize;
	
	public RKCloudChatSideBar(Context context) {
		super(context);
		init();
	}

	public RKCloudChatSideBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public RKCloudChatSideBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	private void init(){
		mFontSize = getResources().getDimension(R.dimen.rkcloud_chat_address_sidebar_textsize);
	}

	public void setListView(ListView listview) {
		mList = listview;
		mSectionIndexter = (SectionIndexer) listview.getAdapter();
	}

	public void setTextView(TextView dialogText) {
		mDialogText = dialogText;
	}

	//显示 当点击 某字符时 出现在中间的 包含该字符的大图；
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);
		int i = (int) event.getY();
		int singleHeight = getMeasuredHeight() / SHOW_CHARS.length;
		int idx = i / singleHeight;
		if (idx >= SHOW_CHARS.length) {
			idx = SHOW_CHARS.length - 1;
		} else if (idx < 0) {
			idx = 0;
		}
		if (event.getAction() == MotionEvent.ACTION_DOWN
				|| event.getAction() == MotionEvent.ACTION_MOVE) {
			hasClickedDown = true;
			mDialogText.setVisibility(View.VISIBLE);
			mDialogText.setText("" + SHOW_CHARS[idx]);
			if (mSectionIndexter == null) {
				mSectionIndexter = (SectionIndexer) mList.getAdapter();
			}
			int position = mSectionIndexter.getPositionForSection(SHOW_CHARS[idx]);
			if (position == -1) {
				return true;
			}
			mList.setSelection(position);
		} else {
			hasClickedDown = false;
			mDialogText.setVisibility(View.GONE);
		}
		return true;
	}
	

	protected void onDraw(Canvas canvas) {
		paint.setTextSize(mFontSize);
		paint.setTextAlign(Paint.Align.CENTER);
		paint.setAntiAlias(true);
		
		if(hasClickedDown){
			paint.setColor(0xffffffff);
			mBgPaint.setColor(0x60000000);
		}else{
			paint.setColor(0xff595c61);
			mBgPaint.setColor(0x00000000);
		}
		
		float widthCenter = getMeasuredWidth() / 2;
		int singleHeight = getMeasuredHeight() / SHOW_CHARS.length;
		
		mBgRcf = new RectF(0, 0, getMeasuredWidth(), getMeasuredHeight());
		canvas.drawRoundRect(mBgRcf, 5.0f, 5.0f, mBgPaint);

		for (int i = 0; i < SHOW_CHARS.length; i++) {
			canvas.drawText(String.valueOf(SHOW_CHARS[i]), widthCenter, singleHeight + (i * singleHeight), paint);
		}
		super.onDraw(canvas);
	}
}
