package com.rongkecloud.chat.demo.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

public class RKCloudChatManageGridView extends GridView{
	private Context mContext;
	
	public RKCloudChatManageGridView(Context context) {
		super(context);	
		mContext = context;
		init(null);
	}
	
	public RKCloudChatManageGridView(Context context, AttributeSet attrs) {
		super(context, attrs);	
		mContext = context;
		init(attrs);
	}
	
	public RKCloudChatManageGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		init(attrs);
	}
	
	private void init(AttributeSet attrs){
		setNumColumns(4);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE>>2, MeasureSpec.AT_MOST);
		super.onMeasure(widthMeasureSpec, expandSpec);		
	}
}
