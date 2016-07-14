package com.rongkecloud.chat.demo.ui;

import android.app.AlertDialog;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import com.rongke.cloud.av.demo.RKCloudAVDemoManager;
import com.rongke.cloud.meeting.demo.RKCloudMeetingDemoManager;
import com.rongkecloud.chat.*;
import com.rongkecloud.chat.RKCloudChatBaseMessage.MSG_STATUS;
import com.rongkecloud.chat.demo.RKCloudChatConstants;
import com.rongkecloud.chat.demo.RKCloudChatContactManager;
import com.rongkecloud.chat.demo.RKCloudChatMmsManager;
import com.rongkecloud.chat.demo.RKCloudChatUiHandlerMessage;
import com.rongkecloud.chat.demo.entity.RKCloudChatContact;
import com.rongkecloud.chat.demo.entity.RKCloudChatEmojiRes;
import com.rongkecloud.chat.demo.entity.RKCloudChatMsgAttachItem;
import com.rongkecloud.chat.demo.entity.RKCloudChatSelectVideoItem;
import com.rongkecloud.chat.demo.tools.RKCloudChatImageTools;
import com.rongkecloud.chat.demo.tools.RKCloudChatPlayAudioMsgTools;
import com.rongkecloud.chat.demo.tools.RKCloudChatScreenTools;
import com.rongkecloud.chat.demo.tools.RKCloudChatTools;
import com.rongkecloud.chat.demo.ui.adapter.RKCloudChatAttachAdapter;
import com.rongkecloud.chat.demo.ui.adapter.RKCloudChatEmojiAdapter;
import com.rongkecloud.chat.demo.ui.adapter.RKCloudChatMsgAdapter;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageAsyncLoader;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageAsyncLoader.ImageLoadedCompleteDelayNotify;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest.IMAGE_REQUEST_TYPE;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageResult;
import com.rongkecloud.chat.demo.ui.widget.RKCloudChatEmojiEditText;
import com.rongkecloud.chat.demo.ui.widget.record.OnRKCloudChatRecordDialogDismissListener;
import com.rongkecloud.chat.demo.ui.widget.record.RKCloudChatRecordPopupWindow;
import com.rongkecloud.multiVoice.RKCloudMeetingCallState;
import com.rongkecloud.multiVoice.RKCloudMeetingInfo;
import com.rongkecloud.sdkbase.RKCloud;
import com.rongkecloud.test.R;

import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.*;

public class RKCloudChatMsgActivity extends RKCloudChatBaseActivity implements OnClickListener, OnTouchListener, OnRKCloudChatRecordDialogDismissListener, ImageLoadedCompleteDelayNotify
{
	private static final String TAG = RKCloudChatMsgActivity.class.getSimpleName();
	// 多个UI之间intent需要传递的内容项
	public static final String INTENT_KEY_MSGLIST_CHATID = "chat_id";// 单聊ID
	public static final String INTENT_KEY_MSGLIST_GROUPID = "group_id";// 群聊ID
	public static final String INTENT_KEY_MSGLIST_MSGID = "msglist_msgid";// 搜索消息显示的消息ID

	// 查询数据的类型
	private static final int QUERY_TYPE_LOAD_DATA = 1;// 获取数据
	private static final int QUERY_TYPE_LOAD_UNREAD_DATA = 2;// 获取未读数据
	private static final int QUERY_TYPE_LOAD_HISTORY_DATA = 3;// 获取历史数据
	private static final int QUERY_TYPE_LOAD_SEARCH_DATA = 4;// 获取搜索到的数据
	private static final int QUERY_TYPE_LOAD_NEW_DATA = 5;// 获取新数据

	// 上下文菜单
	private static final int CONTEXT_MENU_RESEND = 1; // 重发
	private static final int CONTEXT_MENU_COPY = 2; // 复制
	private static final int CONTEXT_MENU_PLAY_WITH_EARPHONE = 3; // 使用听筒播放语音消息
	private static final int CONTEXT_MENU_PLAY_WITH_SPEAKERPHONE = 4; // 使用扬声器播放语音消息
	private static final int CONTEXT_MENU_FORWARD = 5; // 转发
	private static final int CONTEXT_MENU_SHARE = 6; // 分享
	private static final int CONTEXT_MENU_DEL = 7; // 删除
	private static final int CONTEXT_MENU_REVOKE = 8; // 撤回

	// 定义从其它UI返回的结果类型值
	static final int INTENT_RESULT_TAKE_PHOTO = 1;// 拍照
	static final int INTENT_RESULT_CHOOSE_PICTURE = 2;// 选择图片
	static final int INTENT_RESULT_CHOOSE_VIDEO = 3;// 录制视频
	static final int INTENT_RESULT_CHOOSE_ATTACHEMENT = 4;// 选择附件

	// 分段加载历史数据使用的相关变量
	private long lastLoadMsgCreaingId = 0;// 记录已加载消息中自增ID值最小的一个消息ID值
	private boolean mLoadHistoryDataFinished = false;// 消息条数是否全部加载完成，默认表示未完成
	private boolean mLoadingHistoryData = false;// 是否在加载历史消息中，默认不在

	// 分段加载新数据使用的相关变量
	private long newLoadMsgCreaingId = 0;// 记录已加载消息中自增ID值最大的一个消息ID值
	private boolean mLoadNewDataFinished = false;// 新消息条数是否全部加载完成，默认表示未完成
	private boolean mLoadingNewData = false;// 是否在加载历史消息中，默认不在

	// 用于查看完图片或文件后返回时定位消息的位置
	private String remberViewMsgInChatId = null;// 记录查看图片或文件时所在的会话ID
	private int remberFirstPosition = -1;// 列表加载的第一个条目位置
	private int remberDistinceToTop = -1;// 列表加载的第一个条目距离头部位置

	// 接收到新消息时需要在底部提醒使用的相关变量
	public static String tipMsgSerialNum = null;// 当列表滚动条处于中间状态时，记录提示新消息的消息编号
	private long mShowTipMsgTime = 0;// 记录消息提示显示的时间，主要用于收到多条新消息后到达一定时间后隐藏提示条使用

	public static String lastAudioSerialNum = null;// 用于记录最后一个点击下载播放音频消息的消息编号，并且在离开消息列表页面后置为null

	public static RKCloudChatMsgActivity mMsgInstance;// 聊天页面的实例化对象

	// UI组件
	private RelativeLayout mRootView; // 根布局
	private TextView mTitleTV;// 返回
	private TextView text_title_content;// 显示会话名称
	private ImageButton mManagerBtn;// 会话管理图标

	private ImageButton mRecordTextModelSwitch;// 文本/音频模式的转换开关
	private Button mRecordBnt;// 录音按钮
	private LinearLayout mTextLayout;// 文本输入区域
	private LinearLayout mTextContentLayout;// 文本内容的布局
	private RKCloudChatEmojiEditText mSmiliesEditText; // 带表情的消息输入框
	private Button mSendBnt; // 发送图标

	// 展示表情相关的内容
	private ImageView mEmojiSwitch;// 表情开关
	private LinearLayout mEmojiLayout;
	private ViewPager mEmojiPager;
	private LinearLayout mPagerPointsLayout;
	private List<View> mEmojiViews = new ArrayList<View>();

	// 展示附件相关的内容
	private ImageButton mAttachSwitch;// 打开或关闭其他操作按钮
	private LinearLayout mAttachLayout;
	private ViewPager mAttachPager;
	private LinearLayout mAttachPointsLayout;
	private List<View> mAttachViews = new ArrayList<View>();

	private LinearLayout mLoadingHistoryLayout;// 加载历史消息
	private ListView mListView;// 消息列表
	private ProgressBar mLoadMsgBar; // 消息加载进度条
	private TextView mTipReceiveMsg;// 用于收到消息后的提醒
	private TextView mUnreadMsgCntTip;// 未读条数提示

	private TextView mEnterMeetingTip;// 进入多人语音文字提示

	// 成员变量
	private RKCloudChatConfigManager mChatConfigManager;
	private RKCloudChatMmsManager mMmsManager;
	private RKCloudChatContactManager mContactManager;
	private InputMethodManager mInputMethodManager;
	private ClipboardManager mClipboardManager; // 剪贴板管理器对象
	private AudioManager mAudioManager;

	private RKCloudChatPlayAudioMsgTools mAudioHelper;
	private RKCloudChatRecordPopupWindow mRecordPopupWindow; // 录音窗口
	private int mMinHeightToCancel; // 取消语音消息发送时的最短距离
	private boolean mCancelSendVoiceMsg = false;// 是否取消发送语音消息，默认不取消

	private String mChatId;
	private Class<? extends RKCloudChatBaseChat> mChatClassObj;
	private RKCloudChatBaseChat mChatObj;
	private String mCurrAccount;
	private List<RKCloudChatBaseMessage> mAllMsgsData;// 带索引的所有消息数据(包含时间条目)
	private RKCloudChatMsgAdapter mAdapter; // 适配器
	private String mMsgId;// 搜索到的消息ID
	private int mOnceCount = 10;// 一次获取到的消息数量

	private Map<String, RKCloudChatContact> mContacts;// 联系人信息

	private String mTakePhotoTempName = null;// 记录拍照时的图片名称

	private QueryHandlerThread mQueryThread;// 查询数据的线程

	// 提示未读消息条数时使用的变量
	private static final int TIP_UNREADCNT_LEAST_COUNT = 10;// 提示未读条数的最小限制
	public static long unreadLeastMsgId = 0;// 未读消息中的最小ID值
	private boolean mFirstShowUnreadTip = false;// 是否显示未读条数提示
	private int mUnreadCnt = 0;// 未读条数
	private boolean mScrollStatus = false;// 滚动条是否有滚动，用于控制定位使用

	private String singleId;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rkcloud_chat_mms_msg_list);
		initUI();
		initListeners();
		initData(savedInstanceState, getIntent());
		mMsgInstance = this;
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		initData(null, intent);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		// 绑定UI handler
		mMmsManager.bindUiHandler(mUiHandler);
		mContactManager.bindUiHandler(mUiHandler);
		RKCloudChatImageAsyncLoader.getInstance(this).registerDelayListener(this);

		mMmsManager.setUnNeedNotifyChatId(mChatId);// 设置不需要向通知栏中发送该会话的通知
		hiddenTipMsg();// 取消提示条提醒

		showConvInfo();// 获取会话信息

		// 如果需要则重新刷新列表数据
		if (!TextUtils.isEmpty(mMsgId))
		{
			startQuery(QUERY_TYPE_LOAD_SEARCH_DATA);
		}
		else
		{
			startQuery(QUERY_TYPE_LOAD_DATA);
		}

		// TODO 与多人语音结合时使用
		if (GroupChat.class.equals(mChatClassObj))
		{
			// 判断当前会话是否有多人语音会议正在进行
			RKCloudMeetingDemoManager meetingManager = RKCloudMeetingDemoManager.getInstance(this);
			String meetingChatId = meetingManager.getMeetingExtensionInfo();
			RKCloudMeetingInfo meetingInfo = meetingManager.getMeetingInfo();
			if (mChatId.equalsIgnoreCase(meetingChatId) && null != meetingInfo && meetingInfo.callState == RKCloudMeetingCallState.MEETING_CALL_ANSWER)
			{
				mEnterMeetingTip.setVisibility(View.VISIBLE);
			}
			else
			{
				mEnterMeetingTip.setVisibility(View.GONE);
			}
		}
		else
		{
			mEnterMeetingTip.setVisibility(View.GONE);
		}

		jumpListBottom();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		mMmsManager.setUnNeedNotifyChatId(null);// 设置需要向通知栏中发送通知，即使在待机状态下也可以收到通知
		mMmsManager.updateMsgsReadedInChat(mChatId);// 标记该会话中的所有消息为已读状态，放在此处是解决通知栏中统计未读条数不准确的问题
		mRecordPopupWindow.stopRecord();// 关闭录音对话窗口
		initRecordParam();// 初始化录音按钮的相关显示信息，录音未选中时的背景色以及为未选中状态
		closeProgressDialog();// 关闭一些等待框

		// 需要记录位置的相关内容
		if (mAllMsgsData.size() > 0)
		{
			remberViewMsgInChatId = mChatId;
			remberFirstPosition = mListView.getFirstVisiblePosition();
			remberDistinceToTop = mListView.getChildAt(0).getTop();
		}

		// 关闭所有弹出的内容
		hideKeyboard();
		controllMoreOpeZoneOpen(false);
		controllEmojiZoneOpen(false);
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		mMmsManager.saveDraft(mChatId, mSmiliesEditText.getText().toString().trim());// 只有离开消息列表页面时才会保存草稿箱内容
		// 关闭语音消息的播放，放在这里即使为待机状态仍可以继续播放语音消息
		mAudioHelper.stopMsgOfAudio();
		lastAudioSerialNum = null;

		// 设置部分标志位的字段内容
		mScrollStatus = false;
	}

	@Override
	protected void onDestroy()
	{
		if (mQueryThread != null)
		{
			mQueryThread.quit();
			mQueryThread = null;
		}
		mMsgInstance = null;
		super.onDestroy();
	}

	@Override
	protected void finalize() throws Throwable
	{
		super.finalize();
	}

	@Override
	public void onClick(View v)
	{
		int id = v.getId();
		if (R.id.title_imgbtns_rightbtn == id)
		{ // 会话管理
			if (SingleChat.class.equals(mChatClassObj))
			{
				Intent intent = new Intent(this, RKCloudChatSingleManageActivity.class);
				intent.putExtra(RKCloudChatSingleManageActivity.INTENT_SINGLE_CHATID, mChatId);
				startActivity(intent);

			}
			else if (GroupChat.class.equals(mChatClassObj))
			{
				Intent intent = new Intent(this, RKCloudChatGroupManageActivity.class);
				intent.putExtra(RKCloudChatGroupManageActivity.INTENT_GROUP_CHATID, mChatId);
				startActivity(intent);
			}

		}
		else if (R.id.btn_send == id)
		{ // 发送文本消息
			String content = mSmiliesEditText.getText().toString().trim();
			if (!TextUtils.isEmpty(content))
			{
				TextMessage textObj = TextMessage.buildMsg(mChatId, content);
				textObj.setmMsgSummary(textObj.getContent());
				sendMms(textObj);
				mSmiliesEditText.setText(null);// 清空输入框内容
			}

		}
		else if (R.id.textmodel_switcher == id)
		{ // 音频和文本转换开关
			controllMoreOpeZoneOpen(false);
			controllEmojiZoneOpen(false);
			hideKeyboard();
			controllShowTextModel(mTextLayout.getVisibility() == View.GONE);
			jumpListBottom();

		}
		else if (R.id.attach_switcher == id)
		{ // 打开或关闭更多功能
			controllShowTextModel(true);
			hideKeyboard();
			controllEmojiZoneOpen(false);
			controllMoreOpeZoneOpen(mAttachLayout.getVisibility() == View.GONE);
			jumpListBottom();

		}
		else if (R.id.emoji_switcher == id)
		{ // 表情图标
			controllMoreOpeZoneOpen(false);
			hideKeyboard();
			controllEmojiZoneOpen(mEmojiLayout.getVisibility() == View.GONE);
			jumpListBottom();
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		if (R.id.btn_record == v.getId())
		{
			switch (event.getAction())
			{
				case MotionEvent.ACTION_DOWN: // 按下事件
					// 获取取消语音消息的最短距离
					int[] locations = new int[2];
					mRecordBnt.getLocationInWindow(locations);
					int x = locations[0];
					int y = locations[1];
					mMinHeightToCancel = y - mRecordBnt.getHeight();

					// 如果有正在播放的语音消息时先停止
					mAudioHelper.stopMsgOfAudio();

					// 开始录音
					String audioFileName = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date(System.currentTimeMillis())) + "-" + new Random().nextInt(99);
					mRecordPopupWindow.startRecord(RKCloudChatConstants.MMS_TEMP_PATH, audioFileName);

					// 设置录音为选中时的背景色，并且为选中状态
					mRecordBnt.setSelected(true);
					mRecordPopupWindow.setTip(getString(R.string.rkcloud_chat_audio_recording_finger_up_tip), true);
					break;

				case MotionEvent.ACTION_UP: // 抬起事件
					if (null != mRecordPopupWindow)
					{
						mRecordPopupWindow.stopRecord();
					}
					break;

				case MotionEvent.ACTION_MOVE:// 移动
					if ((int) event.getRawY() <= mMinHeightToCancel)
					{
						if (!mCancelSendVoiceMsg)
						{
							mCancelSendVoiceMsg = true;
							mRecordPopupWindow.setTip(getString(R.string.rkcloud_chat_audio_recording_finger_leave_tip), true);
						}
					}
					else
					{
						if (mCancelSendVoiceMsg)
						{
							mCancelSendVoiceMsg = false;
							mRecordPopupWindow.setTip(getString(R.string.rkcloud_chat_audio_recording_finger_up_tip), true);
						}
					}
					break;

				case MotionEvent.ACTION_CANCEL:// 取消
					if (null != mRecordPopupWindow)
					{
						mRecordPopupWindow.stopRecord();
					}
					break;
			}
		}
		else if (R.id.list == v.getId())
		{
			controllMoreOpeZoneOpen(false);
			controllEmojiZoneOpen(false);
			hideKeyboard();
		}
		return false;
	}

	@Override
	public void onRecordDialogDismiss()
	{
		String filePath = mRecordPopupWindow.getRecordFile();
		int duration = mRecordPopupWindow.getRecordDuration();
		if (duration >= 1)
		{
			AudioMessage audioObj = AudioMessage.buildMsg(mChatId, filePath);
			audioObj.setmMsgSummary(getString(R.string.rkcloud_chat_notify_audio));
			if (!mCancelSendVoiceMsg)
			{
				sendMms(audioObj);
			}
		}
		else
		{
			if (duration > 0)
			{
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_audio_recording_duration_smaller));
			}
		}

		// 设置录音未选中时的背景色，并且为未选中状态
		initRecordParam();
	}

	/**
	 * 捕捉键盘上按下事件时的处理
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		switch (keyCode)
		{
			case KeyEvent.KEYCODE_BACK:// 返回键
				if (View.VISIBLE == mAttachLayout.getVisibility())
				{
					controllMoreOpeZoneOpen(false);
					return true;

				}
				else if (View.VISIBLE == mEmojiLayout.getVisibility())
				{
					controllEmojiZoneOpen(false);
					return true;
				}
				else if (KeyEvent.KEYCODE_VOLUME_UP == keyCode)
				{// 音量键上调
					mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
					return true;
				}
				else if (KeyEvent.KEYCODE_VOLUME_DOWN == keyCode)
				{// 音量键下调
					mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
					return true;
				}
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (RESULT_OK != resultCode)
		{
			return;
		}

		if (INTENT_RESULT_TAKE_PHOTO == requestCode)
		{
			String imagePath = mTakePhotoTempName;
			if (TextUtils.isEmpty(imagePath))
			{
				return;
			}
			File file = new File(imagePath);
			if (file.exists() && file.length() > 0)
			{
				// 处理图片时显示等待对话框
				showProgressDialog();
				mMmsManager.processPhoto(mChatId, RKCloudChatConstants.MMS_TEMP_PATH, imagePath, true);
			}
			mTakePhotoTempName = null;

		}
		else if (INTENT_RESULT_CHOOSE_PICTURE == requestCode)
		{
			String imagePath = null != data ? RKCloudChatTools.getChoosePicturePath(this, data.getData()) : null;
			// 如果图片路径为空，或者图片不存在，则结束
			if (TextUtils.isEmpty(imagePath) || !new File(imagePath).exists())
			{
				return;
			}

			// 处理图片时显示等待对话框
			showProgressDialog();
			mMmsManager.processPhoto(mChatId, RKCloudChatConstants.MMS_TEMP_PATH, imagePath, false);

		}
		else if (INTENT_RESULT_CHOOSE_VIDEO == requestCode)
		{ // 小视频
			RKCloudChatSelectVideoItem videoFile = null != data ? (RKCloudChatSelectVideoItem) data.getParcelableExtra(RKCloudChatRecordVideoActivity.INTENT_RETURN_KEY_RECORDEDVIDEO) : null;
			if (null != videoFile)
			{
				VideoMessage videoObj = VideoMessage.buildMsg(mChatId, videoFile.getFilePath());
				videoObj.setmMsgSummary(getString(R.string.rkcloud_chat_notify_video));
				sendMms(videoObj);
			}

		}
		else if (INTENT_RESULT_CHOOSE_ATTACHEMENT == requestCode)
		{ // 附件
			String filePath = null != data ? data.getStringExtra(RKCloudChatSelectFileActivity.INTENT_RETURN_KEY_FILEPATH) : null;
			if (!TextUtils.isEmpty(filePath))
			{
				// 生成附件消息并发送
				FileMessage fileObj = FileMessage.buildMsg(mChatId, filePath);
				fileObj.setmMsgSummary(getString(R.string.rkcloud_chat_notify_file));
				sendMms(fileObj);
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		// 拍照时保存图片名称
		if (!TextUtils.isEmpty(mTakePhotoTempName))
		{
			outState.putString("takephonename", mTakePhotoTempName);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		// 恢复时如果包含takephonename字符串，表示要获取拍照时保存的图片名称
		if (savedInstanceState.containsKey("takephonename"))
		{
			mTakePhotoTempName = savedInstanceState.getString("takephonename");
			savedInstanceState.remove("takephonename");
		}
	}

	@Override
	public void onLoadImageCompleteDelayNotify(IMAGE_REQUEST_TYPE type, List<RKCloudChatImageResult> result)
	{
		mAdapter.notifyDataSetChanged();
		if (IMAGE_REQUEST_TYPE.GET_MSG_THUMBNAIL == type || IMAGE_REQUEST_TYPE.GET_MSG_VIDEO_THUMBNAIL == type)
		{
			if (!mScrollStatus && mAllMsgsData.size() - 1 == mListView.getLastVisiblePosition())
			{
				jumpListBottom();
			}
		}
	}

	private void initUI()
	{
		// 设置title
		mTitleTV = (TextView) findViewById(R.id.txt_title);
		mTitleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		mTitleTV.setText(R.string.bnt_return);

		text_title_content = (TextView) findViewById(R.id.text_title_content);

		// 显示会话管理的图标
		mManagerBtn = (ImageButton) findViewById(R.id.title_imgbtns_rightbtn);
		mManagerBtn.setVisibility(View.VISIBLE);
		mManagerBtn.setImageResource(R.drawable.rkcloud_chat_manage_single);

		mRootView = (RelativeLayout) findViewById(R.id.rootlayout);

		mRecordTextModelSwitch = (ImageButton) findViewById(R.id.textmodel_switcher);
		mRecordBnt = (Button) findViewById(R.id.btn_record);

		mTextLayout = (LinearLayout) findViewById(R.id.layout_textmodel);
		mTextContentLayout = (LinearLayout) findViewById(R.id.textcontent_layout);
		mSmiliesEditText = (RKCloudChatEmojiEditText) findViewById(R.id.msgcontent);
		if (null != RKCloudChatMessageManager.getInstance(this))
		{
			InputFilter filter = new InputFilter.LengthFilter(RKCloudChatMessageManager.getInstance(this).getTextMaxLength());
			mSmiliesEditText.setFilters(new InputFilter[] { filter });
		}
		mSendBnt = (Button) findViewById(R.id.btn_send);

		mEmojiSwitch = (ImageView) findViewById(R.id.emoji_switcher);
		mEmojiLayout = (LinearLayout) findViewById(R.id.layout_emoji);
		mEmojiPager = (ViewPager) findViewById(R.id.page_emoji);
		mPagerPointsLayout = (LinearLayout) findViewById(R.id.layout_emoji_pagerpoints);

		mAttachSwitch = (ImageButton) findViewById(R.id.attach_switcher);
		mAttachLayout = (LinearLayout) findViewById(R.id.layout_attach);
		mAttachPager = (ViewPager) findViewById(R.id.page_attach);
		mAttachPointsLayout = (LinearLayout) findViewById(R.id.layout_attach_pagerpoints);

		mLoadingHistoryLayout = (LinearLayout) findViewById(R.id.layout_loadingmore);
		mListView = (ListView) findViewById(R.id.list);
		mLoadMsgBar = (ProgressBar) findViewById(R.id.loadingMsgList);
		mTipReceiveMsg = (TextView) findViewById(R.id.tip_receivemsg);
		mUnreadMsgCntTip = (TextView) findViewById(R.id.tip_unreadmsgcnt);

		mEnterMeetingTip = (TextView) findViewById(R.id.enter_multimeeting);
		mEnterMeetingTip.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// TODO 与多人语音结合时使用
				RKCloudMeetingDemoManager.getInstance(RKCloudChatMsgActivity.this).enterMutliMeeting(RKCloudChatMsgActivity.this, mChatId);
			}
		});
	}

	/*
	 * 设置录音未选中时的内容
	 */
	private void initRecordParam()
	{
		// 设置录音未选中时的背景色，并且为未选中状态
		mRecordBnt.setSelected(false);
		mRecordPopupWindow.setTip(getString(R.string.rkcloud_chat_audio_recording_finger_up_tip), false);
		mCancelSendVoiceMsg = false;
	}

	/*
	 * 初始化表情区域内容
	 */
	private void initEmojiZone()
	{
		final int showCountPerPage = 20;// 每页显示的个数
		int column = 7;// 设置每行显示的个数
		int space = (int) getResources().getDimension(R.dimen.rkcloud_chat_msg_gridvide_space);// 行间距和列间距

		int emojiCount = RKCloudChatEmojiRes.EMOJI_RESIDS.length;
		int pageCount = emojiCount / showCountPerPage;// 表情显示的总页数
		if (0 != emojiCount % showCountPerPage)
		{
			pageCount++;
		}

		// 初始化
		mPagerPointsLayout.removeAllViews();
		mEmojiViews.clear();

		// 初始化导航点数
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.leftMargin = (int) getResources().getDimension(R.dimen.rkcloud_chat_msg_points_marginleft);
		params.rightMargin = (int) getResources().getDimension(R.dimen.rkcloud_chat_msg_points_marginleft);
		ImageView imageView = null;
		for (int i = 0; i < pageCount; i++)
		{
			imageView = new ImageView(this);
			imageView.setLayoutParams(params);
			imageView.setImageResource(R.drawable.rkcloud_chat_img_point);
			if (0 == i)
			{
				imageView.setSelected(true);
			}
			else
			{
				imageView.setSelected(false);
			}

			mPagerPointsLayout.addView(imageView);
		}

		for (int i = 0; i < pageCount; i++)
		{
			final int currPage = i;
			final List<Integer> emojiDatas = new ArrayList<Integer>();
			for (int j = 0; j < showCountPerPage; j++)
			{
				if (i * showCountPerPage + j < emojiCount)
				{
					emojiDatas.add(RKCloudChatEmojiRes.EMOJI_RESIDS[i * showCountPerPage + j]);
				}
			}

			emojiDatas.add(Integer.valueOf(0));// 添加最后一个图标作为删除使用
			// 创建GridView用于实现表情
			GridView gridView = new GridView(this);
			gridView.setNumColumns(column);
			gridView.setHorizontalSpacing(space);
			gridView.setVerticalSpacing(space);
			gridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
			gridView.setSelector(new ColorDrawable(android.graphics.Color.TRANSPARENT));
			gridView.setCacheColorHint(android.graphics.Color.TRANSPARENT);
			gridView.setVerticalScrollBarEnabled(false);
			gridView.setHorizontalScrollBarEnabled(false);
			gridView.setAdapter(new RKCloudChatEmojiAdapter(RKCloudChatMsgActivity.this, emojiDatas));
			gridView.setOnItemClickListener(new OnItemClickListener()
			{
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
				{
					int id = emojiDatas.get(arg2);
					if (id != 0)
					{
						// 插入表情图标
						mSmiliesEditText.insertIcon(RKCloudChatEmojiRes.EMOJI_REGX[currPage * showCountPerPage + arg2]);
					}
					else
					{
						// 删除操作
						if (TextUtils.isEmpty(mSmiliesEditText.getText()))
						{
							return;
						}
						int selectionStart = mSmiliesEditText.getSelectionStart();// 获取光标的位置
						if (selectionStart <= 0)
						{
							return;
						}

						String tempStr = mSmiliesEditText.getText().toString().substring(0, selectionStart);
						int emojiAliasLength = RKCloudChatEmojiRes.EMOJI_REGP_LENGTH;
						if (tempStr.length() >= emojiAliasLength)
						{
							String str = tempStr.substring(selectionStart - emojiAliasLength, selectionStart);
							if (str.matches(RKCloudChatEmojiRes.EMOJI_REGP_RULE))
							{
								mSmiliesEditText.getEditableText().delete(selectionStart - emojiAliasLength, selectionStart);

							}
							else
							{
								mSmiliesEditText.getEditableText().delete(selectionStart - 1, selectionStart);
							}

						}
						else
						{
							mSmiliesEditText.getEditableText().delete(selectionStart - 1, selectionStart);
						}
					}
				}
			});

			mEmojiViews.add(gridView);
		}

		// 设置表情翻页的adapter
		mEmojiPager.setAdapter(new PagerAdapter()
		{
			@Override
			public int getCount()
			{
				return mEmojiViews.size();
			}

			@Override
			public int getItemPosition(Object object)
			{
				return super.getItemPosition(object);
			}

			@Override
			public boolean isViewFromObject(View arg0, Object arg1)
			{
				return arg1 == arg0;
			}

			@Override
			public void destroyItem(ViewGroup container, int position, Object object)
			{
				if (position < mEmojiViews.size())
				{
					((ViewPager) container).removeView(mEmojiViews.get(position));
				}
			}

			@Override
			public Object instantiateItem(ViewGroup container, int position)
			{
				View view = mEmojiViews.get(position);
				((ViewPager) container).addView(view, 0);
				return view;
			}
		});

		// 设置表情页码发生变化的监听事件
		mEmojiPager.setOnPageChangeListener(new SimpleOnPageChangeListener()
		{
			@Override
			public void onPageSelected(int position)
			{
				super.onPageSelected(position);
				int count = mPagerPointsLayout.getChildCount();
				ImageView imageView = null;
				for (int i = 0; i < count; i++)
				{
					imageView = (ImageView) mPagerPointsLayout.getChildAt(i);
					if (position != i)
					{
						imageView.setSelected(false);
					}
					else
					{
						imageView.setSelected(true);
					}
				}
			}
		});

		mPagerPointsLayout.setVisibility(pageCount <= 1 ? View.GONE : View.VISIBLE);
	}

	/*
	 * 初始化附加功能区域内容
	 */
	private void initAttachZone()
	{
		int showCountPerPage = 8;// 每页显示的个数
		int column = 4;// 设置每行显示的个数
		int space = (int) getResources().getDimension(R.dimen.rkcloud_chat_msg_gridvide_space);// 行间距和列间距

		// 初始化显示的内容
		List<RKCloudChatMsgAttachItem> datas = new ArrayList<RKCloudChatMsgAttachItem>();
		// 添加图片
		RKCloudChatMsgAttachItem itemObj = new RKCloudChatMsgAttachItem(RKCloudChatMsgAttachItem.ATTACH_TYPE.SELECTLOCALIMAGE, R.drawable.rkcloud_chat_attachope_selectimg,
				getString(R.string.rkcloud_chat_msgfooter_attach_img));
		datas.add(itemObj);
		// 添加拍照
		itemObj = new RKCloudChatMsgAttachItem(RKCloudChatMsgAttachItem.ATTACH_TYPE.TAKEPHOTO, R.drawable.rkcloud_chat_attachope_takephoto,
				getString(R.string.rkcloud_chat_msgfooter_attach_takephoto));
		datas.add(itemObj);
		// 添加拍摄小视频
		itemObj = new RKCloudChatMsgAttachItem(RKCloudChatMsgAttachItem.ATTACH_TYPE.RECORDVIDEO, R.drawable.rkcloud_chat_attachope_video, getString(R.string.rkcloud_chat_msgfooter_attach_video));
		datas.add(itemObj);
		// 添加文件
		itemObj = new RKCloudChatMsgAttachItem(RKCloudChatMsgAttachItem.ATTACH_TYPE.FILE, R.drawable.rkcloud_chat_attachope_file, getString(R.string.rkcloud_chat_msgfooter_attach_file));
		datas.add(itemObj);
		if (SingleChat.class.equals(mChatClassObj))
		{
			// TODO 单聊与音视频通话结合时添加
			itemObj = new RKCloudChatMsgAttachItem(RKCloudChatMsgAttachItem.ATTACH_TYPE.AUDIOCALL, R.drawable.rkcloud_chat_attachope_audiocall,
					getString(R.string.rkcloud_chat_msgfooter_attach_audiocall));
			datas.add(itemObj);
			itemObj = new RKCloudChatMsgAttachItem(RKCloudChatMsgAttachItem.ATTACH_TYPE.VIDEOCALL, R.drawable.rkcloud_chat_attachope_videocall,
					getString(R.string.rkcloud_chat_msgfooter_attach_videocall));
			datas.add(itemObj);
		}
		else if (GroupChat.class.equals(mChatClassObj))
		{
			// TODO 群聊与多人语音结合时添加
			itemObj = new RKCloudChatMsgAttachItem(RKCloudChatMsgAttachItem.ATTACH_TYPE.MULTIMEETING, R.drawable.rkcloud_chat_attachope_mutlimeeting,
					getString(R.string.rkcloud_chat_msgfooter_attach_multimeeting));
			datas.add(itemObj);
		}

		int attachCount = datas.size();// 可操作的功能总个数
		int pageCount = attachCount / showCountPerPage;// 显示的总页数
		if (0 != attachCount % showCountPerPage)
		{
			pageCount++;
		}

		// 初始化
		mAttachPointsLayout.removeAllViews();
		mAttachViews.clear();

		// 初始化导航点数
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.leftMargin = (int) getResources().getDimension(R.dimen.rkcloud_chat_msg_points_marginleft);
		params.rightMargin = (int) getResources().getDimension(R.dimen.rkcloud_chat_msg_points_marginleft);
		ImageView imageView = null;
		for (int i = 0; i < pageCount; i++)
		{
			imageView = new ImageView(this);
			imageView.setLayoutParams(params);
			imageView.setImageResource(R.drawable.rkcloud_chat_img_point);
			if (0 == i)
			{
				imageView.setSelected(true);
			}
			else
			{
				imageView.setSelected(false);
			}

			mAttachPointsLayout.addView(imageView);
		}
		for (int i = 0; i < pageCount; i++)
		{
			final List<RKCloudChatMsgAttachItem> attachDatas = new ArrayList<RKCloudChatMsgAttachItem>();
			for (int j = 0; j < showCountPerPage; j++)
			{
				if (i * showCountPerPage + j < attachCount)
				{
					attachDatas.add(datas.get(i * showCountPerPage + j));
				}
			}
			// 创建GridView
			GridView gridView = new GridView(this);
			gridView.setNumColumns(column);
			gridView.setHorizontalSpacing(space);
			gridView.setVerticalSpacing(space);
			gridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
			gridView.setSelector(new ColorDrawable(android.graphics.Color.TRANSPARENT));
			gridView.setCacheColorHint(android.graphics.Color.TRANSPARENT);
			gridView.setVerticalScrollBarEnabled(false);
			gridView.setHorizontalScrollBarEnabled(false);
			gridView.setAdapter(new RKCloudChatAttachAdapter(RKCloudChatMsgActivity.this, attachDatas));
			gridView.setOnItemClickListener(new OnItemClickListener()
			{
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id)
				{
					RKCloudChatMsgAttachItem obj = attachDatas.get(position);
					if (RKCloudChatMsgAttachItem.ATTACH_TYPE.SELECTLOCALIMAGE == obj.type)
					{
						Intent intent = new Intent(Intent.ACTION_PICK);
						intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
						startActivityForResult(intent, INTENT_RESULT_CHOOSE_PICTURE);

					}
					else if (RKCloudChatMsgAttachItem.ATTACH_TYPE.TAKEPHOTO == obj.type)
					{
						// 创建目录，并生成临时图片名称
						RKCloudChatTools.createDirectory(RKCloudChatConstants.MMS_TEMP_PATH);
						mTakePhotoTempName = String.format("%stakephoto_%d.jpg", RKCloudChatConstants.MMS_TEMP_PATH, System.currentTimeMillis());
						File image = new File(mTakePhotoTempName);

						Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
						intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image));
						startActivityForResult(intent, INTENT_RESULT_TAKE_PHOTO);

					}
					else if (RKCloudChatMsgAttachItem.ATTACH_TYPE.RECORDVIDEO == obj.type)
					{
						startActivityForResult(new Intent(RKCloudChatMsgActivity.this, RKCloudChatRecordVideoActivity.class), INTENT_RESULT_CHOOSE_VIDEO);

					}
					else if (RKCloudChatMsgAttachItem.ATTACH_TYPE.FILE == obj.type)
					{
						startActivityForResult(new Intent(RKCloudChatMsgActivity.this, RKCloudChatSelectFileActivity.class), INTENT_RESULT_CHOOSE_ATTACHEMENT);

					}
					else if (RKCloudChatMsgAttachItem.ATTACH_TYPE.VIDEOCALL == obj.type)
					{
						// TODO 与音视频通话结合时使用
						RKCloudAVDemoManager.getInstance(RKCloudChatMsgActivity.this).dial(RKCloudChatMsgActivity.this, mChatId, true);

					}
					else if (RKCloudChatMsgAttachItem.ATTACH_TYPE.AUDIOCALL == obj.type)
					{
						// TODO 与音视频通话结合时使用
						RKCloudAVDemoManager.getInstance(RKCloudChatMsgActivity.this).dial(RKCloudChatMsgActivity.this, mChatId, false);

					}
					else if (RKCloudChatMsgAttachItem.ATTACH_TYPE.MULTIMEETING == obj.type)
					{// 多人语音
						// TODO 与多人语音结合时使用
						RKCloudMeetingDemoManager.getInstance(RKCloudChatMsgActivity.this).enterMutliMeeting(RKCloudChatMsgActivity.this, mChatId);
					}
				}
			});
			mAttachViews.add(gridView);
		}

		// 设置翻页的adapter
		mAttachPager.setAdapter(new PagerAdapter()
		{
			@Override
			public int getCount()
			{
				return mAttachViews.size();
			}

			@Override
			public int getItemPosition(Object object)
			{
				return super.getItemPosition(object);
			}

			@Override
			public boolean isViewFromObject(View arg0, Object arg1)
			{
				return arg1 == arg0;
			}

			@Override
			public void destroyItem(ViewGroup container, int position, Object object)
			{
				if (position < mAttachViews.size())
				{
					((ViewPager) container).removeView(mAttachViews.get(position));
				}
			}

			@Override
			public Object instantiateItem(ViewGroup container, int position)
			{
				View view = mAttachViews.get(position);
				((ViewPager) container).addView(view, 0);
				return view;
			}
		});

		// 设置页码发生变化的监听事件
		mAttachPager.setOnPageChangeListener(new SimpleOnPageChangeListener()
		{
			@Override
			public void onPageSelected(int position)
			{
				super.onPageSelected(position);
				int count = mAttachPointsLayout.getChildCount();
				ImageView imageView = null;
				for (int i = 0; i < count; i++)
				{
					imageView = (ImageView) mAttachPointsLayout.getChildAt(i);
					if (position != i)
					{
						imageView.setSelected(false);
					}
					else
					{
						imageView.setSelected(true);
					}
				}
			}
		});

		mAttachPointsLayout.setVisibility(pageCount <= 1 ? View.GONE : View.VISIBLE);
	}

	private void initListeners()
	{
		mTitleTV.setOnClickListener(mExitListener);
		mManagerBtn.setOnClickListener(this);

		mRecordTextModelSwitch.setOnClickListener(this);
		mSendBnt.setOnClickListener(this);
		mEmojiSwitch.setOnClickListener(this);
		mAttachSwitch.setOnClickListener(this);

		mRecordBnt.setOnTouchListener(this);

		mUnreadMsgCntTip.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				int index = 0;
				for (RKCloudChatBaseMessage msgObj : mAllMsgsData)
				{
					if (unreadLeastMsgId == msgObj.getMsgCreasingId())
					{
						mListView.setSelection(index);
						return;
					}
					index++;
				}

				// 表示已有的数据中不存在，需要重新获取
				startQuery(QUERY_TYPE_LOAD_UNREAD_DATA);
			}
		});
		mTipReceiveMsg.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				jumpListBottom();
			}
		});

		// 文本消息输入框相关的监听事件
		mSmiliesEditText.setOnFocusChangeListener(new OnFocusChangeListener()
		{
			@Override
			public void onFocusChange(View v, boolean hasFocus)
			{
				mTextContentLayout.setBackgroundResource(hasFocus ? R.drawable.rkcloud_chat_emojiedittext_bg_focused : R.drawable.rkcloud_chat_emojiedittext_bg_normal);
				if (hasFocus)
				{
					controllMoreOpeZoneOpen(false);
					controllEmojiZoneOpen(false);
				}
				jumpListBottom();
			}
		});
		mSmiliesEditText.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				controllMoreOpeZoneOpen(false);
				controllEmojiZoneOpen(false);
				jumpListBottom();
			}
		});
		mSmiliesEditText.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				if (TextUtils.isEmpty(s))
				{
					mSendBnt.setVisibility(View.GONE);
					mAttachSwitch.setVisibility(View.VISIBLE);
				}
				else
				{
					mSendBnt.setVisibility(View.VISIBLE);
					mAttachSwitch.setVisibility(View.GONE);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
			}

			@Override
			public void afterTextChanged(Editable s)
			{
			}
		});

		mListView.setOnTouchListener(this);
		// 设置列表的滚动事件
		mListView.setOnScrollListener(new OnScrollListener()
		{
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState)
			{
				mScrollStatus = true;
				controllMoreOpeZoneOpen(false);
				controllEmojiZoneOpen(false);
				hideKeyboard();

				if (OnScrollListener.SCROLL_STATE_IDLE == scrollState)
				{
					if (0 == view.getFirstVisiblePosition() && !mLoadingHistoryData && !mLoadHistoryDataFinished)
					{
						// 加载历史数据
						mLoadingHistoryData = true;
						mLoadingHistoryLayout.setVisibility(View.VISIBLE);
						startQuery(QUERY_TYPE_LOAD_HISTORY_DATA);
					}
					else if (mAllMsgsData.size() - 1 == view.getLastVisiblePosition() && !mLoadingNewData && !mLoadNewDataFinished)
					{
						// 加载新数据
						mLoadingNewData = true;
						startQuery(QUERY_TYPE_LOAD_NEW_DATA);
					}
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
			{
			}
		});
	}

	private void initData(Bundle savedInstanceState, Intent intent)
	{
		singleId = intent.getStringExtra(INTENT_KEY_MSGLIST_CHATID);
		String groupId = intent.getStringExtra(INTENT_KEY_MSGLIST_GROUPID);
		if (null != intent.getStringExtra(INTENT_KEY_MSGLIST_MSGID))
		{
			mMsgId = intent.getStringExtra(INTENT_KEY_MSGLIST_MSGID);
		}
		if (TextUtils.isEmpty(RKCloud.getUserName()))
		{
			finish();
			return;
		}
		if ((TextUtils.isEmpty(singleId) && TextUtils.isEmpty(groupId)) || (!TextUtils.isEmpty(singleId) && !TextUtils.isEmpty(groupId)))
		{
			finish();
			return;
		}
		else if (TextUtils.isEmpty(singleId) || !TextUtils.isEmpty(groupId))
		{
			mChatId = groupId;
			mChatClassObj = GroupChat.class;
		}
		else if (!TextUtils.isEmpty(singleId) || TextUtils.isEmpty(groupId))
		{
			mChatId = singleId;
			mChatClassObj = SingleChat.class;
		}
		// 禁止给自己发消息
		if (mChatId.equalsIgnoreCase(RKCloud.getUserName()))
		{
			RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_forbid_enterselfmms));
			finish();
			return;
		}
		mCurrAccount = RKCloud.getUserName();

		// 实例化manager对象
		mClipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		mChatConfigManager = RKCloudChatConfigManager.getInstance(this);
		mMmsManager = RKCloudChatMmsManager.getInstance(this);
		mContactManager = RKCloudChatContactManager.getInstance(this);
		mAudioHelper = RKCloudChatPlayAudioMsgTools.getInstance(this);

		// 设置管理图标
		if (SingleChat.class.equals(mChatClassObj))
		{
			mManagerBtn.setImageResource(R.drawable.rkcloud_chat_manage_single);
		}
		else if (GroupChat.class.equals(mChatClassObj))
		{
			mManagerBtn.setImageResource(R.drawable.rkcloud_chat_manage_mutli);
		}

		// 初始化数据
		mScrollStatus = false;
		mLoadHistoryDataFinished = false;
		mLoadingHistoryData = false;
		mLoadingHistoryLayout.setVisibility(View.GONE);
		mLoadNewDataFinished = false;
		mLoadingNewData = false;

		mFirstShowUnreadTip = true;
		mUnreadCnt = 0;

		lastLoadMsgCreaingId = 0;
		remberViewMsgInChatId = null;
		lastAudioSerialNum = null;
		mCancelSendVoiceMsg = false;

		newLoadMsgCreaingId = 0;

		// 实例化音频窗口对象并设置其监听事件
		mRecordPopupWindow = new RKCloudChatRecordPopupWindow(this, mRootView);
		mRecordPopupWindow.setOnRecordDialogDismissListener(this);
		new MediaRecorder();// 提前做出是否通行的选择，防止录音过程中的异常

		// 绑定List和Adapter
		mAllMsgsData = new ArrayList<RKCloudChatBaseMessage>();
		mContacts = new HashMap<String, RKCloudChatContact>();
		// 如果是单聊会话则查询相关的联系人信息
		if (SingleChat.class.equals(mChatClassObj))
		{
			mContacts.put(mChatId, mContactManager.getContactInfo(mChatId));
			if (!TextUtils.isEmpty(mCurrAccount))
			{
				mContacts.put(mCurrAccount, mContactManager.getContactInfo(mCurrAccount));
			}
			// 同步单聊的个人信息
			mContactManager.syncContactInfo(mCurrAccount);
		}
		mAdapter = new RKCloudChatMsgAdapter(this, mChatClassObj, mAllMsgsData, mContacts, mUiHandler);
		mListView.setAdapter(mAdapter);

		mLoadMsgBar.setVisibility(View.VISIBLE);
		// 获取草稿箱内容并设置
		String draftContent = mMmsManager.getDraft(mChatId);
		if (null != draftContent)
		{
			CharSequence currContent = RKCloudChatTools.parseMsgFace(RKCloudChatMsgActivity.this, draftContent, 0, 3);
			mSmiliesEditText.setText(currContent);
			mSmiliesEditText.setSelection(currContent.length());
			mSmiliesEditText.requestFocus();
			showKeyboard();
		}
		else
		{
			hideKeyboard();
		}
		// 设置当前为文本模式
		controllShowTextModel(true);
		// 初始化表情
		initEmojiZone();
		// 初始化附加功能区域内容
		initAttachZone();
	}

	/*
	 * 显示会话的基本信息
	 */
	private void showConvInfo()
	{
		RKCloudChatBaseChat chatObj = mMmsManager.queryChat(mChatId);
		if (null == chatObj)
		{
			// 群不存在时结束
			if (GroupChat.class.equals(mChatClassObj))
			{
				finish();
				return;
			}
			else if (SingleChat.class.equals(mChatClassObj))
			{
				chatObj = SingleChat.buildSingleChat(mChatId);
				// 清空已有的消息
				mAllMsgsData.clear();
				mAdapter.notifyDataSetChanged();
			}
		}

		mChatObj = chatObj;

		// 设置名称
		if (GroupChat.class.equals(mChatClassObj))
		{
			GroupChat groupChatObj = (GroupChat) mChatObj;
			text_title_content.setText(String.format("%s(%d)", groupChatObj.getChatShowName(), mChatObj.getUserCounts()));
		}
		else
		{
			RKCloudChatContact contactObj = mContacts.get(mChatId);
			text_title_content.setText(null != contactObj ? contactObj.getShowName() : mChatId);
		}
		if (null != mChatConfigManager && mChatConfigManager.getVoicePlayModel())
		{
			mTitleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, R.drawable.rkcloud_chat_playmodel_earphone, 0);
		}
		else
		{
			mTitleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		}

		// 设置聊天背景
		Drawable bgRes = null;
		if (null != mChatObj && !TextUtils.isEmpty(mChatObj.getBackgroundImagePath()) && new File(mChatObj.getBackgroundImagePath()).exists())
		{
			Bitmap map = RKCloudChatImageTools.resizeBitmap(mChatObj.getBackgroundImagePath(), RKCloudChatScreenTools.getInstance(this).getScreenWidth(),
					RKCloudChatScreenTools.getInstance(this).getScreenHeight());
			bgRes = null != map ? new BitmapDrawable(getResources(), map) : null;
		}

		if (null != bgRes)
		{
			mRootView.setBackgroundDrawable(bgRes);

		}
		else
		{
			mRootView.setBackgroundResource(R.color.rkcloud_chat_ui_bgcolor);
		}
	}

	private void showUnreadCntTip(int unreadcnt)
	{
		// 未读条数的显示
		if (unreadcnt > 0 && unreadcnt >= TIP_UNREADCNT_LEAST_COUNT)
		{
			mUnreadCnt = unreadcnt;
			mUnreadMsgCntTip.setVisibility(View.VISIBLE);
			mUnreadMsgCntTip.setText(getString(R.string.rkcloud_chat_unreadcnt_tip, mUnreadCnt));
			unreadLeastMsgId = mMmsManager.getLeastMsgIdOfUnreadMsgs(mChatId, mUnreadCnt);
		}
		else
		{
			mUnreadMsgCntTip.setVisibility(View.GONE);
		}
	}

	/*
	 * 控制文本/音频模式转换
	 * @param show true:显示文本 false:显示语音
	 */
	private void controllShowTextModel(boolean show)
	{
		if (show)
		{
			mTextLayout.setVisibility(View.VISIBLE);
			mRecordBnt.setVisibility(View.GONE);
			mAttachSwitch.setVisibility(TextUtils.isEmpty(mSmiliesEditText.getText().toString()) ? View.VISIBLE : View.GONE);
			mSendBnt.setVisibility(TextUtils.isEmpty(mSmiliesEditText.getText().toString()) ? View.GONE : View.VISIBLE);
			mRecordTextModelSwitch.setImageResource(R.drawable.rkcloud_chat_msgfooter_mode_audio);
		}
		else
		{
			mTextLayout.setVisibility(View.GONE);
			mRecordBnt.setVisibility(View.VISIBLE);
			mAttachSwitch.setVisibility(View.VISIBLE);
			mRecordTextModelSwitch.setImageResource(R.drawable.rkcloud_chat_msgfooter_mode_keyboard);
		}
	}

	/*
	 * 控制更多操作区域是否显示
	 */
	private void controllMoreOpeZoneOpen(boolean open)
	{
		if (open)
		{
			mTextContentLayout.requestFocus();
			mAttachLayout.setVisibility(View.VISIBLE);
			mAttachSwitch.setSelected(true);
		}
		else
		{
			mAttachLayout.setVisibility(View.GONE);
			mAttachSwitch.setSelected(false);
			mAttachPager.setCurrentItem(0);
		}
	}

	/*
	 * 控制表情区域是否显示
	 */
	private void controllEmojiZoneOpen(boolean open)
	{
		if (open)
		{
			mEmojiLayout.setVisibility(View.VISIBLE);
			mEmojiSwitch.setSelected(true);
		}
		else
		{
			mEmojiLayout.setVisibility(View.GONE);
			mEmojiSwitch.setSelected(false);
			mEmojiPager.setCurrentItem(0);
		}
	}

	/*
	 * 隐藏软键盘
	 */
	private void hideKeyboard()
	{
		if (WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN != getWindow().getAttributes().softInputMode)
		{
			mInputMethodManager.hideSoftInputFromWindow(mSmiliesEditText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}

	private void showKeyboard()
	{
		if (WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE != getWindow().getAttributes().softInputMode)
		{
			mInputMethodManager.showSoftInput(mSmiliesEditText, InputMethodManager.SHOW_FORCED);
		}
	}

	/*
	 * 查询数据
	 */
	void startQuery(int queryType)
	{
		if (null == mQueryThread)
		{
			mQueryThread = new QueryHandlerThread("QueryMsgListActivityThread");
			mQueryThread.start();
		}
		mQueryThread.startQuery(queryType);
	}

	/*
	 * 查询数据的线程类
	 */
	private class QueryHandlerThread extends HandlerThread implements Callback
	{
		private Handler mQuerHandler;

		public QueryHandlerThread(String name)
		{
			super(name);
		}

		public void startQuery(int queryType)
		{
			if (null == mQuerHandler)
			{
				mQuerHandler = new Handler(getLooper(), this);
			}
			if (!mQuerHandler.hasMessages(queryType))
			{
				mQuerHandler.sendEmptyMessage(queryType);
			}

		}

		@Override
		public boolean handleMessage(Message msg)
		{
			if (msg.what == QUERY_TYPE_LOAD_DATA)
			{
				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.MSG_LOAD_DATA_FINISHED;
				message.arg1 = 0;
				message.obj = mMmsManager.queryMmsList(mChatId, lastLoadMsgCreaingId, RKCloudChatConstants.LOAD_MSG_DEFAULT_COUNT);
				message.sendToTarget();

			}
			else if (msg.what == QUERY_TYPE_LOAD_UNREAD_DATA)
			{
				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.MSG_LOAD_DATA_FINISHED;
				message.arg1 = 1;
				message.obj = mMmsManager.queryMmsList(mChatId, unreadLeastMsgId, 0);
				message.sendToTarget();

			}
			else if (msg.what == QUERY_TYPE_LOAD_HISTORY_DATA)
			{
				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.MSG_LOAD_HISTROY_DATA_FINISHED;
				message.obj = mMmsManager.queryHistoryMmsList(mChatId, lastLoadMsgCreaingId, RKCloudChatConstants.LOAD_MSG_DEFAULT_COUNT);
				message.sendToTarget();

			}
			else if (msg.what == QUERY_TYPE_LOAD_SEARCH_DATA)
			{
				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.MSG_LOAD_SEARCH_DATA_FINISHED;
				message.arg1 = 0;
				message.obj = mMmsManager.queryLocalChatMsgs(mChatId, mMsgId, mOnceCount);
				message.sendToTarget();
			}
			else if (msg.what == QUERY_TYPE_LOAD_NEW_DATA)
			{
				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.MSG_LOAD_NEW_DATA_FINISHED;
				message.obj = mMmsManager.queryNewChatMsgs(mChatId, newLoadMsgCreaingId, RKCloudChatConstants.LOAD_MSG_DEFAULT_COUNT);
				message.sendToTarget();
			}

			return true;
		}
	}

	/**
	 * 定义上下文菜单
	 * 
	 * @param msgObj
	 */
	public void showContextMenu(final RKCloudChatBaseMessage msgObj)
	{
		// 时间提示条、事件类型的消息、发送中和下载中的消息、以及提示扩展是提示类型的消息时无上下文菜单
		MSG_STATUS status = msgObj.getStatus();
		if (msgObj instanceof TipMessage || MSG_STATUS.SEND_SENDING == status || MSG_STATUS.RECEIVE_DOWNING == status || RKCloudChatConstants.FLAG_LOCAL_TIPMESSAGE.equals(msgObj.getExtension()))
		{
			return;
		}

		// 设置显示的条目内容
		List<Integer> ids = new ArrayList<Integer>();
		List<String> contents = new ArrayList<String>();
		// 发送失败的消息时增加“重发”
		if (MSG_STATUS.SEND_FAILED == status)
		{
			ids.add(CONTEXT_MENU_RESEND);
			contents.add(getString(R.string.rkcloud_chat_msglist_context_resend));
		}
		// 文本消息时增加“复制”
		if (msgObj instanceof TextMessage)
		{
			ids.add(CONTEXT_MENU_COPY);
			contents.add(getString(R.string.rkcloud_chat_msglist_context_copy));
		}
		else if (msgObj instanceof AudioMessage)
		{
			// 语音消息增加“使用听筒模式” or “使用扬声器模式”
			if (null != mChatConfigManager)
			{
				if (mChatConfigManager.getVoicePlayModel())
				{
					ids.add(CONTEXT_MENU_PLAY_WITH_SPEAKERPHONE);
					contents.add(getString(R.string.rkcloud_chat_msglist_context_speakerphone));
				}
				else
				{
					ids.add(CONTEXT_MENU_PLAY_WITH_EARPHONE);
					contents.add(getString(R.string.rkcloud_chat_msglist_context_earphone));
				}
			}
		}
		// 增加“转发”、“分享”
		if (msgObj instanceof TextMessage)
		{
			ids.add(CONTEXT_MENU_FORWARD);
			contents.add(getString(R.string.rkcloud_chat_msglist_context_forward));

			ids.add(CONTEXT_MENU_SHARE);
			contents.add(getString(R.string.rkcloud_chat_msglist_context_share));
		}
		else if (msgObj instanceof ImageMessage || msgObj instanceof AudioMessage || msgObj instanceof VideoMessage || msgObj instanceof FileMessage)
		{
			String filePath = null;
			if (msgObj instanceof ImageMessage)
			{
				filePath = ((ImageMessage) msgObj).getFilePath();
			}
			else if (msgObj instanceof AudioMessage)
			{
				filePath = ((AudioMessage) msgObj).getFilePath();
			}
			else if (msgObj instanceof VideoMessage)
			{
				filePath = ((VideoMessage) msgObj).getFilePath();
			}
			else if (msgObj instanceof FileMessage)
			{
				filePath = ((FileMessage) msgObj).getFilePath();
			}
			if (!TextUtils.isEmpty(filePath) && new File(filePath).exists())
			{
				ids.add(CONTEXT_MENU_FORWARD);
				contents.add(getString(R.string.rkcloud_chat_msglist_context_forward));

				ids.add(CONTEXT_MENU_SHARE);
				contents.add(getString(R.string.rkcloud_chat_msglist_context_share));
			}
		}

		// 增加“删除”
		ids.add(CONTEXT_MENU_DEL);
		contents.add(getString(R.string.rkcloud_chat_msglist_context_delete));

		// 增加“撤回”
		if (RKCloudChatBaseMessage.MSG_DIRECTION.SEND == msgObj.getDirection() && msgObj.getSender().equals(RKCloud.getUserName()) && MSG_STATUS.SEND_SENDED == msgObj.getStatus()
				&& (System.currentTimeMillis() - msgObj.getCreatedTime() <= RKCloud.getRevokeMessageTimeout() * 1000))
		{
			ids.add(CONTEXT_MENU_REVOKE);
			contents.add(getString(R.string.rkcloud_chat_msglist_context_revoke));
		}

		// 由List转换为数组
		String[] realContents = (String[]) contents.toArray(new String[contents.size()]);
		final Integer[] realIds = (Integer[]) ids.toArray(new Integer[ids.size()]);

		// 获取联系人信息
		RKCloudChatContact contactObj = mContacts.get(msgObj.getSender());
		// 弹出上下文菜单项
		AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(null != contactObj ? contactObj.getShowName() : msgObj.getSender()).setItems(realContents,
				new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						switch (realIds[which])
						{
							case CONTEXT_MENU_RESEND: // 重发
								mMmsManager.reSendMms(msgObj.getMsgSerialNum());
								break;

							case CONTEXT_MENU_COPY: // 复制
								TextMessage textObj = (TextMessage) msgObj;
								mClipboardManager.setPrimaryClip(ClipData.newPlainText(null, textObj.getContent()));
								break;

							case CONTEXT_MENU_PLAY_WITH_EARPHONE:
								mChatConfigManager.setVoicePlayModel(true);
								mTitleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, R.drawable.rkcloud_chat_playmodel_earphone, 0);
								RKCloudChatTools.showToastText(RKCloudChatMsgActivity.this, getString(R.string.rkcloud_chat_switch_earphone));
								break;

							case CONTEXT_MENU_PLAY_WITH_SPEAKERPHONE:
								mChatConfigManager.setVoicePlayModel(false);
								mTitleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
								RKCloudChatTools.showToastText(RKCloudChatMsgActivity.this, getString(R.string.rkcloud_chat_switch_speaker));
								break;

							case CONTEXT_MENU_FORWARD:
								Intent forwardIntent = new Intent(RKCloudChatMsgActivity.this, RKCloudChatForwardActivity.class);
								forwardIntent.putExtra(RKCloudChatForwardActivity.INTENT_KEY_FUNC_TYPE, RKCloudChatForwardActivity.FUNC_FORWARD);
								forwardIntent.putExtra(RKCloudChatForwardActivity.INTENT_KEY_MSGSERIALNUM, msgObj.getMsgSerialNum());
								startActivity(forwardIntent);
								break;

							case CONTEXT_MENU_SHARE:
								Intent shareIntent = new Intent();
								shareIntent.setAction(Intent.ACTION_SEND);
								shareIntent.putExtra(RKCloudChatForwardActivity.INTENT_KEY_FUNC_TYPE, RKCloudChatForwardActivity.FUNC_INSIDE_SHARE);
								shareIntent.putExtra(RKCloudChatForwardActivity.INTENT_KEY_MSGSERIALNUM, msgObj.getMsgSerialNum());
								if (msgObj instanceof TextMessage)
								{
									shareIntent.setType("text/plain");
									shareIntent.putExtra(Intent.EXTRA_TEXT, msgObj.getContent());
								}
								else if (msgObj instanceof ImageMessage || msgObj instanceof AudioMessage || msgObj instanceof VideoMessage || msgObj instanceof FileMessage)
								{
									String fileName = null;
									String filePath = null;
									String mime = null;
									if (msgObj instanceof ImageMessage)
									{
										fileName = ((ImageMessage) msgObj).getFileName();
										filePath = ((ImageMessage) msgObj).getFilePath();
										mime = "image/*";
									}
									else if (msgObj instanceof AudioMessage)
									{
										fileName = ((AudioMessage) msgObj).getFileName();
										filePath = ((AudioMessage) msgObj).getFilePath();
										mime = "audio/*";
									}
									else if (msgObj instanceof VideoMessage)
									{
										fileName = ((VideoMessage) msgObj).getFileName();
										filePath = ((VideoMessage) msgObj).getFilePath();
										mime = "video/*";
									}
									else if (msgObj instanceof FileMessage)
									{
										fileName = ((FileMessage) msgObj).getFileName();
										filePath = ((FileMessage) msgObj).getFilePath();
										mime = "*/*";
									}
									shareIntent.setType(mime);
									shareIntent.putExtra(Intent.EXTRA_TITLE, fileName);
									shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
								}
								startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.rkcloud_chat_msglist_share_title)));
								break;

							case CONTEXT_MENU_DEL: // 删除
								mMmsManager.delMsg(msgObj.getMsgSerialNum(), mChatId);
								// 如果当前语音文件正在播放，则停止
								String currPlayingFile = mAudioHelper.getPlayingMsgSerialNum();
								if (msgObj.getMsgSerialNum().equals(currPlayingFile))
								{
									mAudioHelper.stopMsgOfAudio();
								}
								// 删除消息
								for (RKCloudChatBaseMessage obj : mAllMsgsData)
								{
									if (obj.getMsgSerialNum().equals(msgObj.getMsgSerialNum()))
									{
										mAllMsgsData.remove(obj);
										mAdapter.notifyDataSetChanged();
										break;
									}
								}
								break;

							case CONTEXT_MENU_REVOKE:// 撤回
								showProgressDialog();
								mMmsManager.revokeMsg(msgObj.getMsgSerialNum());
								break;
						}
					}
				});

		builder.create().show();
	}

	/**
	 * 向文本框内容添加内容
	 * 
	 * @param name
	 */
	public void appendContent(String name)
	{
		String showName = String.format("%s@%s ", mSmiliesEditText.getText(), name);
		mSmiliesEditText.setText(showName);
		mSmiliesEditText.setSelection(showName.length());
		mSmiliesEditText.requestFocus();

		controllShowTextModel(true);
		showKeyboard();
		jumpListBottom();
	}

	/*
	 * 发送消息
	 */
	private void sendMms(RKCloudChatBaseMessage msgObj)
	{
		if (null != msgObj)
		{
			mScrollStatus = false;
			mMmsManager.sendMms(msgObj);
			addMsg(msgObj, true);
			newLoadMsgCreaingId = msgObj.getMsgCreasingId();
		}
	}

	/*
	 * 添加消息
	 */
	private synchronized void addMsg(RKCloudChatBaseMessage msgObj, boolean needShowBottom)
	{
		// 是否存在
		boolean hasExist = false;
		for (RKCloudChatBaseMessage obj : mAllMsgsData)
		{
			if (obj.getMsgSerialNum().equals(msgObj.getMsgSerialNum()))
			{
				hasExist = true;
				break;
			}
		}
		if (!hasExist)
		{
			// 加载消息对象
			mAllMsgsData.add(msgObj);
			mAdapter.notifyDataSetChanged();

			if (needShowBottom)
			{
				jumpListBottom();
			}
		}
	}

	/*
	 * 控制消息提示条的显示
	 */
	private void showTipMsg(RKCloudChatBaseMessage msgObj)
	{
		String content = mMmsManager.getNotificationContent(mChatClassObj, msgObj, mContacts.get(msgObj.getSender()));
		tipMsgSerialNum = msgObj.getMsgSerialNum();
		if (mTipReceiveMsg.getVisibility() != View.VISIBLE)
		{
			mTipReceiveMsg.setVisibility(View.VISIBLE);
		}
		if (msgObj instanceof TextMessage)
		{
			mTipReceiveMsg.setText(RKCloudChatTools.parseMsgFace(this, content, -1, 2));
		}
		else
		{
			mTipReceiveMsg.setText(content);
		}

		mShowTipMsgTime = System.currentTimeMillis();
		mUiHandler.sendEmptyMessageDelayed(RKCloudChatUiHandlerMessage.HIDDEN_TIP_NEWMSG, 30000);
	}

	/*
	 * 隐藏消息提示
	 */
	private void hiddenTipMsg()
	{
		mTipReceiveMsg.setVisibility(View.INVISIBLE);
		tipMsgSerialNum = null;
	}

	/*
	 * 跳转到最底端
	 */
	private void jumpListBottom()
	{
		hiddenTipMsg();
		final int count = mAdapter.getCount();
		if (count > 0)
		{
			mUiHandler.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					mListView.setSelection(count - 1);
				}
			}, 100);
		}
	}

	/*
	 * 初始化列表数据
	 */
	private void loadListData(List<RKCloudChatBaseMessage> msgsList, boolean unreadMsgs)
	{
		mLoadMsgBar.setVisibility(View.GONE);
		mAllMsgsData.clear();

		if (null == msgsList || msgsList.size() == 0)
		{
			mAdapter.notifyDataSetChanged();
			showUnreadCntTip(0);
			mLoadHistoryDataFinished = true;
			mFirstShowUnreadTip = false;
			return;
		}
		else
		{
			if (msgsList.size() < RKCloudChatConstants.LOAD_MSG_DEFAULT_COUNT)
			{
				mLoadHistoryDataFinished = true;
			}
		}

		lastLoadMsgCreaingId = msgsList.get(0).getMsgCreasingId();// 记录已加载消息中自增ID值最小的一个消息ID值
		newLoadMsgCreaingId = msgsList.get(msgsList.size() - 1).getMsgCreasingId();// 记录已加载消息中自增ID值最大的一个消息ID值

		mAllMsgsData.addAll(msgsList);
		mAdapter.notifyDataSetChanged();

		if (unreadMsgs)
		{
			mListView.setSelection(0);

		}
		else
		{
			// 如果有定位的地方则显示，否则根据滚动条的滚动状态定位列表位置
			if (!TextUtils.isEmpty(remberViewMsgInChatId))
			{
				if (remberViewMsgInChatId.equalsIgnoreCase(mChatId))
				{
					mListView.setSelectionFromTop(remberFirstPosition, remberDistinceToTop);
				}
				else
				{
					mListView.setSelection(mAllMsgsData.size() - 1);
				}

				remberViewMsgInChatId = null;
				remberFirstPosition = -1;
				remberDistinceToTop = -1;

			}
			else
			{
				mListView.setSelection(mAllMsgsData.size() - 1);
			}
		}

		// 显示未读条数提示
		if (mFirstShowUnreadTip)
		{
			showUnreadCntTip(mChatObj.getUnReadMsgCnt());
			mFirstShowUnreadTip = false;
		}
	}

	/*
	 * 加载历史数据
	 */
	private void loadHistoryData(List<RKCloudChatBaseMessage> msgsList)
	{
		mLoadingHistoryData = false;
		mLoadingHistoryLayout.setVisibility(View.GONE);

		if (null == msgsList || msgsList.size() == 0)
		{
			mLoadHistoryDataFinished = true;
			return;

		}
		else if (msgsList.size() < RKCloudChatConstants.LOAD_MSG_DEFAULT_COUNT)
		{
			// 如果历史条数小于加载条数，表示消息已经全部加载完成
			mLoadHistoryDataFinished = true;
		}

		// 记录已加载消息中自增ID值最小的一个消息ID值
		lastLoadMsgCreaingId = msgsList.get(0).getMsgCreasingId();

		mAllMsgsData.addAll(0, msgsList);
		mAdapter.notifyDataSetChanged();
		// 延迟100ms定位
		final int position = msgsList.size();// 用于加载历史数据后定位
		mUiHandler.postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				mListView.setSelection(position);
			}
		}, 100);
	}

	/*
	 * 加载搜索数据
	 */
	private void loadSearchData(List<RKCloudChatBaseMessage> msgsList)
	{
		mLoadMsgBar.setVisibility(View.GONE);
		mAllMsgsData.clear();

		if (null == msgsList || msgsList.size() == 0)
		{
			mAdapter.notifyDataSetChanged();
			showUnreadCntTip(0);
			mFirstShowUnreadTip = false;
			return;
		}

		mLoadHistoryDataFinished = false;
		mLoadNewDataFinished = false;

		lastLoadMsgCreaingId = msgsList.get(0).getMsgCreasingId();// 记录已加载消息中自增ID值最小的一个消息ID值
		newLoadMsgCreaingId = msgsList.get(msgsList.size() - 1).getMsgCreasingId();// 记录已加载消息中自增ID值最大的一个消息ID值

		mAllMsgsData.addAll(msgsList);
		mAdapter.notifyDataSetChanged();

		mListView.setSelection(0);

		// 显示未读条数提示
		if (mFirstShowUnreadTip)
		{
			showUnreadCntTip(mChatObj.getUnReadMsgCnt());
			mFirstShowUnreadTip = false;
		}
	}

	/*
	 * 加载新数据
	 */
	private void loadNewData(List<RKCloudChatBaseMessage> msgsList)
	{
		mLoadMsgBar.setVisibility(View.GONE);
		mLoadingNewData = false;
		if (null == msgsList || msgsList.size() == 0)
		{
			mAdapter.notifyDataSetChanged();
			showUnreadCntTip(0);
			mLoadNewDataFinished = true;
			mFirstShowUnreadTip = false;
			return;
		}
		else
		{
			if (msgsList.size() < RKCloudChatConstants.LOAD_MSG_DEFAULT_COUNT)
			{
				mLoadNewDataFinished = true;
			}
		}

		newLoadMsgCreaingId = msgsList.get(msgsList.size() - 1).getMsgCreasingId();// 记录已加载消息中自增ID值最大的一个消息ID值

		mAllMsgsData.addAll(msgsList);
		mAdapter.notifyDataSetChanged();

		// 显示未读条数提示
		if (mFirstShowUnreadTip)
		{
			showUnreadCntTip(mChatObj.getUnReadMsgCnt());
			mFirstShowUnreadTip = false;
		}
	}

	/*
	 * 同步消息
	 */
	private void syncMsgContent(String msgSerialNum)
	{
		RKCloudChatBaseMessage newMsgObj = mMmsManager.queryChatMsg(msgSerialNum);
		if (null == newMsgObj || !mChatId.equalsIgnoreCase(newMsgObj.getChatId()))
		{
			return;
		}

		for (RKCloudChatBaseMessage obj : mAllMsgsData)
		{
			if (obj.getMsgSerialNum().equals(msgSerialNum))
			{
				obj.copyData(newMsgObj);
				mAdapter.notifyDataSetChanged();
				break;
			}
		}
	}

	@Override
	public void processResult(Message msg)
	{
		int what = msg.what;

		if (RKCloudChatUiHandlerMessage.SDCARD_NOT_EXIST == what || RKCloudChatUiHandlerMessage.SDCARD_ERROR == what)
		{
			if (mChatId.equalsIgnoreCase((String) msg.obj))
			{
				closeProgressDialog();
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_sdcard_unvalid));
			}

		}
		else if (RKCloudChatUiHandlerMessage.IMAGE_COMPRESS_FAILED == what)
		{ // 图片压缩失败
			if (mChatId.equalsIgnoreCase((String) msg.obj))
			{
				closeProgressDialog();
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_resize_image_failed));
			}

		}
		else if (RKCloudChatUiHandlerMessage.IMAGE_COMPRESS_SUCCESS == what)
		{ // 图片压缩成功
			String content = (String) msg.obj;
			String[] contents = !TextUtils.isEmpty(content) ? content.split(",") : null;
			if (null != contents && 2 == contents.length && mChatId.equalsIgnoreCase(contents[0]))
			{
				closeProgressDialog();
				ImageMessage body = ImageMessage.buildMsg(mChatId, contents[1]);
				body.setmMsgSummary(getString(R.string.rkcloud_chat_notify_image));
				sendMms(body);
			}

		}
		else if (RKCloudChatUiHandlerMessage.GET_CONTACTSINFO_FINISHED == what)
		{ // 联系人信息获取完成
			mContacts.clear();
			Map<String, RKCloudChatContact> datas = (Map<String, RKCloudChatContact>) msg.obj;
			if (null != datas && datas.size() > 0)
			{
				mContacts.putAll(datas);
			}
			mAdapter.notifyDataSetChanged();

			if (SingleChat.class.equals(mChatClassObj))
			{
				RKCloudChatContact contactObj = mContacts.get(mChatId);
				text_title_content.setText(null != contactObj ? contactObj.getShowName() : mChatId);
			}

		}
		else if (RKCloudChatUiHandlerMessage.MSG_LOAD_DATA_FINISHED == what)
		{ // 数据查询完毕
			loadListData((List<RKCloudChatBaseMessage>) msg.obj, msg.arg1 == 1);

		}
		else if (RKCloudChatUiHandlerMessage.MSG_LOAD_HISTROY_DATA_FINISHED == what)
		{ // 历史数据加载完成
			loadHistoryData((List<RKCloudChatBaseMessage>) msg.obj);

		}
		else if (RKCloudChatUiHandlerMessage.MSG_LOAD_SEARCH_DATA_FINISHED == what)
		{ // 数据查询完毕
			loadSearchData((List<RKCloudChatBaseMessage>) msg.obj);

		}
		else if (RKCloudChatUiHandlerMessage.MSG_LOAD_NEW_DATA_FINISHED == what)
		{ // 新数据查询完毕
			loadNewData((List<RKCloudChatBaseMessage>) msg.obj);

		}
		else if (RKCloudChatUiHandlerMessage.HIDDEN_TIP_UNREADCNT == what)
		{
			mUnreadMsgCntTip.setVisibility(View.GONE);
			unreadLeastMsgId = 0;
			mUnreadCnt = 0;

		}
		else if (RKCloudChatUiHandlerMessage.HIDDEN_TIP_NEWMSG == what)
		{ // 隐藏提示条的显示
			hiddenTipMsg();

		}
		else if (RKCloudChatUiHandlerMessage.HIDDEN_TIP_NEWMSG_TIMER == what)
		{ // 固定时间后隐藏提示条的显示
			if (System.currentTimeMillis() - mShowTipMsgTime >= 30000)
			{
				hiddenTipMsg();
			}

		}
		else if (RKCloudChatUiHandlerMessage.RESPONSE_SEND_MMS == what || RKCloudChatUiHandlerMessage.RESPONSE_RESEND_MMS == what)
		{ // 发送消息或重新发送消息
			String msgSerialNum = (String) msg.obj;
			if (RKCloudChatErrorCode.CHAT_MMS_CANNOT_SEND_OWN == msg.arg1)
			{
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_forbid_sendmms_toself));

			}
			else if (RKCloudChatErrorCode.RK_INVALID_USER == msg.arg1)
			{
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_illegal_receiver));
				finish();

			}
			else if (RKCloudChatErrorCode.CHAT_GROUP_USER_NOT_EXIST == msg.arg1)
			{
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_unuser_in_group));
				finish();

			}
			else if (RKCloudChatErrorCode.CHAT_MMS_SIZE_EXCEED_LIMIT == msg.arg1)
			{
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_mmssize_exceed));

			}
			else if (RKCloudChatErrorCode.CHAT_MMS_DURATION_EXCEED_LIMIT == msg.arg1)
			{
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_mmsduration_exceed));

			}
			else if (RKCloudChatErrorCode.CHAT_MMS_NOTEXIST == msg.arg1)
			{
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_unfound_resource));

			}
			else if (RKCloudChatErrorCode.RK_SDK_UNINIT == msg.arg1)
			{
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_sdk_uninit));

			}
			else if (RKCloudChatErrorCode.RK_NOT_NETWORK == msg.arg1)
			{
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_network_off));

			}
			mAdapter.removeDowningProgress(msgSerialNum);
			syncMsgContent(msgSerialNum);// 同步消息

		}
		else if (RKCloudChatUiHandlerMessage.RESPONSE_REVOKE_MMS == what)
		{// 撤销消息
			closeProgressDialog();
			String msgSerialNum = (String) msg.obj;
			if (RKCloudChatErrorCode.CHAT_MMS_NOTEXIST == msg.arg1)
			{
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_unfound_resource));

			}
			else if (RKCloudChatErrorCode.RK_SDK_UNINIT == msg.arg1)
			{
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_sdk_uninit));

			}
			else if (RKCloudChatErrorCode.RK_NOT_NETWORK == msg.arg1)
			{
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_network_off));

			}
			mAdapter.removeDowningProgress(msgSerialNum);
			syncMsgContent(msgSerialNum);// 同步消息

		}
		else if (RKCloudChatUiHandlerMessage.CALLBACK_RECEIVED_MOREMMS == what)
		{ // 批量处理完MMS消息
			List<RKCloudChatBaseMessage> datas = (List<RKCloudChatBaseMessage>) msg.obj;
			if (null != datas && datas.size() > 0)
			{
				if (datas.size() >= TIP_UNREADCNT_LEAST_COUNT)
				{
					// 重新查询
					mLoadHistoryDataFinished = false;
					lastLoadMsgCreaingId = 0;
					startQuery(QUERY_TYPE_LOAD_DATA);
					showUnreadCntTip(datas.size());

				}
				else
				{
					// 是否定位到底端
					boolean jumpBottom = false;
					if (mAllMsgsData.size() - 1 == mListView.getLastVisiblePosition())
					{
						jumpBottom = true;
					}

					mAllMsgsData.addAll(datas);// 追加数据
					mAdapter.notifyDataSetChanged();
					if (jumpBottom)
					{
						jumpListBottom();
					}
					else
					{
						// 显示最后一条消息内容
						RKCloudChatBaseMessage newMsgObj = (RKCloudChatBaseMessage) datas.get(datas.size() - 1);
						showTipMsg(newMsgObj);
					}
				}
			}

		}
		else if (RKCloudChatUiHandlerMessage.CALLBACK_RECEIVED_MMS == what)
		{ // 接收一条消息
			RKCloudChatBaseMessage msgObj = (RKCloudChatBaseMessage) msg.obj;
			if (null != msgObj && mChatId.equalsIgnoreCase(msgObj.getChatId()))
			{
				// 列表中有消息时，需要先判断消息显示的位置，如果显示的是最后一条消息，则在收到新消息时自动滚动到最底端
				if (mAllMsgsData.size() - 1 == mListView.getLastVisiblePosition())
				{
					addMsg(msgObj, true);
				}
				else
				{
					addMsg(msgObj, false);
					showTipMsg(msgObj);
				}
			}

		}
		else if (RKCloudChatUiHandlerMessage.ADD_MSG_TO_LOCALDB == what)
		{ // 向DB表中插入一条消息
			RKCloudChatBaseMessage msgObj = (RKCloudChatBaseMessage) msg.obj;
			if (null != msgObj && mChatId.equalsIgnoreCase(msgObj.getChatId()))
			{
				addMsg(msgObj, true);
			}

		}
		else if (RKCloudChatUiHandlerMessage.MSG_STATUS_HAS_CHANGED == what || RKCloudChatUiHandlerMessage.RESPONSE_THUMBIMAGE_DOWNED == what)
		{ // 消息内容或状态有变化或缩略图下载完成
			syncMsgContent((String) msg.obj);

		}
		else if (RKCloudChatUiHandlerMessage.RESPONSE_UPDATE_DOWNING_PROGRESS == what)
		{ // 更新附件的下载进度
			String msgSerialNum = (String) msg.obj;
			if (!TextUtils.isEmpty(msgSerialNum))
			{
				mAdapter.updateDowningProgress(msgSerialNum, msg.arg1);
			}

		}
		else if (RKCloudChatUiHandlerMessage.RESPONSE_MEDIAFILE_DOWNED == what)
		{ // 附件下载完成
			// 如果音频消息下载完成后则自动进行播放处理
			String msgSerialNum = (String) msg.obj;
			if (!TextUtils.isEmpty(msgSerialNum))
			{
				// 先同步信息
				syncMsgContent(msgSerialNum);
				if (0 == msg.arg1 && msgSerialNum.equals(lastAudioSerialNum))
				{
					// 如果有正在播放的音频文件则先停止
					mAudioHelper.stopMsgOfAudio();
					// 开始播放下载后的音频文件
					for (RKCloudChatBaseMessage obj : mAllMsgsData)
					{
						if (obj.getMsgSerialNum().equals(lastAudioSerialNum))
						{
							AudioMessage msgObj = (AudioMessage) obj;
							mAudioHelper.playMsgOfAudio(msgObj.getMsgSerialNum(), msgObj.getFilePath());
							break;
						}
					}
				}
				mAdapter.removeDowningProgress(msgSerialNum);
			}

		}
		else if (RKCloudChatUiHandlerMessage.CALLBACK_KICKOUT == what || RKCloudChatUiHandlerMessage.CALLBACK_GROUP_DISSOLVED == what)
		{ // 被踢出会话或群解散
			if (mChatId.equalsIgnoreCase((String) msg.obj))
			{
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_kickoff_by_owner));
				finish();
			}
		}
		else if (RKCloudChatUiHandlerMessage.DELETE_SINGLE_CHAT == msg.what)
		{ // 会话被删除
			if (mChatId.equalsIgnoreCase((String) msg.obj))
			{
				finish();
			}

		}
		else if (RKCloudChatUiHandlerMessage.CALLBACK_GROUP_INFO_CHANGED == what)
		{
			if (mChatId.equalsIgnoreCase((String) msg.obj))
			{
				showConvInfo();
			}
		}
		else if (RKCloudChatUiHandlerMessage.CALLBACK_ALL_GROUP_INFO_COMPLETE == msg.what)
		{
			showConvInfo();
		}
		else if (RKCloudChatUiHandlerMessage.CONTACTSINFO_CHANGED == msg.what)
		{ // 联系人信息有变化
			List<String> accounts = (List<String>) msg.obj;
			if (null != accounts && accounts.size() > 0)
			{
				for (String account : accounts)
				{
					if (mContacts.containsKey(account))
					{
						mContacts.put(account, mContactManager.getContactInfo(account));
					}
				}
				mAdapter.notifyDataSetChanged();
			}

		}
		else if (RKCloudChatUiHandlerMessage.CONTACT_HEADERIMAGE_CHANGED == msg.what)
		{ // 联系人头像有变化
			String account = (String) msg.obj;
			if (mContacts.containsKey(account))
			{
				mContacts.put(account, mContactManager.getContactInfo(account));
			}
			mAdapter.notifyDataSetChanged();
		}
	}
}
