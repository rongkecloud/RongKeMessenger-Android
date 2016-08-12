package com.rongke.cloud.av.demo.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.rongke.cloud.av.demo.RKCloudAVContactManager;
import com.rongke.cloud.av.demo.RKCloudAVDemoManager;
import com.rongke.cloud.av.demo.RKCloudAVUiHandlerMessage;
import com.rongke.cloud.av.demo.entity.RKCloudAVContact;
import com.rongke.cloud.av.demo.tools.CpuMonitor;
import com.rongke.cloud.av.demo.tools.RKCloudAVUtils;
import com.rongke.jni.RongKeJNI;
import com.rongke.jni.interfaces.RKCallStatisticalCallBack;
import com.rongkecloud.av.RKCloudAVCallInfo;
import com.rongkecloud.av.RKCloudAVCallState;
import com.rongkecloud.test.R;
import com.rongkecloud.test.ui.widget.RoundedImageView;
import org.webrtc.StatsReport;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.*;

public class RKCloudAVDemoActivity extends Activity implements OnClickListener, SensorEventListener{
	private static final String TAG = RKCloudAVDemoActivity.class.getSimpleName();

	public static final String INTENT_KEY_OUTCALL_ACCOUNT = "intent_key_outcall_account";// 外呼时的账号
	public static final String INTENT_KEY_INCALL_ACCOUNT = "intent_key_incall_account";// 呼入时的账号

	private static final int DELAY_EXECUTE_DIMSCREEN_TIME = 1000;// 1s

	// UI组件
	private View mRootView;// 根布局
	private View mCallBgView;

	private RelativeLayout mUserInfoLayout;
	private RoundedImageView mHeaderImage;// 头像
	private TextView mShowNameTV;// 名称
	private TextView mCallStatusTV;// 通话状态

	private RelativeLayout mRemoteVideoLayout;

	private TextView mTimer;// 记时器

	private ImageView mHideInAudioBtn, mHideInVideoBtn;// 隐藏按钮
	private LinearLayout mAllBtnsLayout;// 按钮区域
	private LinearLayout mCallingBtnsLayout;// 通话中时的按钮区域
	// 静音按钮
	private LinearLayout mMuteLayout;
	private ImageView mMuteBtn;
	// 免提按钮
	private LinearLayout mHandFreeLayout;
	private ImageView mHandFreeBtn;
	// 切换摄像头
	private LinearLayout mSwitchCameraLayout;
	private ImageView mSwitchCameraBtn;
	// 切为语音聊天
	private LinearLayout mToAudioLayout;
	private ImageView mToAudioBtn;
	// 挂断按钮
	private LinearLayout mHangupLayout;
	private ImageView mHangupBtn;
	private TextView mHangupTV;
	// 接听按钮
	private LinearLayout mAnswerLayout;
	private ImageView mAnswerBtn;

	// 成员变量
	private RKCloudAVContact mContactObj;
	private RKCloudAVDemoManager mAVManager;
	private boolean mClickRootView = false;// 是否点击了根目录 true:视频聊天时隐藏部分组件

	private Handler mUiHandler;
	private AudioManager mAudioManager;
	// 距离感应相关的内容
	private SensorManager mSensorManager;
	private Sensor mSensor = null;
	private float mDistance;
	private boolean mIsSensorTimerRunning = false;// 距离感应使用的定时器是否在运行

	private TextView mAVTextView1;
	private TextView mAVTextView2;
	private TextView mAVTextView3;
	private TextView mAVTextView4;
	private TextView mAVTextView5;

	private CpuMonitor cpuMonitor = new CpuMonitor();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                |WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.rkcloud_av_call);
		initUiAndListeners();
		// 初始化变量内容
		mAVManager = RKCloudAVDemoManager.getInstance(this);
		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		mUiHandler = new UiHandler(this);
		mAVManager.setCallUiShowVideoLayout(mRemoteVideoLayout);
		RKCloudAVCallInfo avCallInfo = mAVManager.getAVCallInfo();
		if(null == avCallInfo){
			mAVManager.hideOutCallNotification();
			mAVManager.hideInCallNotification();
			finish();
			return;
		}
		// intent解析
		resolveIntent(getIntent());
		// 组件初始化
		showWidgets();
		// 如果是在通话中则更新通话时长
		if(RKCloudAVCallState.AV_CALL_STATE_ANSWER == mAVManager.getAVCallInfo().callState){
			updateCallTime();
		}else{
			// 设置音量键调节的音量类型
			setVolumeControlStream(AudioManager.STREAM_RING);
		}

		mAVTextView1 = (TextView) findViewById(R.id.call_text1);
		mAVTextView2 = (TextView) findViewById(R.id.call_text2);
		mAVTextView3 = (TextView) findViewById(R.id.call_text3);
		mAVTextView4 = (TextView) findViewById(R.id.call_text4);
		mAVTextView5 = (TextView) findViewById(R.id.call_text5);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		resolveIntent(intent);
	};

	@Override
	protected void onResume() {
		super.onResume();
		mAVManager.bindUiHandler(mUiHandler);
		RKCloudAVContactManager.getInstance(this).bindUiHandler(mUiHandler);
		mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
		mAVManager.setCallUiIsShow(true);

		RKCloudAVCallInfo info = mAVManager.getAVCallInfo();
		if(null != info && info.callState == RKCloudAVCallState.AV_CALL_STATE_ANSWER){
			RongKeJNI.getInstance().getStatisticalData(true, new RKCallStatisticalCallBack() {
				@Override
				public void onStatisticalDataReciver(StatsReport[] reports) {
					updateEncoderStatistics(reports);
				}
			}, 1000);
		}
	};

    private static final Set<String> send = new HashSet<>();
    private static final Set<String>reciver = new HashSet<>();
    private static final Set<String>connStat = new HashSet<>();
    private static final Set<String>rates = new HashSet<>();

    private boolean isShowSendStat(String name)
    {
        if(send.size() == 0)
        {
            send.add("codecImplementationName");
            send.add("mediaType");
            send.add("ssrc");
            send.add("transportId");
            send.add("AdaptationChanges");
            send.add("BandwidthLimitedResolution");
            send.add("AvgEncodeMs");
            send.add("CpuLimitedResolution");
            send.add("EncodeUsagePercent");
            send.add("FirsReceived");
            send.add("NacksReceived");
            send.add("PlisReceived");
            send.add("Rtt");
            send.add("TrackId");
            send.add("ViewLimitedResolution");
        }
        return !send.contains(name);
    }

    private boolean isShowReciveStat(String name)
    {
        if(reciver.size() == 0)
        {
            reciver.add("codecImplementationName");
            reciver.add("mediaType");
            reciver.add("ssrc");
            reciver.add("transportId");
            reciver.add("CaptureStartNtpTimeMs");
            reciver.add("FirsSent");
            reciver.add("TrackId");
            reciver.add("TargetDelayMs");
            reciver.add("RenderDelayMs");
            reciver.add("PlisSent");
            reciver.add("NacksSent");
            reciver.add("MinPlayoutDelayMs");
            reciver.add("MaxDecodeMs");
            reciver.add("JitterBufferMs");
            reciver.add("FrameRateDecoded");
        }
        return !reciver.contains(name);
    }


    private boolean isConnStat(String name)
    {
        if(connStat.size() == 0)
        {
            connStat.add("ActiveConnection");
            connStat.add("Readable");
            connStat.add("ChannelId");
            connStat.add("localCandidateId");
            connStat.add("remoteCandidateId");
            connStat.add("Rtt");
            connStat.add("packetsDiscardedOnSend");
            connStat.add("TransportType");
            connStat.add("Writable");
        }
        return !connStat.contains(name);
    }

    private boolean isRateStat(String name)
    {
        return true;
    }








    public void updateEncoderStatistics(final StatsReport[] reports) {

        final StringBuilder encoderStat = new StringBuilder(128);
        final StringBuilder bweStat = new StringBuilder();
        final StringBuilder connectionStat = new StringBuilder();
        final StringBuilder videoSendStat = new StringBuilder();
        final StringBuilder videoRecvStat = new StringBuilder();
        String fps = null;
        String targetBitrate = null;
        String actualBitrate = null;

        for (StatsReport report : reports) {
            if (report.type.equals("ssrc") && report.id.contains("ssrc") && report.id.contains("send"))
            {
                // Send video statistics.
                Map<String, String> reportMap = getReportMap(report);
                String trackId = reportMap.get("googTrackId");
                if (trackId != null && trackId.contains("ARDAMSv0"))
                {
                    fps = reportMap.get("googFrameRateSent");
                    //videoSendStat.append(report.id).append("\n");
                    for (StatsReport.Value value : report.values)
                    {
                        String name = value.name.replace("goog", "");
                        if(isShowSendStat(name))
                        {
                            videoSendStat.append(name).append("=").append(value.value).append("\n");
                        }
                    }
                }
            } else if (report.type.equals("ssrc") && report.id.contains("ssrc")
                    && report.id.contains("recv"))
            {
                // Receive video statistics.
                Map<String, String> reportMap = getReportMap(report);
                // Check if this stat is for video track.
                String frameWidth = reportMap.get("googFrameWidthReceived");
                if (frameWidth != null)
                {
//					videoRecvStat.append(report.id).append("\n");
                    for (StatsReport.Value value : report.values)
                    {
                        String name = value.name.replace("goog", "");
                        if(isShowReciveStat(name))
                            videoRecvStat.append(name).append("=").append(value.value).append("\n");
                    }
                }
            } else if (report.id.equals("bweforvideo")) {
                // BWE statistics.
                Map<String, String> reportMap = getReportMap(report);
                targetBitrate = reportMap.get("googTargetEncBitrate");
                actualBitrate = reportMap.get("googActualEncBitrate");

//				bweStat.append(report.id).append("\n");
                for (StatsReport.Value value : report.values)
                {
                    String name = value.name.replace("goog", "").replace("Available", "");
                    if(isRateStat(name))
                        bweStat.append(name).append("=").append(value.value).append("\n");
                }
            } else if (report.type.equals("googCandidatePair"))
            {
                // Connection statistics.
                Map<String, String> reportMap = getReportMap(report);
                String activeConnection = reportMap.get("googActiveConnection");
                if (activeConnection != null && activeConnection.equals("true"))
                {
//					connectionStat.append(report.id).append("\n");
                    for (StatsReport.Value value : report.values)
                    {
                        String name = value.name.replace("goog", "");
                        if(isConnStat(name))
                            connectionStat.append(name).append("=").append(value.value).append("\n");
                    }
                }
            }
        }
//		mAVTextView.setText(bweStat.toString());
//		hudViewConnection.setText(connectionStat.toString());
//		hudViewVideoSend.setText(videoSendStat.toString());
//		hudViewVideoRecv.setText(videoRecvStat.toString());

        if (true) {
            if (fps != null) {
                encoderStat.append("Fps:  ").append(fps).append("\n");
            }
            if (targetBitrate != null) {
                encoderStat.append("Target BR: ").append(targetBitrate).append("\n");
            }
            if (actualBitrate != null) {
                encoderStat.append("Actual BR: ").append(actualBitrate).append("\n");
            }
        }

        if (cpuMonitor.sampleCpuUtilization()) {
            encoderStat.append("CPU%: ")
                    .append(cpuMonitor.getCpuCurrent()).append("/")
                    .append(cpuMonitor.getCpuAvg3()).append("/")
                    .append(cpuMonitor.getCpuAvgAll());
        }
//		encoderStatView.setText(encoderStat.toString());

//		final StringBuilder builder = new StringBuilder();
//		builder.append("编解码和cpu使用率:").append(encoderStat);
//		builder.append("码率:").append(bweStat).append("\n").append("链路状态:").append(connectionStat).append("发送统计:").append(videoSendStat).append("接收统计:");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAVTextView5.setText("编解码和cpu使用率:" + "\n" + encoderStat.toString());

                if(bweStat.length() > 0)
                    mAVTextView4.setText("码率:" + "\n" + bweStat.toString());
                else
                    mAVTextView4.setVisibility(View.GONE);

                mAVTextView2.setText("链路状态:" + "\n" + connectionStat.toString());

                if(videoSendStat.length() > 0)
                    mAVTextView1.setText("发送统计:" + "\n" + videoSendStat.toString());
                else
                    mAVTextView1.setVisibility(View.GONE);

                if(videoRecvStat.length() > 0 )
                    mAVTextView3.setText("接收统计:" + "\n" + videoRecvStat.toString());
                else
                    mAVTextView3.setVisibility(View.GONE);
            }
        });

    }

    private Map<String, String> getReportMap(StatsReport report) {
        Map<String, String> reportMap = new HashMap<String, String>();
        for (StatsReport.Value value : report.values) {
            reportMap.put(value.name, value.value);
        }
        return reportMap;
    }

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
		lightScreen();
	}

	@Override
	protected void onStop() {
		super.onStop();
		mIsSensorTimerRunning = false;
		mAVManager.setCallUiIsShow(false);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.layout_callcontent:// 点击根布局时的操作，只有视频模式有此操作
			// 无通话或是非视频聊天时返回
			RKCloudAVCallInfo avCallInfo = mAVManager.getAVCallInfo();
			if(null==avCallInfo || RKCloudAVCallState.AV_CALL_STATE_ANSWER!=avCallInfo.callState
					|| !avCallInfo.isCurrVideoOpen){
				return;
			}
			if(mClickRootView){
				mHideInVideoBtn.setVisibility(View.VISIBLE);
				mAllBtnsLayout.setVisibility(View.VISIBLE);
				mTimer.setVisibility(View.VISIBLE);

				mClickRootView = false;

			}else{
				mHideInVideoBtn.setVisibility(View.GONE);
				mAllBtnsLayout.setVisibility(View.GONE);
				mTimer.setVisibility(View.GONE);

				mClickRootView = true;
			}
			break;

		case R.id.hideui_invideo:
		case R.id.hideui_inaudio:
			onBackPressed();
			break;

		case R.id.mute:
			boolean mute = mMuteBtn.isSelected();
			mAVManager.mute(!mute);
			mMuteBtn.setSelected(!mute);
			break;

		case R.id.handfree:
			boolean handfree = mHandFreeBtn.isSelected();
			mAVManager.handFree(!handfree);
			mHandFreeBtn.setSelected(!handfree);
			break;

		case R.id.switchcamera:
			boolean cameraStatus = mSwitchCameraBtn.isSelected();
			mAVManager.switchCamera(!cameraStatus);
			mSwitchCameraBtn.setSelected(!cameraStatus);
			break;

		case R.id.toaudio:
			mAVManager.switchToAudioCall();
			showWidgets();
			break;

		case R.id.hangup:
			mAVManager.hangup();
			break;

		case R.id.answer:
			mAVManager.answer();
			break;
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		mDistance = event.values[0];
		if(mDistance < mSensor.getMaximumRange()){
			if(!mIsSensorTimerRunning){
				mUiHandler.sendEmptyMessageDelayed(RKCloudAVUiHandlerMessage.HANDLER_MSG_WHAT_SENSOR, DELAY_EXECUTE_DIMSCREEN_TIME);
				mIsSensorTimerRunning = true;
			}

		}else{
			lightScreen();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		RKCloudAVCallInfo avCallInfo = mAVManager.getAVCallInfo();
		int callState = null!=avCallInfo ? avCallInfo.callState : RKCloudAVCallState.AV_CALL_STATE_IDLE;
		if(KeyEvent.KEYCODE_VOLUME_UP == keyCode){// 音量键上调
			int streamType = AudioManager.STREAM_VOICE_CALL;
			if(RKCloudAVCallState.AV_CALL_STATE_RINGBACK==callState || RKCloudAVCallState.AV_CALL_STATE_RINGIN==callState){
				streamType = AudioManager.STREAM_RING;
			}
			mAudioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
			return true;

		}else if(KeyEvent.KEYCODE_VOLUME_DOWN == keyCode){// 音量键下调
			int streamType = AudioManager.STREAM_VOICE_CALL;
			if(RKCloudAVCallState.AV_CALL_STATE_RINGBACK==callState || RKCloudAVCallState.AV_CALL_STATE_RINGIN==callState){
				streamType = AudioManager.STREAM_RING;
			}
			mAudioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
			return true;

		}else if(KeyEvent.KEYCODE_BACK == keyCode){
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	private void lightScreen() {
		WindowManager.LayoutParams attrs = getWindow().getAttributes();
		attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
		mCallBgView.setVisibility(View.VISIBLE);
		mCallBgView.setClickable(true);
		mRootView.setBackgroundColor(Color.TRANSPARENT);
		getWindow().setAttributes(attrs);
	}


	private void dimScreen() {
		WindowManager.LayoutParams attrs = getWindow().getAttributes();
		attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
		mCallBgView.setVisibility(View.INVISIBLE);
		mCallBgView.setClickable(false);
		mRootView.setBackgroundColor(Color.BLACK);
		getWindow().setAttributes(attrs);
	}

	private void initUiAndListeners(){
		mRootView = findViewById(R.id.layout_callroot);
		mCallBgView = findViewById(R.id.layout_callcontent);

		mUserInfoLayout = (RelativeLayout)findViewById(R.id.layout_userinfo);
		mHeaderImage = (RoundedImageView)findViewById(R.id.headerimage);
		mShowNameTV = (TextView)findViewById(R.id.username);
		mCallStatusTV  = (TextView)findViewById(R.id.callstatus);

		mRemoteVideoLayout = (RelativeLayout)findViewById(R.id.remotevideo);

		mTimer = (TextView)findViewById(R.id.timer);

		mHideInAudioBtn = (ImageView)findViewById(R.id.hideui_inaudio);
		mHideInVideoBtn = (ImageView)findViewById(R.id.hideui_invideo);

		mAllBtnsLayout = (LinearLayout)findViewById(R.id.btnzone);
		mCallingBtnsLayout = (LinearLayout)findViewById(R.id.btnzone_calling);

		mMuteLayout = (LinearLayout)findViewById(R.id.layout_mute);
		mMuteBtn = (ImageView)findViewById(R.id.mute);
		mHandFreeLayout = (LinearLayout)findViewById(R.id.layout_handfree);
		mHandFreeBtn = (ImageView)findViewById(R.id.handfree);
		mSwitchCameraLayout = (LinearLayout)findViewById(R.id.layout_switchcamera);
		mSwitchCameraBtn = (ImageView)findViewById(R.id.switchcamera);
		mToAudioLayout = (LinearLayout)findViewById(R.id.layout_toaudio);
		mToAudioBtn = (ImageView)findViewById(R.id.toaudio);
		mHangupLayout = (LinearLayout)findViewById(R.id.layout_hangup);
		mHangupBtn = (ImageView)findViewById(R.id.hangup);
		mHangupTV = (TextView)findViewById(R.id.hangup_text);
		mAnswerLayout = (LinearLayout)findViewById(R.id.layout_answer);
		mAnswerBtn = (ImageView)findViewById(R.id.answer);

		mCallBgView.setOnClickListener(this);
		mHideInAudioBtn.setOnClickListener(this);
		mHideInVideoBtn.setOnClickListener(this);
		mMuteBtn.setOnClickListener(this);
		mHandFreeBtn.setOnClickListener(this);
		mSwitchCameraBtn.setOnClickListener(this);
		mToAudioBtn.setOnClickListener(this);
		mHangupBtn.setOnClickListener(this);
		mAnswerBtn.setOnClickListener(this);
	}

	private void resolveIntent(Intent intent){
		// 获取对端号码
		String outAccount = intent.getStringExtra(INTENT_KEY_OUTCALL_ACCOUNT);
		String inAccount = intent.getStringExtra(INTENT_KEY_INCALL_ACCOUNT);
		RKCloudAVCallInfo avCallInfo = mAVManager.getAVCallInfo();
		// 两个号码全为空或是两个号码都不空时返回并结束
		if((TextUtils.isEmpty(outAccount) && TextUtils.isEmpty(inAccount))
				|| (!TextUtils.isEmpty(outAccount) && !TextUtils.isEmpty(inAccount))
				|| null==avCallInfo
				|| (RKCloudAVCallState.AV_CALL_STATE_IDLE==avCallInfo.callState /*|| RKCloudAVCallState.AV_CALL_STATE_PREPARING == avCallInfo.callState*/)){
			mAVManager.hideOutCallNotification();
			mAVManager.hideInCallNotification();
			finish();
			return;
		}
		// 与当前正在通话的类型或号码不匹配时结束
		if(!TextUtils.isEmpty(inAccount)){
			if(!inAccount.equalsIgnoreCase(avCallInfo.peerAccount) || avCallInfo.isCaller){
				mAVManager.hideInCallNotification();
				finish();
				return;
			}
		}else if(!TextUtils.isEmpty(outAccount)){
			if(!outAccount.equalsIgnoreCase(avCallInfo.peerAccount) || !avCallInfo.isCaller){
				mAVManager.hideOutCallNotification();
				finish();
				return;
			}
		}

		// 用户信息显示
		showUserInfo(avCallInfo.peerAccount);
	}

	/*
	 * 显示对端用户信息
	 */
	private void showUserInfo(String account){
		mContactObj = RKCloudAVContactManager.getInstance(this).getContactInfo(account);
		mShowNameTV.setText(null!=mContactObj ? mContactObj.showName : account);
		if(null!=mContactObj && !TextUtils.isEmpty(mContactObj.thumbPath) && new File(mContactObj.thumbPath).exists()){
			mHeaderImage.setImageBitmap(BitmapFactory.decodeFile(mContactObj.thumbPath));
		}else{
			mHeaderImage.setImageResource(R.drawable.rkcloud_av_img_header_default);
		}
	}

	private void showWidgets(){
		RKCloudAVCallInfo avCallInfo = mAVManager.getAVCallInfo();
        if(null == avCallInfo)return;
		switch(avCallInfo.callState){
		case RKCloudAVCallState.AV_CALL_STATE_PREPARING:
			mCallStatusTV.setText(R.string.rkcloud_av_tip_caller_connect);
			mHangupLayout.setVisibility(View.VISIBLE);
			break;
		case RKCloudAVCallState.AV_CALL_STATE_RINGIN:
		case RKCloudAVCallState.AV_CALL_STATE_RINGBACK:
			mUserInfoLayout.setVisibility(View.VISIBLE);
			mCallStatusTV.setVisibility(View.VISIBLE);
			if(RKCloudAVCallState.AV_CALL_STATE_RINGBACK == avCallInfo.callState){
				mCallStatusTV.setText(R.string.rkcloud_av_tip_caller_connect);
			}else{
				if(avCallInfo.isVideoCall){
					mCallStatusTV.setText(R.string.rkcloud_av_tip_callee_invitevideo);
				}else{
					mCallStatusTV.setText(R.string.rkcloud_av_tip_callee_inviteaudio);
				}
			}

			mTimer.setVisibility(View.GONE);
			mRemoteVideoLayout.setVisibility(View.GONE);

			mHideInAudioBtn.setVisibility(View.GONE);
			mHideInVideoBtn.setVisibility(View.GONE);

			mCallingBtnsLayout.setVisibility(View.GONE);
			mMuteLayout.setVisibility(View.GONE);
			mHandFreeLayout.setVisibility(View.GONE);
			mSwitchCameraLayout.setVisibility(View.GONE);

			mToAudioLayout.setVisibility(View.GONE);
			mHangupLayout.setVisibility(View.VISIBLE);
			if(RKCloudAVCallState.AV_CALL_STATE_RINGIN == avCallInfo.callState){
				mAnswerLayout.setVisibility(View.VISIBLE);
			}else{
				mAnswerLayout.setVisibility(View.GONE);
				mHangupTV.setText(R.string.rkcloud_av_btn_cancel);
			}

			break;

		case RKCloudAVCallState.AV_CALL_STATE_ANSWER:
			if(!avCallInfo.isCurrVideoOpen){
				mUserInfoLayout.setVisibility(View.VISIBLE);
				mCallStatusTV.setVisibility(View.GONE);

				mRemoteVideoLayout.setVisibility(View.GONE);

				mHideInAudioBtn.setVisibility(View.VISIBLE);
				mHideInVideoBtn.setVisibility(View.GONE);

				mSwitchCameraLayout.setVisibility(View.GONE);
				mToAudioLayout.setVisibility(View.GONE);

			}else{
				mUserInfoLayout.setVisibility(View.GONE);
				mCallStatusTV.setVisibility(View.GONE);

				mRemoteVideoLayout.setVisibility(View.VISIBLE);

				mHideInAudioBtn.setVisibility(View.GONE);
				mHideInVideoBtn.setVisibility(View.VISIBLE);

				if(mAVManager.checkCameraHardware() && mAVManager.enableSwitchCamera()){
					mSwitchCameraLayout.setVisibility(View.VISIBLE);
					mSwitchCameraBtn.setSelected(mAVManager.getSwitchCameraStatus());
				}else{
					mSwitchCameraLayout.setVisibility(View.GONE);
				}

				mToAudioLayout.setVisibility(View.VISIBLE);
			}

			mTimer.setVisibility(View.VISIBLE);

			mCallingBtnsLayout.setVisibility(View.VISIBLE);
			mMuteLayout.setVisibility(View.VISIBLE);
			mMuteBtn.setSelected(mAVManager.getMuteStatus());
			mHandFreeLayout.setVisibility(View.VISIBLE);
			mHandFreeBtn.setSelected(mAVManager.getHandFreeStatus());
			mAnswerLayout.setVisibility(View.GONE);
			mHangupLayout.setVisibility(View.VISIBLE);
			mHangupTV.setText(R.string.rkcloud_av_btn_hangup);
			break;

		default:
			mUserInfoLayout.setVisibility(View.VISIBLE);
			mCallStatusTV.setVisibility(View.GONE);
			mRemoteVideoLayout.setVisibility(View.GONE);
			mTimer.setVisibility(View.GONE);
			mHideInAudioBtn.setVisibility(View.GONE);
			mHideInVideoBtn.setVisibility(View.GONE);
			mCallingBtnsLayout.setVisibility(View.GONE);
			mMuteLayout.setVisibility(View.VISIBLE);
			mHandFreeLayout.setVisibility(View.VISIBLE);
			mSwitchCameraLayout.setVisibility(View.GONE);
			mToAudioLayout.setVisibility(View.GONE);
			mAnswerLayout.setVisibility(View.GONE);
			mHangupLayout.setVisibility(View.VISIBLE);
			mHangupTV.setText(R.string.rkcloud_av_btn_hangup);
			break;
		}
	}

	/*
	 * 更新通话时长
	 */
	private void updateCallTime(){
		RKCloudAVCallInfo avCallInfo = mAVManager.getAVCallInfo();
		if(null != avCallInfo){
			int duration = (int)Math.ceil((System.currentTimeMillis()-avCallInfo.callAnswerTime)/1000);
			mTimer.setText(RKCloudAVUtils.secondConvertToTime(duration));
			mUiHandler.sendEmptyMessageDelayed(RKCloudAVUiHandlerMessage.HANDLER_MSG_WHAT_UPDATETIME, 1000);
		}
	}

	private void processResult(Message msg) {
		if(RKCloudAVUiHandlerMessage.HANDLER_MSG_WHAT_SENSOR == msg.what){
			RKCloudAVCallInfo avCallInfo = mAVManager.getAVCallInfo();
			if(mDistance <mSensor.getMaximumRange()){
				if(!avCallInfo.isCurrVideoOpen){
					// 只有语音通话时才起作用
					dimScreen();
				}
			}
			mIsSensorTimerRunning = false;

		}else if(RKCloudAVUiHandlerMessage.HANDLER_MSG_WHAT_UPDATETIME == msg.what){
			updateCallTime();

		}else if(RKCloudAVUiHandlerMessage.HANDLER_MSG_WHAT_AV == msg.what){
			int state = msg.arg1;
			switch(state){
			case RKCloudAVCallState.AV_CALL_STATE_RINGBACK:
				mCallStatusTV.setText(R.string.rkcloud_av_tip_caller_wait);
				break;

			case RKCloudAVCallState.AV_CALL_STATE_ANSWER:
				updateCallTime();
				showWidgets();
				// 设置音量键调节的音量类型
				setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
				if(mAVManager.getAVCallInfo().isCurrVideoOpen){
					// 视频通话时默认为免提
					mAVManager.handFree(true);
					mHandFreeBtn.setSelected(true);
				}
				RongKeJNI.getInstance().getStatisticalData(true, new RKCallStatisticalCallBack() {
					@Override
					public void onStatisticalDataReciver(StatsReport[] reports) {
						updateEncoderStatistics(reports);
					}
				}, 1000);

				break;

			case RKCloudAVCallState.AV_CALL_STATE_VIDEO_INIT:
				break;

			case RKCloudAVCallState.AV_CALL_STATE_VIDEO_STOP:
			case RKCloudAVCallState.AV_CALL_STATE_VIDEO_START:
				mAllBtnsLayout.setVisibility(View.VISIBLE);
				mClickRootView = false;
				showWidgets();
				if(RKCloudAVCallState.AV_CALL_STATE_VIDEO_STOP == state){
					mAVManager.showToastText(getString(R.string.rkcloud_av_tip_remote_toaudio));
				}else{
					mAVManager.showToastText(getString(R.string.rkcloud_av_tip_remote_tovideo));
				}
				break;

			case RKCloudAVCallState.AV_CALL_STATE_HANGUP:
				finish();
				break;
			}
		}else if(RKCloudAVUiHandlerMessage.MSG_WHAT_MEETING_CONTACTSINFO_CHANGED == msg.what){ // 联系人信息有变更
			List<String> accounts = (List<String>)msg.obj;
			if(null!=accounts && accounts.size()>0){
				RKCloudAVCallInfo avCallInfo = mAVManager.getAVCallInfo();
				if(null != avCallInfo && accounts.contains(avCallInfo.peerAccount)){
					showUserInfo(avCallInfo.peerAccount);
				}
			}

		}else if(RKCloudAVUiHandlerMessage.MSG_WHAT_MEETING_CONTACT_HEADERIMAGE_CHANGED == msg.what){ // 联系人头像有变更
			String account = (String)msg.obj;
			RKCloudAVCallInfo avCallInfo = mAVManager.getAVCallInfo();
			if(null != avCallInfo && account.equalsIgnoreCase(avCallInfo.peerAccount)){
				showUserInfo(avCallInfo.peerAccount);
			}
		}
	}

	private class UiHandler extends Handler{
		private WeakReference<Activity> mContext;

		public UiHandler(Activity context){
			mContext = new WeakReference<Activity>(context);
		}

		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if(null == mContext || null == mContext.get()){
				return;
			}
			processResult(msg);
		}
	}
}
