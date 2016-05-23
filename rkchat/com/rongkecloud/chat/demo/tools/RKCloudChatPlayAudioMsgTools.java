package com.rongkecloud.chat.demo.tools;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.rongkecloud.chat.RKCloudChatConfigManager;

/**
 * 音频相关工具类
 * 
 */
public class RKCloudChatPlayAudioMsgTools {
	private static final String TAG = RKCloudChatPlayAudioMsgTools.class.getSimpleName();	
	public static final long[] VIBRATE_PATTERN_NEW_MMS = { 200, 200, 200, 200 }; // 震动模式：新消息
		
	private static RKCloudChatPlayAudioMsgTools mInstance;
	
	private Context mContext;
	
	private Vibrator mVibrator;
	private AudioManager mAudioManager;	
	private MediaPlayer mMediaPlayer;
	
	private String mPlayingMsgSerialNum;// 记录当前正在播放语音消息的消息编号	
	
	private RKCloudChatPlayAudioMsgTools(Context context){
		mContext = context;
		mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);	
		mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		mAudioManager.setMode(AudioManager.MODE_NORMAL);
		
		mMediaPlayer = new MediaPlayer();
		mMediaPlayer.setScreenOnWhilePlaying(true);
	}
	
	public static RKCloudChatPlayAudioMsgTools getInstance(Context context){
		if(null == mInstance){
			mInstance = new RKCloudChatPlayAudioMsgTools(context);
		}
		return mInstance;
	}
	
	/**
	 * MediaPlayer是否处于播放状态
	 * @return boolean
	 */
	public boolean isPlaying(){
		return null!=mMediaPlayer ? mMediaPlayer.isPlaying() : false;
	}
	
	/**
	 * 获得一个新消息提醒时的URL播放资源文件
	 * @return
	 */
	public Uri getNotificationUri() {
		Uri notifiToneUri = null;
		String notifiStr = RKCloudChatConfigManager.getInstance(mContext).getNotifyRingUri();
		if (TextUtils.isEmpty(notifiStr)) {
			notifiToneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		} else {
			if (notifiStr.startsWith(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString())) {
				String status = Environment.getExternalStorageState();
				if (status.equals(Environment.MEDIA_MOUNTED) || status.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
					notifiToneUri = Uri.parse(notifiStr);
				} else {
					notifiToneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				}
			} else {
				notifiToneUri = Uri.parse(notifiStr);
			}
		}
		
		return notifiToneUri;
	}

	/**
	 * 播放指定的语音消息
	 */
	public void playMsgOfAudio(String msgSerialNum, String filePath){
		// 文件不存在或者消息编号不存在时返回
		if (TextUtils.isEmpty(filePath) || !new File(filePath).exists() || TextUtils.isEmpty(msgSerialNum)) {
			RKCloudChatPrint.i(TAG, "file is null or file not exist.");
			return;
		}
		
		// 如果音频正处于播放状态时返回
		if (isPlaying()) {
			return;
		}
		// 停止播放系统音乐
		Intent intent = new Intent("com.android.music.musicservicecommand");
		intent.putExtra("command", "stop");
		mContext.sendBroadcast(intent);

		try {
			if(null == mMediaPlayer){
				mMediaPlayer = new MediaPlayer();
			}
			mMediaPlayer.setDataSource(filePath);
			setAudioPlayModel(RKCloudChatConfigManager.getInstance(mContext).getVoicePlayModel());// 设置播放模式 听筒 or 扬声器	
			
			mMediaPlayer.prepare();
			mMediaPlayer.setOnErrorListener(new OnErrorListener() {
				public boolean onError(MediaPlayer mp, int what, int extra) {
					switch (what) {
					case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
						RKCloudChatPrint.e(TAG, "Error: MEDIA_ERROR_SERVER_DIED");
						mMediaPlayer.reset();
						mMediaPlayer.release();			
						mMediaPlayer = new MediaPlayer();
						mMediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
						return true;
						
					default:
						RKCloudChatPrint.e(TAG, "Error: " + what + "," + extra);
						break;
					}
					return false;
				}
			});
			// 记录正在播放的语音消息
			mPlayingMsgSerialNum = msgSerialNum;	
			mMediaPlayer.start();// 开始播放
			// 播放完成的监听事件
			mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mp) {		
					String deviceModel = Build.MODEL;
					if("5860A".equalsIgnoreCase(deviceModel)){
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {						
							e.printStackTrace();
						}
					}
					stopMsgOfAudio();
				}
			});
			
		} catch (Exception e) {
			if(null != mMediaPlayer){
				mMediaPlayer.reset();
				mMediaPlayer.release();	
				mMediaPlayer = null;
			}
			RKCloudChatPrint.e(TAG, "playMsgOfAudio()--init error, info="+e.getMessage());
		} 
	}
	
	/**
	 * 停止音频播放
	 */
	public void stopMsgOfAudio(){				
		try {
			if(isPlaying()){
				mMediaPlayer.stop();
			}
			if(null != mMediaPlayer){
				mMediaPlayer.reset();
				mMediaPlayer.release();	
				mMediaPlayer = null;
			}
			mPlayingMsgSerialNum = null;
			mAudioManager.setMode(AudioManager.MODE_NORMAL);
		} catch (Exception e) {				
		}
		// 停止震动
		if(null != mVibrator){
			mVibrator.cancel();
		}
		mAudioManager.setMode(AudioManager.MODE_NORMAL);
	}
						
	/**
	 * 获取当前正在播放的语音消息编号
	 * @return
	 */
	public String getPlayingMsgSerialNum(){
		return mPlayingMsgSerialNum;
	}
		
	/**
	 * 取得语音消息的播放时长
	 * @return
	 */
	public int getPlayingAudioDuration(){
		if (isPlaying()) {
			return mMediaPlayer.getDuration();
		} else {
			return 0;
		}
	}
	
	/**
	 * 获取当前正在播放的语音消息的时间
	 * @return
	 */
	public int getPlayingAudioPosition(){
		if (isPlaying()) {
			return mMediaPlayer.getCurrentPosition();
		} else {
			return 0;
		}
	}
		
	/*
	 * 设置语音的播放模式
	 * @param earPhone true:听筒模式 false:扬声器模式
	 */
	private void setAudioPlayModel(boolean earPhone){
		if(earPhone){
			// 听筒模式
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
			if(!isSupportStreamVoiceCall()){
				if (AudioManager.MODE_IN_CALL != mAudioManager.getMode()) {
					mAudioManager.setMode(AudioManager.MODE_IN_CALL);
				}
			}
			
		}else{
			// 扬声器模式
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		}
	}
	
	/*
	 * 是否支持MediaPlayer 设置成{@link AudioManager.STREAM_VOICE_CALL}，声音就会从耳筒里面出来，用来解决ZTE和正文手机声音外方的问题
	 * @return 如果支持返回true,其他返回false
	 */
	private boolean isSupportStreamVoiceCall() {
		String mode = Build.MODEL.replaceAll(" +", "");
		if (mode.equalsIgnoreCase("GM800") || mode.equalsIgnoreCase("ZTE-CN760") || mode.equalsIgnoreCase("ZTE-UV880")
				|| "5860A".equalsIgnoreCase(mode) || "5680A".equalsIgnoreCase(mode)
				|| "LenovoA765e".equalsIgnoreCase(mode) || "huaweig520-0000".equalsIgnoreCase(mode)) {
			return false;
		}
		return true;
	}

}
