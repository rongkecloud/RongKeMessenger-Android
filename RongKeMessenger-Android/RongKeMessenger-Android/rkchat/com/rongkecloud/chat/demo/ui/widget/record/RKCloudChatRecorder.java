package com.rongkecloud.chat.demo.ui.widget.record;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.text.TextUtils;
import android.util.Log;
import com.rongkecloud.sdkbase.RKCloudLog;

import java.io.File;
import java.io.IOException;

public class RKCloudChatRecorder
{
	static final String TAG = RKCloudChatRecorder.class.getSimpleName();
	// 声音采样率
	private static final int SAMPLE_BITRATE_3GPP = 8000;
	// 编码采样率
	private static final int ENCODE_BITRATE_3GPP = 12000;

	// 录音状态常量值
	static final int RECORD_STATE_IDLE = 1;// 空闲
	static final int RECORD_STATE_BUSY = 2;// 正在录音

	// 错误常量值定义
	static final int ERROR_DEFAULT = 0;// 未知错误
	static final int ERROR_PARAMETERS = 1;// 参数错误
	static final int SDCARD_ACCESS_ERROR = 2;// SD卡访问异常
	static final int SDCARD_HAS_FULL = 3;// SD卡已满
	static final int INTERNAL_ERROR = 4;// 内部错误
	static final int BEYOND_MAX_FILESIZE = 5;// 超出文件最大限制
	static final int BEYOND_MAX_DURATION = 6;// 超出最大时长

	// 录音文件相关属性值
	private long mRecordStartTime;// 记录录音开始的时间点
	private File mFile;// 存储的录音文件
	private String mExtension;// 文件后缀

	// 录音器相关属性值
	private MediaRecorder mRecorder = null;// 录音器
	private int mAudioOutputFormat; // 文件输出格式
	private int mAudioEncoder; // 音频文件编码
	private int mAudioSourceType = MediaRecorder.AudioSource.MIC;// 声音来源，默认是从麦克风录入
	private int mSampleRecordBit = -1;
	private int mEncodeRecordBit = -1;

	private long mFileSizeLimit;
	private int mMaxDuration;

	// 成员变量定义
	private int mCurrState = RECORD_STATE_IDLE;// 当前的录音状态，1:空闲 2: 录音中
	private OnRecordStateChangedListener mOnStateChangedListener = null;// 监听器接口

	interface OnRecordStateChangedListener
	{
		/**
		 * 录音状态发生变化
		 *
		 * @param state
		 *            当前录音状态值
		 */
		public void onStateChanged(int state);

		/**
		 * 录音出现异常
		 *
		 * @param error
		 */
		public void onError(int error);
	}

	/*
	 * 构造函数 <p>为保证录制的音频文件在多平台间正常播放，请尽量采用默认提供的录音格式 vivo 和 oppo
	 * 手机录音不支持3gp格式，如果是这两种手机，应使用AUDIO_TYPE_AMR格式，其余的可直接使用默认的3gp格式即可
	 */
	public RKCloudChatRecorder()
	{
		mAudioOutputFormat = MediaRecorder.OutputFormat.THREE_GPP;
		mAudioEncoder = MediaRecorder.AudioEncoder.AAC;
		mExtension = ".3gp";
		mSampleRecordBit = SAMPLE_BITRATE_3GPP;
		mEncodeRecordBit = ENCODE_BITRATE_3GPP;

		mRecordStartTime = 0;
		mCurrState = RECORD_STATE_IDLE;
	}

	/**
	 * 设置监听器以便回调状态的改变与错误的提示
	 *
	 * @param listener
	 */
	public void setOnRecordStateChangedListener(OnRecordStateChangedListener listener)
	{
		mOnStateChangedListener = listener;
	}

	/*
	 * 录音出错的回调
	 * @param error
	 */
	private void setError(int error)
	{
		if (null != mRecorder)
		{
			mRecorder.reset();
			mRecorder.release();
			mRecorder = null;
		}

		if (error != BEYOND_MAX_FILESIZE && error != BEYOND_MAX_DURATION)
		{
			mFile = null;
		}
		mOnStateChangedListener.onError(error);
		// 设置录音状态
		if (mCurrState != RECORD_STATE_IDLE)
		{
			mCurrState = RECORD_STATE_IDLE;
			mOnStateChangedListener.onStateChanged(mCurrState);
		}
	}

	/**
	 * 获取录音最大振幅
	 *
	 * @return
	 */
	public int getMaxAmplitude()
	{
		// 如果录音未开始，则返回
		if (mCurrState == RECORD_STATE_IDLE)
		{
			return 0;
		}

		return null != mRecorder ? mRecorder.getMaxAmplitude() : 0;
	}

	/**
	 * 记录录音开始的时间点，为毫秒级的时间戳
	 *
	 * @return
	 */
	public long getRecordStartTime()
	{
		// 如果录音未开始，则返回0
		if (mCurrState == RECORD_STATE_IDLE)
		{
			return 0;
		}
		return mRecordStartTime;
	}

	/**
	 * 获取录音文件所在路径
	 *
	 * @return
	 */
	public String getRecordFile()
	{
		if (null == mFile)
		{
			return null;
		}
		return mFile.getAbsolutePath();
	}

	/**
	 * 获取录音时长，单位：秒
	 *
	 * @return
	 */
	public int getRecordDuration()
	{
		if (null == mFile)
		{
			return -1;
		}

		int duration = -1;
		MediaPlayer mediaPlayer = null;
		try
		{
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setDataSource(mFile.getAbsolutePath());
			mediaPlayer.prepare();
			duration = (int) Math.floor(mediaPlayer.getDuration() / 1000.0f);
		}
		catch (Exception e)
		{
			RKCloudLog.d(TAG, "get media file's duration exception, info=" + e.getMessage());
		}
		finally
		{
			if (null != mediaPlayer)
			{
				mediaPlayer.reset();
				mediaPlayer.release();
				mediaPlayer = null;
			}
		}
		return duration;
	}

	/**
	 * 获取录音当前状态
	 */
	public int getCurrState()
	{
		return mCurrState;
	}

	/**
	 * 设置文件大小限制
	 *
	 * @param limit
	 *            单位：字节
	 */
	public void setFileSizeLimit(long limit)
	{
		mFileSizeLimit = limit;
	}

	/**
	 * 设置最大时长，单位：秒
	 *
	 * @param duration
	 */
	public void setMaxDuration(int duration)
	{
		mMaxDuration = duration;
	}

	/**
	 * 开始录音
	 *
	 * @param
	 * @return
	 */
	public void startRecord(String directory, String fileName)
	{
		// 正在录音时操作失败
		if (mCurrState != RECORD_STATE_IDLE)
		{
			setError(ERROR_DEFAULT);
			return;
		}
		// 后缀名不存在时，抛出异常
		if (TextUtils.isEmpty(mExtension))
		{
			setError(ERROR_PARAMETERS);
			return;
		}

		// 初始化部分参数
		mFile = null;
		// 初始化录音文件保存路径
		try
		{
			initRecordFileSavePath(directory, fileName);
		}
		catch (IOException e)
		{
			setError(SDCARD_ACCESS_ERROR);
			return;
		}

		// 初始化录音文件时长
		mRecorder = new MediaRecorder();// 实例化录音器对象
		mRecorder.setAudioSource(mAudioSourceType);// 设置声音来源，如：麦克风
		mRecorder.setOutputFormat(mAudioOutputFormat); // 设置所录制的音频文件的输出格式
		mRecorder.setAudioEncoder(mAudioEncoder);// 设置所录制的声音的编码格式
		mRecorder.setAudioSamplingRate(mSampleRecordBit);// 设置所录制的声音的采样率
		mRecorder.setAudioEncodingBitRate(mEncodeRecordBit);// 设置所录制的声音的编码位率
		mRecorder.setAudioChannels(1);// 设置录制的音频通道数

		if (mFileSizeLimit > 0)
		{
			mRecorder.setMaxFileSize(mFileSizeLimit);
		}

		if (mMaxDuration > 0)
		{
			mRecorder.setMaxDuration(mMaxDuration * 1000);// 设置录制的最长持续时间(以ms为单位)
		}
		mRecorder.setOutputFile(mFile.getAbsolutePath());// 设置录制的音频文件的保存路径
		// 注册一个用于记录录制时出现的错误的监听器
		mRecorder.setOnErrorListener(new OnErrorListener()
		{
			@Override
			public void onError(MediaRecorder mr, int what, int extra)
			{
				Log.d(TAG, "onError what=" + what);
			}
		});
		// 注册一个用于记录录制时出现的信息事件
		mRecorder.setOnInfoListener(new OnInfoListener()
		{
			@Override
			public void onInfo(MediaRecorder mr, int what, int extra)
			{
				Log.d(TAG, "onInfo what=" + what);
				if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED == what)
				{
					setError(BEYOND_MAX_DURATION);
				}
				else if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED == what)
				{
					setError(BEYOND_MAX_FILESIZE);
				}
			}
		});

		// 记录相关的录音值
		mRecordStartTime = System.currentTimeMillis();

		try
		{
			mRecorder.prepare(); // 准备录音
			mRecorder.start();// 开始录音
			// 设置录音状态
			mCurrState = RECORD_STATE_BUSY;
			mOnStateChangedListener.onStateChanged(mCurrState);

		}
		catch (Exception e)
		{
			setError(INTERNAL_ERROR);
			// 如果生成录音文件，则删除
			if (mFile != null && mFile.exists())
			{
				mFile.delete();
			}
			mFile = null;
		}
	}

	/**
	 * 停止录音
	 */
	public void stopRecord()
	{
		if (mRecorder == null || mCurrState == RECORD_STATE_IDLE)
		{
			return;
		}

		// 释放录音器资源
		try
		{
			mRecorder.stop();
			mRecorder.reset();
			mRecorder.release();
			mRecorder = null;
		}
		catch (Exception e)
		{

		}

		// 设置录音状态
		mCurrState = RECORD_STATE_IDLE;
		mOnStateChangedListener.onStateChanged(mCurrState);
	}

	/*
	 * 初始化录音文件路径
	 */
	private void initRecordFileSavePath(String directory, String fileName) throws IOException
	{
		String filePath = directory;
		if (!directory.endsWith("/"))
		{
			filePath += "/";
		}
		filePath += fileName + mExtension;
		File parentDir = new File(filePath).getParentFile();
		// 如果目录不存在时创建
		if (!parentDir.exists())
		{
			if (!parentDir.mkdirs())
			{
				throw new IOException();
			}
		}

		mFile = new File(filePath);
		// 文件已存在时先删除再创建
		if (mFile.exists())
		{
			if (!mFile.delete())
			{
				throw new IOException();
			}
		}
		// 创建文件
		if (!mFile.createNewFile())
		{
			throw new IOException();
		}
		Log.d(TAG, "initRecordFileSavePath: filename=" + mFile.getAbsolutePath());
	}
}