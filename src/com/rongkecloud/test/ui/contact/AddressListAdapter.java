package com.rongkecloud.test.ui.contact;

import java.io.File;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.rongkecloud.chat.demo.tools.RKCloudChatTools;
import com.rongkecloud.chat.demo.ui.RKCloudChatSelectUsersActivity;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageAsyncLoader;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest.IMAGE_REQUEST_TYPE;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageResult;
import com.rongkecloud.test.R;
import com.rongkecloud.test.entity.Constants;
import com.rongkecloud.test.entity.ContactGroup;
import com.rongkecloud.test.entity.ContactInfo;
import com.rongkecloud.test.manager.ContactManager;
import com.rongkecloud.test.manager.PersonalManager;
import com.rongkecloud.test.ui.widget.RoundedImageView;
import com.rongkecloud.test.utility.OtherUtilities;

public class AddressListAdapter extends BaseExpandableListAdapter {
	private List<ContactGroup> mGroupList;
	private ContactManager mContactManager;
	private Context mContext;
	private LayoutInflater mInfalInflater;
	private AddressListActivity mActivity;
	private List<Integer> mExpandGroupIds;
	private ExpandableListView mExpandableListView;
	
	public AddressListAdapter(AddressListActivity activity, List<ContactGroup> groupList, List<Integer> openedGroups, ExpandableListView expandableListView) {
		mActivity = activity;
		mContext = activity.getActivity();
		mGroupList = groupList;
		mExpandGroupIds = openedGroups;
		mExpandableListView = expandableListView;
		mContactManager = ContactManager.getInstance();
		mInfalInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}	

	@Override
	public ContactGroup getGroup(int groupPosition) {
		return mGroupList.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return mGroupList.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}
	
	@Override
	public int getChildrenCount(int groupPosition) {
		if(groupPosition < mGroupList.size()){
			ContactGroup group = mGroupList.get(groupPosition);
			if(null!=group && null!=group.mContactFriendInfo){
				return group.mContactFriendInfo.size();
			}
		}
		return 0;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}
	
	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	@Override
	public ContactInfo getChild(int groupPosition, int childPosition) {
		if(groupPosition < mGroupList.size()){
			ContactGroup group = mGroupList.get(groupPosition);
			if(null!=group && null!=group.mContactFriendInfo && childPosition<group.mContactFriendInfo.size()){
				return group.mContactFriendInfo.get(childPosition);
			}
		}
		
		return null;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}
	
	@Override
	public View getGroupView(final int groupPosition, final boolean isExpanded, View convertView, ViewGroup parent) {
		if (null == convertView) {
			convertView = mInfalInflater.inflate(R.layout.contact_expistview_group, null);
		}
		
		ImageView openFlagImg = (ImageView) convertView.findViewById(R.id.group_indicator);
		TextView groupNameTV = (TextView) convertView.findViewById(R.id.textGroup);
		
		final ContactGroup group = (ContactGroup) getGroup(groupPosition);
		final Integer groupId = Integer.valueOf(group.mGroupId);
		
		groupNameTV.setText(group.mGroupName);
		if(mExpandGroupIds.contains(Integer.valueOf(group.mGroupId))){
			openFlagImg.setImageResource(R.drawable.icon_friendgroup_opened);
		}else{
			openFlagImg.setImageResource(R.drawable.icon_friendgroup_closed);
		}
		convertView.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				if(mExpandGroupIds.contains(groupId)){
					// 表示关闭操作
					mExpandableListView.collapseGroup(groupPosition);
					mExpandGroupIds.remove(groupId);
					
				}else{
					// 表示打开操作
					mExpandableListView.expandGroup(groupPosition);
					mExpandableListView.setSelectedGroup(groupPosition);
					group.mContactFriendInfo.clear();
					group.mContactFriendInfo.addAll(PersonalManager.getInstance().queryContactFriendsByGroupId(groupId));
					
					mExpandGroupIds.add(groupId);
					notifyDataSetChanged();
				}
			}
		});
		
		convertView.setOnLongClickListener(new OnLongClickListener() {			
			@Override
			public boolean onLongClick(View v) {
				int id = 0;
				if(0 == groupPosition){
					id = R.array.contact_manager_group_main;
				}else{
					id = R.array.contact_manager_group_item;
				}
				
				new AlertDialog.Builder(mContext)
					.setTitle(mContext.getResources().getString(R.string.contact_manager_group))
					.setItems(id, new DialogInterface.OnClickListener() {					
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case AddressListActivity.CONTACT_MANAGER_GROUP_ADD:
								addGroup();
								break;
								
							case AddressListActivity.CONTACT_MANAGER_GROUP_MODIFY:
								modifyGroup(group);
								break;
								
							case AddressListActivity.CONTACT_MANAGER_GROUP_DELETE:
								deleteGroup(group.mGroupId);
								break;
								
							case AddressListActivity.CONTACT_MANAGER_GROUP_SELECT_FRIENDS:
								AddressListActivity.mOpeGroupId = group.mGroupId;
								Intent intent = new Intent(mContext, RKCloudChatSelectUsersActivity.class);
								List<String> existAccounts = mContactManager.queryFriendAccountsByGroupId(group.mGroupId);
								if(null!=existAccounts && existAccounts.size()>0){
									String joinStrings = RKCloudChatTools.joinStrings(existAccounts);
									intent.putExtra(RKCloudChatSelectUsersActivity.INTENT_KEY_EXIST_ACCOUNTS, joinStrings);
								}
								mActivity.startActivityForResult(intent, AddressListActivity.INTENT_RESULT_SELECT_FRIENDS);
								break;
							}
						}
					}).create().show();
				return false;
			}
		});
		return convertView;
	}
	
	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = mInfalInflater.inflate(R.layout.contact_expistview_child, null);
		}
		// 解析UI组件
		TextView nameTV = (TextView) convertView.findViewById(R.id.textChild);
		RoundedImageView headerImg = (RoundedImageView) convertView.findViewById(R.id.text_child_avatar);
		
		ContactInfo obj = getChild(groupPosition, childPosition);
		// 设置显示的名称
		nameTV.setText(obj.getShowName());
		// 设置头像
		headerImg.setImageResource(R.drawable.rkcloud_chat_img_header_default);
		if(!TextUtils.isEmpty(obj.mThumbPath) && new File(obj.mThumbPath).exists()){
			RKCloudChatImageRequest imageReq = new RKCloudChatImageRequest(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG, obj.mThumbPath, obj.mAccount);
			RKCloudChatImageResult imgResult = RKCloudChatImageAsyncLoader.getInstance(mContext).sendPendingRequestQuryCache(imageReq);
			if(null!=imgResult && null!=imgResult.resource){
				headerImg.setImageDrawable(imgResult.resource);
			}	
		}
		
		// 同步头像
		if(obj.mAvatarServerVersion>0 && (TextUtils.isEmpty(obj.mThumbPath) || !new File(obj.mThumbPath).exists()
				|| obj.mAvatarServerVersion>obj.mAvatarClientThumbVersion)){	
			PersonalManager.getInstance().getAvatarThumb(obj.mAccount, obj.mAvatarServerVersion);
		}
		
		return convertView;
	}
	
	//添加分组
	private void addGroup(){
		View layout = LayoutInflater.from(mContext).inflate(R.layout.contact_input_remark, null);
		final EditText conEdit = (EditText)layout.findViewById(R.id.remark_input);
		InputFilter filter = new InputFilter.LengthFilter(30);
		conEdit.setFilters(new InputFilter[] {filter});
		new AlertDialog.Builder(mContext)
			.setTitle(mContext.getString(R.string.contact_manager_group_add))
			.setView(layout)
			.setNegativeButton(R.string.bnt_cancel, null)
			.setPositiveButton(R.string.bnt_confirm, new DialogInterface.OnClickListener() {			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String groupName = conEdit.getText().toString().trim();
					if(TextUtils.isEmpty(groupName)){
						OtherUtilities.showToastText(mContext, mContext.getString(R.string.contact_group_name_not_null));
					}else{
						if(groupName.equals(mContext.getString(R.string.contact_default_friendgroup))){
							OtherUtilities.showToastText(mContext, mContext.getString(R.string.contact_group_name_exist));
						}else{
							if(null == mContactManager.queryGroupInfoByName(groupName)){
								mActivity.showProgressDialog();
								mContactManager.operationGroupInfo(Constants.OPERATION_GROUP_ADD, groupName, 0);
							}else{
								OtherUtilities.showToastText(mContext, mContext.getString(R.string.contact_group_name_exist));
							}
						}
					}
				}
			}).create().show();
	}
	
	//修改分组
	private void modifyGroup(final ContactGroup groupInfo){
		View layout = LayoutInflater.from(mContext).inflate(R.layout.contact_input_remark, null);		
		final EditText conEdit = (EditText)layout.findViewById(R.id.remark_input);
		InputFilter filter = new InputFilter.LengthFilter(30);
		conEdit.setFilters(new InputFilter[] {filter});
		conEdit.setText(groupInfo.mGroupName);
		conEdit.setSelection(groupInfo.mGroupName.length());
		
		new AlertDialog.Builder(mContext)
			.setTitle(mContext.getString(R.string.contact_manager_group_modify))
			.setView(layout)
			.setNegativeButton(R.string.bnt_cancel, null)
			.setPositiveButton(R.string.bnt_confirm, new DialogInterface.OnClickListener() {			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String groupName = conEdit.getText().toString().trim();
					if(TextUtils.isEmpty(groupName)){
						OtherUtilities.showToastText(mContext, mContext.getString(R.string.contact_group_name_not_null));
					}else{
						if(groupName.equals(mContext.getString(R.string.contact_default_friendgroup))){
							OtherUtilities.showToastText(mContext, mContext.getString(R.string.contact_group_name_exist));
						}else{
							if(!groupName.equals(groupInfo.mGroupName)){
								mActivity.showProgressDialog();
								mContactManager.operationGroupInfo(Constants.OPERATION_GROUP_MODIFY, groupName, groupInfo.mGroupId);
							}
						}
					}
				}
			}).create().show();
	}
	
	//删除分组
	private void deleteGroup(final int groupId){
		new AlertDialog.Builder(mContext)
			.setTitle(mContext.getString(R.string.contact_manager_group_delete)).
			setMessage(mContext.getString(R.string.contact_manager_group_delete_confirm))
			.setNegativeButton(R.string.bnt_cancel, null)
			.setPositiveButton(R.string.bnt_confirm, new DialogInterface.OnClickListener() {			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mActivity.showProgressDialog();
					mContactManager.operationGroupInfo(Constants.OPERATION_GROUP_DELETE, "", groupId);
				}
			}).create().show();
	}
}
