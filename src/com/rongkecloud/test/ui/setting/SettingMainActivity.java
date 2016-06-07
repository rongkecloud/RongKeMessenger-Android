package com.rongkecloud.test.ui.setting;

import java.io.File;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rongkecloud.chat.demo.RKCloudChatMmsManager;
import com.rongkecloud.chat.demo.RKCloudChatUiHandlerMessage;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseFragment;
import com.rongkecloud.test.R;
import com.rongkecloud.test.entity.ContactInfo;
import com.rongkecloud.test.manager.AccountManager;
import com.rongkecloud.test.manager.PersonalManager;
import com.rongkecloud.test.manager.SettingManager;
import com.rongkecloud.test.manager.uihandlermsg.ContactUiMessage;
import com.rongkecloud.test.manager.uihandlermsg.SettingUiMessage;
import com.rongkecloud.test.system.ConfigKey;
import com.rongkecloud.test.system.RKCloudDemo;
import com.rongkecloud.test.ui.LoginActivity;
import com.rongkecloud.test.ui.setting.mms.SettingMmsActivity;
import com.rongkecloud.test.ui.widget.RKCloudChatCircleDrawable;
import com.rongkecloud.test.ui.widget.RoundedImageView;
import com.rongkecloud.test.utility.OtherUtilities;
import com.rongkecloud.test.utility.SystemInfo;

public class SettingMainActivity extends RKCloudChatBaseFragment implements OnClickListener{
	// UI组件
	private LinearLayout mPersonalLayout;
	private LinearLayout mModifyPwdLayout;
	private LinearLayout mMmsLayout;
	private LinearLayout mClearMsgLayout;
	private LinearLayout mUpdateLayout;
	private LinearLayout mFeedbackLayout;
	private LinearLayout mSettingAboutLayout;
	private LinearLayout mTestLayout, mSPLayout, mDBLayout;
	private LinearLayout layout_logout;
	private TextView mVersionText;
	private TextView mAccountView;
	private TextView mNameView;
	private RoundedImageView mAvatarView;
	
	private RKCloudChatMmsManager mMmsManager;
	private PersonalManager mPersonalManager;
	private SettingManager mSettingManager;
	
	private ContactInfo mSelfInfo;
	private Context mContext;	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.setting, container, false);
		view.setOnClickListener(null);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		mContext = getActivity();
		mMmsManager = RKCloudChatMmsManager.getInstance(mContext);
		mPersonalManager = PersonalManager.getInstance();
		mSettingManager = SettingManager.getInstance();
		
		initUIAndListener();
		refresh();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		String currAccount = RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, null);
		if(!TextUtils.isEmpty(currAccount)){
			ContactInfo selfObj = mPersonalManager.getContactInfo(currAccount);
			if(null!=selfObj && !TextUtils.isEmpty(selfObj.mRealName)){
				mNameView.setVisibility(View.VISIBLE);
				mNameView.setText(selfObj.mRealName);
			}else{
				mNameView.setVisibility(View.GONE);
			}
		}
	}
	
	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		if(!hidden){
			refresh();
		}
	}
	
	@Override
	public void refresh(){
		mMmsManager.bindUiHandler(mUiHandler);
		mPersonalManager.bindUiHandler(mUiHandler);
		mSettingManager.bindUiHandler(mUiHandler);
		mContext = getActivity();
		mSelfInfo = mPersonalManager.getContactInfo(RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, ""));
		if(null!=mSelfInfo && mSelfInfo.mAvatarServerVersion>0 && 
				(TextUtils.isEmpty(mSelfInfo.mThumbPath) || !new File(mSelfInfo.mThumbPath).exists()
				|| mSelfInfo.mAvatarServerVersion>mSelfInfo.mAvatarClientThumbVersion)){
			mPersonalManager.getAvatarThumb(mSelfInfo.mAccount, mSelfInfo.mAvatarServerVersion);
		}
		showHeaderThumb();
	}
	
	private void initUIAndListener(){
		// 设置title
		TextView titleTV = (TextView)getView().findViewById(R.id.txt_title);
		titleTV.setVisibility(TextView.GONE);

        TextView text_title_content = (TextView)getView().findViewById(R.id.text_title_content);
        text_title_content.setText(getString(R.string.tab_setting));
		
		mPersonalLayout = (LinearLayout) getView().findViewById(R.id.layout_personal_info);
		mModifyPwdLayout = (LinearLayout)getView().findViewById(R.id.layout_modify_pwd);
		mMmsLayout = (LinearLayout)getView().findViewById(R.id.layout_mms);
		mClearMsgLayout = (LinearLayout)getView().findViewById(R.id.layout_clearmms);
		mTestLayout = (LinearLayout)getView().findViewById(R.id.layout_test);
		mUpdateLayout = (LinearLayout) getView().findViewById(R.id.layout_update);
		mFeedbackLayout = (LinearLayout) getView().findViewById(R.id.layout_feedback);
		mSettingAboutLayout = (LinearLayout) getView().findViewById(R.id.layout_about);
		mSPLayout = (LinearLayout)getView().findViewById(R.id.layout_showsp);
		mDBLayout = (LinearLayout)getView().findViewById(R.id.layout_showdb);

        layout_logout = (LinearLayout)getView().findViewById(R.id.layout_logout);
		mVersionText = (TextView) getView().findViewById(R.id.vesion_code);
		mAccountView = (TextView) getView().findViewById(R.id.setting_account_view);
		mNameView = (TextView) getView().findViewById(R.id.setting_name_view);
		mAvatarView = (RoundedImageView) getView().findViewById(R.id.setting_avatar_view);
		
		mAccountView.setText(getString(R.string.setting_account_format, RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, null)));
		mVersionText.setText(SystemInfo.getVersionName());
		
		mPersonalLayout.setOnClickListener(this);
		mModifyPwdLayout.setOnClickListener(this);
		mMmsLayout.setOnClickListener(this);
		mFeedbackLayout.setOnClickListener(this);
		mSettingAboutLayout.setOnClickListener(this);
		mClearMsgLayout.setOnClickListener(this);
		layout_logout.setOnClickListener(this);
		mUpdateLayout.setOnClickListener(this);
		
		if(RKCloudDemo.debugModel){
			mTestLayout.setVisibility(View.VISIBLE);
			mSPLayout.setOnClickListener(this);
			mDBLayout.setOnClickListener(this);
		}else{
			mTestLayout.setVisibility(View.GONE);
			mSPLayout.setOnClickListener(this);
			mDBLayout.setOnClickListener(this);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.layout_personal_info:
			startActivity(new Intent(mContext, SettingPersonalActivity.class));
			break;

		case R.id.layout_modify_pwd:
			startActivity(new Intent(mContext, SettingModifyPwdActivity.class));
			break;
			
		case R.id.layout_mms:
			startActivity(new Intent(mContext, SettingMmsActivity.class));
			break;
			
		case R.id.layout_clearmms:
			// 显示确认对话框
			new AlertDialog.Builder(mContext).setTitle(R.string.chatdemo_setting_clearallmsg_title)
				.setMessage(R.string.chatdemo_setting_clearallmsg_confirm)
				.setNegativeButton(R.string.rkcloud_chat_btn_cancel, null)
				.setPositiveButton(R.string.rkcloud_chat_btn_confirm, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						showProgressDialog();
						new Thread(new Runnable() {
							@Override
							public void run() {
								mMmsManager.delAllChatInfos(true);
							}
						}).start();
					}
			}).show();
			break;
			
		case R.id.layout_update:
			showProgressDialog();
			mSettingManager.checkUpdate(false);
			break;
			
		case R.id.layout_feedback:
			startActivity(new Intent(mContext, SettingFeedbackActivity.class));
			break;
			
		case R.id.layout_about:
			startActivity(new Intent(mContext, SettingAboutActivity.class));
			break;
			
		case R.id.layout_logout:
			// 显示确认对话框
			new AlertDialog.Builder(mContext).setTitle(R.string.chatdemo_setting_logout_title)
				.setMessage(R.string.chatdemo_setting_logout_confirm)
				.setNegativeButton(R.string.rkcloud_chat_btn_cancel, null)
				.setPositiveButton(R.string.rkcloud_chat_btn_confirm, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//清除相关内容
						AccountManager.getInstance().logout();
						Intent intent = new Intent(mContext, LoginActivity.class);
						startActivity(intent);
					}
				}).show();
			break;
			
		case R.id.layout_showsp:
			Intent intentSp = new Intent(mContext, ShowContentsActivity.class);
			intentSp.putExtra(ShowContentsActivity.KEY_TYPE, ShowContentsActivity.KEY_TYPE_SP);
			startActivity(intentSp);
			break;
			
		case R.id.layout_showdb:
			Intent intentDb = new Intent(mContext, ShowContentsActivity.class);
			intentDb.putExtra(ShowContentsActivity.KEY_TYPE, ShowContentsActivity.KEY_TYPE_DB);
			startActivity(intentDb);
			break;
			
		}
	}
	
	
	private void showHeaderThumb(){
		if(null!=mSelfInfo && !TextUtils.isEmpty(mSelfInfo.mThumbPath) && new File(mSelfInfo.mThumbPath).exists()){
			Bitmap bitmap = BitmapFactory.decodeFile(mSelfInfo.mThumbPath);
			if(null != bitmap){
				mAvatarView.setImageBitmap(bitmap);
			}else{
				mAvatarView.setImageResource(R.drawable.rkcloud_chat_img_header_default);
			}
		}else{
			mAvatarView.setImageResource(R.drawable.rkcloud_chat_img_header_default);
		}
	}
	
	@Override
	public void processResult(Message msg) {
		if(!mShow){
			return;
		}
		switch (msg.what) {
		case RKCloudChatUiHandlerMessage.DELETE_ALL_CHATS:
			closeProgressDialog();
			if(msg.arg1 > 0){
				OtherUtilities.showToastText(mContext, getString(R.string.operation_success));
			}
			break;
			
		case SettingUiMessage.RESPONSE_CHECK_UPDATE:
			closeProgressDialog();
			break;
			
		case ContactUiMessage.RESPONSE_GET_AVATAR_THUMB:
			if(RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "").equalsIgnoreCase((String)msg.obj)){
				mSelfInfo = mPersonalManager.getContactInfo(RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, ""));
				showHeaderThumb();
			}
			break;
		}
	}
}
