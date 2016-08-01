package com.rongkecloud.chat.demo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import com.rongke.cloud.meeting.demo.RKCloudMeetingDemoManager;
import com.rongkecloud.chat.*;
import com.rongkecloud.chat.demo.entity.RKCloudChatContact;
import com.rongkecloud.chat.demo.tools.RKCloudChatImageTools;
import com.rongkecloud.chat.demo.tools.RKCloudChatPlayAudioMsgTools;
import com.rongkecloud.chat.demo.tools.RKCloudChatTools;
import com.rongkecloud.chat.demo.ui.RKCloudChatListActivity;
import com.rongkecloud.chat.demo.ui.RKCloudChatMsgActivity;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatCustomDialog;
import com.rongkecloud.chat.interfaces.RKCloudChatGroupCallBack;
import com.rongkecloud.chat.interfaces.RKCloudChatReceivedMsgCallBack;
import com.rongkecloud.chat.interfaces.RKCloudChatRequestCallBack;
import com.rongkecloud.chat.interfaces.RKCloudChatResult;
import com.rongkecloud.sdkbase.RKCloud;
import com.rongkecloud.sdkbase.RKCloudLog;
import com.rongkecloud.test.R;
import com.rongkecloud.test.system.RKCloudDemo;
import com.rongkecloud.test.ui.MainActivity;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * 云视互动即时通信中消息相关的处理类，关联了SDK和Demo之间的交互
 * 
 * @author jessica.yang
 *
 */
public class RKCloudChatMmsManager implements RKCloudChatReceivedMsgCallBack, RKCloudChatGroupCallBack
{

	/** {@link Message#getData()} 里面的会话ID Key定义 */
	public static final String KEY_CONVERSATION_ID = "key.conversation.id";

	private static final String TAG = RKCloudChatMmsManager.class.getSimpleName();
	private static RKCloudChatMmsManager mInstance = null;
	private Context mContext;

	private RKCloudChatMessageManager mChatManager;
	private Handler mUiHandler;
	private Handler mMainUiHandler;// 主界面带tab切换的handler
	private String mUnNeedSendNotifyChatId;// 当前不需要通知的会话ID
	private String mLastShowChatId;// 最后一次显示的聊天会话ID

	private RKCloudMeetingDemoManager mMeetingDemoManager;

	// 多媒体消息的notifacation id
	private NotificationManager mNotificationManager;
	private static final String TAG_NOTIFICATION_MMS = "RKCLOUD_CHAT_MMS";
	private Map<String, Integer> mNotificationChatIdCache = null;// 通知栏中按照会话ID分别通知时记录的信息，其中key为会话ID，value为对应的id值
	private int mNotificationIdStart;// 通知ID的起始值
	private Map<String, Long> mRecordDownMms;// 记录下载的媒体消息 key为消息编号，value为下载的开始时间

	private RKCloudChatMmsManager(Context context)
	{
		mContext = context;
		mChatManager = RKCloudChatMessageManager.getInstance(mContext);
		mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationChatIdCache = new HashMap<String, Integer>();
		mRecordDownMms = new HashMap<String, Long>();
		mNotificationIdStart = 50;
		mMeetingDemoManager = RKCloudMeetingDemoManager.getInstance(mContext);
	}

	public static RKCloudChatMmsManager getInstance(Context context)
	{
		if (null == mInstance)
		{
			mInstance = new RKCloudChatMmsManager(context);
		}
		return mInstance;
	}

	@Override
	protected void finalize() throws Throwable
	{
		super.finalize();
	}

	/**
	 * 绑定UI页面的handler，需要与主界面的handler分开绑定
	 * 
	 * @param handler
	 */
	public void bindUiHandler(Handler handler)
	{
		mUiHandler = handler;
	}

	/**
	 * 绑定主界面的hanlder
	 * 
	 * @param handler
	 */
	public void bindMainUiHandler(Handler handler)
	{
		mMainUiHandler = handler;
	}

	private void sendHandlerMsg(int what, Object obj)
	{
		if (null != mUiHandler)
		{
			Message msg = mUiHandler.obtainMessage();
			msg.what = what;
			msg.obj = obj;
			msg.sendToTarget();
		}
	}

	private void sendHandlerMsg(int what, int arg1)
	{
		if (null != mUiHandler)
		{
			Message msg = mUiHandler.obtainMessage();
			msg.what = what;
			msg.arg1 = arg1;
			msg.sendToTarget();
		}
	}

	private void sendHandlerMsg(int what, int arg1, Object obj)
	{
		if (null != mUiHandler)
		{
			Message msg = mUiHandler.obtainMessage();
			msg.what = what;
			msg.arg1 = arg1;
			msg.obj = obj;
			msg.sendToTarget();
		}
	}

	private void sendHandlerMsg(Message msg)
	{
		if (null != mUiHandler)
		{
			mUiHandler.sendMessage(msg);
		}
	}

	/**
	 * 获取当前用户所有的群信息
	 * 
	 * @return
	 */
	public List<RKCloudChatBaseChat> queryAllGroupsInfo()
	{
		if (null == mChatManager)
		{
			return new ArrayList();
		}
		return mChatManager.queryAllGroups();
	}

	/**
	 * 获取当前用户创建的群
	 * 
	 * @return
	 */
	public List<RKCloudChatBaseChat> queryMyCreatedGroups()
	{
		if (null == mChatManager)
		{
			return new ArrayList();
		}
		return mChatManager.queryAllMyCreatedGroups();
	}

	/**
	 * 获取当前用户参与的群
	 * 
	 * @return
	 */
	public List<RKCloudChatBaseChat> queryAllMyAttendedGroups()
	{
		if (null == mChatManager)
		{
			return new ArrayList();
		}
		return mChatManager.queryAllMyAttendedGroups();
	}

	/**
	 * 进入会话列表页面
	 */
	public void enterChatListActivity()
	{
		Intent intent = new Intent(mContext, RKCloudChatListActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(intent);
	}

	/**
	 * 进入聊天消息页面
	 * 
	 * @param chatId
	 */
	public void enterMsgListActivity(String chatId)
	{
		if (TextUtils.isEmpty(chatId))
		{
			return;
		}
		if (null != RKCloudChatMsgActivity.mMsgInstance)
		{
			RKCloudChatMsgActivity.mMsgInstance.finish();
		}
		chatId = chatId.toLowerCase();
		// 查询会话信息
		RKCloudChatBaseChat chatObj = queryChat(chatId);
		// Intent intent = new Intent(mContext, RKCloudChatMsgActivity.class);
		// if(null==chatObj || chatObj instanceof SingleChat){
		// intent.putExtra(RKCloudChatMsgActivity.INTENT_KEY_MSGLIST_CHATID,
		// chatId);
		// }else if(chatObj instanceof GroupChat){
		// intent.putExtra(RKCloudChatMsgActivity.INTENT_KEY_MSGLIST_GROUPID,
		// chatId);
		// }
		// intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// mContext.startActivity(intent);

		// TODO 跳转到主页面，统一入口
		Intent intent = new Intent(mContext, MainActivity.class);
		intent.setAction(MainActivity.ACTION_TO_MSGUI);
		if (null == chatObj || chatObj instanceof SingleChat)
		{
			intent.putExtra(MainActivity.ACTION_TO_MSGUI_PARAMS_SINGLECHATID, chatId);
		}
		else if (chatObj instanceof GroupChat)
		{
			intent.putExtra(MainActivity.ACTION_TO_MSGUI_PARAMS_GROUPCHATID, chatId);
		}

		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(intent);
	}

	/**
	 * 发送消息并打开聊天页面
	 * 
	 * @param context
	 * @param msgObj
	 */
	public void sendMmsAndOpenMsgUi(Context context, RKCloudChatBaseMessage msgObj)
	{
		context = null == context ? mContext : context;
		if (null == context || null == msgObj || TextUtils.isEmpty(msgObj.getChatId()))
		{
			return;
		}
		// 发送消息
		sendMms(msgObj);
		// 进入聊天页面
		enterMsgListActivity(msgObj.getChatId());
	}

	/**
	 * @function 设置不需要向通知栏中提醒收到新消息的会话ID
	 * @param
	 * @return
	 **/
	public void setUnNeedNotifyChatId(String chatId)
	{
		mUnNeedSendNotifyChatId = chatId;
		if (!TextUtils.isEmpty(chatId))
		{
			cancelNotify(chatId);
			mLastShowChatId = chatId;
		}
	}

	/**
	 * 通知主页面更新未读消息总数
	 */
	public void updateUnreadMsgCountsInMain()
	{
		if (null != mMainUiHandler)
		{
			mMainUiHandler.sendEmptyMessage(RKCloudChatUiHandlerMessage.UNREAD_MSG_COUNT_CHANGED);
		}
	}

	/**
	 * 获取所有会话中的未读消息条数
	 * 
	 * @return
	 */
	public int getAllUnReadMsgCounts()
	{
		if (null == mChatManager)
		{
			return 0;
		}
		return mChatManager.getAllUnReadMsgsCount();
	}

	/**
	 * @function 获取所有会话信息
	 * @return {@link RKCloudChatBaseChat}的对象集合
	 */
	public List<RKCloudChatBaseChat> queryAllChats()
	{
		if (null == mChatManager)
		{
			return new ArrayList();
		}
		return mChatManager.queryAllChats();
	}

	/**
	 * @function 获取单个会话信息
	 * @param chatId
	 *            String 会话ID
	 * @return {@link RKCloudChatBaseChat}对象
	 */
	public RKCloudChatBaseChat queryChat(String chatId)
	{
		if (null == mChatManager)
		{
			return null;
		}
		return mChatManager.queryChat(chatId);
	}

	/**
	 * 设置会话是否置顶
	 * 
	 * @param chatId
	 * @param isTop
	 * @return
	 */
	public long setChatTop(String chatId, boolean isTop)
	{
		if (null == mChatManager)
		{
			return 0;
		}
		return mChatManager.setChatIsTop(chatId, isTop);
	}

	/**
	 * 更新聊天背景图片
	 * 
	 * @param chatId
	 * @param imagePath
	 * @return
	 */
	public long updateChatBgImg(String chatId, String imagePath)
	{
		if (null == mChatManager)
		{
			return 0;
		}
		return mChatManager.updateBackgroundImageInChat(chatId, imagePath);
	}

	/**
	 * 清空所有会话信息
	 * 
	 * @param delFile
	 *            boolean 是否删除媒体消息文件 true:删除 false:不删除 默认是false
	 */
	public long delAllChatInfos(boolean delFile)
	{
		if (null == mChatManager)
		{
			return 0;
		}
		long result = mChatManager.clearChatsAndMsgs(delFile);
		if (result > 0)
		{
			// 清除通知栏中内容
			cancelNotify(null);
			updateUnreadMsgCountsInMain();// 更新主页面的未读条数
		}
		sendHandlerMsg(RKCloudChatUiHandlerMessage.DELETE_ALL_CHATS, (int) result);
		return result;
	}

	/**
	 * @function 删除单个会话及相关内容
	 * @param chatId
	 *            String 会话ID，如果是单聊则为好友的云视互动账号；如果是群聊则为群ID
	 * @param delFile
	 *            boolean 是否删除该会话下面的媒体消息文件 true:删除 false:不删除 默认是false
	 * @return long >0 删除成功 <0 删除失败
	 */
	public long delChat(String chatId, boolean delFile)
	{
		if (null == mChatManager)
		{
			return 0;
		}
		long delRes = mChatManager.deleteChat(chatId, delFile);
		if (delRes > 0)
		{
			updateUnreadMsgCountsInMain();// 更新主页面的未读条数
			cancelNotify(chatId);
		}
		sendHandlerMsg(RKCloudChatUiHandlerMessage.DELETE_SINGLE_CHAT, (int) delRes, chatId);
		return delRes;
	}

	/**
	 * 获取会话中所有成员的号码
	 * 
	 * @param chatId
	 * @return
	 */
	public List<String> queryGroupUsers(String chatId)
	{
		if (null == mChatManager)
		{
			return new ArrayList<String>();
		}
		List<String> accounts = mChatManager.queryGroupUsers(chatId);
		// 同步群成员信息
		RKCloudChatContactManager.getInstance(mContext).syncContactsInfo(accounts);
		return accounts;
	}

	/**
	 * 获取草稿箱
	 * 
	 * @param chatId
	 * @return
	 */
	public String getDraft(String chatId)
	{
		if (null == mChatManager)
		{
			return null;
		}
		return mChatManager.getDraft(chatId);
	}

	/**
	 * 保存草稿箱内容
	 * 
	 * @param chatId
	 * @param content
	 * @return
	 */
	public long saveDraft(String chatId, String content)
	{
		if (null == mChatManager)
		{
			return 0;
		}
		long result = mChatManager.saveDraft(chatId, content);
		if (result > 0)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.DRAFT_MAG_CHANGED, (int) result);
		}

		return result;
	}

	/**
	 * 退出消息列表时更新消息全部为已读
	 * 
	 * @param chatId
	 * @return
	 */
	public long updateMsgsReadedInChat(String chatId)
	{
		if (null == mChatManager)
		{
			return 0;
		}
		long result = mChatManager.updateMsgsReadedInChat(chatId);
		if (result > 0)
		{
			updateUnreadMsgCountsInMain();
		}
		return result;
	}

	/**
	 * @function 获取单个会话中的消息
	 * @param chatId
	 *            String 会话Id
	 * @param minMsgId
	 *            消息ID
	 * @param limit
	 *            int 获取的消息条数
	 * @return {@link RKCloudChatBaseMessage}的对象集合
	 */
	public List<RKCloudChatBaseMessage> queryMmsList(String chatId, long minMsgId, int limit)
	{
		if (null == mChatManager)
		{
			return new ArrayList();
		}
		if (minMsgId > 0)
		{
			return mChatManager.queryLocalChatMsgs(chatId, minMsgId);
		}
		else
		{
			return mChatManager.queryLocalChatMsgs(chatId, limit);
		}
	}

	/**
	 * @function 获取单个会话中的历史消息
	 * @param chatId
	 *            String 会话Id
	 * @param maxMsgId
	 *            消息ID
	 * @param limit
	 *            int 获取的消息条数
	 * @return {@link RKCloudChatBaseMessage}的对象集合
	 */
	public List<RKCloudChatBaseMessage> queryHistoryMmsList(String chatId, long maxMsgId, int limit)
	{
		if (null == mChatManager)
		{
			return new ArrayList();
		}
		return mChatManager.queryLocalHistoryChatMsgs(chatId, maxMsgId, limit);
	}

	/**
	 * 获取聊天页面未读条数中最小的消息ID值
	 * 
	 * @param chatId
	 * @param unreadcnt
	 * @return
	 */
	public long getLeastMsgIdOfUnreadMsgs(String chatId, int unreadcnt)
	{
		if (null == mChatManager)
		{
			return 0;
		}
		return mChatManager.getLeastMsgIdOfUnreadMsgs(chatId, unreadcnt);
	}

	/**
	 * 获取会话中指定类型的所有消息
	 * 
	 * @param chatId
	 *            String 会话Id
	 * @param type
	 *            Class<RKCloudChatBaseMessage>中的type
	 * @return {@link RKCloudChatBaseMessage}的对象集合
	 */
	public List<RKCloudChatBaseMessage> queryMmsByType(String chatId, String type)
	{
		if (null == mChatManager)
		{
			return new ArrayList<>();
		}
		return mChatManager.queryAllMsgsByType(chatId, type);
	}

	/**
	 * @function 查询单条消息
	 * @param msgSerialNum
	 *            String 消息编号
	 * @return {@link RKCloudChatBaseMessage}对象
	 */
	public RKCloudChatBaseMessage queryChatMsg(String msgSerialNum)
	{
		if (null == mChatManager)
		{
			return null;
		}
		return mChatManager.queryChatMsg(msgSerialNum);
	}

	/**
	 * 插入消息
	 * 
	 * @param msgObj
	 * @param chatClassObj
	 * @return
	 */
	public long addLocalMsg(LocalMessage msgObj, Class<? extends RKCloudChatBaseChat> chatClassObj)
	{
		if (null == mChatManager)
		{
			return 0;
		}

		long result = mChatManager.addLocalMsg(msgObj, chatClassObj);
		if (result > 0)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.ADD_MSG_TO_LOCALDB, msgObj);
		}
		return result;
	}

	/**
	 * 更新消息状态为已读
	 * 
	 * @param msgSerialNum
	 * @return
	 */
	public long updateMsgStatusHasReaded(String msgSerialNum)
	{
		if (null == mChatManager)
		{
			return 0;
		}
		return mChatManager.updateMsgStatusHasReaded(msgSerialNum);
	}

	/**
	 * @function 删除会话中的所有消息
	 * @param chatId
	 *            String 会话ID
	 * @return long >0 删除成功 <=0 删除失败
	 */
	public long delMsgsByChatId(String chatId)
	{
		if (null == mChatManager)
		{
			return 0;
		}
		// 删除消息
		long delRes = mChatManager.deleteAllMsgsInChat(chatId, false);
		if (delRes > 0)
		{
			// 通知更新主页面的未读条数
			updateUnreadMsgCountsInMain();
		}

		return delRes;
	}

	/**
	 * @function 删除一条消息
	 * @param msgSerialNum
	 *            String 消息的唯一编号
	 * @param chatId
	 *            String 消息所在的会话ID，主要是用于更新会话中的最后一条消息时使用
	 * @return long >0 删除成功 <=0 删除失败
	 */
	public long delMsg(String msgSerialNum, String chatId)
	{
		if (null == mChatManager)
		{
			return 0;
		}
		return mChatManager.deleteChatMsg(chatId, msgSerialNum, false);
	}

	/**
	 * 批量删除消息
	 * 
	 * @param msgSerialNums
	 * @param chatId
	 * @return
	 */
	public long delMsgs(List<String> msgSerialNums, String chatId)
	{
		if (null == mChatManager)
		{
			return 0;
		}
		return mChatManager.deleteChatMsgs(chatId, msgSerialNums, false);
	}

	/**
	 * 根据关键词搜索消息
	 * 
	 * @param keyword
	 * @return
	 */
	public List<HashMap<RKCloudChatBaseChat, List<RKCloudChatBaseMessage>>> queryMessageKeyWord(String keyword)
	{
		if (null == mChatManager)
		{
			return new ArrayList();
		}
		return mChatManager.queryMessageKeyWord(keyword);
	}

	/**
	 * 查询搜索到的消息及该消息的前后消息
	 *
	 * @param chatId
	 *            会话id
	 * @param msgId
	 *            消息id
	 * @param onceCount
	 *            一次需要获取到的数量
	 * @return List<RKCloudChatBaseMessage> 查询到的消息数组
	 */
	public List<RKCloudChatBaseMessage> queryLocalChatMsgs(String chatId, String msgId, long onceCount)
	{
		if (null == mChatManager)
		{
			return new ArrayList();
		}
		return mChatManager.queryLocalChatMsgs(chatId, msgId, onceCount);
	}

	/**
	 * @function 获取会话的新消息
	 * @param chatId
	 *            会话id
	 * @param minMsgId
	 *            消息id
	 * @param limit
	 *            一次需要获取到的数量
	 * @return List<RKCloudChatBaseMessage> 查询到的消息数组
	 */
	public List<RKCloudChatBaseMessage> queryNewChatMsgs(String chatId, long minMsgId, int limit)
	{
		if (null == mChatManager)
		{
			return new ArrayList();
		}
		return mChatManager.queryNewChatMsgs(chatId, minMsgId, limit);
	}


    public  List<RKCloudChatBaseMessage> refreshMessageState(String chatId, long msgId)
    {
        if (null == mChatManager)
        {
            return new ArrayList<>();
        }

        return  mChatManager.queryLocalChatMsgs(chatId,msgId);
    }


	/**
	 * 获取会话消息
	 * 
	 * @param chatId
	 *            会话id
	 * @param msgId
	 *            消息id
	 * @param limit
	 *            一次需要获取到的数量
	 */
	public List<RKCloudChatBaseMessage> getChatMsgs(String chatId, String chatType, long msgId, int limit)
	{
		if (null == mChatManager)
		{
			return new ArrayList<>();
		}
		RKCloudLog.e(TAG, "getChatMsgs=====================================");
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		final List<RKCloudChatBaseMessage> messages = new ArrayList<>();
		mChatManager.queryChatMsgs(chatId, chatType, msgId, limit, new RKCloudChatResult<List<RKCloudChatBaseMessage>>()
		{
			@Override
			public void onResult(List<RKCloudChatBaseMessage> value)
			{
				RKCloudLog.e(TAG, "getChatMsgs==========value = " + value);
				messages.addAll(value);
				countDownLatch.countDown();
			}
		});
		try
		{
			countDownLatch.await();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		return messages;
	}

	/**
	 * 重新发送失败的消息
	 * 
	 * @param msgSerialNum
	 */
	public void reSendMms(final String msgSerialNum)
	{
		if (null == mChatManager)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_RESEND_MMS, RKCloudChatErrorCode.RK_SDK_UNINIT, msgSerialNum);
			return;
		}
		mChatManager.reSendChatMsg(msgSerialNum, new RKCloudChatRequestCallBack()
		{
			@Override
			public void onSuccess(Object results)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_RESEND_MMS, 0, msgSerialNum);
			}

			@Override
			public void onFailed(int errorCode, Object obj)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_RESEND_MMS, errorCode, msgSerialNum);
			}

			@Override
			public void onProgress(int value)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_UPDATE_DOWNING_PROGRESS, value, msgSerialNum);
			}
		});
	}

	/**
	 * 发送消息
	 * 
	 * @param msgObj
	 */
	public void sendMms(final RKCloudChatBaseMessage msgObj)
	{
		if (null == msgObj)
		{
			return;
		}

		if (null == mChatManager)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_SEND_MMS, RKCloudChatErrorCode.RK_SDK_UNINIT, msgObj.getMsgSerialNum());
			return;
		}
		mChatManager.sendChatMsg(msgObj, new RKCloudChatRequestCallBack()
		{
			@Override
			public void onSuccess(Object results)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_SEND_MMS, 0, msgObj.getMsgSerialNum());
			}

			@Override
			public void onFailed(int errorCode, Object obj)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_SEND_MMS, errorCode, msgObj.getMsgSerialNum());
			}

			@Override
			public void onProgress(int value)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_UPDATE_DOWNING_PROGRESS, value, msgObj.getMsgSerialNum());
			}
		});
	}

	/**
	 * 转发消息
	 * 
	 * @param msgSerialNum
	 * @param chatId
	 */
	public void forwardMms(final String msgSerialNum, String chatId)
	{
		if (null == mChatManager)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_FORWARD_MMS, RKCloudChatErrorCode.RK_SDK_UNINIT, msgSerialNum);
			return;
		}
		mChatManager.forwardChatMsg(msgSerialNum, chatId, new RKCloudChatRequestCallBack()
		{
			@Override
			public void onSuccess(Object results)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_FORWARD_MMS, 0, msgSerialNum);
			}

			@Override
			public void onFailed(int errorCode, Object obj)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_FORWARD_MMS, errorCode, msgSerialNum);
			}

			@Override
			public void onProgress(int value)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_UPDATE_DOWNING_PROGRESS, value, msgSerialNum);
			}
		});
	}

	/**
	 * 撤销消息
	 *
	 * @param msgSerialNum
	 */
	public void revokeMsg(final String msgSerialNum)
	{
		if (null == mChatManager)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_REVOKE_MMS, RKCloudChatErrorCode.RK_SDK_UNINIT, msgSerialNum);
			return;
		}
		mChatManager.revokeMessage(msgSerialNum, new RKCloudChatRequestCallBack()
		{
			@Override
			public void onSuccess(Object results)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_REVOKE_MMS, 0, msgSerialNum);
			}

			@Override
			public void onFailed(int errorCode, Object obj)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_REVOKE_MMS, errorCode, msgSerialNum);
			}

			@Override
			public void onProgress(int value)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_REVOKE_MMS, value, msgSerialNum);
			}
		});
	}

	/**
	 * @function 设置会话是否需要提醒功能
	 * @param chatId
	 *            String 会话ID
	 * @param isRemind
	 *            boolean true: 提醒 false: 不提醒
	 * @return >0 更新成功 <0 更新失败
	 */
	public void maskGroupMsgRemind(String chatId, boolean isRemind)
	{
		if (null == mChatManager)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_MASK_GROUP_REMIND, RKCloudChatErrorCode.RK_SDK_UNINIT);
			return;
		}
		mChatManager.maskGroupMsgRemind(chatId, isRemind, new RKCloudChatRequestCallBack()
		{
			@Override
			public void onSuccess(Object results)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_MASK_GROUP_REMIND, RKCloudChatErrorCode.RK_SUCCESS);
			}

			@Override
			public void onProgress(int value)
			{

			}

			@Override
			public void onFailed(int errorCode, Object object)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_MASK_GROUP_REMIND, errorCode);
			}
		});
	}

	/**
	 * 拍照或选择图片后的处理
	 * 
	 * @param filePath
	 */
	public void processPhoto(final String chatId, final String imageDirecotry, final String filePath, final boolean isTakePhone)
	{
		// sd卡不存在
		if (!Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.SDCARD_NOT_EXIST, chatId);
			return;
		}
		// sd卡已满或者不可用的错误
		if (null == Environment.getExternalStorageDirectory())
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.SDCARD_ERROR, chatId);
			return;
		}
		// 开始进行图片的压缩处理
		new Thread(new Runnable()
		{
			public void run()
			{
				// 获取压缩后的Bitmap对象
				Bitmap newImageMap = RKCloudChatImageTools.resizeBitmap(filePath, RKCloudChatConstants.IMAGE_DEFAULT_WIDTH, RKCloudChatConstants.IMAGE_DEFAULT_HEIGHT);
				// 如果对象为null，则返回
				if (newImageMap == null)
				{
					sendHandlerMsg(RKCloudChatUiHandlerMessage.IMAGE_COMPRESS_FAILED, chatId);
					return;
				}

				// 生成新的图片路径
				String tempImgName = null;
				if (isTakePhone)
				{
					tempImgName = filePath;
				}
				else
				{

					File file = new File(imageDirecotry);
					if (!file.exists())
					{
						if (!file.mkdirs())
						{
							sendHandlerMsg(RKCloudChatUiHandlerMessage.SDCARD_ERROR, chatId);
						}
					}
					if (imageDirecotry.endsWith("/"))
					{
						tempImgName = String.format("%stempimage_%d.jpg", imageDirecotry, System.currentTimeMillis());
					}
					else
					{
						tempImgName = String.format("%s/tempimage_%d.jpg", imageDirecotry, System.currentTimeMillis());
					}
				}

				// 保存
				File imageFile = null;
				try
				{
					imageFile = RKCloudChatImageTools.compressBitmap(newImageMap, tempImgName);
				}
				catch (IOException e)
				{
					Log.w(TAG, "Save resize image failed. info=" + e.getMessage());
				}

				// 回收Bitmap对象
				newImageMap.recycle();

				// 获取压缩后的图片路径
				String newImagePath = null != imageFile ? imageFile.getAbsolutePath() : null;
				if (TextUtils.isEmpty(newImagePath))
				{
					sendHandlerMsg(RKCloudChatUiHandlerMessage.IMAGE_COMPRESS_FAILED, chatId);
				}
				else
				{
					sendHandlerMsg(RKCloudChatUiHandlerMessage.IMAGE_COMPRESS_SUCCESS, chatId + "," + newImagePath);
				}
			}
		}).start();
	}

	// /////////////处理向通知栏中发送通知、以及取消通知栏中的内容 begin///////////////////////
	/**
	 * 取消通知栏中的通知
	 */
	public void cancelNotify(String chatId)
	{
		if (TextUtils.isEmpty(chatId))
		{
			for (String key : mNotificationChatIdCache.keySet())
			{
				mNotificationManager.cancel(TAG_NOTIFICATION_MMS, mNotificationChatIdCache.get(key));
			}

		}
		else
		{
			if (mNotificationChatIdCache.containsKey(chatId))
			{
				mNotificationManager.cancel(TAG_NOTIFICATION_MMS, mNotificationChatIdCache.get(chatId));
			}
		}
	}

	/*
	 * 发送通知
	 */
	private synchronized void notifyNewReceivedMsg(RKCloudChatBaseChat noticeChatObj, RKCloudChatBaseMessage noticeMsgObj)
	{
		// 如果当前GSM在通话中则返回
		TelephonyManager telManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
		if (telManager.getCallState() != TelephonyManager.CALL_STATE_IDLE)
		{
			return;
		}

		if (noticeMsgObj.getSender().equals(RKCloud.getUserName()))
		{
			return;
		}
		// 不具备通知能力时返回
		if (!RKCloudChatConfigManager.getInstance(mContext).getBoolean(RKCloudChatConfigManager.NEWMSG_SHOW_IN_NOTIFICATIONBAR))
		{
			return;
		}

		// 是否声音提醒
		boolean isPlayAudio = RKCloudChatConfigManager.getInstance(mContext).getBoolean(RKCloudChatConfigManager.NEWMSG_NOTICE_SOUND);
		// 是否振动提醒
		boolean isLibration = RKCloudChatConfigManager.getInstance(mContext).getBoolean(RKCloudChatConfigManager.NEWMSG_NOTICE_VIBRATION);
		// 结合系统对声音和振动的设置进行最终的声音提醒和振动提醒
		AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		switch (audioManager.getRingerMode())
		{
			case AudioManager.RINGER_MODE_NORMAL:
				break;

			case AudioManager.RINGER_MODE_VIBRATE:
				isPlayAudio = false;
				break;

			case AudioManager.RINGER_MODE_SILENT:
				isPlayAudio = false;
				isLibration = false;
				break;
		}

		// 把已通知的会话Id记录到缓存中，保证每个会话只有一个显示通知
		if (!mNotificationChatIdCache.containsKey(noticeChatObj.getChatId()))
		{
			mNotificationChatIdCache.put(noticeChatObj.getChatId(), mNotificationIdStart++);
		}
		// 通知id
		int notifyId = mNotificationChatIdCache.get(noticeChatObj.getChatId());

		// 设置点击notifacation 后跳转的activity
		// Intent intent = new Intent(mContext, RKCloudChatMsgActivity.class);
		// if(noticeChatObj instanceof SingleChat){
		// intent.putExtra(RKCloudChatMsgActivity.INTENT_KEY_MSGLIST_CHATID,
		// noticeChatObj.getChatId());
		// }else if(noticeChatObj instanceof GroupChat){
		// intent.putExtra(RKCloudChatMsgActivity.INTENT_KEY_MSGLIST_GROUPID,
		// noticeChatObj.getChatId());
		// }
		// TODO 跳转到主页面，统一入口
		Intent intent = new Intent(mContext, MainActivity.class);
		intent.setAction(MainActivity.ACTION_TO_MSGUI);
		if (noticeChatObj instanceof SingleChat)
		{
			intent.putExtra(MainActivity.ACTION_TO_MSGUI_PARAMS_SINGLECHATID, noticeChatObj.getChatId());
		}
		else if (noticeChatObj instanceof GroupChat)
		{
			intent.putExtra(MainActivity.ACTION_TO_MSGUI_PARAMS_GROUPCHATID, noticeChatObj.getChatId());
		}

		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(mContext, notifyId, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		// 构建通知中使用的信息
		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
		// 通知栏中显示的标题
		builder.setContentTitle(mContext.getString(R.string.rkcloud_chat_notify_unreadcnt_title));
		// 通知栏中显示的内容
		Class<? extends RKCloudChatBaseChat> chatClassObj = null;
		if (noticeChatObj instanceof SingleChat)
		{
			chatClassObj = SingleChat.class;
		}
		else
		{
			chatClassObj = GroupChat.class;
		}

		String content;
		if (RKCloudChatConfigManager.getInstance(mContext).getBoolean(RKCloudChatConfigManager.NEWMSG_NOTICE_MSGCONTENT))
		{
			content = getNotificationContent(chatClassObj, noticeMsgObj, RKCloudChatContactManager.getInstance(mContext).getContactInfo(noticeMsgObj.getSender()));
		}
		else
		{
			content = mContext.getString(R.string.rkcloud_chat_notify_content_close);
		}
		int showMaxLength = 60;// 通知栏中消息内容显示的最大长度，超出长度时进行截取
		if (content.length() > showMaxLength)
		{
			content = content.subSequence(0, showMaxLength) + "...";
		}
		CharSequence showContent = RKCloudChatTools.parseMsgFaceToAlias(mContext, content);
		builder.setContentText(showContent);
		// 通知栏中显示的icon图标
		builder.setSmallIcon(R.drawable.rkcloud_chat_ic_newmsg);
		// 通知栏中右下角显示的小图标
		// builder.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(),
		// R.drawable.rkcloud_chat_ic_newmsg));
		// 通知栏中点击后的响应处理
		builder.setContentIntent(pendingIntent);
		// 构建通知对象
		Notification notification = builder.build();
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		if (isPlayAudio)
		{
			notification.sound = RKCloudChatPlayAudioMsgTools.getInstance(mContext).getNotificationUri();
		}

		if (isLibration)
		{
			notification.defaults = Notification.DEFAULT_VIBRATE;// 振动
			notification.vibrate = RKCloudChatPlayAudioMsgTools.VIBRATE_PATTERN_NEW_MMS;
		}
		mNotificationManager.notify(TAG_NOTIFICATION_MMS, notifyId, notification);
	}

	/**
	 * 获取通知栏或消息列表新消息提示中显示的内容
	 */
	public String getNotificationContent(Class<? extends RKCloudChatBaseChat> chatClassObj, RKCloudChatBaseMessage msg, RKCloudChatContact contactObj)
	{
		// 显示的前缀信息
		String preStr = "";
		if (msg instanceof TipMessage || RKCloudChatConstants.FLAG_LOCAL_TIPMESSAGE.equals(msg.getExtension()))
		{
			// 提示型的消息无前缀
			preStr = "";
		}
		else
		{
			if (msg instanceof TextMessage && ((TextMessage) msg).isDraftMsg())
			{
				// 草稿型的文本消息时前缀变为:[草稿]
				preStr = mContext.getString(R.string.rkcloud_chat_draft) + " ";
			}
			else
			{
				if (!SingleChat.class.equals(chatClassObj))
				{
					if (msg.getSender().equalsIgnoreCase(RKCloud.getUserName()))
					{
						preStr = mContext.getString(R.string.rkcloud_chat_notify_me) + ":";

					}
					else
					{
						String showName = null != contactObj && !TextUtils.isEmpty(contactObj.getShowName()) ? contactObj.getShowName() : msg.getSender();
						preStr = showName + ":";
					}
				}
			}
		}
		// 显示的内容
		String contentStr = null;
		if (RKCloudChatConstants.FLAG_LOCAL_TIPMESSAGE.equals(msg.getExtension()))
		{
			contentStr = msg.getContent();
		}
		else if (RKCloudChatConstants.FLAG_AVCALL_IS_AUDIO.equals(msg.getExtension()))
		{
			contentStr = mContext.getString(R.string.rkcloud_chat_audiocall);
		}
		else if (RKCloudChatConstants.FLAG_AVCALL_IS_VIDEO.equals(msg.getExtension()))
		{
			contentStr = mContext.getString(R.string.rkcloud_chat_videocall);
		}
		else if (!TextUtils.isEmpty(msg.getExtension()) && msg.getExtension().startsWith(RKCloudChatConstants.FLAG_MEETING_MUTLIMEETING, 0))
		{
			contentStr = mContext.getString(R.string.rkcloud_chat_mutlimeeting);
		}
		else
		{
			if (msg instanceof TextMessage)
			{
				contentStr = msg.getContent();
			}
			else if (msg instanceof ImageMessage)
			{
				contentStr = mContext.getString(R.string.rkcloud_chat_notify_image);
			}
			else if (msg instanceof AudioMessage)
			{
				contentStr = mContext.getString(R.string.rkcloud_chat_notify_audio);
			}
			else if (msg instanceof VideoMessage)
			{
				contentStr = mContext.getString(R.string.rkcloud_chat_notify_video);
			}
			else if (msg instanceof FileMessage)
			{
				contentStr = mContext.getString(R.string.rkcloud_chat_notify_file);
			}
			else if (msg instanceof TipMessage)
			{
				contentStr = msg.getContent();
			}
			else if (msg instanceof LocalMessage)
			{
				// TODO 完善本地消息内容的显示
				contentStr = msg.getContent();

			}
			else if (msg instanceof CustomMessage)
			{
				// TODO 完善自定义消息内容的显示
				contentStr = msg.getContent();
			}
		}

		return preStr + contentStr;
	}

	/////////////// 处理向通知栏中发送通知、以及取消通知栏中的内容 end///////////////////////

	////////////// 与http模块的交互 begin//////////////////////////
	/**
	 * 显示群名称窗口
	 * 
	 * @param activity
	 * @param accounts
	 */
	public void showGroupNameDialog(final RKCloudChatBaseActivity activity, final List<String> accounts)
	{
		final EditText groupNameET = new EditText(activity);
		groupNameET.setBackgroundResource(R.drawable.rkcloud_chat_edittext_bg);
		groupNameET.setSingleLine();
		groupNameET.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		InputFilter filter = new InputFilter.LengthFilter(30);
		groupNameET.setFilters(new InputFilter[] { filter });
		groupNameET.setText(R.string.rkcloud_chat_creategroupname_tempname);
		groupNameET.setHint(R.string.rkcloud_chat_creategroupname_hint);
		groupNameET.setCursorVisible(true);
		groupNameET.setSelected(true);
		groupNameET.setSelection(groupNameET.getText().toString().trim().length());

		final RKCloudChatCustomDialog.Builder dialog = new RKCloudChatCustomDialog.Builder(activity);
		dialog.setTitle(R.string.rkcloud_chat_creategroupname_title);
		dialog.setNegativeButton(R.string.rkcloud_chat_btn_cancel, null);
		dialog.setPositiveButton(R.string.rkcloud_chat_btn_confirm, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				activity.showProgressDialog();
				createGroup(accounts, groupNameET.getText().toString().trim());
			}
		});

		dialog.addContentView(groupNameET);
		dialog.create().show();
		// 设置输入框内容改变事件
		groupNameET.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
			{
				dialog.setPositiveButtonEnabled(TextUtils.isEmpty(arg0.toString().trim()) ? false : true);
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
			{
			}

			@Override
			public void afterTextChanged(Editable arg0)
			{
			}
		});
	}

	/**
	 * 创建群
	 * 
	 * @param accounts
	 * @param groupName
	 */
	public void createGroup(List<String> accounts, String groupName)
	{
		if (null == mChatManager)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_APPLY_GROUP, RKCloudChatErrorCode.RK_SDK_UNINIT);
			return;
		}
		mChatManager.applyGroup(accounts, groupName, new RKCloudChatRequestCallBack()
		{
			@Override
			public void onSuccess(Object results)
			{
				String chatId = (String) results;
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_APPLY_GROUP, 0, chatId);
			}

			@Override
			public void onFailed(int errorCode, Object obj)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_APPLY_GROUP, errorCode, obj);
			}

			@Override
			public void onProgress(int value)
			{
			}
		});
	}

	/**
	 * 邀请成员
	 * 
	 * @param chatId
	 * @param users
	 */
	public void inviteUsers(final String chatId, List<String> users)
	{
		if (null == mChatManager)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_INVITE_USERS, RKCloudChatErrorCode.RK_SDK_UNINIT);
			return;
		}
		mChatManager.inviteUsers(chatId, users, new RKCloudChatRequestCallBack()
		{
			@Override
			public void onSuccess(Object results)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_INVITE_USERS, 0, chatId);
			}

			@Override
			public void onFailed(int errorCode, Object obj)
			{
				Message msg = Message.obtain();
				msg.what = RKCloudChatUiHandlerMessage.RESPONSE_INVITE_USERS;
				msg.arg1 = errorCode;
				msg.obj = obj;
				msg.getData().putString(KEY_CONVERSATION_ID, chatId);
				sendHandlerMsg(msg);
			}

			@Override
			public void onProgress(int value)
			{
			}
		});
	}

	/**
	 * 退出群
	 * 
	 * @param chatId
	 */
	public void quitGroup(final String chatId)
	{
		if (null == mChatManager)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_QUIT_GROUP, RKCloudChatErrorCode.RK_SDK_UNINIT, chatId);
			return;
		}
		mChatManager.quitGroup(chatId, new RKCloudChatRequestCallBack()
		{
			@Override
			public void onSuccess(Object results)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_QUIT_GROUP, 0, chatId);
				// 多人语音正在进行时，群主解散群，执行解散群操作操作成功之后，群主自己退出群，挂断多人语音；
				// 多人语音正在进行时，参与者进行退出群操作，退出群操作成功之后，挂断多人语音；
				if (null != mMeetingDemoManager)
				{
					String mGroupId = mMeetingDemoManager.getMeetingExtensionInfo();
					if (!TextUtils.isEmpty(mGroupId))
					{
						if (chatId.equals(mGroupId))
						{
							mMeetingDemoManager.handup();
						}
					}
				}
			}

			@Override
			public void onFailed(int errorCode, Object obj)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_QUIT_GROUP, errorCode, chatId);
			}

			@Override
			public void onProgress(int value)
			{
			}
		});
	}

	/**
	 * 修改邀请权限
	 * 
	 * @param chatId
	 * @param open
	 */
	public void modifyGroupInviteAuth(final String chatId, final boolean open)
	{
		if (null == mChatManager)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_MODIFY_GROUP_INVITEAUTH, RKCloudChatErrorCode.RK_SDK_UNINIT);
			return;
		}
		mChatManager.modifyGroupInviteAuth(chatId, open, new RKCloudChatRequestCallBack()
		{
			@Override
			public void onSuccess(Object results)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_MODIFY_GROUP_INVITEAUTH, 0, chatId);
			}

			@Override
			public void onFailed(int errorCode, Object obj)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_MODIFY_GROUP_INVITEAUTH, errorCode);
			}

			@Override
			public void onProgress(int value)
			{
			}
		});
	}

	/**
	 * @function 修改群名称
	 * @param groupId
	 *            String 群ID
	 * @param name
	 *            String 群名称
	 */
	public void modifyGroupName(String name, String groupId)
	{
		if (null == mChatManager)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.CALLBACK_MODIFY_GROUP_NAME, RKCloudChatErrorCode.RK_SDK_UNINIT);
			return;
		}
		mChatManager.modifyGroupName(name, groupId, new RKCloudChatRequestCallBack()
		{
			@Override
			public void onSuccess(Object results)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.CALLBACK_MODIFY_GROUP_NAME, RKCloudChatErrorCode.RK_SUCCESS, results);
			}

			@Override
			public void onProgress(int value)
			{
			}

			@Override
			public void onFailed(int errorCode, Object object)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.CALLBACK_MODIFY_GROUP_NAME, errorCode);
			}
		});
	}

	/**
	 * @function 修改群名称
	 * @param groupId
	 *            String 群ID
	 * @param desc
	 *            String 群备注
	 */
	public void modifyGroupDescription(String desc, String groupId)
	{
		if (null == mChatManager)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.CALLBACK_MODIFY_GROUP_DESC, RKCloudChatErrorCode.RK_SDK_UNINIT);
			return;
		}
		mChatManager.modifyGroupDescription(desc, groupId, new RKCloudChatRequestCallBack()
		{
			@Override
			public void onSuccess(Object results)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.CALLBACK_MODIFY_GROUP_DESC, RKCloudChatErrorCode.RK_SUCCESS, results);
			}

			@Override
			public void onProgress(int value)
			{
			}

			@Override
			public void onFailed(int errorCode, Object object)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.CALLBACK_MODIFY_GROUP_DESC, errorCode);
			}
		});
	}

	/**
	 * 踢出某位参与者
	 * 
	 * @param chatId
	 * @param user
	 */
	public void kickUserFromGroup(final String chatId, String user)
	{
		if (null == mChatManager)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_KICKUSER, RKCloudChatErrorCode.RK_SDK_UNINIT);
			return;
		}
		mChatManager.kickUser(chatId, user, new RKCloudChatRequestCallBack()
		{
			@Override
			public void onSuccess(Object results)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_KICKUSER, 0);
			}

			@Override
			public void onFailed(int errorCode, Object obj)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_KICKUSER, errorCode);
			}

			@Override
			public void onProgress(int value)
			{
			}
		});
	}

	/**
	 * 下载缩略图
	 */
	public void downThumbImage(final String msgSerialNum)
	{
		mChatManager.downThumbImage(msgSerialNum, new RKCloudChatRequestCallBack()
		{
			@Override
			public void onSuccess(Object results)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_THUMBIMAGE_DOWNED, msgSerialNum);
			}

			@Override
			public void onFailed(int errorCode, Object obj)
			{
			}

			@Override
			public void onProgress(int value)
			{
			}
		});
	}

	/**
	 * 下载媒体消息
	 */
	public void downAttach(final String msgSerialNum)
	{
		if (null == mChatManager)
		{
			return;
		}
		mChatManager.downMediaFile(msgSerialNum, new RKCloudChatRequestCallBack()
		{
			@Override
			public void onSuccess(Object results)
			{
				mRecordDownMms.remove(msgSerialNum);
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_MEDIAFILE_DOWNED, 0, msgSerialNum);
			}

			@Override
			public void onFailed(int opCode, Object obj)
			{
				mRecordDownMms.remove(msgSerialNum);
				sendHandlerMsg(RKCloudChatUiHandlerMessage.MSG_STATUS_HAS_CHANGED, opCode, msgSerialNum);
			}

			@Override
			public void onProgress(int value)
			{
				sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_UPDATE_DOWNING_PROGRESS, value, msgSerialNum);
			}
		});

		mRecordDownMms.put(msgSerialNum, System.currentTimeMillis());
	}

	/**
	 * 通知对端消息已读
	 * 
	 * @param msgObj
	 */
	public void notifyOtherMsgHasReaded(RKCloudChatBaseMessage msgObj)
	{
		if (null == msgObj)
		{
			return;
		}
		mChatManager.sendReadedReceipt(msgObj);
	}

	/**
	 * 通知该会话的消息已读
	 *
	 * @param chatId
	 */
	public void notifyOtherDeviceMsgHasReaded(String chatId)
	{
		if (TextUtils.isEmpty(chatId))
		{
			return;
		}
		mChatManager.clearOtherPlatformNewMMSCounts(chatId);
	}

	/**
	 * 获取媒体消息下载的时间
	 * 
	 * @return
	 */
	public long getMmsDownTime(String msgSerialNum)
	{
		return mRecordDownMms.containsKey(mRecordDownMms) ? mRecordDownMms.get(msgSerialNum) : 0;
	}

	////////////// 与http模块的交互 end////////////////////////////
	/*
	 * 分隔字符串
	 */
	public static List<String> splitString(String content)
	{
		List<String> results = null;
		if (!TextUtils.isEmpty(content))
		{
			String[] arr = content.split(",");
			results = new ArrayList<String>(arr.length);
			for (String str : arr)
			{
				results.add(str);
			}
		}
		else
		{
			results = new ArrayList<String>();
		}
		return results;
	}

	@Override
	public void onAllGroupInfoSynComplete()
	{
		sendHandlerMsg(RKCloudChatUiHandlerMessage.CALLBACK_ALL_GROUP_INFO_COMPLETE, "");
	}

	@Override
	public void onGroupCreated(String groupId)
	{
		sendHandlerMsg(RKCloudChatUiHandlerMessage.CALLBACK_GROUP_INFO_CHANGED, groupId);
	}

	@Override
	public void onGroupInfoChanged(String groupId)
	{
		sendHandlerMsg(RKCloudChatUiHandlerMessage.CALLBACK_GROUP_INFO_CHANGED, groupId);
	}

	@Override
	public void onGroupInfoChanged(final String groupId, int type)
	{
		if (RKCloudChatBaseChat.CHANGE_TYPE_GROUP_NAME == type)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.CALLBACK_MODIFY_GROUP_NAME, groupId);
		}

		if (RKCloudChatBaseChat.CHANGE_TYPE_GROUP_DESCRIPTION == type)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.CALLBACK_MODIFY_GROUP_DESC, groupId);
		}

		if (RKCloudChatBaseChat.CHANGE_TYPE_GROUP_TRANSFER == type)
		{
			GroupChat groupChat = (GroupChat) queryChat(groupId);
			if (null != groupChat)
			{
				String createAccount = groupChat.getGroupCreater();
                RKCloudChatContact rkCloudChatContact = RKCloudChatContactManager.getInstance(mContext).getContactInfo(createAccount);
				LocalMessage localMessage = LocalMessage.buildSendMsg(groupId, String.format(mContext.getString(R.string.rkcloud_chat_manage_transfer_group_tip_other), null == rkCloudChatContact ?createAccount : rkCloudChatContact.getShowName()), groupId);
				localMessage.setExtension(RKCloudChatConstants.FLAG_LOCAL_TIPMESSAGE);
				addLocalMsg(localMessage, GroupChat.class);
				sendHandlerMsg(RKCloudChatUiHandlerMessage.CALLBACK_GROUP_INFO_CHANGED, groupId);
			}
		}

		if (RKCloudChatBaseChat.CHANGE_TYPE_GROUP_POPULATION == type)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_GROUP_POPULATION_CHANGED, groupId);
		}

		if (RKCloudChatBaseChat.CHANGE_TYPE_GROUP_JOIN_AUTHORITY == type)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_MODIFY_GROUP_INVITEAUTH, groupId);
		}

		if (RKCloudChatBaseChat.CHANGE_TYPE_GROUP_INFO == type)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.CALLBACK_GROUP_INFO_CHANGED, groupId);
		}
	}

	@Override
	public void onGroupRemoved(String chatId, int removeType)
	{
		cancelNotify(chatId);
		updateUnreadMsgCountsInMain();
		if (1 == removeType)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.RESPONSE_QUIT_GROUP, chatId);

		}
		if (2 == removeType)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.CALLBACK_KICKOUT, chatId);

		}
		else if (3 == removeType)
		{
			sendHandlerMsg(RKCloudChatUiHandlerMessage.CALLBACK_GROUP_DISSOLVED, chatId);
		}

		// 多人语音正在进行时，群主解散群操作成功之后，群成员会收到群解散的通知，回调到当前方法，群成员挂断多人语音；
		// 多人语音正在进行时，参与者收到被踢出群的通知，回调到当前方法，被踢成员挂断多人语音；
		if (null != mMeetingDemoManager)
		{
			String mGroupId = mMeetingDemoManager.getMeetingExtensionInfo();
			if (!TextUtils.isEmpty(mGroupId))
			{
				if (chatId.equals(mGroupId))
				{
					mMeetingDemoManager.handup();
				}
			}
		}
	}

	@Override
	public void onGroupUsersChanged(String chatId)
	{
		sendHandlerMsg(RKCloudChatUiHandlerMessage.CALLBACK_GROUP_USERS_CHANGED, chatId);
	}

	@Override
	public void onReceivedMsgs(Map<RKCloudChatBaseChat, List<RKCloudChatBaseMessage>> msgDatas)
	{
		if (null == msgDatas || 0 == msgDatas.size())
		{
			return;
		}

		List<String> syncContactDatas = new ArrayList<String>();
		List<RKCloudChatBaseMessage> datas = null;
		List<RKCloudChatBaseMessage> handlerMsgObj = null;// handler返回的对象
		for (RKCloudChatBaseChat chatObj : msgDatas.keySet())
		{
			datas = msgDatas.get(chatObj);
			if (chatObj.getChatId().equalsIgnoreCase(mUnNeedSendNotifyChatId))
			{
				if (null != datas && datas.size() > 0)
				{
					handlerMsgObj = datas;
				}

			}
			else
			{
				if (chatObj.getRemindStatus() && null != datas && datas.size() > 0)
				{
					notifyNewReceivedMsg(chatObj, datas.get(datas.size() - 1));
				}
			}
			for (RKCloudChatBaseMessage msgObj : datas)
			{
				if (chatObj instanceof SingleChat && !msgObj.getSender().equals(RKCloud.getUserName()))
				{
					mChatManager.sendArrivedReceipt(msgObj);
				}

				if (!syncContactDatas.contains(msgObj.getSender()))
				{
					syncContactDatas.add(msgObj.getSender());
				}
				if ((!chatObj.getChatId().equalsIgnoreCase(mUnNeedSendNotifyChatId) && chatObj.getRemindStatus() && null != datas && datas.size() > 0) || isHasAtMeMsg(chatObj,msgObj))
				{
					notifyNewReceivedMsg(chatObj, datas.get(datas.size() - 1));
				}

				pareAtMeMsg(chatObj, msgObj);
			}
		}

		updateUnreadMsgCountsInMain();
		sendHandlerMsg(RKCloudChatUiHandlerMessage.CALLBACK_RECEIVED_MOREMMS, handlerMsgObj);

		// 同步用户信息
		RKCloudChatContactManager.getInstance(mContext).syncContactsInfo(syncContactDatas);
	}

	@Override
	public void onReceivedMsg(RKCloudChatBaseChat chatObj, RKCloudChatBaseMessage msgObj)
	{
		if (null == chatObj || null == msgObj)
		{
			return;
		}
		if (chatObj instanceof SingleChat && !msgObj.getSender().equals(RKCloud.getUserName()))
		{
			mChatManager.sendArrivedReceipt(msgObj);
		}
		// 发送通知
		if ((!chatObj.getChatId().equalsIgnoreCase(mUnNeedSendNotifyChatId) && chatObj.getRemindStatus()) || isHasAtMeMsg(chatObj,msgObj))
		{
			notifyNewReceivedMsg(chatObj, msgObj);
		}
		pareAtMeMsg(chatObj, msgObj);
		updateUnreadMsgCountsInMain();
		sendHandlerMsg(RKCloudChatUiHandlerMessage.CALLBACK_RECEIVED_MMS, msgObj);
		// 同步用户信息
		RKCloudChatContactManager.getInstance(mContext).syncContactInfo(msgObj.getSender());
	}

	/**
	 * 是否包含 @我的消息
	 * @param msgObj
	 * @return true:包含；false:不包含.
	 */
	private boolean isHasAtMeMsg(RKCloudChatBaseChat chatObj ,RKCloudChatBaseMessage msgObj)
	{
		boolean result = false;
		if(chatObj instanceof SingleChat)
		{
            return false;
        }
		if(msgObj instanceof TextMessage)
		{
			TextMessage msg = (TextMessage)msgObj;
			if(TextUtils.isEmpty(msg.getAtUser()))
			{
				result = false;
			}
			else
			{
				if(msg.getAtUser().contains(RKCloudChatConstants.KEY_GROUP_ALL))
				{
					result = true;
				}
				else
				{
					try
					{
						JSONArray array = new JSONArray(msg.getAtUser());
						String currAccount = RKCloud.getUserName();
						/**
						 * 是为了避免群主设置了消息不提醒， 群主自己@所有成员之后,群主还出现提示音、通知栏的问题”
						 */
						if (!msgObj.getSender().equals(currAccount))
						{
							for (int i = 0; i < array.length(); i++)
							{
								if (array.get(i).equals(currAccount))
								{
									result = true;
									break;
								}
							}
						}
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		else
		{
			result = false;
		}
		return result;
	}

	/**
	 * 文本消息 包含@成员的处理
	 * 
	 * @param chatObj
	 * @param msgObj
	 */
	private void pareAtMeMsg(RKCloudChatBaseChat chatObj, RKCloudChatBaseMessage msgObj)
	{
		if (chatObj instanceof GroupChat && msgObj instanceof TextMessage)
		{
			TextMessage msg = (TextMessage) msgObj;
			if (!TextUtils.isEmpty(msg.getAtUser()))
			{
				// @ all的处理
				if (msg.getAtUser().contains(RKCloudChatConstants.KEY_GROUP_ALL))
				{
					RKCloudDemo.config.put(chatObj.getChatId(), msg.getMsgSerialNum());
				}
				else
				{
					// @ 某些成员的处理
					try
					{
						JSONArray array = new JSONArray(msg.getAtUser());
						String currAccount = RKCloud.getUserName();
						/**
						 * 是为了避免 群主自己@所有成员之后，群主自己还显示“[有人@我]”
						 */
						if (!msgObj.getSender().equals(currAccount))
						{
							for (int i = 0; i < array.length(); i++)
							{
								if (array.get(i).equals(currAccount))
								{
									// 获取到服务器返回的 被@成员数据包含自己
									RKCloudDemo.config.put(chatObj.getChatId(), msg.getMsgSerialNum());
									break;
								}
							}
						}
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
	}

	@Override
	public void onMsgHasChanged(String msgSerialNum)
	{
		updateUnreadMsgCountsInMain();
		sendHandlerMsg(RKCloudChatUiHandlerMessage.MSG_STATUS_HAS_CHANGED, msgSerialNum);
		RKCloudChatBaseMessage msg = mChatManager.queryChatMsg(msgSerialNum);
		if(RKCloudDemo.config.getString(msg.getChatId(),"").equals(msgSerialNum))
		{
			RKCloudDemo.config.remove(msg.getChatId());
		}
	}

	/**
	 * 检查是否包含群成员
	 * 
	 * @param content
	 * @return
	 */
	public boolean containsAtUsername(String content, List<String> accountList)
	{
		if (TextUtils.isEmpty(content))
		{
			return false;
		}
		if (null == accountList || accountList.size() == 0)
		{
			return false;
		}
		for (String account : accountList)
		{
			String nick = account;// 用户账号
			RKCloudChatContact conactInfo = RKCloudChatContactManager.getInstance(mContext).getContactInfo(account);
			if (null != conactInfo)
			{
				nick = RKCloudChatContactManager.getInstance(mContext).getContactName(account);// 用户名称
			}
			if (content.contains(nick))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * 检查是否是所有群成员
	 * 
	 * @param content
	 * @return
	 */
	public boolean containsAtAll(String content)
	{
		String atAll = "@" + mContext.getString(R.string.rkcloud_chat_all_members);
		if (content.contains(atAll))
		{
			return true;
		}
		return false;
	}

	/**
	 * 检查所有有效的@用户 为了避免 @123456（应该是@16）这种情况出现
	 * 
	 * @param content
	 * @return
	 */
	public List<String> getAtMessageUsernames(String content, List<String> accountList)
	{
		if (TextUtils.isEmpty(content))
		{
			return null;
		}

		List<String> list = null;
		for (String account : accountList)
		{
			String nick = account;
			RKCloudChatContact conactInfo = RKCloudChatContactManager.getInstance(mContext).getContactInfo(account);
			if (null != conactInfo)
			{
				nick = RKCloudChatContactManager.getInstance(mContext).getContactName(account);
			}
			if (content.contains(nick))
			{
				if (list == null)
				{
					list = new ArrayList<String>();
				}
				list.add(account);
			}
		}
		return list;
	}
}
