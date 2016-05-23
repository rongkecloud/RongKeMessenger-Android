package com.rongkecloud.test.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.sdkbase.RKCloudBaseErrorCode;
import com.rongkecloud.test.R;
import com.rongkecloud.test.http.HttpResponseCode;
import com.rongkecloud.test.manager.AccountManager;
import com.rongkecloud.test.manager.SDKManager;
import com.rongkecloud.test.manager.uihandlermsg.AccountUiMessage;
import com.rongkecloud.test.utility.OtherUtilities;
import com.rongkecloud.test.utility.RegularCheckTools;

public class RegisterActivity extends RKCloudChatBaseActivity{
	private EditText mNameTV;
	private EditText mPwdTV;
	private EditText mPwdAgainTv;
	private Button mRegisterBnt;	
	private String regName;
	private String regPwd;
	
	private AccountManager mAccountManager;	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.register);
		initViewAndListeners();
		mAccountManager = AccountManager.getInstance();		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mAccountManager.bindUiHandler(mUiHandler);
		SDKManager.getInstance().bindUiHandler(mUiHandler);
	}
	
	private void initViewAndListeners(){
		TextView titleTV = (TextView) findViewById(R.id.txt_title);
		titleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		titleTV.setOnClickListener(mExitListener);
		titleTV.setText(getString(R.string.bnt_return));

        TextView text_title_content = (TextView) findViewById(R.id.text_title_content);
        text_title_content.setText(getString(R.string.register_btn));

		mNameTV = (EditText)findViewById(R.id.register_name);		
		mPwdTV = (EditText)findViewById(R.id.register_pwd);
		mPwdAgainTv = (EditText) findViewById(R.id.register_pwd_again);
		
		mRegisterBnt = (Button)findViewById(R.id.registerbnt);	
		mRegisterBnt.setEnabled(true);	
		
		mRegisterBnt.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				doRegister();									
			}
		});	
	}
	
	private void doRegister(){
		regName = mNameTV.getText().toString().trim();
		if(!RegularCheckTools.checkAccount(regName)){
			OtherUtilities.showToastText(this, getString(R.string.register_name_format_error));
			mNameTV.setFocusable(true);
			return;
		}
		
		regPwd = mPwdTV.getText().toString().trim();
		if(!RegularCheckTools.checkPwd(regPwd)){
			OtherUtilities.showToastText(this, getString(R.string.register_pwd_format_error));
			mPwdTV.setFocusable(true);
			return;
		}
		
		String regPwdAgain = mPwdAgainTv.getText().toString().trim();
		if(!RegularCheckTools.checkPwd(regPwdAgain)){
			OtherUtilities.showToastText(this, getString(R.string.register_pwd_again_format_error));
			mPwdAgainTv.setFocusable(true);
			return;
		}
		
		if(!regPwd.equals(regPwdAgain)){
			OtherUtilities.showToastText(this, getString(R.string.register_pwd_not_equal));
			mPwdAgainTv.setFocusable(true);
			return;
		}
	
		showProgressDialog(getString(R.string.tip), getString(R.string.register_tip));
		mAccountManager.register(regName, regPwd);
	}	
	
	@Override
	public void processResult(Message msg) {
		switch(msg.what){
		case AccountUiMessage.RESPONSE_REGISTER:
			closeProgressDialog();
			if(msg.arg1 == HttpResponseCode.OK){
				showProgressDialog(getString(R.string.tip), getString(R.string.login_tip));
				mAccountManager.login(mNameTV.getText().toString().trim(), mPwdTV.getText().toString().trim());
			}else if(msg.arg1 == HttpResponseCode.NO_NETWORK){
				OtherUtilities.showToastText(this, getString(R.string.network_off));
			}else if(msg.arg1 == HttpResponseCode.ACCOUNT_EXIST){
				OtherUtilities.showToastText(this, getString(R.string.register_account_exist));
			}else{
				OtherUtilities.showToastText(this, getString(R.string.operation_failed));
			}
			break;
			
		case AccountUiMessage.RESPONSE_LOGIN:
			closeProgressDialog();
			if(msg.arg1 == HttpResponseCode.OK){
				showProgressDialog(getString(R.string.tip), getString(R.string.sdk_init_tip));
				SDKManager.getInstance().initSDK();
			}
			break;	
			
		case AccountUiMessage.SDK_INIT_FINISHED:
			closeProgressDialog();
			if(RKCloudBaseErrorCode.RK_NOT_NETWORK==msg.arg1 || RKCloudBaseErrorCode.RK_SUCCESS==msg.arg1){
				startActivity(new Intent(this, MainActivity.class));
				finish();
			}else if(RKCloudBaseErrorCode.BASE_ACCOUNT_PW_ERROR == msg.arg1){
				OtherUtilities.showToastText(this, getString(R.string.sdk_init_accounterror));
			}else if(RKCloudBaseErrorCode.BASE_ACCOUNT_BANNED == msg.arg1){
				OtherUtilities.showToastText(this, getString(R.string.sdk_init_banneduser));
			}else if(RKCloudBaseErrorCode.BASE_APP_KEY_AUTH_FAIL == msg.arg1){
				OtherUtilities.showToastText(this, getString(R.string.sdk_init_authfailed));
			}else{
				OtherUtilities.showToastText(this, getString(R.string.sdk_init_failed));
			}
			break;
		}
	}
}
