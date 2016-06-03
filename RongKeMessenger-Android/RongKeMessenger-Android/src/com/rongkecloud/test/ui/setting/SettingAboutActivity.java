package com.rongkecloud.test.ui.setting;

import android.os.Bundle;
import android.os.Message;
import android.text.Html;
import android.widget.TextView;

import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.test.R;
import com.rongkecloud.test.entity.Constants;
import com.rongkecloud.test.utility.SystemInfo;

public class SettingAboutActivity extends RKCloudChatBaseActivity{	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_about);
		
		// 设置title
		TextView titleTV = (TextView)findViewById(R.id.txt_title);
		titleTV.setText(R.string.bnt_return);
		titleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		titleTV.setOnClickListener(mExitListener);

        TextView text_title_content = (TextView)findViewById(R.id.text_title_content);
        text_title_content.setText(R.string.setting_about);

		TextView versionCode = (TextView) findViewById(R.id.vesioninfo);
		versionCode.setText(getString(R.string.setting_about_versoncode, getString(R.string.app_name), SystemInfo.getVersionName()));
		TextView copyright = (TextView) findViewById(R.id.copyright);
		copyright.setText(Html.fromHtml("&copy;"+getString(R.string.setting_about_copy_right)));
		
		TextView qqGroup = (TextView) findViewById(R.id.qqgroup);
		qqGroup.setText(getString(R.string.setting_about_qqgroup, Constants.SUPPORT_QQ_GROUP));
		
		TextView businessEmail = (TextView) findViewById(R.id.business_email);
		businessEmail.setText(getString(R.string.setting_about_business_email, Constants.SUPPORT_BUSINESS_EMAIL));
		
		TextView technologyEmail = (TextView) findViewById(R.id.technology_email);
		technologyEmail.setText(getString(R.string.setting_about_technology_email, Constants.SUPPORT_TECHNOLOGY_EMAIL));
		
		TextView website = (TextView) findViewById(R.id.wesite);
		website.setText(getString(R.string.setting_about_website, Constants.SUPPORT_WEBSITE));
	}

	@Override
	public void processResult(Message msg) {
		
	}
}
