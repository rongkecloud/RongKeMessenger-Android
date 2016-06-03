package com.rongkecloud.test.http;

import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.content.Context;

class HttpKit{
	private static final String TAG = HttpKit.class.getSimpleName();	
	private SynRequest mSynRequest;	//异步处理请求类
	private static HttpKit mHttpKit = null;
	private CommonHttpParams mCommonHttpParams = CommonHttpParams.getInstance();
	
	/**
	 * 设置userAgent默认为rk_http(1.0)
	 * @param userAgent
	 */
	protected void setUserAgent(String userAgent){
		mCommonHttpParams.setUserAgent(userAgent);
	}
	
	protected static HttpKit getInstance(Context mContext){
		if(null == mHttpKit){
			mHttpKit = new HttpKit(mContext);
		}
		return mHttpKit;
	}
	
	protected HttpKit(Context context) {
		mSynRequest = SynRequest.getInstance(context);
	}
	
	public void setOnHttpFatalException(HttpFatalExceptionCallBack callBack){
		mSynRequest.setOnHttpFatalException(callBack);
	}
	
	public void clearHttpRequest(){
		mSynRequest.interuptAllRequest();
	}
	
	/*
	 * 执行http请求
	 */
	public void execute(Request req){
		mSynRequest.execute(req);
	}
	
	public void abortHttpRequest(String requestId){
		mSynRequest.abortRequest(requestId);
	}
}

class CommonHttpParams {
	private HttpParams mReqParams = null;
	private static CommonHttpParams mCommonHttpParams = null;
	private CommonHttpParams(){
		mReqParams = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(mReqParams, 120 * 1000);
		HttpConnectionParams.setConnectionTimeout(mReqParams, 10 * 1000);
	}
	
	public static CommonHttpParams getInstance(){
		if(null == mCommonHttpParams){
			mCommonHttpParams = new CommonHttpParams();
		}
		return mCommonHttpParams;
	}
	private String userAgent = "rk_http(1.0)";

	public HttpParams getHttpParams() {
		HttpProtocolParams.setUserAgent(mReqParams, userAgent);
		return mReqParams;
	}
	
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
}
