package com.rongkecloud.test.ui.setting;

import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.test.R;
import com.rongkecloud.test.http.HttpResponseCode;
import com.rongkecloud.test.manager.SettingManager;
import com.rongkecloud.test.manager.uihandlermsg.SettingUiMessage;
import com.rongkecloud.test.utility.OtherUtilities;

public class SettingFeedbackActivity extends RKCloudChatBaseActivity {
	// UI组件
	private TextView mTitle;
	private Button mConfirmBtn;
	private Spinner mFeedbackSpinner;
	private EditText mFeedbackEdit;
	
	private SettingManager mSettingManager;
	
	private int mFeedbackType = 1;
	private String mFeedbackContent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting_feedback);
		initUIAndListener();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mSettingManager.bindUiHandler(mUiHandler);
	}
	
	private void initUIAndListener(){
		mTitle = (TextView) findViewById(R.id.txt_title);
		mTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		mTitle.setText(R.string.bnt_return);
		mTitle.setOnClickListener(mExitListener);

        TextView text_title_content = (TextView) findViewById(R.id.text_title_content);
        text_title_content.setText(R.string.setting_feedback);

		mConfirmBtn = (Button) findViewById(R.id.title_right_btn);
		mConfirmBtn.setVisibility(View.VISIBLE);
//        mConfirmBtn.setTextColor(getResources().getColor(R.color.title_content));
        mConfirmBtn.setBackgroundColor(getResources().getColor(R.color.bg_transparent));
		mConfirmBtn.setText(getResources().getString(R.string.bnt_confirm));
		
		mFeedbackSpinner = (Spinner) findViewById(R.id.feedback_spinner);
		mFeedbackEdit = (EditText) findViewById(R.id.feedback_content);
		
		mSettingManager = SettingManager.getInstance();
		
		mFeedbackSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				mFeedbackType = arg2 + 1;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
		});
		
		mConfirmBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mFeedbackContent = mFeedbackEdit.getText().toString();
				if(!TextUtils.isEmpty(mFeedbackContent)){
					showProgressDialog();
					mSettingManager.addFeedback(mFeedbackType, mFeedbackContent);
				}else{
					OtherUtilities.showToastText(SettingFeedbackActivity.this, getString(R.string.setting_feedback_content_not_null));
				}
			}
		});
	}
	
	@Override
	public void processResult(Message msg) {
		switch (msg.what) {
		case SettingUiMessage.RESPONSE_ADD_FEEDBACK:
			closeProgressDialog();
			if(HttpResponseCode.OK == msg.arg1){
				OtherUtilities.showToastText(SettingFeedbackActivity.this, getString(R.string.operation_success));
				finish();
			}else if(HttpResponseCode.NO_NETWORK == msg.arg1){
				OtherUtilities.showToastText(SettingFeedbackActivity.this, getString(R.string.network_off));
			}else{
				OtherUtilities.showToastText(SettingFeedbackActivity.this, getString(R.string.operation_failed));
			}
			break;
		}
	}
}
