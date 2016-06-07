package com.rongkecloud.test.ui.setting.mms;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rongkecloud.chat.RKCloudChatConfigManager;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.test.R;

public class SettingMmsActivity extends RKCloudChatBaseActivity implements OnClickListener{
	private LinearLayout mNotifyLayout;
	private ImageView mPlayModel;	
	private RKCloudChatConfigManager mConfigManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting_mms);
		initUIAndListeners();
		mConfigManager = RKCloudChatConfigManager.getInstance(this);
		if(null != mConfigManager){
			mPlayModel.setSelected(mConfigManager.getVoicePlayModel());
		}
	}
	
	private void initUIAndListeners(){
		// 设置title
		TextView titleTV = (TextView)findViewById(R.id.txt_title);
		titleTV.setText(R.string.bnt_return);
		titleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		titleTV.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

        TextView text_title_content = (TextView)findViewById(R.id.text_title_content);
        text_title_content.setText(R.string.chatdemo_setting_mms);
		
		mNotifyLayout = (LinearLayout)findViewById(R.id.layout_newmsg_notify);
		mPlayModel = (ImageView)findViewById(R.id.audio_playmodel);
		
		mNotifyLayout.setOnClickListener(this);
		mPlayModel.setOnClickListener(this);
	}
	
	@Override
	public void processResult(Message arg0) {
		
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if(R.id.layout_newmsg_notify == id){
			startActivity(new Intent(this, SettingMmsNotifyActivity.class));
			
		}else if(R.id.audio_playmodel == id){
			boolean isSelected = mPlayModel.isSelected();
			mPlayModel.setSelected(!isSelected);
			if(null != mConfigManager){
				mConfigManager.setVoicePlayModel(!isSelected);
			}
		}
	}
}
