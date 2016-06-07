package com.rongke.cloud.meeting.demo.ui.adapter;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.rongke.cloud.meeting.demo.entity.RKCloudMeetingUserInfo;
import com.rongkecloud.test.R;
import com.rongkecloud.test.ui.widget.RoundedImageView;

public class RKCloudMeetingUserAdapter extends BaseAdapter {

	private List<RKCloudMeetingUserInfo> mDatas;
	private Context mContext;
	
	public RKCloudMeetingUserAdapter(Context context , List<RKCloudMeetingUserInfo> datas){
		mContext = context;
		mDatas = datas;
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
		
		 ViewHolder mHolder;
		 
		if(null == convertView){
			convertView = LayoutInflater.from(mContext).inflate(R.layout.rkcloud_meeting_user_item, null);
			mHolder = new ViewHolder(convertView);
			convertView.setTag(mHolder);
		}else{
			mHolder = (ViewHolder) convertView.getTag();
		}
		RKCloudMeetingUserInfo rkUserInfo = mDatas.get(position);
		// 头像显示
		mHolder.mImageView.setImageResource(R.drawable.rkcloud_meeting_img_header_default);
		if(!TextUtils.isEmpty(rkUserInfo.avatarPath) && new File(rkUserInfo.avatarPath).exists()){
			try{
				Bitmap bitmap = BitmapFactory.decodeFile(rkUserInfo.avatarPath);
				if(null != bitmap){
					mHolder.mImageView.setImageBitmap(bitmap);
				}
			}catch(Exception e){
			}
		}
		
		//显示名称
		mHolder.tvUserName.setText(rkUserInfo.getShowName());
		// 是否静音的图标显示
		mHolder.muteImg.setImageResource(rkUserInfo.isMute() ? R.drawable.rkcloud_meeting_user_mute : R.drawable.rkcloud_meeting_user_unmute);
		return convertView;
	}

	class ViewHolder{
		public ImageView muteImg;
        public RoundedImageView mImageView;
		public TextView tvUserName;
		ViewHolder(View convertView){
			mImageView = (RoundedImageView) convertView.findViewById(R.id.multimeeting_user_item_avatar);
			muteImg = (ImageView) convertView.findViewById(R.id.muteimg);
			tvUserName = (TextView) convertView.findViewById(R.id.multimeeting_user_item_name);
		}
	}
}
