package com.rongkecloud.test.http;

/**
 * 账号致命异常的回调接口，如：重复登录或被禁等
 */
public interface HttpFatalExceptionCallBack{	
	/**
	 * 账号异常的回调处理
	 * @param errorType 错误类型 1：重复登录 2：账号被禁
	 * @return
	 */
	public void onHttpFatalException(int errorType);
}
