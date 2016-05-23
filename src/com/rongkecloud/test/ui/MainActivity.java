package com.rongkecloud.test.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import com.rongkecloud.chat.demo.RKCloudChatMmsManager;
import com.rongkecloud.chat.demo.RKCloudChatUiHandlerMessage;
import com.rongkecloud.chat.demo.ui.RKCloudChatListFragment;
import com.rongkecloud.chat.demo.ui.RKCloudChatMsgActivity;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseFragment;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseFragmentActivity;
import com.rongkecloud.sdkbase.RKCloudBaseErrorCode;
import com.rongkecloud.test.R;
import com.rongkecloud.test.entity.Constants;
import com.rongkecloud.test.http.HttpResponseCode;
import com.rongkecloud.test.manager.AccountManager;
import com.rongkecloud.test.manager.PersonalManager;
import com.rongkecloud.test.manager.SDKManager;
import com.rongkecloud.test.manager.SettingManager;
import com.rongkecloud.test.manager.uihandlermsg.AccountUiMessage;
import com.rongkecloud.test.manager.uihandlermsg.SettingUiMessage;
import com.rongkecloud.test.system.ConfigKey;
import com.rongkecloud.test.system.RKCloudDemo;
import com.rongkecloud.test.ui.contact.AddressListActivity;
import com.rongkecloud.test.ui.reminder.ReminderActivity;
import com.rongkecloud.test.ui.reminder.ReminderBroadcast;
import com.rongkecloud.test.ui.setting.SettingMainActivity;
import com.rongkecloud.test.utility.NetworkUtil;
import com.rongkecloud.test.utility.OtherUtilities;
import com.rongkecloud.test.utility.SystemInfo;

import java.util.Map;

public class MainActivity extends RKCloudChatBaseFragmentActivity{
	private static final String TAG = MainActivity.class.getSimpleName();
	
	public static final String ACTION_TO_MSGUI = "action.to.msg";
	public static final String ACTION_TO_MSGUI_PARAMS_SINGLECHATID = "action.to.msg.param.single.chatid";
	public static final String ACTION_TO_MSGUI_PARAMS_GROUPCHATID = "action.to.msg.param.group.chatid";
	
	private Button[] mTabs;
	private RKCloudChatBaseFragment[] mAllFragments;	
	private int mCurrentTabIndex = -1;// 当前fragment的index
	
	// UI元素
	private Button mConvBtn, mAddressBtn, mSettingBtn;
	private TextView mMsgUnreadCntTV;
		 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		if(null == RKCloudDemo.context){
			finish();
			return;
		}
		setContentView(R.layout.main);
		initUI();
		initTabs();		
		resolveIntent(getIntent());
		SettingManager.getInstance().bindMainUiHandler(mUiHandler);
//		checkServerUpgrade();
		// 同步个人信息
		PersonalManager.getInstance().syncUserInfo(AccountManager.getInstance().getAccount());
	}
	
	@Override
	protected void onNewIntent(Intent intent) {		
		super.onNewIntent(intent);
		resolveIntent(intent);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		if(RKCloudDemo.config.getBoolean(ConfigKey.ACCOUNT_HAS_NOT_EXIST, false)){
			AccountManager.getInstance().logout();
			startActivity(new Intent(this, LoginActivity.class));
			finish();
			return;
		}
	}
		
	@Override
	protected void onResume() {
		super.onResume();
		RKCloudChatMmsManager.getInstance(this).bindMainUiHandler(mUiHandler);		
		SettingManager.getInstance().bindMainUiHandler(mUiHandler);
		SDKManager.getInstance().bindUiHandler(mUiHandler);
		
		selectTab(RKCloudDemo.config.getInt(ConfigKey.LAST_SELECTED_TAB, 0));
		if(SDKManager.getInstance().getSDKInitStatus()){
			if(mAllFragments[mCurrentTabIndex].isAdded()){
				mAllFragments[mCurrentTabIndex].refresh();
			}
			updateMsgUnreadCnt();
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			moveTaskToBack(true);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	/*
	 * 解析intent内容
	 */
	private void resolveIntent(Intent intent){			
		String action = intent.getAction();
		if(!TextUtils.isEmpty(action)){
			if(ACTION_TO_MSGUI.equals(action)){		
				RKCloudDemo.config.put(ConfigKey.LAST_SELECTED_TAB, 0);
				
				String singleChatId = intent.getStringExtra(ACTION_TO_MSGUI_PARAMS_SINGLECHATID);
				String groupChatId = intent.getStringExtra(ACTION_TO_MSGUI_PARAMS_GROUPCHATID);

				Intent msgIntent = new Intent(this, RKCloudChatMsgActivity.class);
				if(!TextUtils.isEmpty(singleChatId)){
					msgIntent.putExtra(RKCloudChatMsgActivity.INTENT_KEY_MSGLIST_CHATID, singleChatId);
				}else if(!TextUtils.isEmpty(groupChatId)){
					msgIntent.putExtra(RKCloudChatMsgActivity.INTENT_KEY_MSGLIST_GROUPID, groupChatId);
				}
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(msgIntent);
			}
		}
	}
	
	private void initUI(){
		mMsgUnreadCntTV = (TextView)findViewById(R.id.msg_unread_cnt);
		mConvBtn = (Button) findViewById(R.id.bnt_conversation);
		mAddressBtn = (Button) findViewById(R.id.bnt_address_book);
		mSettingBtn = (Button) findViewById(R.id.bnt_setting);	
	}
	
	/*
	 * 初始化tab项
	 */
	private void initTabs(){
		mTabs = new Button[3];
		mTabs[0] = mConvBtn;
		mTabs[1] = (Button) findViewById(R.id.bnt_address_book);
		mTabs[2] = mSettingBtn;	
		for(int i=0; i<mTabs.length; i++){
			final int index = i;
			mTabs[i].setOnClickListener(new OnClickListener() {		
				@Override
				public void onClick(View v) {
					selectTab(index);
				}
			});
		}
		
		mAllFragments = new RKCloudChatBaseFragment[] { new RKCloudChatListFragment(), new AddressListActivity(), new SettingMainActivity() };
	}
	
	private void selectTab(int index){
		if(mCurrentTabIndex != index){
			FragmentTransaction trx = getSupportFragmentManager().beginTransaction();
			if(-1 != mCurrentTabIndex){
				trx.hide(mAllFragments[mCurrentTabIndex]);
			}
			if (!mAllFragments[index].isAdded()) {
				trx.add(R.id.fragment_container, mAllFragments[index]);
			}
			trx.show(mAllFragments[index]).commit();
		}
		if(-1 != mCurrentTabIndex){
			mTabs[mCurrentTabIndex].setSelected(false);
		}
		// 把当前tab设为选中状态
		mTabs[index].setSelected(true);
		mCurrentTabIndex = index;
		RKCloudDemo.config.put(ConfigKey.LAST_SELECTED_TAB, mCurrentTabIndex);
	}
		
	/*
	 * 检查更新
	 */
	private void checkServerUpgrade(){
		long currTime = System.currentTimeMillis();
		long lastCheckServerTime = RKCloudDemo.config.getLong(ConfigKey.UPGRADE_LASTTIME, 0);
		if(0 == lastCheckServerTime || (currTime-lastCheckServerTime > Constants.CHECK_TIME_INTERVAL)){
			if(NetworkUtil.isNetworkAvaliable()){
				SettingManager.getInstance().checkUpdate(true);
			}
		}
	}
	
	/*
	 * 更新会话的未读条数
	 */
	private void updateMsgUnreadCnt(){
		int unreadCnt = RKCloudChatMmsManager.getInstance(this).getAllUnReadMsgCounts();
		if(unreadCnt > 0){
			mMsgUnreadCntTV.setVisibility(View.VISIBLE);
			mMsgUnreadCntTV.setText(String.valueOf(unreadCnt));
		}else{
			mMsgUnreadCntTV.setVisibility(View.INVISIBLE);
		}
	}
	
	@Override
	public void processResult(Message msg) {	
		switch(msg.what){
		case AccountUiMessage.SDK_INIT_FINISHED:
			if(RKCloudBaseErrorCode.RK_NOT_NETWORK==msg.arg1 || RKCloudBaseErrorCode.RK_SUCCESS==msg.arg1){
				if(mCurrentTabIndex >= 0){
					if(mAllFragments[mCurrentTabIndex].isAdded()){
						mAllFragments[mCurrentTabIndex].refresh();
					}
					updateMsgUnreadCnt();
				}
			}
			break;
			
		case RKCloudChatUiHandlerMessage.UNREAD_MSG_COUNT_CHANGED:
			updateMsgUnreadCnt();
			break;
			
		case SettingUiMessage.RESPONSE_CHECK_UPDATE:
			Map<String, String> values = (Map<String, String>)msg.obj;
			boolean autoCheck = "1" == values.get("autoCheck");
			
			if(HttpResponseCode.OK == msg.arg1){				
				Intent intent = new Intent(this, ReminderActivity.class);				
				if (SystemInfo.isForceUpgrade(values.get("minVer"))) {
					intent.setAction(ReminderBroadcast.ACTION_REMIND_FORCE_UPGRADE);
				} else {
					intent.setAction(ReminderBroadcast.ACTION_REMIND_UPGRADE);
				}

				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.putExtra(ReminderBroadcast.KEY_UPGRADE_DESC, values.get("desc"));
				intent.putExtra(ReminderBroadcast.KEY_UPGRADE_FILESIZE, Long.valueOf(values.get("fileSize")));
				intent.putExtra(ReminderBroadcast.KEY_UPGRADE_NEW_VER, values.get("updateVer"));
				intent.putExtra(ReminderBroadcast.KEY_UPGRADE_URL, values.get("url"));
				startActivity(intent);
				
			}else{
				if(!autoCheck){
					if(HttpResponseCode.NO_CHECK_UPDATE == msg.arg1){
						OtherUtilities.showToastText(this, getString(R.string.setting_update_notneed));
					}else if(HttpResponseCode.NO_NETWORK == msg.arg1){
						OtherUtilities.showToastText(this, getString(R.string.network_off));
					}else{
						OtherUtilities.showToastText(this, getString(R.string.operation_failed));
					}
				}
			}
			break;
		}		
	}
}
