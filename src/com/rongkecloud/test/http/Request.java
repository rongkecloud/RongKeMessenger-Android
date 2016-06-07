package com.rongkecloud.test.http;

import java.io.File;
import java.util.HashMap;

import org.apache.http.HttpHost;


public class Request {
	public enum Method {
		POST, GET
	}
	
	public enum RequestType{
		VALUE,TEXT,FILE,MESSAGE
	}
	
	public String filePath;// 文件路径及名称
	public int type; // 请求的类型
	public RequestType requestType = RequestType.VALUE;
	public HttpHost host;
	public String url;// 请求地址
	public Method method = Method.POST;
	public HashMap<String, String> params;
	public HashMap<String, File> files;
	public String requesterId;
	
	// ////////用于请求携带额外的数据///////
	public String arg0;
	public String arg1;
	public String arg2;
	public Object obj;
	public HttpCallback mHttpCallback;

	public Request(int t, HttpHost h, String u,RequestType rt) {		
		this(t,h,u);
		requestType = rt;
	}
	
	public Request(int t, HttpHost h, String u) {
		type = t;
		host = h;
		url = u;		
	}
}