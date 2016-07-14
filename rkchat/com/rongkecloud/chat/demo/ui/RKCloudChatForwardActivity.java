package com.rongkecloud.chat.demo.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.RadioGroup.OnCheckedChangeListener;
import com.rongkecloud.chat.*;
import com.rongkecloud.chat.demo.RKCloudChatConstants;
import com.rongkecloud.chat.demo.RKCloudChatContactManager;
import com.rongkecloud.chat.demo.RKCloudChatMmsManager;
import com.rongkecloud.chat.demo.RKCloudChatUiHandlerMessage;
import com.rongkecloud.chat.demo.entity.RKCloudChatContact;
import com.rongkecloud.chat.demo.tools.RKCloudChatImageTools;
import com.rongkecloud.chat.demo.tools.RKCloudChatSDCardTools;
import com.rongkecloud.chat.demo.tools.RKCloudChatTools;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageAsyncLoader;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageAsyncLoader.ImageLoadedCompleteDelayNotify;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest.IMAGE_REQUEST_TYPE;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageResult;
import com.rongkecloud.test.R;
import com.rongkecloud.test.ui.widget.RoundedImageView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RKCloudChatForwardActivity extends RKCloudChatBaseActivity implements ImageLoadedCompleteDelayNotify{
	private static final int MAX_COUNT_PRE_SHARE = 10;// 每次允许最多上传的个数
	// 定义此页面使用的功能类型
	public static final int FUNC_FORWARD = 1;// 转发
	public static final int FUNC_INSIDE_SHARE = 2; // 内部分享
	public static final int FUNC_OUTSIDE_SHARE = 3;// 通过第三方分享
	
	public static final String INTENT_KEY_FUNC_TYPE = "func_type";// 使用的功能类型
	public static final String INTENT_KEY_MSGSERIALNUM = "forward_msgserialnum";
	public static final String INTENT_KEY_OUTSIDE_SHARE_ISMUTIL = "key.outside.share.ismutil";// 是否为多存图片分享时使用到的key
	
	// 分享类型
	private static final int SHARE_TYPE_IMAGE = 1;// 图片分享
	
	// 获取其它UI返回的结果类型
	private static final int INTENT_FORWARD_KEY_SELECT_USER = 1;// 选择成员
		
	// 查询类型
	private static final int QUERY_TYPE_GET_EXISTCHATS = 1;// 查询存在的所有会话
	private static final int QUERY_TYPE_GET_ALLCONTACTS = 2;// 查询所有联系人
	private static final int QUERY_TYPE_SEARCH_CHAT = 3;// 搜索会话
	private static final int QUERY_TYPE_SEARCH_CONTACT = 4;// 搜索联系人
	// tab类型
	private static final int TAB_EXISTCHAT = 1;
	private static final int TAB_ADDRESS = 2;
	// 新建会话的标识
	private static final String CREATE_NEWCHAT = "create_newchat";
	// UI组件
	private RadioGroup mRadioGroup;
	private RadioButton mExistChatBnt, mAddressBnt;
	private EditText mSearchET;
	private TextView mNoDataTV;
	private ProgressBar mLoadingPB;
	private ListView mListView;
	
	// 成员变量
	private int mCurrFunc;
	private String mMsgSerialNum;
	private boolean mIsMutilShareImg = false;// 是否是多个图片的分享
	private List<String> mOutSideShareFilePaths = new ArrayList<String>();// 记录外部分享时的文件路径，目前只能分享图片
	private int mOutSideShareType = SHARE_TYPE_IMAGE;// 分享类型 ，默认为图片分享

	private RKCloudChatMmsManager mMmsManager;
	private RKCloudChatContactManager mContactManager;
	private List<RKCloudChatContact> mAllContactDatas;// 所有联系人
	private List<RKCloudChatContact> mSearchContactDatas;
	private List<RKCloudChatBaseChat> mAllExistChatDatas;// 所有存在的会话
	private List<RKCloudChatBaseChat> mSearchChatDatas;
	private RKCloudChatForwardAdapter mAdapter;
	private QueryHandlerThread mQuerThread;
	private int mCurrTab;
	private boolean mNeedGetDatas = false;
	private BackgroundColorSpan backgroundColorSpan;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rkcloud_chat_forward);
		mCurrFunc = getIntent().getIntExtra(INTENT_KEY_FUNC_TYPE, FUNC_OUTSIDE_SHARE);
		if(FUNC_FORWARD!=mCurrFunc && FUNC_INSIDE_SHARE!=mCurrFunc && FUNC_OUTSIDE_SHARE!=mCurrFunc){
			RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_sdk_uninit));
			finish();
			return;
		}
		
		if(FUNC_OUTSIDE_SHARE == mCurrFunc){
			mIsMutilShareImg = getIntent().getBooleanExtra(INTENT_KEY_OUTSIDE_SHARE_ISMUTIL, false);
			processOutSideShareIntent(getIntent());
			
		}else if(FUNC_INSIDE_SHARE==mCurrFunc || FUNC_FORWARD==mCurrFunc){
			mMsgSerialNum = getIntent().getStringExtra(INTENT_KEY_MSGSERIALNUM);
			if(TextUtils.isEmpty(mMsgSerialNum)){
				finish();
				return;
			}
		}
		
		initUIAndListeners();
		initDatas();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mMmsManager.bindUiHandler(mUiHandler);	
		mContactManager.bindUiHandler(mUiHandler);	
		RKCloudChatImageAsyncLoader.getInstance(this).registerDelayListener(this);
		
		if(mNeedGetDatas){
			mLoadingPB.setVisibility(View.VISIBLE);
			startQuery(QUERY_TYPE_GET_ALLCONTACTS);
			startQuery(QUERY_TYPE_GET_EXISTCHATS);
			mNeedGetDatas = false;
		}
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
				for(RKCloudChatContact obj : mAllContactDatas){
					if(obj.rkAccount.equalsIgnoreCase(userAccounts.get(0))){
						showConfirmOpe(userAccounts.get(0), obj.getShowName());
						break;
					}
				}
				
			}else if(userAccounts.size() >= 2){
				mMmsManager.showGroupNameDialog(this, userAccounts);
			}
			break;
		}
	}
	
	@Override
	public void onLoadImageCompleteDelayNotify(IMAGE_REQUEST_TYPE type, List<RKCloudChatImageResult> result) {
		if(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG == type){
			mAdapter.notifyDataSetChanged();
		}
	}
	
	private void initUIAndListeners(){
		// 设置title
		TextView titleTV = (TextView)findViewById(R.id.txt_title);
		titleTV.setText(R.string.rkcloud_chat_forwardmsg_title);
		titleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		titleTV.setOnClickListener(mExitListener);
		
		mRadioGroup = (RadioGroup)findViewById(R.id.radiogroup);
		mExistChatBnt = (RadioButton)findViewById(R.id.existchats);
		mAddressBnt = (RadioButton)findViewById(R.id.address);
		mSearchET = (EditText)findViewById(R.id.searchedittext);
		mListView = (ListView) findViewById(R.id.listview);
		mLoadingPB = (ProgressBar) findViewById(R.id.loading_progressbar);
		mNoDataTV = (TextView)findViewById(R.id.emptytv);
		
		mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if((R.id.existchats==checkedId && TAB_EXISTCHAT==mCurrTab)
						|| (R.id.address==checkedId && TAB_ADDRESS==mCurrTab)){
					return;
				}
				
				switch(checkedId){
				case R.id.existchats:
					mCurrTab = TAB_EXISTCHAT;
					startQuery(QUERY_TYPE_SEARCH_CHAT);
					break;
					
				case R.id.address:
					mCurrTab = TAB_ADDRESS;
					startQuery(QUERY_TYPE_SEARCH_CONTACT);
					break;
				}
			}
		});
		
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(TAB_EXISTCHAT == mCurrTab){
					RKCloudChatBaseChat obj = mSearchChatDatas.get(position);
					if(CREATE_NEWCHAT.equalsIgnoreCase(obj.getChatId())){
						// 创建新的群
						startActivityForResult(new Intent(RKCloudChatForwardActivity.this, RKCloudChatSelectUsersActivity.class), INTENT_FORWARD_KEY_SELECT_USER);
					}else{
						String name = null;
						if(obj instanceof SingleChat){
							for(RKCloudChatContact cObj : mAllContactDatas){
								if(cObj.rkAccount.equalsIgnoreCase(obj.getChatId())){
									name = cObj.getShowName();
									break;
								}
							}
						}
						if(null == name){
							name = obj.getChatShowName();
						}
						showConfirmOpe(obj.getChatId(), name);
					}
					
				}else if(TAB_ADDRESS == mCurrTab){
					RKCloudChatContact obj = mSearchContactDatas.get(position);
					showConfirmOpe(obj.rkAccount, obj.getShowName());
				}
			}
		});
		
		mSearchET.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				
			}

			@Override
			public void afterTextChanged(Editable s) {
				switch (mCurrTab) {
				case TAB_EXISTCHAT:
					startQuery(QUERY_TYPE_SEARCH_CHAT);
					break;

				case TAB_ADDRESS:
					startQuery(QUERY_TYPE_SEARCH_CONTACT);
					break;
				}
			}
		});
	}
	
	private void initDatas(){
		mMmsManager = RKCloudChatMmsManager.getInstance(this);
		mContactManager = RKCloudChatContactManager.getInstance(this);
		
		mAllContactDatas = new ArrayList<RKCloudChatContact>();
		mSearchContactDatas = new ArrayList<RKCloudChatContact>();
		mAllExistChatDatas = new ArrayList<RKCloudChatBaseChat>();
		mSearchChatDatas = new ArrayList<RKCloudChatBaseChat>();
		
		mAdapter = new RKCloudChatForwardAdapter();
		mListView.setAdapter(mAdapter);
		
		mCurrTab = TAB_EXISTCHAT;
		mRadioGroup.check(R.id.existchats);
		
		backgroundColorSpan = new BackgroundColorSpan(getResources().getColor(R.color.rkcloud_chat_search_result_highlightcolor));
		mNeedGetDatas = true;
	}
	
	private void showConfirmOpe(final String chatId, String showName){
		int contentId = mCurrFunc==FUNC_FORWARD ? R.string.rkcloud_chat_forwardmsg_confirmmsg : R.string.rkcloud_chat_sharemsg_confirmmsg;
		AlertDialog.Builder dialog = new AlertDialog.Builder(this)
		.setTitle(R.string.rkcloud_chat_tip)
		.setMessage(getString(contentId, showName))
		.setNegativeButton(R.string.rkcloud_chat_btn_cancel, null)
		.setPositiveButton(R.string.rkcloud_chat_btn_confirm, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {				
				if(mCurrFunc == FUNC_FORWARD || mCurrFunc == FUNC_INSIDE_SHARE){
					showProgressDialog();
					mMmsManager.forwardMms(mMsgSerialNum, chatId);
				}else{
					if (null != mOutSideShareFilePaths && mOutSideShareFilePaths.size() > 0) {
						if (mOutSideShareType == SHARE_TYPE_IMAGE) {
							for (String path : mOutSideShareFilePaths) {
								ImageMessage body = ImageMessage.buildMsg(chatId, path);
								mMmsManager.sendMms(body);
							}
						}
						// TODO 跳转到聊天页面，也可以用户自己定义去向
						mMmsManager.enterMsgListActivity(chatId);
						finish();
					}
				}
			}
		});

		dialog.show();
	}
	
	/*
	 * 外部分享时处理获取的intent内容
	 */
	private void processOutSideShareIntent(Intent intent) {
		String mimeType = intent.getType();
		Bundle extras = intent.getExtras();

		if (mimeType.startsWith("image")) {
			mOutSideShareType = SHARE_TYPE_IMAGE;
			// 获取图片的Uri
			final List<Uri> arrayUris = new ArrayList<Uri>();
			if (!mIsMutilShareImg) {
				Uri data = extras.getParcelable(Intent.EXTRA_STREAM);
				arrayUris.add(data);
			} else {
				List<Uri> datas = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
				arrayUris.addAll(datas);
			}

			if (null == arrayUris || arrayUris.size() == 0) {
				finish();
				return;
			}
			// 一次性允许上传的张数
			if(arrayUris.size() > MAX_COUNT_PRE_SHARE){
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_sharemsg_imgcnt_beyond, MAX_COUNT_PRE_SHARE));
				finish();
				return;
			}

			// sd卡不存在
			if (!RKCloudChatSDCardTools.getExternalStorageCard()) {
				closeProgressDialog();
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_sdcard_unvalid));
				finish();
				return;
			}

			// sd卡已满或者不可用的错误
			if (!RKCloudChatSDCardTools.diskSpaceAvailable()) {
				closeProgressDialog();
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_sdcard_full));
				finish();
				return;
			}

			// 处理图片时显示等待对话框
			showProgressDialog();
			new Thread(new Runnable() {
				@Override
				public void run() {
					int totalSize = arrayUris.size();
					// 生成新的图片路径
					RKCloudChatTools.createDirectory(RKCloudChatConstants.MMS_TEMP_PATH);
					for (Uri data : arrayUris) {
						final String imagePath = RKCloudChatTools.getChoosePicturePath(RKCloudChatForwardActivity.this, data);
						// 如果图片路径为空，或者图片不存在，则跳过
						if (TextUtils.isEmpty(imagePath) || !new File(imagePath).exists()) {
							break;
						}
						// 获取压缩后的Bitmap对象
						Bitmap newImageMap = RKCloudChatImageTools.resizeBitmap(imagePath, RKCloudChatConstants.IMAGE_DEFAULT_WIDTH, RKCloudChatConstants.IMAGE_DEFAULT_HEIGHT);
						if (null == newImageMap) {
							break;
						}
						
						String tempImgName = String.format("%stempimage_%d.jpg", RKCloudChatConstants.MMS_TEMP_PATH, System.currentTimeMillis());
						// 保存压缩后的图片
						File imageFile = null;
						try {
							imageFile = RKCloudChatImageTools.compressBitmap(newImageMap, tempImgName);
						} catch (IOException e) {
						}

						// 回收Bitmap对象
						newImageMap.recycle();

						if (null == imageFile) {
							break;
						}
						mOutSideShareFilePaths.add(imageFile.getAbsolutePath());
					}

					if (mOutSideShareFilePaths.size() == totalSize) {
						mUiHandler.sendEmptyMessage(RKCloudChatUiHandlerMessage.IMAGE_COMPRESS_SUCCESS);
					} else {
						mUiHandler.sendEmptyMessage(RKCloudChatUiHandlerMessage.IMAGE_COMPRESS_FAILED);
					}
					
				}
				
			}).start();
		}
	}
	
	/*
	 * 查询数据
	 */
	private void startQuery(int queryType) {
		if (null == mQuerThread) {
			mQuerThread = new QueryHandlerThread("QueryForwardActivityThread");
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
				if(QUERY_TYPE_SEARCH_CHAT == queryType){
					List<RKCloudChatBaseChat> chatDatas = new ArrayList<RKCloudChatBaseChat>();
					if(mAllExistChatDatas.size() > 0){
						chatDatas.addAll(mAllExistChatDatas);
					}
					msg.obj = chatDatas;
				}else if(QUERY_TYPE_SEARCH_CONTACT == queryType){
					List<RKCloudChatContact> datas = new ArrayList<RKCloudChatContact>();
					if(mAllContactDatas.size() > 0){
						datas.addAll(mAllContactDatas);
					}
					msg.obj = datas;
				}
				msg.sendToTarget();
			}
		}

		@Override
		public boolean handleMessage(Message msg) {
			if(QUERY_TYPE_GET_EXISTCHATS == msg.what){
				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.FORWARD_GET_ALLCHATS_FINISH;
				message.obj = mMmsManager.queryAllChats();
				message.sendToTarget();
				
			}else if(QUERY_TYPE_GET_ALLCONTACTS == msg.what){
				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.FORWARD_GET_ALLCONTACTS_FINISH;
				message.obj = mContactManager.getAllContacts();
				message.sendToTarget();
				
			}else if(QUERY_TYPE_SEARCH_CHAT == msg.what){
				List<RKCloudChatBaseChat> chatDatas = (List<RKCloudChatBaseChat>)msg.obj;
				String filter = mSearchET.getText().toString().trim();
				List<RKCloudChatBaseChat> datas = new ArrayList<RKCloudChatBaseChat>();
				if(!TextUtils.isEmpty(filter)){
					for(RKCloudChatBaseChat obj : chatDatas){
						if(obj instanceof SingleChat){
							for(RKCloudChatContact contactObj : mAllContactDatas){
								if(contactObj.rkAccount.equalsIgnoreCase(obj.getChatId())){
									((SingleChat)obj).setContactShowName(contactObj.getShowName());
									break;
								}
							}
						}
						obj.matchName(filter, backgroundColorSpan);
						if(null != obj.highLightName){
							datas.add(obj);
						}
					}
					
				}else{
					datas.add(SingleChat.buildSingleChat(CREATE_NEWCHAT));
					for(RKCloudChatBaseChat obj : chatDatas){
						obj.highLightName = null;
						datas.add(obj);
					}
				}
				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.FORWARD_QUERY_ALLCHATS_FINISH;
				message.obj = datas;
				message.sendToTarget();
				
			}else if(QUERY_TYPE_SEARCH_CONTACT == msg.what){
				List<RKCloudChatContact> allDatas = (ArrayList<RKCloudChatContact>)msg.obj;
				String filter = mSearchET.getText().toString().trim();
				List<RKCloudChatContact> datas = new ArrayList<RKCloudChatContact>();
				if(!TextUtils.isEmpty(filter)){
					for(RKCloudChatContact obj : allDatas){
						obj.matchName(filter, backgroundColorSpan);
						if(null != obj.highLightName){
							datas.add(obj);
						}
					}
				}else{
					for(RKCloudChatContact obj : allDatas){
						obj.highLightName = null;
						datas.add(obj);
					}
				}
				
				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.FORWARD_QUERY_ALLCONTACTS_FINISH;
				message.obj = datas;
				message.sendToTarget();
			}
			return true;
		}
	}

	@Override
	public void processResult(Message msg) {
		if(RKCloudChatUiHandlerMessage.FORWARD_GET_ALLCHATS_FINISH == msg.what){
			mAllExistChatDatas.clear();
			List<RKCloudChatBaseChat> objs = (List<RKCloudChatBaseChat>) msg.obj;
			if(null!=objs && objs.size()>0){
				mAllExistChatDatas.addAll(objs);
			}
			if(TAB_EXISTCHAT == mCurrTab){
				startQuery(QUERY_TYPE_SEARCH_CHAT);
			}
			
		}else if(RKCloudChatUiHandlerMessage.FORWARD_GET_ALLCONTACTS_FINISH == msg.what){
			mAllContactDatas.clear();
			List<RKCloudChatContact> objs = (List<RKCloudChatContact>)msg.obj;
			if(null!=objs && objs.size()>0){
				mAllContactDatas.addAll(objs);
			}
			
			if(TAB_ADDRESS == mCurrTab){
				startQuery(QUERY_TYPE_SEARCH_CONTACT);
			}else{
				mAdapter.notifyDataSetChanged();
			}
			
		}else if(RKCloudChatUiHandlerMessage.FORWARD_QUERY_ALLCHATS_FINISH == msg.what){
			mSearchChatDatas.clear();
			List<RKCloudChatBaseChat> objs = (List<RKCloudChatBaseChat>) msg.obj;
			if(null!=objs && objs.size()>0){
				mSearchChatDatas.addAll(objs);
			}
			mAdapter.notifyDataSetChanged();
			if(0 == mSearchChatDatas.size()){
				mNoDataTV.setVisibility(View.VISIBLE);
				mListView.setVisibility(View.GONE);
			}else{
				mNoDataTV.setVisibility(View.GONE);
				mListView.setVisibility(View.VISIBLE);
			}
			mLoadingPB.setVisibility(View.GONE);
			
		}else if(RKCloudChatUiHandlerMessage.FORWARD_QUERY_ALLCONTACTS_FINISH == msg.what){
			mSearchContactDatas.clear();
			List<RKCloudChatContact> objs = (List<RKCloudChatContact>)msg.obj;
			if(null!=objs && objs.size()>0){
				mSearchContactDatas.addAll(objs);
			}
			mAdapter.notifyDataSetChanged();
			if(0 == mSearchContactDatas.size()){
				mNoDataTV.setVisibility(View.VISIBLE);
				mListView.setVisibility(View.GONE);
			}else{
				mNoDataTV.setVisibility(View.GONE);
				mListView.setVisibility(View.VISIBLE);
			}
			mLoadingPB.setVisibility(View.GONE);
			
		}else if(RKCloudChatUiHandlerMessage.RESPONSE_APPLY_GROUP == msg.what){ // 申请群
			closeProgressDialog();
			if(0 == msg.arg1){
				RKCloudChatBaseChat chatObj = mMmsManager.queryChat((String)msg.obj);
				mAllExistChatDatas.add(0, chatObj);
				startQuery(QUERY_TYPE_SEARCH_CHAT);
				showConfirmOpe(chatObj.getChatId(), chatObj.getChatShowName());
				
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
		}else if(RKCloudChatUiHandlerMessage.CALLBACK_GROUP_INFO_CHANGED==msg.what
				|| RKCloudChatUiHandlerMessage.CALLBACK_MODIFY_GROUP_NAME == msg.what
				|| RKCloudChatUiHandlerMessage.CALLBACK_MODIFY_GROUP_DESC == msg.what
				|| RKCloudChatUiHandlerMessage.RESPONSE_GROUP_POPULATION_CHANGED == msg.what
				|| RKCloudChatUiHandlerMessage.RESPONSE_MODIFY_GROUP_INVITEAUTH == msg.what
				|| RKCloudChatUiHandlerMessage.CALLBACK_RECEIVED_MMS==msg.what 
				|| RKCloudChatUiHandlerMessage.CALLBACK_KICKOUT==msg.what
				|| RKCloudChatUiHandlerMessage.CALLBACK_RECEIVED_MOREMMS==msg.what
				|| RKCloudChatUiHandlerMessage.CALLBACK_GROUP_DISSOLVED==msg.what
				|| RKCloudChatUiHandlerMessage.DELETE_SINGLE_CHAT==msg.what
				|| RKCloudChatUiHandlerMessage.CALLBACK_ALL_GROUP_INFO_COMPLETE == msg.what){
			startQuery(QUERY_TYPE_GET_EXISTCHATS);
			
		}else if(RKCloudChatUiHandlerMessage.RESPONSE_FORWARD_MMS == msg.what){ // 转发消息
			closeProgressDialog();
			if(FUNC_FORWARD == mCurrFunc){
				RKCloudChatTools.showToastText(this, getString(0 == msg.arg1 ? R.string.rkcloud_chat_forwardmsg_success : R.string.rkcloud_chat_forwardmsg_failed));
			}else{
				RKCloudChatTools.showToastText(this, getString(0 == msg.arg1 ? R.string.rkcloud_chat_sharemsg_success : R.string.rkcloud_chat_sharemsg_failed));				
			}
			finish();
			
		}else if(RKCloudChatUiHandlerMessage.CONTACTSINFO_CHANGED == msg.what 
				|| RKCloudChatUiHandlerMessage.CONTACT_HEADERIMAGE_CHANGED == msg.what){ // 联系人信息或头像有变化
			startQuery(QUERY_TYPE_GET_ALLCONTACTS);	
		}else if(RKCloudChatUiHandlerMessage.IMAGE_COMPRESS_SUCCESS == msg.what || RKCloudChatUiHandlerMessage.IMAGE_COMPRESS_FAILED==msg.what){
			closeProgressDialog();
			if(RKCloudChatUiHandlerMessage.IMAGE_COMPRESS_FAILED == msg.what){
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_resize_image_failed));
				finish();
			}
		}
	}
	
	private class RKCloudChatForwardAdapter extends BaseAdapter {
		private ItemViewBuffer mItemBuffer;

		public RKCloudChatForwardAdapter() {			
		}
		
		@Override
		public int getCount() {
			int size = 0;
			switch (mCurrTab) {
			case TAB_EXISTCHAT:
				size = mSearchChatDatas.size();
				break;
				
			case TAB_ADDRESS:
				size = mSearchContactDatas.size();

			default:
				break;
			}
			return size;
		}

		@Override
		public Object getItem(int arg0) {
			Object obj = null;
			switch (mCurrTab) {
			case TAB_EXISTCHAT:
				obj = mSearchChatDatas.get(arg0);
				break;
				
			case TAB_ADDRESS:
				obj = mSearchContactDatas.get(arg0);

			default:
				break;
			}
			return obj;
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		/*
		 * 定义每个条目中使用View的基本元素
		 */
		private class ItemViewBuffer {
			RoundedImageView headerImg;
			TextView nameTV; 
			
			public ItemViewBuffer(View convertView) {
				headerImg = (RoundedImageView)convertView.findViewById(R.id.headerphoto);
				nameTV = (TextView) convertView.findViewById(R.id.name);
			}
		}

		@Override
		public View getView(int arg0, View convertView, ViewGroup arg2) {
			if (null == convertView) {
				convertView = getLayoutInflater().inflate(R.layout.rkcloud_chat_forward_item, null);
				mItemBuffer = new ItemViewBuffer(convertView);
				convertView.setTag(mItemBuffer);
			} else {
				mItemBuffer = (ItemViewBuffer) convertView.getTag();
			}
			
			mItemBuffer.headerImg.setVisibility(View.VISIBLE);
			mItemBuffer.headerImg.setImageResource(R.drawable.rkcloud_chat_img_header_default);
			
			if(TAB_EXISTCHAT == mCurrTab){
				// 获取会话数据
				RKCloudChatBaseChat chatObj = mSearchChatDatas.get(arg0);
				if(CREATE_NEWCHAT.equalsIgnoreCase(chatObj.getChatId())){
					mItemBuffer.headerImg.setVisibility(View.GONE);
					mItemBuffer.nameTV.setText(R.string.rkcloud_chat_forwardmsg_creategroup);
				}else{
					if(chatObj instanceof SingleChat) { // 单聊
						RKCloudChatContact contactObj = null;
						for(RKCloudChatContact obj : mAllContactDatas){
							if(obj.rkAccount.equalsIgnoreCase(chatObj.getChatId())){
								contactObj = obj;
								break;
							}
						}
						if(null != chatObj.highLightName){
							mItemBuffer.nameTV.setText(chatObj.highLightName);
						}else{
							mItemBuffer.nameTV.setText(null!=contactObj ? contactObj.getShowName() : chatObj.getChatId());
						}
						// 设置头像
						if(null!=contactObj && !TextUtils.isEmpty(contactObj.getHeaderThumbImagePath())){
							RKCloudChatImageRequest imageReq = new RKCloudChatImageRequest(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG, contactObj.getHeaderThumbImagePath(), chatObj.getChatId());
							RKCloudChatImageResult imgResult = RKCloudChatImageAsyncLoader.getInstance(RKCloudChatForwardActivity.this).sendPendingRequestQuryCache(imageReq);
							if(null!=imgResult && null!=imgResult.resource){
								mItemBuffer.headerImg.setImageDrawable(imgResult.resource);
							}	
						}
						
					}else if(chatObj instanceof GroupChat){ // 群聊
						mItemBuffer.headerImg.setImageResource(R.drawable.rkcloud_chat_img_header_mutlichat_default);	
						if(null!=chatObj.highLightName){
							mItemBuffer.nameTV.setText(chatObj.highLightName.append("(").append(""+chatObj.getUserCounts()).append(")"));
						}else{
							mItemBuffer.nameTV.setText(String.format("%s(%d)", chatObj.getChatShowName(), chatObj.getUserCounts()));
						}
					}
				}
				
			}else if(TAB_ADDRESS == mCurrTab){
				RKCloudChatContact obj = mSearchContactDatas.get(arg0);
				mItemBuffer.nameTV.setText(null!=obj.highLightName ? obj.highLightName : obj.getShowName());
				// 设置头像
				if(!TextUtils.isEmpty(obj.getHeaderThumbImagePath())){
					RKCloudChatImageRequest imageReq = new RKCloudChatImageRequest(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG, obj.getHeaderThumbImagePath(), obj.rkAccount);
					RKCloudChatImageResult imgResult = RKCloudChatImageAsyncLoader.getInstance(RKCloudChatForwardActivity.this).sendPendingRequestQuryCache(imageReq);
					if(null!=imgResult && null!=imgResult.resource){
						mItemBuffer.headerImg.setImageDrawable(imgResult.resource);
					}	
				}
			}
			
			return convertView;
		}
	}
}
