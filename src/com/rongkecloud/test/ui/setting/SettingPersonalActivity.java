package com.rongkecloud.test.ui.setting;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.test.R;
import com.rongkecloud.test.entity.Constants;
import com.rongkecloud.test.entity.ContactInfo;
import com.rongkecloud.test.http.HttpResponseCode;
import com.rongkecloud.test.manager.PersonalManager;
import com.rongkecloud.test.manager.SettingManager;
import com.rongkecloud.test.manager.uihandlermsg.ContactUiMessage;
import com.rongkecloud.test.manager.uihandlermsg.SettingUiMessage;
import com.rongkecloud.test.system.ConfigKey;
import com.rongkecloud.test.system.RKCloudDemo;
import com.rongkecloud.test.ui.contact.BigImageActivity;
import com.rongkecloud.test.ui.widget.RoundedImageView;
import com.rongkecloud.test.utility.ImageUtil;
import com.rongkecloud.test.utility.OtherUtilities;
import com.rongkecloud.test.utility.RegularCheckTools;

public class SettingPersonalActivity extends RKCloudChatBaseActivity implements OnClickListener{
	//从其他ui返回的结果码
	private static final int PHOTO = 1;
	private static final int LOCAL_PHOTO = 2;
	private static final int RESULT_PHOTO = 3;
	
	// UI组件
	private TextView mAccountTV;
	private LinearLayout mPhotoLayout;
	private RoundedImageView mPhotoImg;
	private LinearLayout mNameLayout;
	private TextView mNameTV;
	private LinearLayout mSexLayout;
	private TextView mSexTV;
	private LinearLayout mAddrLayout;
	private TextView mAddressTV;
	private LinearLayout mMobileLayout;
	private TextView mMobileTV;
	private LinearLayout mEmailLayout;
	private TextView mEmailTV;
	private CheckBox mAddFriendPreCheckBox;
			
	private String mCurrAccount;
	private ContactInfo mCurrUserInfo;
	private SettingManager mSettingManager;
	private PersonalManager mPersonalManager;
	
	private String mTakePhotoTempName = null;// 记录拍照时的图片名称
	private String mTempName;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting_personal_info);
		initUIAndListener();
		
		mSettingManager = SettingManager.getInstance();
		mPersonalManager = PersonalManager.getInstance();
		mCurrAccount = RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "");
		mAccountTV.setText(mCurrAccount);
		initData();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mSettingManager.bindUiHandler(mUiHandler);
		mPersonalManager.bindUiHandler(mUiHandler);
		if(null!=mCurrUserInfo && mCurrUserInfo.mAvatarServerVersion>0 && 
				(TextUtils.isEmpty(mCurrUserInfo.mThumbPath) || !new File(mCurrUserInfo.mThumbPath).exists()
				|| mCurrUserInfo.mAvatarServerVersion>mCurrUserInfo.mAvatarClientThumbVersion)){
			mPersonalManager.getAvatarThumb(mCurrUserInfo.mAccount, mCurrUserInfo.mAvatarServerVersion);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// 拍照时保存图片名称
		if (!TextUtils.isEmpty(mTakePhotoTempName)) {
			outState.putString("takephonename", mTakePhotoTempName);
		}
		if (!TextUtils.isEmpty(mTempName)) {
			outState.putString("localphonename", mTempName);
		}
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// 恢复时如果包含takephonename字符串，表示要获取拍照时保存的图片名称
		if (savedInstanceState.containsKey("takephonename")) {
			mTakePhotoTempName = savedInstanceState.getString("takephonename");
			savedInstanceState.remove("takephonename");
		}
		if (savedInstanceState.containsKey("localphonename")) {
			mTempName = savedInstanceState.getString("localphonename");
			savedInstanceState.remove("localphonename");
		}
	}
	
	private void initUIAndListener(){
		// 设置title
		TextView titleTV = (TextView)findViewById(R.id.txt_title);
		titleTV.setText(R.string.bnt_return);
		titleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		titleTV.setOnClickListener(mExitListener);

        TextView text_title_content = (TextView)findViewById(R.id.text_title_content);
        text_title_content.setText(R.string.setting_personal_info);

		mAccountTV = (TextView) findViewById(R.id.account);		
		
		mPhotoLayout = (LinearLayout) findViewById(R.id.layout_photo);
		mPhotoImg = (RoundedImageView)findViewById(R.id.photo);
		
		mNameLayout = (LinearLayout)findViewById(R.id.setting_name);
		mNameTV = (TextView) findViewById(R.id.setting_name_content);
		
		mSexLayout = (LinearLayout)findViewById(R.id.setting_sex);
		mSexTV = (TextView) findViewById(R.id.setting_sex_content);
		
		mAddrLayout = (LinearLayout)findViewById(R.id.setting_addr);
		mAddressTV = (TextView) findViewById(R.id.setting_addr_content);
		
		mMobileLayout = (LinearLayout) findViewById(R.id.setting_mobile);
		mMobileTV = (TextView) findViewById(R.id.setting_mobile_content);
		
		mEmailLayout = (LinearLayout) findViewById(R.id.setting_email);
		mEmailTV = (TextView) findViewById(R.id.setting_email_content);
		
		mAddFriendPreCheckBox = (CheckBox) findViewById(R.id.add_friend_premission_checkbox);
		
		mPhotoLayout.setOnClickListener(this);
		mPhotoImg.setOnClickListener(this);
		mNameLayout.setOnClickListener(this);
		mSexLayout.setOnClickListener(this);
		mAddrLayout.setOnClickListener(this);
		mMobileLayout.setOnClickListener(this);
		mEmailLayout.setOnClickListener(this);
		
		mAddFriendPreCheckBox.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checkStatus = RKCloudDemo.config.getInt(ConfigKey.LOGIN_ADD_FRIEND_PERMISSION, 0) == Constants.ADD_FRIEND_PREMISSION_YES;
				showProgressDialog();
				mSettingManager.modifySelfInfo("permission", String.valueOf(checkStatus ? Constants.ADD_FRIEND_PREMISSION_NO : Constants.ADD_FRIEND_PREMISSION_YES));
				
			}
		});
	}
	
	private void initData(){
		mCurrUserInfo = mPersonalManager.getContactInfo(mCurrAccount);
		mNameTV.setText(mCurrUserInfo.mRealName);
		mAddressTV.setText(mCurrUserInfo.mAddress);
		if(Constants.SEX_MAN == mCurrUserInfo.mSex){
			mSexTV.setText(R.string.person_sex_man);
		}else if(Constants.SEX_WOMAN == mCurrUserInfo.mSex){
			mSexTV.setText(R.string.person_sex_woman);
		}
		mMobileTV.setText(mCurrUserInfo.mMobile);
		mEmailTV.setText(mCurrUserInfo.mEmail);
		// 设置头像
		mPhotoImg.setImageResource(R.drawable.rkcloud_chat_img_header_default);
		if(null != mCurrUserInfo && !TextUtils.isEmpty(mCurrUserInfo.mThumbPath) && new File(mCurrUserInfo.mThumbPath).exists()){
			Bitmap bitmap = BitmapFactory.decodeFile(mCurrUserInfo.mThumbPath);
			if(null != bitmap){
				mPhotoImg.setImageBitmap(bitmap);
			}
		}
		
		// 是否验证
		if(RKCloudDemo.config.getInt(ConfigKey.LOGIN_ADD_FRIEND_PERMISSION, 0) == Constants.ADD_FRIEND_PREMISSION_YES){
			mAddFriendPreCheckBox.setChecked(true);
		}else{
			mAddFriendPreCheckBox.setChecked(false);
		}
	}
	
	/*
	 * 自定义弹出对话框（头像）；
	 */
	public void showSelectImageDialog() {
		new AlertDialog.Builder(this).setItems(R.array.take_photo,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						// 拍照
						case 0:
							mTakePhotoTempName = Constants.TEMP_PATH + System.currentTimeMillis() + ".jpg";
							File photo = new File(mTakePhotoTempName);
							if (!photo.getParentFile().exists()) {
								photo.getParentFile().mkdirs();
							}
							
							Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
							intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
							intent.putExtra("autofocus", true); // 自动对焦
							intent.putExtra("fullScreen", false); // 全屏
							intent.putExtra("showActionIcons", false);
							startActivityForResult(intent, PHOTO);
							break;
							
						// 从相册选择图片
						case 1:
							Intent intent2 = new Intent(Intent.ACTION_PICK);
							intent2.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
							startActivityForResult(intent2, LOCAL_PHOTO);
							break;
						}
					}
				}).create().show();
	}
	
	/**
	 * 
	 * 启动照片裁剪
	 */
	public void startPhotoCutter(Uri uri) {
		mTempName = Constants.TEMP_PATH + mCurrAccount + ".jpg";
		File file = new File(mTempName);
		if(!file.getParentFile().exists()){
			file.getParentFile().mkdirs();
		}
		
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");
		// 设置裁剪
		intent.putExtra("crop", "true");
		// aspectX aspectY 是宽高的比例
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		// outputX outputY 是裁剪图片宽高
		intent.putExtra("scale", true);
		intent.putExtra("return-data", false);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(mTempName)));
		startActivityForResult(intent, RESULT_PHOTO);
	}
	
	@Override
	public void onClick(View v) {
		View layout = LayoutInflater.from(this).inflate(R.layout.contact_input_remark, null);
		final EditText conEdit = (EditText)layout.findViewById(R.id.remark_input);
		switch (v.getId()) {
		case R.id.layout_photo:
			showSelectImageDialog();			
			break;
			
		case R.id.photo:
			if(null != mCurrUserInfo && mCurrUserInfo.mAvatarServerVersion>0){
				Intent intent = new Intent(this, BigImageActivity.class);
				intent.putExtra(BigImageActivity.INTENT_KEY_ACCOUNT, mCurrAccount);
				startActivity(intent);
			}else{
				showSelectImageDialog();		
			}
			break;
			
		case R.id.setting_name:
			InputFilter nameFilter = new InputFilter.LengthFilter(30);
			conEdit.setFilters(new InputFilter[] {nameFilter});
			conEdit.setText(TextUtils.isEmpty(mCurrUserInfo.mRealName) ? "" : mCurrUserInfo.mRealName);
			conEdit.setSelection(conEdit.getText().toString().trim().length());
			
			new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.setting_name)).setView(layout)
			.setNegativeButton(R.string.bnt_cancel, null)
			.setPositiveButton(R.string.bnt_confirm, new DialogInterface.OnClickListener() {			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String content = conEdit.getText().toString().trim();
					if(!content.equals(mCurrUserInfo.mRealName)){
						showProgressDialog();
						mSettingManager.modifySelfInfo("name", content);
					}
				}
			}).create().show();
			break;
		case R.id.setting_sex:
			/*
			 * 弹出 性别 对话框
			 */
			int index = -1;
			if(Constants.SEX_MAN == mCurrUserInfo.mSex){
				index = 0;
			}else if(Constants.SEX_WOMAN == mCurrUserInfo.mSex){
				index = 1;
			}
			AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
			alertDialog.setSingleChoiceItems(R.array.setting_sex, index, 
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							int sex = -1;
							if(which == 0){
								sex = Constants.SEX_MAN;
							}else{
								sex = Constants.SEX_WOMAN;
							}
							
							if(mCurrUserInfo.mSex != sex){
								showProgressDialog();
								mSettingManager.modifySelfInfo("sex", String.valueOf(sex));
							}
							dialog.dismiss();
						}
					});
			alertDialog.create().show();
			break;
		case R.id.setting_addr:	
			InputFilter addrFilter = new InputFilter.LengthFilter(50);
			conEdit.setFilters(new InputFilter[] {addrFilter});
			conEdit.setText(TextUtils.isEmpty(mCurrUserInfo.mAddress) ? "" : mCurrUserInfo.mAddress);
			conEdit.setSelection(conEdit.getText().toString().trim().length());
			
			new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.setting_addr)).setView(layout)
			.setNegativeButton(R.string.bnt_cancel, null)
			.setPositiveButton(R.string.bnt_confirm, new DialogInterface.OnClickListener() {			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String address = conEdit.getText().toString().trim();
					if(!address.equals(mCurrUserInfo.mAddress)){
						showProgressDialog();
						mSettingManager.modifySelfInfo("address", address);
					}
				}
			}).create().show();
			break;
			
		case R.id.setting_mobile:
			InputFilter mobileFilter = new InputFilter.LengthFilter(20);
			conEdit.setFilters(new InputFilter[] {mobileFilter});
			conEdit.setText(TextUtils.isEmpty(mCurrUserInfo.mMobile) ? "" : mCurrUserInfo.mMobile);
			conEdit.setSelection(conEdit.getText().toString().trim().length());
			conEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
			new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.setting_mobile)).setView(layout)
			.setNegativeButton(R.string.bnt_cancel, null).
			setPositiveButton(R.string.bnt_confirm, new DialogInterface.OnClickListener() {			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String mobile = conEdit.getText().toString().trim();
					if(!mobile.equals(mCurrUserInfo.mMobile)){
						if(TextUtils.isEmpty(mobile) || RegularCheckTools.isMobile(mobile)){
							showProgressDialog();
							mSettingManager.modifySelfInfo("mobile", mobile);
						}else{
							if(!TextUtils.isEmpty(mobile)){
								OtherUtilities.showToastText(SettingPersonalActivity.this, getString(R.string.register_mobile_format_error));
							}
						}
					}
				}
			}).create().show();
			break;
		case R.id.setting_email:
			InputFilter emailFilter = new InputFilter.LengthFilter(50);
			conEdit.setFilters(new InputFilter[] {emailFilter});
			conEdit.setText(TextUtils.isEmpty(mCurrUserInfo.mEmail) ? "" : mCurrUserInfo.mEmail);
			conEdit.setSelection(conEdit.getText().toString().trim().length());
			
			new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.setting_email)).setView(layout)
			.setNegativeButton(R.string.bnt_cancel, null)
			.setPositiveButton(R.string.bnt_confirm, new DialogInterface.OnClickListener() {			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String email = conEdit.getText().toString().trim();
					if(!email.equals(mCurrUserInfo.mEmail)){
						if(TextUtils.isEmpty(email) || RegularCheckTools.isEmail(email)){
							showProgressDialog();
							mSettingManager.modifySelfInfo("email", email);
						}else{
							if(!TextUtils.isEmpty(email)){
								OtherUtilities.showToastText(SettingPersonalActivity.this, getString(R.string.register_emile_format_error));
							}
						}
					}
					dialog.dismiss();
				}
			}).create().show();
			break;
		}
	}
	
	@Override
	protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(Activity.RESULT_OK != resultCode){
			return;
		}
		
		switch (requestCode) {
		case PHOTO:
			String imagePath = mTakePhotoTempName;
			if(TextUtils.isEmpty(imagePath)){
				return;
			}
			File file = new File(imagePath);
			if(file.exists() && file.length()>0){
				Uri imageUri = Uri.fromFile(file);
				startPhotoCutter(imageUri);
			}
			mTakePhotoTempName = null;
			break;
			
		case LOCAL_PHOTO:
			if (null != data) {
				startPhotoCutter(data.getData());
			}
			break;
			
		case RESULT_PHOTO:
			if(!TextUtils.isEmpty(mTempName)){
				String tempName = mTempName;
				Bitmap tempBitmap = ImageUtil.resizeBitmapForce(tempName, Constants.SETTING_AVATAR_WIDTH, Constants.SETTING_AVATAR_HEIGHT);// 640*480
				if(null != tempBitmap){
					try {
						File ff = ImageUtil.saveBitmap(tempBitmap, tempName);// 将压缩后的图片保存到临时文件夹
						if(null!=ff && ff.exists()){
							showProgressDialog();
							mSettingManager.uploadAvatar(tempName);
						}
						
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				mTempName = null;
			}
			break;
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
//		FileUtil.deleteDir(Constants.TEMP_PATH);
	}

	@Override
	public void processResult(Message msg) {
		switch (msg.what) {
		case SettingUiMessage.RESPONSE_MODIFY_SELFINFO:
			closeProgressDialog();
			if(HttpResponseCode.OK == msg.arg1){
				initData();
				
			}else if(HttpResponseCode.NO_NETWORK == msg.arg1){
				OtherUtilities.showToastText(this, getString(R.string.network_off));
			}else{
				OtherUtilities.showToastText(this, getString(R.string.operation_failed));
			}
			break;
			
		case SettingUiMessage.RESPONSE_UPLOAD_AVATAR:
			closeProgressDialog();
			if(HttpResponseCode.OK == msg.arg1){
				initData();
				
			}else if(HttpResponseCode.UPLOAD_IMAGE_ERROR == msg.arg1){
				OtherUtilities.showToastText(this, getString(R.string.setting_upload_avatar_failed));
			}else if(HttpResponseCode.NO_NETWORK == msg.arg1){
				OtherUtilities.showToastText(this, getString(R.string.network_off));
			}else{
				OtherUtilities.showToastText(this, getString(R.string.operation_failed));
			}
			break;
			
		case ContactUiMessage.RESPONSE_GET_AVATAR_THUMB:
			if(RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "").equalsIgnoreCase((String)msg.obj)){
				initData();
			}
		}
	}
}
