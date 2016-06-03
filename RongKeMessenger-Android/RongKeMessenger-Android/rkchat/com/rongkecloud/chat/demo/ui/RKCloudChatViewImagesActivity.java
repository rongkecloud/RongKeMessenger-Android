package com.rongkecloud.chat.demo.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.rongkecloud.chat.ImageMessage;
import com.rongkecloud.chat.RKCloudChatBaseMessage;
import com.rongkecloud.chat.demo.RKCloudChatConstants;
import com.rongkecloud.chat.demo.RKCloudChatMmsManager;
import com.rongkecloud.chat.demo.RKCloudChatUiHandlerMessage;
import com.rongkecloud.chat.demo.tools.RKCloudChatSDCardTools;
import com.rongkecloud.chat.demo.tools.RKCloudChatTools;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.chat.demo.ui.widget.RKCloudChatTouchImageView;
import com.rongkecloud.chat.demo.ui.widget.RKCloudChatTouchImageViewPager;
import com.rongkecloud.test.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RKCloudChatViewImagesActivity extends RKCloudChatBaseActivity{
	private static final String TAG = RKCloudChatViewImagesActivity.class.getSimpleName();
	
	public static final String INTENT_KEY_MSGOBJ = "key_msgobj";// 当前查看的消息对象
	
	// UI组件
	private TextView mTitleTV;
	private LinearLayout mLayoutOpe;// 操作功能用的布局
	private TextView mReturnTV, mSaveTV, mForwardTV, mShareTV;
	
	private RKCloudChatTouchImageViewPager mViewPager;
	
	// 成员变量
	private String mChatId;
	private ImageMessage mDefaultMsgObj;
	
	private List<RKCloudChatBaseMessage> mDatas;
	private RKCloudChatMmsManager mMmsManager;
	private GuidePageAdapter mGuidPageAdapter;
	
	private Map<String, View> mPageViews;
	private Map<String, RKCloudChatTouchImageView> mTouchViews;
	private List<String> mRecordDowningMsg;
	private List<String> mRecordDowningThumbMsg;
	private String mCurrSelectedMsgSerialNum;// 当前显示的消息编号	
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rkcloud_chat_view_images);
		
		mDefaultMsgObj = getIntent().getParcelableExtra(INTENT_KEY_MSGOBJ);
		if(null==mDefaultMsgObj){
			finish();
			return;
		}
		mChatId = mDefaultMsgObj.getChatId();	
		initUIAndListeners();
		initData();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mMmsManager.bindUiHandler(mUiHandler);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}

	/*
	 * 初始化组件
	 */
	private void initUIAndListeners() {
		mTitleTV = (TextView)findViewById(R.id.txt_title);
		
		mViewPager = (RKCloudChatTouchImageViewPager) findViewById(R.id.viewPager);
		
		mLayoutOpe = (LinearLayout) findViewById(R.id.layout_ope);
		mReturnTV = (TextView) findViewById(R.id.nav_return);
		mSaveTV = (TextView) findViewById(R.id.nav_save);
		mForwardTV = (TextView) findViewById(R.id.nav_forward);
		mShareTV = (TextView) findViewById(R.id.nav_share);
		
		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageSelected(int arg0) {
				mTitleTV.setText((arg0+1) +"/" +mDatas.size());
				mViewPager.setCurrentItem(arg0);
				ImageMessage msgObj = (ImageMessage)mDatas.get(arg0);
				mCurrSelectedMsgSerialNum = msgObj.getMsgSerialNum();
				
				mLayoutOpe.setVisibility(View.GONE);
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});
		
		mViewPager.setOnRKCloudChatTouchImageViewListener(new RKCloudChatTouchImageViewPager.RKCloudChatTouchImageViewListener() {
			@Override
			public View onGetCurrTouchImageView(int pos) {
				final ImageMessage msgObj = (ImageMessage)mDatas.get(pos);
				View view = null!=msgObj ? mTouchViews.get(msgObj.getMsgSerialNum()) : null;
				if(null != view){
					view.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							if(!TextUtils.isEmpty(msgObj.getFilePath()) && new File(msgObj.getFilePath()).exists()){
								if(mLayoutOpe.getVisibility() == View.GONE){
									mLayoutOpe.setVisibility(View.VISIBLE);
								}else{
									mLayoutOpe.setVisibility(View.GONE);
								}	
							}else{
								mLayoutOpe.setVisibility(View.GONE);
							}
						}
					});
					view.setOnLongClickListener(new OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							mLayoutOpe.setVisibility(View.GONE);
							if(!TextUtils.isEmpty(msgObj.getFilePath()) && new File(msgObj.getFilePath()).exists()){
								showOpeDialog(msgObj);
							}
							return false;
						}
					});
				}
				return view;
			}
		});
		
		mReturnTV.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});		
		
		mSaveTV.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(TextUtils.isEmpty(mCurrSelectedMsgSerialNum)){
					return;
				}
				for(RKCloudChatBaseMessage obj : mDatas){
					if(obj.getMsgSerialNum().equals(mCurrSelectedMsgSerialNum)){
						saveMsg((ImageMessage)obj);
						break;
					}
				}
			}
		});		
		mForwardTV.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(TextUtils.isEmpty(mCurrSelectedMsgSerialNum)){
					return;
				}
				for(RKCloudChatBaseMessage obj : mDatas){
					if(obj.getMsgSerialNum().equals(mCurrSelectedMsgSerialNum)){
						forwardMsg((ImageMessage)obj);
						break;
					}
				}
			}
		});		
		mShareTV.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(TextUtils.isEmpty(mCurrSelectedMsgSerialNum)){
					return;
				}
				for(RKCloudChatBaseMessage obj : mDatas){
					if(obj.getMsgSerialNum().equals(mCurrSelectedMsgSerialNum)){
						shareMsg((ImageMessage)obj);
						break;
					}
				}
			}
		});		
	}
	
	private void initData(){
		mDatas = new ArrayList<RKCloudChatBaseMessage>();
		mPageViews = new HashMap<String, View>();
		mTouchViews = new HashMap<String, RKCloudChatTouchImageView>();
		mRecordDowningMsg = new ArrayList<String>();
		mRecordDowningThumbMsg = new ArrayList<String>();
		
		mMmsManager = RKCloudChatMmsManager.getInstance(this);
		mMmsManager.bindUiHandler(mUiHandler);
		
		// 定位索引号位置
		int index = -1;
		// 获取会话中的数据
		List<RKCloudChatBaseMessage> datas = mMmsManager.queryMmsByType(mChatId, RKCloudChatBaseMessage.MSG_TYPE_IMAGE);
		mDatas.clear();
		if(null!=datas && datas.size()>0){
			mDatas.addAll(datas);
			for(int i=0; i<datas.size(); i++){
				View view = getLayoutInflater().inflate(R.layout.rkcloud_chat_view_images_item, null);
				ImageMessage msgObj = (ImageMessage)mDatas.get(i);
				mPageViews.put(msgObj.getMsgSerialNum(), view);
				
				if(msgObj.getMsgSerialNum().equals(mDefaultMsgObj.getMsgSerialNum())){
					index = i;
				}
			}
		}
		
		if(-1 == index){
			finish();
			return;
		}
		mTitleTV.setText((index+1) +"/" +mDatas.size());
		mGuidPageAdapter = new GuidePageAdapter();
		mViewPager.setAdapter(mGuidPageAdapter);		
		mViewPager.setCurrentItem(index);
		mCurrSelectedMsgSerialNum = mDefaultMsgObj.getMsgSerialNum(); 
	}
	
	private void saveMsg(ImageMessage msgObj){
		if(null!=msgObj && !TextUtils.isEmpty(msgObj.getFilePath())){
			File srcFile = new File(msgObj.getFilePath());
			// 原文件不存在时返回
			if(!srcFile.exists()){
				return;
			}
			// 组装保存后的文件
			File savedFile = new File(RKCloudChatConstants.ROOT_PATH + getPackageName()+"/images/"+RKCloudChatTools.getFileName(msgObj.getFilePath())+".jpg");
			// 父目录不存在时创建
			if(!savedFile.getParentFile().exists()){
				if(!savedFile.getParentFile().mkdirs()){
					return;
				}
			}
			// 文件不存在时创建
			if(!savedFile.exists()){
				if(!RKCloudChatTools.copyFile(srcFile, savedFile)){
					return;
				}
			}
			
			RKCloudChatTools.showToastText(RKCloudChatViewImagesActivity.this, getString(R.string.rkcloud_chat_viewimg_showlocation, savedFile.getAbsolutePath()));
		}
	}
		
	private void forwardMsg(ImageMessage msgObj){
		if(null!=msgObj && !TextUtils.isEmpty(msgObj.getFilePath()) && new File(msgObj.getFilePath()).exists()){
			Intent forwardIntent = new Intent(RKCloudChatViewImagesActivity.this, RKCloudChatForwardActivity.class);
			forwardIntent.putExtra(RKCloudChatForwardActivity.INTENT_KEY_FUNC_TYPE, RKCloudChatForwardActivity.FUNC_FORWARD);
			forwardIntent.putExtra(RKCloudChatForwardActivity.INTENT_KEY_MSGSERIALNUM, msgObj.getMsgSerialNum());
			startActivity(forwardIntent);
		}
	}
	
	private void shareMsg(ImageMessage msgObj){
		if(null!=msgObj && !TextUtils.isEmpty(msgObj.getFilePath()) && new File(msgObj.getFilePath()).exists()){
			Intent shareIntent = new Intent();
			shareIntent.setAction(Intent.ACTION_SEND);
			shareIntent.putExtra(RKCloudChatForwardActivity.INTENT_KEY_FUNC_TYPE, RKCloudChatForwardActivity.FUNC_INSIDE_SHARE);
			shareIntent.putExtra(RKCloudChatForwardActivity.INTENT_KEY_MSGSERIALNUM, msgObj.getMsgSerialNum());
			shareIntent.setType("image/*");
			shareIntent.putExtra(Intent.EXTRA_TITLE, msgObj.getFileName());
			shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(msgObj.getFilePath())));
			startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.rkcloud_chat_msglist_share_title)));
		}
	}

	private void showOpeDialog(final ImageMessage msgObj){
		String[] arrContents = new String[]{
				getString(R.string.rkcloud_chat_btn_save), 
				getString(R.string.rkcloud_chat_btn_forward), 
				getString(R.string.rkcloud_chat_btn_share)};
		new AlertDialog.Builder(this).setItems(arrContents, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch(which){
				case 0:// 保存
					saveMsg(msgObj);
					break;
					
				case 1:// 转发
					forwardMsg(msgObj);
					break;
					
				case 2:// 分享
					shareMsg(msgObj);
					break;
				}
				
			}
		}).create().show();
	}

	@Override
	public void processResult(Message msg) {
		switch (msg.what) {
		case RKCloudChatUiHandlerMessage.RESPONSE_THUMBIMAGE_DOWNED:
		case RKCloudChatUiHandlerMessage.RESPONSE_MEDIAFILE_DOWNED:
			String msgSerialNum = (String)msg.obj;
			ImageMessage msgObj = null;
			for(RKCloudChatBaseMessage obj : mDatas){
				if(obj.getMsgSerialNum().equals(msgSerialNum)){
					msgObj = (ImageMessage)obj;
					break;
				}
			}
			if(null != msgObj){
				msgObj.copyData(mMmsManager.queryChatMsg(msgSerialNum));				
				if(RKCloudChatUiHandlerMessage.RESPONSE_THUMBIMAGE_DOWNED == msg.what){
					mRecordDowningThumbMsg.remove(msgSerialNum);
				}else{
					mRecordDowningMsg.remove(msgSerialNum);
				}
				
				mGuidPageAdapter.notifyDataSetChanged();
			}
			break;
			
		case RKCloudChatUiHandlerMessage.DELETE_SINGLE_CHAT:
			if (mChatId.equalsIgnoreCase((String) msg.obj)) {
				finish();
			}
			break;
		}
	}
	
	private class GuidePageAdapter extends PagerAdapter{
		@Override
		public int getCount() {
			return mDatas.size();
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}
		
		@Override
		public int getItemPosition(Object object) {
			return POSITION_NONE;
		}
		
		@Override
		public void destroyItem(View container, int position, Object object) {
			ImageMessage msgObj = (ImageMessage)mDatas.get(position);
			String msgSerialNum = (null != msgObj) ? msgObj.getMsgSerialNum() : null;
			if(null != msgSerialNum){
				mRecordDowningMsg.remove(msgSerialNum);
				mPageViews.remove(msgSerialNum);
			}
			
			((ViewPager) container).removeView((View)object);
		}
		
		@Override
		public View instantiateItem(View container, int position) {
			final ImageMessage msgObj = (ImageMessage)mDatas.get(position);
			View view = mPageViews.get(msgObj.getMsgSerialNum());
			if(null == view){
				view = getLayoutInflater().inflate(R.layout.rkcloud_chat_view_images_item, null);
			}
			RKCloudChatTouchImageView imageView = (RKCloudChatTouchImageView) view.findViewById(R.id.image);
			ImageView smallImageView = (ImageView) view.findViewById(R.id.smallimage);	
			ProgressBar pb = (ProgressBar)view.findViewById(R.id.progressbar);
			
			Drawable drawable = null;
			Drawable thumbDrawable = null;
			
			try{
				if(!TextUtils.isEmpty(msgObj.getFilePath())){
					if(new File(msgObj.getFilePath()).exists()){
						drawable = Drawable.createFromPath(msgObj.getFilePath());
					}else if(!TextUtils.isEmpty(msgObj.getThumbPath()) && new File(msgObj.getThumbPath()).exists()){
						thumbDrawable = Drawable.createFromPath(msgObj.getThumbPath());
					}
					
				}else{
					// 先显示缩略图
					if(!TextUtils.isEmpty(msgObj.getThumbPath()) && new File(msgObj.getThumbPath()).exists()){
						thumbDrawable = Drawable.createFromPath(msgObj.getThumbPath());
					}
				}
			}catch(Exception e){
			}
			
			// 是否下载缩略图
			if((TextUtils.isEmpty(msgObj.getThumbPath()) || !new File(msgObj.getThumbPath()).exists()) 
					&& !mRecordDowningThumbMsg.contains(msgObj.getMsgSerialNum())
					&& RKCloudChatSDCardTools.diskSpaceAvailable()){
				mRecordDowningThumbMsg.add(msgObj.getMsgSerialNum());// 记录下载进度条的组件
				mMmsManager.downThumbImage(msgObj.getMsgSerialNum());
			}
			// 是否下载原图
			if((TextUtils.isEmpty(msgObj.getFilePath()) || !new File(msgObj.getFilePath()).exists()) 
					&& !mRecordDowningMsg.contains(msgObj.getMsgSerialNum())
					&& RKCloudChatSDCardTools.diskSpaceAvailable()){
				mRecordDowningMsg.add(msgObj.getMsgSerialNum());// 记录下载进度条的组件
				mMmsManager.downAttach(msgObj.getMsgSerialNum());
			}
			
			pb.setVisibility(View.GONE);
			if(null != drawable){
				imageView.setVisibility(View.VISIBLE);
				smallImageView.setVisibility(View.GONE);
				imageView.setImageDrawable(drawable);
				mTouchViews.put(msgObj.getMsgSerialNum(), imageView);
			}else{
				mTouchViews.remove(msgObj.getMsgSerialNum());
				pb.setVisibility(View.VISIBLE);
				imageView.setVisibility(View.GONE);
				smallImageView.setVisibility(View.VISIBLE);
				if(null != thumbDrawable){
					smallImageView.setImageDrawable(thumbDrawable);
				}else{
					smallImageView.setImageResource(R.drawable.rkcloud_chat_img_picmsg_default);
				}
			}
			
			mPageViews.put(msgObj.getMsgSerialNum(), view);
			((ViewPager) container).addView(view);
			return view;
		}
	}
}
