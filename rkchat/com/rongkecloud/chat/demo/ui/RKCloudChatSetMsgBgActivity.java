package com.rongkecloud.chat.demo.ui;

import java.io.File;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rongkecloud.chat.demo.RKCloudChatConstants;
import com.rongkecloud.chat.demo.RKCloudChatMmsManager;
import com.rongkecloud.chat.demo.RKCloudChatUiHandlerMessage;
import com.rongkecloud.chat.demo.tools.RKCloudChatImageTools;
import com.rongkecloud.chat.demo.tools.RKCloudChatScreenTools;
import com.rongkecloud.chat.demo.tools.RKCloudChatTools;
import com.rongkecloud.chat.demo.ui.base.RKCloudChatBaseActivity;
import com.rongkecloud.test.R;

public class RKCloudChatSetMsgBgActivity extends RKCloudChatBaseActivity implements OnClickListener {
	private static final String TAG = RKCloudChatSetMsgBgActivity.class.getSimpleName();
	
	public static final String INTENT_CHAT_ID = "intent_key_chatid";
	
	// 设置返回结果的key，其内容为boolean类型值
	public static final String INTENT_RETURNKEY_SETIMG_OK = "intent_returnkey_setok";

	private static final int INTENT_RESULT_CHOOSE_PICTURE = 1;// 选择图片
	private static final int INTENT_RESULT_TAKE_PHOTO = 2;// 拍照

	// UI元素
	private RelativeLayout mRootView;
	private LinearLayout mContentLayout;
	private LinearLayout mTakePhotoLayout;
	private LinearLayout mSelectPicLayout;
	private LinearLayout mCancelLayout;

	private LinearLayout mFooterLayout;
	private Button mConfirmBnt;
	private Button mReturnBnt;

	// 成员变量
	private String mChatId = null;
	private String mTakePhotoTempName = null;// 记录拍照时的图片名称
	private String mBgFileName;// 背景图片的名称
	private RKCloudChatMmsManager mMmsManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mChatId = getIntent().getStringExtra(INTENT_CHAT_ID);
		if (TextUtils.isEmpty(mChatId)) {
			finish();
		}

		setContentView(R.layout.rkcloud_chat_setmsgbg);
		// 初始化UI视图
		TextView titleTV = (TextView)findViewById(R.id.txt_title);
		titleTV.setText(getString(R.string.bnt_return));
		titleTV.setCompoundDrawablesWithIntrinsicBounds(R.drawable.rkcloud_chat_img_back, 0, 0, 0);
		titleTV.setOnClickListener(mExitListener);

        TextView text_title_content = (TextView)findViewById(R.id.text_title_content);
        text_title_content.setText(getString(R.string.rkcloud_chat_setmsgbg_title));

		mRootView = (RelativeLayout) findViewById(R.id.root);
		mContentLayout = (LinearLayout) findViewById(R.id.content);
		mTakePhotoLayout = (LinearLayout) findViewById(R.id.layout_takephoto);
		mSelectPicLayout = (LinearLayout) findViewById(R.id.layout_pic);
		mCancelLayout = (LinearLayout) findViewById(R.id.layout_cancel);
		mFooterLayout = (LinearLayout) findViewById(R.id.footer);
		mConfirmBnt = (Button) findViewById(R.id.confirm);
		mReturnBnt = (Button) findViewById(R.id.returnbnt);

		mTakePhotoLayout.setOnClickListener(this);
		mSelectPicLayout.setOnClickListener(this);
		mCancelLayout.setOnClickListener(this);
		mConfirmBnt.setOnClickListener(this);
		mReturnBnt.setOnClickListener(this);

		showOperFunction(true);
		
		mMmsManager = RKCloudChatMmsManager.getInstance(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mMmsManager.bindUiHandler(mUiHandler);
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// 拍照时保存图片名称
		if (!TextUtils.isEmpty(mTakePhotoTempName)) {
			outState.putString("img", mTakePhotoTempName);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// 恢复时如果包含img字符串，表示要获取拍照时保存的图片名称
		if (savedInstanceState.containsKey("img")) {
			mTakePhotoTempName = savedInstanceState.getString("img");
			savedInstanceState.remove("img");
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.layout_takephoto:
			// 创建目录，并生成临时图片名称
			RKCloudChatTools.createDirectory(RKCloudChatConstants.MMS_TEMP_PATH);
			mTakePhotoTempName = String.format(Locale.getDefault(), "%stakephoto_%d.jpg", RKCloudChatConstants.MMS_TEMP_PATH, System.currentTimeMillis());
			File image = new File(mTakePhotoTempName);

			Intent intent2 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			Uri imageUri = Uri.fromFile(image);
			intent2.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
			startActivityForResult(intent2, INTENT_RESULT_TAKE_PHOTO);
			break;

		case R.id.layout_pic:
			Intent intent = new Intent(Intent.ACTION_PICK);
			intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
			startActivityForResult(intent, INTENT_RESULT_CHOOSE_PICTURE);
			break;
			
		case R.id.layout_cancel:
			setMsgBgImg("");
			break;

		case R.id.confirm:
			setMsgBgImg(mBgFileName);
			break;

		case R.id.returnbnt:
			// 显示确认对话框
			AlertDialog.Builder dialog = new AlertDialog.Builder(this)
					.setTitle(R.string.rkcloud_chat_tip)
					.setMessage(R.string.rkcloud_chat_setmsgbg_cancel_confirm)
					.setNegativeButton(R.string.rkcloud_chat_btn_cancel, null)
					.setPositiveButton(R.string.rkcloud_chat_btn_confirm, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mRootView.setBackgroundResource(R.color.rkcloud_chat_ui_bgcolor);
							showOperFunction(true);
						}
					});
			dialog.show();
			break;
		}
	}

	@Override
	protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (RESULT_OK != resultCode) {
			return;
		}

		switch (requestCode) {
		case INTENT_RESULT_CHOOSE_PICTURE:// 选择本地图片
		case INTENT_RESULT_TAKE_PHOTO: // 拍照之后获取图片
			// 获取图片路径
			final String imgPath = (requestCode==INTENT_RESULT_CHOOSE_PICTURE ? RKCloudChatTools.getChoosePicturePath(this, data.getData()) : mTakePhotoTempName);
			mTakePhotoTempName = null;// 设置为null 以免对下次的路径造成影响
			// 如果图片路径为空，或者图片不存在，则结束
			if (TextUtils.isEmpty(imgPath) || !new File(imgPath).exists()) {
				mUiHandler.sendEmptyMessage(RKCloudChatUiHandlerMessage.IMAGE_CONTENT_UNNORMAL);
				return;
			}
			
			// 处理图片时显示等待对话框
			showProgressDialog();
			mMmsManager.processPhoto(mChatId, RKCloudChatConstants.MMS_TEMP_PATH, imgPath, requestCode==INTENT_RESULT_TAKE_PHOTO);
			break;
		}
	}

	/*
	 * 设置背景
	 * @param imgpath
	 * @param commendImg 是否为推荐的背景图片，true:推荐的图片 false:从相册或拍照中取得
	 */
	private void setMsgBgImg(String imgpath) {
		RKCloudChatMmsManager.getInstance(this).updateChatBgImg(mChatId, imgpath);
		Intent backIntent = new Intent();
		backIntent.putExtra(INTENT_RETURNKEY_SETIMG_OK, true);
		setResult(RESULT_OK, backIntent);
		finish();
	}

	/*
	 * 显示选择背景的操作功能
	 */
	private void showOperFunction(boolean isShow) {
		if (isShow) {
			mContentLayout.setVisibility(View.VISIBLE);
			mFooterLayout.setVisibility(View.GONE);
		} else {
			mContentLayout.setVisibility(View.GONE);
			mFooterLayout.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void processResult(Message msg) {
		closeProgressDialog();

		switch (msg.what) {
		case RKCloudChatUiHandlerMessage.SDCARD_NOT_EXIST:
		case RKCloudChatUiHandlerMessage.SDCARD_ERROR:
			showOperFunction(true);
			RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_sdcard_unvalid));
			break;

		case RKCloudChatUiHandlerMessage.IMAGE_COMPRESS_FAILED:
			showOperFunction(true);
			RKCloudChatTools.showToastText(this, getString(R.string.rkcloud_chat_resize_image_failed));
			break;

		case RKCloudChatUiHandlerMessage.IMAGE_COMPRESS_SUCCESS:
			String content = (String)msg.obj;
			String[] contents = !TextUtils.isEmpty(content) ? content.split(",") : null;
			if(null!=contents && 2==contents.length && mChatId.equalsIgnoreCase(contents[0])){
				showOperFunction(false);
				Bitmap map = RKCloudChatImageTools.resizeBitmap(contents[1], RKCloudChatScreenTools.getInstance(this).getScreenWidth(), RKCloudChatScreenTools.getInstance(this).getScreenHeight());
				Drawable bgRes = new BitmapDrawable(getResources(), map);
				if (null != bgRes) {
					mRootView.setBackgroundDrawable(bgRes);
					mBgFileName = contents[1];
				}
			}
			break;
		}
	}
}
