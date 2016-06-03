package com.rongkecloud.chat.demo.ui.adapter;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.rongkecloud.test.R;

/**
 * 实现表情的适配器
 */
public class RKCloudChatEmojiAdapter extends BaseAdapter{
	private Context mContext;
	private List<Integer> mDatas;
	private ItemViewBuffer mItemBuffer;
	
	public RKCloudChatEmojiAdapter(Context context, List<Integer> datas){
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
			convertView = ((Activity) mContext).getLayoutInflater().inflate(R.layout.rkcloud_chat_selectemoji_item, null);
			mItemBuffer = new ItemViewBuffer(convertView);
			convertView.setTag(mItemBuffer);
			
		}else{
			mItemBuffer = (ItemViewBuffer)convertView.getTag();
			
		}
		int id = mDatas.get(position); 
		mItemBuffer.mImageView.setImageResource(0!=id ? id : R.drawable.rkcloud_chat_emoji_delete);		
		return convertView;
	}
	
	private class ItemViewBuffer{
		ImageView mImageView;
		
		public ItemViewBuffer(View view){
			mImageView = (ImageView)view.findViewById(R.id.emoji);
		}
	}

}
