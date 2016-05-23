package com.rongke.cloud.meeting.demo.ui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.rongke.cloud.meeting.demo.RKCloudMeetingContactManager;
import com.rongke.cloud.meeting.demo.RKCloudMeetingDemoManager;
import com.rongke.cloud.meeting.demo.entity.RKCloudMeetingUserInfo;
import com.rongkecloud.multiVoice.RKCloudMeetingInvitedInfoBean;
import com.rongkecloud.test.R;

public class RKCloudMeetingDemoInvitedActivity extends Activity{
	// 收到邀请进入多人语音会议室的广播
	public static final String INVITED_BROADCAST = "action_rkcloud_mutlimeeting_invite";
	public static final String INVITED_BROADCAST_DATAS = "invited_datas";
	
	private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		String action = getIntent().getAction();
		if(INVITED_BROADCAST.equals(action)){
			ArrayList<RKCloudMeetingInvitedInfoBean> datas = getIntent().getParcelableArrayListExtra(INVITED_BROADCAST_DATAS);
			if(null!=datas && datas.size()>0){
				showMeetingInviteMsg(datas);
			}else{
				finish();
			}
		}
	}
	
	//处理收到的多人语音邀请消息
	private void showMeetingInviteMsg(final ArrayList<RKCloudMeetingInvitedInfoBean> datas){
		InviteAdapter adapter = new InviteAdapter(datas);
		new AlertDialog.Builder(this)
		.setTitle(R.string.rkcloud_meeting_invitetitle)
		.setCancelable(false)
		.setAdapter(adapter, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				RKCloudMeetingDemoManager.getInstance(RKCloudMeetingDemoInvitedActivity.this).joinMeeting(RKCloudMeetingDemoInvitedActivity.this, datas.get(which));
				dialog.dismiss();
				finish();
			}
		})
		.setNeutralButton(R.string.rkcloud_meeting_invite_cancle, new DialogInterface.OnClickListener() {		
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				finish();
			}
		})
		.create().show();
	}
	
	private class InviteAdapter extends BaseAdapter{
		private ArrayList<RKCloudMeetingInvitedInfoBean> mDatas;
		private RKCloudMeetingContactManager mContactManager;
		
		InviteAdapter(ArrayList<RKCloudMeetingInvitedInfoBean> datas){
			mDatas = datas;
			mContactManager = RKCloudMeetingContactManager.getInstance(RKCloudMeetingDemoInvitedActivity.this);
		}
		@Override
		public int getCount() {
			return mDatas.size();
		}

		@Override
		public Object getItem(int position) {
			return mDatas.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(null == convertView){
				convertView = getLayoutInflater().inflate(R.layout.rkcloud_meeting_ivite_item, null);
			}
			
			TextView nameTV = (TextView)convertView.findViewById(R.id.name);
			TextView timeTV = (TextView)convertView.findViewById(R.id.time);
			Button addBtn = (Button)convertView.findViewById(R.id.addbtn);
			
			final RKCloudMeetingInvitedInfoBean info = mDatas.get(position);
			RKCloudMeetingUserInfo contactObj = mContactManager.getContactInfo(info.getInvitorAccount());
			nameTV.setText(null!=contactObj ? contactObj.getShowName() : info.getInvitorAccount());
			
			if(info.getTime() > 0){
				timeTV.setVisibility(View.VISIBLE);
				timeTV.setText(mDateFormat.format(info.getTime()*1000));
			}else{
				timeTV.setVisibility(View.GONE);
			}
			addBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					RKCloudMeetingDemoManager.getInstance(RKCloudMeetingDemoInvitedActivity.this).joinMeeting(RKCloudMeetingDemoInvitedActivity.this, info);
					finish();
				}
			});
			
			return convertView;
		}		
	}
}
