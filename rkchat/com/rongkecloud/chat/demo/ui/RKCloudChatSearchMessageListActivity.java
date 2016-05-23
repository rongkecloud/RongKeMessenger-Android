package com.rongkecloud.chat.demo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.rongkecloud.chat.GroupChat;
import com.rongkecloud.chat.RKCloudChatBaseChat;
import com.rongkecloud.chat.RKCloudChatBaseMessage;
import com.rongkecloud.chat.SingleChat;
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

public class RKCloudChatSearchMessageListActivity extends RKCloudChatBaseActivity implements ImageLoadedCompleteDelayNotify
{

	public static final String INTENT_KEY_FILTER = "intent_key_filter";// 搜索消息的过滤条件
	public static final String INTENT_KEY_CHAT_DATA = "intent_key_chat_data";// 会话对象key
	public static final String INTENT_KEY_MESSAGE_DATA = "intent_key_message_data";// 消息集合的key
	// 查询类型
	private static final int QUERY_TYPE_GET_CONTACTS = 1;// 查询联系人信息

	// UI组件
	private EditText mSearchET;
	private ListView mListView;
	private TextView mChatDescTV;

	// 成员变量
	private RKCloudChatMmsManager mMmsManager;
	private RKCloudChatContactManager mContactManager;

	private RKCloudChatListAdapter mAdapter;// 会话适配器
	private ArrayList<RKCloudChatBaseMessage> mAllData;// 会话列表显示的数据
	private RKCloudChatBaseChat mChat;// 当前消息所有的会话信息
	private List<String> mContactAccounts;
	private Map<String, RKCloudChatContact> mContacts;
	private QueryHandlerThread mQuerThread;// 查询数据的线程
	private BackgroundColorSpan backgroundColorSpan;
	private String mFilter;// 搜索消息的keyword

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rkcloud_chat_searchchatlist);

		mChat = getIntent().getParcelableExtra(INTENT_KEY_CHAT_DATA);
		mAllData = getIntent().getParcelableArrayListExtra(INTENT_KEY_MESSAGE_DATA);
		mFilter = getIntent().getStringExtra(INTENT_KEY_FILTER);
		initUIAndListener();
		// 初始化成员变量
		mMmsManager = RKCloudChatMmsManager.getInstance(this);
		mContactManager = RKCloudChatContactManager.getInstance(this);

		mContacts = new HashMap();
		mContactAccounts = new ArrayList();
		mAdapter = new RKCloudChatListAdapter();
		mListView.setAdapter(mAdapter);
		backgroundColorSpan = new BackgroundColorSpan(getResources().getColor(R.color.rkcloud_chat_search_result_highlightcolor));

		if (null != mChat.getLastMsgObj() && null != mChat.getLastMsgObj().getSender() && !mContactAccounts.contains(mChat.getLastMsgObj().getSender()))
		{
			mContactAccounts.add(mChat.getLastMsgObj().getSender());
		}
		if (mChat instanceof SingleChat && !mContactAccounts.contains(mChat.getChatId()))
		{
			mContactAccounts.add(mChat.getChatId());
		}

		startQuery(QUERY_TYPE_GET_CONTACTS);
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
		mSearchET.setVisibility(View.GONE);
		mListView = (ListView) findViewById(R.id.listview);
		mListView.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				RKCloudChatBaseMessage message = mAllData.get(arg2);
				Intent intent = new Intent(RKCloudChatSearchMessageListActivity.this, RKCloudChatMsgActivity.class);
				if (mChat instanceof SingleChat)
				{
					intent.putExtra(RKCloudChatMsgActivity.INTENT_KEY_MSGLIST_CHATID, mChat.getChatId());
				}
				else if (mChat instanceof GroupChat)
				{
					intent.putExtra(RKCloudChatMsgActivity.INTENT_KEY_MSGLIST_GROUPID, mChat.getChatId());
				}
				intent.putExtra(RKCloudChatMsgActivity.INTENT_KEY_MSGLIST_MSGID, message.getMsgSerialNum());
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				RKCloudChatSearchMessageListActivity.this.startActivity(intent);
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
			mQuerThread = new QueryHandlerThread("QueryChatSearchListActivityThread");
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
			mAdapter.notifyDataSetChanged();
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

	private class RKCloudChatListAdapter extends BaseAdapter
	{
		private ItemViewBuffer mItemBuffer;// 每个条目的缓存对象

		public RKCloudChatListAdapter()
		{
		}

		/*
		 * 定义每个条目中使用View的基本元素
		 */
		private class ItemViewBuffer
		{
			TextView chatListDesc;// 会话描述信息显示部分
			RoundedImageView headerPhotoView; // 会话头像
			TextView convNameTV; // 会话名称
			TextView msgContentTV;// 消息的内容
			TextView msgDateTV;// 消息日期
			TextView msgTimeTV;// 消息时间

			public ItemViewBuffer(View convertView)
			{
				chatListDesc = (TextView) convertView.findViewById(R.id.chatlist_desc);
				headerPhotoView = (RoundedImageView) convertView.findViewById(R.id.headerphoto);
				convNameTV = (TextView) convertView.findViewById(R.id.name);
				msgContentTV = (TextView) convertView.findViewById(R.id.msgcontent);
				msgDateTV = (TextView) convertView.findViewById(R.id.lastmsgdate);
				msgTimeTV = (TextView) convertView.findViewById(R.id.lastmsgtime);
			}
		}

		@Override
		public int getCount()
		{
			return mAllData.size();
		}

		@Override
		public Object getItem(int arg0)
		{
			return mAllData.get(arg0);
		}

		@Override
		public long getItemId(int arg0)
		{
			return arg0;
		}

		@Override
		public View getView(int arg0, View convertView, ViewGroup arg2)
		{
			if (null == convertView)
			{
				convertView = LayoutInflater.from(RKCloudChatSearchMessageListActivity.this).inflate(R.layout.rkcloud_chat_searchchatlist_item, null);
				mItemBuffer = new ItemViewBuffer(convertView);
				convertView.setTag(mItemBuffer);
			}
			else
			{
				mItemBuffer = (ItemViewBuffer) convertView.getTag();
			}

			if (0 == arg0)
			{
				mItemBuffer.chatListDesc.setVisibility(View.VISIBLE);
				mItemBuffer.chatListDesc.setText(getString(R.string.rkcloud_chat_chatlist_search_count, mAllData.size(), mFilter));
			}
			else
			{
				mItemBuffer.chatListDesc.setVisibility(View.GONE);
			}

			final RKCloudChatBaseMessage messageObj = mAllData.get(arg0);// 获取会话数据

			if (mChat instanceof SingleChat)
			{ // 单聊
				RKCloudChatContact contactObj = mContacts.get(mChat.getChatId());
				// 设置会话名称
				mItemBuffer.convNameTV.setText(null != contactObj ? contactObj.getShowName() : mChat.getChatId());
				// 设置头像
				mItemBuffer.headerPhotoView.setImageResource(R.drawable.rkcloud_chat_img_header_default);
				if (null != contactObj && !TextUtils.isEmpty(contactObj.getHeaderThumbImagePath()))
				{
					RKCloudChatImageRequest imageReq = new RKCloudChatImageRequest(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG, contactObj.getHeaderThumbImagePath(), mChat.getChatId());
					// 如果在缓存中则直接设置图片
					RKCloudChatImageResult imgResult = RKCloudChatImageAsyncLoader.getInstance(RKCloudChatSearchMessageListActivity.this).sendPendingRequestQuryCache(imageReq);
					if (null != imgResult && null != imgResult.resource)
					{
						mItemBuffer.headerPhotoView.setImageDrawable(imgResult.resource);
					}
				}
			}
			else if (mChat instanceof GroupChat)
			{ // 群聊
				mItemBuffer.convNameTV.setText(mChat.getChatShowName());
				// 设置默认头像
				mItemBuffer.headerPhotoView.setImageResource(R.drawable.rkcloud_chat_img_header_mutlichat_default);
			}

			// 设置消息内容的显示
			if (null != matchMsg(messageObj.getContent()))
			{
				mItemBuffer.msgContentTV.setText(matchMsg(messageObj.getContent()));
			}
			else
			{
				mItemBuffer.msgContentTV.setText(messageObj.getContent());
			}

			// 设置消息时间的显示
			String date = RKCloudChatTools.getShowDate(messageObj.getCreatedTime(), RKCloudChatSearchMessageListActivity.this);
			String time = RKCloudChatTools.getShowTime(messageObj.getCreatedTime());
			mItemBuffer.msgDateTV.setText(date);
			mItemBuffer.msgTimeTV.setText(time);
			return convertView;
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
			int start = 0, end = 0;
			// 消息高亮字段
			if (content.contains(mFilter))
			{
				start = content.indexOf(mFilter);
				end = start + mFilter.length();
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
