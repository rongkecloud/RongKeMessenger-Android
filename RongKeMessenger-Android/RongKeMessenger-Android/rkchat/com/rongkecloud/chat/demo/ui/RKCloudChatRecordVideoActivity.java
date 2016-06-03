package com.rongkecloud.chat.demo.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.rongkecloud.chat.RKCloudChatMessageManager;
import com.rongkecloud.chat.demo.RKCloudChatConstants;
import com.rongkecloud.chat.demo.entity.RKCloudChatSelectVideoItem;
import com.rongkecloud.chat.demo.tools.RKCloudChatTools;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.test.R;
import com.rongkecloud.test.utility.FileLog;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class RKCloudChatRecordVideoActivity extends RKCloudChatBaseActivity implements Callback
{
	private static final String TAG = RKCloudChatRecordVideoActivity.class.getSimpleName();
	static final String INTENT_RETURN_KEY_RECORDEDVIDEO = "intent_return_key_recordedvideo";

	private static final int DEFAULT_FRAMERATE = 15;// 默认帧率
	private static final int DEFAULT_PREVIDE_WIDTH = 640;
	private static final int DEFAULT_PREVIDE_HEIGHT = 480;

	private static final int MSG_WHAT_UPDATE_RECORDING_TIME = 1;
	private static final int UPDATE_RECORDING_INTERVALTIME = 100;// //
																	// 更新录制时长的间隔时间，单位：毫秒
	// UI元素
	// 取消
	private TextView mCancel;
	private VideoView mVideoView;
	// 开启按钮
	private RelativeLayout mStartLayout;
	// 停止按钮
	private RelativeLayout mStopLayout;
	// 切换摄像头按钮
	private ImageView mSwitchCameraBtn;
	// 录音过程中的进度
	private LinearLayout mRecordingLayout;
	private TextView mRecordingTV, mMaxRecordingTV;
	private ProgressBar mProgressbar;

	// 成员变量
	private PowerManager.WakeLock mWakeLock;
	private MediaRecorder mMediarecorder;// 录制视频的类
	private SurfaceHolder mSurfaceHolder;

	private Camera mCamera;
	private int mCameraId = -1;// 摄像头(前置 or 后置)
	private boolean mEnableSwitchCamera = true;// 是否允许切换摄像头
	private int mCameraDegree;

	private int mPreviewWidth = -1;// 预览的宽
	private int mPreviewHeight = -1;// 预览的高
	private int mFrameRate = -1;// 帧率

	private String mFilePath;
	private MediaScannerConnection mMsc;
	private long mFileSizeLimit;
	private int mMaxDuration;// 最大时长
	private int mRecordingDuration;// 录音时长，单位：毫秒
	private boolean mRecording = false;// 是否在录音中

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rkcloud_chat_recordvideo);
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
		{
			// 无摄像头设备
			RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_videorecording_unhave_camera));
			finish();
			return;
		}
		else
		{
			mCameraId = CameraInfo.CAMERA_FACING_BACK;// 默认为后置摄像头
			mEnableSwitchCamera = Camera.getNumberOfCameras() > 1;
		}

		initUIAndListeners();
		initData();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		if (mRecording)
		{
			stopRecord();
		}

		if (mWakeLock != null)
		{
			mWakeLock.release();
			mWakeLock = null;
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		mSurfaceHolder = holder;
		setCameraId();
		if (null == mCamera)
		{
			finish();
			return;
		}
		// 获取视频支持的帧率
		boolean hasSupportRate = false;
		List<Integer> supportedFrameRates = mCamera.getParameters().getSupportedPreviewFrameRates();
		if (null != supportedFrameRates && supportedFrameRates.size() > 0)
		{
			Collections.sort(supportedFrameRates);
			Log.d(TAG, "supported framerates:" + supportedFrameRates.toString());
			for (int i = 0; i < supportedFrameRates.size(); i++)
			{
				int supportRate = supportedFrameRates.get(i);
				if (DEFAULT_FRAMERATE == supportRate)
				{
					hasSupportRate = true;
					break;
				}
			}

			if (hasSupportRate)
			{
				mFrameRate = DEFAULT_FRAMERATE;
			}
			else
			{
				// 帧率取中间
				int frameRateLocation = supportedFrameRates.size() / 2;
				if (supportedFrameRates.size() % 2 == 0)
				{
					frameRateLocation = frameRateLocation - 1;
				}
				mFrameRate = supportedFrameRates.get(frameRateLocation);
			}

			Log.d(TAG, "selected supported framerates:" + mFrameRate);
		}

		// 获取摄像头所有支持的分辨率
		List<Camera.Size> resolutionList = mCamera.getParameters().getSupportedPreviewSizes();
		if (null != resolutionList && resolutionList.size() > 0)
		{
			boolean hasSize = false;
			for (Camera.Size size : resolutionList)
			{
				if (size.width == DEFAULT_PREVIDE_WIDTH && size.height == DEFAULT_PREVIDE_HEIGHT)
				{
					hasSize = true;
					break;
				}
			}
			// 如果不支持则设为中间的那个
			if (hasSize)
			{
				mPreviewWidth = DEFAULT_PREVIDE_WIDTH;
				mPreviewHeight = DEFAULT_PREVIDE_HEIGHT;
			}
			else
			{
				int sizeLocation = resolutionList.size() / 2;
				if (resolutionList.size() % 2 == 0)
				{
					sizeLocation = sizeLocation - 1;
				}
				Camera.Size previewSize = resolutionList.get(sizeLocation);
				mPreviewWidth = previewSize.width;
				mPreviewHeight = previewSize.height;
			}
			Log.d(TAG, "selected resolution width:" + mPreviewWidth + ", height=" + mPreviewHeight);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{
		mSurfaceHolder = holder;
	}

	private void initUIAndListeners()
	{
		mCancel = (TextView) findViewById(R.id.cancel);
		mVideoView = (VideoView) findViewById(R.id.videoview);
		mStartLayout = (RelativeLayout) findViewById(R.id.layout_start);
		mStopLayout = (RelativeLayout) findViewById(R.id.layout_stop);
		mSwitchCameraBtn = (ImageView) findViewById(R.id.switchcamera);

		mRecordingLayout = (LinearLayout) findViewById(R.id.layout_progressbar);
		mRecordingTV = (TextView) findViewById(R.id.recordingduration);
		mMaxRecordingTV = (TextView) findViewById(R.id.recordingmxduration);
		mProgressbar = (ProgressBar) findViewById(R.id.progressbar);
		// 设置按钮的显示与隐藏状态
		mCancel.setVisibility(View.VISIBLE);
		mStartLayout.setVisibility(View.VISIBLE);
		mStopLayout.setVisibility(View.GONE);
		mSwitchCameraBtn.setVisibility(mEnableSwitchCamera ? View.VISIBLE : View.GONE);

		mStartLayout.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startRecord();
			}
		});
		mStopLayout.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				stopRecord();
			}
		});
		mSwitchCameraBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (mCameraId == CameraInfo.CAMERA_FACING_FRONT)
				{
					mCameraId = CameraInfo.CAMERA_FACING_BACK;
				}
				else
				{
					mCameraId = CameraInfo.CAMERA_FACING_FRONT;
				}
				setCameraId();
				mSwitchCameraBtn.setSelected(!mSwitchCameraBtn.isSelected());
			}
		});

		mCancel.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				FileLog.e(TAG, "cancel on click");
				releaseRecoder();
				releaseCamera();
				finish();
			}
		});
	}

	@SuppressLint("NewApi")
	private void initData()
	{
		mFileSizeLimit = RKCloudChatMessageManager.getInstance(this).getMediaMmsMaxSize();
		mMaxDuration = RKCloudChatMessageManager.getInstance(this).getVideoMaxDuration();
		mMaxRecordingTV.setText(getString(R.string.rkcloud_chat_audio_recording_playtime, mMaxDuration));
		mProgressbar.setMax(mMaxDuration * 1000);
		mProgressbar.setProgress(0);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
		mWakeLock.acquire();

		mSurfaceHolder = mVideoView.getHolder();// 取得holder
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	@SuppressLint("NewApi")
	private synchronized void setCameraId()
	{
		if (null != mCamera)
		{
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}

		try
		{
			if (android.os.Build.VERSION.SDK_INT > 8)
			{
				mCamera = Camera.open(mCameraId);
			}
			else
			{
				mCamera = Camera.open();
			}

			mCamera.setPreviewDisplay(mSurfaceHolder);
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(mCameraId, info);

			Camera.Parameters parameters = mCamera.getParameters();
			parameters.set("orientation", "portrait");// 竖屏显示
			mCamera.setDisplayOrientation(90);
			mCamera.setParameters(parameters);
			mCamera.startPreview();

			if (mCameraId == CameraInfo.CAMERA_FACING_FRONT)
			{
				mCameraDegree = 270;
			}
			else
			{
				mCameraDegree = 90;
			}
		}
		catch (Exception e)
		{
			// 显示确认对话框
			AlertDialog.Builder dialog = new AlertDialog.Builder(this).setTitle(R.string.rkcloud_chat_tip).setMessage(R.string.rkcloud_chat_videorecording_openfailed).setCancelable(false)
					.setPositiveButton(R.string.rkcloud_chat_btn_confirm, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							finish();
						}
					});
			dialog.create().show();
			releaseCamera();
		}
	}

	private void sendRecoder()
	{
		// 判断文件大小
		File file = new File(mFilePath);
		long size = 0;
		if (file.exists() && file.isFile())
		{
			size = file.length();
		}

		if (size <= 0 || size > mFileSizeLimit)
		{
			RKCloudChatTools.showToastText(RKCloudChatRecordVideoActivity.this, getString(R.string.rkcloud_chat_selectfile_beyond_filesize, mFileSizeLimit / 1024 / 1024));
			return;
		}

		// 显示确认对话框
		AlertDialog.Builder dialog = new AlertDialog.Builder(RKCloudChatRecordVideoActivity.this);
		dialog.setTitle(R.string.rkcloud_chat_tip);
		dialog.setMessage(R.string.rkcloud_chat_videorecording_send_confirm);
		dialog.setCancelable(false);
		dialog.setNegativeButton(R.string.rkcloud_chat_btn_cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				releaseCamera();
				releaseRecoder();
				finish();
			}
		});
		dialog.setPositiveButton(R.string.rkcloud_chat_btn_confirm, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				mMsc = new MediaScannerConnection(RKCloudChatRecordVideoActivity.this, new MediaScannerConnection.MediaScannerConnectionClient()
				{
					@Override
					public void onMediaScannerConnected()
					{
						mMsc.scanFile(mFilePath, "video/*");
					}

					@Override
					public void onScanCompleted(String path, Uri uri)
					{
						mMsc.disconnect();

						Cursor cursor = null;
						RKCloudChatSelectVideoItem obj = null;
						try
						{
							cursor = getContentResolver().query(uri, null, null, null, null);
							if (cursor.getCount() > 0)
							{
								cursor.moveToFirst();
								obj = new RKCloudChatSelectVideoItem();
								obj.setId(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)));
								obj.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)));
								obj.setFilePath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)));
								obj.setFileSize(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)));
								obj.setDuration(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)));
							}
						}
						catch (Exception e)
						{

						}
						finally
						{
							if (null != cursor)
							{
								cursor.close();
							}
						}

						if (null != mWakeLock)
						{
							mWakeLock.release();
							mWakeLock = null;
						}

						if (null != mCamera)
						{
							mCamera.stopPreview();
							mCamera.release();
							mCamera = null;
						}
						mVideoView = null;
						mSurfaceHolder = null;
						mMediarecorder = null;

						if (null != obj)
						{
							Intent intent = new Intent();
							intent.putExtra(INTENT_RETURN_KEY_RECORDEDVIDEO, obj);
							setResult(RESULT_OK, intent);
						}
						finish();
					}
				});
				mMsc.connect();
			}
		});
		dialog.show();
	}

	@SuppressLint("NewApi")
	private void startRecord()
	{
		if (mRecording)
		{
			return;
		}
		if (null == mCamera)
		{
			setCameraId();
		}
		mCamera.unlock();
		mMediarecorder = new MediaRecorder();// 创建mediarecorder对象
		mMediarecorder.setCamera(mCamera);

		mMediarecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);// 需要放在setOutputFormat之前
		mMediarecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);// 设置录制视频源为Camera（相机），需要放在setOutputFormat之前
		mMediarecorder.setOrientationHint(mCameraDegree);
		mMediarecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);// 设置录制完成后视频的封装格式THREE_GPP为3gp，MPEG_4为mp4

		mMediarecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);// 设置录制的音频编码，需要放在setOutputFormat之后
		mMediarecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);// 设置录制的视频编码，需要放在setOutputFormat之后
		mMediarecorder.setVideoSize(mPreviewWidth, mPreviewHeight);// 设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错
		mMediarecorder.setVideoEncodingBitRate(600 * 1024);// 设置视频的比特率
		if (mFrameRate != -1)
		{
			mMediarecorder.setVideoFrameRate(mFrameRate);// 设置视频的帧速率，必须放在设置编码和格式的后面，否则报错
		}
		mMediarecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

		// 设置文件大小限制
		if (mFileSizeLimit > 0)
		{
			mMediarecorder.setMaxFileSize(mFileSizeLimit);
		}
		// 设置时长限制
		if (mMaxDuration > 0)
		{
			mMediarecorder.setMaxDuration((mMaxDuration + 1) * 1000);
		}

		// 设置视频文件输出的路径
		RKCloudChatTools.createDirectory(RKCloudChatConstants.MMS_TEMP_PATH);
		mFilePath = String.format("%svideo_%d.mp4", RKCloudChatConstants.MMS_TEMP_PATH, System.currentTimeMillis());
		mMediarecorder.setOutputFile(mFilePath);
		mMediarecorder.setOnErrorListener(new OnErrorListener()
		{
			@Override
			public void onError(MediaRecorder mr, int what, int extra)
			{
				Log.w(TAG, "startRecord--onError--what=" + what);
				stopRecord();
			}
		});
		mMediarecorder.setOnInfoListener(new OnInfoListener()
		{
			@Override
			public void onInfo(MediaRecorder mr, int what, int extra)
			{
				Log.d(TAG, "startRecord--onInfo--what=" + what);
				if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED == what)
				{
					RKCloudChatTools.showToastText(RKCloudChatRecordVideoActivity.this, getString(R.string.rkcloud_chat_audio_recording_error_beyond_maxduration));
					stopRecord();
				}
				else if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED == what)
				{
					RKCloudChatTools.showToastText(RKCloudChatRecordVideoActivity.this, getString(R.string.rkcloud_chat_audio_recording_error_beyond_maxsize));
					stopRecord();
				}
			}
		});

		try
		{
			// 准备录制
			mMediarecorder.prepare();
			// 开始录制
			mMediarecorder.start();

			mRecordingLayout.setVisibility(View.VISIBLE);
			mRecordingDuration = 0;
			showRecordingProgress();
			mUiHandler.sendEmptyMessageDelayed(MSG_WHAT_UPDATE_RECORDING_TIME, UPDATE_RECORDING_INTERVALTIME);

			RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_videorecording_start));
			mStartLayout.setVisibility(View.GONE);
			mStopLayout.setVisibility(View.VISIBLE);
			mSwitchCameraBtn.setVisibility(View.GONE);
			mCancel.setVisibility(View.GONE);

			mRecording = true;
		}
		catch (Exception e)
		{
			Log.e(TAG, "startRecord--exception info=" + e.getMessage());
			stopRecord();
		}
	}

	private void stopRecord()
	{
		if (mMediarecorder != null)
		{
			// 设置后不会崩
			mMediarecorder.setOnErrorListener(null);
			mMediarecorder.setPreviewDisplay(null);
			// 停止录制
			mMediarecorder.stop();
			// 释放资源
			releaseRecoder();
		}

		try
		{
			if (null != mCamera)
			{
				mCamera.reconnect();
			}
		}
		catch (IOException e)
		{
			RKCloudChatTools.showToastText(this, "reconect fail");
		}

		mRecordingLayout.setVisibility(View.GONE);
		mRecordingDuration = 0;
		mUiHandler.removeMessages(MSG_WHAT_UPDATE_RECORDING_TIME);

		mStartLayout.setVisibility(View.VISIBLE);
		mStopLayout.setVisibility(View.GONE);
		mCancel.setVisibility(View.VISIBLE);
		mSwitchCameraBtn.setVisibility(mEnableSwitchCamera ? View.VISIBLE : View.GONE);

		mRecording = false;

		sendRecoder();
	}

	private void releaseCamera()
	{
		if (mMediarecorder != null)
		{
			mMediarecorder.release();
			mMediarecorder = null;
		}
	}

	private void releaseRecoder()
	{
		try
		{
			if (mCamera != null)
			{
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
			}
		}
		catch (Exception e)
		{
		}
	}

	private void showRecordingProgress()
	{
		int iPercent = (int) Math.floor(mRecordingDuration / 1000.0f);
		mRecordingTV.setText(getString(R.string.rkcloud_chat_audio_recording_playtime, mMaxDuration > 0 && mMaxDuration < iPercent ? mMaxDuration : iPercent));
		mProgressbar.setProgress(mRecordingDuration);
	}

	@Override
	public void processResult(Message msg)
	{
		if (MSG_WHAT_UPDATE_RECORDING_TIME == msg.what)
		{
			mRecordingDuration += UPDATE_RECORDING_INTERVALTIME;
			showRecordingProgress();
			if (mRecordingDuration < mMaxDuration * 1000)
			{
				mUiHandler.sendEmptyMessageDelayed(MSG_WHAT_UPDATE_RECORDING_TIME, UPDATE_RECORDING_INTERVALTIME);
			}
			else
			{
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_videorecording_duration_exceed));
				stopRecord();
			}
		}
	}
}
