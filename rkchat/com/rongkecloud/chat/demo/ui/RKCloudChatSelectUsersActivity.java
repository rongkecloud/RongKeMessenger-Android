package com.rongkecloud.chat.demo.ui;

import java.util.*;

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
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.rongkecloud.chat.demo.RKCloudChatContactManager;
import com.rongkecloud.chat.demo.RKCloudChatUiHandlerMessage;
import com.rongkecloud.chat.demo.entity.RKCloudChatContact;
import com.rongkecloud.chat.demo.tools.RKCloudChatTools;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageAsyncLoader;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageAsyncLoader.ImageLoadedCompleteDelayNotify;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest.IMAGE_REQUEST_TYPE;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageResult;
import com.rongkecloud.chat.demo.ui.widget.RKCloudChatSideBar;
import com.rongkecloud.sdkbase.RKCloud;
import com.rongkecloud.test.R;
import com.rongkecloud.test.ui.widget.RoundedImageView;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public class RKCloudChatSelectUsersActivity extends RKCloudChatBaseActivity implements ImageLoadedCompleteDelayNotify{	
	public static final String INTENT_KEY_EXIST_ACCOUNTS = "exist_accounts";// 已经存在的账号，多个之间用半角逗号分隔，注意：已存在的账号禁止执行选中或取消选中操作
	public static final String INTENT_KEY_SELECTED_ACCOUNTS = "selected_accounts";// 默认选中的账号，多个之间用半角逗号分隔，注意：该账号可以执行选中或取消选中操作
	public static final String INTENT_KEY_SELECT_MODEL = "select_model";// 选择模式
	public static final String INTENT_RETURN_KEY_SELECTED_ACCOUNTS = "result_selected_accounts";// 已选择的账号，多个之间用半角逗号分隔
	// 选择模式
	public static final int SELECT_MODEL_SINGLE = 1;// 单选
	public static final int SELECT_MODEL_MUTLI = 2;// 多选
	
	public static final String SELECTED_DEFAULT_ACCOUNT = "+1";// 默认选中头像对应的账号
	// 查询类型
	private static final int QUERY_TYPE_GET_ALL_CONTACTS = 0;// 获取所有数据
	private static final int QUERY_TYPE_SEARCH = 1;// 搜索
		
	// UI组件
	private ListView mListView;
	private ProgressBar mLoadingPB;
	private TextView mNoDataTV;
	
	private EditText mSearchET;
	
	private RKCloudChatSideBar mSideBar;
	private TextView mShowAlpha;
	
	private Button mConfirmBtn; 
	private HorizontalScrollView mHScrollView;
	private LinearLayout mSelectedLinearLayout;
	private GridView mGridView;
	
	// 成员变量
	private RKCloudChatContactManager mContactManager;
	
	private List<String> mExistedAccounts;// 已存在的账号
	private int mCurrSelectModel;// 当前的选择模式，默认是多选
	
	private List<RKCloudChatContact> mAllContacts;
	private List<RKCloudChatContact> mDatas;
	private RKCloudChatSelectUsersAdapter mAdapter;
	
	private List<String> mSelectedAccounts;// 已选择的账号
	private Map<String, String> mHeaderImgDatas;
	private RKCloudChatHasSelectedUsersAdapter mSelectedAdapter;
	
	private QueryHandlerThread mQuerThread;// 查询数据的线程
	private boolean mFirstLoadData = false;// 是否加载数据
	
	private int mGridViewWidth;
	private int mGridViewHSpecing;
	private String mCurrAccount;
	private BackgroundColorSpan backgroundColorSpan;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rkcloud_chat_selectusers);
		initUIAndListeners();
		initData();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mContactManager.bindUiHandler(mUiHandler);
		RKCloudChatImageAsyncLoader.getInstance(this).registerDelayListener(this);
		
		if(mFirstLoadData){
			startQuery(QUERY_TYPE_GET_ALL_CONTACTS);
			mFirstLoadData = false;
		}
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
			mAdapter.notifyDataSetChanged();
			mSelectedAdapter.notifyDataSetChanged();
		}
	}
	
	private void initUIAndListeners(){
		// 设置title
		TextView titleTV = (TextView)findViewById(R.id.txt_title);
		titleTV.setText(R.string.bnt_return);
        titleTV.setTextColor(getResources().getColor(R.color.title_right_btn_text_color));
		titleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		titleTV.setOnClickListener(mExitListener);

        TextView text_title_content = (TextView)findViewById(R.id.text_title_content);
        text_title_content.setText(getString(R.string.rkcloud_chat_selectuser_title));
		
		// 初始化UI元素
		mListView = (ListView) findViewById(R.id.listview);
		mLoadingPB = (ProgressBar) findViewById(R.id.loading_progressbar);
		mNoDataTV = (TextView)findViewById(R.id.emptytv);
		
		mSearchET = (EditText)findViewById(R.id.searchedittext);
		
		mSideBar = (RKCloudChatSideBar) findViewById(R.id.sidebar);
		mShowAlpha = (TextView)findViewById(R.id.selectedalpha);
		mSideBar.setListView(mListView);
		mSideBar.setTextView(mShowAlpha);
		
		mConfirmBtn = (Button) findViewById(R.id.btn_confirm);	
		mConfirmBtn.setEnabled(false);
		mHScrollView = (HorizontalScrollView)findViewById(R.id.h_scrollview);
		mSelectedLinearLayout = (LinearLayout)findViewById(R.id.selected_linearlayout);
		mGridView = (GridView)findViewById(R.id.selected_gridview);
		
		// 监听器
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(null==mSelectedAccounts || 0==mSelectedAccounts.size()){
					return;
				}
				String account = mSelectedAccounts.get(position);
				if(!SELECTED_DEFAULT_ACCOUNT.equalsIgnoreCase(account)){
					for(String acc : mSelectedAccounts){
						if(acc.equalsIgnoreCase(account)){
							mSelectedAccounts.remove(account);
							break;
						}
					}
					mHeaderImgDatas.remove(account);
					mAdapter.notifyDataSetChanged();
					setGridViewParams(false);
				}
			}
		});

		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(null==mDatas || 0==mDatas.size()){
					return;
				}
				
				RKCloudChatContact obj = mDatas.get(position);
				clickListViewItem(view, obj);	
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
				startQuery(QUERY_TYPE_SEARCH);
			}
		});
		
		mConfirmBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent backIntent = new Intent();
				mSelectedAccounts.remove(SELECTED_DEFAULT_ACCOUNT);
				StringBuffer sb = new StringBuffer();
				for(String account : mSelectedAccounts){
					sb.append(account).append(",");
				}
				if(sb.length() > 0){
					sb.deleteCharAt(sb.length()-1);
				}
				
				backIntent.putExtra(INTENT_RETURN_KEY_SELECTED_ACCOUNTS, sb.toString());
				setResult(RESULT_OK, backIntent);
				finish();
			}
		});
	}
	
	public void clickListViewItem(View view, RKCloudChatContact obj){
		if(mExistedAccounts.contains(obj.rkAccount) || obj.rkAccount.equalsIgnoreCase(mCurrAccount)){
			return;
		}
		
		if(SELECT_MODEL_SINGLE == mCurrSelectModel){		
			if(!mSelectedAccounts.contains(obj.rkAccount)){
				if(mSelectedAccounts.size() >= 2){
					mSelectedAccounts.remove(0);
					mHeaderImgDatas.remove(obj.rkAccount);
				}
				mSelectedAccounts.add(mSelectedAccounts.size()-1, obj.rkAccount);
				if(!TextUtils.isEmpty(obj.getHeaderThumbImagePath())){
					mHeaderImgDatas.put(obj.rkAccount, obj.getHeaderThumbImagePath());
				}
				setGridViewParams(true);
				mAdapter.notifyDataSetChanged();
			}
			
		}else{
			if(mSelectedAccounts.contains(obj.rkAccount)){
				// 取消选择
				mSelectedAccounts.remove(obj.rkAccount);
				mHeaderImgDatas.remove(obj.rkAccount);
				mSelectedAdapter.notifyDataSetChanged();
				setGridViewParams(false);
				mAdapter.notifyDataSetChanged();
				
			}else{
				mSelectedAccounts.add(mSelectedAccounts.size()-1, obj.rkAccount);
				if(!TextUtils.isEmpty(obj.getHeaderThumbImagePath())){
					mHeaderImgDatas.put(obj.rkAccount, obj.getHeaderThumbImagePath());
				}
				mSelectedAdapter.notifyDataSetChanged();
				setGridViewParams(true);
				mAdapter.notifyDataSetChanged();
			}
		}
	}
	
	private void initData(){
		// 已存在账号的处理
		mExistedAccounts = new ArrayList<String>();
		List<String> existAccounts = RKCloudChatTools.splitStrings(getIntent().getStringExtra(INTENT_KEY_EXIST_ACCOUNTS));
		if(existAccounts.size() > 0){
			for(String account : existAccounts){
				mExistedAccounts.add(account);
			}
		}
		
		// 默认选中账号的处理
		mHeaderImgDatas = new HashMap<String, String>();
		mSelectedAccounts = new ArrayList<String>();
		List<String> selectedAccounts = RKCloudChatTools.splitStrings(getIntent().getStringExtra(INTENT_KEY_SELECTED_ACCOUNTS));
		if(selectedAccounts.size() > 0){
			mSelectedAccounts.addAll(selectedAccounts);
			Map<String, RKCloudChatContact> selectedContacts = RKCloudChatContactManager.getInstance(this).getContactInfos(selectedAccounts);
			for(String account : selectedContacts.keySet()){
				RKCloudChatContact obj = selectedContacts.get(account);
				if(null!=obj && !TextUtils.isEmpty(obj.getHeaderThumbImagePath())){
					mHeaderImgDatas.put(account, obj.getHeaderThumbImagePath());
				}
			}
		}
		mCurrSelectModel = getIntent().getIntExtra(INTENT_KEY_SELECT_MODEL, SELECT_MODEL_MUTLI);
					
		// 获取GridView中显示图像的宽度和行间距
		DisplayMetrics metric = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metric);
		mGridViewWidth = (int) getResources().getDimension(R.dimen.rkcloud_chat_selectuser_columnwidth);
		mGridViewHSpecing = (int)getResources().getDimension(R.dimen.rkcloud_chat_selectuser_gridview_horizontalspacing);
		
		mCurrAccount = RKCloud.getUserName();
		backgroundColorSpan = new BackgroundColorSpan(getResources().getColor(R.color.rkcloud_chat_search_result_highlightcolor));
		
		mContactManager = RKCloudChatContactManager.getInstance(this);
		
		mAllContacts = new ArrayList<RKCloudChatContact>();
		mDatas = new ArrayList<RKCloudChatContact>();
		mAdapter = new RKCloudChatSelectUsersAdapter();
		mListView.setAdapter(mAdapter);
		
		mSelectedAccounts.add(SELECTED_DEFAULT_ACCOUNT);
		mSelectedAdapter = new RKCloudChatHasSelectedUsersAdapter();
		mGridView.setAdapter(mSelectedAdapter);		
		setGridViewParams(true);
		
		mFirstLoadData = true;
		mLoadingPB.setVisibility(View.VISIBLE);	
	}
	
	/*
	 * 选中成员有变化时动态设置相关属性，使其单行显示选中的成员信息
	 */
	private void setGridViewParams(boolean toLast){	
		mSelectedAdapter.notifyDataSetChanged();
		LayoutParams linearParams = (LinearLayout.LayoutParams)mGridView.getLayoutParams();	
		linearParams.width = mSelectedAccounts.size()*(mGridViewWidth + mGridViewHSpecing);
		mGridView.setLayoutParams(linearParams);
		mGridView.setNumColumns(mSelectedAccounts.size());
		if(toLast){		
			mUiHandler.sendEmptyMessage(RKCloudChatUiHandlerMessage.SELECTED_CONTACT_GRIDVIEW_LAST);			
		}
		
		int size = mSelectedAccounts.size()-1;
		if(size > 0){
			mConfirmBtn.setText(String.format("%s(%d)", getString(R.string.rkcloud_chat_btn_confirm), size));
		}else{
			mConfirmBtn.setText(R.string.rkcloud_chat_btn_confirm);
		}
		mConfirmBtn.setEnabled(size > 0 ? true : false);
	}
	
	/*
	 * 查询数据
	 */
	private void startQuery(int queryType) {
		if (null == mQuerThread) {
			mQuerThread = new QueryHandlerThread("QuerySelectUsersActivityThread");
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
			Collections.sort(mAllContacts, new Comparator<RKCloudChatContact>()
			{
				@Override public int compare(RKCloudChatContact lhs, RKCloudChatContact rhs)
				{
					String lName = getFirstChar(lhs);
					String rName = getFirstChar(rhs);
					if(lName.equals(rName))return 0;
					if("#".equals(lName))return -1;
					if("#".equals(rName))return 1;
					return lName.compareTo(rName);
				}
			});
			mLoadingPB.setVisibility(View.GONE);
			startQuery(QUERY_TYPE_SEARCH);
			break;
			
		case RKCloudChatUiHandlerMessage.ADDRESSLIST_SEARCH_FINISHED:
			mDatas.clear();
			List<RKCloudChatContact> data2 = (List<RKCloudChatContact>)msg.obj;
			if(null!=data2 && data2.size()>0){
				mDatas.addAll(data2);
			}
			Collections.sort(mDatas, new Comparator<RKCloudChatContact>()
			{
				@Override public int compare(RKCloudChatContact lhs, RKCloudChatContact rhs)
				{
					String lName = getFirstChar(lhs);
					String rName = getFirstChar(rhs);
					if(lName.equals(rName))return 0;
					if("#".equals(lName))return -1;
					if("#".equals(rName))return 1;
					return lName.compareTo(rName);
				}
			});
			mAdapter.notifyDataSetChanged();
			mNoDataTV.setVisibility(mDatas.size()>0 ? View.GONE : View.VISIBLE);
			break;
			
		case RKCloudChatUiHandlerMessage.SELECTED_CONTACT_GRIDVIEW_LAST:
			int offsetX = mSelectedLinearLayout.getMeasuredWidth()-mGridViewWidth-mGridViewHSpecing-mHScrollView.getMeasuredWidth();
			if(offsetX > 0){
				mHScrollView.scrollTo(offsetX, 0);
			}
			break;
			
		case RKCloudChatUiHandlerMessage.CONTACTSINFO_CHANGED:// 联系人信息有变化
		case RKCloudChatUiHandlerMessage.CONTACT_HEADERIMAGE_CHANGED:// 联系人头像有变化
			startQuery(QUERY_TYPE_GET_ALL_CONTACTS);
			break;
		}
	}
	
	private class RKCloudChatSelectUsersAdapter extends BaseAdapter implements SectionIndexer{
		private Map<String, Integer> mSectionMap;
		private ItemViewBuffer mItemBuffer;// 每个条目的缓存对象
		
		public RKCloudChatSelectUsersAdapter(){
			mSectionMap = new HashMap<String, Integer>();
		}
		
		private class ItemViewBuffer {
			TextView categoryName;
			RoundedImageView headerPhotoView;
			TextView nameTV; 
			ImageView checkbox;
			ImageView divideView;
			
			public ItemViewBuffer(View convertView) {
				categoryName = (TextView) convertView.findViewById(R.id.categoryname);
				headerPhotoView = (RoundedImageView) convertView.findViewById(R.id.headerphoto);
				nameTV = (TextView) convertView.findViewById(R.id.name);
				checkbox = (ImageView) convertView.findViewById(R.id.checkbox);
				divideView = (ImageView) convertView.findViewById(R.id.divide);
			}
		}
		
		@Override
		public void notifyDataSetChanged() {
			updateSectionMap();
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
				convertView = getLayoutInflater().inflate(R.layout.rkcloud_chat_selectusers_item, null);
				mItemBuffer = new ItemViewBuffer(convertView);
				convertView.setTag(mItemBuffer);
			} else {
				mItemBuffer = (ItemViewBuffer) convertView.getTag();
			}
			final View view = convertView;
			final RKCloudChatContact obj = mDatas.get(position);
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
					
			mItemBuffer.nameTV.setText(null!=obj.highLightName ? obj.highLightName : obj.getShowName());
			mItemBuffer.headerPhotoView.setImageResource(R.drawable.rkcloud_chat_img_header_default);
			// 加载头像
			if(!TextUtils.isEmpty(obj.getHeaderThumbImagePath())){
				RKCloudChatImageRequest imageReq = new RKCloudChatImageRequest(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG, obj.getHeaderThumbImagePath(), obj.rkAccount);
				// 如果在缓存中则直接设置图片
				RKCloudChatImageResult imgResult = RKCloudChatImageAsyncLoader.getInstance(RKCloudChatSelectUsersActivity.this).sendPendingRequestQuryCache(imageReq);
				if(null!=imgResult && null!=imgResult.resource){
					mItemBuffer.headerPhotoView.setImageDrawable(imgResult.resource);
				}	
			}
			
			if(RKCloudChatSelectUsersActivity.SELECT_MODEL_SINGLE == mCurrSelectModel){
				mItemBuffer.checkbox.setImageResource(R.drawable.rkcloud_chat_radio_bg);
			}else{
				mItemBuffer.checkbox.setImageResource(R.drawable.rkcloud_chat_checkbox_bg);
			}
			
			if(mExistedAccounts.contains(obj.rkAccount) || obj.rkAccount.equalsIgnoreCase(mCurrAccount)){
				mItemBuffer.checkbox.setEnabled(false);
			}else{
				mItemBuffer.checkbox.setEnabled(true);
				mItemBuffer.checkbox.setSelected(mSelectedAccounts.contains(obj.rkAccount));
			}
			
			mItemBuffer.checkbox.setOnClickListener(new OnClickListener() {			
				@Override
				public void onClick(View v) {
					clickListViewItem(view, obj);
				}
			});
			
			return convertView;
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
		
//		private String getFirstChar (RKCloudChatContact obj){
//			String s = !TextUtils.isEmpty(obj.getSortKey()) ? obj.getSortKey() : obj.rkAccount;
//			if(!TextUtils.isEmpty(s)){
//				char first = s.charAt(0);
//				if((first>='A' && first<='Z') || (first>='a' && first<='z')){
//					return s.substring(0, 1).toUpperCase();
//				} else {
//					return "#";
//				}
//			}
//			return "";
//		}
	}

	private String getFirstChar(RKCloudChatContact obj)
	{
		char name = 0;
		String s = !TextUtils.isEmpty(obj.getSortKey()) ? obj.getSortKey() : obj.rkAccount;
		if (!TextUtils.isEmpty(s))
		{
			HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();// jar包
			// 汉字输出转码；
			defaultFormat.setCaseType(HanyuPinyinCaseType.UPPERCASE);// 设置成大写字母；
			defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
			try
			{
				if (null != PinyinHelper.toHanyuPinyinStringArray(s.charAt(0), defaultFormat))
				{
					name = PinyinHelper.toHanyuPinyinStringArray(s.charAt(0), defaultFormat)[0].charAt(0);
				}
				else
				{
					name = s.charAt(0);
				}
			}
			catch (BadHanyuPinyinOutputFormatCombination e)
			{
				e.printStackTrace();
			}
			if((name >= 'A' && name <= 'Z') || (name >= 'a' && name <= 'z'))
			{
				return String.valueOf(name).toUpperCase();
			}
			else
			{
				return "#";
			}
		}
		return String.valueOf(name);
	}


	private class RKCloudChatHasSelectedUsersAdapter extends BaseAdapter {
		private ItemViewBuffer mItemBuffer;

		public RKCloudChatHasSelectedUsersAdapter() {
		}
		
		/*
		 * 定义每个条目中使用View的基本元素
		 */
		private class ItemViewBuffer {
			ImageView headerPhotoView; // 头像
			
			public ItemViewBuffer(View convertView) {
				headerPhotoView = (ImageView) convertView.findViewById(R.id.headerimg);
			}
		}
		
		@Override
		public int getCount() {
			return mSelectedAccounts.size();
		}

		@Override
		public Object getItem(int arg0) {
			return mSelectedAccounts.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int arg0, View convertView, ViewGroup arg2) {
			if (null == convertView) {
				convertView = getLayoutInflater().inflate(R.layout.rkcloud_chat_has_selectedusers_item, null);
				mItemBuffer = new ItemViewBuffer(convertView);
				convertView.setTag(mItemBuffer);
			} else {
				mItemBuffer = (ItemViewBuffer) convertView.getTag();
			}

			String account = mSelectedAccounts.get(arg0);
			if(RKCloudChatSelectUsersActivity.SELECTED_DEFAULT_ACCOUNT.equalsIgnoreCase(account)){
				mItemBuffer.headerPhotoView.setImageResource(R.drawable.rkcloud_chat_selecteduser_default_headerimg);	
			}else{
				mItemBuffer.headerPhotoView.setImageResource(R.drawable.rkcloud_chat_img_header_default);	
				String headerImage = mHeaderImgDatas.get(account);
				RKCloudChatImageRequest imageReq = new RKCloudChatImageRequest(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG, headerImage, account);
				// 如果在缓存中则直接设置图片
				RKCloudChatImageResult imgResult = RKCloudChatImageAsyncLoader.getInstance(RKCloudChatSelectUsersActivity.this).sendPendingRequestQuryCache(imageReq);
				if(null!=imgResult && null!=imgResult.resource){
					mItemBuffer.headerPhotoView.setImageDrawable(imgResult.resource);
				}	
			}
			
			return convertView;
		}
	}
}
