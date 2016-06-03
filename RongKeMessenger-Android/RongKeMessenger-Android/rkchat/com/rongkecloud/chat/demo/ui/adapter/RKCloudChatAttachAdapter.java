package com.rongkecloud.chat.demo.ui.adapter;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.rongkecloud.chat.demo.entity.RKCloudChatMsgAttachItem;
import com.rongkecloud.test.R;

/**
 * 实现更多功能区域展示内容的适配器
 */
public class RKCloudChatAttachAdapter extends BaseAdapter{
	private Context mContext;
	private List<RKCloudChatMsgAttachItem> mDatas;
	private ItemViewBuffer mItemBuffer;
	
	public RKCloudChatAttachAdapter(Context context, List<RKCloudChatMsgAttachItem> datas){
		mContext = context;
		mDatas = datas;
	}

	@Override
	public int getCount() {		
		return mDatas.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(null == convertView){
			convertView = ((Activity) mContext).getLayoutInflater().inflate(R.layout.rkcloud_chat_selectattach_item, null);
			mItemBuffer = new ItemViewBuffer(convertView);
			convertView.setTag(mItemBuffer);
			
		}else{
			mItemBuffer = (ItemViewBuffer)convertView.getTag();
			
		}
		
		RKCloudChatMsgAttachItem obj = mDatas.get(position); 
		mItemBuffer.mImageView.setImageResource(obj.drawableResId);
		mItemBuffer.mContent.setText(obj.content);
		
		return convertView;
	}
	
	private class ItemViewBuffer{
		ImageView mImageView;
		TextView mContent;
		
		public ItemViewBuffer(View view){
			mImageView = (ImageView)view.findViewById(R.id.img);
			mContent = (TextView)view.findViewById(R.id.content);
		}
	}
}