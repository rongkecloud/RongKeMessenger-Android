package com.rongkecloud.test.manager;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import com.rongke.cloud.av.demo.RKCloudAVDemoManager;
import com.rongke.cloud.meeting.demo.RKCloudMeetingDemoManager;
import com.rongkecloud.av.RKCloudAV;
import com.rongkecloud.chat.RKCloudChat;
import com.rongkecloud.chat.RKCloudChatMessageManager;
import com.rongkecloud.chat.demo.RKCloudChatContactManager;
import com.rongkecloud.chat.demo.RKCloudChatLogoutManager;
import com.rongkecloud.chat.demo.RKCloudChatMmsManager;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageAsyncLoader;
import com.rongkecloud.multiVoice.RKCloudMeeting;
import com.rongkecloud.sdkbase.RKCloud;
import com.rongkecloud.sdkbase.RKCloudBaseErrorCode;
import com.rongkecloud.sdkbase.interfaces.InitCallBack;
import com.rongkecloud.sdkbase.interfaces.RKCloudFatalExceptionCallBack;
import com.rongkecloud.sdkbase.interfaces.RKCloudLogCallBack;
import com.rongkecloud.test.manager.uihandlermsg.AccountUiMessage;
import com.rongkecloud.test.system.ConfigKey;
import com.rongkecloud.test.system.RKCloudDemo;
import com.rongkecloud.test.ui.reminder.ReminderActivity;
import com.rongkecloud.test.ui.reminder.ReminderBroadcast;
import com.rongkecloud.test.utility.FileLog;

public class SDKManager implements RKCloudFatalExceptionCallBack
{
	private static final String TAG = SDKManager.class.getSimpleName();

	private static final int SDK_INIT_WHAT = 88;
	private static final int ACCOUNT_EXCEPTION_WHAT = 99;

	private static SDKManager mInstance = null;
	private Handler mHandler;
	private Handler mUiHandler;

	private boolean sdkInitStatus = false;// SDK的初始化状态 false:失败 true:成功

	private SDKManager()
	{
		mHandler = new Handler()
		{
			public void handleMessage(Message msg)
			{
				switch (msg.what)
				{
					case SDK_INIT_WHAT:
						if (0 == msg.arg1)
						{
							FileLog.d(TAG, "initSDK success.");
							sdkInitStatus = true;
						}
						else
						{
							FileLog.d(TAG, "initSDK failed, code=" + msg.arg1);
						}
						if (null != mUiHandler)
						{
							Message initMsg = mUiHandler.obtainMessage();
							initMsg.what = AccountUiMessage.SDK_INIT_FINISHED;
							initMsg.arg1 = msg.arg1;
							initMsg.sendToTarget();
						}
						break;

					case ACCOUNT_EXCEPTION_WHAT:
						Intent intent = new Intent(RKCloudDemo.context, ReminderActivity.class);
						if (1 == msg.arg1)
						{
							intent.setAction(ReminderBroadcast.ACTION_REMIND_KICKED_USER);
						}
						else if (2 == msg.arg1)
						{
							intent.setAction(ReminderBroadcast.ACTION_REMIND_BANNED_USER);
						}
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						RKCloudDemo.context.startActivity(intent);
						break;
				}
			}
		};
	}

	public static SDKManager getInstance()
	{
		if (null == mInstance)
		{
			mInstance = new SDKManager();
		}
		return mInstance;
	}

	public void bindUiHandler(Handler handler)
	{
		mUiHandler = handler;
	}

	/**
	 * 初始化云视互动sdk
	 */
	public void initSDK()
	{
		FileLog.d(TAG, "initSDK--begin");
		String rkcloudAccount = RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, null);
		String rkcloudPwd = RKCloudDemo.config.getString(ConfigKey.LOGIN_RKCLOUD_PWD, null);
		RKCloud.setMiPushAppIdAndAppKey("2882303761517329291", "5761732975291");
		RKCloud.setHuaweiPushAppId("10760441");
        // 设置Debug模式为打开状态
        RKCloud.setDebugMode(true);

		if (!TextUtils.isEmpty(rkcloudAccount) && !TextUtils.isEmpty(rkcloudPwd))
		{

			// 设置SDK的日志回调
			RKCloud.setRKCloudLogCallBack(new RKCloudLogCallBack()
			{
				@Override
				public void onLog(int logLevel, String tag, String content)
				{
					switch (logLevel)
					{
						case Log.VERBOSE:
						case Log.DEBUG:
						case Log.INFO:
						case Log.WARN:
						case Log.ERROR:
							FileLog.log("SDK-" + tag, content);
							break;
					}
				}
			});

			// 云视互动SDK初始化
			RKCloud.init(RKCloudDemo.context, rkcloudAccount, rkcloudPwd, new InitCallBack()
			{
				@Override
				public void onSuccess()
				{
                    sdkSuccessDo();
					Message msg = mHandler.obtainMessage();
					msg.what = SDK_INIT_WHAT;
					msg.arg1 = RKCloudBaseErrorCode.RK_SUCCESS;
					msg.sendToTarget();
				}

				@Override
				public void onFail(int failCode)
				{
					Message msg = mHandler.obtainMessage();
					msg.what = SDK_INIT_WHAT;
					msg.arg1 = failCode;
					msg.sendToTarget();
				}
			});
		}
	}

	/**
	 * 获取SDK的初始化状态值
	 * 
	 * @return
	 */
	public boolean getSDKInitStatus()
	{
		return sdkInitStatus;
	}

	/**
	 * 初始化sdk成功要处理的内容
	 */
	private void sdkSuccessDo()
	{
		// 设置SDK账号异常的回调处理
		RKCloud.setOnRKCloudFatalExceptionCallBack(this);
		// 设置推送消息的回调处理
		RKCloud.setOnRKCloudReceivedUserDefinedMsgCallBack(MessageManager.getInstance());
		// 云视互动即时通信SDK的初始化
		RKCloudChat.init();
		// 绑定消息回调、群变更的通知
		RKCloudChatMessageManager manager = RKCloudChatMessageManager.getInstance(RKCloudDemo.context);
		manager.registerRKCloudChatReceivedMsgCallBack(RKCloudChatMmsManager.getInstance(RKCloudDemo.context));
		manager.registerRKCloudChatGroupCallBack(RKCloudChatMmsManager.getInstance(RKCloudDemo.context));
		// 与应用端的联系人接口进行绑定
		manager.registerRKCloudContactCallBack(RKCloudChatContactManager.getInstance(RKCloudDemo.context));

		// 音视频互动初始化
		RKCloudAV.init(RKCloudAVDemoManager.getInstance(RKCloudDemo.context));
		// 多人语音初始化
		RKCloudMeeting.init(RKCloudMeetingDemoManager.getInstance(RKCloudDemo.context));
	}

	@Override
	public void onRKCloudFatalException(int errorCode)
	{
		Message msg = mHandler.obtainMessage();
		msg.what = ACCOUNT_EXCEPTION_WHAT;
		msg.arg1 = errorCode;
		msg.sendToTarget();
	}

	public void logout()
	{
		// 退出之后，一定要清理已经初始化的SDK内容
		sdkInitStatus = false;
		// 清除所有图片缓存
		RKCloudChatImageAsyncLoader.getInstance(RKCloudDemo.context).removeAllImages();
		// 结束所有的UI
		RKCloudChatLogoutManager.getInstance(RKCloudDemo.context).logout();
		// 音视频退出时的操作
		RKCloudAVDemoManager.getInstance(RKCloudDemo.context).logout();
		RKCloudMeetingDemoManager.getInstance(RKCloudDemo.context).logout();
		RKCloudChat.unInit();
		RKCloudAV.unInit();
		RKCloudMeeting.unInit();
		RKCloud.unInit();
	}
}
