package com.rongkecloud.test.http;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpHost;

import android.content.Context;

public class HttpTools implements HttpApi{
	// http host类型定义
	public static final String HTTPHOST_TYPE_ROOT = "root";
	
	private static HttpTools mInstance;	
	private static HttpKit mKit = null;	
	
	private Map<String, HttpHost> mHosts = null;
	
	private HttpTools(Context context){
		mKit = HttpKit.getInstance(context);
		mHosts = new HashMap<String, HttpHost>();
		mHosts.put(HTTPHOST_TYPE_ROOT, new HttpHost(ROOT_HOST_NAME, ROOT_HOST_PORT));
	}
	
	public static HttpTools getInstance(Context context){
		if(null == mInstance){
			mInstance = new HttpTools(context);
		}
		return mInstance;
	}
	
	/**
	 * 根据类型获取host
	 * @param type
	 * @return
	 */
	public HttpHost getHttpHost(String type){
		return mHosts.get(type);
	}
	
	public void setOnHttpFatalException(HttpFatalExceptionCallBack callBack){
		mKit.setOnHttpFatalException(callBack);
	}
	
	/**
	 * 退出时清空相关的请求
	 */
	public void destroyClient() {
		// 除root 之外清除其余请求
		Iterator<Map.Entry<String, HttpHost>> hosts = mHosts.entrySet().iterator();
		Map.Entry<String, HttpHost> host = null;
		while(hosts.hasNext()){
			host = hosts.next();
			if(host.getKey().equals(HTTPHOST_TYPE_ROOT)){
				continue;
			}
			hosts.remove();
		}
		
		mKit.clearHttpRequest();
	}
	
	/**
	 * 发起请求
	 * @param req
	 */
	public void execute(Request req){
		mKit.execute(req);
	}
	
	public void abortHttpRequest(String requestId){
		mKit.abortHttpRequest(requestId);
	}
}
