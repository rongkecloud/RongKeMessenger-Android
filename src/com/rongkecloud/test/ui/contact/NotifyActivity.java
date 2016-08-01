package com.rongkecloud.test.ui.contact;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.test.R;
import com.rongkecloud.test.entity.Constants;
import com.rongkecloud.test.entity.FriendNotify;
import com.rongkecloud.test.http.HttpResponseCode;
import com.rongkecloud.test.manager.ContactManager;
import com.rongkecloud.test.manager.MessageManager;
import com.rongkecloud.test.manager.uihandlermsg.ContactUiMessage;
import com.rongkecloud.test.utility.OtherUtilities;

public class NotifyActivity extends RKCloudChatBaseActivity{
	// 上下文菜单定义
	private static final int CONTEXT_MENU_DELETE_NOTIFY = 1;// 删除单个通知
	
	// UI对象
	private ListView mListView; // 列表对象
	private TextView mNoDataTV;// 无数据时的显示
	private Button mClearBnt;// 清空按钮
	// 成员属性
	private List<FriendNotify> mNotifyDatas;
	private NotifyAdapter mAdapter;
	private ContactManager mContactManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.friend_notify_list);
		initUI();
		mContactManager = ContactManager.getInstance();
		mNotifyDatas = new ArrayList<FriendNotify>();
		mAdapter = new NotifyAdapter();
		mListView.setAdapter(mAdapter);
		// 给列表注册上下文菜单
		registerForContextMenu(mListView);
		initData();
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				//列表条目的点击事件，跳转到详细信息界面
				FriendNotify obj = mNotifyDatas.get(arg2);
				Intent intent = new Intent(NotifyActivity.this, ContactDetailInfoActivity.class);
				intent.putExtra(ContactDetailInfoActivity.INTENT_CONTACT_ACCOUNT, obj.account);
				startActivity(intent);
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mContactManager.bindUiHandler(mUiHandler);	
		MessageManager.getInstance().bindUiHandler(mUiHandler);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		closeContextMenu();		
		// 更新为已读状态
		mContactManager.updateNotifyReaded();
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		FriendNotify obj = mNotifyDatas.get(info.position);
		menu.setHeaderTitle(obj.getDisplayName());
		menu.add(0, CONTEXT_MENU_DELETE_NOTIFY, 1, R.string.friendnotify_context_delete);
		
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
		FriendNotify notifyObj = mNotifyDatas.get(info.position);
		switch (item.getItemId()) {
		case CONTEXT_MENU_DELETE_NOTIFY:
			mContactManager.delFriendsNotify(notifyObj.account, notifyObj.type);
			mNotifyDatas.remove(info.position);
			mAdapter.notifyDataSetChanged();
			break;
		}
		return super.onContextItemSelected(item);
	}
	
	private void initUI(){
		// 头部title栏的设置
		TextView titleTV = (TextView)findViewById(R.id.txt_title);
		titleTV.setText(R.string.bnt_return);
		titleTV.setOnClickListener(mExitListener);
		titleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);

        TextView text_title_content = (TextView)findViewById(R.id.text_title_content);
        text_title_content.setText(R.string.friendnotify_title);

        // 清空按钮
		mClearBnt = (Button)findViewById(R.id.title_right_btn);
//        mClearBnt.setTextColor(getResources().getColor(R.color.title_right_btn_text_color));
        mClearBnt.setBackgroundColor(getResources().getColor(R.color.bg_transparent));
		mClearBnt.setVisibility(View.VISIBLE);
		mClearBnt.setText(getString(R.string.bnt_clear));
		mClearBnt.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				clearAllData();							
			}
		});
		// 页面组件元素
		mListView = (ListView)findViewById(R.id.list);
		mNoDataTV = (TextView) findViewById(R.id.empty);
	}
	
	private void initData(){
		mNotifyDatas.clear();
		mNotifyDatas.addAll(mContactManager.queryAllNotifys());
		mAdapter.notifyDataSetChanged();
		mClearBnt.setEnabled(mNotifyDatas.size() > 0);
		mNoDataTV.setVisibility(mNotifyDatas.size() > 0 ? View.GONE : View.VISIBLE);
	}
	
	/*
	 * 清空通知
	 */
	private void clearAllData(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.friendnotify_clear_confirm_title));
		builder.setMessage(getString(R.string.friendnotify_clear_confirm_content));
		builder.setPositiveButton(R.string.bnt_confirm, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showProgressDialog();
				new Thread(new Runnable() {
					@Override
					public void run() {
						long result = mContactManager.delAllFriendsNotify();
						Message msg = mUiHandler.obtainMessage();
						msg.what = ContactUiMessage.CLEAR_FRIEND_NOTIFY_FINISHED;
						msg.obj = result;
						msg.sendToTarget();	
					}							
				}).start();				
			}			
		}).setNegativeButton(R.string.bnt_cancel, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();					
			}				
		}).create().show();		
	}
	
	@Override
	public void processResult(Message msg) {		
		switch(msg.what){
		case ContactUiMessage.CLEAR_FRIEND_NOTIFY_FINISHED:// 清空
			closeProgressDialog();
			if((Long)msg.obj > 0){
				mNotifyDatas.clear();
				mAdapter.notifyDataSetChanged();
				mClearBnt.setEnabled(false);
			}else{
				OtherUtilities.showToastText(this, getString(R.string.friendnotify_clear_failed));
			}
			break;
			
		case ContactUiMessage.RESPONSE_CONFIRM_ADDFRIEND:
			closeProgressDialog();
			if(HttpResponseCode.OK == msg.arg1){
				initData();
			}else if(msg.arg1 == HttpResponseCode.NO_NETWORK){
				OtherUtilities.showToastText(this, getString(R.string.network_off));
			}else{
				OtherUtilities.showToastText(this, getString(R.string.friendnotify_confirmadd_failed));
			}
			break;
			
		case ContactUiMessage.RECEIVED_FRIEND_ADDREQUEST:	
		case ContactUiMessage.RECEIVED_FRIEND_ADDCONFIRM:	
		case ContactUiMessage.RECEIVED_FRIEND_DELETED:
			initData();
			break;
		}
	}
	
	private class NotifyAdapter extends BaseAdapter{
		private ItemViewBuffer mItemBuffer;
		
		public NotifyAdapter(){
		}
		
		@Override
		public int getCount() {		
			return mNotifyDatas.size();
		}

		@Override
		public Object getItem(int position) {		
			return mNotifyDatas.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (null == convertView) {
				convertView = getLayoutInflater().inflate(R.layout.friend_notify_list_item, null);
				mItemBuffer = new ItemViewBuffer(convertView);
				convertView.setTag(mItemBuffer);
			}else{
				mItemBuffer = (ItemViewBuffer)convertView.getTag();
			}
			final FriendNotify obj = mNotifyDatas.get(position);
			// 设置名称
			mItemBuffer.nameTV.setText(obj.getDisplayName());
			// 操作说明
			mItemBuffer.opeBnt.setVisibility(View.GONE);
			mItemBuffer.opeTV.setVisibility(View.GONE);
			if(obj.type.equals(Constants.MESSGE_TYPE_ADD_REQUEST)){
				if(obj.status == Constants.FRIEND_NOTIFY_STATUS_VERIFY){ // 通过验证
					mItemBuffer.opeBnt.setVisibility(View.VISIBLE);			
					mItemBuffer.opeBnt.setText(getString(R.string.friendnotify_ope_confirmadd));
					
				}else if(obj.status == Constants.FRIEND_NOTIFY_STATUS_HAS_FRIEND){ // 已添加
					mItemBuffer.opeTV.setVisibility(View.VISIBLE);
					mItemBuffer.opeTV.setText(getString(R.string.contact_has_been_friend));				
				}
				
			}else if(obj.type.equals(Constants.MESSGE_TYPE_ADD_CONRIFM)){
				if(obj.status == Constants.FRIEND_NOTIFY_STATUS_WAITVERIFY){ // 等待验证
					mItemBuffer.opeTV.setVisibility(View.VISIBLE);
					mItemBuffer.opeTV.setText(getString(R.string.friendnotify_ope_waitverify));
				}else if(obj.status == Constants.FRIEND_NOTIFY_STATUS_ADD){ // 加为好友
					mItemBuffer.opeBnt.setVisibility(View.VISIBLE);			
					mItemBuffer.opeBnt.setText(getString(R.string.friendnotify_ope_addfriend));
				}else if(obj.status == Constants.FRIEND_NOTIFY_STATUS_HAS_FRIEND){ // 已添加
					mItemBuffer.opeTV.setVisibility(View.VISIBLE);
					mItemBuffer.opeTV.setText(getString(R.string.contact_has_been_friend));
				}
			}
			if(!TextUtils.isEmpty(obj.content)){
				mItemBuffer.tipTV.setText(obj.content);
				mItemBuffer.tipTV.setVisibility(View.VISIBLE);
			}else{
				mItemBuffer.tipTV.setVisibility(View.GONE);
			}
			
			
			// 设置按钮的监听事件
			if(mItemBuffer.opeBnt.getVisibility() == View.VISIBLE){
				mItemBuffer.opeBnt.setOnClickListener(new OnClickListener() {				
					@Override
					public void onClick(View v) {
						if(obj.status == Constants.FRIEND_NOTIFY_STATUS_VERIFY){
							showProgressDialog();		
							mContactManager.confirmAddFriend(obj.account);	
						}					
					}
				});
			}
			return convertView;
		}
		
		private class ItemViewBuffer{
			TextView nameTV;
			TextView tipTV;
			TextView opeTV;
			Button opeBnt;
			
			ItemViewBuffer(View view){			
				nameTV = (TextView)view.findViewById(R.id.name);
				tipTV = (TextView) view.findViewById(R.id.tip);
				opeTV = (TextView)view.findViewById(R.id.openote); 
				opeBnt = (Button)view.findViewById(R.id.bnt); 
			}
		}
	}
}
