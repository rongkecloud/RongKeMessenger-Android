package com.rongkecloud.chat.demo.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.rongkecloud.chat.GroupChat;
import com.rongkecloud.chat.RKCloudChatBaseChat;
import com.rongkecloud.chat.RKCloudChatErrorCode;
import com.rongkecloud.chat.demo.RKCloudChatContactManager;
import com.rongkecloud.chat.demo.RKCloudChatMmsManager;
import com.rongkecloud.chat.demo.RKCloudChatUiHandlerMessage;
import com.rongkecloud.chat.demo.entity.RKCloudChatContact;
import com.rongkecloud.chat.demo.tools.RKCloudChatTools;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
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

public class RKCloudChatGroupManageActivity extends RKCloudChatBaseActivity implements OnClickListener, ImageLoadedCompleteDelayNotify{
	// 定义从其它UI返回的结果类型值
	private static final int INTENT_FORWARD_KEY_SELECT_USER = 1;// 选择成员
	public static final int INTENT_FORWARD_KEY_SET_BGIMG = 2;// 设置背景图片
	
	public static final String USER_INVITE_FLAG = "+1";// 邀请成员时的特殊号码
	public static final String USER_KICK_FLAG = "-1";// 邀请成员时的特殊号码
	
	// 定义查询使用的相关类型
	private static final int QUERY_TYPE_GROUP_USERS = 1;// 获取群内所有成员
	private static final int QUERY_TYPE_QUERY_CONTACTS = 2;// 查询联系人信息
	
	// 需要从消息列表传递的参数项
	public static final String INTENT_GROUP_CHATID = "group_chatid";
	
	private TextView mTitleName,text_title_content;// title
	private GridView mGridView;// 头像组件	
	private LinearLayout mGroupNameLayout;// 会话名称布局
	private TextView mGroupNameTV; // 会话名称
	private LinearLayout mInviteAuthLayout;// 邀请权限布局
	private ImageView mInviteAuthImg;
	private ImageView mSetTopImg;// 是否置顶聊天
	private ImageView mIsRemindImg; // 接收消息提醒复选框
	private LinearLayout mSetBgImgLayout;// 设置聊天背景
	private LinearLayout mClearMsgLayout;// 清空消息
	private Button mQuitBnt;// 退出并删除会话 或解散群
	
	// 成员变量
	private String mChatId; // 会话ID
	private GroupChat mGroupChatObj; // 会话对象
	private String mCurrAccount;

	private List<String> mDatas;
	private Map<String, RKCloudChatContact> mContacts;
	private RKCloudChatGroupManageAdapter mAdapter; // 适配器
	
	private RKCloudChatMmsManager mMmsManager; // 消息管理器对象
	private RKCloudChatContactManager mContactManager;
	
	private boolean mDelStatus = false;// 是否处于踢除用户的状态
	private boolean mInviteAuth;
	private QueryHandlerThread mQueryThread;// 查询数据使用的相关变量
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rkcloud_chat_manage_group);
		mChatId = getIntent().getStringExtra(INTENT_GROUP_CHATID);
		if (TextUtils.isEmpty(mChatId)) {
			finish();
			return;
		}
		initUIAndListeners();
		initData();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mMmsManager.bindUiHandler(mUiHandler);
		mContactManager.bindUiHandler(mUiHandler);
		RKCloudChatImageAsyncLoader.getInstance(this).registerDelayListener(this);
		refreshGroupInfo();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		closeProgressDialog();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		cancelDelStatus();
	}
	
	@Override
	protected void onDestroy() {
		if (mQueryThread != null) {
			mQueryThread.quit();
			mQueryThread = null;
		}
		super.onDestroy();
	}

	@Override
	public void onLoadImageCompleteDelayNotify(IMAGE_REQUEST_TYPE type, List<RKCloudChatImageResult> result) {
		if(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG == type){
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onClick(View v) {
		// 先取消踢除用户操作
		cancelDelStatus();
		
		int id = v.getId();
		if(R.id.layout_groupremark == id){ // 修改会话名称
			modifyGroupRemark(mGroupChatObj.getChatShowName());
			
		}else if(R.id.inviteauth == id){ // 邀请权限
			boolean isSelected = mInviteAuthImg.isSelected();
			showProgressDialog();
			mMmsManager.modifyGroupInviteAuth(mChatId, !isSelected);
			
		}else if(R.id.settop == id){ // 是否置顶
			boolean isSelected = mSetTopImg.isSelected();
			if(mMmsManager.setChatTop(mChatId, !isSelected) > 0){
				if (null != mGroupChatObj) {
					RKCloudChatBaseChat chatObj = mMmsManager.queryChat(mChatId);
					mGroupChatObj.copyData(chatObj);
				}
				mSetTopImg.setSelected(!isSelected);
			}
		}else if(R.id.isremind == id){ // 是否提醒
			boolean isSelected = mIsRemindImg.isSelected();
			if(mMmsManager.isRemindInGroup(mChatId, !isSelected) > 0){
				if (null != mGroupChatObj) {
					RKCloudChatBaseChat chatObj = mMmsManager.queryChat(mChatId);
					mGroupChatObj.copyData(chatObj);
				}
				mIsRemindImg.setSelected(!isSelected);
			}
			
		}else if(R.id.layout_setbgimg == id){ // 设置聊天背景
			Intent bgIntent = new Intent(this, RKCloudChatSetMsgBgActivity.class);
			bgIntent.putExtra(RKCloudChatSetMsgBgActivity.INTENT_CHAT_ID, mChatId);
			startActivityForResult(bgIntent, INTENT_FORWARD_KEY_SET_BGIMG);
			
		}else if(R.id.layout_clearmsg == id){ // 清空消息记录
			// 显示确认对话框
			AlertDialog.Builder dialog = new AlertDialog.Builder(this)
					.setTitle(R.string.rkcloud_chat_tip)
					.setMessage(R.string.rkcloud_chat_manage_clearmsg_confirm)
					.setNegativeButton(R.string.rkcloud_chat_btn_cancel, null)
					.setPositiveButton(R.string.rkcloud_chat_btn_confirm, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							long result = mMmsManager.delMsgsByChatId(mChatId);
							if(result > 0){
								RKCloudChatTools.showToastText(RKCloudChatGroupManageActivity.this, getString(R.string.rkcloud_chat_operation_success));
							}else{
								RKCloudChatTools.showToastText(RKCloudChatGroupManageActivity.this, getString(R.string.rkcloud_chat_operation_failed));
							}
						}
					});
			dialog.show();
			
		}else if(R.id.quit == id){ // 退群
			// 显示确认对话框
			AlertDialog.Builder dialog = new AlertDialog.Builder(this)
					.setTitle(R.string.rkcloud_chat_tip)
					.setMessage(!TextUtils.isEmpty(mGroupChatObj.getGroupCreater()) && mGroupChatObj.getGroupCreater().equalsIgnoreCase(mCurrAccount) ? R.string.rkcloud_chat_manage_resolve_confirm : R.string.rkcloud_chat_manage_quit_confirm)
					.setNegativeButton(R.string.rkcloud_chat_btn_cancel, null)
					.setPositiveButton(R.string.rkcloud_chat_btn_confirm, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							showProgressDialog();
							mMmsManager.quitGroup(mChatId);
						}
					});
			dialog.show();
			
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (null==data || resultCode!=RESULT_OK) {
			return;
		}
		
		switch(requestCode){
		case INTENT_FORWARD_KEY_SELECT_USER:
			String accounts = data.getStringExtra(RKCloudChatSelectUsersActivity.INTENT_RETURN_KEY_SELECTED_ACCOUNTS);
			if(TextUtils.isEmpty(accounts)){
				return;
			}
			List<String> userAccounts = RKCloudChatTools.splitStrings(accounts);
			if(userAccounts.size() > 0){
				// 表示邀请成员
				showProgressDialog();
				mMmsManager.inviteUsers(mChatId, userAccounts);
			}
			break;
			
		case INTENT_FORWARD_KEY_SET_BGIMG:
			boolean setBgImgOk = data.getBooleanExtra(RKCloudChatSetMsgBgActivity.INTENT_RETURNKEY_SETIMG_OK, false);
			if (setBgImgOk) {
				finish();
			}
			break;
		}
	}
	
	private void initUIAndListeners(){
		// 初始化UI视图
		mTitleName = (TextView)findViewById(R.id.txt_title);
		mTitleName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		mTitleName.setOnClickListener(mExitListener);

        text_title_content = (TextView)findViewById(R.id.text_title_content);

		mGridView = (GridView) findViewById(R.id.gridview);
		mGroupNameLayout = (LinearLayout) findViewById(R.id.layout_groupremark);
		mGroupNameTV = (TextView) findViewById(R.id.groupremark);
		mInviteAuthLayout = (LinearLayout) findViewById(R.id.layout_inviteauth);
		mInviteAuthImg = (ImageView) findViewById(R.id.inviteauth);
		mSetTopImg = (ImageView) findViewById(R.id.settop);
		mIsRemindImg = (ImageView) findViewById(R.id.isremind);
		mSetBgImgLayout = (LinearLayout) findViewById(R.id.layout_setbgimg);
		mClearMsgLayout = (LinearLayout) findViewById(R.id.layout_clearmsg);
		mQuitBnt = (Button) findViewById(R.id.quit);
				
		// 设置监听
		mGroupNameLayout.setOnClickListener(this);
		mInviteAuthImg.setOnClickListener(this);
		mSetTopImg.setOnClickListener(this);
		mIsRemindImg.setOnClickListener(this);
		mSetBgImgLayout.setOnClickListener(this);
		mClearMsgLayout.setOnClickListener(this);
		mQuitBnt.setOnClickListener(this);
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (position >= mDatas.size()) {
					return;
				}
				
				String userAccount = mDatas.get(position);
				if (userAccount.equals(USER_INVITE_FLAG)) {
					if(!mDelStatus){
						Intent intent = new Intent(RKCloudChatGroupManageActivity.this, RKCloudChatSelectUsersActivity.class);
						StringBuffer existAccounts = new StringBuffer();
						for(String account : mDatas){
							if(USER_INVITE_FLAG.equals(account) || USER_KICK_FLAG.equals(account)){
								continue;
							}
							existAccounts.append(account).append(",");
						}
						if(existAccounts.length() > 0){
							existAccounts.deleteCharAt(existAccounts.length()-1);
						}
						intent.putExtra(RKCloudChatSelectUsersActivity.INTENT_KEY_EXIST_ACCOUNTS, existAccounts.toString());
						startActivityForResult(intent, INTENT_FORWARD_KEY_SELECT_USER);
					}
					
				}else if(userAccount.equals(USER_KICK_FLAG)){
					mDelStatus = !mDelStatus;
					mAdapter.setDelStatus(mDelStatus);
					mAdapter.notifyDataSetChanged();
					
				}else{
					if(!mDelStatus){
						mContactManager.jumpContactDetailInfoUI(RKCloudChatGroupManageActivity.this, userAccount);
					}else{
						if(!userAccount.equalsIgnoreCase(mCurrAccount)){
							kickUser(userAccount);
						}
					}
				}
			}
		});
	}
	
	private void initData(){
		mMmsManager = RKCloudChatMmsManager.getInstance(this);
		mContactManager = RKCloudChatContactManager.getInstance(this);
		mCurrAccount = RKCloud.getUserName();
		
		mDatas = new ArrayList<String>();
		if(!TextUtils.isEmpty(mCurrAccount)){
			mDatas.add(mCurrAccount);
		}
		mContacts = new HashMap<String, RKCloudChatContact>();
		
		mAdapter = new RKCloudChatGroupManageAdapter();
		mGridView.setAdapter(mAdapter);
		mDelStatus = false;
	}
	
	/*
	 * 取消删除状态
	 */
	private void cancelDelStatus(){
		mDelStatus = false;
		mAdapter.setDelStatus(mDelStatus);
		mAdapter.notifyDataSetChanged();
	}	
	
	private void kickUser(final String rkAccount){
		// 获取用户名称
		RKCloudChatContact contactObj = mContacts.get(rkAccount);
		String showName = null!=contactObj ? contactObj.getShowName() : rkAccount;
		// 显示确认对话框
		AlertDialog.Builder dialog = new AlertDialog.Builder(this)
				.setTitle(R.string.rkcloud_chat_tip)
				.setMessage(getString(R.string.rkcloud_chat_manage_kickuser_confirm, showName))
				.setNegativeButton(R.string.rkcloud_chat_btn_cancel, null)
				.setPositiveButton(R.string.rkcloud_chat_btn_confirm, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// 踢人操作
						showProgressDialog();
						mMmsManager.kickUserFromGroup(mChatId, rkAccount);
					}
				});
		dialog.show();
	}
	
	/*
	 * 修改会话名称 
	 */
	private void modifyGroupRemark(final String groupName){
		final EditText groupNameET = new EditText(this);	
		groupNameET.setBackgroundResource(R.drawable.rkcloud_chat_edittext_bg);
		groupNameET.setSingleLine();
		groupNameET.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
		InputFilter filter = new InputFilter.LengthFilter(30);
		groupNameET.setFilters(new InputFilter[] {filter});
		groupNameET.setText(groupName);
		groupNameET.setHint(R.string.rkcloud_chat_creategroupname_hint);
		groupNameET.setCursorVisible(true);
		groupNameET.setSelected(true);
		groupNameET.setSelection(groupNameET.getText().toString().trim().length());
		
		final RKCloudChatCustomDialog.Builder dialog = new RKCloudChatCustomDialog.Builder(this);
		dialog.setTitle(R.string.rkcloud_chat_creategroupname_title);									
		dialog.setNegativeButton(R.string.rkcloud_chat_btn_cancel, null);				
		dialog.setPositiveButton(R.string.rkcloud_chat_btn_confirm, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String content = groupNameET.getText().toString().trim();
				if(!content.equals(groupName)){
					mMmsManager.modifyGroupRemark(mChatId, content);
					mGroupChatObj.setGroupRemark(content);
					mGroupNameTV.setText(content);
				}
			}
		});		

		dialog.addContentView(groupNameET);		
		dialog.create().show();	
		// 设置输入框内容改变事件
		groupNameET.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				dialog.setPositiveButtonEnabled(TextUtils.isEmpty(arg0.toString().trim()) ? false : true);
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			}

			@Override
			public void afterTextChanged(Editable arg0) {
			}
		});			
	}
	
	/*
	 * 查询数据
	 */
	private void startQuery(int queryType) {
		if (null == mQueryThread) {
			mQueryThread = new QueryHandlerThread("QueryChatGroupManageActivityThread");
			mQueryThread.start();
		}
		mQueryThread.startQuery(queryType);
	}

	private class QueryHandlerThread extends HandlerThread implements Callback {
		private Handler mQuerHandler;

		public QueryHandlerThread(String name) {
			super(name);
		}

		public void startQuery(int queryType) {
			if (null == mQuerHandler) {
				mQuerHandler = new Handler(getLooper(), this);
			}
			if (!mQuerHandler.hasMessages(queryType)) {
				Message msg = mQuerHandler.obtainMessage();
				msg.what = queryType;
				if(QUERY_TYPE_QUERY_CONTACTS == queryType){
					List<String> accounts = new ArrayList<String>();
					if(mDatas.size() > 0){
						accounts.addAll(mDatas);
						accounts.remove(USER_INVITE_FLAG);
						accounts.remove(USER_KICK_FLAG);
					}
					msg.obj = accounts;
				}
				
				msg.sendToTarget();
			}
		}

		@Override
		public boolean handleMessage(Message msg) {
			if(QUERY_TYPE_GROUP_USERS == msg.what){
				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.QUERY_GROUP_USERS_FINISH;
				message.obj = mMmsManager.queryGroupUsers(mChatId);
				message.sendToTarget();
				
			}else if(QUERY_TYPE_QUERY_CONTACTS == msg.what){
				List<String> accounts = (List<String>)msg.obj;
				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.GET_CONTACTSINFO_FINISHED;
				message.obj = mContactManager.getContactInfos(accounts);
				message.sendToTarget();
			}

			return true;
		}
	}
	
	private void refreshGroupInfo(){	
		RKCloudChatBaseChat chatObj = mMmsManager.queryChat(mChatId);
		// 非群会话时结束并返回
		if(null==chatObj || !(chatObj instanceof GroupChat)){
			finish();
			return;
		}
		
		mGroupChatObj = (GroupChat)chatObj;
        text_title_content.setText(getString(R.string.rkcloud_chat_manage_title, mGroupChatObj.getUserCounts()));
        // 设置群名称
		mGroupNameTV.setText(mGroupChatObj.getChatShowName());
		// 设置邀请方式的显示与隐藏
		if(!TextUtils.isEmpty(mGroupChatObj.getGroupCreater()) && mGroupChatObj.getGroupCreater().equalsIgnoreCase(mCurrAccount)){
			mInviteAuthLayout.setVisibility(View.VISIBLE);
			findViewById(R.id.inviteauth_underline).setVisibility(View.VISIBLE);
			mInviteAuth = mGroupChatObj.getInviteAuth();
			mInviteAuthImg.setSelected(mInviteAuth);
		}else{
			mInviteAuthLayout.setVisibility(View.GONE);
			findViewById(R.id.inviteauth_underline).setVisibility(View.GONE);
		}
		// 设置提醒方式
		mSetTopImg.setSelected(mGroupChatObj.isTop());
		mIsRemindImg.setSelected(mGroupChatObj.getRemindStatus());
		mQuitBnt.setText(!TextUtils.isEmpty(mGroupChatObj.getGroupCreater()) && mGroupChatObj.getGroupCreater().equalsIgnoreCase(mCurrAccount) ? R.string.rkcloud_chat_manage_resolve : R.string.rkcloud_chat_manage_quit);
		// 查询群成员列表数据
		startQuery(QUERY_TYPE_GROUP_USERS);
	}

	@Override
	public void processResult(Message msg) {
		if(RKCloudChatUiHandlerMessage.QUERY_GROUP_USERS_FINISH == msg.what){ // 数据获取完成
			mDatas.clear();
			List<String> data = (List<String>) msg.obj;
			if (data!=null && data.size()>0) {
				mDatas.addAll(data);
			}
			
			// 如果是创建者则添加踢除功能
			if(!TextUtils.isEmpty(mGroupChatObj.getGroupCreater()) && mGroupChatObj.getGroupCreater().equalsIgnoreCase(mCurrAccount)){
				mDatas.add(USER_KICK_FLAG);
			}
			// 添加邀请图标
			if(mGroupChatObj.getInviteAuth() || (!mGroupChatObj.getInviteAuth() && !TextUtils.isEmpty(mGroupChatObj.getGroupCreater()) && mGroupChatObj.getGroupCreater().equalsIgnoreCase(mCurrAccount))){
				mDatas.add(USER_INVITE_FLAG);
			}
			
			mAdapter.notifyDataSetChanged();
			startQuery(QUERY_TYPE_QUERY_CONTACTS);
			
		}else if(RKCloudChatUiHandlerMessage.GET_CONTACTSINFO_FINISHED == msg.what){ // 联系人信息获取完成
			mContacts.clear();
			Map<String, RKCloudChatContact> datas = (Map<String, RKCloudChatContact>)msg.obj;
			if(null!=datas && datas.size()>0){
				for(String acc : datas.keySet()){
					mContacts.put(acc, datas.get(acc));
				}
			}
			mAdapter.notifyDataSetChanged();
			
		}else if(RKCloudChatUiHandlerMessage.RESPONSE_INVITE_USERS == msg.what){ // 邀请成员
			closeProgressDialog();
			if(0 == msg.arg1){				
				refreshGroupInfo();				
			}else if(RKCloudChatErrorCode.RK_SDK_UNINIT == msg.arg1){
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_sdk_uninit));
				
			}else if(RKCloudChatErrorCode.RK_NOT_NETWORK == msg.arg1){
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_network_off));
			}else if(RKCloudChatErrorCode.CHAT_GROUP_USER_NUMBER_EXCEED_LIMIT == msg.arg1){
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_groupusers_count_byond));
			}else if(RKCloudChatErrorCode.CHAT_GROUP_USER_NOT_EXIST == msg.arg1 || RKCloudChatErrorCode.CHAT_GROUP_NOT_EXIST == msg.arg1){
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_dissolve));
				finish();
			}else if(RKCloudChatErrorCode.RK_INVALID_USER == msg.arg1){
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_illegal_users, msg.obj));
			}else if(RKCloudChatErrorCode.CHAT_GROUP_USER_HAS_EXIST == msg.arg1){
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_selectuser_exist, msg.obj));
			}else if(RKCloudChatErrorCode.CHAT_MMS_CANNOT_INVITE_OWN == msg.arg1){
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_selectuser_containown));
			}else {
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_operation_failed));
			}
			
		}else if(RKCloudChatUiHandlerMessage.RESPONSE_KICKUSER == msg.what){ // 踢出成员
			closeProgressDialog();
			if(0 == msg.arg1){
				refreshGroupInfo();			
			}else if(RKCloudChatErrorCode.RK_SDK_UNINIT == msg.arg1){
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_sdk_uninit));
			}else if(RKCloudChatErrorCode.RK_NOT_NETWORK == msg.arg1){
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_network_off));
			}else {
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_operation_failed));
			}
			// 取消踢除操作
			mDelStatus = false;
			mAdapter.setDelStatus(mDelStatus);
			mAdapter.notifyDataSetChanged();
			
		}else if(RKCloudChatUiHandlerMessage.RESPONSE_QUIT_GROUP == msg.what){ // 主动退出群
			closeProgressDialog();
			if(0 == msg.arg1){
				finish();				
			}else if(RKCloudChatErrorCode.RK_SDK_UNINIT == msg.arg1){
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_sdk_uninit));
			}else if(RKCloudChatErrorCode.RK_NOT_NETWORK == msg.arg1){
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_network_off));
			}else {
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_operation_failed));
			}
			
		}else if(RKCloudChatUiHandlerMessage.CALLBACK_GROUP_USERS_CHANGED==msg.what){ // 成员有变化
			if(mChatId.equalsIgnoreCase((String)msg.obj)){
				refreshGroupInfo();
			}
			
		}else if(RKCloudChatUiHandlerMessage.CALLBACK_KICKOUT==msg.what){ // 被踢出群
			// 如果用户被踢出当前会话，则给出提示，并返回到会话列表页面
			if (mChatId.equalsIgnoreCase((String) msg.obj)) {
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_kickoff_by_owner));
				finish();
			}
			
		}else if(RKCloudChatUiHandlerMessage.CALLBACK_GROUP_DISSOLVED==msg.what){ // 群解散
			if (mChatId.equalsIgnoreCase((String) msg.obj)) {
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_dissolve));
				finish();
			}
			
		}else if(RKCloudChatUiHandlerMessage.DELETE_SINGLE_CHAT==msg.what){ // 群被删除
			if (mChatId.equalsIgnoreCase((String) msg.obj)) {
				finish();
			}
			
		}else if(RKCloudChatUiHandlerMessage.RESPONSE_MODIFY_GROUP_INVITEAUTH == msg.what){ // 修改邀请权限
			if (mChatId.equalsIgnoreCase((String) msg.obj)) {
				closeProgressDialog();
				if(0 == msg.arg1){
					mInviteAuth = !mInviteAuth;
					mGroupChatObj.setInviteAuth(mInviteAuth);
					mInviteAuthImg.setSelected(mInviteAuth);
					if(mInviteAuth){
						if(!mDatas.contains(USER_INVITE_FLAG)){
							mDatas.add(USER_INVITE_FLAG);
							mAdapter.notifyDataSetChanged();
						}
						
					}else{
						if(TextUtils.isEmpty(mGroupChatObj.getGroupCreater()) || !mGroupChatObj.getGroupCreater().equalsIgnoreCase(mCurrAccount)){
							mDatas.remove(USER_INVITE_FLAG);
							mAdapter.notifyDataSetChanged();
						}
					}
					
				}else if(RKCloudChatErrorCode.RK_SDK_UNINIT == msg.arg1){
					RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_sdk_uninit));
				}else if(RKCloudChatErrorCode.RK_NOT_NETWORK == msg.arg1){
					RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_network_off));
				}else if(RKCloudChatErrorCode.CHAT_GROUP_UNAUTH_INVITE == msg.arg1){
					RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_manage_inviteauth_failed_unauth));
				}else {
					RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_operation_failed));
				}
			}
		}else if(RKCloudChatUiHandlerMessage.CALLBACK_GROUP_INFO_CHANGED == msg.what){
			if(mChatId.equalsIgnoreCase((String)msg.obj)){
				refreshGroupInfo();
			}
		}else if(RKCloudChatUiHandlerMessage.CALLBACK_ALL_GROUP_INFO_COMPLETE == msg.what){
			refreshGroupInfo();
		}else if(RKCloudChatUiHandlerMessage.CONTACTSINFO_CHANGED == msg.what){ // 联系人信息有变化
			List<String> accounts = (List<String>)msg.obj;
			if(null!=accounts && accounts.size()>0){
				boolean needReGet = false;
				for(String account : mDatas){
					if(accounts.contains(account)){
						needReGet = true;
						break;
					}
				}
				if(needReGet){
					startQuery(QUERY_TYPE_QUERY_CONTACTS);
				}
			}
			
		}else if(RKCloudChatUiHandlerMessage.CONTACT_HEADERIMAGE_CHANGED == msg.what){ // 联系人头像有变化
			String account = (String)msg.obj;
			for(String account1 : mDatas){
				if(account1.equalsIgnoreCase(account)){
					mContacts.put(account, mContactManager.getContactInfo(account));
					break;
				}
			}
		}
	}
	
	private class RKCloudChatGroupManageAdapter extends BaseAdapter {
		private ItemViewBuffer mItemBuffer;

		public RKCloudChatGroupManageAdapter() {
		}
		
		/*
		 * 定义每个条目中使用View的基本元素
		 */
		private class ItemViewBuffer {
            RoundedImageView headerImgView;// 头像
			TextView nameTV;// 名称	
			ImageView delIcon;

			public ItemViewBuffer(View convertView) {		
				headerImgView = (RoundedImageView) convertView.findViewById(R.id.headerimg);
				nameTV = (TextView) convertView.findViewById(R.id.name);
				delIcon = (ImageView)convertView.findViewById(R.id.delimg);
			}
		}
		
		public int getCount() {
			return mDatas.size();
		}

		public Object getItem(int arg0) {
			return mDatas.get(arg0);
		}

		public long getItemId(int arg0) {
			return arg0;
		}
		
		public void setDelStatus(boolean delete){
			mDelStatus = delete;
		}
		
		public View getView(int arg0, View convertView, ViewGroup arg2) {	
			if (null == convertView) {
				convertView = getLayoutInflater().inflate(R.layout.rkcloud_chat_manage_gridview_item, null);
				mItemBuffer = new ItemViewBuffer(convertView);
				convertView.setTag(mItemBuffer);
			} else {
				mItemBuffer = (ItemViewBuffer) convertView.getTag();
			}
			
			// 获取数据
			final String userAccount = mDatas.get(arg0);	
			if(userAccount.equalsIgnoreCase(RKCloudChatGroupManageActivity.USER_INVITE_FLAG)){ // 邀请成员的图标
				mItemBuffer.nameTV.setVisibility(View.INVISIBLE);
				mItemBuffer.delIcon.setVisibility(View.GONE);
				if(mDelStatus){
					mItemBuffer.headerImgView.setVisibility(View.INVISIBLE);
				}else{
					mItemBuffer.headerImgView.setVisibility(View.VISIBLE);
					mItemBuffer.headerImgView.setBackgroundResource(R.drawable.rkcloud_chat_img_add_user);
				}
				
			}else if(userAccount.equalsIgnoreCase(RKCloudChatGroupManageActivity.USER_KICK_FLAG)){ // 删除成员的图标
				mItemBuffer.nameTV.setVisibility(View.INVISIBLE);
				mItemBuffer.delIcon.setVisibility(View.GONE);
				
				mItemBuffer.headerImgView.setVisibility(View.VISIBLE);
				mItemBuffer.headerImgView.setBackgroundResource(R.drawable.rkcloud_chat_img_minus_user);
				
			}else{
				RKCloudChatContact contactObj = mContacts.get(userAccount);
				// 设置名称
				mItemBuffer.nameTV.setVisibility(View.VISIBLE);
				mItemBuffer.nameTV.setText(null!=contactObj ? contactObj.getShowName() : userAccount); 
				// 设置头像
				mItemBuffer.headerImgView.setVisibility(View.VISIBLE);
				mItemBuffer.headerImgView.setBackgroundResource(R.drawable.rkcloud_chat_img_header_default);
				if(null!=contactObj && !TextUtils.isEmpty(contactObj.getHeaderThumbImagePath())){
					RKCloudChatImageRequest imageReq = new RKCloudChatImageRequest(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG, contactObj.getHeaderThumbImagePath(), userAccount);
					// 如果在缓存中则直接设置图片
					RKCloudChatImageResult imgResult = RKCloudChatImageAsyncLoader.getInstance(RKCloudChatGroupManageActivity.this).sendPendingRequestQuryCache(imageReq);
					if(null!=imgResult && null!=imgResult.resource){
						mItemBuffer.headerImgView.setImageDrawable(imgResult.resource);
					}	
				}
				// 设置删除图标是否显示
				mItemBuffer.delIcon.setVisibility(!mDelStatus || userAccount.equalsIgnoreCase(mCurrAccount) ? View.GONE : View.VISIBLE);
			}
			return convertView;
		}
	}
}
