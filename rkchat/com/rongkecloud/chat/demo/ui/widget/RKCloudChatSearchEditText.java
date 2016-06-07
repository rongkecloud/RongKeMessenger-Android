package com.rongkecloud.chat.demo.ui.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.EditText;

public class RKCloudChatSearchEditText extends EditText implements TextWatcher{
	
	private final static String TAG = RKCloudChatSearchEditText.class.getSimpleName();
	
	private Drawable clearDrawable;
	
	public RKCloudChatSearchEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	private void init() {
		clearDrawable = getCompoundDrawables()[2];
		if(clearDrawable == null){
			return ;
		}
		clearDrawable.setBounds(0, 0, clearDrawable.getIntrinsicWidth(), clearDrawable.getIntrinsicHeight());
		setClearIconVisible(false);
		addTextChangedListener(this);
	}

	/**
	 * 设置清空图片显示状态
	 * **/
	protected void setClearIconVisible(boolean visible){
		Drawable drawable = visible? clearDrawable : null;
		setCompoundDrawables(
				getCompoundDrawables()[0], 
				getCompoundDrawables()[1], 
				drawable,
				getCompoundDrawables()[3]);
	}
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		if(clearDrawable != null){
			if(event.getAction() == MotionEvent.ACTION_UP){
				boolean clear = event.getX() > (getWidth() - getPaddingRight() - clearDrawable.getIntrinsicWidth());
				if(clear){
					setText(null);
				}
			}
		}
		return super.onTouchEvent(event);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {

	}

	@Override
	public void afterTextChanged(Editable s) {
		setClearIconVisible(getText().length() > 0); 
	}
	
	@Override 
	public void onTextChanged(CharSequence s, int start, int count, int after) { 
	}
}