package com.rongkecloud.chat.demo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.RadioGroup.OnCheckedChangeListener;
import com.rongkecloud.chat.GroupChat;
import com.rongkecloud.chat.RKCloudChatErrorCode;
import com.rongkecloud.chat.demo.RKCloudChatMmsManager;
import com.rongkecloud.chat.demo.RKCloudChatUiHandlerMessage;
import com.rongkecloud.chat.demo.tools.RKCloudChatTools;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.test.R;

import java.util.ArrayList;
import java.util.List;

public class RKCloudChatGroupListActivity extends RKCloudChatBaseActivity{
	// 获取其它UI返回的结果类型
	private static final int INTENT_FORWARD_KEY_SELECT_USER = 1;// 选择成员
	
	// 查询类型
	private static final int QUERY_TYPE_GET_MY_CREATED_GROUPS = 0;// 查询我创建的群
	private static final int QUERY_TYPE_GET_MY_ATTENDED_GROUPS = 1;// 查询我参与的群
	
	private static final int TAB_MYCREATED = 1;
	private static final int TAB_MYATTENDED = 2;
	
	// UI组件
	private ImageButton mSelectUserBtn;
	private RadioGroup mRadioGroup;
	private RadioButton mMyCreatedBnt, mMyAttendedBnt;
	private ListView mListView;
	private TextView mNoDataTV; 
	private ProgressBar mLoadingPB;
	
	// 成员变量
	private RKCloudChatMmsManager mMmsManager;
	
	private List<GroupChat> mMyCreatedGroups;// 我创建的群
	private List<GroupChat> mMyAttendedGroups;// 我参与的群
	
	private RKCloudChatGroupListAdapter mAdapter;
	private List<GroupChat> mAllDatas;
	
	private QueryHandlerThread mQuerThread;
	private int mCurrTab;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rkcloud_chat_grouplist);
		
		initUIAndListener();
		// 初始化数据
		mMmsManager = RKCloudChatMmsManager.getInstance(this);
		mCurrTab = TAB_MYCREATED;
		mRadioGroup.check(R.id.mycreated);
		
		mMyCreatedGroups = new ArrayList<GroupChat>();
		mMyAttendedGroups = new ArrayList<GroupChat>();
		
		mAllDatas = new ArrayList<GroupChat>();
		mAdapter = new RKCloudChatGroupListAdapter();
		mListView.setAdapter(mAdapter);
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mMmsManager.bindUiHandler(mUiHandler);	
		startQuery(QUERY_TYPE_GET_MY_CREATED_GROUPS);
		startQuery(QUERY_TYPE_GET_MY_ATTENDED_GROUPS);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		closeProgressDialog();
	}
	
	@Override
	protected void onDestroy() {
		if (null != mQuerThread) {
			mQuerThread.quit();
			mQuerThread = null;
		}
		super.onDestroy();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (null==data || RESULT_OK!=resultCode) {
			return;
		}
		switch(requestCode){
		case INTENT_FORWARD_KEY_SELECT_USER:
			String accounts = data.getStringExtra(RKCloudChatSelectUsersActivity.INTENT_RETURN_KEY_SELECTED_ACCOUNTS);
			if(TextUtils.isEmpty(accounts)){
				return;
			}
			List<String> userAccounts = RKCloudChatTools.splitStrings(accounts);
			if(1 == userAccounts.size()){ 
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_creategroup_failed_usercountnotenough));
			}else if(userAccounts.size() >= 2){
				mMmsManager.showGroupNameDialog(this, userAccounts);
			}
			
			break;
		}
	}
	
	private void initUIAndListener(){
		// 设置title
		TextView titleTV = (TextView)findViewById(R.id.txt_title);
		titleTV.setText(R.string.bnt_return);
		titleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		titleTV.setOnClickListener(mExitListener);

        TextView text_title_content = (TextView)findViewById(R.id.text_title_content);
        text_title_content.setText(R.string.contact_group_list);

		// 显示创建会话的图标
		mSelectUserBtn = (ImageButton)findViewById(R.id.title_imgbtns_rightbtn);
		mSelectUserBtn.setImageResource(R.drawable.rkcloud_chat_img_add);
		mSelectUserBtn.setVisibility(View.GONE);
		mSelectUserBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(RKCloudChatGroupListActivity.this, RKCloudChatSelectUsersActivity.class), INTENT_FORWARD_KEY_SELECT_USER);
			}
		});
		
		// 初始化UI元素
		mRadioGroup = (RadioGroup)findViewById(R.id.radiogroup);
		mMyCreatedBnt = (RadioButton)findViewById(R.id.mycreated);
		mMyAttendedBnt = (RadioButton)findViewById(R.id.myattended);
		
		mListView = (ListView) findViewById(R.id.listview);
		mNoDataTV = (TextView) findViewById(R.id.emptytv);
		mLoadingPB = (ProgressBar) findViewById(R.id.loading_progressbar);
		mLoadingPB.setVisibility(View.VISIBLE);
		
		// 监听器
		mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if((R.id.mycreated==checkedId && TAB_MYCREATED==mCurrTab)
						|| (R.id.myattended==checkedId && TAB_MYATTENDED==mCurrTab)){
					return;
				}
				
				switch(checkedId){
				case R.id.mycreated:
					mCurrTab = TAB_MYCREATED;
					break;
					
				case R.id.myattended:
					mCurrTab = TAB_MYATTENDED;
					break;
				}
				showDatas();
			}
		});
		
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (null == mAllDatas || arg2 >= mAllDatas.size()) {
					return;
				}
				GroupChat groupObj = mAllDatas.get(arg2);
				mMmsManager.enterMsgListActivity(groupObj.getChatId());
			}
		});
	}
		
	/*
	 * 查询数据
	 */
	private void startQuery(int queryType) {
		if (null == mQuerThread) {
			mQuerThread = new QueryHandlerThread("QueryGroupListActivityThread");
			mQuerThread.start();
		}
		mQuerThread.startQuery(queryType);
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
				msg.sendToTarget();
			}
		}

		@Override
		public boolean handleMessage(Message msg) {
			if(QUERY_TYPE_GET_MY_CREATED_GROUPS == msg.what){
				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.GET_ALL_MY_CREATED_GROUPS_FINISHED;
				message.obj = mMmsManager.queryMyCreatedGroups();
				message.sendToTarget();
				
			}else if(QUERY_TYPE_GET_MY_ATTENDED_GROUPS == msg.what){
				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.GET_ALL_MY_ATTENDED_GROUPS_FINISHED;
				message.obj = mMmsManager.queryAllMyAttendedGroups();
				message.sendToTarget();
			}
			return true;
		}
	}
	
	
	private void showDatas(){
		mAllDatas.clear();
		if(TAB_MYCREATED == mCurrTab){
			if(mMyCreatedGroups.size() > 0){
				mAllDatas.addAll(mMyCreatedGroups);
			}
		}else if(TAB_MYATTENDED == mCurrTab){
			if(mMyAttendedGroups.size() > 0){
				mAllDatas.addAll(mMyAttendedGroups);
			}
		}
		
		if(0 == mAllDatas.size()){
			mNoDataTV.setVisibility(View.VISIBLE);
			mListView.setVisibility(View.GONE);
			
		}else{
			mNoDataTV.setVisibility(View.GONE);
			mListView.setVisibility(View.VISIBLE);
		}
				
		mLoadingPB.setVisibility(View.GONE);
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void processResult(Message msg) {
		if(RKCloudChatUiHandlerMessage.GET_ALL_MY_CREATED_GROUPS_FINISHED == msg.what){ 
			mMyCreatedGroups.clear();
			List<GroupChat> objs = (List<GroupChat>) msg.obj;
			if(null!=objs && objs.size()>0){
				mMyCreatedGroups.addAll(objs);
			}
			showDatas();
			
		}else if(RKCloudChatUiHandlerMessage.GET_ALL_MY_ATTENDED_GROUPS_FINISHED == msg.what){
			mMyAttendedGroups.clear();
			List<GroupChat> objs = (List<GroupChat>) msg.obj;
			if(null!=objs && objs.size()>0){
				mMyAttendedGroups.addAll(objs);
			}
			showDatas();
			
		}else if(RKCloudChatUiHandlerMessage.CALLBACK_GROUP_INFO_CHANGED == msg.what
				|| RKCloudChatUiHandlerMessage.CALLBACK_MODIFY_GROUP_NAME == msg.what
				|| RKCloudChatUiHandlerMessage.CALLBACK_MODIFY_GROUP_DESC == msg.what
				|| RKCloudChatUiHandlerMessage.RESPONSE_GROUP_POPULATION_CHANGED == msg.what
				|| RKCloudChatUiHandlerMessage.RESPONSE_MODIFY_GROUP_INVITEAUTH == msg.what
				|| RKCloudChatUiHandlerMessage.RESPONSE_QUIT_GROUP == msg.what
				|| RKCloudChatUiHandlerMessage.CALLBACK_KICKOUT == msg.what
				|| RKCloudChatUiHandlerMessage.CALLBACK_GROUP_DISSOLVED == msg.what
				|| RKCloudChatUiHandlerMessage.DELETE_SINGLE_CHAT == msg.what
				|| RKCloudChatUiHandlerMessage.CALLBACK_ALL_GROUP_INFO_COMPLETE == msg.what){
			startQuery(QUERY_TYPE_GET_MY_CREATED_GROUPS);
			startQuery(QUERY_TYPE_GET_MY_ATTENDED_GROUPS);
			
		}else if(RKCloudChatUiHandlerMessage.RESPONSE_APPLY_GROUP == msg.what){ // 申请群
			closeProgressDialog();
			if(0 == msg.arg1){
				startQuery(QUERY_TYPE_GET_MY_CREATED_GROUPS);
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_creategroup_success));
				
			}else if(RKCloudChatErrorCode.RK_SDK_UNINIT == msg.arg1){
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_sdk_uninit));
				
			}else if(RKCloudChatErrorCode.RK_NOT_NETWORK == msg.arg1){
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_network_off));
				
			}else if(RKCloudChatErrorCode.CHAT_GROUP_USER_NUMBER_EXCEED_LIMIT == msg.arg1){
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_groupusers_count_byond));
				
			}else if(RKCloudChatErrorCode.CHAT_GROUP_COUNT_EXCEED_LIMIT == msg.arg1){
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_groups_count_byond));
				
			}else if(RKCloudChatErrorCode.RK_INVALID_USER == msg.arg1){
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_illegal_users, msg.obj));
				
			}else {
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_operation_failed));
				
			}
			
		}
	}
	
	private class RKCloudChatGroupListAdapter extends BaseAdapter {
		private ItemViewBuffer mItemBuffer;

		public RKCloudChatGroupListAdapter() {			
		}
		
		@Override
		public int getCount() {
			return mAllDatas.size();
		}

		@Override
		public Object getItem(int arg0) {
			return mAllDatas.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		/*
		 * 定义每个条目中使用View的基本元素
		 */
		private class ItemViewBuffer {
			TextView nameTV; 
			
			public ItemViewBuffer(View convertView) {
				nameTV = (TextView) convertView.findViewById(R.id.name);
			}
		}

		@Override
		public View getView(int arg0, View convertView, ViewGroup arg2) {
			if (null == convertView) {
				convertView = getLayoutInflater().inflate(R.layout.rkcloud_chat_grouplist_item, null);
				mItemBuffer = new ItemViewBuffer(convertView);
				convertView.setTag(mItemBuffer);
			} else {
				mItemBuffer = (ItemViewBuffer) convertView.getTag();
			}

			// 获取会话数据
			GroupChat chatObj = mAllDatas.get(arg0);
			String convName = String.format("%s(%d)", chatObj.getChatShowName(), chatObj.getUserCounts());
			mItemBuffer.nameTV.setText(convName);
			
			return convertView;
		}
	}

}
