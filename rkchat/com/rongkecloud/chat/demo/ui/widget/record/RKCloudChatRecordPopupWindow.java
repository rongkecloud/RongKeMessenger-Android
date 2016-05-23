package com.rongkecloud.chat.demo.ui.widget.record;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.rongkecloud.chat.RKCloudChatMessageManager;
import com.rongkecloud.chat.demo.tools.RKCloudChatPlayAudioMsgTools;
import com.rongkecloud.chat.demo.tools.RKCloudChatTools;
import com.rongkecloud.test.R;

public class RKCloudChatRecordPopupWindow{
	private static String TAG = RKCloudChatRecordPopupWindow.class.getSimpleName();
	
	private static final int WHAT_UPDATE_RECORD_PROGRESS = 11;// 更新录音进度
	// UI 视图
	ViewGroup mRootView; // 录音布局需要在当前哪个UI上显示，即当前那个UI的根布局
	private View mView; // 录音的根View
	private RKCloudChatRecordingView mVoiceAnimView; // 显示动态录音的图片	
	private TextView mCurrDurationTV;// 当前已录音的时长
	private TextView mMaxDurationTV; // 录音最高时长
	private ProgressBar mRecordingProgressBar;// 动态显示录音进度状态进度条
	private TextView mTip;// 文字提醒
	
	// 成员变量
	private WakeLock mWakeLock;
	private Context mContext;
	
	private RKCloudChatRecorder mRecorder;
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			if(msg.what == WHAT_UPDATE_RECORD_PROGRESS){
				if (mRecorder.getCurrState() == RKCloudChatRecorder.RECORD_STATE_BUSY) {
					int iPercent = (int)Math.floor((System.currentTimeMillis()-mRecorder.getRecordStartTime())/1000.0f);
					mRecordingProgressBar.setProgress(iPercent*1000);
					mCurrDurationTV.setText(mContext.getResources().getString(R.string.rkcloud_chat_audio_recording_playtime, iPercent > mMaxDuration ? mMaxDuration : iPercent));
					mHandler.sendEmptyMessageDelayed(WHAT_UPDATE_RECORD_PROGRESS, 100);
				}
			}
		};
	};

	private PopupWindow mPopupWindow;// 弹出窗口对象
	
	private OnRKCloudChatRecordDialogDismissListener mDialogDismissListener;// 用于通知录音窗口关闭的回调监听接口
	
	private int mPopupWindowHeight, mPopupWindowWidth;// 弹出窗口的高度和宽度
	private int mMaxDuration;
	private long mFileSizeLimit;

	/**
	 * 构造函数
	 * @param context
	 * @param rootView
	 */
	public RKCloudChatRecordPopupWindow(Context context, ViewGroup rootView) {
		mContext = context;
		mRootView = rootView;
		// 获取设备的宽度和高度		
		DisplayMetrics metric = new DisplayMetrics();
		WindowManager windowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
		windowManager.getDefaultDisplay().getMetrics(metric);
		int screenWidth = metric.widthPixels;
		int screenHeight = metric.heightPixels;	
		// 设定弹出窗口尺寸
		int max = screenHeight<screenWidth ? screenHeight : screenWidth;
		mPopupWindowWidth = mPopupWindowHeight = max*3/5;
		// 获取录音最大时长和大小限制
		mMaxDuration = RKCloudChatMessageManager.getInstance(mContext).getAudioMaxDuration();
		mFileSizeLimit = RKCloudChatMessageManager.getInstance(mContext).getMediaMmsMaxSize();
		
		// 实例化录音对象，并设置监听
		mRecorder = new RKCloudChatRecorder();
		mRecorder.setOnRecordStateChangedListener(new RKCloudChatRecorder.OnRecordStateChangedListener(){
			@Override
			public void onStateChanged(int state) {
				Log.d(TAG, "onStateChanged:" + state);
				if (state == RKCloudChatRecorder.RECORD_STATE_BUSY) {
					mWakeLock.acquire(); 
					mHandler.sendEmptyMessage(WHAT_UPDATE_RECORD_PROGRESS);
					mVoiceAnimView.invalidate();
				} else {
					if (mWakeLock.isHeld())
						mWakeLock.release();
				}
			}

			@Override
			public void onError(int error) {
				processError(error);
			}
		});
		mRecorder.setFileSizeLimit(mFileSizeLimit);
		mRecorder.setMaxDuration(mMaxDuration+1);

		PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
		initViews();		
	}
	
	/**
	 * 设置监听事件
	 * @param listener
	 */
	public void setOnRecordDialogDismissListener(OnRKCloudChatRecordDialogDismissListener listener){
		mDialogDismissListener = listener;
	}
	
	/**
	 * 开始录音
	 */
	public void startRecord(String directory, String fileName) {
		// 正在录音时不执行
		if(mRecorder.getCurrState() != RKCloudChatRecorder.RECORD_STATE_IDLE){
			return;
		}
		// 停止系统正在播放的音乐
		Intent i = new Intent("com.android.music.musicservicecommand");
		i.putExtra("command", "pause");
		mContext.sendBroadcast(i);
		
		// 停止正在播放的语音
		if (RKCloudChatPlayAudioMsgTools.getInstance(mContext).isPlaying()) {
			RKCloudChatPlayAudioMsgTools.getInstance(mContext).stopMsgOfAudio();
		}
		mCurrDurationTV.setText(mContext.getResources().getString(R.string.rkcloud_chat_audio_recording_playtime, 0));
		mRecordingProgressBar.setProgress(0);
		
		mPopupWindow = new PopupWindow(mView, mPopupWindowWidth, mPopupWindowHeight);
		mPopupWindow.showAtLocation(mRootView, Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);		
		mTip.setText(mContext.getString(R.string.rkcloud_chat_audio_recording_finger_up_tip));
		// 开始录音
		mRecorder.startRecord(directory, fileName);
	}
	
	/**
	 * 关闭录音与录音对话框
	 */
	public void stopRecord() {
		if (mPopupWindow != null) {
			mPopupWindow.dismiss();
			mPopupWindow = null;
			
			mHandler.removeMessages(WHAT_UPDATE_RECORD_PROGRESS);
			mRecorder.stopRecord();
			
			if(null != mDialogDismissListener){
				mDialogDismissListener.onRecordDialogDismiss();
			}
		}
	}
	
	/**
	 * 设置录音窗口顶部的提示信息
	 */
	public void setTip(String content, boolean isShow){
		if(isShow){		
			mTip.setText(content);					
		}else{
			mTip.setText(null);
		}
	}

	/**
	 * 获取录音文件所在路径
	 * @return
	 */
	public String getRecordFile(){
		return mRecorder.getRecordFile();
	}
	
	/**
	 * 获取录音时长，单位：秒
	 * @return
	 */
	public int getRecordDuration(){
		return mRecorder.getRecordDuration();
	}
	
	/*
	 * 初始化UI 视图
	 */
	private void initViews() {
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mView = inflater.inflate(R.layout.rkcloud_chat_audio_recording_view, null);
		
		mVoiceAnimView = (RKCloudChatRecordingView) mView.findViewById(R.id.animview);
		mVoiceAnimView.setRecorder(mRecorder);
		
		mCurrDurationTV = (TextView) mView.findViewById(R.id.tv_percent);
		mMaxDurationTV = (TextView) mView.findViewById(R.id.tv_max);		
		mRecordingProgressBar = (ProgressBar) mView.findViewById(R.id.progressbar);
		
		mTip = (TextView)mView.findViewById(R.id.tip);	
		
		mMaxDurationTV.setText(mContext.getResources().getString(R.string.rkcloud_chat_audio_recording_playtime, mMaxDuration));
		
		mRecordingProgressBar.setMax(mMaxDuration*1000);
		mRecordingProgressBar.setProgress(0);
	}
	
	private void processError(int error) {
		int resId = 0;
		switch (error) {
		case RKCloudChatRecorder.ERROR_PARAMETERS:
			resId = R.string.rkcloud_chat_audio_recording_error_parameters_error;
			break;
				
		case RKCloudChatRecorder.SDCARD_ACCESS_ERROR:
			resId = R.string.rkcloud_chat_sdcard_unvalid;
			break;
		case RKCloudChatRecorder.SDCARD_HAS_FULL:
			resId = R.string.rkcloud_chat_sdcard_full;
			break;
		case RKCloudChatRecorder.INTERNAL_ERROR:
			resId = R.string.rkcloud_chat_audio_recording_error_device_busy;
			break;
			
		case RKCloudChatRecorder.BEYOND_MAX_FILESIZE:
			resId = R.string.rkcloud_chat_audio_recording_error_beyond_maxsize;
			break;
			
		case RKCloudChatRecorder.BEYOND_MAX_DURATION:
			resId = R.string.rkcloud_chat_audio_recording_error_beyond_maxduration;
			break;
			
		default:
			resId = R.string.rkcloud_chat_audio_recording_error;
			break;
		}

		if (resId > 0) {
			// 关闭弹出窗口
			stopRecord();
			RKCloudChatTools.showToastText(mContext, mContext.getString(resId));			
		}
	}

}