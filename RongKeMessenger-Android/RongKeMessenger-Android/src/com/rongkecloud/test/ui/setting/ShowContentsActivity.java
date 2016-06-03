package com.rongkecloud.test.ui.setting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.test.R;
import com.rongkecloud.test.db.RKCloudDemoDb;
import com.rongkecloud.test.entity.Constants;
import com.rongkecloud.test.manager.SettingManager;

public class ShowContentsActivity extends RKCloudChatBaseActivity{
	public static final String KEY_TYPE = "show_type";
	public static final int KEY_TYPE_DB = 0;
	public static final int KEY_TYPE_SP = 1;
		
	// UI组件
	private TextView mContentView;
	private Button mExport;
	// 成员变量
	
	private String mContent;
	private int mShowType;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.showcontents);
		
		mShowType = getIntent().getIntExtra(KEY_TYPE, KEY_TYPE_DB);
		
		// 初始化UI组件
		mContentView = (TextView) findViewById(R.id.showdbcontent);
		mExport = (Button) findViewById(R.id.export);
		
		if(KEY_TYPE_DB == mShowType){
			mContent = SettingManager.getInstance().getTableContents();
		}else{
			mContent = SettingManager.getInstance().getSPContents();
		}
		mContentView.setText(mContent);
		
		mExport.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(KEY_TYPE_DB == mShowType){
					exportDB();
				}else{
					exportSP();
				}
			}
		});
	}

	private void exportDB(){
		String filePath = String.format("%s%s.db", Constants.ROOT_PATH, RKCloudDemoDb.DATABASE_NAME);
		try {
			FileInputStream fin = new FileInputStream(getDatabasePath(RKCloudDemoDb.DATABASE_NAME));
			FileOutputStream fos = new FileOutputStream(filePath);
			byte[] buffer = new byte[4089];
			int len = -1;
			while ((len = fin.read(buffer)) > 0) {
				fos.write(buffer, 0, len);
			}
			fos.flush();
			fin.close();
			fos.close();
			Toast.makeText(this, "export success.", Toast.LENGTH_SHORT).show();
			mExport.setEnabled(false);
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "export failed.", Toast.LENGTH_SHORT).show();
		}
	}

	private void exportSP() {
		String filePath = String.format("%ssp.txt", Constants.ROOT_PATH);
		try {
			File file = new File(filePath);
			File parentDir = file.getParentFile();
			if (!parentDir.exists()) {
				parentDir.mkdirs();
			}
			
			// 文件不存在时创建，存在时清空内容
			if (file.exists()) {
				file.delete();
			}
			file.createNewFile();

			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
			out.write(mContent);
			out.close();
			Toast.makeText(this, "export success.", Toast.LENGTH_SHORT).show();
			mExport.setEnabled(false);
		} catch (Exception e) {
			Toast.makeText(this, "export failed.", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}

	@Override
	public void processResult(Message msg) {
	}
}
