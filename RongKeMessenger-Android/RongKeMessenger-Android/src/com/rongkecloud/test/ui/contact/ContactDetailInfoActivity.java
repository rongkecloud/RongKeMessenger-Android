package com.rongkecloud.test.ui.contact;

import java.io.File;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;

import com.rongke.cloud.av.demo.RKCloudAVDemoManager;
import com.rongkecloud.chat.demo.RKCloudChatMmsManager;
import com.rongkecloud.chat.demo.tools.RKCloudChatImageTools;
import com.rongkecloud.chat.demo.tools.RKCloudChatScreenTools;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.test.R;
import com.rongkecloud.test.entity.Constants;
import com.rongkecloud.test.entity.ContactInfo;
import com.rongkecloud.test.http.HttpResponseCode;
import com.rongkecloud.test.manager.ContactManager;
import com.rongkecloud.test.manager.MessageManager;
import com.rongkecloud.test.manager.PersonalManager;
import com.rongkecloud.test.manager.uihandlermsg.ContactUiMessage;
import com.rongkecloud.test.system.ConfigKey;
import com.rongkecloud.test.system.RKCloudDemo;
import com.rongkecloud.test.ui.widget.RoundedImageView;
import com.rongkecloud.test.utility.OtherUtilities;
import com.rongkecloud.test.utility.SDCardUtil;

public class ContactDetailInfoActivity extends RKCloudChatBaseActivity implements OnClickListener{
	
	public static final String INTENT_CONTACT_ACCOUNT = "intent_contact_account";// 用户账号
	
	private TextView mTitle;
	private TextView mAccountTV;
	private TextView mNameTV;
	private TextView mRemarkTV;
	private TextView mAddressTV;
	private Button mSendMMSBtn;
	private RelativeLayout mAddFriendBtn;
	private Button mDeleteFriendBtn;
	private Button mVedioBtn;
	private RoundedImageView mAvatar;
	private LinearLayout mRemarkLinear;
	
	private String mContactAccount;
	private ContactInfo mCurrObj;
	
	private ContactManager mContactManager;
	private PersonalManager mPersonalManager;
	private String mModifyRemark;
	
	private boolean mSyncFirst = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contact_detail);
		mContactAccount = getIntent().getStringExtra(INTENT_CONTACT_ACCOUNT);
		if(TextUtils.isEmpty(mContactAccount)){
			finish();
			return;
		}
		initViews();
		initListeners();
		mContactManager = ContactManager.getInstance();
		mPersonalManager = PersonalManager.getInstance();
	}
	
	private void initViews(){
		mTitle = (TextView) findViewById(R.id.txt_title);
		mTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		mTitle.setText(R.string.bnt_return);
		mTitle.setOnClickListener(mExitListener);

        TextView text_title_content = (TextView) findViewById(R.id.text_title_content);
        text_title_content.setText(R.string.contact_detail_title);

		mAccountTV = (TextView) findViewById(R.id.account_text);
		mRemarkLinear = (LinearLayout) findViewById(R.id.contact_remark_linear);
		mNameTV = (TextView) findViewById(R.id.name_text);
		mRemarkTV = (TextView) findViewById(R.id.remark_text);
		mAddressTV = (TextView) findViewById(R.id.address_text);
		mSendMMSBtn = (Button) findViewById(R.id.contact_send_mms);
		mAddFriendBtn = (RelativeLayout)findViewById(R.id.contact_add_friend);
		mDeleteFriendBtn = (Button) findViewById(R.id.contact_delete_friend);
		mVedioBtn = (Button) findViewById(R.id.contact_vedio);
		mAvatar = (RoundedImageView) findViewById(R.id.avatar_img);
	}
	
	private void initListeners(){
		mRemarkLinear.setOnClickListener(this);
		mAvatar.setOnClickListener(this);
		mSendMMSBtn.setOnClickListener(this);
		mAddFriendBtn.setOnClickListener(this);
		mDeleteFriendBtn.setOnClickListener(this);
		mVedioBtn.setOnClickListener(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mContactManager.bindUiHandler(mUiHandler);
		mPersonalManager.bindUiHandler(mUiHandler);
		MessageManager.getInstance().bindUiHandler(mUiHandler);
				
		controlShowContent();
		if(null != mCurrObj){
			// 是否同步用户的基本信息
			if(System.currentTimeMillis()-mCurrObj.mInfoLastGetTime >= Constants.GET_PERSONAL_INFO_TIME){
				// 同步用户的基本信息
				showProgressDialog();
				mPersonalManager.syncUserInfo(mContactAccount);
			}else{
				// 查看头像是否需要同步
				if(mCurrObj.mAvatarServerVersion > 0 && SDCardUtil.diskSpaceAvailable()){
					if(TextUtils.isEmpty(mCurrObj.mThumbPath) 
							|| !new File(mCurrObj.mThumbPath).exists() 
							|| mCurrObj.mAvatarServerVersion>mCurrObj.mAvatarClientThumbVersion){
						mPersonalManager.getAvatarThumb(mCurrObj.mAccount, mCurrObj.mAvatarServerVersion);
					}
				}
			}
		}else{
			if(mSyncFirst){
				showProgressDialog();
				mSyncFirst = false;
			}
			mPersonalManager.syncUserInfo(mContactAccount);
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.avatar_img:
			if(null != mCurrObj){
				//若头像信息存在，点击头像跳转到大图界面，查看头像大图
				if(0 != mCurrObj.mAvatarClientThumbVersion){
					Intent intent = new Intent(this, BigImageActivity.class);
					intent.putExtra(BigImageActivity.INTENT_KEY_ACCOUNT, mCurrObj.mAccount);
					startActivity(intent);
				}
			}
			break;
			
		case R.id.contact_remark_linear:
			//修改好友备注名称
			View layout = LayoutInflater.from(this).inflate(R.layout.contact_input_remark, null);
			final EditText conEdit = (EditText)layout.findViewById(R.id.remark_input);
			InputFilter filter = new InputFilter.LengthFilter(30);
			conEdit.setFilters(new InputFilter[] {filter});
			conEdit.setText(TextUtils.isEmpty(mCurrObj.mRemark) ? "" : mCurrObj.mRemark);
			conEdit.setSelection(conEdit.getText().toString().trim().length());
			new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.contact_modify_remark_title)).setView(layout)
			.setNegativeButton(R.string.bnt_cancel, new DialogInterface.OnClickListener() {			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			}).setPositiveButton(R.string.bnt_confirm, new DialogInterface.OnClickListener() {			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mModifyRemark = conEdit.getText().toString().trim();
					showProgressDialog();
					mContactManager.modifyFriendInfo(mCurrObj.mAccount, mModifyRemark);
				}
			}).create().show();
			break;

		case R.id.contact_send_mms:
			//发消息，调用云视互动sdk发起消息界面
			RKCloudChatMmsManager.getInstance(ContactDetailInfoActivity.this).enterMsgListActivity(mCurrObj.mAccount);
			break;
			
		case R.id.contact_add_friend:
			//添加好友
			showProgressDialog();
			mContactManager.addFriend(mCurrObj.mAccount, "");
			break;
			
		case R.id.contact_delete_friend: 
			//删除好友
			new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.contact_detail_delete_friend)).
			setMessage(getResources().getString(R.string.contact_detail_delete_friend_confirm))
			.setNegativeButton(R.string.bnt_cancel, new DialogInterface.OnClickListener() {			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			}).setPositiveButton(R.string.bnt_confirm, new DialogInterface.OnClickListener() {			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					showProgressDialog();
					mContactManager.deleteFriend(mCurrObj.mAccount);
				}
			}).create().show();
			break;
			
		case R.id.contact_vedio:
			new AlertDialog.Builder(ContactDetailInfoActivity.this)
			.setItems(new String[]{getString(R.string.rkcloud_av_dial_withaudio), getString(R.string.rkcloud_av_dial_withvideo)}, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch(which){
					case 0:
						RKCloudAVDemoManager.getInstance(ContactDetailInfoActivity.this).dial(ContactDetailInfoActivity.this, mCurrObj.mAccount, false);
						break;
						
					case 1:
						RKCloudAVDemoManager.getInstance(ContactDetailInfoActivity.this).dial(ContactDetailInfoActivity.this, mCurrObj.mAccount, true);
						break;
					}
				}
			}).create().show();
			break;
		}
	}
	
	private void controlShowContent(){
		mCurrObj = mPersonalManager.getContactInfo(mContactAccount);
		Drawable imageDrawable = null;
		if(null != mCurrObj){
			mAccountTV.setText(mContactAccount);
			if(!TextUtils.isEmpty(mCurrObj.mRemark)){
				mRemarkTV.setText(mCurrObj.mRemark);
			}else{
				mRemarkTV.setText(R.string.contact_modify_remark);
			}
			mAddressTV.setText(mCurrObj.mAddress);
			mNameTV.setText(mCurrObj.mRealName);
			if(null!=mCurrObj.mThumbPath && new File(mCurrObj.mThumbPath).exists()){
				Bitmap bitmap = RKCloudChatImageTools.resizeBitmap(mCurrObj.mThumbPath, RKCloudChatScreenTools.getInstance(this).getScreenWidth(), RKCloudChatScreenTools.getInstance(this).getScreenHeight());
				imageDrawable = new BitmapDrawable(getResources(), bitmap);
			}
		}else{
			mAccountTV.setText(mContactAccount);
		}	
		
		if(null != imageDrawable){
			mAvatar.setImageDrawable(imageDrawable);
		}else{
			mAvatar.setImageResource(R.drawable.rkcloud_chat_img_header_default);
		}
		
		if(RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "").equalsIgnoreCase(mContactAccount)){
			//若个人信息id为用户自己，操作按钮均隐藏
			mRemarkLinear.setVisibility(View.GONE);
			mAddFriendBtn.setVisibility(View.GONE);
			mDeleteFriendBtn.setVisibility(View.GONE);
			mSendMMSBtn.setVisibility(View.GONE);
			mVedioBtn.setVisibility(View.GONE);
		}else{
			if(null!=mCurrObj && mCurrObj.mGroupId>=0){ // 表示为好友关系
				//如果是好友，隐藏添加好友按钮，显示删除好友，发消息，发起视频通话按钮
				mAddFriendBtn.setVisibility(View.GONE);
				mDeleteFriendBtn.setVisibility(View.VISIBLE);
				mRemarkLinear.setVisibility(View.VISIBLE);
				mSendMMSBtn.setVisibility(View.VISIBLE);
				mVedioBtn.setVisibility(View.VISIBLE);
			}else{
				//如果不是好友，隐藏删除好友按钮，发消息，发起视频通话按钮，显示添加好友按钮
				mAddFriendBtn.setVisibility(View.VISIBLE);
				mDeleteFriendBtn.setVisibility(View.GONE);
				mRemarkLinear.setVisibility(View.GONE);
				mSendMMSBtn.setVisibility(View.GONE);
				mVedioBtn.setVisibility(View.GONE);
			}
		}
	}

	@Override
	public void processResult(Message msg) {		
		switch (msg.what) {
		case ContactUiMessage.SYNC_PERSON_INFOS:
			if(HttpResponseCode.OK == msg.arg1){
				List<String> syncAccounts = (List<String>)msg.obj;
				if(syncAccounts.contains(mContactAccount)){
					closeProgressDialog();
					controlShowContent();
					// 查看头像是否需要同步
					if(mCurrObj.mAvatarServerVersion > 0 && SDCardUtil.diskSpaceAvailable()){
						if(TextUtils.isEmpty(mCurrObj.mThumbPath) 
								|| !new File(mCurrObj.mThumbPath).exists() 
								|| mCurrObj.mAvatarServerVersion>mCurrObj.mAvatarClientThumbVersion){
							mPersonalManager.getAvatarThumb(mCurrObj.mAccount, mCurrObj.mAvatarServerVersion);
						}
					}
				}
			}else if(HttpResponseCode.NO_NETWORK == msg.arg1){
				closeProgressDialog();
				OtherUtilities.showToastText(this, getString(R.string.network_off));
			}else{
				closeProgressDialog();
				OtherUtilities.showToastText(this, getString(R.string.operation_failed));
			}
			
			break;
		case ContactUiMessage.RECEIVED_FRIEND_ADDCONFIRM:
			String friendAccount = (String)msg.obj;
			if(mCurrObj.mAccount.equalsIgnoreCase(friendAccount)){
				mCurrObj.mGroupId = msg.arg1;
				controlShowContent();
			}
			break;
			
		case ContactUiMessage.RESPONSE_ADDFRIEND:
			closeProgressDialog();
			if(HttpResponseCode.OK == msg.arg1){
				mCurrObj.mGroupId = 0;
				controlShowContent();
				OtherUtilities.showToastText(this, getString(R.string.operation_success));
				
			}else if(HttpResponseCode.ADDFRIEND_NEEDVERIFY == msg.arg1){
				mContactManager.showAddFriendVerfiyWin(this, (String)msg.obj);// 输入验证信息
			}else if(HttpResponseCode.ADDFRIEND_WAITVERIFY == msg.arg1){
				OtherUtilities.showToastText(this, getString(R.string.contact_add_waitverify));
				
			}else if(HttpResponseCode.ADDFRIEND_FORBIDYOURSELF == msg.arg1){
				OtherUtilities.showToastText(this, getString(R.string.contact_add_yourself));
				
			}else{
				OtherUtilities.showToastText(this, getString(R.string.operation_failed));
			}
			break;
			
		case ContactUiMessage.RESPONSE_DELFRIEND:
			closeProgressDialog();
			if(HttpResponseCode.OK == msg.arg1){
				mCurrObj.mGroupId = -1;
				controlShowContent();
				OtherUtilities.showToastText(this, getString(R.string.operation_success));
				
			}else{
				OtherUtilities.showToastText(this, getString(R.string.operation_failed));
			}
			break;
			
		case ContactUiMessage.RECEIVED_FRIEND_DELETED:
			String delAccount = (String)msg.obj;
			if(mCurrObj.mAccount.equalsIgnoreCase(delAccount)){
				mCurrObj.mGroupId = -1;
				controlShowContent();
			}
			break;
			
		case ContactUiMessage.RESPONSE_MODIFY_FRIENDINFO:
			closeProgressDialog();
			if(HttpResponseCode.OK == msg.arg1){
				mCurrObj.mRemark = mModifyRemark;
				controlShowContent();

			}else if(HttpResponseCode.NO_NETWORK == msg.arg1){
				OtherUtilities.showToastText(this, getString(R.string.network_off));
			}else{
				OtherUtilities.showToastText(this, getString(R.string.operation_failed));
			}
			break;
			
		case ContactUiMessage.RESPONSE_GET_AVATAR_THUMB:
			//获取缩略图成功后的处理
			if(HttpResponseCode.OK == msg.arg1){
				controlShowContent();
			}else if(HttpResponseCode.GET_AVATAR_FAIL == msg.arg1){
				OtherUtilities.showToastText(this, getString(R.string.setting_get_avatar_failed));
			}else if(HttpResponseCode.NO_NETWORK == msg.arg1){
				OtherUtilities.showToastText(this, getString(R.string.network_off));
			}else{
				OtherUtilities.showToastText(this, getString(R.string.operation_failed));
			}
			break;
		}
	}
}
