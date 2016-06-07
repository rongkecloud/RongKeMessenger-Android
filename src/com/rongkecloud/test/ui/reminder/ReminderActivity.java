package com.rongkecloud.test.ui.reminder;

import java.io.File;

import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatCustomDialog;
import com.rongkecloud.test.R;
import com.rongkecloud.test.entity.Constants;
import com.rongkecloud.test.http.HttpResponseCode;
import com.rongkecloud.test.http.Progress;
import com.rongkecloud.test.manager.AccountManager;
import com.rongkecloud.test.manager.SettingManager;
import com.rongkecloud.test.manager.uihandlermsg.SettingUiMessage;
import com.rongkecloud.test.system.ConfigKey;
import com.rongkecloud.test.system.RKCloudDemo;
import com.rongkecloud.test.ui.LoginActivity;
import com.rongkecloud.test.utility.OtherUtilities;

public class ReminderActivity extends RKCloudChatBaseActivity{
	private static final String TAG = ReminderActivity.class.getSimpleName();	

	// 升级时使用的属性值
	private RKCloudChatCustomDialog mProgressDialog = null;
	private ProgressBar mUpgradeProgressBar;
	private TextView mUpgradeValue, mUpgradeMax;

	private SettingManager mSettingManager;
	
	private OnCancelListener mCancelListener = new OnCancelListener() {
		@Override
		public void onCancel(DialogInterface dialog) {
			finish();
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		String action = getIntent().getAction();

		mSettingManager = SettingManager.getInstance();
		mSettingManager.bindUiHandler(mUiHandler);
		
		if(ReminderBroadcast.ACTION_REMIND_BANNED_USER.equals(action)){
			RKCloudDemo.config.put(ConfigKey.ACCOUNT_HAS_NOT_EXIST, true);
			showBannedUserDialog();
		} else if(ReminderBroadcast.ACTION_REMIND_KICKED_USER.equals(action)) {
			RKCloudDemo.config.put(ConfigKey.ACCOUNT_HAS_NOT_EXIST, true);
			showKickedUserDialog();
		} else if(ReminderBroadcast.ACTION_REMIND_UPGRADE.equals(action)){
			showUpgradeDialog(false);
		} else if(ReminderBroadcast.ACTION_REMIND_FORCE_UPGRADE.equals(action)){
			showUpgradeDialog(true);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(null != mProgressDialog && mProgressDialog.isShowing()){
			mProgressDialog.dismiss();
		}
	}
	
	// 被禁用户
	private void showBannedUserDialog(){
		RKCloudChatCustomDialog.Builder builder = new RKCloudChatCustomDialog.Builder(this)
		.setTitle(R.string.forbiden_title)
		.setMessage(getString(R.string.forbiden_content))
		.setPositiveButton(R.string.bnt_close, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {							
				quitApp();
			}
		}).setOnCancelListener(new OnCancelListener() {			
			@Override
			public void onCancel(DialogInterface dialog) {					
				quitApp();
			}
		}).setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				finish();
			}
		}).setCancelable(false);
		builder.create().show();					
	}
	
	// 被踢用户
	private void showKickedUserDialog() {
		RKCloudChatCustomDialog.Builder builder = new RKCloudChatCustomDialog.Builder(this)
		.setTitle(R.string.offline_title)
		.setMessage(R.string.offline_content)
		.setNegativeButton(R.string.bnt_close, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {	
				quitApp();
			}
		}).setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				quitApp();
			}
		}).setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				finish();
			}
		});
		builder.create().show();		
	}
	
	private void quitApp(){
		AccountManager.getInstance().logout();
		startActivity(new Intent(this, LoginActivity.class));
	}
	
	//版本更新
	private void showUpgradeDialog(boolean isForceUpgrade) {
		Intent intent = getIntent();
		String newVer = intent.getStringExtra(ReminderBroadcast.KEY_UPGRADE_NEW_VER);
		String desc = intent.getStringExtra(ReminderBroadcast.KEY_UPGRADE_DESC);
		desc = desc.replace("<br>", "\r\n").replace("<br />", "\r\n");
		final long fileSize = intent.getLongExtra(ReminderBroadcast.KEY_UPGRADE_FILESIZE, 1);
		final String url = intent.getStringExtra(ReminderBroadcast.KEY_UPGRADE_URL);		
		
		View view = getLayoutInflater().inflate(R.layout.upgrade_layout, null);
		TextView currVerTV = (TextView)view.findViewById(R.id.currver_tv);
		TextView descTV = (TextView)view.findViewById(R.id.desc_tv);
		TextView tipTV = (TextView)view.findViewById(R.id.tip_tv);
		currVerTV.setText(getString(R.string.setting_update_new_ver_prefix, newVer));
		descTV.setText(desc);
		tipTV.setText(getString(R.string.setting_update_confirm));
		
		
		RKCloudChatCustomDialog.Builder builder = new RKCloudChatCustomDialog.Builder(this)
		.setTitle(R.string.setting_update_low_tip)
		.setContentView(view)
		.setPositiveButton(R.string.setting_update_now, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mSettingManager.downloadApk(url, fileSize, Constants.UPGRADE_APK_PATH);
				showDownloadProgressDialog();
			}
		})
		.setOnCancelListener(mCancelListener)
		.setCancelable(false);
		
		if(!isForceUpgrade){
			builder.setNegativeButton(R.string.bnt_cancel, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
		}
		
		builder.create().show();
	}

	private void showDownloadProgressDialog() {
		View view = getLayoutInflater().inflate(R.layout.upgrade_progress_layout, null);
		mUpgradeProgressBar = (ProgressBar)view.findViewById(R.id.progressbar);
		mUpgradeValue = (TextView)view.findViewById(R.id.tv_percent);
		mUpgradeMax = (TextView)view.findViewById(R.id.tv_max);
		mUpgradeValue.setText("0%");
		mUpgradeMax.setText("0/100");
		
		mProgressDialog = new RKCloudChatCustomDialog.Builder(this)
		.setTitle(R.string.setting_update_downing)
		.setContentView(view)
		.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				RKCloudDemo.kit.abortHttpRequest(Constants.DOWNLOAD_APK);
				finish();
			}
		})
		.create();
		mProgressDialog.setCancelable(false);
		mProgressDialog.show();
	}
	
	@Override
	public void processResult(Message msg) {
		switch(msg.what) {
		case SettingUiMessage.RESPONSE_UPDATE_DOWNLOAD_PROGRESS:
			// 判断为空的目的，就是防止对象被回收，出现的空指针异常
			if(mUpgradeProgressBar != null) {
				int value = ((Progress) msg.obj).value;
				mUpgradeProgressBar.setProgress(value);
				mUpgradeValue.setText(value+"%");
				mUpgradeMax.setText(value+"/100");
			}			
			break;
			
		case SettingUiMessage.RESPONSE_DOWNLOAD_APK:
			if(null != mProgressDialog){
				mUpgradeProgressBar = null;
				mUpgradeValue = null;
				mUpgradeMax = null;
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			if(HttpResponseCode.OK == msg.arg1){
				// 升级操作
				Intent upgrade = new Intent(Intent.ACTION_VIEW);
				upgrade.setDataAndType(Uri.fromFile(new File(Constants.UPGRADE_APK_PATH)), "application/vnd.android.package-archive");
				upgrade.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				RKCloudDemo.context.startActivity(upgrade);
			}else{
				OtherUtilities.showToastText(this, getString(R.string.upgrade_downapk_failed));
			}
			finish();
			break;
		}
	}
}
