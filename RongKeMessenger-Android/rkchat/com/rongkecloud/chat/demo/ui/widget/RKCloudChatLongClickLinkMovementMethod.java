package com.rongkecloud.chat.demo.ui.widget;

import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.MotionEvent;
import android.widget.TextView;

public class RKCloudChatLongClickLinkMovementMethod extends LinkMovementMethod {

	private static final int LONGCLICK_TIME = 500;// 定义长点击的时间间隔，单位：ms
	private static RKCloudChatLongClickLinkMovementMethod instance;
	private long mLastClickTime = 0L;

	@Override
	public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
		int action = event.getAction();
		switch(action){
		case MotionEvent.ACTION_DOWN:
			mLastClickTime = System.currentTimeMillis();
			break;
			
		case MotionEvent.ACTION_UP:
			int x = (int) event.getX();
			int y = (int) event.getY();
			x -= widget.getTotalPaddingLeft();
			y -= widget.getTotalPaddingTop();
			x += widget.getScrollX();
			y += widget.getScrollY();
			Layout layout = widget.getLayout();
			int line = layout.getLineForVertical(y);
			int off = layout.getOffsetForHorizontal(line, x);

			RKCloudChatLongClickableSpan[] link = buffer.getSpans(off, off, RKCloudChatLongClickableSpan.class);
			if (link.length > 0) {
				if (System.currentTimeMillis() - mLastClickTime < LONGCLICK_TIME){
					link[0].onClick(widget);
				}else{
					// 长点击时暂时不做处理					
				}
				return true;
			}
			break;
		}		

		return super.onTouchEvent(widget, buffer, event);
	}

	public static MovementMethod getInstance() {
		if (instance == null)
			instance = new RKCloudChatLongClickLinkMovementMethod();

		return instance;
	}
}