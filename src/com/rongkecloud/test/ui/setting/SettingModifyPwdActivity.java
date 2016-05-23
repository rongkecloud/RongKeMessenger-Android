package com.rongkecloud.test.ui.setting;

import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.test.R;
import com.rongkecloud.test.http.HttpResponseCode;
import com.rongkecloud.test.manager.SettingManager;
import com.rongkecloud.test.manager.uihandlermsg.SettingUiMessage;
import com.rongkecloud.test.utility.OtherUtilities;
import com.rongkecloud.test.utility.RegularCheckTools;

public class SettingModifyPwdActivity extends RKCloudChatBaseActivity{

	private EditText mOldPwdET;
	private EditText mNewPwdET;
	private EditText mNewPwdAgainET;
	private Button mConfirmBtn;
	
	private SettingManager mSettingManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting_modify_pwd);
		initViewAndListeners();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mSettingManager.bindUiHandler(mUiHandler);
	}
	
	private void initViewAndListeners(){
		// 设置title
		TextView titleTV = (TextView)findViewById(R.id.txt_title);
		titleTV.setText(R.string.bnt_return);
		titleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		titleTV.setOnClickListener(mExitListener);

        TextView text_title_content = (TextView)findViewById(R.id.text_title_content);
        text_title_content.setText(getString(R.string.setting_modify_pwd));

		mConfirmBtn = (Button) findViewById(R.id.title_right_btn);
		mConfirmBtn.setVisibility(View.VISIBLE);
        mConfirmBtn.setTextColor(getResources().getColor(R.color.text_Color_Hint));
        mConfirmBtn.setBackgroundColor(getResources().getColor(R.color.bg_transparent));
		mConfirmBtn.setText(getResources().getString(R.string.bnt_confirm));
		
		mOldPwdET = (EditText) findViewById(R.id.setting_old_pwd);
		mNewPwdET = (EditText) findViewById(R.id.setting_new_pwd);
		mNewPwdAgainET = (EditText) findViewById(R.id.setting_new_pwd_again);
		
		mSettingManager = SettingManager.getInstance();
		
		mConfirmBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				modifyPwd();
			}
		});
	}
	
	private void modifyPwd(){
		String oldPwd = mOldPwdET.getText().toString().trim();
		String newPwd = mNewPwdET.getText().toString().trim();
		String confrimPwd = mNewPwdAgainET.getText().toString().trim();
		if(!RegularCheckTools.checkPwd(oldPwd)){
			OtherUtilities.showToastText(this, getString(R.string.setting_old_pwd_format_error));
			mOldPwdET.setFocusable(true);
			return;
		}
		if(!RegularCheckTools.checkPwd(newPwd)){
			OtherUtilities.showToastText(this, getString(R.string.setting_new_pwd_format_error));
			mNewPwdET.setFocusable(true);
			return;
		}
		if(!confrimPwd.equals(newPwd)){
			OtherUtilities.showToastText(this, getString(R.string.setting_new_pwd_again_format_error));
			mNewPwdAgainET.setFocusable(true);
			return;
		}
		
		showProgressDialog();
		mSettingManager.modifyPwd(oldPwd, newPwd);
	}
	
	@Override
	public void processResult(Message msg) {
		switch (msg.what) {
		case SettingUiMessage.RESPONSE_MODIFY_PWD:
			closeProgressDialog();
			if(HttpResponseCode.OK == msg.arg1){
				finish();
				OtherUtilities.showToastText(SettingModifyPwdActivity.this, getString(R.string.setting_modify_pwd_success));
				
			}else if(HttpResponseCode.PWD_ERROR == msg.arg1){
				OtherUtilities.showToastText(this, getString(R.string.setting_modify_pwd_old_error));
				
			}else{
				OtherUtilities.showToastText(SettingModifyPwdActivity.this, getString(R.string.operation_failed));
			}
			break;
		}
	}
}
