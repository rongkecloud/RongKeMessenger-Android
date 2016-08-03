package com.rongkecloud.chat.demo.ui;

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
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.rongkecloud.chat.*;
import com.rongkecloud.chat.demo.RKCloudChatConstants;
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
import com.rongkecloud.chat.interfaces.RKCloudChatRequestCallBack;
import com.rongkecloud.sdkbase.RKCloud;
import com.rongkecloud.test.R;
import com.rongkecloud.test.ui.widget.RoundedImageView;
import com.rongkecloud.test.utility.Print;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.*;

public class RKCloudChatTransferGroupSelectUsersActivity extends RKCloudChatBaseActivity implements ImageLoadedCompleteDelayNotify
{
	public static final String INTENT_KEY_GROUP_USERS = "group_users";//群成员使用的key
	public static final String INTENT_KEY_GROUP_ID = "group_id";//群组ID使用的key
	public static final String INTENT_KEY_TO_ACCOUNT = "to_account";//传递新群主账号使用的key
	public static final String INTENT_KEY_FROM_MSG_ACTIVITY = "msg_activity";//来自于聊天页面
	public static final String INTENT_KEY_IS_GROUP_CREATER = "is_group_creater";//传递是否是群主使用的key
	// UI组件
	private ListView mListView;
	private ProgressBar mLoadingPB;
	private TextView mNoDataTV;
	private EditText mSearchET;
	private RKCloudChatSideBar mSideBar;
	private TextView mShowAlpha;
	private LinearLayout mAllLayout;
	private TextView mAllName;

	// 成员变量
	private RKCloudChatContactManager mContactManager;
	private List<RKCloudChatContact> mDatas;
	private List<RKCloudChatContact> mAllDatas;
	private RKCloudChatSelectUsersAdapter mAdapter;
	private BackgroundColorSpan backgroundColorSpan;
	private String mGroupId;
	private boolean isGroupCreater;
	private boolean isMsgActivity;

	private QueryHandlerThread mQuerThread;// 查询数据的线程
	private static final int QUERY_TYPE_SEARCH = 1;// 搜索

	@Override protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rkcloud_chat_transfergroup_selectusers);
		initUIAndListeners();
		initData();
	}

	@Override protected void onResume()
	{
		super.onResume();
		mContactManager.bindUiHandler(mUiHandler);
		RKCloudChatImageAsyncLoader.getInstance(this).registerDelayListener(this);

	}

	@Override protected void onDestroy()
	{

		super.onDestroy();
	}

	@Override public void onLoadImageCompleteDelayNotify(IMAGE_REQUEST_TYPE type, List<RKCloudChatImageResult> result)
	{
		if (IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG == type)
		{
			mAdapter.notifyDataSetChanged();
		}
	}

	private void initUIAndListeners()
	{
		// 设置title
		TextView titleTV = (TextView) findViewById(R.id.txt_title);
		titleTV.setText(R.string.bnt_return);
		titleTV.setTextColor(getResources().getColor(R.color.title_right_btn_text_color));
		titleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		titleTV.setOnClickListener(mExitListener);

		TextView text_title_content = (TextView) findViewById(R.id.text_title_content);
		text_title_content.setText(getString(R.string.rkcloud_chat_selectuser_title));
		mAllLayout = (LinearLayout) findViewById(R.id.layout_all);
		mAllName = (TextView) findViewById(R.id.layout_all_name);
		mAllLayout.setOnClickListener(new View.OnClickListener()
		{
			@Override public void onClick(View v)
			{
				Intent intent = getIntent();
				intent.putExtra(INTENT_KEY_TO_ACCOUNT,RKCloudChatConstants.KEY_GROUP_ALL);
				setResult(RESULT_OK,intent);
				finish();
			}
		});

		// 初始化UI元素
		mListView = (ListView) findViewById(R.id.transfergroup_listview);
		mLoadingPB = (ProgressBar) findViewById(R.id.transfergroup_loading_progressbar);
		mNoDataTV = (TextView) findViewById(R.id.transfergroup_emptytv);

		mSearchET = (EditText) findViewById(R.id.transfergroup_searchedittext);

		mSideBar = (RKCloudChatSideBar) findViewById(R.id.transfergroup_sidebar);
		mShowAlpha = (TextView) findViewById(R.id.transfergroup_selectedalpha);
		mSideBar.setListView(mListView);
		mSideBar.setTextView(mShowAlpha);

		mListView.setOnItemClickListener(new OnItemClickListener()
		{
			@Override public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				if (null == mDatas || 0 == mDatas.size())
				{
					return;
				}

				if(isMsgActivity)
				{
					if(null == mDatas.get(position))
					{
						return;
					}
					String account =  mDatas.get(position).rkAccount;
					Intent intent = getIntent();
					intent.putExtra(INTENT_KEY_TO_ACCOUNT,account);
					setResult(RESULT_OK,intent);
					finish();
				}
				else
				{
					final RKCloudChatContact obj = mDatas.get(position);
					if(null == obj)
					{
						return;
					}
					showProgressDialog();
					final RKCloudChatMessageManager mChatMessageManager = RKCloudChatMessageManager.getInstance(RKCloudChatTransferGroupSelectUsersActivity.this);
					mChatMessageManager.transferGroup(mGroupId, obj.rkAccount, new RKCloudChatRequestCallBack()
					{
						@Override public void onSuccess(Object results)
						{
							closeProgressDialog();
							Intent intent = getIntent();
							intent.putExtra(INTENT_KEY_TO_ACCOUNT,obj.rkAccount);
							setResult(RESULT_OK,intent);
							LocalMessage msg = LocalMessage.buildSendMsg(mGroupId,String.format(getString(R.string.rkcloud_chat_manage_transfer_group_tip),obj.getShowName()), RKCloud.getUserName());
							msg.setExtension(RKCloudChatConstants.FLAG_LOCAL_TIPMESSAGE);
							mChatMessageManager.addLocalMsg(msg,GroupChat.class);
							finish();
						}

						@Override public void onProgress(int value)
						{

						}

						@Override public void onFailed(int errorCode, Object object)
						{
							closeProgressDialog();
							RKCloudChatTools.showToastText(RKCloudChatTransferGroupSelectUsersActivity.this, String.valueOf(object));
						}
					});
				}

			}
		});

		mSearchET.addTextChangedListener(new TextWatcher()
		{
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
			}

			@Override public void onTextChanged(CharSequence s, int start, int before, int count)
			{
			}

			@Override public void afterTextChanged(Editable s)
			{
				startQuery(QUERY_TYPE_SEARCH);
			}
		});

	}

//	private void addHeadView()
//	{
//		View convertView = getLayoutInflater().inflate(R.layout.rkcloud_chat_selectusers_item, null);
//		TextView categoryName = (TextView) convertView.findViewById(R.id.categoryname);
//		categoryName.setVisibility(View.GONE);
//		RoundedImageView headerPhotoView = (RoundedImageView) convertView.findViewById(R.id.headerphoto);
//		headerPhotoView.setImageResource(R.drawable.rkcloud_chat_img_header_default);
//		TextView nameTV = (TextView) convertView.findViewById(R.id.name);
//		nameTV.setText(getString(R.string.rkcloud_chat_all_members));
//		ImageView checkbox = (ImageView) convertView.findViewById(R.id.checkbox);
//		checkbox.setVisibility(View.GONE);
//		mListView.addHeaderView(convertView);
//	}

	private void initData()
	{
		backgroundColorSpan = new BackgroundColorSpan(getResources().getColor(R.color.rkcloud_chat_search_result_highlightcolor));
		isMsgActivity = getIntent().getBooleanExtra(INTENT_KEY_FROM_MSG_ACTIVITY,false);
		isGroupCreater = getIntent().getBooleanExtra(INTENT_KEY_IS_GROUP_CREATER,false);
		if(isGroupCreater)
		{
			mAllLayout.setVisibility(View.VISIBLE);
		}
		else
		{
			mAllLayout.setVisibility(View.GONE);
		}
		mContactManager = RKCloudChatContactManager.getInstance(this);
		mGroupId = getIntent().getStringExtra(INTENT_KEY_GROUP_ID);
		List<String> tempList = getIntent().getStringArrayListExtra(INTENT_KEY_GROUP_USERS);
		mDatas = new ArrayList<RKCloudChatContact>(tempList.size());
		mAllDatas =  new ArrayList<RKCloudChatContact>(tempList.size());
		if(null != tempList && tempList.size() > 0)
		{
			for (String account : tempList)
			{
				mDatas.add(mContactManager.getContactInfo(account));
				mAllDatas.add(mContactManager.getContactInfo(account));
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

			Collections.sort(mAllDatas, new Comparator<RKCloudChatContact>()
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
		}

		mAdapter = new RKCloudChatSelectUsersAdapter();
		mListView.setAdapter(mAdapter);

		mLoadingPB.setVisibility(View.VISIBLE);
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

	private class QueryHandlerThread extends HandlerThread implements Callback
	{
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
				if(QUERY_TYPE_SEARCH == queryType)
				{
					List<RKCloudChatContact> datas = new ArrayList<RKCloudChatContact>();
					if(mAllDatas.size() > 0){
						datas.addAll(mAllDatas);
					}
					msg.obj = datas;
				}
				msg.sendToTarget();
			}
		}

		@Override
		public boolean handleMessage(Message msg) {
			if(QUERY_TYPE_SEARCH == msg.what){
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

	@Override public void processResult(Message msg)
	{
		switch (msg.what)
		{
			case RKCloudChatUiHandlerMessage.ADDRESSLIST_SEARCH_FINISHED:
				mDatas.clear();
				List<RKCloudChatContact> data2 = (List<RKCloudChatContact>) msg.obj;
				if (null != data2 && data2.size() > 0)
				{
					mDatas.addAll(data2);
				}
				mAdapter.notifyDataSetChanged();
				mNoDataTV.setVisibility(mDatas.size() > 0 ? View.GONE : View.VISIBLE);
				break;
		}
	}

	private class RKCloudChatSelectUsersAdapter extends BaseAdapter implements SectionIndexer
	{
		private Map<String, Integer> mSectionMap;
		private ItemViewBuffer mItemBuffer;// 每个条目的缓存对象

		public RKCloudChatSelectUsersAdapter()
		{
			mSectionMap = new HashMap<String, Integer>();
		}

		private class ItemViewBuffer
		{
			TextView categoryName;
			RoundedImageView headerPhotoView;
			TextView nameTV;
			ImageView checkbox;
			ImageView divideView;

			public ItemViewBuffer(View convertView)
			{
				categoryName = (TextView) convertView.findViewById(R.id.categoryname);
				headerPhotoView = (RoundedImageView) convertView.findViewById(R.id.headerphoto);
				nameTV = (TextView) convertView.findViewById(R.id.name);
				checkbox = (ImageView) convertView.findViewById(R.id.checkbox);
				checkbox.setVisibility(View.GONE);
				divideView = (ImageView) convertView.findViewById(R.id.divide);
			}
		}

		@Override public void notifyDataSetChanged()
		{
			updateSectionMap();
			super.notifyDataSetChanged();
		}

		@Override public Object[] getSections()
		{
			return null;
		}

		@Override public int getPositionForSection(int section)
		{
			String secStr = null;
			try
			{
				secStr = String.valueOf((char) section);
			}
			catch (Exception e)
			{
			}
			int ret = -1;
			if (!TextUtils.isEmpty(secStr) && mSectionMap.containsKey(secStr))
			{
				ret = mSectionMap.get(secStr);
			}

			if (ret >= 0 && ret < getCount())
			{
				return ret;
			}
			return -1;
		}

		@Override public int getSectionForPosition(int position)
		{
			return 0;
		}

		@Override public int getCount()
		{
			return mDatas.size();
		}

		@Override public Object getItem(int position)
		{
			return mDatas.get(position);
		}

		@Override public long getItemId(int position)
		{
			return position;
		}

		@Override public View getView(int position, View convertView, ViewGroup parent)
		{
			if (null == convertView)
			{
				convertView = getLayoutInflater().inflate(R.layout.rkcloud_chat_selectusers_item, null);
				mItemBuffer = new ItemViewBuffer(convertView);
				convertView.setTag(mItemBuffer);
			}
			else
			{
				mItemBuffer = (ItemViewBuffer) convertView.getTag();
			}
			final View view = convertView;
			final RKCloudChatContact obj = mDatas.get(position);
			if(null != obj)
			{
				String currcatalogName = getFirstChar(obj);
				String previousCataName = null;
				if (0 != position)
				{
					previousCataName = getFirstChar(mDatas.get(position-1));
				}
				// 分类名称显示的控制
				if (TextUtils.isEmpty(previousCataName) || !currcatalogName.equalsIgnoreCase(previousCataName))
				{
					mItemBuffer.categoryName.setVisibility(View.VISIBLE);
					mItemBuffer.categoryName.setText(currcatalogName);
				}
				else
				{
					mItemBuffer.categoryName.setVisibility(View.GONE);
				}

				// 分隔条的显示控制
				if (position < mDatas.size() - 1)
				{
					String afterCataName = getFirstChar(mDatas.get(position));
					if (currcatalogName.equalsIgnoreCase(afterCataName))
					{
						mItemBuffer.divideView.setVisibility(View.VISIBLE);
					}
					else
					{
						mItemBuffer.divideView.setVisibility(View.GONE);
					}
				}

				mItemBuffer.nameTV.setText(null != obj.highLightName ? obj.highLightName : obj.getShowName());
				mItemBuffer.headerPhotoView.setImageResource(R.drawable.rkcloud_chat_img_header_default);
				// 加载头像
				if (!TextUtils.isEmpty(obj.getHeaderThumbImagePath()))
				{
					RKCloudChatImageRequest imageReq = new RKCloudChatImageRequest(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG, obj.getHeaderThumbImagePath(), obj.rkAccount);
					// 如果在缓存中则直接设置图片
					RKCloudChatImageResult imgResult = RKCloudChatImageAsyncLoader.getInstance(RKCloudChatTransferGroupSelectUsersActivity.this).sendPendingRequestQuryCache(imageReq);
					if (null != imgResult && null != imgResult.resource)
					{
						mItemBuffer.headerPhotoView.setImageDrawable(imgResult.resource);
					}
				}
			}

			return convertView;
		}

		private void updateSectionMap()
		{
			mSectionMap.clear();
			if (null == mDatas || 0 == mDatas.size())
			{
				return;
			}
			int size = mDatas.size();
			for (int pos = 0; pos < size; pos++)
			{
				RKCloudChatContact obj = mDatas.get(pos);
				String sortKey = getFirstChar(obj);
				if (!mSectionMap.containsKey(sortKey))
				{
					mSectionMap.put(sortKey, pos);
				}
			}
		}

//		private String getFirstChar(RKCloudChatContact obj)
//		{
//			String s = !TextUtils.isEmpty(obj.getSortKey()) ? obj.getSortKey() : obj.rkAccount;
//			if (!TextUtils.isEmpty(s))
//			{
//				char first = s.charAt(0);
//				if ((first >= 'A' && first <= 'Z') || (first >= 'a' && first <= 'z'))
//				{
//					return s.substring(0, 1).toUpperCase();
//				}
//				else
//				{
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

}
