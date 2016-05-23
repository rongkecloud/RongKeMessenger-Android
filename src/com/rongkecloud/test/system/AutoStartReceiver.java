package com.rongkecloud.test.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.rongkecloud.test.utility.FileLog;

public class AutoStartReceiver extends BroadcastReceiver {
	private String TAG = AutoStartReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context ctx, Intent intent) {
		FileLog.d(TAG, "onReceive--action:"+intent.getAction());
	}
}
