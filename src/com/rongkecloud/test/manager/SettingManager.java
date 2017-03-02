package com.rongkecloud.test.manager;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpHost;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageAsyncLoader;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest.IMAGE_REQUEST_TYPE;
import com.rongkecloud.test.db.dao.TestTablesDao;
import com.rongkecloud.test.db.table.PersonInfoColumns;
import com.rongkecloud.test.entity.Constants;
import com.rongkecloud.test.http.HttpCallback;
import com.rongkecloud.test.http.HttpResponseCode;
import com.rongkecloud.test.http.HttpTools;
import com.rongkecloud.test.http.HttpType;
import com.rongkecloud.test.http.Progress;
import com.rongkecloud.test.http.Request;
import com.rongkecloud.test.http.Result;
import com.rongkecloud.test.manager.uihandlermsg.SettingUiMessage;
import com.rongkecloud.test.system.Config;
import com.rongkecloud.test.system.ConfigKey;
import com.rongkecloud.test.system.RKCloudDemo;
import com.rongkecloud.test.utility.ImageUtil;
import com.rongkecloud.test.utility.SystemInfo;

public class SettingManager{
	
	private static SettingManager mInstance = null;
	private PersonalManager mPersonalManager;
	private Handler mUiHandler;
	private Handler mMainUiHandler;
	
	private SettingManager(){
		mPersonalManager = PersonalManager.getInstance();
	}
	
	public static SettingManager getInstance(){
		if(null == mInstance){
			mInstance = new SettingManager();
		}
		return mInstance;
	}	
	
	public void bindUiHandler(Handler handler) {
		mUiHandler = handler;
	}
	
	public void bindMainUiHandler(Handler handler) {
		mMainUiHandler = handler;
	}
	
	//////////////////////////////////////////////////db操作 begin////////////////////////////////////////////////
	
	//////////////////////////////////////////////////db操作 end////////////////////////////////////////////////

	//操作个人信息
	public void modifySelfInfo(final String key, final String content){
		String session= AccountManager.getInstance().getSession();
		if(TextUtils.isEmpty(session)){
			return;
		}
		Request request = new Request(HttpType.OPERATION_PERSONAL_INFO, RKCloudDemo.kit.getHttpHost(HttpTools.HTTPHOST_TYPE_ROOT), HttpTools.OPERATION_PERSONAL_INFO_URL);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("ss", session);
		params.put("key", key);
		params.put("content", content);
		request.params = params;
		request.mHttpCallback = new HttpCallback() {			
			@Override
			public void onThreadResponse(Result result) {
				String account = RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "");
				if (HttpResponseCode.OK == result.opCode) { 
					String values = result.values.get("result");
					try {
						JSONObject jo = new JSONObject(values);
						int infoVer = jo.getInt("info_version");
						ContentValues cv = new ContentValues();
						if("name".equals(key)){
							cv.put(PersonInfoColumns.NAME, content);
						}else if("sex".equals(key)){
							cv.put(PersonInfoColumns.SEX, content);
						}else if("address".equals(key)){
							cv.put(PersonInfoColumns.ADDRESS, content);
						}else if("mobile".equals(key)){
							cv.put(PersonInfoColumns.MOBILE, content);
						}else if("email".equals(key)){
							cv.put(PersonInfoColumns.EMAIL, content);
						}else if("permission".equals(key)){
							RKCloudDemo.config.put(ConfigKey.LOGIN_ADD_FRIEND_PERMISSION, Integer.valueOf(content));
						}
						
						cv.put(PersonInfoColumns.INFO_SERVER_VERSION, infoVer);
						cv.put(PersonInfoColumns.INFO_CLIENT_VERSION, infoVer);
						cv.put(PersonInfoColumns.INFO_SYNC_LASTTIME, System.currentTimeMillis());
						mPersonalManager.updateContactInfo(account, cv);
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
				}
				if(null != mUiHandler){
					Message msg = mUiHandler.obtainMessage();
					msg.what = SettingUiMessage.RESPONSE_MODIFY_SELFINFO;
					msg.arg1 = result.opCode;
					msg.sendToTarget();
				}
			}
			
			@Override
			public void onThreadProgress(Progress progress) {
			}
		};
		RKCloudDemo.kit.execute(request);
	}
	
	//上传头像
	public void uploadAvatar(final String filePath){
		String session= AccountManager.getInstance().getSession();
		if(TextUtils.isEmpty(session)){
			return;
		}
		Request request = new Request(HttpType.UPLOAD_PERSONAL_AVATAR, RKCloudDemo.kit.getHttpHost(HttpTools.HTTPHOST_TYPE_ROOT), HttpTools.UPLOAD_PERSONAL_AVATAR_URL);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("ss", session);
		request.files = new HashMap<String, File>();
		request.files.put("file", new File(filePath));
		request.params = params;
		request.mHttpCallback = new HttpCallback() {			
			@Override
			public void onThreadResponse(Result result) {
				if (HttpResponseCode.OK == result.opCode) {
					String values = result.values.get("result");
					try {
						JSONObject jo = new JSONObject(values);
						int avatarVer = jo.getInt("avatar_version");
						
						String currLoginAccount = RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, "");	
						String avatarPath = String.format("%savatars/%s/%s_%d", Constants.ROOT_PATH, currLoginAccount, currLoginAccount, avatarVer);
						String thumbPath = String.format("%savatars/%s/%s_%d_thumb", Constants.ROOT_PATH, currLoginAccount, currLoginAccount, avatarVer);
						
						File avatarFile = new File(avatarPath);
						File thumbFile = new File(thumbPath);
						if(!avatarFile.getParentFile().exists()){
							avatarFile.getParentFile().mkdirs();
						}
						if(!thumbFile.getParentFile().exists()){
							thumbFile.getParentFile().mkdirs();
						}
						
						Bitmap bitmap = BitmapFactory.decodeFile(filePath);//大图
						Bitmap thumbBitmap = ImageUtil.resizeBitmapForce(filePath, 120, 120);// 120*120//缩略图
						
						ImageUtil.saveBitmap(bitmap, avatarPath);
						ImageUtil.saveBitmap(thumbBitmap, thumbPath);
						
						ContentValues cv = new ContentValues();
						cv.put(PersonInfoColumns.AVATAR_PATH, avatarPath);
						cv.put(PersonInfoColumns.AVATAR_THUMB, thumbPath);
						cv.put(PersonInfoColumns.AVATAR_SERVER_VERSION, avatarVer);
						cv.put(PersonInfoColumns.AVATAR_CLIENT_THUMB_VERSION, avatarVer);
						cv.put(PersonInfoColumns.AVATAR_CLIENT_VERSION, avatarVer);
						mPersonalManager.updateContactInfo(RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, ""), cv);
					} catch (Exception e) {
						e.printStackTrace();
					} 
				}
				// 去除缓存头像
				RKCloudChatImageAsyncLoader.getInstance(RKCloudDemo.context).removeImagesByType(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG, RKCloudDemo.config.getString(ConfigKey.LOGIN_NAME, null));
				if(null != mUiHandler){
					Message msg = mUiHandler.obtainMessage();
					msg.what = SettingUiMessage.RESPONSE_UPLOAD_AVATAR;
					msg.arg1 = result.opCode;
					msg.sendToTarget();
				}
			}
			
			@Override
			public void onThreadProgress(Progress progress) {
			}
		};
		RKCloudDemo.kit.execute(request);
	}
	
	//添加意见反馈
	public void addFeedback(int type, String content){
		String session= AccountManager.getInstance().getSession();
		if(TextUtils.isEmpty(session)){
			return;
		}
		Request request = new Request(HttpType.ADD_FEEDBACK, RKCloudDemo.kit.getHttpHost(HttpTools.HTTPHOST_TYPE_ROOT), HttpTools.ADD_FEEDBACK_URL);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("ss", session);
		params.put("type", type+"");
		params.put("content", content);
		request.params = params;
		request.mHttpCallback = new HttpCallback() {			
			@Override
			public void onThreadResponse(Result result) {
				if(null != mUiHandler){
					Message msg = mUiHandler.obtainMessage();
					msg.what = SettingUiMessage.RESPONSE_ADD_FEEDBACK;
					msg.arg1 = result.opCode;
					msg.sendToTarget();
				}
			}
			
			@Override
			public void onThreadProgress(Progress progress) {
			}
		};
		RKCloudDemo.kit.execute(request);
	}
	
	//修改密码
	public void modifyPwd(String oldpwd,final String newpwd){
		String session= AccountManager.getInstance().getSession();
		if(TextUtils.isEmpty(session)){
			return;
		}
		Request request = new Request(HttpType.MODIFY_PWD, RKCloudDemo.kit.getHttpHost(HttpTools.HTTPHOST_TYPE_ROOT), HttpTools.MODIFY_PWD_URL);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("ss", session);
		params.put("oldpwd", oldpwd);
		params.put("newpwd", newpwd);
		request.params = params;
		request.mHttpCallback = new HttpCallback() {			
			@Override
			public void onThreadResponse(Result result) {
				if(HttpResponseCode.OK == result.opCode){
					RKCloudDemo.config.put(ConfigKey.LOGIN_PWD, newpwd);
				}
				if(null != mUiHandler){
					Message msg = mUiHandler.obtainMessage();
					msg.what = SettingUiMessage.RESPONSE_MODIFY_PWD;
					msg.arg1 = result.opCode;
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
	 * 检查版本更新
	 * */
	public void checkUpdate(final boolean autoCheck){
		String session = AccountManager.getInstance().getSession();
		if(TextUtils.isEmpty(session)){
			return;
		}
		Request request = new Request(HttpType.CHECK_UPDATE, RKCloudDemo.kit.getHttpHost(HttpTools.HTTPHOST_TYPE_ROOT), HttpTools.CHECK_UPDATE_URL);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("os", SystemInfo.getOsName());
		params.put("cv", SystemInfo.getClientVersion());
		request.params = params;
		request.mHttpCallback = new HttpCallback() {			
			@Override
			public void onThreadResponse(Result result) {
				Map<String, String> values = new HashMap<String, String>();
				values.put("autoCheck", autoCheck ? "1" : "0");
				
				if(HttpResponseCode.OK == result.opCode){
					String url = result.values.get("download_url");
					String updateVersion = result.values.get("update_version");
					String desc = result.values.get("update_description");
					String fileName = result.values.get("file_name");
					long fileSize = Long.parseLong(result.values.get("file_size"));
					String minVer = result.values.get("min_version");
					if(!TextUtils.isEmpty(url) && 0 != fileSize){
						values.put("url", url);
						values.put("fileName", fileName);
						values.put("fileSize", String.valueOf(fileSize));
						values.put("desc", desc);
						values.put("updateVer", updateVersion);
						values.put("minVer", minVer);
					}
					
					RKCloudDemo.config.put(ConfigKey.UPGRADE_LASTTIME, System.currentTimeMillis());
				}else if(HttpResponseCode.NO_CHECK_UPDATE == result.opCode){
					RKCloudDemo.config.put(ConfigKey.UPGRADE_LASTTIME, System.currentTimeMillis());
				}
				if(null != mUiHandler){
					Message msg = mUiHandler.obtainMessage();
					msg.what = SettingUiMessage.RESPONSE_CHECK_UPDATE;
					msg.arg1 = result.opCode;
					msg.obj = values;
					msg.sendToTarget();
				}
				if(null != mMainUiHandler){
					Message msg = mMainUiHandler.obtainMessage();
					msg.what = SettingUiMessage.RESPONSE_CHECK_UPDATE;
					msg.arg1 = result.opCode;
					msg.obj = values;
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
	 * 下载apk
	 */
	public void downloadApk(String urlStr, long fileSize, String path) {
		try {
			URL url = new URL(urlStr);
			HttpHost host = new HttpHost(url.getHost(),url.getPort());
			Request req = new Request(HttpType.DOWNLOAD_APK, host, url.getPath());
			req.requestType = Request.RequestType.FILE;
			req.requesterId = Constants.DOWNLOAD_APK;
			req.method = Request.Method.GET;
			req.filePath = path;
			req.mHttpCallback = new HttpCallback() {
				
				@Override
				public void onThreadResponse(Result result) {
					Message msg = mUiHandler.obtainMessage();
					msg.what = SettingUiMessage.RESPONSE_DOWNLOAD_APK;
					msg.arg1 = result.opCode;
					msg.sendToTarget();
				}
				
				@Override
				public void onThreadProgress(Progress progress) {
					Message msg = mUiHandler.obtainMessage();
					msg.what = SettingUiMessage.RESPONSE_UPDATE_DOWNLOAD_PROGRESS;
					msg.obj = progress;
					msg.sendToTarget();
				}
			};
			RKCloudDemo.kit.execute(req);	
		} catch (MalformedURLException e) {
			
		}
	}
	
	/**
	 * 获取表中的数据
	 * @return
	 */
	public String getTableContents(){
		TestTablesDao dao = new TestTablesDao();
		// 获取所有表名
		String[]  allTableNames = dao.getAllTables();
		StringBuffer sb = new StringBuffer();
		int index = 0;
		int length = allTableNames.length;
		for(index=0; index<length; index++){
			if("sqlite_sequence".equals(allTableNames[index]) || "android_metadata".equals(allTableNames[index])){
				continue;
			}
			
			sb.append(dao.getTableInfo(allTableNames[index]));
			if(index != (length-1)){
				sb.append("============================================\r\n");
			}
		}
		
		return sb.toString();
	}
	
	/**
	 * 获取SP中的所有数据
	 * @return
	 */
	public String getSPContents(){
		SharedPreferences sp = RKCloudDemo.context.getSharedPreferences(Config.CONFIG_FILE, 0);
		
		HashMap<String, String> values = (HashMap<String, String>) sp.getAll();
		StringBuffer sb = new StringBuffer();
		for (Iterator iter=values.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			sb.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\r\n");
		}
		return sb.toString();
	}
}