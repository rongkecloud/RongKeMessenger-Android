package com.rongkecloud.test.ui.contact;

import java.io.File;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.widget.ImageView;

import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.test.R;
import com.rongkecloud.test.entity.ContactInfo;
import com.rongkecloud.test.http.HttpResponseCode;
import com.rongkecloud.test.manager.PersonalManager;
import com.rongkecloud.test.manager.uihandlermsg.ContactUiMessage;
import com.rongkecloud.test.utility.OtherUtilities;

public class BigImageActivity extends RKCloudChatBaseActivity {
	
	//进入此界面需要传入的参数
	public static final String INTENT_KEY_ACCOUNT = "intent_key_account";//需要查看的图片UID key值
	
	private ImageView mImageView;
	
	private String mAccount;
	private PersonalManager mPersonalManager;	
	private ContactInfo mContactObj;
	private boolean mHeaderImageDowning = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.big_image);
		mAccount = getIntent().getStringExtra(INTENT_KEY_ACCOUNT);
		if(TextUtils.isEmpty(mAccount)){
			finish();
			return;
		}
		initViewAndSetListener();
		mPersonalManager = PersonalManager.getInstance();		
		mContactObj = mPersonalManager.getContactInfo(mAccount);
		// 同步头像
		if(null!=mContactObj && mContactObj.mAvatarServerVersion>0){
			if(TextUtils.isEmpty(mContactObj.mPath) || !new File(mContactObj.mPath).exists() 
					|| mContactObj.mAvatarServerVersion>mContactObj.mAvatarClientVersion){
				showProgressDialog();
				mPersonalManager.getAvatarBig(mContactObj.mAccount, mContactObj.mAvatarServerVersion);
				mHeaderImageDowning = true;
			}
		}
		initData();
	}

	private void initViewAndSetListener(){
		mImageView = (ImageView) this.findViewById(R.id.imgShowBigImage);
		mImageView.setOnClickListener(mExitListener);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mPersonalManager.bindUiHandler(mUiHandler);
	}
	
	private void initData(){		
		if(null!=mContactObj){
			if(mHeaderImageDowning){
				if(null!=mContactObj.mThumbPath && new File(mContactObj.mThumbPath).exists()){
					mImageView.setImageBitmap(BitmapFactory.decodeFile(mContactObj.mThumbPath));
				}
			}else{
				if(null!=mContactObj.mPath && new File(mContactObj.mPath).exists()){
					mImageView.setImageBitmap(BitmapFactory.decodeFile(mContactObj.mPath));
				}else{
					if(null!=mContactObj.mThumbPath && new File(mContactObj.mThumbPath).exists()){
						mImageView.setImageBitmap(BitmapFactory.decodeFile(mContactObj.mThumbPath));
					}
				}
			}
		}
	}

	@Override
	public void processResult(Message msg) {		
		switch (msg.what) {
		case ContactUiMessage.RESPONSE_GET_AVATAR_BIG:
			closeProgressDialog();
			mHeaderImageDowning = false;
			if(HttpResponseCode.OK == msg.arg1){
				mContactObj = mPersonalManager.getContactInfo(mAccount);
				initData();
				
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
