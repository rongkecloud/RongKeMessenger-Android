package com.rongkecloud.test.ui.contact;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.test.R;
import com.rongkecloud.test.entity.ContactInfo;
import com.rongkecloud.test.http.HttpResponseCode;
import com.rongkecloud.test.http.Result;
import com.rongkecloud.test.manager.ContactManager;
import com.rongkecloud.test.manager.uihandlermsg.ContactUiMessage;
import com.rongkecloud.test.utility.OtherUtilities;
import com.rongkecloud.test.utility.RegularCheckTools;

public class AddFriendsActivity extends RKCloudChatBaseActivity {
	
	private TextView mTitle;
	private EditText mSearchET;
	private TextView mSearchTV;
	private LinearLayout mSearchLayout;
	private ContactManager mContactManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_friend);
		initViews();
		initListeners();
	}
	
	private void initViews(){
		mTitle = (TextView) findViewById(R.id.txt_title);
		mTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		mTitle.setText(getResources().getString(R.string.bnt_return));

        TextView text_title_content = (TextView)findViewById(R.id.text_title_content);
        text_title_content.setText(R.string.contact_add_friend);

		mSearchET = (EditText) findViewById(R.id.search_friend_edittext);
		mSearchTV = (TextView) findViewById(R.id.search_friend_account);
		mSearchLayout = (LinearLayout) findViewById(R.id.search_layout);
		
		mContactManager = ContactManager.getInstance();
	}
	
	private void initListeners(){
		mTitle.setOnClickListener(mExitListener);
		mTitle.setOnClickListener(mExitListener);
		//搜索好友对EditText的实时监听
		mSearchET.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String content = mSearchET.getText().toString().trim();
				mSearchLayout.setVisibility(TextUtils.isEmpty(content) ? View.GONE : View.VISIBLE);
				mSearchTV.setText(content);
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			
			}
			
			@Override
			public void afterTextChanged(Editable s) {
			
			}
		});
		
		mSearchLayout.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				String content = mSearchET.getText().toString().trim();
				if(TextUtils.isEmpty(content) || !RegularCheckTools.checkAccount2(content)){
					OtherUtilities.showToastText(AddFriendsActivity.this, getString(R.string.contact_search_cond_format));
					mSearchET.setFocusable(true);
					return;
				}
				
				showProgressDialog();
				mContactManager.searchContactInfo(content);
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mContactManager.bindUiHandler(mUiHandler);
	}
	
	@Override
	public void processResult(Message msg) {
		switch (msg.what) {
		case ContactUiMessage.SEARCH_CONTACT_INFO:
			closeProgressDialog();
			Result result = (Result) msg.obj;
			if(HttpResponseCode.OK == result.opCode){
				//查询通讯录信息成功后的处理
				List<ContactInfo> mList = new ArrayList<ContactInfo>();
				
				try {
					JSONArray jsonArray = new JSONArray(result.values.get("result"));
					if(0 == jsonArray.length()){
						OtherUtilities.showToastText(AddFriendsActivity.this, getString(R.string.contact_search_no_friend));
						return;
					}
					// 获取好友分组
					Map<String, Integer> friends = mContactManager.getGroupIdByAccounts();
					ContactInfo info;
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject obj = new JSONObject(jsonArray.get(i).toString());
						info = new ContactInfo();
						info.mAccount = obj.getString("account");
						info.mGroupId = friends.containsKey(info.mAccount) ? friends.get(info.mAccount) : -1;
						mList.add(info);
					}
					if(null != mList && 0 != mList.size()){
						Intent intent = new Intent(this,AddFriendsListActivity.class);
						intent.putParcelableArrayListExtra(AddFriendsListActivity.INTENT_KEY_USER, (ArrayList<ContactInfo>) mList);
						startActivity(intent);
					}else{
						OtherUtilities.showToastText(AddFriendsActivity.this, getString(R.string.contact_search_friend_exist));
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}else if(HttpResponseCode.NO_NETWORK == result.opCode){
				OtherUtilities.showToastText(AddFriendsActivity.this, getString(R.string.network_off));
			}else{
				OtherUtilities.showToastText(AddFriendsActivity.this, getString(R.string.operation_failed));
			}
			break;
		}
	}
}
