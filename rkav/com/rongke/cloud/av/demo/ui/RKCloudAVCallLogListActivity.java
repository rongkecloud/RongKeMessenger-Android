package com.rongke.cloud.av.demo.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.rongke.cloud.av.demo.RKCloudAVDemoManager;
import com.rongke.cloud.av.demo.tools.RKCloudAVUtils;
import com.rongkecloud.av.RKCloudAVCallLog;
import com.rongkecloud.test.R;

public class RKCloudAVCallLogListActivity extends Activity{	
	private ListView mListView;
	private ImageButton mRefresh;
	
	private ArrayList<RKCloudAVCallLog> mCallLogDatas;
	private RKCloudAVCallLogAdapter mAdapter;
	private RKCloudAVDemoManager mAVManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rkcloud_av_calllog_list);	
		initUI();
		initData();
	}
	
	private void initUI(){
		mListView = (ListView)findViewById(R.id.callloglist);
		mRefresh = (ImageButton) findViewById(R.id.refresh);
		mRefresh.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				refreshData();
			}
		});
		
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {				
				AlertDialog.Builder builder = new AlertDialog.Builder(RKCloudAVCallLogListActivity.this);
				String[] items = new String[]{
						getString(R.string.rkcloud_av_calllog_context_audiodial)
						,getString(R.string.rkcloud_av_calllog_context_videodial)
						,getString(R.string.rkcloud_av_calllog_context_deletesingle)
						,getString(R.string.rkcloud_av_calllog_context_deleteall)};
				
				builder.setItems(items, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						RKCloudAVCallLog calllog = mCallLogDatas.get(position);
						switch (which) {
						case 0:// 发起语音呼叫
							mAVManager.dial(RKCloudAVCallLogListActivity.this, calllog.getOtherAccount(), false);
							break;
							
						case 1://发起视频通话
							mAVManager.dial(RKCloudAVCallLogListActivity.this, calllog.getOtherAccount(), true);
							break;
							
						case 2:
							//删除一条通话记录
							if(mAVManager.delCallLog(calllog.getCallId())){
								mCallLogDatas.remove(position);
								mAdapter.notifyDataSetChanged();
							}
							break;
						
						case 3:
							// 删除所有通话记录
							new AlertDialog.Builder(RKCloudAVCallLogListActivity.this)
							.setTitle(R.string.rkcloud_av_tip)
							.setMessage(R.string.rkcloud_av_calllog_clear_confirm)
							.setNegativeButton(R.string.rkcloud_av_btn_cancel, null)
							.setPositiveButton(R.string.rkcloud_av_btn_confirm, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									if(mAVManager.clearCallLogs()){
										mCallLogDatas.clear();
										mAdapter.notifyDataSetChanged();
									}
								}
							}).show();		
							break;
						}
					}
				});
				builder.create().show();
			}
		});		
	}
	
	private void initData(){
		mCallLogDatas = new ArrayList<RKCloudAVCallLog>();
		mAdapter = new RKCloudAVCallLogAdapter();				
		mListView.setAdapter(mAdapter);
		
		mAVManager = RKCloudAVDemoManager.getInstance(this);
	}
	
	private void refreshData(){
		List<RKCloudAVCallLog> datas = mAVManager.getAllCallLogs();
		mCallLogDatas.clear();
		if(null!=datas && datas.size()>0){
			mCallLogDatas.addAll(datas);
		}
		mAdapter.notifyDataSetChanged();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		refreshData();
		mAVManager.hideMissedCallNotification();
	}
	
	private class RKCloudAVCallLogAdapter extends BaseAdapter {
		private ItemViewBuffer mItemBuffer;

		public RKCloudAVCallLogAdapter() {				
		}
		
		@Override
		public int getCount() {
			return mCallLogDatas.size();
		}

		@Override
		public Object getItem(int arg0) {
			return mCallLogDatas.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		private class ItemViewBuffer {
			TextView nameTV; 
			TextView callType;
			TextView callTime;
			
			public ItemViewBuffer(View convertView) {
				nameTV = (TextView) convertView.findViewById(R.id.calllogusername);
				callType = (TextView) convertView.findViewById(R.id.calllogtype);
				callTime = (TextView) convertView.findViewById(R.id.calllogtime);
			}
		}

		@Override
		public View getView(int arg0, View convertView, ViewGroup arg2) {
			if (null == convertView) {
				convertView = getLayoutInflater().inflate(R.layout.rkcloud_av_calllog, null);
				mItemBuffer = new ItemViewBuffer(convertView);
				convertView.setTag(mItemBuffer);
			} else {
				mItemBuffer = (ItemViewBuffer) convertView.getTag();
			}

			// 获取会话数据
			RKCloudAVCallLog callLog = mCallLogDatas.get(arg0);
			String userName = callLog.getOtherAccount();
			String type = "";
			switch(callLog.getCallType()){
				case RKCloudAVCallLog.INCOMING_CALL:
					type = getString(R.string.rkcloud_av_calllog_type_in);
					break;
				case RKCloudAVCallLog.OUTGOING_CALL:
					type = getString(R.string.rkcloud_av_calllog_type_out);
					break;
				case RKCloudAVCallLog.MISSED_CALL:
					type = getString(R.string.rkcloud_av_calllog_type_missed);
					break;
			}
			mItemBuffer.callTime.setText(RKCloudAVUtils.getTimeExactMinute(callLog.getCallStartTime()));
			mItemBuffer.nameTV.setText(userName);
			mItemBuffer.callType.setText(type);
			
			return convertView;
		}
	}
}
