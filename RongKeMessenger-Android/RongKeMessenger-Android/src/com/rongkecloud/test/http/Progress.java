package com.rongkecloud.test.http;

/**
 * 下载或上传进度值
 */
public class Progress {
	public int type; //请求的类型
	public String requesterId = null;// 请求id值
	public int value = 0;// 0~100的范围

	public Progress(int type,String id, int prog) {
		this.type = type;
		requesterId = id;
		value = prog;
	}
}