package com.rongkecloud.test.system;

import java.io.File;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;

import com.rongkecloud.test.utility.Print;

/**
 * 定义所有访问sharedpreference的方法
 */
public class Config {
	private final static String TAG = Config.class.getSimpleName();

	// 存放sp信息的文件名
	public static final String CONFIG_FILE = "rongkeclound_sp";
	private SharedPreferences mSp = null;

	public Config(Context context) {
		mSp = context.getSharedPreferences(CONFIG_FILE, 0);
	}
	
	public void clear() {
		mSp.edit().clear().commit();
	}

	public void remove(String key) {
		mSp.edit().remove(key).commit();
	}
	
	public void removeAll(){
		Set<String> keys = mSp.getAll().keySet();
		for(String key: keys){
			mSp.edit().remove(key).commit();
		}		
	}
	
	public boolean put(String key, boolean value) {
		return mSp.edit().putBoolean(key, value).commit();
	}

	public boolean put(String key, int value) {
		return mSp.edit().putInt(key, value).commit();
	}

	public boolean put(String key, long value) {
		return mSp.edit().putLong(key, value).commit();
	}

	public boolean put(String key, float value) {
		return mSp.edit().putFloat(key, value).commit();
	}

	public boolean put(String key, String value) {
		return mSp.edit().putString(key, value).commit();
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		return mSp.getBoolean(key, defaultValue);
	}

	public int getInt(String key, int defaultValue) {
		return mSp.getInt(key, defaultValue);
	}

	public long getLong(String key, long defaultValue) {
		return mSp.getLong(key, defaultValue);
	}

	public float getFloat(String key, float defaultValue) {
		return mSp.getFloat(key, defaultValue);
	}

	public String getString(String key, String defaultValue) {
		return mSp.getString(key, defaultValue);
	}

	public boolean upgradeSP() {
		if (getBoolean(ConfigKey.UPGRADE_AP, false))
			return false;

		Set<String> keys = mSp.getAll().keySet();

		put(ConfigKey.UPGRADE_AP, true);
		return true;
	}
	
	
	/**
	 * 返回程序的配置文件的绝对路径
	 * /data/data/com.rongkeclound/shared_prefs/rongkeclound_sp
	 * @return
	 */
	public String getSPFilePathNoSuffix(){
		if(null == RKCloudDemo.context){
			return "/data/data/com.rongkeclound/shared_prefs/"+CONFIG_FILE;
		}
		
		StringBuilder sb = new StringBuilder().append("/data/data/").append(RKCloudDemo.context.getPackageName()).append("/shared_prefs/").append(CONFIG_FILE);
		return sb.toString();
	}
	
	/**
	 * 更改/data/data/com.rongkeclound/shared_prefs/rongkeclound_sp.xml文件
	 * 如果本次登陆的用户(loginUid)和上一次登陆的用户(lastUid)不同，则将：
	 * 1：当前的配置文件 rongkeclound_sp.xml 重命名为rongkeclound_sp_lastUid.xml 文件；
	 * 2：如果存在 rongkeclound_sp_loginUid.xml文件，则将其 重命名为vehicle_sp.xml
	 * @return ： 成功返回true，否则为false
	 */
	public boolean exchangeSPFile(long lastUid, long loginUid) {
		boolean ret = false;
		try {
			String spStr = getSPFilePathNoSuffix();
			File currFile = new File(spStr + ".xml");
			
			// 将当前的sp文件重命名为lastUID的文件
			File lastSPFile = new File(spStr +"_" + lastUid + ".xml");
			try {
				if(lastSPFile.exists())
					lastSPFile.delete();
			} catch (Exception e) {
			}
			boolean re = currFile.renameTo(lastSPFile);
			Print.i(TAG, "A: rename currSPFile to lastSPFile<"+lastSPFile.toString() + ">("+re+")");

			// 如果存在和登陆UID相同的sp文件，则重命名该文件为currFile
			File loginUserSPFile = new File(spStr +"_" + loginUid + ".xml");
			try {
				if(loginUserSPFile.exists()){
					re = loginUserSPFile.renameTo(currFile);
					Print.i(TAG, "B: rename loginUserSPFile<" +loginUserSPFile.toString() +"> to currSPFile"+ "("+re+")");
				}
			} catch (Exception e) {
			}
			
			ret = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// 更新系统的静态配置变量
			RKCloudDemo.config = new Config(RKCloudDemo.context);
		}
		return ret;
	}
}
