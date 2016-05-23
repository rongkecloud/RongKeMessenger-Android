package com.rongkecloud.test.http;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class Result {	
	public int type;
	public int opCode;
	public String requesterId;
	public HashMap<String, String> values;
	public List<String> messages;
	public File file;
	public byte[] data;
	public String arg0;
	public String arg1;
	public String arg2;
	public Object obj;
	
	
	public Result(int type){
		this.type = type;
	}
}
