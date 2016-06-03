package com.rongkecloud.chat.demo.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.*;
import com.rongkecloud.chat.*;
import com.rongkecloud.chat.RKCloudChatBaseMessage.MSG_DIRECTION;
import com.rongkecloud.chat.RKCloudChatBaseMessage.MSG_STATUS;
import com.rongkecloud.chat.demo.RKCloudChatConstants;
import com.rongkecloud.chat.demo.RKCloudChatContactManager;
import com.rongkecloud.chat.demo.RKCloudChatMmsManager;
import com.rongkecloud.chat.demo.RKCloudChatUiHandlerMessage;
import com.rongkecloud.chat.demo.entity.RKCloudChatContact;
import com.rongkecloud.chat.demo.tools.RKCloudChatTools;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseFragment;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatCustomDialog;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageAsyncLoader;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageAsyncLoader.ImageLoadedCompleteDelayNotify;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest.IMAGE_REQUEST_TYPE;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageResult;
import com.rongkecloud.sdkbase.RKCloud;
import com.rongkecloud.test.R;
import com.rongkecloud.test.ui.widget.RoundedImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RKCloudChatListFragment extends RKCloudChatBaseFragment implements ImageLoadedCompleteDelayNotify
{
	// 获取其它UI返回的结果类型
	private static final int INTENT_FORWARD_KEY_SELECT_USER = 1;// 选择成员

	// 上下文菜单定义
	private static final int CONTEXT_MENU_SET_READ = 1;// 标为已读
	private static final int CONTEXT_MENU_SET_TOP = 2;// 置顶聊天
	private static final int CONTEXT_MENU_CANCEL_TOP = 3;// 取消置顶
	private static final int CONTEXT_MENU_DELETE_CHAT = 4;// 删除会话
	private static final int CONTEXT_MENU_QUIT_CHAT = 5;// 退出并删除(群时存在)
	private static final int CONTEXT_MENU_RESOLVE_CHAT = 6;// 解散群(群并且是群主时存在)

	// 查询类型
	private static final int QUERY_TYPE_GET_ALL_CHATS = 0;// 获取会话
	private static final int QUERY_TYPE_GET_CONTACTS = 1;// 查询联系人信息
	private static final int QUERY_TYPE_QUERY_CHATS = 2;// 搜索会话

	// UI组件
	private Button title_right_btn;
	private EditText mSearchET;
	private ListView mListView;
	private TextView mNoDataTV;
	private ProgressBar mLoadingPB;

	// 成员变量
	private RKCloudChatMmsManager mMmsManager;
	private RKCloudChatContactManager mContactManager;

	private RKCloudChatListAdapter mAdapter;// 会话适配器
	private List<RKCloudChatBaseChat> mAllData;// 会话列表显示的数据
	private List<RKCloudChatBaseChat> mSearchChatDatas;
	private List<String> mContactAccounts;
	private Map<String, RKCloudChatContact> mContacts;

	private QueryHandlerThread mQuerThread;// 查询数据的线程
	private BackgroundColorSpan backgroundColorSpan;
	private Context mContext;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.rkcloud_chat_chatlist, container, false);
		view.setOnClickListener(null);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		initUIAndListener();
		// 初始化成员变量
		mContext = getActivity();
		mMmsManager = RKCloudChatMmsManager.getInstance(mContext);
		mContactManager = RKCloudChatContactManager.getInstance(mContext);

		mAllData = new ArrayList<RKCloudChatBaseChat>();
		mSearchChatDatas = new ArrayList<RKCloudChatBaseChat>();

		mContacts = new HashMap<String, RKCloudChatContact>();
		mContactAccounts = new ArrayList<String>();
		mAdapter = new RKCloudChatListAdapter(mContext);
		mListView.setAdapter(mAdapter);
		backgroundColorSpan = new BackgroundColorSpan(getResources().getColor(R.color.rkcloud_chat_search_result_highlightcolor));
	}

	@Override
	public void onResume()
	{
		super.onResume();
		refresh();
	}

	@Override
	public void onHiddenChanged(boolean hidden)
	{
		super.onHiddenChanged(hidden);
		if (!hidden)
		{
			refresh();
		}
	}

	@Override
	public void refresh()
	{
		mShow = true;
		mMmsManager.bindUiHandler(mUiHandler);
		mContactManager.bindUiHandler(mUiHandler);
		mContext = getActivity();
		RKCloudChatImageAsyncLoader.getInstance(mContext).registerDelayListener(this);
		startQuery(QUERY_TYPE_GET_ALL_CHATS);// 查询数据
	}

	@Override
	public void onDestroyView()
	{
		if (mQuerThread != null)
		{
			mQuerThread.quit();
			mQuerThread = null;
		}
		super.onDestroyView();
	}

	@Override
	public void onDetach()
	{
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

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (null == data)
		{
			return;
		}
		switch (requestCode)
		{
			case INTENT_FORWARD_KEY_SELECT_USER:
				String accounts = data.getStringExtra(RKCloudChatSelectUsersActivity.INTENT_RETURN_KEY_SELECTED_ACCOUNTS);
				if (TextUtils.isEmpty(accounts))
				{
					return;
				}
				List<String> userAccounts = RKCloudChatTools.splitStrings(accounts);
				if (1 == userAccounts.size())
				{
					mMmsManager.enterMsgListActivity(userAccounts.get(0));
				}
				else if (userAccounts.size() >= 2)
				{
					applyGroup(userAccounts, mContext);
				}

				break;
		}
	}

	private void showContextMenu(final RKCloudChatBaseChat chatObj, final Context context)
	{
		// 设置title
		String title = "";
		if (chatObj instanceof SingleChat)
		{
			RKCloudChatContact contactObj = mContacts.get(chatObj.getChatId());
			title = null != contactObj ? contactObj.getShowName() : chatObj.getChatId();
		}
		else if (chatObj instanceof GroupChat)
		{
			title = chatObj.getChatShowName();
		}
		// 设置显示的条目内容
		List<Integer> ids = new ArrayList<Integer>();
		List<String> contents = new ArrayList<String>();
		if (chatObj.getUnReadMsgCnt() > 0)
		{
			// 设为已读
			ids.add(CONTEXT_MENU_SET_READ);
			contents.add(context.getString(R.string.rkcloud_chat_chatlist_context_read));
		}
		if (chatObj.isTop())
		{
			// 取消置顶
			ids.add(CONTEXT_MENU_CANCEL_TOP);
			contents.add(context.getString(R.string.rkcloud_chat_chatlist_context_canceltop));

		}
		else
		{
			// 置顶聊天
			ids.add(CONTEXT_MENU_SET_TOP);
			contents.add(context.getString(R.string.rkcloud_chat_chatlist_context_settop));
		}
		// 删除会话
		ids.add(CONTEXT_MENU_DELETE_CHAT);
		contents.add(context.getString(R.string.rkcloud_chat_chatlist_context_del));

		if (chatObj instanceof GroupChat)
		{
			GroupChat groupChatObj = (GroupChat) chatObj;
			if (!TextUtils.isEmpty(groupChatObj.getGroupCreater()) && groupChatObj.getGroupCreater().equalsIgnoreCase(RKCloud.getUserName()))
			{
				// 解散群
				ids.add(CONTEXT_MENU_RESOLVE_CHAT);
				contents.add(context.getString(R.string.rkcloud_chat_chatlist_context_resolve));
			}
			else
			{
				// 退出群
				ids.add(CONTEXT_MENU_QUIT_CHAT);
				contents.add(context.getString(R.string.rkcloud_chat_chatlist_context_quit));
			}
		}
		String[] realContents = new String[contents.size()];
		final int[] realIds = new int[contents.size()];
		for (int i = 0; i < contents.size(); i++)
		{
			realContents[i] = contents.get(i);
			realIds[i] = ids.get(i);
		}
		AlertDialog contextDialog = new AlertDialog.Builder(context).setTitle(title).setItems(realContents, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				switch (realIds[which])
				{
					case CONTEXT_MENU_SET_READ:
						if (mMmsManager.updateMsgsReadedInChat(chatObj.getChatId()) > 0)
						{
							startQuery(QUERY_TYPE_GET_ALL_CHATS);
							mMmsManager.cancelNotify(chatObj.getChatId());
						}
						break;

					case CONTEXT_MENU_SET_TOP:
					case CONTEXT_MENU_CANCEL_TOP:
						if (mMmsManager.setChatTop(chatObj.getChatId(), CONTEXT_MENU_SET_TOP == realIds[which]) > 0)
						{
							startQuery(QUERY_TYPE_GET_ALL_CHATS);
						}
						break;

					case CONTEXT_MENU_DELETE_CHAT: // 删除会话
						deleteChat(chatObj, context);
						break;

					case CONTEXT_MENU_QUIT_CHAT:// 退出并删除会话
					case CONTEXT_MENU_RESOLVE_CHAT:
						quitGroup((GroupChat) chatObj, context);
						break;
				}
			}
		}).create();
		contextDialog.show();
	}

	private void applyGroup(final List<String> accounts, Context context)
	{
		final EditText groupNameET = new EditText(context);
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

		final RKCloudChatCustomDialog.Builder dialog = new RKCloudChatCustomDialog.Builder(context).setTitle(R.string.rkcloud_chat_creategroupname_title)
				.setNegativeButton(R.string.rkcloud_chat_btn_cancel, null).setPositiveButton(R.string.rkcloud_chat_btn_confirm, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int whichButton)
					{
						showProgressDialog();
						mMmsManager.createGroup(accounts, groupNameET.getText().toString().trim());
					}
				}).addContentView(groupNameET);
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

	private void initUIAndListener()
	{
		// 设置title
		TextView titleTV = (TextView) getView().findViewById(R.id.txt_title);
		titleTV.setVisibility(TextView.GONE);

		TextView text_title_content = (TextView) getView().findViewById(R.id.text_title_content);
		text_title_content.setText(getString(R.string.rkcloud_chat_chatlist_title));

		title_right_btn = (Button) getView().findViewById(R.id.title_right_btn);
		title_right_btn.setText(R.string.start_group_chat);
		title_right_btn.setTextColor(getResources().getColor(R.color.title_right_btn_text_color));
		title_right_btn.setBackgroundColor(getResources().getColor(R.color.bg_transparent));
		title_right_btn.setVisibility(View.VISIBLE);
		title_right_btn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivityForResult(new Intent(mContext, RKCloudChatSelectUsersActivity.class), INTENT_FORWARD_KEY_SELECT_USER);
			}
		});

		// 初始化UI元素
		mSearchET = (EditText) getView().findViewById(R.id.searchedittext);
		mListView = (ListView) getView().findViewById(R.id.listview);
		mNoDataTV = (TextView) getView().findViewById(R.id.emptytv);
		mLoadingPB = (ProgressBar) getView().findViewById(R.id.loading_progressbar);
		mLoadingPB.setVisibility(View.VISIBLE);

		mSearchET.setFocusable(false);
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
			}
		});

		mSearchET.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivity(new Intent(getActivity(), RKCloudChatSearchChatListActivity.class));
			}
		});
	}

	/*
	 * 查询数据
	 */
	private void startQuery(int queryType)
	{
		if (null == mQuerThread)
		{
			mQuerThread = new QueryHandlerThread("QueryChatListActivityThread");
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
					List<String> accounts = new ArrayList<String>();
					if (mContactAccounts.size() > 0)
					{
						accounts.addAll(mContactAccounts);
					}
					msg.obj = accounts;

				}
				else if (QUERY_TYPE_QUERY_CHATS == queryType)
				{
					List<RKCloudChatBaseChat> chatDatas = new ArrayList<RKCloudChatBaseChat>();
					if (mAllData.size() > 0)
					{
						chatDatas.addAll(mAllData);
					}
					msg.obj = chatDatas;
				}

				msg.sendToTarget();
			}
		}

		@Override
		public boolean handleMessage(Message msg)
		{
			if (QUERY_TYPE_GET_ALL_CHATS == msg.what)
			{
				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.GET_ALLCHATS_INFO_FINISHED;
				message.obj = mMmsManager.queryAllChats();
				message.sendToTarget();

			}
			else if (QUERY_TYPE_GET_CONTACTS == msg.what)
			{
				List<String> accounts = (List<String>) msg.obj;
				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.GET_CONTACTSINFO_FINISHED;
				message.obj = mContactManager.getContactInfos(accounts);
				message.sendToTarget();

			}
			else if (QUERY_TYPE_QUERY_CHATS == msg.what)
			{
				List<RKCloudChatBaseChat> chatDatas = (List<RKCloudChatBaseChat>) msg.obj;
				String filter = mSearchET.getText().toString().trim();
				List<RKCloudChatBaseChat> datas = new ArrayList<RKCloudChatBaseChat>();
				if (!TextUtils.isEmpty(filter))
				{
					for (RKCloudChatBaseChat obj : chatDatas)
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
				else
				{
					for (RKCloudChatBaseChat obj : chatDatas)
					{
						obj.highLightName = null;
						datas.add(obj);
					}
				}

				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.QUERY_ALLCHATS_INFO_FINISHED;
				message.obj = datas;
				message.sendToTarget();
			}

			return true;
		}
	}

	/*
	 * 删除会话
	 */
	private void deleteChat(final RKCloudChatBaseChat chatObj, final Context context)
	{
		String title = "";
		if (chatObj instanceof SingleChat)
		{
			RKCloudChatContact contactObj = mContacts.get(chatObj.getChatId());
			title = null != contactObj ? contactObj.getShowName() : chatObj.getChatId();
		}
		else if (chatObj instanceof GroupChat)
		{
			title = chatObj.getChatShowName();
		}
		AlertDialog.Builder delDialog = new AlertDialog.Builder(context).setTitle(title).setMessage(R.string.rkcloud_chat_chatlist_context_del_confirm)
				.setNegativeButton(R.string.rkcloud_chat_btn_cancel, null).setPositiveButton(R.string.rkcloud_chat_btn_confirm, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						long result = mMmsManager.delChat(chatObj.getChatId(), false);
						if (result <= 0)
						{
							RKCloudChatTools.showToastText(context, getString(R.string.rkcloud_chat_operation_failed));
						}
					}
				});

		delDialog.create().show();
	}

	/*
	 * 退出群
	 */
	private void quitGroup(final GroupChat chatObj, Context context)
	{
		AlertDialog.Builder quitDialog = new AlertDialog.Builder(context).setTitle(chatObj.getChatShowName())
				.setMessage(TextUtils.isEmpty(chatObj.getGroupCreater()) || !chatObj.getGroupCreater().equalsIgnoreCase(RKCloud.getUserName()) ? R.string.rkcloud_chat_chatlist_context_quit_confirm
						: R.string.rkcloud_chat_chatlist_context_resolve_confirm)
				.setNegativeButton(R.string.rkcloud_chat_btn_cancel, null).setPositiveButton(R.string.rkcloud_chat_btn_confirm, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						showProgressDialog();
						mMmsManager.quitGroup(chatObj.getChatId());
					}
				});

		quitDialog.create().show();
	}

	@Override
	public void processResult(Message msg)
	{
		if (!mShow)
		{
			return;
		}
		if (RKCloudChatUiHandlerMessage.GET_ALLCHATS_INFO_FINISHED == msg.what)
		{ // 会话信息获取完成
			mAllData.clear();
			List<RKCloudChatBaseChat> objs = (List<RKCloudChatBaseChat>) msg.obj;
			if (null != objs && objs.size() > 0)
			{
				mAllData.addAll(objs);
			}

			// 获取个人账户信息
			mContactAccounts.clear();
			for (RKCloudChatBaseChat obj : mAllData)
			{
				if (null != obj.getLastMsgObj() && null != obj.getLastMsgObj().getSender() && !mContactAccounts.contains(obj.getLastMsgObj().getSender()))
				{
					mContactAccounts.add(obj.getLastMsgObj().getSender());
				}
				if (obj instanceof SingleChat && !mContactAccounts.contains(obj.getChatId()))
				{
					mContactAccounts.add(obj.getChatId());
				}
			}
			startQuery(QUERY_TYPE_GET_CONTACTS);

		}
		else if (RKCloudChatUiHandlerMessage.GET_CONTACTSINFO_FINISHED == msg.what)
		{ // 联系人信息获取完成
			mContacts.clear();
			Map<String, RKCloudChatContact> datas = (Map<String, RKCloudChatContact>) msg.obj;
			if (null != datas && datas.size() > 0)
			{
				mContacts.putAll(datas);
			}
			startQuery(QUERY_TYPE_QUERY_CHATS);

		}
		else if (RKCloudChatUiHandlerMessage.QUERY_ALLCHATS_INFO_FINISHED == msg.what)
		{ // 会话信息搜索完成
			mSearchChatDatas.clear();

			List<RKCloudChatBaseChat> objs = (List<RKCloudChatBaseChat>) msg.obj;
			if (null != objs && objs.size() > 0)
			{
				mSearchChatDatas.addAll(objs);
				mNoDataTV.setVisibility(View.GONE);
				mListView.setVisibility(View.VISIBLE);
			}
			else
			{
				mNoDataTV.setVisibility(View.VISIBLE);
				mListView.setVisibility(View.GONE);
			}
			mLoadingPB.setVisibility(View.GONE);
			mAdapter.notifyDataSetChanged();

		}
		else if (RKCloudChatUiHandlerMessage.CALLBACK_GROUP_INFO_CHANGED == msg.what || RKCloudChatUiHandlerMessage.RESPONSE_SEND_MMS == msg.what
				|| RKCloudChatUiHandlerMessage.RESPONSE_RESEND_MMS == msg.what || RKCloudChatUiHandlerMessage.CALLBACK_RECEIVED_MMS == msg.what
				|| RKCloudChatUiHandlerMessage.CALLBACK_KICKOUT == msg.what || RKCloudChatUiHandlerMessage.CALLBACK_RECEIVED_MOREMMS == msg.what
				|| RKCloudChatUiHandlerMessage.CALLBACK_GROUP_DISSOLVED == msg.what || RKCloudChatUiHandlerMessage.DELETE_SINGLE_CHAT == msg.what
				|| RKCloudChatUiHandlerMessage.DRAFT_MAG_CHANGED == msg.what || RKCloudChatUiHandlerMessage.ADD_MSG_TO_LOCALDB == msg.what
				|| RKCloudChatUiHandlerMessage.MSG_STATUS_HAS_CHANGED == msg.what)
		{
			startQuery(QUERY_TYPE_GET_ALL_CHATS);

		}
		else if (RKCloudChatUiHandlerMessage.RESPONSE_APPLY_GROUP == msg.what)
		{ // 申请群
			closeProgressDialog();
			if (0 == msg.arg1)
			{
				mMmsManager.enterMsgListActivity((String) msg.obj);

			}
			else if (RKCloudChatErrorCode.RK_SDK_UNINIT == msg.arg1)
			{
				RKCloudChatTools.showToastText(mContext, getString(R.string.rkcloud_chat_sdk_uninit));

			}
			else if (RKCloudChatErrorCode.RK_NOT_NETWORK == msg.arg1)
			{
				RKCloudChatTools.showToastText(mContext, getString(R.string.rkcloud_chat_network_off));

			}
			else if (RKCloudChatErrorCode.CHAT_GROUP_USER_NUMBER_EXCEED_LIMIT == msg.arg1)
			{
				RKCloudChatTools.showToastText(mContext, getString(R.string.rkcloud_chat_groupusers_count_byond));

			}
			else if (RKCloudChatErrorCode.CHAT_GROUP_COUNT_EXCEED_LIMIT == msg.arg1)
			{
				RKCloudChatTools.showToastText(mContext, getString(R.string.rkcloud_chat_groups_count_byond));

			}
			else if (RKCloudChatErrorCode.RK_INVALID_USER == msg.arg1)
			{
				RKCloudChatTools.showToastText(mContext, getString(R.string.rkcloud_chat_illegal_users, msg.obj));

			}
			else
			{
				RKCloudChatTools.showToastText(mContext, getString(R.string.rkcloud_chat_operation_failed));

			}

		}
		else if (RKCloudChatUiHandlerMessage.RESPONSE_QUIT_GROUP == msg.what)
		{ // 退出群
			closeProgressDialog();
			if (0 == msg.arg1)
			{
				startQuery(QUERY_TYPE_GET_ALL_CHATS);

			}
			else if (RKCloudChatErrorCode.RK_SDK_UNINIT == msg.arg1)
			{
				RKCloudChatTools.showToastText(mContext, getString(R.string.rkcloud_chat_sdk_uninit));

			}
			else if (RKCloudChatErrorCode.RK_NOT_NETWORK == msg.arg1)
			{
				RKCloudChatTools.showToastText(mContext, getString(R.string.rkcloud_chat_network_off));

			}
			else
			{
				RKCloudChatTools.showToastText(mContext, getString(R.string.rkcloud_chat_operation_failed));
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

	private class RKCloudChatListAdapter extends BaseAdapter
	{
		private Context mAdapterContext;
		private ItemViewBuffer mItemBuffer;// 每个条目的缓存对象

		public RKCloudChatListAdapter(Context context)
		{
			mAdapterContext = context;
		}

		/*
		 * 定义每个条目中使用View的基本元素
		 */
		private class ItemViewBuffer
		{
			View rootView;
			RoundedImageView headerPhotoView; // 会话头像
			TextView convNameTV; // 会话名称
			TextView unReadMsgCntTV;// 未读消息条数
			TextView lastMsgContentTV;// 最后一条消息的内容
			TextView lastmsgDateTV;// 最后一条消息日期
			TextView lastmsgTimeTV;// 最后一条消息时间
			TextView msgFailedTV; // 消息发送失败时的显示内容

			public ItemViewBuffer(View convertView)
			{
				rootView = convertView.findViewById(R.id.root);
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
			return mSearchChatDatas.size();
		}

		@Override
		public Object getItem(int arg0)
		{
			return mSearchChatDatas.get(arg0);
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
				convertView = LayoutInflater.from(mAdapterContext).inflate(R.layout.rkcloud_chat_chatlist_item, null);
				mItemBuffer = new ItemViewBuffer(convertView);
				convertView.setTag(mItemBuffer);
			}
			else
			{
				mItemBuffer = (ItemViewBuffer) convertView.getTag();
			}

			final RKCloudChatBaseChat chatObj = mSearchChatDatas.get(arg0);// 获取会话数据
			// 设置背景色
			mItemBuffer.rootView.setBackgroundResource(chatObj.isTop() ? R.drawable.rkcloud_chat_list_item_settop_bg : R.drawable.rkcloud_chat_listview_item_bg);

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
				// 设置头像
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

			// 如果未读消息个数大于0，则显示未读消息条数
			if (chatObj.getUnReadMsgCnt() > 0)
			{
				mItemBuffer.unReadMsgCntTV.setVisibility(View.VISIBLE);
				// mItemBuffer.unReadMsgCntTV.setText(chatObj.getUnReadMsgCnt()
				// > 99 ? "99+" : String.valueOf(chatObj.getUnReadMsgCnt()));
				mItemBuffer.unReadMsgCntTV.setText(String.valueOf(chatObj.getUnReadMsgCnt()));
			}
			else
			{
				mItemBuffer.unReadMsgCntTV.setVisibility(View.GONE);
			}

			// 设置最后一条消息内容的显示
			RKCloudChatBaseMessage msgObj = chatObj.getLastMsgObj();
			if (null == msgObj || (msgObj instanceof TipMessage) || RKCloudChatConstants.FLAG_LOCAL_TIPMESSAGE.equals(msgObj.getExtension()))
			{
				// 提示类型的消息时不显示
				mItemBuffer.lastMsgContentTV.setVisibility(View.GONE);
			}
			else
			{
				mItemBuffer.lastMsgContentTV.setVisibility(View.VISIBLE);
				Class<? extends RKCloudChatBaseChat> chatClassObj = null;
				if (chatObj instanceof SingleChat)
				{
					chatClassObj = SingleChat.class;
				}
				else
				{
					chatClassObj = GroupChat.class;
				}

				if (MSG_STATUS.MESSAGE_REVOKE == msgObj.getStatus())
				{
					if (msgObj.getSender().equals(RKCloud.getUserName()))
					{
						mItemBuffer.lastMsgContentTV.setText(mContext.getString(R.string.rkcloud_chat_revoke_message_self));
					}
					else
					{
						if (!mContacts.containsKey(msgObj.getSender()))
						{
							mContacts.put(msgObj.getSender(), mContactManager.getContactInfo(msgObj.getSender()));
						}
						RKCloudChatContact contactObj = mContacts.get(msgObj.getSender());
						mItemBuffer.lastMsgContentTV.setText(mContext.getString(R.string.rkcloud_chat_revoke_message_other, null != contactObj ? contactObj.getShowName() : msgObj.getSender()));
					}
				}
				else
				{
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
				mItemBuffer.lastmsgTimeTV.setVisibility(View.GONE);
			}

			// 设置最后一条消息发送失败状态的显示
			if (null != msgObj && msgObj.getDirection() == MSG_DIRECTION.SEND && msgObj.getStatus() == MSG_STATUS.SEND_FAILED)
			{
				mItemBuffer.msgFailedTV.setVisibility(View.VISIBLE);
			}
			else
			{
				mItemBuffer.msgFailedTV.setVisibility(View.GONE);
			}
			// 点击事件
			convertView.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					mMmsManager.enterMsgListActivity(chatObj.getChatId());
				}
			});
			// 长点击事件
			convertView.setOnLongClickListener(new OnLongClickListener()
			{
				@Override
				public boolean onLongClick(View v)
				{
					showContextMenu(chatObj, mAdapterContext);
					return false;
				}
			});
			return convertView;
		}
	}
}
