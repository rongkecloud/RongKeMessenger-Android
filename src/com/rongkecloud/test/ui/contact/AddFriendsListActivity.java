package com.rongkecloud.test.ui.contact;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.test.R;
import com.rongkecloud.test.entity.Account;
import com.rongkecloud.test.entity.Constants;
import com.rongkecloud.test.entity.ContactInfo;
import com.rongkecloud.test.entity.FriendNotify;
import com.rongkecloud.test.http.HttpResponseCode;
import com.rongkecloud.test.manager.AccountManager;
import com.rongkecloud.test.manager.ContactManager;
import com.rongkecloud.test.manager.uihandlermsg.ContactUiMessage;
import com.rongkecloud.test.utility.OtherUtilities;

public class AddFriendsListActivity extends RKCloudChatBaseActivity {
	
	public static final String INTENT_KEY_USER = "intent_key_user";
	
	private ListView mListView;
	private ContactManager mContactManager;
	private AddFriendsListAdapter mAdapter;
	
	private List<ContactInfo> mDatas;
	private List<String> mAddedAccounts;// 已经申请添加的账号
	private String mCurrAccount;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_friend_from_contact_main);
		initUIAndListeners();
		initData();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mContactManager.bindUiHandler(mUiHandler);
		mAdapter.notifyDataSetChanged();
	}
	
	private void initUIAndListeners(){
		// 设置title
		TextView titleTV = (TextView)findViewById(R.id.txt_title);
		titleTV.setText(R.string.bnt_return);
		titleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		titleTV.setOnClickListener(mExitListener);

        TextView text_title_content = (TextView)findViewById(R.id.text_title_content);
        text_title_content.setText(R.string.contact_search_friend_title);

		// 初始化UI元素
		mListView = (ListView) findViewById(R.id.mListView);		
		
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				//列表条目的点击事件，跳转到详细信息界面
				ContactInfo obj = mDatas.get(arg2);
				Intent intent = new Intent(AddFriendsListActivity.this, ContactDetailInfoActivity.class);
				intent.putExtra(ContactDetailInfoActivity.INTENT_CONTACT_ACCOUNT, obj.mAccount);
				startActivity(intent);
			}
		});
	}
	
	//初始化数据
	private void initData(){
		mContactManager = ContactManager.getInstance();
		Account accObj = AccountManager.getInstance().getCurrentAccount();
		mCurrAccount = null!=accObj ? accObj.loginName : null;
		mDatas = getIntent().getParcelableArrayListExtra(INTENT_KEY_USER);
		mAddedAccounts = new ArrayList<String>();
		for(ContactInfo info : mDatas){
			if(info.mGroupId >= 0){
				mAddedAccounts.add(info.mAccount);
			}
		}
		// 获取好友通知中的信息
		List<FriendNotify> notifyDatas = mContactManager.queryAllNotifys();
		for(FriendNotify obj : notifyDatas){
			boolean addAccount = false;
			if(obj.type.equals(Constants.MESSGE_TYPE_ADD_REQUEST)){
				if(obj.status == Constants.FRIEND_NOTIFY_STATUS_VERIFY){ // 通过验证
					
				}else if(obj.status == Constants.FRIEND_NOTIFY_STATUS_HAS_FRIEND){ // 已添加
					addAccount = true;
				}				
			}else if(obj.type.equals(Constants.MESSGE_TYPE_ADD_CONRIFM)){
				if(obj.status == Constants.FRIEND_NOTIFY_STATUS_WAITVERIFY){ // 等待验证
					addAccount = true;
				}else if(obj.status == Constants.FRIEND_NOTIFY_STATUS_ADD){ // 加为好友
					
				}else if(obj.status == Constants.FRIEND_NOTIFY_STATUS_HAS_FRIEND){ // 已添加
					addAccount = true;
				}
			}
			if(addAccount){
				if(!mAddedAccounts.contains(obj.account)){
					mAddedAccounts.add(obj.account);
				}
			}
		}
		
		mAdapter = new AddFriendsListAdapter();
		mListView.setAdapter(mAdapter);
	}

	@Override
	public void processResult(Message msg) {
		switch (msg.what) {
		case ContactUiMessage.RESPONSE_ADDFRIEND:
			closeProgressDialog();
			String addAccount = (String)msg.obj;
			if(HttpResponseCode.OK == msg.arg1){
				for(ContactInfo data : mDatas){
					if(data.mAccount.equalsIgnoreCase(addAccount)){
						data.mGroupId = 0;
						mAddedAccounts.add(addAccount);
						break;
					}
				}
				
				mAdapter.notifyDataSetChanged();
				OtherUtilities.showToastText(this, getString(R.string.operation_success));
				
			}else if(HttpResponseCode.ADDFRIEND_NEEDVERIFY == msg.arg1){
				mContactManager.showAddFriendVerfiyWin(this, (String)msg.obj);
				
			}else if(HttpResponseCode.ADDFRIEND_WAITVERIFY == msg.arg1){
				OtherUtilities.showToastText(this, getString(R.string.contact_add_waitverify));
				mAddedAccounts.add(addAccount);
				mAdapter.notifyDataSetChanged();
				
			}else if(HttpResponseCode.ADDFRIEND_FORBIDYOURSELF == msg.arg1){
				OtherUtilities.showToastText(this, getString(R.string.contact_add_yourself));
				
			}else{
				OtherUtilities.showToastText(this, getString(R.string.operation_failed));
			}
			break;
		}
	}
	
	private class AddFriendsListAdapter extends BaseAdapter {
		private ItemViewBuffer mItemBuffer;
		
		public AddFriendsListAdapter(){
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
				convertView = getLayoutInflater().inflate(R.layout.add_friend_from_contact_list_item, null);
				mItemBuffer = new ItemViewBuffer(convertView);
				convertView.setTag(mItemBuffer);
			}else{
				mItemBuffer = (ItemViewBuffer) convertView.getTag();
			}
			
			final ContactInfo obj = mDatas.get(position);
			mItemBuffer.mTextView.setText(obj.mAccount);
			if(obj.mGroupId >= 0){
				mItemBuffer.mTip.setVisibility(View.VISIBLE);
				mItemBuffer.mTip.setText(R.string.contact_has_been_friend);
				mItemBuffer.mAddBtn.setVisibility(View.GONE);
			}else{
				if(obj.mAccount.equalsIgnoreCase(mCurrAccount)){
					mItemBuffer.mTip.setVisibility(View.GONE);
					mItemBuffer.mAddBtn.setVisibility(View.GONE);
				}else{
					if(mAddedAccounts.contains(obj.mAccount)){
						mItemBuffer.mTip.setVisibility(View.VISIBLE);
						mItemBuffer.mTip.setText(R.string.contact_waiting_verify);
						mItemBuffer.mAddBtn.setVisibility(View.GONE);
					}else{
						mItemBuffer.mTip.setVisibility(View.GONE);
						mItemBuffer.mAddBtn.setVisibility(View.VISIBLE);
						mItemBuffer.mAddBtn.setOnClickListener(new OnClickListener() {		
							@Override
							public void onClick(View v) {
								showProgressDialog();
								mContactManager.addFriend(obj.mAccount, "");
							}
						});
					}
				}
			}
			
			return convertView;
		}
		
		private class ItemViewBuffer {
			TextView mTextView;
			Button mAddBtn;
			TextView mTip;
			
			public ItemViewBuffer(View convertView) {
				mTextView = (TextView) convertView.findViewById(R.id.contact_name);
				mAddBtn = (Button) convertView.findViewById(R.id.add_contact);
				mTip = (TextView) convertView.findViewById(R.id.friendtip);
			}
		}
	}
}
