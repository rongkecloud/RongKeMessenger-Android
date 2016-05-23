package com.rongkecloud.test.system;

import android.app.Application;

import com.rongkecloud.test.utility.CrashHandler;
import com.rongkecloud.test.utility.FileLog;

public class App extends Application{
    private static final String TAG = App.class.getSimpleName();
    
    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.getInstance(this).init();	
        FileLog.d(TAG, "App--onCreate--enter");
		RKCloudDemo.create(this);
    }
    
    @Override
    public void onLowMemory() {
    	super.onLowMemory();
    	FileLog.d(TAG, "App--onLowMemory--enter");
    }
    
    @Override
    public void onTerminate() {
    	super.onTerminate();
    	FileLog.d(TAG, "App--onTerminate--enter");
    }
}