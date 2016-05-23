package com.rongkecloud.test.ui.setting.mms;

import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
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
	private static final int INTENT_RESULT_SELECT_RINGTONE = 0;
	
	
	private ImageView mBar, mSound, mVibration;
	private LinearLayout mNotifySoundLayout;//声音提示布局
	private LinearLayout mNotifyVibrationLayout;//震动提示布局
	private LinearLayout mSelectSoundLayout;
	private RelativeLayout mLayoutDiver;
	private TextView mSoundName;
	
	
	private Uri mSelectedMusicUri = null;
	private boolean mIsBarNotify = true;// 通知栏是否提醒
	private boolean mIsSound = true;//声音提示是否打开
	private boolean mIsVibration = true;//震动提示是否打开
	private RKCloudChatConfigManager mConfigManager;

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
		mSound = (ImageView) findViewById(R.id.notify_sound);
		mVibration = (ImageView) findViewById(R.id.notify_vibration);
		mSelectSoundLayout = (LinearLayout) findViewById(R.id.layout_selectsound);
		mSoundName = (TextView) findViewById(R.id.soundname);
		mNotifySoundLayout = (LinearLayout) findViewById(R.id.notify_sound_layout);
		mNotifyVibrationLayout = (LinearLayout) findViewById(R.id.notify_vibration_layout);
		mLayoutDiver = (RelativeLayout) findViewById(R.id.layout_diver);
		
		mBar.setOnClickListener(this);
		mSound.setOnClickListener(this);
		mVibration.setOnClickListener(this);
		mSelectSoundLayout.setOnClickListener(this);
	}
	
	private void controlViewShow(){
		if(mIsBarNotify){//通知栏提示显示
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
			mNotifySoundLayout.setVisibility(View.GONE);
			mNotifyVibrationLayout.setVisibility(View.GONE);
			mSelectSoundLayout.setVisibility(View.GONE);
		}
	}
	
	private void initData(){
		mConfigManager = RKCloudChatConfigManager.getInstance(this);
		mIsBarNotify = null!=mConfigManager ? mConfigManager.getNotificationEnable() : true;
		if(mIsBarNotify){
			mBar.setSelected(true);
			mSound.setSelected(null!=mConfigManager ? mConfigManager.getNoticeBySound() : true);
			mVibration.setSelected(null!=mConfigManager ? mConfigManager.getNoticedByVibrate() : true);
		}else{
			mBar.setSelected(false);
			mSound.setSelected(false);
			mVibration.setSelected(false);
		}
		String music = null!=mConfigManager ? mConfigManager.getNotifyRingUri() : "";
		if(!TextUtils.isEmpty(music)){
			mSelectedMusicUri = Uri.parse(music);
			String musicName = RingtoneManager.getRingtone(this, mSelectedMusicUri).getTitle(this);
			mSoundName.setText(musicName);
		}else{
			mSoundName.setText(R.string.chatdemo_setting_mms_notify_sound_music_default);
		}
		
		mIsSound = null!=mConfigManager ? mConfigManager.getNoticeBySound() : true;
		controlViewShow();
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if(R.id.notify_bar == id){
			mIsBarNotify = !mIsBarNotify;
			if(mIsBarNotify){
				mBar.setSelected(true);
				mSound.setSelected(null!=mConfigManager ? mConfigManager.getNoticeBySound() : true);
				mVibration.setSelected(null!=mConfigManager ? mConfigManager.getNoticedByVibrate() : true);
			}else{
				mBar.setSelected(false);
				mSound.setSelected(false);
				mVibration.setSelected(false);
			}
			if(null!=mConfigManager){
				mConfigManager.setNotificationEnable(mIsBarNotify);
			}
			
		}else if(R.id.notify_sound == id){
			if(mIsBarNotify){
				boolean isSelected = mSound.isSelected();
				mSound.setSelected(!isSelected);
				mIsSound = !isSelected;
				if(null!=mConfigManager){
					mConfigManager.setNoticeBySound(!isSelected);
				}
			}
			
		}else if(R.id.notify_vibration == id){
			if(mIsBarNotify){
				boolean isSelected = mVibration.isSelected();
				mVibration.setSelected(!isSelected);
				if(null!=mConfigManager){
					mConfigManager.setNoticedByVibrate(!isSelected);
				}
			}
			
		}else if(R.id.layout_selectsound == id){
			if(mIsBarNotify && (null!=mConfigManager && mConfigManager.getNoticeBySound())){
				showWinToSelectMusic();
			}
		}
		
		controlViewShow();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (null==data || resultCode!=RESULT_OK) {
			return;
		}
		
		if(INTENT_RESULT_SELECT_RINGTONE == requestCode){
			mSelectedMusicUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
			if(null!=mSelectedMusicUri && !mSelectedMusicUri.equals("")){
				String musicName = RingtoneManager.getRingtone(this, mSelectedMusicUri).getTitle(this);
				mSoundName.setText(musicName);
				if(null != mConfigManager){
					mConfigManager.setNotifyRingUri(mSelectedMusicUri.toString());
				}
			}
		}
	}
	
	private void showWinToSelectMusic(){
		Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, R.string.chatdemo_setting_mms_notify_sound_music_title);// 设置显示的标题
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);// 显示默认的铃声
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);// 不显示静音
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, mSelectedMusicUri);// 将现有的uri传给 系统铃声列表显示
		startActivityForResult(intent, INTENT_RESULT_SELECT_RINGTONE);
	}
	
	@Override
	public void processResult(Message arg0) {		
	}

}
