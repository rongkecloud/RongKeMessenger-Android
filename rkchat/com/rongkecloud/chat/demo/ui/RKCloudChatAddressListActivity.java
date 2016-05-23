package com.rongkecloud.chat.demo.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.rongkecloud.chat.demo.RKCloudChatContactManager;
import com.rongkecloud.chat.demo.RKCloudChatMmsManager;
import com.rongkecloud.chat.demo.RKCloudChatUiHandlerMessage;
import com.rongkecloud.chat.demo.entity.RKCloudChatContact;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageAsyncLoader;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageAsyncLoader.ImageLoadedCompleteDelayNotify;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest.IMAGE_REQUEST_TYPE;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageResult;
import com.rongkecloud.chat.demo.ui.widget.RKCloudChatSideBar;
import com.rongkecloud.test.R;
import com.rongkecloud.test.ui.widget.RoundedImageView;

public class RKCloudChatAddressListActivity extends RKCloudChatBaseActivity implements ImageLoadedCompleteDelayNotify{
	// 查询类型
	private static final int QUERY_TYPE_GET_ALL_CONTACTS = 0;// 获取所有数据
	private static final int QUERY_TYPE_SEARCH = 1;// 搜索
	
	private BackgroundColorSpan backgroundColorSpan;
	
	// UI组件
	private ListView mListView;
	private ProgressBar mLoadingPB;
	private TextView mEmptyTV;
	
	private LinearLayout mGroupZoneLayout, mGroupLayout;
	
	private EditText mSearchET;
	
	private RKCloudChatSideBar mSideBar;
	private TextView mShowAlpha;
	
	// 成员变量
	private RKCloudChatContactManager mContactManager;
	
	private List<RKCloudChatContact> mAllContacts;
	private List<RKCloudChatContact> mDatas;
	private RKCloudChatAddressListAdapter mAdapter;
	
	private QueryHandlerThread mQuerThread;// 查询数据的线程
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rkcloud_chat_addresslist);
		initUIAndListeners();
		initData();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mContactManager.bindUiHandler(mUiHandler);
		RKCloudChatImageAsyncLoader.getInstance(this).registerDelayListener(this);
		
		startQuery(QUERY_TYPE_GET_ALL_CONTACTS);
	}
	
	@Override
	protected void onDestroy() {
		if (mQuerThread != null) {
			mQuerThread.quit();
			mQuerThread = null;
		}
		super.onDestroy();
	}
	

	@Override
	public void onLoadImageCompleteDelayNotify(IMAGE_REQUEST_TYPE type, List<RKCloudChatImageResult> result) {
		if(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG == type){
			mAdapter.onLoadedHeaderImage();
		}
	}
	
	private void initUIAndListeners(){
		// 设置title
		TextView titleTV = (TextView)findViewById(R.id.txt_title);
		titleTV.setText(R.string.rkcloud_chat_address_title);
		
		// 初始化UI元素
		mListView = (ListView) findViewById(R.id.add_listview);
		mLoadingPB = (ProgressBar) findViewById(R.id.add_loading_progressbar);
		mEmptyTV = (TextView)findViewById(R.id.add_emptytv);
		
		mGroupZoneLayout = (LinearLayout)findViewById(R.id.layout_groupzone);
		mGroupLayout = (LinearLayout)findViewById(R.id.layout_group);
		
		mSearchET = (EditText)findViewById(R.id.searchedittext);
		
		mSideBar = (RKCloudChatSideBar) findViewById(R.id.sidebar);
		mShowAlpha = (TextView)findViewById(R.id.selectedalpha);
		mSideBar.setListView(mListView);
		mSideBar.setTextView(mShowAlpha);
		// 监听器
		mGroupLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(RKCloudChatAddressListActivity.this, RKCloudChatGroupListActivity.class));
			}
		});
		
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				RKCloudChatContact obj = mDatas.get(position);
				RKCloudChatMmsManager.getInstance(RKCloudChatAddressListActivity.this).enterMsgListActivity(obj.rkAccount);
			}
		});
		
		mSearchET.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				mGroupZoneLayout.setVisibility(TextUtils.isEmpty(mSearchET.getText().toString().trim()) ? View.VISIBLE : View.GONE);
				startQuery(QUERY_TYPE_SEARCH);
			}
		});
	}
	
	private void initData(){
		backgroundColorSpan = new BackgroundColorSpan(getResources().getColor(R.color.rkcloud_chat_search_result_highlightcolor));
		
		mContactManager = RKCloudChatContactManager.getInstance(this);
		
		mAllContacts = new ArrayList<RKCloudChatContact>();
		mDatas = new ArrayList<RKCloudChatContact>();
		mAdapter = new RKCloudChatAddressListAdapter();
		mListView.setAdapter(mAdapter);
		
		mLoadingPB.setVisibility(View.VISIBLE);
	}
	
	/*
	 * 查询数据
	 */
	private void startQuery(int queryType) {
		if (null == mQuerThread) {
			mQuerThread = new QueryHandlerThread("QueryAddressListActivityThread");
			mQuerThread.start();
		}
		mQuerThread.startQuery(queryType);
	}

	private class QueryHandlerThread extends HandlerThread implements Callback {
		private Handler mQuerHandler;

		public QueryHandlerThread(String name) {
			super(name);
		}

		public void startQuery(int queryType) {
			if (null == mQuerHandler) {
				mQuerHandler = new Handler(getLooper(), this);
			}
			if (!mQuerHandler.hasMessages(queryType)) {
				Message msg = mQuerHandler.obtainMessage();
				msg.what = queryType;
				if(QUERY_TYPE_SEARCH == queryType){
					List<RKCloudChatContact> datas = new ArrayList<RKCloudChatContact>();
					if(mAllContacts.size() > 0){
						datas.addAll(mAllContacts);
					}
					msg.obj = datas;
				}
				msg.sendToTarget();
			}
		}

		@Override
		public boolean handleMessage(Message msg) {
			if(QUERY_TYPE_GET_ALL_CONTACTS == msg.what){
				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.ADDRESSLIST_LOAD_ALL_CONTACTS_FINISHED;
				message.obj = mContactManager.getAllContacts();
				message.sendToTarget();
				
			}else if(QUERY_TYPE_SEARCH == msg.what){
				List<RKCloudChatContact> allDatas = (List<RKCloudChatContact>)msg.obj;
				String filter = mSearchET.getText().toString().trim();
				List<RKCloudChatContact> datas = new ArrayList<RKCloudChatContact>();
				if(!TextUtils.isEmpty(filter)){
					for(RKCloudChatContact obj : allDatas){
						obj.matchName(filter, backgroundColorSpan);
						if(null != obj.highLightName){
							datas.add(obj);
						}
					}
				}else{
					for(RKCloudChatContact obj : allDatas){
						obj.highLightName = null;
						datas.add(obj);
					}
				}
				Message message = mUiHandler.obtainMessage();
				message.what = RKCloudChatUiHandlerMessage.ADDRESSLIST_SEARCH_FINISHED;
				message.obj = datas;
				message.sendToTarget();
			}
			
			return true;
		}
	}

	@Override
	public void processResult(Message msg) {
		switch(msg.what){
		case RKCloudChatUiHandlerMessage.ADDRESSLIST_LOAD_ALL_CONTACTS_FINISHED:
			mAllContacts.clear();
			List<RKCloudChatContact> datas = (List<RKCloudChatContact>)msg.obj;
			if(null!=datas && datas.size()>0){
				mAllContacts.addAll(datas);
			}
			mLoadingPB.setVisibility(View.GONE);
			startQuery(QUERY_TYPE_SEARCH);
			break;
			
		case RKCloudChatUiHandlerMessage.ADDRESSLIST_SEARCH_FINISHED:
			mDatas.clear();
			List<RKCloudChatContact> data2 = (List<RKCloudChatContact>)msg.obj;
			if(null!=data2 && data2.size()>0){
				mDatas.addAll(data2);
				mEmptyTV.setVisibility(View.GONE);
			}else{
				mEmptyTV.setVisibility(View.VISIBLE);
			}
			mAdapter.notifyDataSetChanged();
			break;
			
		case RKCloudChatUiHandlerMessage.CONTACTSINFO_CHANGED:// 联系人信息有变化
		case RKCloudChatUiHandlerMessage.CONTACT_HEADERIMAGE_CHANGED:// 联系人头像有变化
			startQuery(QUERY_TYPE_GET_ALL_CONTACTS);
			break;
		}
	}
	
	private class RKCloudChatAddressListAdapter extends BaseAdapter implements SectionIndexer{
		private Map<String, Integer> mSectionMap;
		private ItemViewBuffer mItemBuffer;// 每个条目的缓存对象
		
		public RKCloudChatAddressListAdapter(){
			mSectionMap = new HashMap<String, Integer>();
		}
		
		@Override
		public void notifyDataSetChanged() {
			updateSectionMap();
			super.notifyDataSetChanged();
		}
		
		public void onLoadedHeaderImage(){
			super.notifyDataSetChanged();
		}
		
		@Override
		public Object[] getSections() {
			return null;
		}

		@Override
		public int getPositionForSection(int section) {
			String secStr = null;
			try {
				secStr = String.valueOf((char)section);
			} catch (Exception e) {
			}
			int ret = -1;
			if(!TextUtils.isEmpty(secStr) && mSectionMap.containsKey(secStr)){
				ret = mSectionMap.get(secStr);
			}
			
			if(ret>=0 && ret<getCount()){
				return ret;
			}
			return -1;
		}

		@Override
		public int getSectionForPosition(int position) {
			return 0;
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
			if (null == convertView) {
				convertView = getLayoutInflater().inflate(R.layout.rkcloud_chat_addresslist_item, null);
				mItemBuffer = new ItemViewBuffer(convertView);
				convertView.setTag(mItemBuffer);
			} else {
				mItemBuffer = (ItemViewBuffer) convertView.getTag();
			}
			
			RKCloudChatContact obj = mDatas.get(position);
			mItemBuffer.nameTV.setText(null!=obj.highLightName ? obj.highLightName : obj.getShowName());
			
			String currcatalogName = getFirstChar(obj);
			String previousCataName = null;
			if(0 != position){
				previousCataName = getFirstChar(mDatas.get(position-1));
			}
			// 分类名称显示的控制
			if(TextUtils.isEmpty(previousCataName) || !currcatalogName.equalsIgnoreCase(previousCataName)){
				mItemBuffer.categoryName.setVisibility(View.VISIBLE);
				mItemBuffer.categoryName.setText(currcatalogName);
			}else{
				mItemBuffer.categoryName.setVisibility(View.GONE);
			}
			// 分隔条的显示控制
			if(position < mDatas.size()-1){
				String afterCataName = getFirstChar(mDatas.get(position+1));
				if(currcatalogName.equalsIgnoreCase(afterCataName)){
					mItemBuffer.divideView.setVisibility(View.VISIBLE);
				}else{
					mItemBuffer.divideView.setVisibility(View.GONE);
				}
			}
				
			mItemBuffer.headerPhotoView.setImageResource(R.drawable.rkcloud_chat_img_header_default);
			// 加载头像
			if(!TextUtils.isEmpty(obj.getHeaderThumbImagePath())){
				RKCloudChatImageRequest imageReq = new RKCloudChatImageRequest(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG, obj.getHeaderThumbImagePath(), obj.rkAccount);
				// 如果在缓存中则直接设置图片
				RKCloudChatImageResult imgResult = RKCloudChatImageAsyncLoader.getInstance(RKCloudChatAddressListActivity.this).sendPendingRequestQuryCache(imageReq);
				if(null!=imgResult && null!=imgResult.resource){
					mItemBuffer.headerPhotoView.setImageDrawable(imgResult.resource);
				}	
			}
			
			return convertView;
		}
		
		private class ItemViewBuffer {
			TextView categoryName;
			TextView nameTV; // 名称
			RoundedImageView headerPhotoView; // 头像
			ImageView divideView;
			
			public ItemViewBuffer(View convertView) {
				categoryName = (TextView) convertView.findViewById(R.id.categoryname);
				nameTV = (TextView) convertView.findViewById(R.id.name);
				headerPhotoView = (RoundedImageView) convertView.findViewById(R.id.headerphoto);
				divideView = (ImageView) convertView.findViewById(R.id.divide);
			}
		}
		
		private void updateSectionMap() {
			mSectionMap.clear();
			if(null==mDatas || 0==mDatas.size()){
				return;
			}
			int size = mDatas.size();
			for(int pos=0; pos<size; pos++){
				RKCloudChatContact obj = mDatas.get(pos);		
				String sortKey = getFirstChar(obj);
				if( !mSectionMap.containsKey(sortKey) ){
					mSectionMap.put(sortKey, pos);
				}
			}
		}
		
		private String getFirstChar (RKCloudChatContact obj){
			String s = !TextUtils.isEmpty(obj.getSortKey()) ? obj.getSortKey() : obj.rkAccount;
			if(!TextUtils.isEmpty(s)){
				char first = s.charAt(0);
				if((first>='A' && first<='Z') || (first>='a' && first<='z')){
					return s.substring(0, 1).toUpperCase();
				} else {
					return "#";
				}
			}
			return "";
		}
	}
}
