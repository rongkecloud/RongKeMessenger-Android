package com.rongkecloud.chat.demo.ui.widget.record;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.rongkecloud.test.R;

/**
 * 本类是录音动画的实现 原理：在imageView上画一个矩形，根据录音的最大振幅来计算 矩形的高度
 */
public class RKCloudChatRecordingView extends ImageView {
	private static final long ANIMATION_INTERVAL = 100;

	private Paint mPaint;
	private RKCloudChatRecorder mRecorder;
	private int mMaxAmplitude = 32768;
	private Rect progressRect = new Rect();
	
	public RKCloudChatRecordingView(Context context) {
		super(context);
		init(context);		
	}

	public RKCloudChatRecordingView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);		
	}
	
	private void init(Context context) {
		setBackgroundResource(R.drawable.rkcloud_chat_ic_audio_recording);
		
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setColor(Color.rgb(52, 52, 52));
		mPaint.setAlpha(0x60);
	}

	public void setRecorder(RKCloudChatRecorder recorder) {
		mRecorder = recorder;
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int iAmplitude = 0;
		if (null != mRecorder) {
			iAmplitude = mRecorder.getMaxAmplitude();
			iAmplitude = iAmplitude>mMaxAmplitude ? mMaxAmplitude : iAmplitude;
		}

		float mPrecent = iAmplitude*getHeight()/mMaxAmplitude;

		progressRect.set(0, 0, getWidth(), (int) (getHeight()-mPrecent));
		canvas.drawRect(progressRect, mPaint);
		
		if (null!=mRecorder && mRecorder.getCurrState()==RKCloudChatRecorder.RECORD_STATE_BUSY){
			postInvalidateDelayed(ANIMATION_INTERVAL);
		}
	}
}