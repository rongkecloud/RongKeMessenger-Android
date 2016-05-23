package com.rongkecloud.test.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;

import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.sdkbase.RKCloudBaseErrorCode;
import com.rongkecloud.test.R;
import com.rongkecloud.test.entity.Account;
import com.rongkecloud.test.manager.AccountManager;
import com.rongkecloud.test.manager.SDKManager;
import com.rongkecloud.test.manager.uihandlermsg.AccountUiMessage;
import com.rongkecloud.test.system.Config;
import com.rongkecloud.test.system.ConfigKey;
import com.rongkecloud.test.system.RKCloudDemo;
import com.rongkecloud.test.utility.OtherUtilities;

public class WelcomeActivity extends RKCloudChatBaseActivity{
	private static final String TAG = WelcomeActivity.class.getSimpleName();
	
	private static final int MSG_ENTER_MAIN = 1;// 进入主页面
	private static final int MSG_ENTER_LOGIN = 2;// 进入登录页面
    private static final int GOTO_GUI = 3;// 进入登录页面
    boolean goToGui = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome);
		// 创建快捷方式
		OtherUtilities.createShortcut(this);		
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		onLoadCompleted();
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		onLoadCompleted();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		AccountManager.getInstance().bindUiHandler(mUiHandler);
		SDKManager.getInstance().bindUiHandler(mUiHandler);		
	}
	
	private void onLoadCompleted() {
        goToGui = RKCloudDemo.config.getBoolean(ConfigKey.SP_GUIDEPAGES_SHOW,false);
        if(goToGui){
            Account account = AccountManager.getInstance().getCurrentAccount();
            if(null == account){
                mUiHandler.sendEmptyMessage(MSG_ENTER_LOGIN);
            }else{
                if(SDKManager.getInstance().getSDKInitStatus()){
                    mUiHandler.sendEmptyMessage(MSG_ENTER_MAIN);
                }
            }
        }else{
            mUiHandler.sendEmptyMessage(GOTO_GUI);
        }
	}
	
	@Override
	public void processResult(Message msg) {
		switch(msg.what){
		case MSG_ENTER_LOGIN:
			startActivity(new Intent(this, LoginActivity.class));
			finish();
			break;
			
		case MSG_ENTER_MAIN:
			startActivity(new Intent(this, MainActivity.class));
			finish();
			break;

        case GOTO_GUI:
            startActivity(new Intent(this, GuideActivity.class));
            finish();
            break;
		case AccountUiMessage.SDK_INIT_FINISHED:
			if(RKCloudBaseErrorCode.RK_NOT_NETWORK==msg.arg1 || RKCloudBaseErrorCode.RK_SUCCESS==msg.arg1){
                goToGui = RKCloudDemo.config.getBoolean(ConfigKey.SP_GUIDEPAGES_SHOW,false);
                if(goToGui){
                    mUiHandler.sendEmptyMessage(MSG_ENTER_MAIN);
                }
			}else if(RKCloudBaseErrorCode.BASE_ACCOUNT_PW_ERROR == msg.arg1){
				OtherUtilities.showToastText(this, getString(R.string.sdk_init_accounterror));
				AccountManager.getInstance().logout();
				startActivity(new Intent(this, LoginActivity.class));
				
			}else if(RKCloudBaseErrorCode.BASE_ACCOUNT_BANNED == msg.arg1){
				OtherUtilities.showToastText(this, getString(R.string.sdk_init_banneduser));
				AccountManager.getInstance().logout();
				startActivity(new Intent(this, LoginActivity.class));
				
			}else if(RKCloudBaseErrorCode.BASE_APP_KEY_AUTH_FAIL == msg.arg1){
				OtherUtilities.showToastText(this, getString(R.string.sdk_init_authfailed));
				AccountManager.getInstance().logout();
				startActivity(new Intent(this, LoginActivity.class));
				
			}else{
				OtherUtilities.showToastText(this, getString(R.string.sdk_init_failed));
			}
			break;
		}
	}
}