package com.rongkecloud.chat.demo.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

import com.rongkecloud.sdkbase.RKCloud;

public class RKCloudChatReceiveThirdInfoActivity extends Activity {
	public static final String TAG = RKCloudChatReceiveThirdInfoActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// SDK未初始化成功时返回
		if(!RKCloud.isSDKInitSuccess()){
			finish();
			return;
		}

		Intent receivedIntent = getIntent();
		if (receivedIntent != null) {
			String action = receivedIntent.getAction();
			Intent intent = new Intent(this, RKCloudChatForwardActivity.class);

			if (Intent.ACTION_SEND.equals(action)) {
				// 表示单张图片的分享
				intent.setType(receivedIntent.getType());
				intent.putExtras(receivedIntent.getExtras());
				intent.putExtra(RKCloudChatForwardActivity.INTENT_KEY_OUTSIDE_SHARE_ISMUTIL, false);
				
			} if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
				// 表示多张图片的分享
				intent.setType(receivedIntent.getType());
				intent.putExtras(receivedIntent.getExtras());
				intent.putExtra(RKCloudChatForwardActivity.INTENT_KEY_OUTSIDE_SHARE_ISMUTIL, true);
			} 

			startActivity(intent);
			finish();
		}
	}
}
