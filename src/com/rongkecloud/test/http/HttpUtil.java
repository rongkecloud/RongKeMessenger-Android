package com.rongkecloud.test.http;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import android.text.TextUtils;
import android.util.Log;

class HttpUtil {
	private static final String TAG = HttpUtil.class.getSimpleName();
	
	private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String ENCODING_GZIP = "gzip";
	
	private static final InitHttpClient initHttpClient = new InitHttpClient();

	static DefaultHttpClient getHttpClient() {
		return initHttpClient.get();
	}
	
	/**
	 * http响应是否成功
	 */
	static boolean isHttpResponseOk(HttpResponse httpResp) {
		int retCode = httpResp.getStatusLine().getStatusCode();
		return retCode == HttpStatus.SC_OK;
	} 
	
	/**
	 * 解析http响应之后的消息内容
	 * @param resp
	 * @param values
	 * @param messages
	 * @throws UnsupportedEncodingException
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	static void parseToMessage(HttpResponse resp, HashMap<String, String> values, List<String> messages, BufferedReader br)
			throws UnsupportedEncodingException, IllegalStateException, IOException {
		br = new BufferedReader(new InputStreamReader(resp.getEntity().getContent(), HTTP.UTF_8), 8192);
		String line = null;
		Log.d(TAG, "parseToMessage begin--------------------------");
		while ((line = br.readLine()) != null) {
			String[] array = line.split("=");
			if (array.length == 2) {
				String k = new String(array[0].trim());
				String v = new String(array[1].trim());
				if (!TextUtils.isEmpty(k) && !TextUtils.isEmpty(v)) {
					Log.d(TAG, String.format("%s=%s", k, v));
				} else if (!TextUtils.isEmpty(k)) {
					Log.d(TAG, String.format("%s, value is null", k));
				}
				if (k.equals("msg"))
					messages.add(v);
				else
					values.put(k, v);
			}
		}
		Log.d(TAG, "parseToMessage end=========================");
//		if(null != br){
//			br.close();
//		}
//		closeHttpEntity(resp.getEntity());
	}	
	
	/**
	 * 解析http响应之后的返回结果
	 * @param resp
	 * @param values
	 * @throws UnsupportedEncodingException
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	static void parseToValue(HttpResponse resp, HashMap<String, String> values, BufferedReader br) throws UnsupportedEncodingException,
			IllegalStateException, IOException {
		br = new BufferedReader(new InputStreamReader(resp.getEntity().getContent(), HTTP.UTF_8), 8192);
		String line = null;
		Log.d(TAG, "parseToValue begin--------------------------");
		while ((line = br.readLine()) != null) {
			String[] array = line.split("=");
			if (array.length == 2) {
				String k = array[0].trim();
				String v = array[1].trim();
				if (!TextUtils.isEmpty(k) && !TextUtils.isEmpty(v)) {
					Log.d(TAG, String.format("%s=%s", k, v));
				} else if (!TextUtils.isEmpty(k)) {
					Log.d(TAG, String.format("%s, value is null", k));
				}
				values.put(k, v);
			} else if(array.length == 1){
				String k = new String(array[0].trim());
				String v = "";
				if (!TextUtils.isEmpty(k) && !TextUtils.isEmpty(v)) {
					Log.d(TAG, String.format("%s=%s", k, v));
				} else if (!TextUtils.isEmpty(k)) {
					Log.d(TAG, String.format("%s, value is null", k));
				}
				values.put(k, v);
			}
		}
		Log.d(TAG, "parseToValue end=========================");
//		if(null != br){
//			br.close();
//		}
//		closeHttpEntity(resp.getEntity());
	}
	
	/**
	 * 解析http响应之后的返回结果
	 * @param resp
	 * @param values
	 * @throws UnsupportedEncodingException
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	static void parseToText(HttpResponse resp, HashMap<String, String> values, BufferedReader br) throws UnsupportedEncodingException,
			IllegalStateException, IOException {
		br = new BufferedReader(new InputStreamReader(resp.getEntity().getContent(), HTTP.UTF_8), 8192);
		String line = null;
		StringBuffer text = new StringBuffer();
		Log.d(TAG, "parseToText begin--------------------------");		
		while ((line = br.readLine()) != null) {
			text.append(line);
		}
		values.put("text", text.toString());
		Log.d(TAG, "parseToText end=========================");
//		if(null != br){
//			br.close();
//		}
//		closeHttpEntity(resp.getEntity());
	}
	
	/**
	 * 解析http下载后的内容
	 * @param resp
	 * @param requesterId
	 * @param file
	 * @param fileSize
	 * @param values
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	static void parseToFile(HttpResponse resp, int type,String requesterId, File file, HashMap<String, String> values, BufferedReader br, HttpCallback bl) throws IllegalStateException, IOException {
		values.put("oper_result", "-1");
		String contentType = getContentType(resp);
		//Log.d(TAG, String.format("parseToFile -- contentType=%s, requesterId=%s, fileSize=%d", contentType, requesterId, fileSize));
		
		if (contentType.contains("text")) {
			parseToValue(resp, values,br);
			return;
		}

		int fileLen = 0;
		Header[] allHheaders = resp.getAllHeaders();
		if (null!=allHheaders && allHheaders.length>0) {
			for (Header obj : allHheaders) {
//				Log.d(TAG, String.format("parseToFile -- header name=%s, value=%s", obj.getName(), obj.getValue()));
				if ("Accept-Length".equals(obj.getName())) {
					fileLen = Integer.valueOf(obj.getValue());
					break;
				}
			}
		}
		if (fileLen == 0) {
			allHheaders = resp.getHeaders(HTTP.CONTENT_LEN);
			if (null!=allHheaders && allHheaders.length>0) {
				fileLen = Integer.parseInt(allHheaders[0].getValue());
			}
		}
		Log.d(TAG, String.format("parseToFile -- get file Length from header, length=%d", fileLen));
		
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		
		int total = 0;
		File tempFile = null;		
		if (fileLen > 0) {
			tempFile = new File(file.getAbsolutePath() + System.currentTimeMillis() +  ".tmp");
			if(!tempFile.exists()){
				tempFile.createNewFile();
			}

			FileOutputStream fos = new FileOutputStream(tempFile);
			InputStream is = resp.getEntity().getContent();
			byte[] buf = new byte[2048];
			int readLen = 0;

			while ((readLen = is.read(buf)) >= 0) {
				total += readLen;
				fos.write(buf, 0, readLen);		
				if(null != bl){
					Progress prog = new Progress(type,requesterId, total * 100 / fileLen);
					bl.onThreadProgress(prog);
				}
			}
			fos.flush();
			closeStream(is);
			closeStream(fos);
		}
		closeHttpEntity(resp.getEntity());
		if(null != tempFile){
			boolean renameResult = tempFile.renameTo(file);
			if(!renameResult){
				tempFile.delete();
			}
		}
		Log.d(TAG, String.format("parseToFile -- total=%d, fileLen=%d", total, fileLen));
		if (total != fileLen) {
			throw new IOException("Download file failed, because file size is error.");
		}
		values.put("oper_result", "0");
	}
	
	private static String getContentType(HttpResponse response) {
		String contentType = "";
		if (null != response) {
			HttpEntity entity = response.getEntity();
			if (null != entity) {
				if (null != entity.getContentType()) {
					contentType = entity.getContentType().getValue();
				}
			}
		}
		return contentType;
	}

	/*
	 * 关闭http响应流
	 */
	public static void closeHttpEntity(HttpEntity httpEntity) {
		if (null != httpEntity) {
			if (httpEntity.isStreaming()) {
				try {
					closeStream(httpEntity.getContent()); // 关闭流
				} catch (IOException e) {
					Log.d(TAG, "closeHttpEntity -- close stream is failed, exception info=" + e.getMessage());
				}
			}
			
			try {
				httpEntity.consumeContent(); // 释放链接
			} catch (IOException e) {
				Log.d(TAG, "closeHttpEntity -- release link is failed, exception info=" + e.getMessage());
			}
			httpEntity = null;
		}
	}

	private static void closeStream(Closeable stream) {
		if (null != stream) {
			try {
				stream.close();
			} catch (IOException e) {
			}
			stream = null;
		}
	}

	private static class InitHttpClient {
		private DefaultHttpClient client;

		InitHttpClient() {
			HttpParams params = new BasicHttpParams();
			HttpConnectionParams.setTcpNoDelay(params, true);
			HttpClientParams.setRedirecting(params, true);
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
			HttpProtocolParams.setUseExpectContinue(params, false);
			HttpConnectionParams.setSocketBufferSize(params, 8192);
			ConnManagerParams.setMaxConnectionsPerRoute(params, new ConnPerRoute() {
				@Override
				public int getMaxForRoute(HttpRoute route) {
					return 4;
				}
			});
			ConnManagerParams.setMaxTotalConnections(params, 30);
			SchemeRegistry supportedSchemes = new SchemeRegistry();
			supportedSchemes.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			supportedSchemes.register(new Scheme("Https", SSLSocketFactory.getSocketFactory(), 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, supportedSchemes);
			client = new DefaultHttpClient(ccm, params);
			// 增加HTTP访问失败重试 机制
			client.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(3, true));

			client.addRequestInterceptor(new HttpRequestInterceptor() {
				public void process(HttpRequest request, HttpContext context) {
					if (!request.containsHeader(HEADER_ACCEPT_ENCODING)) {
						request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
					}
				}
			});
			client.addResponseInterceptor(new HttpResponseInterceptor() {
				public void process(HttpResponse response, HttpContext context) {
					final HttpEntity entity = response.getEntity();
					if (entity == null) {
						return;
					}
					final Header encoding = entity.getContentEncoding();
					if (encoding != null) {
						for (HeaderElement element : encoding.getElements()) {
							if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
								response.setEntity(new InflatingEntity(response.getEntity()));
								break;
							}
						}
					}
				}
			});
			client.setKeepAliveStrategy(new ConnectionKeepAliveStrategy() {

				public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
					// Honor 'keep-alive' header
					HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
					while (it.hasNext()) {
						HeaderElement he = it.nextElement();
						String param = he.getName();
						String value = he.getValue();
						// timeout时间设定
						if (value != null && param.equalsIgnoreCase("timeout")) {
							try {
								long timeout = Long.parseLong(value);
								Log.d(TAG, "timeout = " + timeout);
								return (timeout - 8 > 0 ? timeout - 5 : timeout) * 1000;
							} catch (NumberFormatException ignore) {
							}
						}
					}
					Log.d(TAG, "defaulttimeout = " + 10 * 1000);
					return 10 * 1000;
				}
			});
		}

		DefaultHttpClient get() {
			return client;
		}
	}

	private static class InflatingEntity extends HttpEntityWrapper {
		public InflatingEntity(HttpEntity wrapped) {
			super(wrapped);
		}

		@Override
		public InputStream getContent() throws IOException {
			return new GZIPInputStream(wrappedEntity.getContent());
		}

		@Override
		public long getContentLength() {
			return -1;
		}
	}

}
