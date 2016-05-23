package com.rongkecloud.test.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;

import com.rongkecloud.chat.LocalMessage;
import com.rongkecloud.chat.SingleChat;
import com.rongkecloud.chat.demo.RKCloudChatConstants;
import com.rongkecloud.chat.demo.RKCloudChatMmsManager;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatCustomDialog;
import com.rongkecloud.sdkbase.RKCloud;
import com.rongkecloud.test.R;
import com.rongkecloud.test.db.dao.ContactNotifyDao;
import com.rongkecloud.test.db.dao.ContactsDao;
import com.rongkecloud.test.entity.Constants;
import com.rongkecloud.test.entity.ContactGroup;
import com.rongkecloud.test.entity.ContactInfo;
import com.rongkecloud.test.entity.FriendNotify;
import com.rongkecloud.test.http.HttpCallback;
import com.rongkecloud.test.http.HttpResponseCode;
import com.rongkecloud.test.http.HttpTools;
import com.rongkecloud.test.http.HttpType;
import com.rongkecloud.test.http.Progress;
import com.rongkecloud.test.http.Request;
import com.rongkecloud.test.http.Result;
import com.rongkecloud.test.manager.uihandlermsg.ContactUiMessage;
import com.rongkecloud.test.system.ConfigKey;
import com.rongkecloud.test.system.RKCloudDemo;

/**
 * 好友分组及好友相关的管理器操作类
 */
public class ContactManager{	
	private static ContactManager mInstance = null;
	private ContactsDao mContactsDao;
	private ContactNotifyDao mContactNotifyDao;
	private PersonalManager mPersonalManager;
	private Handler mUiHandler;
	
	private ContactManager(){
		mPersonalManager = PersonalManager.getInstance();
		mContactsDao = new ContactsDao();
		mContactNotifyDao = new ContactNotifyDao();
	}
	
	public static ContactManager getInstance(){
		if(null == mInstance){
			mInstance = new ContactManager();
		}
		return mInstance;
	}
	
	public void bindUiHandler(Handler handler) {
		mUiHandler = handler;
	}
	
	//////////////////////////////////////////////db操作  begin//////////////////////////////////////////////////
	//获取所有分组
	public List<ContactGroup> queryAllGroupInfos(){
		return mContactsDao.queryAllGroupInfos();
	}
	
	// 获取对应分组所拥有的好友账号
	public List<String> queryFriendAccountsByGroupId(int gid){
		return mContactsDao.queryFriendAccountsByGroupId(gid);
	}
	
	public Map<String, Integer> getGroupIdByAccounts(){
		return mContactsDao.getGroupIdsByAccounts();
	}
	
	//根据分组名称查询分组
	public ContactGroup queryGroupInfoById(int groupId){
		return mContactsDao.queryGroupInfo(groupId);
	}
	//根据分组名称查询分组
	public ContactGroup queryGroupInfoByName(String gname){
		return mContactsDao.queryGroupInfo(gname);
	}
	// 根据账号查询好友所在的分组id
	public int queryGroupId(String account){
		return mContactsDao.queryGroupId(account);
	}
	
	//插入分组
	public boolean insertContactGroup(int groupId, String groupName){
		return mContactsDao.insertGroup(groupId, groupName);
	}
	
	// 插入好友分组信息
	public boolean insertGroupFriend(int groupId, String account){
		return mContactsDao.insertFriend(groupId, account);
	}
	
	//删除分组好友
	public boolean deleteContactFriends(String account){
		return mContactsDao.deleteFriend(account);
	}
	
	/////////好友通知相关的内容  begin//////////////////////////////////////////
	
	// 获取所有的好友通知
	public List<FriendNotify> queryAllNotifys() {
		return mContactNotifyDao.queryAllNotifys();
	}
	
	// 获取好友通知的未读条数
	public int getFriendNotifyUnreadCount(){
		return mContactNotifyDao.getNotityCnt();
	}
	
	// 插入好友通知
	public boolean insertFriendsNotify(FriendNotify obj){
		return mContactNotifyDao.addFriendNotify(obj);
	}

	// 更新好友通知为已读状态
	public long updateNotifyReaded() {
		return mContactNotifyDao.updateNotifyReaded();
	}	

	// 删除好友通知信息
	public long delFriendsNotify(String account) {
		return mContactNotifyDao.delFriendsNotify(account);
	}
	
	// 删除好友通知信息
	public long delFriendsNotify(String account, String type) {
		return mContactNotifyDao.delFriendsNotify(account, type);
	}
	
	// 清空所有好友通知
	public long delAllFriendsNotify(){
		return mContactNotifyDao.delAllFriendsNotify();
	}
	
	// 更新好友通知
	public boolean updateFriendNotify(FriendNotify data){
		return mContactNotifyDao.updateFriendNotify(data);
	}
	/////////好友通知相关的内容  end//////////////////////////////////////////

	//////////////////////////////////////////////db操作 end////////////////////////////////////////////////////
	
	//////////////与api有关的交互 begin/////////////////////////////////////////
	/**
	 * 同步组信息
	 */
	public void syncGroupInfos(){
		String session= AccountManager.getInstance().getSession();
		if(TextUtils.isEmpty(session)){
			return;
		}
		Request request = new Request(HttpType.GET_GROUP_INFO, RKCloudDemo.kit.getHttpHost(HttpTools.HTTPHOST_TYPE_ROOT), HttpTools.GET_GROUP_INFO_URL);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("ss", session);
		request.params = params;
		request.mHttpCallback = new HttpCallback() {
			@Override
			public void onThreadResponse(Result result) {
				if(result.opCode == HttpResponseCode.OK){
					try {
						//获取成功的操作
						if(!TextUtils.isEmpty(result.values.get("result"))){						
							JSONArray jsonArray = new JSONArray(result.values.get("result"));
							if(jsonArray.length() > 0){
								List<ContactGroup> groupList = new ArrayList<ContactGroup>(jsonArray.length());
								JSONObject obj = null;
								ContactGroup groupObj = null;
								for (int i = 0; i < jsonArray.length(); i++) {
									obj = new JSONObject(jsonArray.get(i).toString());
									groupObj = new ContactGroup();
									groupObj.mGroupId = obj.getInt("gid");
									groupObj.mGroupName = obj.getString("gname");
									groupList.add(groupObj);
								}
								
								//将分组信息插入到分组表中
								mContactsDao.batchInsertGroups(groupList);
								if(null != mUiHandler){
									mUiHandler.sendEmptyMessage(ContactUiMessage.SYNC_GROUP_INFOS);
								}
							}
						}
						
						RKCloudDemo.config.put(ConfigKey.SYNC_ALLGROUPS_LASTTIME, System.currentTimeMillis());
						// 获取好友信息
						syncFriendsInfo();
					
					} catch (Exception e) {
						e.printStackTrace();
					} 
				}
			}
			
			@Override
			public void onThreadProgress(Progress progress) {
			}
		};
		RKCloudDemo.kit.execute(request);
	}
	
	/**
	 * 同步好友信息
	 */
	public void syncFriendsInfo(){
		String session= AccountManager.getInstance().getSession();
		if(TextUtils.isEmpty(session)){
			return;
		}
		Request request = new Request(HttpType.GET_FRIENDS_INFO, RKCloudDemo.kit.getHttpHost(HttpTools.HTTPHOST_TYPE_ROOT), HttpTools.GET_FRIENDS_INFO_URL);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("ss", session);
		request.params = params;
		request.mHttpCallback = new HttpCallback() {
			@Override
			public void onThreadResponse(Result result) {
				if(result.opCode != HttpResponseCode.OK){
					return;
				}
				
				try{
					JSONArray jsonArray = new JSONArray(result.values.get("result"));
					if(null==jsonArray || 0==jsonArray.length()){
						return;
					}
					//获取成功的操作
					List<ContactInfo> contactFriendsList = new ArrayList<ContactInfo>(jsonArray.length());
					JSONObject jsonObj = null;
					ContactInfo obj = null;
					for (int i = 0; i < jsonArray.length(); i++) {
						jsonObj = new JSONObject(jsonArray.get(i).toString());
						obj = new ContactInfo();
						obj.mGroupId = jsonObj.getInt("gid");
						obj.mAccount = jsonObj.getString("account");
						obj.mRealName = jsonObj.getString("name");
						obj.mRemark = jsonObj.getString("remark");
						obj.mAddress = jsonObj.getString("address");
						obj.mUserType = jsonObj.getInt("type");
						obj.mMobile = jsonObj.getString("mobile");
						obj.mEmail = jsonObj.getString("email");
						obj.mSex = jsonObj.getInt("sex");
						obj.mInfoServerVersion = jsonObj.getInt("info_version");
						obj.mAvatarServerVersion = jsonObj.getInt("avatar_version");
						obj.mInfoLastGetTime = System.currentTimeMillis();
						
						contactFriendsList.add(obj);
					}
					
					//将好友信息批量插入到好友表中
					mContactsDao.batchInsertFriends(contactFriendsList);
					// 向用户表中插入用户信息
					mPersonalManager.batchInsertContactInfos(contactFriendsList);
					if(null != mUiHandler){
						mUiHandler.sendEmptyMessage(ContactUiMessage.SYNC_FRIEND_INFOS);
					}
				}catch(Exception e){
				}
			}
			
			@Override
			public void onThreadProgress(Progress progress) {
			}
		};
		RKCloudDemo.kit.execute(request);
	}

	/**
	 * 操作分组信息
	 * @param type操作类型
	 * @param gname分组名称
	 * @param gid分组id
	 */
	public void operationGroupInfo(final int type,final String gname, final int gid){
		String session= AccountManager.getInstance().getSession();
		if(TextUtils.isEmpty(session)){
			return;
		}
		Request request = new Request(HttpType.OPERATION_GROUPS, RKCloudDemo.kit.getHttpHost(HttpTools.HTTPHOST_TYPE_ROOT), HttpTools.OPERATIPN_GROUP_URL);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("ss", session);
		params.put("type", type + "");
		if(Constants.OPERATION_GROUP_ADD == type){//添加分组
			params.put("gname", gname);
		}else if(Constants.OPERATION_GROUP_MODIFY == type){//修改分组名称
			params.put("gname", gname);
			params.put("gid", gid + "");
		}else if(Constants.OPERATION_GROUP_DELETE == type){//删除分组
			params.put("gid", gid + "");
		}
		request.params = params;
		request.mHttpCallback = new HttpCallback() {
			@Override
			public void onThreadResponse(Result result) {
				int reGid = gid;
				if(result.opCode == HttpResponseCode.OK){
					switch(type){
					case Constants.OPERATION_GROUP_ADD:
						JSONObject objAdd;
						try {
							objAdd = new JSONObject(result.values.get("result"));
							int groupId = objAdd.getInt("gid");
							mContactsDao.insertGroup(groupId, gname);
							reGid = groupId;
						} catch (JSONException e) {
							e.printStackTrace();
						}
						break;
						
					case Constants.OPERATION_GROUP_MODIFY:
						mContactsDao.updateGroup(gid, gname);
						break;
						
					case Constants.OPERATION_GROUP_DELETE:
						mContactsDao.deleteGroup(gid);
						break;
					}
				}
				if(null != mUiHandler){
					Message msg = mUiHandler.obtainMessage();
					msg.what = ContactUiMessage.RESPONSE_OPERATION_GROUP;
					msg.obj = type;
					msg.arg1 = result.opCode;
					msg.arg2 = reGid;
					msg.sendToTarget();
				}
			}
			
			@Override
			public void onThreadProgress(Progress progress) {
			}
		};
		RKCloudDemo.kit.execute(request);
	}
	
	/**
	 * 添加好友
	 * @param id
	 * @param content
	 */
	public void addFriend(final String account, final String content){
		String session= AccountManager.getInstance().getSession();
		if(TextUtils.isEmpty(session)){
			return;
		}
		Request request = new Request(HttpType.ADD_FRIENDS, RKCloudDemo.kit.getHttpHost(HttpTools.HTTPHOST_TYPE_ROOT), HttpTools.ADD_FRIENDS_URL);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("ss", session);
		params.put("account", account);
		params.put("content", content);
		request.params = params;
		request.mHttpCallback = new HttpCallback() {
			@Override
			public void onThreadResponse(Result result) {
				switch(result.opCode){
				case HttpResponseCode.OK:
					mContactsDao.insertFriend(0, account);
					// 生成会话及消息
					LocalMessage receivedMsgObj = LocalMessage.buildReceivedMsg(account, RKCloudDemo.context.getString(R.string.friendnotify_msg_hasfriend), account);
					addLocalMsg(receivedMsgObj, true);
					break;
					
				case HttpResponseCode.ADDFRIEND_WAITVERIFY:
					FriendNotify notifyObj = new FriendNotify();
					notifyObj.account = account;
					notifyObj.content = content;
					notifyObj.type = Constants.MESSGE_TYPE_ADD_CONRIFM;
					notifyObj.status = Constants.FRIEND_NOTIFY_STATUS_WAITVERIFY;
					notifyObj.readStatus = Constants.FRIEND_NOTIFY_READ_STATUS_YES;
					mContactNotifyDao.addFriendNotify(notifyObj);
					break;
				}
				
				if(null != mUiHandler){
					Message msg = mUiHandler.obtainMessage();
					msg.what = ContactUiMessage.RESPONSE_ADDFRIEND;
					msg.arg1 = result.opCode;
					msg.obj = account;
					msg.sendToTarget();
				}
			}
			
			@Override
			public void onThreadProgress(Progress progress) {
			}
		};
		RKCloudDemo.kit.execute(request);
	}
	
	public void showAddFriendVerfiyWin(final RKCloudChatBaseActivity activity, final String account){
		String curraccount = AccountManager.getInstance().getAccount();
		String currName = mPersonalManager.getContactInfo(curraccount).mRealName;
		String hintName = TextUtils.isEmpty(currName) ? curraccount : currName;
		
		final EditText groupNameET = new EditText(activity);	
		groupNameET.setBackgroundResource(R.drawable.rkcloud_chat_edittext_bg);
		groupNameET.setSingleLine();
		groupNameET.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
		InputFilter filter = new InputFilter.LengthFilter(50);
		groupNameET.setFilters(new InputFilter[] {filter});
		groupNameET.setHint(R.string.friendnotify_needverify_hint);
		groupNameET.setText(RKCloudDemo.context.getString(R.string.contact_add_friend_hintinfo, hintName));
		groupNameET.setCursorVisible(true);
		groupNameET.setSelected(true);
		
		final RKCloudChatCustomDialog.Builder dialog = new RKCloudChatCustomDialog.Builder(activity);
		dialog.setTitle(R.string.friendnotify_needverify_title);									
		dialog.setNegativeButton(R.string.rkcloud_chat_btn_cancel, null);				
		dialog.setPositiveButton(R.string.rkcloud_chat_btn_confirm, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				activity.showProgressDialog();
				addFriend(account, groupNameET.getText().toString());
			}
		});		

		dialog.addContentView(groupNameET);		
		dialog.create().show();	
		// 设置输入框内容改变事件
		dialog.setPositiveButtonEnabled(true);
		groupNameET.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				dialog.setPositiveButtonEnabled(TextUtils.isEmpty(arg0.toString().trim()) ? false : true);
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			}

			@Override
			public void afterTextChanged(Editable arg0) {
			}
		});			
	}
	
	public void deleteFriend(final String account){
		String session= AccountManager.getInstance().getSession();
		if(TextUtils.isEmpty(session)){
			return;
		}
		Request request = new Request(HttpType.DELETE_FRIEND, RKCloudDemo.kit.getHttpHost(HttpTools.HTTPHOST_TYPE_ROOT), HttpTools.DELETE_FRIEND_URL);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("ss", session);
		params.put("account", account);
		request.params = params;
		request.mHttpCallback = new HttpCallback() {
			@Override
			public void onThreadResponse(Result result) {
				if(result.opCode == HttpResponseCode.OK){
					RKCloudChatMmsManager.getInstance(RKCloudDemo.context).delChat(account, true);//删除聊天记录
					mContactNotifyDao.delFriendsNotify(account);// 删除通知内容
					deleteContactFriends(account);// 删除好友
				}
				if(null != mUiHandler){
					Message msg = mUiHandler.obtainMessage();
					msg.what = ContactUiMessage.RESPONSE_DELFRIEND;
					msg.arg1 = result.opCode;
					msg.obj = account;
					msg.sendToTarget();
				}
			}
			
			@Override
			public void onThreadProgress(Progress progress) {
			}
		};
		RKCloudDemo.kit.execute(request);
	}
	
	/**
	 * 修改好友分组
	 * @param accounts
	 * @param groupId
	 */
	public void modifyFriendsGroup(final List<String> accounts, final int groupId){
		String session= AccountManager.getInstance().getSession();
		if(TextUtils.isEmpty(session)){
			return;
		}
		StringBuffer sb = new StringBuffer();
		for(String account : accounts){
			sb.append(account).append(",");
		}
		sb.deleteCharAt(sb.length()-1);
		
		Request request = new Request(HttpType.MODIFY_FRIENDS_GROUP, RKCloudDemo.kit.getHttpHost(HttpTools.HTTPHOST_TYPE_ROOT), HttpTools.MODIFY_FRIENDS_GROUP_URL);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("ss", session);
		params.put("accounts", sb.toString());
		params.put("gid", groupId + "");
		request.params = params;
		request.mHttpCallback = new HttpCallback() {
			@Override
			public void onThreadResponse(Result result) {
				if(result.opCode == HttpResponseCode.OK){
					mContactsDao.updateGroupIdByAccounts(groupId, accounts);
				}
				if(null != mUiHandler){
					Message msg = mUiHandler.obtainMessage();
					msg.what = ContactUiMessage.RESPONSE_MODIFY_FRIENDS_GROUP;
					msg.arg1 = result.opCode;
					msg.obj = accounts;
					msg.sendToTarget();
				}
			}
			
			@Override
			public void onThreadProgress(Progress progress) {
			}
		};
		RKCloudDemo.kit.execute(request);
		
	}
	
	/**
	 * 修改好友信息
	 * @param account 好友account
	 * @param remark好友备注
	 */
	public void modifyFriendInfo(final String account, final String remark){
		String session= AccountManager.getInstance().getSession();
		if(TextUtils.isEmpty(session)){
			return;
		}
		Request request = new Request(HttpType.MODIFY_FRIEND_INFO, RKCloudDemo.kit.getHttpHost(HttpTools.HTTPHOST_TYPE_ROOT), HttpTools.MODIFY_FRIEND_INFO_URL);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("ss", session);
		params.put("account", account);
		params.put("remark", remark);
		request.params = params;
		request.mHttpCallback = new HttpCallback() {
			@Override
			public void onThreadResponse(Result result) {
				if(result.opCode == HttpResponseCode.OK){
					mContactsDao.modifyFriendRemark(account, remark);
				}
				if(null != mUiHandler){
					Message msg = mUiHandler.obtainMessage();
					msg.what = ContactUiMessage.RESPONSE_MODIFY_FRIENDINFO;
					msg.arg1 = result.opCode;
					msg.obj = account;
					msg.sendToTarget();
				}
			}
			
			@Override
			public void onThreadProgress(Progress progress) {
			}
		};
		RKCloudDemo.kit.execute(request);
	}
				
	/**
	 * 确认加为好友
	 */
	public void confirmAddFriend(final String account){
		String session= AccountManager.getInstance().getSession();
		if(TextUtils.isEmpty(session)){
			return;
		}
		Request request = new Request(HttpType.CONFIRM_ADD_FRIEND, RKCloudDemo.kit.getHttpHost(HttpTools.HTTPHOST_TYPE_ROOT), HttpTools.CONFIRM_ADD_FRIEND_URL);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("ss", session);
		params.put("account", account);
		request.params = params;
		request.mHttpCallback = new HttpCallback() {
			@Override
			public void onThreadResponse(Result result) {
				if(result.opCode == HttpResponseCode.OK){
					// 添加好友
					mContactsDao.insertFriend(0, account);
					// 修改通知状态
					FriendNotify notify = new FriendNotify();
					notify.account = account;
					notify.status = Constants.FRIEND_NOTIFY_STATUS_HAS_FRIEND;
					updateFriendNotify(notify);
					// 生成会话及消息
					LocalMessage sendMsgObj = LocalMessage.buildSendMsg(account, RKCloudDemo.context.getString(R.string.friendnotify_msg_hasfriend), RKCloud.getUserName());
					addLocalMsg(sendMsgObj, false);
				}
				
				if(null != mUiHandler){
					Message msg = mUiHandler.obtainMessage();
					msg.what = ContactUiMessage.RESPONSE_CONFIRM_ADDFRIEND;
					msg.arg1 = result.opCode;
					msg.obj = account;
					msg.sendToTarget();
				}
			}
			
			@Override
			public void onThreadProgress(Progress progress) {
			}
		};
		RKCloudDemo.kit.execute(request);
	}
	
	/**
	 * 搜索通讯录数据
	 * @param account用户名
	 */
	public void searchContactInfo(String filterContent){
		String session= AccountManager.getInstance().getSession();
		if(TextUtils.isEmpty(session)){
			return;
		}
		Request request = new Request(HttpType.SEARCH_CONTACT_INFO, RKCloudDemo.kit.getHttpHost(HttpTools.HTTPHOST_TYPE_ROOT), HttpTools.SEARCH_CONTACT_INFO_URL);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("ss", session);
		params.put("filter", filterContent);
		request.params = params;
		request.mHttpCallback = new HttpCallback() {
			@Override
			public void onThreadResponse(Result result) {
				if(null != mUiHandler){
					Message msg = mUiHandler.obtainMessage();
					msg.what = ContactUiMessage.SEARCH_CONTACT_INFO;
					msg.obj = result;
					msg.sendToTarget();
				}
			}
			
			@Override
			public void onThreadProgress(Progress progress) {
			}
		};
		RKCloudDemo.kit.execute(request);
	}	
	//////////////与api有关的交互 end/////////////////////////////////////////
	
	private void addLocalMsg(LocalMessage msgObj, boolean sendNotify){
		msgObj.setExtension(RKCloudChatConstants.FLAG_ADD_FRIEND_SUCCESS);
		
		RKCloudChatMmsManager mmsManager = RKCloudChatMmsManager.getInstance(RKCloudDemo.context);
		mmsManager.addLocalMsg(msgObj, SingleChat.class);
		if(sendNotify){
			mmsManager.onReceivedMsg(mmsManager.queryChat(msgObj.getChatId()), msgObj);
		}
	}
}