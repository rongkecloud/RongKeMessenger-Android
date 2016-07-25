package com.rongkecloud.chat.demo.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Handler;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.*;
import com.rongke.cloud.av.demo.RKCloudAVDemoManager;
import com.rongke.cloud.meeting.demo.RKCloudMeetingDemoManager;
import com.rongkecloud.chat.*;
import com.rongkecloud.chat.RKCloudChatBaseMessage.MSG_DIRECTION;
import com.rongkecloud.chat.RKCloudChatBaseMessage.MSG_STATUS;
import com.rongkecloud.chat.demo.RKCloudChatConstants;
import com.rongkecloud.chat.demo.RKCloudChatContactManager;
import com.rongkecloud.chat.demo.RKCloudChatMmsManager;
import com.rongkecloud.chat.demo.RKCloudChatUiHandlerMessage;
import com.rongkecloud.chat.demo.entity.RKCloudChatContact;
import com.rongkecloud.chat.demo.tools.RKCloudChatPlayAudioMsgTools;
import com.rongkecloud.chat.demo.tools.RKCloudChatTools;
import com.rongkecloud.chat.demo.ui.RKCloudChatMsgActivity;
import com.rongkecloud.chat.demo.ui.RKCloudChatViewImagesActivity;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageAsyncLoader;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest.IMAGE_REQUEST_TYPE;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageResult;
import com.rongkecloud.chat.demo.ui.widget.RKCloudChatLongClickLinkMovementMethod;
import com.rongkecloud.chat.demo.ui.widget.RKCloudChatNoLineClickSpan;
import com.rongkecloud.multiVoice.RKCloudMeetingInvitedInfoBean;
import com.rongkecloud.sdkbase.RKCloud;
import com.rongkecloud.test.R;
import com.rongkecloud.test.ui.widget.RoundedImageView;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RKCloudChatMsgAdapter extends BaseAdapter
{
	// 处理长时间下载中和发送中的消息的间隔时间，默认是2分钟，单位：毫秒
	private static final int PROCESS_SENDING_OR_DOWNING_MAXTIME = 2 * 60 * 1000;
	// 定义不同样式对应的类型
	private static final int TOTAL_ITEM_STYLE = 15;// 总条目数

	private static final int ITEM_STYLE_TIP = 0;// 时间提示条
	private static final int ITEM_STYLE_SEND_TEXT = 1;// 文本消息
	private static final int ITEM_STYLE_RECEIVED_TEXT = 2;// 文本消息
	private static final int ITEM_STYLE_SEND_IMAGE = 3;// 图片消息
	private static final int ITEM_STYLE_RECEIVED_IMAGE = 4;// 图片消息
	private static final int ITEM_STYLE_SEND_AUDIO = 5;// 语音消息
	private static final int ITEM_STYLE_RECEIVED_AUDIO = 6;// 语音消息
	private static final int ITEM_STYLE_SEND_VIDEO = 7;// 视频消息
	private static final int ITEM_STYLE_RECEIVED_VIDEO = 8;// 视频消息
	private static final int ITEM_STYLE_SEND_ATTACH = 9;// 附件消息
	private static final int ITEM_STYLE_RECEIVED_ATTACH = 10;// 附件消息
	private static final int ITEM_STYLE_SEND_AVCALL = 11;// 音视频通话消息
	private static final int ITEM_STYLE_RECEIVED_AVCALL = 12;// 音视频通话消息
	private static final int ITEM_STYLE_SEND_MUTLIMEETING = 13;// 多人语音消息
	private static final int ITEM_STYLE_RECEIVED_MUTLIMEETING = 14;// 多人语音消息

	// 上传或下载文件时显示进度值使用
	private Map<TextView, String> mRecordDowningTV = new HashMap<TextView, String>();

	// 私有属性的定义
	private Context mContext;// 当前适配器所在的Context
	private Class<? extends RKCloudChatBaseChat> mChatClassObj;

	private List<RKCloudChatBaseMessage> mAllData; // 消息列表数据
	private Map<String, RKCloudChatContact> mContacts;// 联系人数据

	private RKCloudChatMmsManager mMmsManager; // 消息管理器对象
	private RKCloudChatContactManager mContactManager;
	private Handler mUiHandler; // 绑定的UI Handler

	// 播放语音消息使用的相关变量
	private RKCloudChatPlayAudioMsgTools mAudioHelper;
	private AudioMessage mCurrPlayAudioMsg;
	private TextView mCurrAudioDurationTV;
	private AnimationDrawable mCurrAnimationDrawable;
	private Handler mUpdatePlayTime = new Handler();
	private Runnable mPlayProgressRunner = new Runnable()
	{
		public void run()
		{
			String playingSerialNum = mAudioHelper.getPlayingMsgSerialNum();
			if (!TextUtils.isEmpty(playingSerialNum))
			{

				int duraction = mAudioHelper.getPlayingAudioDuration();
				int hasPlayingTime = mAudioHelper.getPlayingAudioPosition();
				if (hasPlayingTime < duraction)
				{
					if (null != mCurrAudioDurationTV)
					{
						int surplus = (int) Math.floor((duraction - hasPlayingTime) / 1000.0);
						surplus = surplus < 0 ? 0 : surplus;
						surplus = surplus < mCurrPlayAudioMsg.getDuration() ? surplus : mCurrPlayAudioMsg.getDuration();
						mCurrAudioDurationTV.setText(String.format("%d\"", surplus)); // 显示剩余的播放时间

					}
					mUpdatePlayTime.postDelayed(mPlayProgressRunner, 500);// 每次延迟500毫秒再启动线程
				}
				else
				{
					// 时间到达后停止播放
					mAudioHelper.stopMsgOfAudio();
					initPlayingAudioMsgParams();
				}

			}
			else
			{
				initPlayingAudioMsgParams();
			}
		}
	};

	/**
	 * 构造函数
	 * 
	 * @param context
	 * @param chatClassObj
	 * @param msgs
	 * @param contacts
	 * @param uiHandler
	 */
	public RKCloudChatMsgAdapter(Context context, Class<? extends RKCloudChatBaseChat> chatClassObj, List<RKCloudChatBaseMessage> msgs, Map<String, RKCloudChatContact> contacts, Handler uiHandler)
	{
		mContext = context;
		mChatClassObj = chatClassObj;
		mAllData = msgs;
		mContacts = contacts;
		mUiHandler = uiHandler;

		mMmsManager = RKCloudChatMmsManager.getInstance(mContext);
		mContactManager = RKCloudChatContactManager.getInstance(mContext);
		mAudioHelper = RKCloudChatPlayAudioMsgTools.getInstance(mContext);
	}

	/*
	 * 初始化播放语音使用的参数
	 */
	private void initPlayingAudioMsgParams()
	{
		if (null != mCurrPlayAudioMsg)
		{
			mCurrPlayAudioMsg = null;
			mCurrAudioDurationTV = null;
			if (null != mCurrAnimationDrawable)
			{
				mCurrAnimationDrawable.stop();
				mCurrAnimationDrawable.selectDrawable(0);
				mCurrAnimationDrawable = null;
			}
			mUpdatePlayTime.removeCallbacks(mPlayProgressRunner);
			notifyDataSetChanged();
		}
	}

	@Override
	public int getCount()
	{
		return mAllData.size();
	}

	@Override
	public Object getItem(int position)
	{
		return mAllData.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public int getViewTypeCount()
	{
		return TOTAL_ITEM_STYLE;
	}

	@Override
	public int getItemViewType(int position)
	{
		int type = -1;
		RKCloudChatBaseMessage msgObj = mAllData.get(position);
		if (RKCloudChatConstants.FLAG_AVCALL_IS_AUDIO.equals(msgObj.getExtension()) || RKCloudChatConstants.FLAG_AVCALL_IS_VIDEO.equals(msgObj.getExtension()))
		{
			type = MSG_DIRECTION.SEND == msgObj.getDirection() ? ITEM_STYLE_SEND_AVCALL : ITEM_STYLE_RECEIVED_AVCALL;
		}
		else if (!TextUtils.isEmpty(msgObj.getExtension()) && msgObj.getExtension().startsWith(RKCloudChatConstants.FLAG_MEETING_MUTLIMEETING, 0))
		{
			type = MSG_DIRECTION.SEND == msgObj.getDirection() ? ITEM_STYLE_SEND_MUTLIMEETING : ITEM_STYLE_RECEIVED_MUTLIMEETING;
		}
		else if (RKCloudChatConstants.FLAG_LOCAL_TIPMESSAGE.equals(msgObj.getExtension()))
		{
			type = ITEM_STYLE_TIP;
		}
		else
		{
			if (msgObj instanceof TipMessage|| MSG_STATUS.MESSAGE_REVOKE == msgObj.getStatus())
			{
				type = ITEM_STYLE_TIP;
			}
			else if (msgObj instanceof TextMessage)
			{
				type = MSG_DIRECTION.SEND == msgObj.getDirection() ? ITEM_STYLE_SEND_TEXT : ITEM_STYLE_RECEIVED_TEXT;
			}
			else if (msgObj instanceof ImageMessage)
			{
				type = MSG_DIRECTION.SEND == msgObj.getDirection() ? ITEM_STYLE_SEND_IMAGE : ITEM_STYLE_RECEIVED_IMAGE;
			}
			else if (msgObj instanceof AudioMessage)
			{
				type = MSG_DIRECTION.SEND == msgObj.getDirection() ? ITEM_STYLE_SEND_AUDIO : ITEM_STYLE_RECEIVED_AUDIO;
			}
			else if (msgObj instanceof VideoMessage)
			{
				type = MSG_DIRECTION.SEND == msgObj.getDirection() ? ITEM_STYLE_SEND_VIDEO : ITEM_STYLE_RECEIVED_VIDEO;
			}
			else if (msgObj instanceof FileMessage)
			{
				type = MSG_DIRECTION.SEND == msgObj.getDirection() ? ITEM_STYLE_SEND_ATTACH : ITEM_STYLE_RECEIVED_ATTACH;
			}
			else if (msgObj instanceof LocalMessage)
			{
				// TODO 可以在此扩展本地消息使用的样式
				type = MSG_DIRECTION.SEND == msgObj.getDirection() ? ITEM_STYLE_SEND_TEXT : ITEM_STYLE_RECEIVED_TEXT;
			}
			else if (msgObj instanceof CustomMessage)
			{
				// TODO 可以在此扩展自定义消息使用的样式
				type = MSG_DIRECTION.SEND == msgObj.getDirection() ? ITEM_STYLE_SEND_TEXT : ITEM_STYLE_RECEIVED_TEXT;
			}
		}

		return type;
	}

	/*
	 * @function buildItemView 根据消息类型获取对应的item view
	 * @param type int 消息类型
	 * @return View对象
	 */
	private View buildItemView(int type)
	{
		View msgView = null;
		if (ITEM_STYLE_TIP == type)
		{ // 时间提示条
			msgView = LayoutInflater.from(mContext).inflate(R.layout.rkcloud_chat_msg_item_tip, null);

		}
		else
		{
			int bodyLayout = 0;
			switch (type)
			{
				case ITEM_STYLE_SEND_TEXT:
				case ITEM_STYLE_RECEIVED_TEXT:
					bodyLayout = R.layout.rkcloud_chat_msg_item_text_body;
					break;

				case ITEM_STYLE_SEND_IMAGE:
				case ITEM_STYLE_RECEIVED_IMAGE:
					bodyLayout = R.layout.rkcloud_chat_msg_item_image_body;
					break;

				case ITEM_STYLE_SEND_AUDIO:
				case ITEM_STYLE_RECEIVED_AUDIO:
					bodyLayout = R.layout.rkcloud_chat_msg_item_audio_body;
					break;

				case ITEM_STYLE_SEND_VIDEO:
				case ITEM_STYLE_RECEIVED_VIDEO:
					bodyLayout = R.layout.rkcloud_chat_msg_item_video_body;
					break;

				case ITEM_STYLE_SEND_ATTACH:
				case ITEM_STYLE_RECEIVED_ATTACH:
					bodyLayout = R.layout.rkcloud_chat_msg_item_file_body;
					break;

				case ITEM_STYLE_SEND_AVCALL:
				case ITEM_STYLE_RECEIVED_AVCALL:
					bodyLayout = R.layout.rkcloud_chat_msg_item_avcall_body;
					break;

				case ITEM_STYLE_SEND_MUTLIMEETING:
				case ITEM_STYLE_RECEIVED_MUTLIMEETING:
					bodyLayout = R.layout.rkcloud_chat_msg_item_mutlimeeting_body;
					break;
			}

			int rootLayout = 0;
			switch (type)
			{
				case ITEM_STYLE_SEND_TEXT:
				case ITEM_STYLE_SEND_IMAGE:
				case ITEM_STYLE_SEND_AUDIO:
				case ITEM_STYLE_SEND_VIDEO:
				case ITEM_STYLE_SEND_ATTACH:
				case ITEM_STYLE_SEND_AVCALL:
				case ITEM_STYLE_SEND_MUTLIMEETING:
					rootLayout = R.layout.rkcloud_chat_msg_item_send;
					break;

				case ITEM_STYLE_RECEIVED_TEXT:
				case ITEM_STYLE_RECEIVED_IMAGE:
				case ITEM_STYLE_RECEIVED_AUDIO:
				case ITEM_STYLE_RECEIVED_VIDEO:
				case ITEM_STYLE_RECEIVED_ATTACH:
				case ITEM_STYLE_RECEIVED_AVCALL:
				case ITEM_STYLE_RECEIVED_MUTLIMEETING:
					rootLayout = R.layout.rkcloud_chat_msg_item_received;
					break;
			}
			msgView = LayoutInflater.from(mContext).inflate(rootLayout, null);
			RelativeLayout bodyView = (RelativeLayout) msgView.findViewById(R.id.layout_body);
			bodyView.addView(LayoutInflater.from(mContext).inflate(bodyLayout, null));
		}

		return msgView;
	}

	private class ItemViewBuffer
	{
		View rootView;
		LinearLayout timeLayout;
		TextView timeTV;
		TextView nameTV;// 设置消息发送者的名称
		RoundedImageView senderPhoto;// 设置消息发送者的头像
		View contentBackground;// 设置消息背景色
		TextView sendStatus;// 发送状态
		ImageView sendBtn;// 重发图标
		ImageView newMsgSign;// 新消息标志
		RelativeLayout loadLayout;// 下载布局
		TextView loadProgress;// 下载进度值
		ImageView playImg;// 视频播放图标
		TextView msgStatus;//消息状态的回执显示

		ItemViewBuffer(View view)
		{
			rootView = view;
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		final RKCloudChatBaseMessage msgObj = (RKCloudChatBaseMessage) mAllData.get(position);
		// 获取子条目使用的布局类型
		int type;
		if (MSG_STATUS.MESSAGE_REVOKE == msgObj.getStatus())
		{
			type = ITEM_STYLE_TIP;
		}
		else
		{
			type = getItemViewType(position);
		}

		final ItemViewBuffer itemBuffer;

		if (null == convertView)
		{
			convertView = buildItemView(type);
			itemBuffer = new ItemViewBuffer(convertView);
			convertView.setTag(itemBuffer);
		}
		else
		{
			itemBuffer = (ItemViewBuffer) convertView.getTag();
		}

		// 时间提示条的显示设置
		itemBuffer.timeLayout = (LinearLayout) convertView.findViewById(R.id.timelayout);
		itemBuffer.timeTV = (TextView) convertView.findViewById(R.id.msg_time);
		if (0 == position)
		{
			itemBuffer.timeLayout.setVisibility(View.VISIBLE);
			itemBuffer.timeTV.setText(RKCloudChatTools.getMsgShowTime(msgObj.getMsgTime() * 1000));
		}
		else
		{
			RKCloudChatBaseMessage beforeMsgObj = mAllData.get(position - 1);
			if (RKCloudChatTools.isAddTimeTip(beforeMsgObj.getMsgTime() * 1000, msgObj.getMsgTime() * 1000))
			{
				itemBuffer.timeLayout.setVisibility(View.VISIBLE);
				itemBuffer.timeTV.setText(RKCloudChatTools.getMsgShowTime(msgObj.getMsgTime() * 1000));
			}
			else
			{
				itemBuffer.timeLayout.setVisibility(View.GONE);
			}
		}

		if (ITEM_STYLE_TIP == type)
		{
			// 显示提示类型的消息内容
			TextView contentTV = (TextView) convertView.findViewById(R.id.msg_tip);
			if (MSG_STATUS.MESSAGE_REVOKE == msgObj.getStatus())
			{
				if (msgObj.getSender().equals(RKCloud.getUserName()))
				{
					contentTV.setText(mContext.getString(R.string.rkcloud_chat_revoke_message_self));
				}
				else
				{
					if (!mContacts.containsKey(msgObj.getSender()))
					{
						mContacts.put(msgObj.getSender(), mContactManager.getContactInfo(msgObj.getSender()));
					}
					RKCloudChatContact contactObj = mContacts.get(msgObj.getSender());
					contentTV.setText(mContext.getString(R.string.rkcloud_chat_revoke_message_other, null != contactObj ? contactObj.getShowName() : msgObj.getSender()));
				}
			}
			else
			{
				contentTV.setText(msgObj.getContent());
			}
		}
		else
		{
			if (MSG_DIRECTION.SEND == msgObj.getDirection())
			{
				// 发送中的消息超过一定时间则认为发送失败
				if (MSG_STATUS.SEND_SENDING == msgObj.getStatus() && (System.currentTimeMillis() - msgObj.getMsgTime() * 1000 >= PROCESS_SENDING_OR_DOWNING_MAXTIME))
				{
					msgObj.setStatus(MSG_STATUS.SEND_FAILED);
				}
			}
			else if (MSG_DIRECTION.RECEIVE == msgObj.getDirection())
			{
				// 如果是接收的消息并且与消息列表中底部弹出提示条对应的消息编号相同，则控制提示条隐藏不显示
				if (msgObj.getMsgSerialNum().equals(RKCloudChatMsgActivity.tipMsgSerialNum))
				{
					mUiHandler.sendEmptyMessage(RKCloudChatUiHandlerMessage.HIDDEN_TIP_NEWMSG);
				}
				// 所有未读消息完全展现时隐藏右侧上方的未读条数提示
				if (msgObj.getMsgCreasingId() == RKCloudChatMsgActivity.unreadLeastMsgId)
				{
					mUiHandler.sendEmptyMessage(RKCloudChatUiHandlerMessage.HIDDEN_TIP_UNREADCNT);
				}
				// 长时间处于下载中的消息时超出限制则认为下载失败
				if (MSG_STATUS.RECEIVE_DOWNING == msgObj.getStatus() && (System.currentTimeMillis() - mMmsManager.getMmsDownTime(msgObj.getMsgSerialNum()) >= PROCESS_SENDING_OR_DOWNING_MAXTIME))
				{
					msgObj.setStatus(MSG_STATUS.RECEIVE_DOWNFAILED);
				}

				// 单聊会话中向对端发送消息已读通知的处理
				if (SingleChat.class.equals(mChatClassObj))
				{
					if (ITEM_STYLE_RECEIVED_AVCALL == type && ITEM_STYLE_RECEIVED_MUTLIMEETING == type)
					{
						// 音视频通话和多人语音相关的内容暂不做处理
					}
					else
					{
						if (msgObj instanceof TextMessage)
						{
							if (MSG_STATUS.RECEIVE_RECEIVED == msgObj.getStatus())
							{
								if(mChatClassObj == SingleChat.class && !msgObj.isHistory())
								{
									mMmsManager.notifyOtherMsgHasReaded(msgObj);
								}
							}
						}
						if (msgObj instanceof CustomMessage)
						{
							// TODO 自定义消息是否需要已读处理，默认是需要的
							if (MSG_STATUS.RECEIVE_RECEIVED == msgObj.getStatus() && !msgObj.isHistory())
							{
								if(mChatClassObj == SingleChat.class)
								{
									mMmsManager.notifyOtherMsgHasReaded(msgObj);
								}
							}

						}
						else if (msgObj instanceof LocalMessage)
						{
							// TODO 本地消息暂不做消息已读的处理
						}
					}
				}
			}

			// 解析组件
			itemBuffer.nameTV = (TextView) convertView.findViewById(R.id.sendername);
			itemBuffer.senderPhoto = (RoundedImageView) convertView.findViewById(R.id.senderphoto);
			itemBuffer.contentBackground = convertView.findViewById(R.id.layout_body);
			itemBuffer.loadLayout = (RelativeLayout) convertView.findViewById(R.id.layout_download);
			itemBuffer.loadProgress = (TextView) convertView.findViewById(R.id.download_percent);
			if (MSG_DIRECTION.SEND == msgObj.getDirection())
			{
				itemBuffer.sendStatus = (TextView) convertView.findViewById(R.id.msgstatus);
				itemBuffer.sendBtn = (ImageView) convertView.findViewById(R.id.btn_resend);
				itemBuffer.msgStatus = (TextView) convertView.findViewById(R.id.tv_status);
			}
			else
			{
				itemBuffer.newMsgSign = (ImageView) convertView.findViewById(R.id.newmsgtip);
			}

			if (msgObj instanceof VideoMessage)
			{
				itemBuffer.playImg = (ImageView) convertView.findViewById(R.id.video_icon);
			}
			// 获取联系人对象，如果不存在则查询并存储
			if (!mContacts.containsKey(msgObj.getSender()))
			{
				mContacts.put(msgObj.getSender(), mContactManager.getContactInfo(msgObj.getSender()));
			}
			final RKCloudChatContact contactObj = mContacts.get(msgObj.getSender());
			// 设置名称
			itemBuffer.nameTV.setText(null != contactObj ? contactObj.getShowName() : msgObj.getSender());
			// 设置头像
			itemBuffer.senderPhoto.setImageResource(R.drawable.rkcloud_chat_img_header_default);
			if (null != contactObj && !TextUtils.isEmpty(contactObj.getHeaderThumbImagePath()) && new File(contactObj.getHeaderThumbImagePath()).exists())
			{
				RKCloudChatImageRequest imageReq = new RKCloudChatImageRequest(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG, contactObj.getHeaderThumbImagePath(), msgObj.getSender());
				RKCloudChatImageResult imgResult = RKCloudChatImageAsyncLoader.getInstance(mContext).sendPendingRequestQuryCache(imageReq);
				if (null != imgResult && null != imgResult.resource)
				{
					itemBuffer.senderPhoto.setImageDrawable(imgResult.resource);
				}
			}
			// 设置头像的点击事件
			itemBuffer.senderPhoto.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					RKCloudChatContactManager.getInstance(mContext).jumpContactDetailInfoUI(mContext, msgObj.getSender());
				}
			});

			// 设置消息背景色及相关的内容
			itemBuffer.loadLayout.setVisibility(View.GONE);
			if (MSG_DIRECTION.SEND == msgObj.getDirection())
			{
				itemBuffer.contentBackground.setBackgroundResource(R.drawable.rkcloud_chat_msgsend_bg);
				itemBuffer.sendStatus.setVisibility(View.GONE);
				itemBuffer.sendBtn.setVisibility(View.GONE);
				itemBuffer.msgStatus.setVisibility(View.GONE);
				// 根据发送状态显示对应的内容
				switch (msgObj.getStatus())
				{
					case SEND_SENDING:
						// 发送中的消息时显示正在发送的动画，并且禁止点击操作
						itemBuffer.sendBtn.setVisibility(View.VISIBLE);
						itemBuffer.sendBtn.setImageResource(R.drawable.rkcloud_chat_msg_sending);
						itemBuffer.sendBtn.setOnClickListener(null);
						AnimationDrawable animationDrawable = (AnimationDrawable) itemBuffer.sendBtn.getDrawable();
						if (!animationDrawable.isRunning())
						{
							animationDrawable.start();
						}
						// 只有媒体消息显示上传进度值
						if (msgObj instanceof ImageMessage || msgObj instanceof AudioMessage || msgObj instanceof VideoMessage || msgObj instanceof FileMessage)
						{
							itemBuffer.loadLayout.setVisibility(View.VISIBLE);
							mRecordDowningTV.put(itemBuffer.loadProgress, msgObj.getMsgSerialNum());
						}
						break;

					case SEND_FAILED:
						// 发送失败时显示重发图标，点击之后执行重发操作
						itemBuffer.sendBtn.setVisibility(View.VISIBLE);
						itemBuffer.sendBtn.setImageResource(R.drawable.rkcloud_chat_msg_img_resend);
						itemBuffer.sendBtn.setOnClickListener(new OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								// 媒体类型的消息需要确保文件存在才可执行重发操作
								if (msgObj instanceof ImageMessage || msgObj instanceof AudioMessage || msgObj instanceof VideoMessage || msgObj instanceof FileMessage)
								{
									String mediaFilePath = null;
									if (msgObj instanceof ImageMessage)
									{
										mediaFilePath = ((ImageMessage) msgObj).getFilePath();
									}
									else if (msgObj instanceof AudioMessage)
									{
										mediaFilePath = ((AudioMessage) msgObj).getFilePath();
									}
									else if (msgObj instanceof VideoMessage)
									{
										mediaFilePath = ((VideoMessage) msgObj).getFilePath();
									}
									else if (msgObj instanceof FileMessage)
									{
										mediaFilePath = ((FileMessage) msgObj).getFilePath();
									}
									// 文件不存在时禁止重发操作
									if (null == mediaFilePath || !new File(mediaFilePath).exists())
									{
										if (!new File(mediaFilePath).exists())
										{
											RKCloudChatTools.showToastText(mContext, mContext.getString(R.string.rkcloud_chat_unfound_resource));
											return;
										}
									}
									// 修改为发送中的状态并显示上传进度值
									itemBuffer.loadLayout.setVisibility(View.VISIBLE);
									itemBuffer.loadProgress.setText("0%");
									msgObj.setStatus(MSG_STATUS.SEND_SENDING);
									mRecordDowningTV.put(itemBuffer.loadProgress, msgObj.getMsgSerialNum());
								}

								mMmsManager.reSendMms(msgObj.getMsgSerialNum());
							}
						});
						break;
					case RECEIVE_RECEIVED:
						itemBuffer.msgStatus.setVisibility(View.VISIBLE);
						itemBuffer.msgStatus.setText(mContext.getString(R.string.rkcloud_chat_msgstatus_send_arrived));
						break;
					case READED:
						itemBuffer.msgStatus.setVisibility(View.VISIBLE);
						itemBuffer.msgStatus.setText(mContext.getString(R.string.rkcloud_chat_msgstatus_send_readed));
						break;

					default:
						if (SingleChat.class.equals(mChatClassObj))
						{
							// 只有单聊会话并且非多人语音和音视频消息时显示发送状态
							if (!RKCloudChatConstants.FLAG_ADD_FRIEND_SUCCESS.equals(msgObj.getExtension()) && type != ITEM_STYLE_SEND_AVCALL)
							{
								// itemBuffer.sendStatus.setVisibility(View.VISIBLE);
								itemBuffer.sendStatus.setText(RKCloudChatTools.getSendMsgStatus(mContext, msgObj.getStatus()));
							}
						}
						break;
				}

			}
			else if (MSG_DIRECTION.RECEIVE == msgObj.getDirection())
			{
				itemBuffer.contentBackground.setBackgroundResource(R.drawable.rkcloud_chat_msgreceive_bg);
				itemBuffer.newMsgSign.setVisibility(View.GONE);
				if ((ITEM_STYLE_RECEIVED_AVCALL == type || ITEM_STYLE_RECEIVED_MUTLIMEETING == type) && MSG_STATUS.RECEIVE_RECEIVED == msgObj.getStatus())
				{
					itemBuffer.newMsgSign.setVisibility(View.VISIBLE);
				}
				else
				{
					if (msgObj instanceof ImageMessage || msgObj instanceof AudioMessage || msgObj instanceof VideoMessage || msgObj instanceof FileMessage)
					{
						switch (msgObj.getStatus())
						{
							case RECEIVE_RECEIVED: // 已收到
							case RECEIVE_DOWNFAILED: // 图片下载失败
								itemBuffer.newMsgSign.setVisibility(View.VISIBLE);
								break;
							case RECEIVE_DOWNING:// 下载中
								itemBuffer.loadLayout.setVisibility(View.VISIBLE);
								break;

							case RECEIVE_DOWNED: // 下载完成
							case READED: // 已读
								break;
							default:
								break;
						}
					}
				}
			}

			// 长点击事件
			itemBuffer.contentBackground.setOnLongClickListener(new OnLongClickListener()
			{
				@Override
				public boolean onLongClick(View v)
				{
					RKCloudChatMsgActivity activity = (RKCloudChatMsgActivity) mContext;
					activity.showContextMenu(msgObj);
					return false;
				}
			});

			// 消息的点击事件
			itemBuffer.contentBackground.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					showClickEvent(msgObj, itemBuffer);
				}
			});

			// 不同类型的消息的内容显示
			if (ITEM_STYLE_SEND_AVCALL == type || ITEM_STYLE_RECEIVED_AVCALL == type)
			{
				// 音视频消息的显示
				ImageView avIcon = (ImageView) convertView.findViewById(R.id.avcall_icon);
				TextView content = (TextView) convertView.findViewById(R.id.call_content);
				changeTextColor(msgObj, content);
				if (RKCloudChatConstants.FLAG_AVCALL_IS_AUDIO.equals(msgObj.getExtension()))
				{
					if (MSG_DIRECTION.RECEIVE == msgObj.getDirection())
					{
						avIcon.setImageResource(R.drawable.rkcloud_chat_img_msg_av_audiocall);
					}
					else
					{
						avIcon.setImageResource(R.drawable.rkcloud_chat_img_msg_av_audiocall_bg_blue);
					}
				}
				else
				{
					if (MSG_DIRECTION.RECEIVE == msgObj.getDirection())
					{
						avIcon.setImageResource(R.drawable.rkcloud_chat_img_msg_av_videocall);
					}
					else
					{
						avIcon.setImageResource(R.drawable.rkcloud_chat_img_msg_av_videocall_bg_blue);
					}
				}
				if (msgObj.getContent().contentEquals(mContext.getResources().getString(R.string.rkcloud_av_msg_callmissed))
						|| msgObj.getContent().contentEquals(mContext.getResources().getString(R.string.rkcloud_av_msg_callee_calleereject)))
				{
					if (RKCloudChatConstants.FLAG_AVCALL_IS_AUDIO.equals(msgObj.getExtension()))
					{
						avIcon.setImageResource(R.drawable.rkcloud_chat_img_msg_av_audiocall_bg_red);
					}
					else
					{
						avIcon.setImageResource(R.drawable.rkcloud_chat_img_msg_av_videocall_bg_red);
					}
					content.setTextColor(mContext.getResources().getColor(R.color.prompt_content_color_red));
				}
				content.setText(msgObj.getContent());

			}
			else if (ITEM_STYLE_SEND_MUTLIMEETING == type || ITEM_STYLE_RECEIVED_MUTLIMEETING == type)
			{
				// 多人语音消息的显示
				TextView content = (TextView) convertView.findViewById(R.id.meeting_content);
				content.setText(msgObj.getContent());
				changeTextColor(msgObj, content);
			}
			else
			{
				if (msgObj instanceof TextMessage)
				{
					showTextMsg(convertView, (TextMessage) msgObj);

				}
				else if (msgObj instanceof ImageMessage)
				{
					showImageMsg(convertView, (ImageMessage) msgObj);

				}
				else if (msgObj instanceof AudioMessage)
				{
					showAudioMsg(convertView, (AudioMessage) msgObj);

				}
				else if (msgObj instanceof VideoMessage)
				{
					showVideoMsg(convertView, itemBuffer.playImg, (VideoMessage) msgObj);

				}
				else if (msgObj instanceof FileMessage)
				{
					showFileMsg(convertView, (FileMessage) msgObj);

				}
				else if (msgObj instanceof CustomMessage)
				{
					showCustomMsg(convertView, (CustomMessage) msgObj);

				}
				else if (msgObj instanceof LocalMessage)
				{
					showLocalMsg(convertView, (LocalMessage) msgObj);
				}
			}
		}
		return convertView;
	}

	/** 改变提示的文字颜色 */
	public void changeTextColor(RKCloudChatBaseMessage msgObj, TextView textView)
	{
		if (MSG_DIRECTION.RECEIVE == msgObj.getDirection())
		{
			textView.setTextColor(mContext.getResources().getColor(R.color.prompt_content_color));
		}
		else
		{
			textView.setTextColor(mContext.getResources().getColor(R.color.white));
		}
	}

	/*
	 * 点击事件
	 */
	private void showClickEvent(RKCloudChatBaseMessage msgObj, ItemViewBuffer itemBuffer)
	{

		if (RKCloudChatConstants.FLAG_AVCALL_IS_AUDIO.equals(msgObj.getExtension()) || RKCloudChatConstants.FLAG_AVCALL_IS_VIDEO.equals(msgObj.getExtension()))
		{
			// TODO 集成了语音通话和视频通话时使用
			RKCloudAVDemoManager.getInstance(mContext).dial(mContext, msgObj.getChatId(), RKCloudChatConstants.FLAG_AVCALL_IS_VIDEO.equals(msgObj.getExtension()));
			// 对未读标识的处理
			if (MSG_DIRECTION.RECEIVE == msgObj.getDirection() && MSG_STATUS.RECEIVE_RECEIVED == msgObj.getStatus())
			{
				if (mMmsManager.updateMsgStatusHasReaded(msgObj.getMsgSerialNum()) > 0)
				{
					msgObj.setStatus(MSG_STATUS.READED);
				}
			}
		}
		else if (!TextUtils.isEmpty(msgObj.getExtension()) && msgObj.getExtension().startsWith(RKCloudChatConstants.FLAG_MEETING_MUTLIMEETING, 0))
		{
			// TODO 集成了多人语音时使用
			String invitedJsonStr = null;
			if (MSG_DIRECTION.RECEIVE == msgObj.getDirection())
			{
				invitedJsonStr = msgObj.getExtension().substring(msgObj.getExtension().indexOf(",") + 1);
				// 对未读标识的处理
				if (MSG_STATUS.RECEIVE_RECEIVED == msgObj.getStatus())
				{
					if (mMmsManager.updateMsgStatusHasReaded(msgObj.getMsgSerialNum()) > 0)
					{
						msgObj.setStatus(MSG_STATUS.READED);
					}
				}
			}

			if (!TextUtils.isEmpty(invitedJsonStr))
			{
				RKCloudMeetingDemoManager.getInstance(mContext).joinMeeting(mContext, RKCloudMeetingInvitedInfoBean.parseJson(invitedJsonStr));
			}
		}
		else
		{
			if (msgObj instanceof ImageMessage)
			{
				// 查看图片
				Intent intent = new Intent(mContext, RKCloudChatViewImagesActivity.class);
				intent.putExtra(RKCloudChatViewImagesActivity.INTENT_KEY_MSGOBJ, msgObj);
				mContext.startActivity(intent);
				if(mChatClassObj == SingleChat.class)
				{
					RKCloudChatMessageManager.getInstance(mContext).sendReadedReceipt(msgObj);
				}

			}
			else if (msgObj instanceof AudioMessage || msgObj instanceof VideoMessage || msgObj instanceof FileMessage)
			{
				if (MSG_STATUS.RECEIVE_RECEIVED == msgObj.getStatus() || MSG_STATUS.RECEIVE_DOWNFAILED == msgObj.getStatus())
				{
					// 表示下载操作
					// 先判断SD卡是否可用、容量是否已满，用于提前处理
					if (!RKCloudChatTools.isSDCardValid())
					{
						RKCloudChatTools.showToastText(mContext, mContext.getString(R.string.rkcloud_chat_sdcard_error));
						return;
					}
					if(null != msgObj)
					{
						RKCloudChatMessageManager.getInstance(mContext).sendReadedReceipt(msgObj);
					}
					// 下载媒体消息
					itemBuffer.newMsgSign.setVisibility(View.GONE);
					itemBuffer.loadLayout.setVisibility(View.VISIBLE);
					itemBuffer.loadProgress.setText("0%");
					msgObj.setStatus(MSG_STATUS.RECEIVE_DOWNING);
					mMmsManager.downAttach(msgObj.getMsgSerialNum());
					// 记录下载的消息
					mRecordDowningTV.put(itemBuffer.loadProgress, msgObj.getMsgSerialNum());

					if (msgObj instanceof VideoMessage)
					{
						itemBuffer.playImg.setVisibility(View.GONE);
					}
					if (msgObj instanceof AudioMessage)
					{
						// 记录播放的最后一条消息编号，用于下载完成后自动播放
						RKCloudChatMsgActivity.lastAudioSerialNum = msgObj.getMsgSerialNum();
					}
				}
				else
				{
					if (msgObj instanceof AudioMessage)
					{ // 播放或关闭播放语音消息
						// 文件不存在时给出提示
						if (!new File(((AudioMessage) msgObj).getFilePath()).exists())
						{
							RKCloudChatTools.showToastText(mContext, mContext.getString(R.string.rkcloud_chat_unfound_resource));
							return;
						}
						if (msgObj.getMsgSerialNum().equals(mAudioHelper.getPlayingMsgSerialNum()))
						{
							// 关闭播放
							mAudioHelper.stopMsgOfAudio();
							mCurrPlayAudioMsg = null;
							mCurrAudioDurationTV = null;
							if (null != mCurrAnimationDrawable)
							{
								mCurrAnimationDrawable.stop();
								mCurrAnimationDrawable.selectDrawable(0);
								mCurrAnimationDrawable = null;
							}
							mUpdatePlayTime.removeCallbacks(mPlayProgressRunner);
							// 清除播放的最后一条消息编号
							RKCloudChatMsgActivity.lastAudioSerialNum = null;

						}
						else
						{
							// 先关闭正在播放的消息
							mAudioHelper.stopMsgOfAudio();
							mCurrPlayAudioMsg = null;
							mCurrAudioDurationTV = null;
							// 开始播放文件
							mAudioHelper.playMsgOfAudio(msgObj.getMsgSerialNum(), ((AudioMessage) msgObj).getFilePath());
							// 记录播放的最后一条消息编号
							RKCloudChatMsgActivity.lastAudioSerialNum = msgObj.getMsgSerialNum();
						}
						notifyDataSetChanged();

					}
					else if (msgObj instanceof VideoMessage)
					{ // 表示播放视频消息
						// 文件不存在时给出提示
						if (!new File(((VideoMessage) msgObj).getFilePath()).exists())
						{
							RKCloudChatTools.showToastText(mContext, mContext.getString(R.string.rkcloud_chat_unfound_resource));
							return;
						}
						// 播放视频
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setDataAndType(Uri.fromFile(new File(((VideoMessage) msgObj).getFilePath())), "video/mp4");
						mContext.startActivity(intent);

					}
					else if (msgObj instanceof FileMessage)
					{ // 打开文件消息
						// 文件不存在时给出提示
						if (!new File(((FileMessage) msgObj).getFilePath()).exists())
						{
							RKCloudChatTools.showToastText(mContext, mContext.getString(R.string.rkcloud_chat_unfound_resource));
							return;
						}
						RKCloudChatTools.openFile(mContext, ((FileMessage) msgObj).getFilePath());
					}
				}
			}
		}
	}

	/**
	 * 更新文件下载进度值
	 * 
	 * @param msgSerialNum
	 * @param progress
	 */
	public void updateDowningProgress(String msgSerialNum, int progress)
	{
		Iterator<TextView> viewCaches = mRecordDowningTV.keySet().iterator();
		TextView tv = null;
		while (viewCaches.hasNext())
		{
			tv = viewCaches.next();
			if (msgSerialNum.equals(mRecordDowningTV.get(tv)))
			{
				tv.setText(progress + "%");
				return;
			}
		}
	}

	/**
	 * 文件下载完成后去除记录
	 * 
	 * @param msgSerialNum
	 */
	public void removeDowningProgress(String msgSerialNum)
	{
		Iterator<TextView> viewCaches = mRecordDowningTV.keySet().iterator();
		TextView tv = null;
		while (viewCaches.hasNext())
		{
			tv = viewCaches.next();
			if (msgSerialNum.equals(mRecordDowningTV.get(tv)))
			{
				mRecordDowningTV.remove(tv);
				return;
			}
		}
	}

	/*
	 * 文本消息的显示
	 */
	private void showTextMsg(View convertView, final TextMessage msgObj)
	{
		TextView contentTV = (TextView) convertView.findViewById(R.id.txt_content);
		contentTV.setOnLongClickListener(new OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				RKCloudChatMsgActivity activity = (RKCloudChatMsgActivity) mContext;
				activity.showContextMenu(msgObj);
				return false;
			}
		});
		SpannableString spStr = new SpannableString(RKCloudChatTools.parseMsgFace(mContext, msgObj.getContent(), 0, 2));
		new RKCloudChatNoLineClickSpan(spStr);
		contentTV.setText(spStr);
		if (MSG_DIRECTION.RECEIVE == msgObj.getDirection())
		{
			contentTV.setTextColor(mContext.getResources().getColor(R.color.title_text_color));
		}
		else
		{
			contentTV.setTextColor(mContext.getResources().getColor(R.color.white));
		}
		contentTV.setMovementMethod(RKCloudChatLongClickLinkMovementMethod.getInstance());
		contentTV.setClickable(true);
	}

	/*
	 * 图片消息的显示
	 */
	private void showImageMsg(View convertView, ImageMessage msgObj)
	{
		ImageView pic = (ImageView) convertView.findViewById(R.id.img_src);
		// 设置默认图片
		pic.setImageResource(R.drawable.rkcloud_chat_img_picmsg_default);
		String filePath = null;
		if (!TextUtils.isEmpty(msgObj.getThumbPath()) && new File(msgObj.getThumbPath()).exists())
		{
			filePath = msgObj.getThumbPath();
		}
		else if (!TextUtils.isEmpty(msgObj.getFilePath()) && new File(msgObj.getFilePath()).exists())
		{
			filePath = msgObj.getFilePath();
		}

		// 加载图像
		if (null != filePath)
		{
			RKCloudChatImageRequest imageReq = new RKCloudChatImageRequest(IMAGE_REQUEST_TYPE.GET_MSG_THUMBNAIL, filePath, msgObj.getMsgSerialNum());
			RKCloudChatImageResult imgResult = RKCloudChatImageAsyncLoader.getInstance(mContext).sendPendingRequestQuryCache(imageReq);
			if (null != imgResult && null != imgResult.resource)
			{
				pic.setImageDrawable(imgResult.resource);
			}
		}
	}

	/*
	 * 音频消息的显示
	 */
	private void showAudioMsg(View convertView, final AudioMessage msgObj)
	{
		ImageView playImg = (ImageView) convertView.findViewById(R.id.audio_playicon);
		TextView durationTV = (TextView) convertView.findViewById(R.id.audio_duration);
		// 设置播放图标
		if (MSG_DIRECTION.SEND == msgObj.getDirection())
		{
			playImg.setImageResource(R.drawable.rkcloud_chat_msg_audio_send_playing);
		}
		else
		{
			playImg.setImageResource(R.drawable.rkcloud_chat_msg_audio_received_playing);
		}
		AnimationDrawable animationDrawable = (AnimationDrawable) playImg.getDrawable();
		// 设置音频文件显示的时长
		durationTV.setText(String.format("%d\"", msgObj.getDuration()));

		if (msgObj.getMsgSerialNum().equals(mAudioHelper.getPlayingMsgSerialNum()))
		{
			// 正在播放中
			if (null == mCurrPlayAudioMsg)
			{
				mCurrPlayAudioMsg = msgObj;
				mUpdatePlayTime.post(mPlayProgressRunner);
			}
			mCurrAudioDurationTV = durationTV;
			mCurrAnimationDrawable = animationDrawable;
			mCurrAnimationDrawable.start();

		}
		else
		{
			if (animationDrawable.isRunning())
			{
				animationDrawable.stop();
				animationDrawable.selectDrawable(0);
			}
			// 防止布局重用时影响其他消息的显示
			if (durationTV == mCurrAudioDurationTV)
			{
				mCurrAudioDurationTV = null;
			}
			if (animationDrawable == mCurrAnimationDrawable)
			{
				mCurrAnimationDrawable = null;
			}
		}
	}

	/*
	 * 视频消息的显示
	 */
	private void showVideoMsg(View convertView, ImageView playImg, VideoMessage msgObj)
	{
		ImageView pic = (ImageView) convertView.findViewById(R.id.video_img);
		LinearLayout infoLayout = (LinearLayout) convertView.findViewById(R.id.video_bottomlayout);
		TextView sizeTV = (TextView) convertView.findViewById(R.id.video_filesize);
		TextView durationTV = (TextView) convertView.findViewById(R.id.video_duration);

		sizeTV.setVisibility(View.GONE);
		// 设置默认图片
		pic.setImageResource(R.drawable.rkcloud_chat_video_default);
		playImg.setVisibility(View.GONE);
		// 显示视频信息布局
		if ((MSG_DIRECTION.RECEIVE == msgObj.getDirection() && MSG_STATUS.RECEIVE_DOWNING == msgObj.getStatus())
				|| (MSG_DIRECTION.SEND == msgObj.getDirection() && MSG_STATUS.SEND_SENDING == msgObj.getStatus()))
		{
			infoLayout.setVisibility(View.GONE);
			playImg.setVisibility(View.GONE);
		}
		else
		{
			infoLayout.setVisibility(View.VISIBLE);
		}
		// 设置大小
		sizeTV.setText(RKCloudChatTools.formatFileSize(msgObj.getFileSize()));
		// 设置时长
		durationTV.setText(RKCloudChatTools.formatDuration(msgObj.getDuration()));
		// 加载图像
		if (!TextUtils.isEmpty(msgObj.getThumbPath()) && new File(msgObj.getThumbPath()).exists())
		{
			// 如果在缓存中则直接设置图片
			RKCloudChatImageRequest imageReq = new RKCloudChatImageRequest(IMAGE_REQUEST_TYPE.GET_MSG_VIDEO_THUMBNAIL, msgObj.getThumbPath(), msgObj.getMsgSerialNum());
			RKCloudChatImageResult imgResult = RKCloudChatImageAsyncLoader.getInstance(mContext).sendPendingRequestQuryCache(imageReq);
			if (null != imgResult && null != imgResult.resource)
			{
				pic.setImageDrawable(imgResult.resource);
				if (MSG_DIRECTION.SEND == msgObj.getDirection() && MSG_STATUS.SEND_SENDING == msgObj.getStatus())
				{
					playImg.setVisibility(View.GONE);
				}
				else
				{
					playImg.setVisibility(View.VISIBLE);
				}
			}
		}
	}

	/*
	 * 文件消息的显示
	 */
	private void showFileMsg(View convertView, FileMessage msgObj)
	{
		TextView fileName = (TextView) convertView.findViewById(R.id.file_name);
		TextView fileSize = (TextView) convertView.findViewById(R.id.file_size);
		fileName.setText(msgObj.getFileName());
		fileSize.setText(RKCloudChatTools.formatFileSize(msgObj.getFileSize()));
	}

	/*
	 * 自定义消息的显示
	 */
	private void showCustomMsg(View convertView, CustomMessage msgObj)
	{
		// TODO 自定义消息的内容显示
		TextView contentTV = (TextView) convertView.findViewById(R.id.txt_content);
		contentTV.setText(msgObj.getContent());
	}

	/*
	 * 本地消息的显示
	 */
	private void showLocalMsg(View convertView, LocalMessage msgObj)
	{
		// TODO 本地消息的内容显示
		TextView contentTV = (TextView) convertView.findViewById(R.id.txt_content);
		contentTV.setText(msgObj.getContent());
	}
}
