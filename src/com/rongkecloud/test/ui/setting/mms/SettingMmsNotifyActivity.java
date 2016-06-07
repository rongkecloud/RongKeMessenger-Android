package com.rongkecloud.test.ui.setting.mms;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.rongkecloud.chat.RKCloudChatConfigManager;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.test.R;

public class SettingMmsNotifyActivity extends RKCloudChatBaseActivity implements OnClickListener {
	private ImageView mBar, mContent, mSound, mVibration;
	private LinearLayout mNotifyContentLayout;//消息详情提示布局
	private LinearLayout mNotifySoundLayout;//声音提示布局
	private LinearLayout mNotifyVibrationLayout;//震动提示布局
	private LinearLayout mSelectSoundLayout;
	private RelativeLayout mLayoutDiver;
	private TextView mSoundName;
	
	private boolean mIsBarNotify = true;// 通知栏是否提醒
	private boolean mIsContent = true;//通知消息详情是否打开
	private boolean mIsSound = true;//声音提示是否打开
	private boolean mIsVibration = true;//震动提示是否打开
	private RKCloudChatConfigManager mConfigManager;
	private String music;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting_mms_notify);
		initUIAndListeners();
		initData();
	}

	private void initUIAndListeners() {
		// 设置title
		TextView titleTV = (TextView) findViewById(R.id.txt_title);
		titleTV.setText(R.string.bnt_return);
		titleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		titleTV.setOnClickListener(mExitListener);

        TextView text_title_content = (TextView) findViewById(R.id.text_title_content);
        text_title_content.setText(R.string.chatdemo_setting_mms_notify);

		mBar = (ImageView) findViewById(R.id.notify_bar);
		mContent = (ImageView) findViewById(R.id.notify_content);
		mSound = (ImageView) findViewById(R.id.notify_sound);
		mVibration = (ImageView) findViewById(R.id.notify_vibration);
		mSelectSoundLayout = (LinearLayout) findViewById(R.id.layout_selectsound);
		mSoundName = (TextView) findViewById(R.id.soundname);
		mNotifyContentLayout = (LinearLayout) findViewById(R.id.notify_content_layout);
		mNotifySoundLayout = (LinearLayout) findViewById(R.id.notify_sound_layout);
		mNotifyVibrationLayout = (LinearLayout) findViewById(R.id.notify_vibration_layout);
		mLayoutDiver = (RelativeLayout) findViewById(R.id.layout_diver);
		
		mBar.setOnClickListener(this);
		mContent.setOnClickListener(this);
		mSound.setOnClickListener(this);
		mVibration.setOnClickListener(this);
		mSelectSoundLayout.setOnClickListener(this);
	}
	
	private void controlViewShow(){
		if(mIsBarNotify){//通知栏提示显示
			mNotifyContentLayout.setVisibility(View.VISIBLE);
			mNotifySoundLayout.setVisibility(View.VISIBLE);
			mNotifyVibrationLayout.setVisibility(View.VISIBLE);
			if(mIsSound){//声音提示选择打开
				mSelectSoundLayout.setVisibility(View.VISIBLE);
				mLayoutDiver.setVisibility(View.VISIBLE);
			}else{
				mSelectSoundLayout.setVisibility(View.GONE);
				mLayoutDiver.setVisibility(View.GONE);
			}
		}else{//通知栏提示不显示
			mNotifyContentLayout.setVisibility(View.GONE);
			mNotifySoundLayout.setVisibility(View.GONE);
			mNotifyVibrationLayout.setVisibility(View.GONE);
			mSelectSoundLayout.setVisibility(View.GONE);
		}
	}
	
	private void initData(){
		mConfigManager = RKCloudChatConfigManager.getInstance(this);
		mIsBarNotify = null!=mConfigManager ? mConfigManager.getBoolean(RKCloudChatConfigManager.NEWMSG_SHOW_IN_NOTIFICATIONBAR) : true;
		if(mIsBarNotify){
			mBar.setSelected(true);
			mContent.setSelected(null!=mConfigManager ? mConfigManager.getBoolean(RKCloudChatConfigManager.NEWMSG_NOTICE_MSGCONTENT) : true);
			mSound.setSelected(null!=mConfigManager ? mConfigManager.getBoolean(RKCloudChatConfigManager.NEWMSG_NOTICE_SOUND) : true);
			mVibration.setSelected(null!=mConfigManager ? mConfigManager.getBoolean(RKCloudChatConfigManager.NEWMSG_NOTICE_VIBRATION) : true);
		}else{
			mBar.setSelected(false);
			mContent.setSelected(false);
			mSound.setSelected(false);
			mVibration.setSelected(false);
		}
		music = null!=mConfigManager ? mConfigManager.getString(RKCloudChatConfigManager.NEWMSG_NOTICE_SOUND_URI) : "";
		if(!TextUtils.isEmpty(music)){
			mSoundName.setText(getString(R.string.chatdemo_setting_mms_notify_sound_music_custom));
		}else{
			mSoundName.setText(R.string.chatdemo_setting_mms_notify_sound_music_default);
		}
		
		mIsSound = null!=mConfigManager ? mConfigManager.getBoolean(RKCloudChatConfigManager.NEWMSG_NOTICE_SOUND) : true;
		controlViewShow();
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if(R.id.notify_bar == id){
			mIsBarNotify = !mIsBarNotify;
			if(mIsBarNotify){
				mBar.setSelected(true);
				mContent.setSelected(null!=mConfigManager ? mConfigManager.getBoolean(RKCloudChatConfigManager.NEWMSG_NOTICE_MSGCONTENT) : true);
				mSound.setSelected(null!=mConfigManager ? mConfigManager.getBoolean(RKCloudChatConfigManager.NEWMSG_NOTICE_SOUND) : true);
				mVibration.setSelected(null!=mConfigManager ? mConfigManager.getBoolean(RKCloudChatConfigManager.NEWMSG_NOTICE_VIBRATION) : true);
			}else{
				mBar.setSelected(false);
				mContent.setSelected(false);
				mSound.setSelected(false);
				mVibration.setSelected(false);
			}
			if(null!=mConfigManager){
				mConfigManager.setBoolean(RKCloudChatConfigManager.NEWMSG_SHOW_IN_NOTIFICATIONBAR, mIsBarNotify);
			}
			
		}else if(R.id.notify_content == id){
			mIsContent = !mIsContent;

			if(mIsBarNotify){
				boolean isSelected = mContent.isSelected();
				mContent.setSelected(!isSelected);
				mIsContent = !isSelected;
				if(null!=mConfigManager){
					mConfigManager.setBoolean(RKCloudChatConfigManager.NEWMSG_NOTICE_MSGCONTENT, mIsContent);
				}
			}
		}else if(R.id.notify_sound == id){
			if(mIsBarNotify){
				boolean isSelected = mSound.isSelected();
				mSound.setSelected(!isSelected);
				mIsSound = !isSelected;
				if(null!=mConfigManager){
					mConfigManager.setBoolean(RKCloudChatConfigManager.NEWMSG_NOTICE_SOUND, mIsSound);
				}
			}
			
		}else if(R.id.notify_vibration == id){
			if(mIsBarNotify){
				boolean isSelected = mVibration.isSelected();
				mVibration.setSelected(!isSelected);
				mIsVibration = !isSelected;
				if(null!=mConfigManager){
					mConfigManager.setBoolean(RKCloudChatConfigManager.NEWMSG_NOTICE_VIBRATION, mIsVibration);
				}
			}
			
		}else if(R.id.layout_selectsound == id){
			if(mIsBarNotify && (null!=mConfigManager && mConfigManager.getBoolean(RKCloudChatConfigManager.NEWMSG_NOTICE_SOUND))){
				showWinToSelectMusic();
			}
		}
		
		controlViewShow();
	}
	
	private void showWinToSelectMusic(){
		/**
			 * 弹出 选择铃声 对话框
			 */
		int index;
		if(TextUtils.isEmpty(music)){
			index = 0;
		}else{
			index = 1;
		}
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setSingleChoiceItems(R.array.setting_sound, index,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						int sex = -1;
						if(which == 0){
							music = "";
							mSoundName.setText("系统铃声");
						}else{
							music = "rkcloud_chat_sound_custom";
							mSoundName.setText("自定义铃声");
						}

						if(null != mConfigManager){
							mConfigManager.setString(RKCloudChatConfigManager.NEWMSG_NOTICE_SOUND_URI, music);
						}
						dialog.dismiss();
					}
				});
		alertDialog.create().show();
	}
	
	@Override
	public void processResult(Message arg0) {		
	}

}
