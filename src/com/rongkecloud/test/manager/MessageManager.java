package com.rongkecloud.test.manager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.rongkecloud.chat.LocalMessage;
import com.rongkecloud.chat.SingleChat;
import com.rongkecloud.chat.demo.RKCloudChatConstants;
import com.rongkecloud.chat.demo.RKCloudChatMmsManager;
import com.rongkecloud.sdkbase.interfaces.RKCloudReceivedUserDefinedMsgCallBack;
import com.rongkecloud.test.R;
import com.rongkecloud.test.entity.Constants;
import com.rongkecloud.test.entity.ContactInfo;
import com.rongkecloud.test.entity.FriendNotify;
import com.rongkecloud.test.manager.uihandlermsg.ContactUiMessage;
import com.rongkecloud.test.system.ConfigKey;
import com.rongkecloud.test.system.RKCloudDemo;

public class MessageManager implements RKCloudReceivedUserDefinedMsgCallBack{
	
	private static MessageManager mInstance = null;
	private Handler mUiHandler;
	private ContactManager mContactManager;
	private MessageManager(){
		mContactManager = ContactManager.getInstance();
	}
	
	public static MessageManager getInstance(){
		if(null == mInstance){
			mInstance = new MessageManager();
		}
		return mInstance;
	}	
	
	public void bindUiHandler(Handler handler) {
		mUiHandler = handler;
	}

	@Override
	public void onReceivedUserDefinedMsg(String sender, String content, long time) {

        String rkcloudAccount = RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, null);
        if(!TextUtils.isEmpty(sender) && sender.equals(rkcloudAccount))
        {
            //不接收同账号发送过来的消息
            return;
        }


		if(content.startsWith(Constants.MESSGE_TYPE_ADD_REQUEST)){ // 添加好友请求
			FriendNotify friendNotify = new FriendNotify();
			friendNotify.account = sender;
			friendNotify.type = Constants.MESSGE_TYPE_ADD_REQUEST;
			if(content.length() > Constants.MESSGE_TYPE_ADD_REQUEST.length()){
				friendNotify.content = content.substring(content.indexOf(Constants.MESSGE_TYPE_ADD_REQUEST)+Constants.MESSGE_TYPE_ADD_REQUEST.length()+1);
			}
			friendNotify.status = Constants.FRIEND_NOTIFY_STATUS_VERIFY;
			friendNotify.readStatus = Constants.FRIEND_NOTIFY_READ_STATUS_NO;			
			mContactManager.insertFriendsNotify(friendNotify);
			
			if(null != mUiHandler){
				Message msg = mUiHandler.obtainMessage();
				msg.what = ContactUiMessage.RECEIVED_FRIEND_ADDREQUEST;
				msg.sendToTarget();
			}
			
		}else if(content.startsWith(Constants.MESSGE_TYPE_ADD_CONRIFM)){ // 确认添加为好友
			mContactManager.insertGroupFriend(0, sender);
			FriendNotify notify = new FriendNotify();
			notify.account = sender;
			notify.status = Constants.FRIEND_NOTIFY_STATUS_HAS_FRIEND;
			String mIsActivited = content.substring(content.indexOf(Constants.MESSGE_TYPE_ADD_CONRIFM)+Constants.MESSGE_TYPE_ADD_CONRIFM.length()+1);
			if(Constants.ADD_FRIEND_TYPE_ISACTIVITED.equals(mIsActivited)){ // other click confirm btn
				// 生成会话及消息
				LocalMessage receivedMsgObj = LocalMessage.buildReceivedMsg(sender, RKCloudDemo.context.getString(R.string.friendnotify_msg_hasfriend), sender);
				receivedMsgObj.setMsgTime(time);
				addLocalMsg(receivedMsgObj, true);
				mContactManager.updateFriendNotify(notify);
			}else{
				// 生成会话及消息
				LocalMessage sendMsgObj = LocalMessage.buildSendMsg(sender, RKCloudDemo.context.getString(R.string.friendnotify_msg_hasfriend), RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, null));
				addLocalMsg(sendMsgObj, false);
				notify.readStatus = Constants.FRIEND_NOTIFY_READ_STATUS_NO;
				notify.type = Constants.MESSGE_TYPE_ADD_CONRIFM;
				mContactManager.insertFriendsNotify(notify);
			}
			
			if(null != mUiHandler){
				Message msg = mUiHandler.obtainMessage();
				msg.what = ContactUiMessage.RECEIVED_FRIEND_ADDCONFIRM;
				msg.arg1 = 0;
				msg.obj = sender;
				msg.sendToTarget();
			}
			
		}else if(content.startsWith(Constants.MESSGE_TYPE_DELETE_FRIEND)){ // 删除好友
			//删除聊天记录
			RKCloudChatMmsManager.getInstance(RKCloudDemo.context).delChat(sender, true);
			//删除好友信息
			mContactManager.deleteContactFriends(sender);
			//删除通知信息
			mContactManager.delFriendsNotify(sender);
			if(null != mUiHandler){
				Message msg = mUiHandler.obtainMessage();
				msg.what = ContactUiMessage.RECEIVED_FRIEND_DELETED;
				msg.obj = sender;
				msg.sendToTarget();
			}
		}	
		
		// 同步好友信息
		ContactInfo info = PersonalManager.getInstance().getContactInfo(sender);
		if(null==info || System.currentTimeMillis()-info.mInfoLastGetTime >= Constants.GET_PERSONAL_INFO_TIME){
			PersonalManager.getInstance().syncUserInfo(sender);
		}
	}

	@Override
	public void onReceivedUserDefinedMsgs(String arg0) {
		if(TextUtils.isEmpty(arg0)){
			return;
		}
		try {
			JSONArray ja = new JSONArray(arg0);
			for(int i = 0; i < ja.length(); i++){
				JSONObject jo = new JSONObject(ja.get(i).toString());
				onReceivedUserDefinedMsg(jo.getString("sender"), jo.getString("content"), jo.getLong("time"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private void addLocalMsg(LocalMessage msgObj, boolean sendNotify){
		msgObj.setExtension(RKCloudChatConstants.FLAG_ADD_FRIEND_SUCCESS);
		
		RKCloudChatMmsManager mmsManager = RKCloudChatMmsManager.getInstance(RKCloudDemo.context);
		mmsManager.addLocalMsg(msgObj, SingleChat.class);
		if(sendNotify){
			mmsManager.onReceivedMsg(mmsManager.queryChat(msgObj.getChatId()), msgObj);
		}
	}
}