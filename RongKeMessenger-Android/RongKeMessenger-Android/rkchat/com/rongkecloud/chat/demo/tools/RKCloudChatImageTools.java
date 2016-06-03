package com.rongkecloud.chat.demo.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.text.TextUtils;
import android.util.Log;

import com.rongkecloud.test.R;

/**
 * 图片工具类
 */
public class RKCloudChatImageTools {
	private static final String TAG = RKCloudChatImageTools.class.getSimpleName();	
	/**
	 * 压缩显示聊天页面中图片的缩略图
	 * @param context
	 * @param srcFilePath
	 * @return
	 */
	public static Bitmap resizeMmsThumbImage(Context context, String srcFilePath){
		if(TextUtils.isEmpty(srcFilePath) || !new File(srcFilePath).exists()){
			return null;
		}
		
		Bitmap taget = null;
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPurgeable = true;
		opts.inJustDecodeBounds = true; 
		BitmapFactory.decodeFile(srcFilePath, opts);
		int resampleWidth = 0;
		int resampleHeight = 0;
		if(opts.outWidth > opts.outHeight){
			resampleHeight = (int)context.getResources().getDimension(R.dimen.rkcloud_chat_msg_image_landscape_height);
		}else{
			resampleHeight = (int)context.getResources().getDimension(R.dimen.rkcloud_chat_msg_image_portrait_height);
		}
		resampleWidth = (int)Math.ceil(opts.outWidth*(double)resampleHeight/opts.outHeight);
		opts.inJustDecodeBounds = false;	
		opts.inSampleSize = getResampleSize(opts.outWidth, opts.outHeight, resampleWidth, resampleHeight);
		
		try{
			Bitmap bitmap = BitmapFactory.decodeFile(srcFilePath, opts);
			if(null == bitmap){
				return null;
			}
			taget = Bitmap.createScaledBitmap(bitmap, resampleWidth, resampleHeight, true);
			if (taget != bitmap) {
				safeReleaseBitmap(bitmap);
			}
		}catch(OutOfMemoryError e){
			
		}
		return taget;
	}

	/**
	 * 等比例压缩图片 
	 * 1：如果图片的宽和高都小于目标压缩图片的宽和高，那么不做任何操作
	 * 2：如果图片的宽和高任何一个大于目标压缩图片的宽和高，那么以最大边为基准来计算压缩比，然后等比例压缩图片
	 * @param srcFilePath 源图片的路径
	 * @param resampleWidth 压缩的目标宽度
	 * @param resampleHeight 压缩的目标高度
	 * @return bitmap 缩小后的图片对象， 使用完要用recycle方法释放空间
	 */
	public static Bitmap resizeBitmap(String srcFilePath, int resampleWidth, int resampleHeight) {
		if(TextUtils.isEmpty(srcFilePath) || !new File(srcFilePath).exists()){
			return null;
		}

		Bitmap taget = null;
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPurgeable = true;
		opts.inJustDecodeBounds = true; 
		BitmapFactory.decodeFile(srcFilePath, opts);
		opts.inJustDecodeBounds = false;
		if(opts.outWidth > resampleWidth && opts.outHeight > resampleHeight){
			opts.inSampleSize = getResampleSize(opts.outWidth, opts.outHeight, resampleWidth, resampleHeight);
			try {
				Bitmap bitmap = BitmapFactory.decodeFile(srcFilePath, opts);
				if (bitmap == null) {
					return taget;
				}
				
				int realWidth=resampleWidth, realHeight=resampleHeight;
				if (bitmap.getWidth() > resampleWidth || bitmap.getHeight() > resampleHeight) {
					float qw = (float) resampleWidth / bitmap.getWidth();
					float qh = (float) resampleHeight / bitmap.getHeight();
					if (qh < qw) {
						realWidth = (int)Math.ceil(qh * bitmap.getWidth());
						
					} else {
						realHeight = (int)Math.ceil(qw * bitmap.getHeight());
					}
				}
				bitmap = Bitmap.createScaledBitmap(bitmap, realWidth, realHeight, true);
				// 拿到图片的旋转值，并进行对应的旋转处理
				int degree = getRotationDegree(srcFilePath);
				Matrix m = new Matrix();
				if (0 != degree) {
					m.setRotate(degree, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
				}
				
				taget = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
				if (taget != bitmap) {
					safeReleaseBitmap(bitmap);
				}

			} catch (OutOfMemoryError e) {
				Log.e(TAG, "resizeBitmap -- outofMemoryError.", e);
			}
		}else{
			taget = BitmapFactory.decodeFile(srcFilePath);
		}
		return taget;
	}	

	/*
	 * 计算需要压缩的尺寸
	 */
	private static int getResampleSize(int oriWidth, int oriHeight, int destWidth, int destHeight) {
		int oriMax = Math.max(oriWidth, oriHeight);
		int destMax = Math.max(destWidth, destHeight);

		int resampleSize = 1;
		for (resampleSize = 1; resampleSize < Integer.MAX_VALUE; resampleSize++) {
			if (resampleSize * destMax > oriMax) {
				resampleSize--;
				break;
			}
		}

		resampleSize = resampleSize > 0 ? resampleSize : 1;
		return resampleSize;
	}

	/*
	 * 获取图片的旋转值
	 */
	private static int getRotationDegree(String filepath) {
		int degree = 0;
		ExifInterface exif = null;
		try {
			exif = new ExifInterface(filepath);
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
			if (orientation != -1) {
				switch (orientation) {
				case ExifInterface.ORIENTATION_ROTATE_90:
					degree = 90;
					break;
				case ExifInterface.ORIENTATION_ROTATE_180:
					degree = 180;
					break;
				case ExifInterface.ORIENTATION_ROTATE_270:
					degree = 270;
					break;
				}
			}
		} catch (IOException ex) {
			Log.e(TAG, "cannot read exif", ex);
		}
		return degree;
	}

	/**
	 * 对图片进行质量压缩，并返回图片的文件
	 * @param Bitmap bmp 要保存的图片对象
	 * @param String dstFilePath 要保存的文件路径
	 * @return File 被保存的图片文件
	 */
	public static File compressBitmap(Bitmap bmp, String dstFilePath) throws IOException {
		if(null==bmp || null==dstFilePath){
			return null;
		}

		File file = new File(dstFilePath);
		// 文件父目录不存在时创建，创建失败时返回null
		if(!file.getParentFile().exists()){
			if(!file.getParentFile().mkdirs()){
				return null;
			}
		}
		// 文件不存在时创建，创建失败时返回null
		if (!file.exists()) {	
			if(!file.createNewFile()){
				return null;
			}
		}
		
		// 开始质量压缩处理，默认是30的质量比压缩
    	ByteArrayOutputStream baos = null;
    	FileOutputStream fileOutStream = null;
    	try {
            baos = new ByteArrayOutputStream();
            bmp.compress(CompressFormat.JPEG, 30, baos);
            fileOutStream = new FileOutputStream(file);
            baos.writeTo(fileOutStream);
            
        } catch (Exception e) {
        	Log.e(TAG, "compressBitmap--1--exception info="+e.getMessage());
            return null;
        }finally{
        	if(null != baos){
        		 try {
					baos.close();
					baos = null;
				} catch (Exception e) {
					Log.e(TAG, "compressBitmap--2--exception info="+e.getMessage());
				}
        	}
        	if(null != fileOutStream){
       		 try {
       			fileOutStream.close();
       			fileOutStream = null;
				} catch (Exception e) {
					Log.e(TAG, "compressBitmap--3--exception info="+e.getMessage());
				}
        	}
        }	

    	// 以下是按照文件大小进行的质量比压缩，您可以根据需要进行对应的打开或注释掉   	
//    	int options = 100;// 压缩质量比，开启以100出现
//    	int maxCompressSize = 500;// 质量压缩图片时的最大值，单位：KB
//    	try {
//            baos = new ByteArrayOutputStream();
//            bmp.compress(CompressFormat.JPEG, options, baos);
//            while (baos.toByteArray().length > maxCompressSize*1000) {
//            	Log.d(TAG, "size="+baos.toByteArray().length);
//                baos.reset();
//                options -= 10;
//                bmp.compress(CompressFormat.JPEG, options, baos);
//            }
//            Log.d(TAG, "size="+baos.toByteArray().length);
//            fileOutStream = new FileOutputStream(file);
//            baos.writeTo(fileOutStream);
//            
//        } catch (Exception e) {
//        	Log.e(TAG, "compressBitmap--1--exception info="+e.getMessage());
//            return null;
//        }finally{
//        	if(null != baos){
//        		 try {
//					baos.close();
//					baos = null;
//				} catch (Exception e) {
//					Log.e(TAG, "compressBitmap--2--exception info="+e.getMessage());
//				}
//        	}
//        	if(null != fileOutStream){
//       		 try {
//       			fileOutStream.close();
//       			fileOutStream = null;
//				} catch (Exception e) {
//					Log.e(TAG, "compressBitmap--3--exception info="+e.getMessage());
//				}
//        	}
//        }	

		return file;
	}
	
	/**
	 * 安全释放Bitmap占用的资源
	 * @param bitmap
	 */
	private static void safeReleaseBitmap(Bitmap bitmap) {
		if (null != bitmap){
			if (!bitmap.isRecycled()) {
				bitmap.recycle();
			}
			bitmap = null;
		}
	}
	
	/**
	 * 切割图片的四个角为圆角
	 * @param bitmap
	 * @param pixels
	 * @return
	 */
	public static Drawable cutImgToRoundCorner(Context context, Bitmap bitmap) {
		if(null == bitmap){
			return null;
		}
		float roundRadius = 10f;// 圆角半径，单位：像素
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		int color = 0xff424242;
		Paint paint = new Paint();
		Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		RectF rectF = new RectF(rect);		
		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundRadius, roundRadius, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		
		Drawable drawable = null;
		if(null != output){
			drawable =new BitmapDrawable(context.getResources(), output);
		}
		
		return drawable;
	}
}
