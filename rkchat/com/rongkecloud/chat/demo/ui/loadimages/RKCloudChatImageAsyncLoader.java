package com.rongkecloud.chat.demo.ui.loadimages;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.rongkecloud.chat.demo.tools.RKCloudChatImageTools;
import com.rongkecloud.chat.demo.ui.loadimages.RKCloudChatImageRequest.IMAGE_REQUEST_TYPE;

/**
 * 异步多线程加载所有图片信息
 */
public class RKCloudChatImageAsyncLoader {
	private static final String TAG = RKCloudChatImageAsyncLoader.class.getSimpleName();
	
	private static final int CACHE_SIZE = 4 * 1024 * 1024; // 4MiB
	private static final int DELAY_NOTIFY_TIME = 500;// 延迟回调的时间，单位：毫秒
	
	private static final int IMAGE_WHAT_LOAD_COMPLETE = 1;// 图片异步加载完成
	private static final int IMAGE_WHAT_DELAY_NOTIFY = 2;// 图片延迟回调
	private static final int IMAGE_WHAT_CACHE_INVALID = 3;// 图片cache失效

	private static RKCloudChatImageAsyncLoader mInstance;
	
	private Context mContext;		
	private Handler mHandler;
	
	private WeakReference<ImageLoadedCompleteNotify> mListener;
	private WeakReference<ImageLoadedCompleteDelayNotify> mDelayListener;
	
	private Set<String> mExistUniqueKey;// 已经存在的key
	private Map<String, List<RKCloudChatImageResult>> mDelayNotifyResult;
	private Map<String, SoftReference<RKCloudChatImageResult>> mEarseCache;// 可回收的图片Cache
	private LruCache<String, RKCloudChatImageResult> mHighCache;// 高速缓存
		
	private RKCloudChatImageAsyncLoader(Context context){
		mContext = context;
		
		mExistUniqueKey = Collections.synchronizedSet(new HashSet<String>());
		mDelayNotifyResult = new HashMap<String, List<RKCloudChatImageResult>>();
		mEarseCache = new ConcurrentHashMap<String, SoftReference<RKCloudChatImageResult>>();
		
		mHighCache = new LruCache<String, RKCloudChatImageResult>(CACHE_SIZE) {
			@Override
			protected void entryRemoved(boolean evicted, String key, RKCloudChatImageResult oldValue, RKCloudChatImageResult newValue) {
				if (evicted) {
					// cache满了被移除掉了
					if (null != oldValue) {
						mEarseCache.put(key, new SoftReference<RKCloudChatImageResult>(oldValue));
					}
				}
			};

			@Override
			protected int sizeOf(String key, RKCloudChatImageResult value) {
				if (null == value || null == value.resource) {
					return 0;
				}
				if (BitmapDrawable.class == value.resource.getClass()) {
					BitmapDrawable drab = (BitmapDrawable) value.resource;
					final Bitmap bmp = drab.getBitmap();
					if (null == bmp) {
						return 0;
					}
					return bmp.getRowBytes() * bmp.getHeight();
				}
				return 1;
			};
		};
		
		mHandler = new Handler(mContext.getMainLooper()) {
			public void handleMessage(Message msg) {
				switch (msg.what) {
					case IMAGE_WHAT_CACHE_INVALID:
						reloadAllImages();
						break;
					
					case IMAGE_WHAT_LOAD_COMPLETE:
						ImageResultHolder holder = (ImageResultHolder) msg.obj;
						mEarseCache.remove(holder.uniqueKey);// 顺序不能颠倒 ，否则会出问题, 移除可擦除缓存中的数据，这步骤可有可无
						mHighCache.put(holder.uniqueKey, holder.result);// 高速缓存 持有引用
						mExistUniqueKey.remove(holder.uniqueKey);// 清除 请求缓存列表
						// 通知
						if(null != mListener){
							final ImageLoadedCompleteNotify l = mListener.get();
							if (null != l) {
								l.onLoadImageCompleteNotify(holder.result);
							}
						}
						// 延迟通知
						if(null != mDelayListener){
							String typeStr = holder.result.type.name();
							if(!mDelayNotifyResult.containsKey(typeStr)){
								mDelayNotifyResult.put(typeStr, new ArrayList<RKCloudChatImageResult>());
							}
							
							mDelayNotifyResult.get(typeStr).add(holder.result);
							if(!hasMessages(IMAGE_WHAT_DELAY_NOTIFY)){
								sendEmptyMessageDelayed(IMAGE_WHAT_DELAY_NOTIFY, DELAY_NOTIFY_TIME);
							}
						}
						break;
						
					case IMAGE_WHAT_DELAY_NOTIFY:
						removeMessages(IMAGE_WHAT_DELAY_NOTIFY);
						if(null != mDelayListener){
							ImageLoadedCompleteDelayNotify delayListener = mDelayListener.get();
							if(null != delayListener){
								Set<String> keys = mDelayNotifyResult.keySet();
								for(String key : keys){
									delayListener.onLoadImageCompleteDelayNotify(IMAGE_REQUEST_TYPE.valueOf(key), mDelayNotifyResult.get(key));
								}
							}
						}
						mDelayNotifyResult.clear();
						break;
				}
			};
		};
	}
	
	public static RKCloudChatImageAsyncLoader getInstance(Context context){
		if(null == mInstance){
			mInstance = new RKCloudChatImageAsyncLoader(context);
		}
		return mInstance;
	}

	/**
	 * 注册 监听器，来监听图片加载结果
	 * 监听器采用的是弱引用，不需要进行unregister动作
	 * 
	 * @param listener
	 */
	public void registerListener(ImageLoadedCompleteNotify listener) {
		if(null != listener){
			mListener = new WeakReference<ImageLoadedCompleteNotify>(listener);
		}else{
			mListener = null;
		}
	}
	
	public void registerDelayListener(ImageLoadedCompleteDelayNotify listener){
		if(null != listener){
			mDelayListener = new WeakReference<ImageLoadedCompleteDelayNotify>(listener);
		}else{
			mDelayListener = null;
		}
	}

	/**
	 * cache中的图片资源失效
	 */
	private void reloadAllImages() {
		for (SoftReference<RKCloudChatImageResult> obj : mEarseCache.values()) {
			if (null != obj && null != obj.get()) {
				obj.get().isExpired = true;
			}
		}

		for (RKCloudChatImageResult obj : mHighCache.snapshot().values()) {
			if (null != obj) {
				obj.isExpired = true;
			}
		}
	}
	
	/**
	 * 清除某种类型的图片
	 * @param type
	 */
	public void removeImagesByType(IMAGE_REQUEST_TYPE type, String delKey){
		if(null == type){
			return;
		}
		String compareKey = type.name()+"-";
		if(null!=delKey){
			compareKey = String.format("%s-%s-", type.name(), delKey);
		}
		mDelayNotifyResult.remove(type.name());
		
		mExistUniqueKey.clear();
		 
		Set<String> keys = mEarseCache.keySet();
		for(String key : keys){
			if(key.startsWith(compareKey)){
				mEarseCache.remove(key);
			}
		}
		
		keys = mHighCache.snapshot().keySet();
		for(String key : keys){
			if(key.startsWith(compareKey)){
				mHighCache.remove(key);
			}
		}
	}
	
	public void removeAllImages(){
		mDelayNotifyResult.clear();
		mExistUniqueKey.clear();
		mEarseCache.clear();
		mHighCache.evictAll();
	}

	/**
	 * 请求图片资源
	 * @param req
	 * @return
	 */
	public RKCloudChatImageResult sendPendingRequestQuryCache(RKCloudChatImageRequest req) {
		// 参数效验
		if (null == req) {
			return null;
		}
		RKCloudChatImageResult result = null;
		String uniqueKey = req.getUniqueKey();
		// 先查找高速缓存
		result = mHighCache.get(uniqueKey);
		// 如果高速缓存没有，那么查找可擦除的缓存
		if (null == result) {
			SoftReference<RKCloudChatImageResult> soResult = mEarseCache.get(uniqueKey);
			if (null != soResult) {
				result = soResult.get();
			}
		}

		boolean needRequest = true;// 是否需要请求加载，true表示需要
		if (null != result && !result.isExpired) {
			needRequest = false;
		}

		if (req.isReload || needRequest) {
			if (!mExistUniqueKey.contains(uniqueKey)) {
				mExistUniqueKey.add(uniqueKey);
				new Thread(new LoadImageRunnable(req)).start();
			}
		}

		return result;
	}

	// 图片加载工作类
	private class LoadImageRunnable implements Runnable {
		private RKCloudChatImageRequest request;

		LoadImageRunnable(RKCloudChatImageRequest req) {
			request = req;
		}

		@Override
		public void run() {
			Drawable drawable = null;
			if(IMAGE_REQUEST_TYPE.GET_MSG_THUMBNAIL==request.getType()){
				Bitmap pic = RKCloudChatImageTools.resizeMmsThumbImage(mContext, request.getKey()); 
				if(null != pic){
					drawable = new BitmapDrawable(mContext.getResources(), pic);
				}
				
			}else if(IMAGE_REQUEST_TYPE.GET_MSG_VIDEO_THUMBNAIL == request.getType()){
				Bitmap pic = BitmapFactory.decodeFile(request.getKey());
				if(null != pic){
					drawable = new BitmapDrawable(mContext.getResources(), pic);
				}
			}else if(IMAGE_REQUEST_TYPE.GET_CONTACT_HEADERIMG == request.getType()){
				try {
					Bitmap bitmap = BitmapFactory.decodeFile(request.getKey());
					if (null != bitmap) {
						if (request.isSquare) {
							drawable = new BitmapDrawable(mContext.getResources(), bitmap);
						} else {
							drawable = RKCloudChatImageTools.cutImgToRoundCorner(mContext, bitmap);
						}
					}
				} catch (OutOfMemoryError e) {
					Log.e(TAG, "load vard photo OutOfMemoryError, info="+e.getMessage());
				}
			}

			RKCloudChatImageResult result = new RKCloudChatImageResult();
			result.type = request.getType();
			result.key = request.getKey();
			result.requester = request.getRequester();
			result.isSquare = request.isSquare;
			result.isExpired = false;
			result.resource = drawable;
			// create holder
			ImageResultHolder holder = new ImageResultHolder(result, request.getUniqueKey());
			Message msg = Message.obtain(mHandler, IMAGE_WHAT_LOAD_COMPLETE, holder);
			msg.sendToTarget();
		}
	}

	private class ImageResultHolder {
		RKCloudChatImageResult result;
		String uniqueKey;

		public ImageResultHolder(RKCloudChatImageResult result, String uniqueKey) {
			this.result = result;
			this.uniqueKey = uniqueKey;
		}
	}

	/**
	 * 图片加载完成类通知
	 */
	public interface ImageLoadedCompleteNotify {
		/**
		 * 图片加载完成（回调）
		 * @param result 请求的结果
		 */
		void onLoadImageCompleteNotify(RKCloudChatImageResult result);
	}
	
	/**
	 * 图片加载完成后的延迟回调
	 */
	public interface ImageLoadedCompleteDelayNotify{
		/**
		 * 图片加载完成后的延迟回调
		 * @param type
		 * @param result
		 */
		void onLoadImageCompleteDelayNotify(IMAGE_REQUEST_TYPE type, List<RKCloudChatImageResult> result);
	}
}