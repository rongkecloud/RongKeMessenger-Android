package com.rongkecloud.test.http;


/**
 * 实现异步请求时的服务器回调
 * @author Administrator
 *
 */
public abstract class HttpCallback{
	public abstract void onThreadResponse(Result result);
	public abstract void onThreadProgress(Progress progress);
}
