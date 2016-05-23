package com.rongkecloud.chat.demo.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.rongkecloud.chat.RKCloudChatMessageManager;
import com.rongkecloud.chat.demo.entity.RKCloudChatSelectFileItem;
import com.rongkecloud.chat.demo.tools.RKCloudChatTools;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.test.R;
import com.rongkecloud.test.ui.widget.RoundedImageView;

/**
 * 管理sd卡中所有文件的类
 */
public class RKCloudChatSelectFileActivity extends RKCloudChatBaseActivity {
	static final String INTENT_RETURN_KEY_FILEPATH = "intent_return_filepath";
			
	// UI元素
	private TextView mFilePathTV;
	private ListView mListView;
	
	// 成员变量
	private File mSdCardRootDirectory;// SD卡根目录
	private File mCurrDirectory;// 当前目录
	
	private List<RKCloudChatSelectFileItem> mDatas = new ArrayList<RKCloudChatSelectFileItem>();	
	private RKCloudChatSelectFileAdapter mAdapter; 
	private long mFileSizeLimit;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.rkcloud_chat_selectfile);
		// 设置title
		TextView titleTV = (TextView)findViewById(R.id.txt_title);
		titleTV.setText(R.string.rkcloud_chat_selectfile_title);
		titleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		titleTV.setOnClickListener(mExitListener);
		// 初始化UI组件
		mFilePathTV = (TextView) findViewById(R.id.filepath);
		mListView = (ListView)findViewById(R.id.list);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				String selectFile = mDatas.get(arg2).getText();
				
				if (selectFile.equals(getString(R.string.rkcloud_chat_selectfile_return))) {
					// 返回父级目录
					if (mSdCardRootDirectory.equals(mCurrDirectory)) {
						RKCloudChatTools.showToastText(RKCloudChatSelectFileActivity.this, getString(R.string.rkcloud_chat_selectfile_rootdir));
					} else {
						browseToDirectory(mCurrDirectory.getParentFile());
					}
					
				} else {
					File file = new File(String.format("%s/%s", mCurrDirectory.getAbsolutePath(), selectFile));
					if (null != file){
						browseToDirectory(file);
					}
				}
			}
		});
		
		initData();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:// 返回键
			if(!mSdCardRootDirectory.equals(mCurrDirectory)) {
				browseToDirectory(mCurrDirectory.getParentFile());
				return true;
			}
			break;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private void initData(){
		mFileSizeLimit = RKCloudChatMessageManager.getInstance(this).getMediaMmsMaxSize();
		// 初始化变量
		mDatas = new ArrayList<RKCloudChatSelectFileItem>();
		mAdapter = new RKCloudChatSelectFileAdapter();
		mListView.setAdapter(mAdapter);
		
		// 浏览SD卡根目录内容
		if (!Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
			RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_sdcard_unvalid));
			return;
		}
		
		mSdCardRootDirectory = Environment.getExternalStorageDirectory();
		browseToDirectory(mSdCardRootDirectory);
	}
	
	/*
	 * 浏览指定目录下面的内容
	 */
	private void browseToDirectory(File file) {
		mFilePathTV.setText(file.getAbsolutePath());
		
		if (file.isDirectory()) {
			// 目录下的文件
			List<File> files = Arrays.asList(file.listFiles());
			if (files == null) {
				// 用户没有权限访问这个文件，弹出toast提示用户错误
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_selectfile_nopermission));
				return;
			}
			mCurrDirectory = file;
			Collections.sort(files, new Comparator<File>(){
			    @Override
			    public int compare(File o1, File o2) {
				    if(o1.isDirectory() && o2.isFile()){
				        return -1;
				    }else if(o1.isFile() && o2.isDirectory()){
				        return 1;
				    }else{
				    	return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
				    }
			    }
			});
			
			refreshDatas(files);
			
		} else {
			// 用户没有权限
			if (null==file || !file.canWrite()) {
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_selectfile_nopermission));
				return;
			}
			// 空文件
			if (0 == file.length()) {
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_selectfile_emptyfile));
				return;
			}
			// 文件大小超出限制时，提示用户
			long size = RKCloudChatTools.getFileSize(file.getAbsolutePath());
			if (size > mFileSizeLimit) {
				RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_selectfile_beyond_filesize, mFileSizeLimit/1024/1024));
				return;
			}
			
			// 返回选中的文件
			Intent intent = new Intent();
			intent.putExtra(INTENT_RETURN_KEY_FILEPATH, file.getAbsolutePath());
			setResult(RESULT_OK, intent);
			finish();
		}
	}

	/*
	 * 将获得的文件加载到listview中
	 */
	private void refreshDatas(List<File> files) {
		mDatas.clear();	
		if (!mCurrDirectory.equals(mSdCardRootDirectory) && null != mCurrDirectory.getParent()) {
			// 添加返回上一级目录的操作项
			RKCloudChatSelectFileItem iconifiedText = new RKCloudChatSelectFileItem(getString(R.string.rkcloud_chat_selectfile_return), R.drawable.rkcloud_chat_img_return);
			iconifiedText.setBackItem(true);
			mDatas.add(iconifiedText);
		}
		
		// 记录文件名称
		List<RKCloudChatSelectFileItem> fileDatas = new ArrayList<RKCloudChatSelectFileItem>();
		for (File currentFile : files) {
			if (currentFile.isDirectory()) {
				mDatas.add(new RKCloudChatSelectFileItem(currentFile.getName(), R.drawable.rkcloud_chat_img_selectfile_folder));
			} else {
				fileDatas.add(new RKCloudChatSelectFileItem(currentFile.getName(), R.drawable.rkcloud_chat_img_selectfile_file));
			}
		}
				
		mDatas.addAll(fileDatas);
		mAdapter.notifyDataSetChanged();
	}
	
	@Override
	public void processResult(Message arg0) {
	}
	
	
	private class RKCloudChatSelectFileAdapter extends BaseAdapter {
		private ItemViewBuffer mItemBuffer;
		
		public RKCloudChatSelectFileAdapter() {
		}
		
		private class ItemViewBuffer {
			RoundedImageView fileIcon;
			TextView fileName;
			public ItemViewBuffer(View convertView) {
				fileIcon = (RoundedImageView) convertView.findViewById(R.id.file_icon);
				fileName = (TextView) convertView.findViewById(R.id.file_name);
			}
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
				convertView = getLayoutInflater().inflate(R.layout.rkcloud_chat_selectfile_item, null);
				mItemBuffer = new ItemViewBuffer(convertView);
				convertView.setTag(mItemBuffer);
			} else {
				mItemBuffer = (ItemViewBuffer) convertView.getTag();
			}
			
			mItemBuffer.fileIcon.setBackgroundResource(mDatas.get(position).getIcon());
			mItemBuffer.fileName.setText(mDatas.get(position).getText());
			
			return convertView;
		}
	}
}
