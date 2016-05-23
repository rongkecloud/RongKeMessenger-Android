package com.rongkecloud.test.db;

import java.io.File;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.rongkecloud.test.db.table.ContactFriendsColumns;
import com.rongkecloud.test.db.table.ContactGroupColumns;
import com.rongkecloud.test.db.table.FriendsNotifyColumns;
import com.rongkecloud.test.db.table.PersonInfoColumns;
import com.rongkecloud.test.system.RKCloudDemo;
import com.rongkecloud.test.utility.Print;

/**
 * 创建数据库，打开和关闭数据库以及数据库的更新升级等操作 目前数据库一直处于打开状态并未关闭
 */
public class RKCloudDemoDb {
	private static final String TAG = RKCloudDemoDb.class.getSimpleName();

	public static final String DATABASE_NAME = "RKCloudDb";
	// 版本1：创建聊天相关的表
	// 版本2：
	private static final int DATABASE_VERSION = 2;

	private SQLiteDatabase mSqlLiteDb;
	private DatabaseHelper mDbHelper;
	private static RKCloudDemoDb mRkCloudDb;

	/*
	 * 打开数据库的操作。
	 * 在某些情况下我们会关闭数据库，所以我们先判断数据库是否被关闭了， 如果关闭则重新打开，否则直接使用数据库
	 */
	private RKCloudDemoDb(Context context) {
		// 数据库存在并处于打开状态那么直接使用
		if (null!=mSqlLiteDb && mSqlLiteDb.isOpen()) {
			return;
		}
		
		try {			
			if (null == mDbHelper) {	
				Print.w(TAG, "DatabaseHelper instanse");
				mDbHelper = new DatabaseHelper(context);
			}
			mSqlLiteDb = mDbHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			Print.w(TAG, "get getWritableDatabase error. info=" + e.getMessage());
			mSqlLiteDb = mDbHelper.getReadableDatabase();
		}
	}

	/**
	 * 创建单例实例
	 * @param 
	 * @return 
	 */
	public synchronized static RKCloudDemoDb getInstance() {
		Print.d(TAG, String.format("execute RKCloudDb getInstance() method %s", System.currentTimeMillis()));
		if (null == mRkCloudDb) {
			mRkCloudDb = new RKCloudDemoDb(RKCloudDemo.context);
		}
		return mRkCloudDb;
	}

	/**
	 * 获得database操作数据库的实例
	 */
	public SQLiteDatabase getSqlDateBase() {
		return mSqlLiteDb;
	}

	/**
	 * 关闭数据库的操作
	 */
	public void close() {
		if(mSqlLiteDb!=null)
			mSqlLiteDb.close();
		if(mDbHelper!=null)
			mDbHelper.close();
	}
	
	private void destoryDb(){
		mSqlLiteDb = null;
		mDbHelper = null;
	}
	
	private static void destory() {
		if(mRkCloudDb != null){
			mRkCloudDb.close();
			mRkCloudDb.destoryDb();
			mRkCloudDb = null;
		}
	}
	
	/**
	 * 1、将当前的db文件关闭；
	 * 2、重命名；
	 * 3、重新创建db实例。
	 * 注意：后期要对相关的manager实例中的db实例重新初始化。
	 * @param lastUid
	 * @param loginUid
	 * @return
	 */
	public synchronized static boolean exchangeDBFile(long lastUid, long loginUid) {
		boolean ret = false;
		try {
			File currDBFile = RKCloudDemo.context.getDatabasePath(RKCloudDemoDb.DATABASE_NAME);
			RKCloudDemoDb.destory();
			
			// 将当前的DB文件重命名为lastUID的文件
			File lastDBFile = new File(currDBFile.getAbsolutePath() +"_"+ lastUid);
			try {
				if(lastDBFile.exists())
					lastDBFile.delete();
			} catch (Exception e) {
			}
			boolean re = currDBFile.renameTo(lastDBFile);
			Print.i(TAG, "A: rename currDBFile to lastDBFile<"+lastDBFile.toString() + ">("+re+")");

			// 如果存在和登陆UID相同的DB文件，则重命名该文件为currDBFile
			File loginUserDBFile = new File(currDBFile.getAbsolutePath() +"_"+ loginUid);
			try {
				if(loginUserDBFile.exists()){
					re = loginUserDBFile.renameTo(currDBFile);
					Print.i(TAG, "B: rename loginUserDBFile<" +loginUserDBFile.toString() +"> to currDBFile"+ "("+re+")");
				}
			} catch (Exception e) {
			}
			
			ret = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// 更新系统的数据库相关变量
			RKCloudDemoDb.getInstance();
		}
		return ret;
	}

	@Override
	protected void finalize() throws Throwable {
		Print.i(TAG, "DB CLOSE");
		close();
		super.finalize();
	}

	/*
	 * 创建内部类DatabaseHelper：创建数据库，穿件表，更新表功能
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		private File mDbFile;

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mDbFile = context.getDatabasePath(DATABASE_NAME);
		}

		@Override
		public synchronized SQLiteDatabase getWritableDatabase() {
			SQLiteDatabase db;
			if (mDbFile.exists()) {
				db = SQLiteDatabase.openDatabase(mDbFile.toString(), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
				// 当前的使用的数据库的版本(为升级之前的)，目前我们只考虑数据库版本的升级不考虑回滚的情况
				int version = db.getVersion();
				Print.d(TAG, String.format("old database version is %s", version));
				if (version != DATABASE_VERSION) {
					if (0 == version) {
						onCreate(db);
					} else {							
						onUpgrade(db, version, DATABASE_VERSION);
					}
				}
			} else {
				db = super.getWritableDatabase();
			}
			return db;
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.beginTransaction();
			try{
				db.execSQL(ContactFriendsColumns.SQL_TABLE_CREATE);
				db.execSQL(ContactGroupColumns.SQL_TABLE_CREATE);
				db.execSQL(PersonInfoColumns.SQL_TABLE_CREATE);
				db.execSQL(FriendsNotifyColumns.SQL_TABLE_CREATE);
				db.setVersion(DATABASE_VERSION);
				db.setTransactionSuccessful();
				
			}catch(Exception e){
				
			}finally{
				db.endTransaction();
			}
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}
}
