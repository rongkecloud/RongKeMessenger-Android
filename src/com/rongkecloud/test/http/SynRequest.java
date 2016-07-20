package com.rongkecloud.test.http;

import android.content.Context;
import android.text.TextUtils;
import com.rongkecloud.test.http.Request.Method;
import com.rongkecloud.test.utility.FileLog;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/***
 * 该类主要是处理大并发Http请求 * 
 */
final class SynRequest{
	private String TAG = getClass().getSimpleName();
	private static SynRequest mSynRequest = null;
	private static final int CORE_POOL_SIZE = 10;// 线程最小个数
	
	private final LinkedBlockingQueue<Request> workQueue = new LinkedBlockingQueue<Request>();
	private SendRequestThread mDispatichRequestThread;
	private WakeLockPhone mWakeLock;
	private Context mContext;
	private DefaultHttpClient mClient = null;
	private ExecutorService mNewCachedThreadPool = Executors.newFixedThreadPool(CORE_POOL_SIZE);
	private HashMap<String, HttpRequestBase> requestList = new HashMap<String, HttpRequestBase>();

	private HttpFatalExceptionCallBack mHttpFatalExceptionCallBack;
	private SynRequest(Context mContext) {
		this.mContext = mContext;
		mWakeLock = new WakeLockPhone(mContext);
		mClient = HttpUtil.getHttpClient();
		mDispatichRequestThread = new SendRequestThread();
		mDispatichRequestThread.start();
	}
	
	public static SynRequest getInstance(Context mContext){
		if(null == mSynRequest){
			mSynRequest = new SynRequest(mContext);
		}
		return mSynRequest;
	}
	
	public void setOnHttpFatalException(HttpFatalExceptionCallBack callBack){
		mHttpFatalExceptionCallBack = callBack;
	}
	
	public synchronized void  abortRequest(String requesterId) {
		HttpRequestBase htb = requestList.remove(requesterId);
		FileLog.d(TAG, "abortRequest requestId = " + requesterId);
		if (htb != null && !htb.isAborted()) {
			FileLog.d(TAG, "null != htb, requestId = " + requesterId);
			htb.abort();
		}
	}
	
	private synchronized void removeRequest(String requesterId){
		FileLog.d(TAG, "removeRequest requestId = " + requesterId);
		if(null != requesterId){			
			requestList.remove(requesterId);
		}
	}
	
	private synchronized void addRequest(String requesterId, HttpRequestBase hb){
		FileLog.d(TAG, "addRequest requestId = " + requesterId);		
		if(null != requesterId && null != hb){
			requestList.put(requesterId, hb);
		}
	}

	public synchronized void execute(Request request) {
		FileLog.w(TAG, "requestBufferSize = " + workQueue.size());
		try {	
			if(checkRequestExists(request.requesterId) || requestList.containsKey(request.requesterId)){
				FileLog.d(TAG, "request execute:requestId = " + request.requesterId + " is exists to request. so ignore this request.");
				return;
			}
			workQueue.put(request);
		} catch (InterruptedException e) {
			FileLog.w(TAG, "thread id = " + mDispatichRequestThread.getId());
			FileLog.w(TAG, "thread runing = " + mDispatichRequestThread.isAlive());
			FileLog.w(TAG, "thread state = " + mDispatichRequestThread.getState());
			FileLog.w(TAG, "execute = " + e);
		}
	}

	
	class DoRquestThread implements Runnable {	
		private Request request;
		public DoRquestThread(Request request) {    
			this.request = request;
		}    
		  
		public void run() {
			FileLog.d("DoRquestThread", String.format("start new DoRquestThread url=%s", request.host.getHostName() + "/" + request.url) );
			processRequest(request);
		}    
	}  

	/**
	 * 线程类
	 * @author Administrator
	 *
	 */
	private class SendRequestThread extends Thread {
		SendRequestThread() {
			super("SynHTTPRequestThread");
		}

		@Override
		public void run() {
			super.run();
			try {
				while (!isInterrupted()) {
					Request request = workQueue.take();
					if (null != request) {
						mWakeLock.wake();
						try {
							if(request.requestType == Request.RequestType.MESSAGE){	
								FileLog.w(TAG, "remove more request,type=" + request.type );
								removeMoreGetMessageRequest(request.type);
							}
							mNewCachedThreadPool.submit(new DoRquestThread(request));
						} finally {
							mWakeLock.release();
						}
					} else {
						FileLog.w(TAG, "BlockingQueue take elment is null.");
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/***
	 * 移除多余的类型为TYPE的请求
	 */
	private synchronized void removeMoreGetMessageRequest(int type) {
		Iterator<Request> it = workQueue.iterator();
		while (it.hasNext()) {
			Request request = it.next();
			if (null != request && type == request.type) {
				FileLog.d(TAG, "remove more getMessage rquest.");
				it.remove();
			}
		}
	}

	/***
	 * 移除多余的类型为TYPE的请求
	 */
	private synchronized boolean checkRequestExists(String requestId) {
		if(TextUtils.isEmpty(requestId)){
			return false;
		}
		Iterator<Request> it = workQueue.iterator();
		while (it.hasNext()) {
			Request re = it.next();
			if (null != re && requestId.equals(re.requesterId)) {				
				return true;
			}
		}
		return false;
	}
	

	/**
	 * 分类处理HTTP 请求
	 * 
	 * @param req
	 */
	private void processRequest(Request req) {
		Result result = new Result(req.type);
		HttpResponse httpResp = null;
		BufferedReader br = null;
		try {
			FileLog.d(TAG, req.url + "/" + "Host=" + req.host.toHostString());

			if (req.method == Method.POST) {
				HttpPost post = new HttpPost(req.url);
				post.setParams(CommonHttpParams.getInstance().getHttpParams());

				if (req.files == null) {
					ArrayList<NameValuePair> list = new ArrayList<NameValuePair>();

					for (String key : req.params.keySet()) {
						String value = req.params.get(key);
						FileLog.d(TAG, req.url + "/" + key + ":" + value);
						list.add(new BasicNameValuePair(key, value));
					}
					UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list, "UTF-8");
					post.setEntity(entity);

				} else {
					MultipartEntity entity = new MultipartEntity();
					for (String key : req.params.keySet()) {
						String value = req.params.get(key);
						FileLog.d(TAG, req.url + "/" + key + ":" + value);
						StringBody strBody = new StringBody(value,Charset.forName(HTTP.UTF_8));
						entity.addPart(key, strBody);
					}					
					for (String key : req.files.keySet()) {
						File file = req.files.get(key);
						FileBody fileBody = new FileBody(file);
						entity.addPart(key, fileBody);						
					}
					post.setEntity(entity);
				}
				addRequest(req.requesterId, post);
				httpResp = HttpUtil.getHttpClient().execute(req.host, post);
				FileLog.w(TAG, "httpResp.getStatusLine=" + httpResp.getStatusLine());				
				result = processHttpResponse(req, httpResp,br);				
				removeRequest(req.requesterId);
			} else if (req.method == Method.GET) {
				HttpGet get = new HttpGet(req.url);		
				addRequest(req.requesterId, get);
				httpResp = mClient.execute(req.host, get);
				result = processHttpResponse(req, httpResp,br);	
				removeRequest(req.requesterId);
			}
		} catch (ClientProtocolException e) {
			FileLog.w(TAG, "ClientProtocolException -- info=" + e.toString());
			e.printStackTrace();
			result = new Result(req.type);
			result.opCode = HttpResponseCode.CLIENT_PROTOCOL_ERROR;			
		} catch (IOException e) {
			FileLog.w(TAG, "IOException -- info=" + e.toString());
			FileLog.w(TAG, String.format("API = %s", req.url));
			result = new Result(req.type);
			result.opCode = HttpResponseCode.NO_NETWORK;

		} catch (NumberFormatException e) {
			FileLog.w(TAG, "NumberFormatException -- info=" + e.toString());
			e.printStackTrace();
			result = new Result(req.type);
			result.opCode = HttpResponseCode.CLIENT_PROTOCOL_ERROR;

		} catch (Exception e) {
			FileLog.w(TAG, "Exception -- info=" + e.toString());
			e.printStackTrace();
			result = new Result(req.type);
			result.opCode = HttpResponseCode.NO_NETWORK;
		} finally {
			if(null != httpResp){
				FileLog.d(TAG, "close http entity");
				HttpUtil.closeHttpEntity(httpResp.getEntity());
			}
			if(null != br){
				FileLog.w(TAG, "close BufferedReader");
				try {
					br.close();
				} catch (IOException e) {
					FileLog.w(TAG, "close BufferedReader fail");
					e.printStackTrace();
				}
			}
		}

		if (result != null) {
			result.requesterId = req.requesterId;
			result.arg0 = req.arg0;	
			result.arg1 = req.arg1;	
			result.obj = req.obj;
		}		
		
		if (result.opCode == HttpResponseCode.INVALID_SESSION) {
			if(null != mHttpFatalExceptionCallBack){
				mHttpFatalExceptionCallBack.onHttpFatalException(1);
			}
		} else if(result.opCode == HttpResponseCode.BANNED_USER){
			if(null != mHttpFatalExceptionCallBack){
				mHttpFatalExceptionCallBack.onHttpFatalException(2);
			}
		} else {
			//将请求结果发送给调用的Manager
			req.mHttpCallback.onThreadResponse(result);
		}
	}

	private Result processHttpResponse(Request req, HttpResponse httpResp,BufferedReader br  ) {
		Result result = new Result(req.type);	
		HashMap<String, String> values = null;
		List<String> messages = null;
		File file = null;
		byte[] data = null;
	
		if (HttpUtil.isHttpResponseOk(httpResp)) {
			try {
				
				switch (req.requestType) {		
					case VALUE:
						values = new HashMap<String, String>();
						HttpUtil.parseToValue(httpResp, values,br);
						result.opCode = Integer.parseInt(values.get("oper_result"));
						break;				
					case TEXT:
						values = new HashMap<String, String>();					
						HttpUtil.parseToText(httpResp, values,br);
						result.opCode = HttpResponseCode.OK;
						break;
					case MESSAGE:
						values = new HashMap<String, String>();
						messages = new ArrayList<String>();
						HttpUtil.parseToMessage(httpResp, values, messages,br);
						result.messages = messages;
						result.opCode = Integer.parseInt(values.get("oper_result"));
						break;				
					case FILE:
						file = new File(req.filePath);
						values = new HashMap<String, String>();
						HttpUtil.parseToFile(httpResp, req.type, req.requesterId, file,values,br , req.mHttpCallback);
						result.opCode = Integer.parseInt(values.get("oper_result"));
						break;
				}
			} catch (UnsupportedEncodingException e) {
				result.opCode = HttpResponseCode.CLIENT_HTTP_RESOLVING_ERROR;
				FileLog.e(TAG,"processHttpResponse -- UnsupportedEncodingException info="	+ e.getMessage());
				e.printStackTrace();
	
			} catch (IllegalStateException e) {
				result.opCode = HttpResponseCode.CLIENT_HTTP_RESOLVING_ERROR;
				FileLog.e(TAG,"processHttpResponse -- IllegalStateException info=" + e.getMessage());
				e.printStackTrace();
	
			} catch (IOException e) {
				result.opCode = HttpResponseCode.CLIENT_HTTP_RESOLVING_ERROR;
				FileLog.e(TAG,"processHttpResponse -- IOException info="	+ e.getMessage());
				e.printStackTrace();
			}
			result.values = values;
			result.messages = messages;
			result.file = file;
			result.data = data;
	
		} else {
			result.opCode = HttpResponseCode.NO_NETWORK;
		}
		FileLog.d(TAG, String.format("%s/result=%s", result.type,result.opCode));
		return result;
	}

	/***
	 * 在注销的时候调用
	 */
	public void interuptAllRequest() {
		workQueue.clear();		
	}
}