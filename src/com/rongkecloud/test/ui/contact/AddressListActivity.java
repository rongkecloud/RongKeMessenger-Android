package com.rongkecloud.test.ui.contact;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.ExpandableListView.OnChildClickListener;
import com.rongkecloud.chat.demo.ui.RKCloudChatGroupListActivity;
import com.rongkecloud.chat.demo.ui.RKCloudChatSelectUsersActivity;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseFragment;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageAsyncLoader;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageAsyncLoader.ImageLoadedCompleteDelayNotify;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest.IMAGE_REQUEST_TYPE;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageResult;
import com.rongkecloud.customerservice.RKServiceChatConnectServiceManager;
import com.rongkecloud.test.R;
import com.rongkecloud.test.entity.Constants;
import com.rongkecloud.test.entity.ContactGroup;
import com.rongkecloud.test.entity.ContactInfo;
import com.rongkecloud.test.http.HttpResponseCode;
import com.rongkecloud.test.manager.ContactManager;
import com.rongkecloud.test.manager.MessageManager;
import com.rongkecloud.test.manager.PersonalManager;
import com.rongkecloud.test.manager.uihandlermsg.ContactUiMessage;
import com.rongkecloud.test.system.ConfigKey;
import com.rongkecloud.test.system.RKCloudDemo;
import com.rongkecloud.test.utility.OtherUtilities;

import java.util.ArrayList;
import java.util.List;

public class AddressListActivity extends RKCloudChatBaseFragment implements ImageLoadedCompleteDelayNotify{
	
	private static final String TAG = AddressListActivity.class.getSimpleName();
	//从其他ui传回的结果码
	public static final int INTENT_RESULT_SELECT_FRIENDS = 2;//选择好友
	
	//上下文操作项
	public static final int CONTACT_MANAGER_GROUP_ADD = 0;//创建分组
	public static final int CONTACT_MANAGER_GROUP_SELECT_FRIENDS = 1;//选择分组好友
	public static final int CONTACT_MANAGER_GROUP_MODIFY = 2;//修改分组
	public static final int CONTACT_MANAGER_GROUP_DELETE = 3;//删除分组
	
	private List<Integer> mExpandGroupIds = new ArrayList<Integer>();// 展开的所有组信息
	public static int mOpeGroupId = -1;// 操作的groupid
	
	// UI组件	
	private LinearLayout mContactGroupLayout;
	private LinearLayout mNewFriendLayout;
	private LinearLayout mServiceLayout;
	public ImageView mUnReadCount;
	private ExpandableListView mExpandableListView;
	
	private ContactManager mContactManager;
	private PersonalManager mPersonalManager;
	private List<ContactGroup> mDatas; 
	private AddressListAdapter mAdapter;
	private Context mContext;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.addresslist, container, false);
		view.setOnClickListener(null);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		initUIAndListeners();
		
		mContext = getActivity();
		mDatas = new ArrayList<ContactGroup>();
		// 添加“我的好友”分组
		ContactGroup contactGroup = new ContactGroup();
		contactGroup.mGroupId = 0;
		contactGroup.mGroupName = getString(R.string.contact_default_friendgroup);
		mDatas.add(contactGroup);
		mAdapter = new AddressListAdapter(this, mDatas, mExpandGroupIds, mExpandableListView);
		mExpandableListView.setAdapter(mAdapter);
		
		mContactManager = ContactManager.getInstance();
		mPersonalManager = PersonalManager.getInstance();
		
		initGroupDatas();
		if(System.currentTimeMillis()-RKCloudDemo.config.getLong(ConfigKey.SYNC_ALLGROUPS_LASTTIME, 0l) >= Constants.SYNC_ALLGROUPINFOS_TIME_INTERVAL){
			mContactManager.syncGroupInfos();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		refresh();
	}
	
	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		if(!hidden){
			refresh();
		}
	}
	
	@Override
	public void refresh(){
		mShow = true;
		mContactManager.bindUiHandler(mUiHandler);
		mPersonalManager.bindUiHandler(mUiHandler);
		MessageManager.getInstance().bindUiHandler(mUiHandler);
		mContext = getActivity();
		RKCloudChatImageAsyncLoader.getInstance(mContext).registerDelayListener(this);
		
		// 更新好友通知提示
		updateUnreadNotify();
		refreshOpenedGroupFriends();
	}

	@Override
	public void onLoadImageCompleteDelayNotify(IMAGE_REQUEST_TYPE type, List<RKCloudChatImageResult> result) {
		if(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG == type){
			mAdapter.notifyDataSetChanged();
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(Activity.RESULT_OK == resultCode){
			switch (requestCode) {			
			case INTENT_RESULT_SELECT_FRIENDS://选择好友成功后返回到主界面，携带选择好的好友，提交服务器，修改此好友的分组id
				String selectedAccounts = data.getStringExtra(RKCloudChatSelectUsersActivity.INTENT_RETURN_KEY_SELECTED_ACCOUNTS);
				if(null==selectedAccounts){
					return;
				}
				
				String[] arrs = selectedAccounts.split(",");
				List<String> accounts = new ArrayList<String>(arrs.length);
				for(String account : arrs){
					accounts.add(account);
				}
				
				showProgressDialog();
				mContactManager.modifyFriendsGroup(accounts, mOpeGroupId);
				break;
			}
		}
	}
	
	private void initUIAndListeners(){
		// 设置title
		TextView titleTV = (TextView)getView().findViewById(R.id.txt_title);
		titleTV.setText(R.string.rkcloud_chat_address_title);
        titleTV.setVisibility(TextView.GONE);

        TextView text_title_content = (TextView)getView().findViewById(R.id.text_title_content);
        text_title_content.setText(R.string.rkcloud_chat_address_title);

		ImageButton mAddFriendBtn = (ImageButton) getView().findViewById(R.id.title_imgbtns_rightbtn);
		mAddFriendBtn.setImageResource(R.drawable.contact_add_friend);
		mAddFriendBtn.setVisibility(View.VISIBLE);
		
		// 初始化UI元素
		mContactGroupLayout = (LinearLayout) getView().findViewById(R.id.contact_group);
		mNewFriendLayout = (LinearLayout) getView().findViewById(R.id.contact_newfriend_layout);
		mUnReadCount = (ImageView) getView().findViewById(R.id.notify_unread);
		mServiceLayout = (LinearLayout) getView().findViewById(R.id.service_layout);
		mExpandableListView = (ExpandableListView) getView().findViewById(R.id.contact_group_listview);
		
		//添加好友
		mAddFriendBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(new Intent(mContext, AddFriendsActivity.class));
			}
		});
		
		//新的朋友
		mNewFriendLayout.setOnClickListener(new OnClickListener() {		
			@Override
			public void onClick(View v) {
				//新的朋友点击事件，跳转到好友通知界面
				startActivity(new Intent(mContext, NotifyActivity.class));
			}
		});
		
		//云视互动小秘书
		mServiceLayout.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//发消息，调用云视互动sdk发起消息界面
				RKServiceChatConnectServiceManager.getInstance(getContext().getApplicationContext()).startConnectCustomerService("6f2683bb7f9b98aa09283fd8b47f4086aec37b56", 0xFF38A1DB, 143);
			}
		});
		
		//分组列表
		mExpandableListView.setOnChildClickListener(new OnChildClickListener() {			
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				ContactGroup group = mDatas.get(groupPosition);
				ContactInfo friend = group.mContactFriendInfo.get(childPosition);
				Intent intent = new Intent(mContext, ContactDetailInfoActivity.class);
				intent.putExtra(ContactDetailInfoActivity.INTENT_CONTACT_ACCOUNT, friend.mAccount);
				startActivity(intent);
				return false;
			}
		});
		
		//进入群聊
		mContactGroupLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				/**
				 * 群聊点击事件，跳转到sdk对应的群聊界面
				 * 群聊界面没有自己做，直接引用云视互动sdk群聊界面
				 */
				startActivity(new Intent(mContext, RKCloudChatGroupListActivity.class));
			}
		});
	}	
	
	private void updateUnreadNotify(){
		int unReadCnt = mContactManager.getFriendNotifyUnreadCount();
		if(unReadCnt > 0){
			mUnReadCount.setVisibility(View.VISIBLE);
		}else{
			mUnReadCount.setVisibility(View.GONE);
		}
	}
	
	private void initGroupDatas(){
		mDatas.clear();
		// 添加“我的好友”分组
		ContactGroup contactGroup = new ContactGroup();
		contactGroup.mGroupId = 0;
		contactGroup.mGroupName = getString(R.string.contact_default_friendgroup);
		mDatas.add(contactGroup);
		List<ContactGroup> groups = mContactManager.queryAllGroupInfos();
		if(null!=groups && groups.size()>0){
			mDatas.addAll(groups);
		}
	}
	
	/*
	 * 刷新展开的组的所有好友信息
	 */
	private void refreshOpenedGroupFriends(){
		if(mExpandGroupIds.size() > 0){
			for(int groupId : mExpandGroupIds){
				for(ContactGroup groupObj : mDatas){
					if(groupObj.mGroupId == groupId){
						groupObj.mContactFriendInfo.clear();
						groupObj.mContactFriendInfo.addAll(mPersonalManager.queryContactFriendsByGroupId(groupObj.mGroupId));
						break;
					}
				}
				
			}
		}
        mAdapter.notifyDataSetChanged();
	}

	@Override
	public void processResult(Message msg) {
		if(!mShow){
			return;
		}
		
		switch(msg.what){		
		case ContactUiMessage.RECEIVED_FRIEND_ADDREQUEST:	
			updateUnreadNotify();
			break;
			
		case ContactUiMessage.SYNC_GROUP_INFOS:
			initGroupDatas();
			break;
			
		case ContactUiMessage.SYNC_FRIEND_INFOS:	
		case ContactUiMessage.SYNC_PERSON_INFOS:
			refreshOpenedGroupFriends();
			break;
		
		case ContactUiMessage.RESPONSE_OPERATION_GROUP:
			closeProgressDialog();
			if(HttpResponseCode.OK == msg.arg1){
				int type = (Integer)msg.obj;
				int groupId = msg.arg2;
				
				ContactGroup modifyGroup = mContactManager.queryGroupInfoById(groupId);
				switch (type) {
				case Constants.OPERATION_GROUP_ADD:
					mDatas.add(modifyGroup);
					break;
					
				case Constants.OPERATION_GROUP_MODIFY:
					for(ContactGroup group : mDatas){
						if(group.mGroupId == groupId){
							group.mGroupName = modifyGroup.mGroupName;
							break;
						}
					}
					break;
					
				case Constants.OPERATION_GROUP_DELETE:	
					// 更新默认分组中的好友
					if(mExpandGroupIds.contains(Integer.valueOf(0))){
						for(ContactGroup groupObj : mDatas){
							if(groupObj.mGroupId == 0){
								groupObj.mContactFriendInfo.clear();
								groupObj.mContactFriendInfo.addAll(mPersonalManager.queryContactFriendsByGroupId(0));
								break;
							}
						}
					}
					// 删除组
					int removeIndex = -1;
					for(int i=0; i<mDatas.size(); i++){
						if(mDatas.get(i).mGroupId == groupId){
							removeIndex = i;
							break;
						}
					}
					if(-1 != removeIndex){
						mDatas.remove(removeIndex);
					}
					break;
				}
				
				mAdapter.notifyDataSetChanged();
				
			}else if(HttpResponseCode.GROPNAME_HASEXIST == msg.arg1){
				OtherUtilities.showToastText(mContext, getString(R.string.contact_group_name_exist));
			}else if(HttpResponseCode.NO_NETWORK == msg.arg1){
				OtherUtilities.showToastText(mContext, getString(R.string.network_off));
			}else{
				OtherUtilities.showToastText(mContext, getString(R.string.operation_failed));
			}
			break;
			
		case ContactUiMessage.RESPONSE_MODIFY_FRIENDS_GROUP:
			closeProgressDialog();
			if(HttpResponseCode.OK == msg.arg1){
				refreshOpenedGroupFriends();
				
			}else if(HttpResponseCode.NO_NETWORK == msg.arg1){
				OtherUtilities.showToastText(mContext, getString(R.string.network_off));
			}else{
				OtherUtilities.showToastText(mContext, getString(R.string.operation_failed));
			}
			break;
			
		case ContactUiMessage.RESPONSE_MODIFY_FRIENDINFO:
		case ContactUiMessage.RESPONSE_GET_AVATAR_THUMB:
			closeProgressDialog();
			if(HttpResponseCode.OK == msg.arg1){
				String account = (String)msg.obj;
				if(mExpandGroupIds.size() <= 0){
					return;
				}
				
				for(int groupId : mExpandGroupIds){
					for(ContactGroup groupObj : mDatas){
						for(ContactInfo friend : groupObj.mContactFriendInfo){
							if(friend.mAccount.equalsIgnoreCase(account)){
								friend.copyData(mPersonalManager.getContactInfo(account));
								mAdapter.notifyDataSetChanged();
								break;
							}
						}
					}
				}

			}else if(HttpResponseCode.NO_NETWORK == msg.arg1){
				OtherUtilities.showToastText(mContext, getString(R.string.network_off));
				return;
			}else{
				OtherUtilities.showToastText(mContext, getString(R.string.operation_failed));
				return;
			}
			break;
			
		case ContactUiMessage.RECEIVED_FRIEND_ADDCONFIRM:
			int groupId = msg.arg1;
			if(mExpandGroupIds.contains(Integer.valueOf(groupId))){
				for(ContactGroup groupObj : mDatas){
					if(groupObj.mGroupId == groupId){
						groupObj.mContactFriendInfo.clear();
						groupObj.mContactFriendInfo.addAll(mPersonalManager.queryContactFriendsByGroupId(groupObj.mGroupId));
						mAdapter.notifyDataSetChanged();
						break;
					}
				}
			}
			updateUnreadNotify();
			break;
			
		case ContactUiMessage.RECEIVED_FRIEND_DELETED:
			if(mExpandGroupIds.size() > 0){
				String delAccount = (String)msg.obj;
				for(int gId : mExpandGroupIds){
					for(ContactGroup groupObj : mDatas){
						if(groupObj.mGroupId == gId){
							int removeAccountIndex = -1;
							for (int i = 0; i < groupObj.mContactFriendInfo.size(); i++) {
								if(groupObj.mContactFriendInfo.get(i).mAccount.equalsIgnoreCase(delAccount)){
									removeAccountIndex = i;
									break;
								}
							}
							if(-1 != removeAccountIndex){
								groupObj.mContactFriendInfo.remove(removeAccountIndex);
								mAdapter.notifyDataSetChanged();
							}
							break;
						}
					}
				}
			}
			updateUnreadNotify();
			break;
			
		case ContactUiMessage.RESPONSE_CONFIRM_ADDFRIEND:
			if(HttpResponseCode.OK == msg.arg1){
				if(mExpandGroupIds.contains(Integer.valueOf(0))){
					for(ContactGroup groupObj : mDatas){
						if(groupObj.mGroupId == 0){
							groupObj.mContactFriendInfo.clear();
							groupObj.mContactFriendInfo.addAll(mPersonalManager.queryContactFriendsByGroupId(0));
							break;
						}
					}
					mAdapter.notifyDataSetChanged();
				}
			}
			break;
		}
	}
}
