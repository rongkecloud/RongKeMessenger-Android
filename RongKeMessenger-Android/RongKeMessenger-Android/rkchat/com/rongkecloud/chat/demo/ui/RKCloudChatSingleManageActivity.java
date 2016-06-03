package com.rongkecloud.chat.demo.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rongkecloud.chat.RKCloudChatBaseChat;
import com.rongkecloud.chat.RKCloudChatErrorCode;
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
import com.rongkecloud.sdkbase.RKCloud;
import com.rongkecloud.test.R;
import com.rongkecloud.test.ui.widget.RoundedImageView;

/**
 * 单聊会话的管理页面
 * @author yangzht2008
 *
 */
public class RKCloudChatSingleManageActivity extends RKCloudChatBaseActivity implements OnClickListener, ImageLoadedCompleteDelayNotify{
	// 定义从其它UI返回的结果类型值
	private static final int INTENT_FORWARD_KEY_SELECT_USER = 1;// 选择成员
	public static final int INTENT_FORWARD_KEY_SET_BGIMG = 2;// 设置背景图片
	
	public static final String USER_INVITE_FLAG = "+1";// 邀请成员时的特殊号码
	
	// 需要从消息列表传递的参数项
	public static final String INTENT_SINGLE_CHATID = "single_chatid";
	
	private GridView mGridView;// 头像组件	
	private ImageView mSetTopImg;// 是否置顶聊天
	private ImageView mIsRemindImg; // 接收消息提醒复选框
	private LinearLayout mSetBgImgLayout;// 设置聊天背景
	private LinearLayout mClearMsgLayout;// 清空消息
	
	// 成员变量
	private String mChatId; // 会话ID
	private SingleChat mSingleChatObj; // 会话对象
	private String mCurrAccount;

	private List<String> mDatas;// 成员账号
	private Map<String, RKCloudChatContact> mContacts;// 联系人对象
	private RKCloudChatSingleManageAdapter mAdapter; // 适配器
	
	private RKCloudChatMmsManager mMmsManager; // 消息管理器对象
	private RKCloudChatContactManager mContactManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rkcloud_chat_manage_single);
		mChatId = getIntent().getStringExtra(INTENT_SINGLE_CHATID);
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
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		closeProgressDialog();
	}

	@Override
	public void onLoadImageCompleteDelayNotify(IMAGE_REQUEST_TYPE type, List<RKCloudChatImageResult> result) {
		if(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG == type){
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if(R.id.settop == id){ // 是否置顶
			boolean isSelected = mSetTopImg.isSelected();
			if(mMmsManager.setChatTop(mChatId, !isSelected) > 0){
				if (null != mSingleChatObj) {
					RKCloudChatBaseChat chatObj = mMmsManager.queryChat(mChatId);
					mSingleChatObj.copyData(chatObj);
				}
				mSetTopImg.setSelected(!isSelected);
			}
			
		}else if(R.id.isremind == id){ // 是否提醒
			boolean isSelected = mIsRemindImg.isSelected();
			if(mMmsManager.isRemindInGroup(mChatId, !isSelected) > 0){
				if (null != mSingleChatObj) {
					RKCloudChatBaseChat chatObj = mMmsManager.queryChat(mChatId);
					mSingleChatObj.copyData(chatObj);
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
								RKCloudChatTools.showToastText(RKCloudChatSingleManageActivity.this, getString(R.string.rkcloud_chat_operation_success));
							}else{
								RKCloudChatTools.showToastText(RKCloudChatSingleManageActivity.this, getString(R.string.rkcloud_chat_operation_failed));
							}
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
				userAccounts.add(mChatId);
				mMmsManager.showGroupNameDialog(this, userAccounts);
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
		TextView titleTV = (TextView)findViewById(R.id.txt_title);
		titleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		titleTV.setOnClickListener(mExitListener);
		titleTV.setText(R.string.bnt_return);

        TextView text_title_content = (TextView)findViewById(R.id.text_title_content);
        text_title_content.setText(getString(R.string.rkcloud_chat_manage_title, 2));

		mGridView = (GridView) findViewById(R.id.gridview);
		mSetTopImg = (ImageView) findViewById(R.id.settop);
		mIsRemindImg = (ImageView) findViewById(R.id.isremind);
		mSetBgImgLayout = (LinearLayout) findViewById(R.id.layout_setbgimg);
		mClearMsgLayout = (LinearLayout) findViewById(R.id.layout_clearmsg);
				
		// 设置监听
		mSetTopImg.setOnClickListener(this);
		mIsRemindImg.setOnClickListener(this);
		mSetBgImgLayout.setOnClickListener(this);
		mClearMsgLayout.setOnClickListener(this);
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (position >= mDatas.size()) {
					return;
				}
				
				String userAccount = mDatas.get(position);
				if (userAccount.equals(USER_INVITE_FLAG)) {
					Intent intent = new Intent(RKCloudChatSingleManageActivity.this, RKCloudChatSelectUsersActivity.class);
					StringBuffer existAccounts = new StringBuffer();
					for(String account : mDatas){
						if(USER_INVITE_FLAG.equals(account)){
							continue;
						}
						existAccounts.append(account).append(",");
					}
					if(existAccounts.length() > 0){
						existAccounts.deleteCharAt(existAccounts.length()-1);
					}
					intent.putExtra(RKCloudChatSelectUsersActivity.INTENT_KEY_EXIST_ACCOUNTS, existAccounts.toString());
					startActivityForResult(intent, INTENT_FORWARD_KEY_SELECT_USER);
					
				}else{
					mContactManager.jumpContactDetailInfoUI(RKCloudChatSingleManageActivity.this, userAccount);
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
		mDatas.add(mChatId);
		mDatas.add(USER_INVITE_FLAG);
		
		mContacts = new HashMap<String, RKCloudChatContact>();
		
		mAdapter = new RKCloudChatSingleManageAdapter();
		mGridView.setAdapter(mAdapter);
		
		// 初始化会话对象
		RKCloudChatBaseChat chatObj = mMmsManager.queryChat(mChatId);
		if(null == chatObj){
			chatObj = SingleChat.buildSingleChat(mChatId);
		}else{
			// 如果不是单聊会话则结束 
			if(!(chatObj instanceof SingleChat)){
				finish();
				return;
			}
		}
		
		mSingleChatObj = (SingleChat)chatObj;
		// 设置提醒方式
		mSetTopImg.setSelected(mSingleChatObj.isTop());
		mIsRemindImg.setSelected(mSingleChatObj.getRemindStatus());
		// 刷新联系人信息
		refreshContactInfo();
	}
	
	/*
	 * 刷新联系人信息
	 */
	private void refreshContactInfo(){
		List<String> accounts = new ArrayList<String>();
		for(String account : mDatas){
			if(USER_INVITE_FLAG.equals(account)){
				continue;
			}
			accounts.add(account);
		}
		Map<String, RKCloudChatContact> datas = mContactManager.getContactInfos(accounts);
		mContacts.clear();
		if(null!=datas && datas.size()>0){
			mContacts.putAll(datas);
		}
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void processResult(Message msg) {
		int what = msg.what;
		if(RKCloudChatUiHandlerMessage.RESPONSE_APPLY_GROUP == what){ // 申请群
			closeProgressDialog();			
			if(0 == msg.arg1){
				mMmsManager.enterMsgListActivity((String)msg.obj);
				finish();
				
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
					refreshContactInfo();
				}
			}
			
		}else if(RKCloudChatUiHandlerMessage.CONTACT_HEADERIMAGE_CHANGED == msg.what){ // 联系人头像有变化
			String account = (String)msg.obj;
			for(String account1 : mDatas){
				if(account1.equalsIgnoreCase(account)){
					mContacts.put(account, mContactManager.getContactInfo(account));
				}
			}
		}else if(RKCloudChatUiHandlerMessage.DELETE_SINGLE_CHAT==msg.what){ // 会话被删除
			if (mChatId.equalsIgnoreCase((String) msg.obj)) {
				finish();
			}
			
		}
	}
	
	private class RKCloudChatSingleManageAdapter extends BaseAdapter {
		private ItemViewBuffer mItemBuffer;

		public RKCloudChatSingleManageAdapter() {
		}
		
		/*
		 * 定义每个条目中使用View的基本元素
		 */
		private class ItemViewBuffer {
			RoundedImageView headerImgView;// 头像
			TextView nameTV;// 名称	

			public ItemViewBuffer(View convertView) {		
				headerImgView = (RoundedImageView) convertView.findViewById(R.id.headerimg);
				nameTV = (TextView) convertView.findViewById(R.id.name);
				// 删除条目隐藏
				ImageView delIcon = (ImageView)convertView.findViewById(R.id.delimg);
				delIcon.setVisibility(View.GONE);
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
			if(userAccount.equals(RKCloudChatSingleManageActivity.USER_INVITE_FLAG)){ // 邀请成员的图标
				// 名称不可见
				mItemBuffer.nameTV.setVisibility(View.INVISIBLE);
				// 显示邀请图标
                mItemBuffer.headerImgView.setBackgroundResource(R.drawable.rkcloud_chat_img_add_user);
			}else{
				RKCloudChatContact contactObj = mContacts.get(userAccount);
				// 设置名称
				mItemBuffer.nameTV.setVisibility(View.VISIBLE);
				mItemBuffer.nameTV.setText(null!=contactObj ? contactObj.getShowName() : userAccount); 
				// 设置头像
				mItemBuffer.headerImgView.setBackgroundResource(R.drawable.rkcloud_chat_img_header_default);
				if(null!=contactObj && !TextUtils.isEmpty(contactObj.getHeaderThumbImagePath())){
					RKCloudChatImageRequest imageReq = new RKCloudChatImageRequest(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG, contactObj.getHeaderThumbImagePath(), userAccount);
					// 如果在缓存中则直接设置图片
					RKCloudChatImageResult imgResult = RKCloudChatImageAsyncLoader.getInstance(RKCloudChatSingleManageActivity.this).sendPendingRequestQuryCache(imageReq);
					if(null!=imgResult && null!=imgResult.resource){
						mItemBuffer.headerImgView.setImageDrawable(imgResult.resource);
					}	
				}
			}
			return convertView;
		}
	}
}
