package com.rongkecloud.chat.demo.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.text.*;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.rongkecloud.chat.*;
import com.rongkecloud.chat.demo.RKCloudChatConstants;
import com.rongkecloud.chat.demo.RKCloudChatContactManager;
import com.rongkecloud.chat.demo.RKCloudChatMmsManager;
import com.rongkecloud.chat.demo.RKCloudChatUiHandlerMessage;
import com.rongkecloud.chat.demo.entity.RKCloudChatContact;
import com.rongkecloud.chat.demo.tools.RKCloudChatTools;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageAsyncLoader;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageAsyncLoader.ImageLoadedCompleteDelayNotify;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest.IMAGE_REQUEST_TYPE;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageResult;
import com.rongkecloud.test.R;
import com.rongkecloud.test.ui.widget.RoundedImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RKCloudChatSearchChatListActivity extends RKCloudChatBaseActivity implements ImageLoadedCompleteDelayNotify
{
	// 查询类型
	private static final int QUERY_TYPE_GET_CONTACTS = 1;// 查询联系人信息
	private static final int QUERY_TYPE_QUERY_CHATS = 2;// 搜索会话
	private static final int QUERY_TYPE_QUERY_CHATS_NAME = 3;// 搜索会话名称

	// UI组件
	private EditText mSearchET;
	private ListView mListView;
	private TextView mDescWhiteTV;

	// 成员变量
	private RKCloudChatMmsManager mMmsManager;
	private RKCloudChatContactManager mContactManager;

	// 整体数据
	private RKCloudChatSearchListAdapter mAdapter;// 会话适配器
	private List<Object> mAllDatas;// 所有数据源

	// 搜索会话名称内容
	private List<RKCloudChatBaseChat> mAllChatData;// 会话列表所有数据
	private List<HashMap<RKCloudChatBaseChat, ArrayList<RKCloudChatBaseMessage>>> mSearchMsgChatData;// 会话列表显示的数据(根据关键字搜索消息搜出的会话)
	private List<RKCloudChatBaseChat> mSearchNameDatas;// 会话列表显示的数据(根据关键字搜索会话名称搜出的会话)

	private List<String> mContactAccounts;
	private Map<String, RKCloudChatContact> mContacts;
	private QueryHandlerThread mQuerThread;// 查询数据的线程
	private BackgroundColorSpan backgroundColorSpan;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rkcloud_chat_searchchatlist);

		initUIAndListener();
		// 初始化成员变量
		mMmsManager = RKCloudChatMmsManager.getInstance(this);
		mContactManager = RKCloudChatContactManager.getInstance(this);

		mAllChatData = mMmsManager.queryAllChats();
		mAllDatas = new ArrayList();
		mSearchMsgChatData = new ArrayList<>();
		mSearchNameDatas = new ArrayList<>();

		mContacts = new HashMap();
		mContactAccounts = new ArrayList();
		mAdapter = new RKCloudChatSearchListAdapter();
		mListView.setAdapter(mAdapter);
		backgroundColorSpan = new BackgroundColorSpan(getResources().getColor(R.color.rkcloud_chat_search_result_highlightcolor));
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mMmsManager.bindUiHandler(mUiHandler);
		mContactManager.bindUiHandler(mUiHandler);
		RKCloudChatImageAsyncLoader.getInstance(this).registerDelayListener(this);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		closeContextMenu();
		closeProgressDialog();
	}

	@Override
	protected void onDestroy()
	{
		if (mQuerThread != null)
		{
			mQuerThread.quit();
			mQuerThread = null;
		}
		super.onDestroy();
	}

	@Override
	public void onLoadImageCompleteDelayNotify(IMAGE_REQUEST_TYPE type, List<RKCloudChatImageResult> result)
	{
		if (IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG == type)
		{
			mAdapter.notifyDataSetChanged();
		}
	}

	private void initUIAndListener()
	{
		// 设置title
		TextView titleTV = (TextView) findViewById(R.id.txt_title);
		titleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		titleTV.setText(R.string.bnt_return);
		titleTV.setOnClickListener(mExitListener);

		TextView text_title_content = (TextView) findViewById(R.id.text_title_content);
		text_title_content.setText(R.string.rkcloud_chat_search_chatlist_title);

		// 初始化UI元素
		mSearchET = (EditText) findViewById(R.id.searchedittext);
		mListView = (ListView) findViewById(R.id.listview);
		mDescWhiteTV = (TextView) findViewById(R.id.chatlist_desc_white);
		mSearchET.setFocusableInTouchMode(true);
		mSearchET.requestFocus();
		mSearchET.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
			}

			@Override
			public void afterTextChanged(Editable s)
			{
				startQuery(QUERY_TYPE_QUERY_CHATS);
				startQuery(QUERY_TYPE_QUERY_CHATS_NAME);
			}
		});
		mListView.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				Object obj = mAllDatas.get(arg2);
				if (obj instanceof RKCloudChatBaseChat)
				{
					Intent intent = new Intent(RKCloudChatSearchChatListActivity.this, RKCloudChatMsgActivity.class);
					if (null == obj || obj instanceof SingleChat)
					{
						intent.putExtra(RKCloudChatMsgActivity.INTENT_KEY_MSGLIST_CHATID, ((RKCloudChatBaseChat) obj).getChatId());
					}
					else if (obj instanceof GroupChat)
					{
						intent.putExtra(RKCloudChatMsgActivity.INTENT_KEY_MSGLIST_GROUPID, ((RKCloudChatBaseChat) obj).getChatId());
					}

					RKCloudChatSearchChatListActivity.this.startActivity(intent);
				}
				else
				{
					HashMap<RKCloudChatBaseChat, ArrayList<RKCloudChatBaseMessage>> chatMap = (HashMap<RKCloudChatBaseChat, ArrayList<RKCloudChatBaseMessage>>) obj;
					for (RKCloudChatBaseChat chat : chatMap.keySet())
					{
						if (1 == chatMap.get(chat).size())
						{
							Intent intent1 = new Intent(RKCloudChatSearchChatListActivity.this, RKCloudChatMsgActivity.class);
							if (chat instanceof SingleChat)
							{
								intent1.putExtra(RKCloudChatMsgActivity.INTENT_KEY_MSGLIST_CHATID, chat.getChatId());
							}
							else if (chat instanceof GroupChat)
							{
								intent1.putExtra(RKCloudChatMsgActivity.INTENT_KEY_MSGLIST_GROUPID, chat.getChatId());
							}
							intent1.putExtra(RKCloudChatMsgActivity.INTENT_KEY_MSGLIST_MSGID, chatMap.get(chat).get(0).getMsgSerialNum());
							intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							RKCloudChatSearchChatListActivity.this.startActivity(intent1);
						}
						else
						{
							Intent intent = new Intent(RKCloudChatSearchChatListActivity.this, RKCloudChatSearchMessageListActivity.class);
							intent.putExtra(RKCloudChatSearchMessageListActivity.INTENT_KEY_FILTER, mSearchET.getText().toString().trim());
							intent.putExtra(RKCloudChatSearchMessageListActivity.INTENT_KEY_CHAT_DATA, chat);
							intent.putExtra(RKCloudChatSearchMessageListActivity.INTENT_KEY_MESSAGE_DATA, chatMap.get(chat));
							startActivity(intent);
						}
					}
				}
			}
		});
	}

	/**
	 * 查询数据
	 */
	private void startQuery(int queryType)
	{
		if (null == mQuerThread)
		{
			mQuerThread = new QueryHandlerThread("QueryChatSearchMessageListActivityThread");
			mQuerThread.start();
		}
		mQuerThread.startQuery(queryType);
	}

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
				Message msg = mQuerHandler.obtainMessage();
				msg.what = queryType;
				if (QUERY_TYPE_GET_CONTACTS == queryType)
				{
					List<String> accounts = new ArrayList<>();
					if (mContactAccounts.size() > 0)
					{
						accounts.addAll(mContactAccounts);
					}
					msg.obj = accounts;

				}
				else if (QUERY_TYPE_QUERY_CHATS == queryType)
				{
					List<HashMap<RKCloudChatBaseChat, List<RKCloudChatBaseMessage>>> chatDatas;
					String filter = mSearchET.getText().toString().trim();
					chatDatas = mMmsManager.queryMessageKeyWord(filter);
					msg.obj = chatDatas;

					for (HashMap<RKCloudChatBaseChat, List<RKCloudChatBaseMessage>> obj : chatDatas)
					{
						for (RKCloudChatBaseChat chat : obj.keySet())
						{
							if (null != chat.getLastMsgObj() && null != chat.getLastMsgObj().getSender() && !mContactAccounts.contains(chat.getLastMsgObj().getSender()))
							{
								mContactAccounts.add(chat.getLastMsgObj().getSender());
							}
							if (chat instanceof SingleChat && !mContactAccounts.contains(chat.getChatId()))
							{
								mContactAccounts.add(chat.getChatId());
							}
						}
					}
					startQuery(QUERY_TYPE_GET_CONTACTS);
				}

				msg.sendToTarget();
			}
		}

		@Override
		public boolean handleMessage(Message msg)
		{
			if (QUERY_TYPE_GET_CONTACTS == msg.what)
			{
				List<String> accounts = (List<String>) msg.obj;
				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.GET_CONTACTSINFO_FINISHED;
				message.obj = mContactManager.getContactInfos(accounts);
				message.sendToTarget();

			}
			else if (QUERY_TYPE_QUERY_CHATS == msg.what)
			{
				List<HashMap<RKCloudChatBaseChat, List<RKCloudChatBaseMessage>>> chats = (List<HashMap<RKCloudChatBaseChat, List<RKCloudChatBaseMessage>>>) msg.obj;
				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.QUERY_ALLCHATS_INFO_FINISHED;
				message.obj = chats;
				message.sendToTarget();

			}
			else if (QUERY_TYPE_QUERY_CHATS_NAME == msg.what)
			{
				String filter = mSearchET.getText().toString().trim();
				List<RKCloudChatBaseChat> datas = new ArrayList<RKCloudChatBaseChat>();
				if (!TextUtils.isEmpty(filter))
				{
					for (RKCloudChatBaseChat obj : mAllChatData)
					{
						if (obj instanceof SingleChat)
						{
							RKCloudChatContact contactObj = (RKCloudChatContact) mContacts.get(obj.getChatId());
							((SingleChat) obj).setContactShowName(null != contactObj ? contactObj.getShowName() : null);
						}

						obj.matchName(filter, backgroundColorSpan);
						if (null != obj.highLightName)
						{
							datas.add(obj);
						}
					}

				}

				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.QUERY_ALLCHATS_NAME_FINISHED;
				message.obj = datas;
				message.sendToTarget();

			}

			return true;
		}
	}

	@Override
	public void processResult(Message msg)
	{
		if (RKCloudChatUiHandlerMessage.GET_CONTACTSINFO_FINISHED == msg.what)
		{ // 联系人信息获取完成
			mContacts.clear();
			Map<String, RKCloudChatContact> datas = (Map<String, RKCloudChatContact>) msg.obj;
			if (null != datas && datas.size() > 0)
			{
				mContacts.putAll(datas);
			}
		}
		else if (RKCloudChatUiHandlerMessage.QUERY_ALLCHATS_INFO_FINISHED == msg.what)
		{ // 会话信息搜索完成
			List<HashMap<RKCloudChatBaseChat, ArrayList<RKCloudChatBaseMessage>>> chatList = (List<HashMap<RKCloudChatBaseChat, ArrayList<RKCloudChatBaseMessage>>>) msg.obj;
			mSearchMsgChatData.clear();
			mSearchMsgChatData.addAll(chatList);
		}
		else if (RKCloudChatUiHandlerMessage.QUERY_ALLCHATS_NAME_FINISHED == msg.what)
		{
			// 会话信息搜索会话名称完成
			List<RKCloudChatBaseChat> chatList = (List<RKCloudChatBaseChat>) msg.obj;
			mSearchNameDatas.clear();
			mSearchNameDatas.addAll(chatList);

			mAllDatas.clear();
			mAllDatas.addAll(mSearchMsgChatData);
			mAllDatas.addAll(mSearchNameDatas);
			mAdapter.notifyDataSetChanged();
			if (0 == mAllDatas.size())
			{
				mDescWhiteTV.setVisibility(View.GONE);
			}
			else
			{
				mDescWhiteTV.setVisibility(View.VISIBLE);
			}
		}
		else if (RKCloudChatUiHandlerMessage.CONTACTSINFO_CHANGED == msg.what)
		{ // 联系人信息有变化
			List<String> accounts = (List<String>) msg.obj;
			if (null != accounts && accounts.size() > 0)
			{
				boolean needReGet = false;
				for (String account : mContactAccounts)
				{
					if (accounts.contains(account))
					{
						needReGet = true;
						break;
					}
				}
				if (needReGet)
				{
					startQuery(QUERY_TYPE_GET_CONTACTS);
				}
			}

		}
		else if (RKCloudChatUiHandlerMessage.CONTACT_HEADERIMAGE_CHANGED == msg.what)
		{ // 联系人头像有变化
			if (mContactAccounts.contains((String) msg.obj))
			{
				startQuery(QUERY_TYPE_GET_CONTACTS);
			}
		}
	}

	private class RKCloudChatSearchListAdapter extends BaseAdapter
	{
		private Context mAdapterContext;

		public RKCloudChatSearchListAdapter()
		{
			mAdapterContext = RKCloudChatSearchChatListActivity.this;
		}

		/*
		 * 定义每个条目中使用View的基本元素
		 */
		private class ItemViewBuffer
		{
			TextView chatListDesc;// 会话描述信息显示部分
			TextView lineTV;// 分割线
			RoundedImageView headerPhotoView; // 会话头像
			TextView convNameTV; // 会话名称
			TextView unReadMsgCntTV;// 未读消息条数
			TextView lastMsgContentTV;// 最后一条消息的内容
			TextView lastmsgDateTV;// 最后一条消息日期
			TextView lastmsgTimeTV;// 最后一条消息时间
			TextView msgFailedTV; // 消息发送失败时的显示内容

			public ItemViewBuffer(View convertView)
			{
				chatListDesc = (TextView) convertView.findViewById(R.id.chatlist_desc);
				lineTV = (TextView) convertView.findViewById(R.id.line);
				headerPhotoView = (RoundedImageView) convertView.findViewById(R.id.headerphoto);
				convNameTV = (TextView) convertView.findViewById(R.id.name);
				unReadMsgCntTV = (TextView) convertView.findViewById(R.id.txt_msg_count_unread);
				lastMsgContentTV = (TextView) convertView.findViewById(R.id.msgcontent);
				lastmsgDateTV = (TextView) convertView.findViewById(R.id.lastmsgdate);
				lastmsgTimeTV = (TextView) convertView.findViewById(R.id.lastmsgtime);
				msgFailedTV = (TextView) convertView.findViewById(R.id.msgfailed);
			}
		}

		@Override
		public int getCount()
		{
			return mAllDatas.size();
		}

		@Override
		public Object getItem(int arg0)
		{
			return mAllDatas.get(arg0);
		}

		@Override
		public long getItemId(int arg0)
		{
			return arg0;
		}

		@Override
		public View getView(int arg0, View convertView, ViewGroup arg2)
		{
			ItemViewBuffer mItemBuffer;
			if (null == convertView)
			{
				convertView = LayoutInflater.from(mAdapterContext).inflate(R.layout.rkcloud_chat_searchchatlist_item, null);
				mItemBuffer = new ItemViewBuffer(convertView);
				convertView.setTag(mItemBuffer);
			}
			else
			{
				mItemBuffer = (ItemViewBuffer) convertView.getTag();
			}

			Object obj = mAllDatas.get(arg0);
			if (0 == arg0)
			{
				if (obj instanceof RKCloudChatBaseChat)
				{
					mItemBuffer.chatListDesc.setText(getString(R.string.rkcloud_chat_chatlist_name_desc));
				}
				else
				{
					mItemBuffer.chatListDesc.setText(getString(R.string.rkcloud_chat_chatlist_desc));
				}
				mItemBuffer.chatListDesc.setVisibility(View.VISIBLE);
			}
			else
			{
				Object lastObj = mAllDatas.get(arg0 - 1);
				if (obj instanceof RKCloudChatBaseChat)
				{
					if (null != lastObj && lastObj instanceof RKCloudChatBaseChat)
					{
						mItemBuffer.chatListDesc.setVisibility(View.GONE);
					}
					else
					{
						mItemBuffer.chatListDesc.setText(getString(R.string.rkcloud_chat_chatlist_name_desc));
						mItemBuffer.chatListDesc.setVisibility(View.VISIBLE);
					}
				}
				else
				{
					mItemBuffer.chatListDesc.setVisibility(View.GONE);
				}
			}

			if (!(obj instanceof RKCloudChatBaseChat) && (arg0 != mAllDatas.size() - 1) && (mAllDatas.get(arg0 + 1) instanceof RKCloudChatBaseChat))
			{
				mItemBuffer.lineTV.setVisibility(View.GONE);
			}
			else
			{
				mItemBuffer.lineTV.setVisibility(View.VISIBLE);
			}

			if (obj instanceof RKCloudChatBaseChat)
			{
				showChatName((RKCloudChatBaseChat) obj, mItemBuffer);
			}
			else
			{
				showChatMsg((HashMap<RKCloudChatBaseChat, ArrayList<RKCloudChatBaseMessage>>) obj, mItemBuffer);
			}

			return convertView;
		}

		private void showChatMsg(HashMap<RKCloudChatBaseChat, ArrayList<RKCloudChatBaseMessage>> chatMap, ItemViewBuffer mItemBuffer)
		{
			for (RKCloudChatBaseChat chatObj : chatMap.keySet())
			{
				if (chatObj instanceof SingleChat)
				{ // 单聊
					RKCloudChatContact contactObj = mContacts.get(chatObj.getChatId());
					// 设置会话名称
					mItemBuffer.convNameTV.setText(null != contactObj ? contactObj.getShowName() : chatObj.getChatId());
					// 设置头像
					mItemBuffer.headerPhotoView.setImageResource(R.drawable.rkcloud_chat_img_header_default);
					if (null != contactObj && !TextUtils.isEmpty(contactObj.getHeaderThumbImagePath()))
					{
						RKCloudChatImageRequest imageReq = new RKCloudChatImageRequest(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG, contactObj.getHeaderThumbImagePath(), chatObj.getChatId());
						// 如果在缓存中则直接设置图片
						RKCloudChatImageResult imgResult = RKCloudChatImageAsyncLoader.getInstance(RKCloudChatSearchChatListActivity.this).sendPendingRequestQuryCache(imageReq);
						if (null != imgResult && null != imgResult.resource)
						{
							mItemBuffer.headerPhotoView.setImageDrawable(imgResult.resource);
						}
					}
				}
				else if (chatObj instanceof GroupChat)
				{ // 群聊
					// 设置会话名称
					mItemBuffer.convNameTV.setText(chatObj.getChatShowName());
					// 设置默认头像
					mItemBuffer.headerPhotoView.setImageResource(R.drawable.rkcloud_chat_img_header_mutlichat_default);
				}

				if (1 == chatMap.get(chatObj).size())
				{
					if (null != matchMsg(chatMap.get(chatObj).get(0).getContent()))
					{
						mItemBuffer.lastMsgContentTV.setText(matchMsg(chatMap.get(chatObj).get(0).getContent()));
					}
					else
					{
						mItemBuffer.lastMsgContentTV.setText(chatMap.get(chatObj).get(0).getContent());
					}
					// 设置最后一条消息时间的显示
					if (chatMap.get(chatObj).get(0).getCreatedTime() > 0)
					{
						String date = RKCloudChatTools.getShowDate(chatMap.get(chatObj).get(0).getCreatedTime(), mAdapterContext);
						String time = RKCloudChatTools.getShowTime(chatMap.get(chatObj).get(0).getCreatedTime());
						mItemBuffer.lastmsgDateTV.setText(date);
						mItemBuffer.lastmsgTimeTV.setText(time);
					}
					else
					{
						mItemBuffer.lastmsgDateTV.setText("");
						mItemBuffer.lastmsgTimeTV.setText("");
					}
				}
				else
				{
					mItemBuffer.lastMsgContentTV.setText(chatMap.get(chatObj).size() + "条相关的聊天记录");
					mItemBuffer.lastmsgDateTV.setText("");
					mItemBuffer.lastmsgTimeTV.setText("");
				}
			}
		}

		private void showChatName(RKCloudChatBaseChat chatObj, ItemViewBuffer mItemBuffer)
		{
			if (chatObj instanceof SingleChat)
			{ // 单聊
				RKCloudChatContact contactObj = mContacts.get(chatObj.getChatId());
				// 设置会话名称
				if (null != chatObj.highLightName)
				{
					mItemBuffer.convNameTV.setText(chatObj.highLightName);
				}
				else
				{
					mItemBuffer.convNameTV.setText(null != contactObj ? contactObj.getShowName() : chatObj.getChatId());
				}

				mItemBuffer.headerPhotoView.setImageResource(R.drawable.rkcloud_chat_img_header_default);
				if (null != contactObj && !TextUtils.isEmpty(contactObj.getHeaderThumbImagePath()))
				{
					RKCloudChatImageRequest imageReq = new RKCloudChatImageRequest(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG, contactObj.getHeaderThumbImagePath(), chatObj.getChatId());
					// 如果在缓存中则直接设置图片
					RKCloudChatImageResult imgResult = RKCloudChatImageAsyncLoader.getInstance(mAdapterContext).sendPendingRequestQuryCache(imageReq);
					if (null != imgResult && null != imgResult.resource)
					{
						mItemBuffer.headerPhotoView.setImageDrawable(imgResult.resource);
					}
				}

			}
			else if (chatObj instanceof GroupChat)
			{ // 群聊
				// 设置会话名称
				if (null != chatObj.highLightName)
				{
					mItemBuffer.convNameTV.setText(chatObj.highLightName.append("(").append("" + chatObj.getUserCounts()).append(")"));
				}
				else
				{
					mItemBuffer.convNameTV.setText(String.format("%s(%d)", chatObj.getChatShowName(), chatObj.getUserCounts()));
				}
				// 设置默认头像
				mItemBuffer.headerPhotoView.setImageResource(R.drawable.rkcloud_chat_img_header_mutlichat_default);
			}

			// 设置最后一条消息内容的显示
			RKCloudChatBaseMessage msgObj = chatObj.getLastMsgObj();
			if (null == msgObj || (msgObj instanceof TipMessage) || RKCloudChatConstants.FLAG_LOCAL_TIPMESSAGE.equals(msgObj.getExtension()))
			{
				mItemBuffer.lastMsgContentTV.setText("");
			}
			else
			{
				Class<? extends RKCloudChatBaseChat> chatClassObj = null;
				if (chatObj instanceof SingleChat)
				{
					chatClassObj = SingleChat.class;
				}
				else
				{
					chatClassObj = GroupChat.class;
				}

				CharSequence showContent = RKCloudChatTools.parseMsgFace(mAdapterContext, mMmsManager.getNotificationContent(chatClassObj, msgObj, mContacts.get(msgObj.getSender())), -1, 1);
				if (msgObj instanceof TextMessage && ((TextMessage) msgObj).isDraftMsg())
				{
					// 如果是草稿消息则增加样式的处理
					String draftFlag = getString(R.string.rkcloud_chat_draft);
					int start = draftFlag.indexOf(draftFlag);
					int end = start + draftFlag.length();

					SpannableStringBuilder style = new SpannableStringBuilder(showContent);
					style.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.rkcloud_chat_chatlist_item_draft_textcolor)), start, end, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
					mItemBuffer.lastMsgContentTV.setText(style);
				}
				else
				{
					mItemBuffer.lastMsgContentTV.setText(showContent);
				}
			}
			// 设置最后一条消息时间的显示
			if (chatObj.getLastMsgCreatedTime() > 0)
			{
				String date = RKCloudChatTools.getShowDate(chatObj.getLastMsgCreatedTime(), mAdapterContext);
				String time = RKCloudChatTools.getShowTime(chatObj.getLastMsgCreatedTime());
				mItemBuffer.lastmsgDateTV.setText(date);
				mItemBuffer.lastmsgTimeTV.setText(time);
			}
			else
			{
				mItemBuffer.lastmsgTimeTV.setText("");
			}

			// 设置最后一条消息发送失败状态的显示
			if (null != msgObj && msgObj.getDirection() == RKCloudChatBaseMessage.MSG_DIRECTION.SEND && msgObj.getStatus() == RKCloudChatBaseMessage.MSG_STATUS.SEND_FAILED)
			{
				mItemBuffer.msgFailedTV.setVisibility(View.VISIBLE);
			}
			else
			{
				mItemBuffer.msgFailedTV.setVisibility(View.GONE);
			}
		}

		/**
		 * 使用搜索条件匹配名字
		 *
		 * @param content
		 *            需要设置的消息内容
		 */
		private SpannableStringBuilder matchMsg(String content)
		{
			SpannableStringBuilder highLightMsg;
			String filter = mSearchET.getText().toString().trim();
			int start = 0, end = 0;
			// 消息高亮字段
			if (content.contains(filter))
			{
				start = content.indexOf(filter);
				end = start + filter.length();
				highLightMsg = new SpannableStringBuilder(content);
				if (null != backgroundColorSpan)
				{
					highLightMsg.setSpan(backgroundColorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}
			else
			{
				highLightMsg = null;
			}
			return highLightMsg;
		}
	}
}
