package com.rongkecloud.test.db.dao;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.rongkecloud.test.db.RKCloudDemoDb;
import com.rongkecloud.test.utility.Print;

/**
 * @class TestGetTableContentsDao 获取表的数据
 */
public class TestTablesDao {
	private static final String TAG = TestTablesDao.class.getSimpleName();
	private SQLiteDatabase mDb;

	/**
	 * 构造函数
	 */
	public TestTablesDao() {
		mDb = RKCloudDemoDb.getInstance().getSqlDateBase();
	}

	/**
	 * 获取所有表名称
	 * @return
	 */
	public String[] getAllTables(){
		Cursor c = null;
		String sql = "SELECT name FROM sqlite_master WHERE type='table';";
		String [] tables = null;
		// 查询操作
		try {
			c = mDb.rawQuery(sql, null);
			if (c != null && c.getCount()>0) {
				tables = new String[c.getCount()];
				int index = 0;
				for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext()){
					tables[index++] = c.getString(0);
				}				
			}
		} catch (SQLiteException e) {
			Print.w(TAG, "getAllTables: Query all table name from DB is failed. info=" + e.getMessage());
		} finally {
			// 关闭游标对象
			if (null != c) {
				c.close();
			}
		}
		
		return tables;
	}	
	
	/**
	 * 获取表数据
	 */
	@SuppressLint("NewApi")
	public String getTableInfo(String tablename) {		
		StringBuffer sb = new StringBuffer();
		sb.append("TableName: ").append(tablename).append("\r\n");
		Cursor c = null;
		// 查询操作
		try {
			c = mDb.query(tablename, null, null, null, null, null, null);
			if (c != null) {
				// 显示列名
				String[] columsNames = c.getColumnNames();
				int totalIndex = columsNames.length;// 列名
				int index = 0;// 索引
				for(index=0; index<totalIndex; index++){
					sb.append(columsNames[index]);
					if(index != (totalIndex-1)){
						sb.append("  |  ");
					}
				}
				sb.append("\r\n");
				
				// 显示数据
				if(c.getCount() > 0){
					for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext()){
						for(index=0; index<totalIndex; index++){							
//							if(c.getType(index) != Cursor.FIELD_TYPE_BLOB){
								sb.append(c.getString(index));
								if(index != (totalIndex-1)){
									sb.append("  |  ");
								}
							}
//						}
						sb.append("\r\n");
					}
				}
			}
		} catch (SQLiteException e) {
			Print.w(TAG, "getTableInfo: Query account from DB is failed. info="
					+ e.getMessage());
		} finally {
			// 关闭游标对象
			if (null != c) {
				c.close();
			}
		}

		return sb.toString();
	}
	
}
