package com.rongkecloud.test.utility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.content.Context;
import android.database.Cursor;
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
import android.net.Uri;
import android.provider.MediaStore.Images.Media;

/**
 * 图片工具类
 */
public class ImageUtil {
	private static final String TAG = ImageUtil.class.getSimpleName();

	public static final int RESIZE_MODE_WIDTH_LIMIT = 1; // 压缩图片时在宽度上限制
	public static final int RESIZE_MODE_HEIGHT_LIMIT = 2; // 压缩图片时在高度上限制

	/**
	 * 按照压缩比例压缩图片
	 * @param src 图片资源
	 * @param factor 压缩系数
	 * @return bitmap
	 */
	public static Bitmap resizeBitmap(Bitmap src, float factor) {
		Bitmap dst = null;
		if (src != null) {
			int dstW = (int) (src.getWidth() * factor);
			int dstH = (int) (src.getHeight() * factor);
			try {
				dst = Bitmap.createScaledBitmap(src, dstW, dstH, true);
			} catch (OutOfMemoryError e) {
				Print.w(TAG, String.format("resizeBitmap -- OutOfMemoryError occur with weight=%s, height=%s", dstW, dstH));
			}

			if (src != dst) {
				safeReleaseBitmap(src);
			}
		}
		return dst;
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
		if (srcFilePath == null)
			throw new IllegalArgumentException("Image file path should not be null");

		Bitmap taget = null;
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPurgeable = true;
		opts.inJustDecodeBounds = true; 
		BitmapFactory.decodeFile(srcFilePath, opts);
		opts.inJustDecodeBounds = false;
		opts.inSampleSize = getClosestResampleSize(opts.outWidth, opts.outHeight, Math.max(resampleWidth, resampleHeight));
		try {
			Bitmap bitmap = BitmapFactory.decodeFile(srcFilePath, opts);
			if (bitmap == null) {
				return taget;
			}
			// 拿到图片的旋转值：为了满足有的机型
			int degree = getExifOrientation(srcFilePath);
			Matrix m = new Matrix();

			if (bitmap.getWidth() > resampleWidth || bitmap.getHeight() > resampleHeight) {
				float qw = ((float) resampleWidth) / bitmap.getWidth();
				float qh = ((float) resampleHeight) / bitmap.getHeight();
				if (qh < qw) {
					float width01 = qh * bitmap.getWidth();
					bitmap = Bitmap.createScaledBitmap(bitmap, (int) width01, (int) resampleHeight, true);
				} else {
					float height01 = qw * bitmap.getHeight();
					bitmap = Bitmap.createScaledBitmap(bitmap, (int) resampleWidth, (int) height01, true);
				}
			}

			if (0 != degree) {
				m.setRotate(degree, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
			}
			taget = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
			if (taget != bitmap) {
				safeReleaseBitmap(bitmap);
			}

		} catch (OutOfMemoryError e) {
			Print.e(TAG, "resizeBitmap -- outofMemoryError.", e);
		}
		return taget;
	}
	
	/**
	 * 压缩图片
	 * @param srcFilePath
	 * @param resampleWidth
	 * @param resampleHeight
	 * @return
	 */
	public static Bitmap resizeBitmapForce(String srcFilePath, int resampleWidth, int resampleHeight) {
		if (srcFilePath == null)
			throw new IllegalArgumentException("Image file path should not be null");

		Bitmap taget = null;
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPurgeable = true;
		opts.inJustDecodeBounds = true; // 为true那么将不返回实际的bitmap
		BitmapFactory.decodeFile(srcFilePath, opts);
		opts.inJustDecodeBounds = false;
		opts.inSampleSize = getClosestResampleSize(opts.outWidth, opts.outHeight, Math.max(resampleWidth, resampleHeight));
		try {
			Bitmap bitmap = BitmapFactory.decodeFile(srcFilePath, opts);
			if (bitmap == null) {
				return taget;
			}
			// 拿到图片的旋转值：为了满足有的机型
			int degree = getExifOrientation(srcFilePath);
			Matrix m = new Matrix();

			float qw = ((float) resampleWidth) / bitmap.getWidth();
			float qh = ((float) resampleHeight) / bitmap.getHeight();
			if (qh < qw) {
				float width01 = qh * bitmap.getWidth();
				bitmap = Bitmap.createScaledBitmap(bitmap, (int) width01, (int) resampleHeight, true);
			} else {

				float height01 = qw * bitmap.getHeight();
				bitmap = Bitmap.createScaledBitmap(bitmap, (int) resampleWidth, (int) height01, true);
			}
			if (0 != degree) {
				m.setRotate(degree, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
			}
			taget = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
			if (taget != bitmap) {
				safeReleaseBitmap(bitmap);
			}

		} catch (OutOfMemoryError e) {
			Print.e(TAG, "resizeBitmap outofMemoryError.", e);
		}
		return taget;
	}		

	public static Bitmap resizeBitmapForce(Bitmap srcBitmap, int resampleWidth, int resampleHeight) {
		if (srcBitmap == null){
			throw new IllegalArgumentException("Image file path should not be null");
		}

		Bitmap taget = null;
		try {
			// 拿到图片的旋转值：为了满足有的机型
			Matrix m = new Matrix();

			float qw = ((float) resampleWidth) / srcBitmap.getWidth();
			float qh = ((float) resampleHeight) / srcBitmap.getHeight();
			if (qh < qw) {
				float width01 = qh * srcBitmap.getWidth();
				srcBitmap = Bitmap.createScaledBitmap(srcBitmap, (int) width01, (int) resampleHeight, true);
			} else {
				float height01 = qw * srcBitmap.getHeight();
				srcBitmap = Bitmap.createScaledBitmap(srcBitmap, (int) resampleWidth, (int) height01, true);
			}
			taget = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(), srcBitmap.getHeight(), m, true);
			if (taget != srcBitmap) {
				safeReleaseBitmap(srcBitmap);
			}

		} catch (OutOfMemoryError e) {
			Print.e(TAG, "resizeBitmap -- outofMemoryError.", e);
		}
		return taget;
	}
	
	/**
	 * 压缩图片之后保存到file文件中
	 * @param src
	 * @param dstFilePath
	 * @param factor
	 * @return
	 * @throws IOException
	 */
	public static File resizeBitmapAndSave(Bitmap src, String dstFilePath, float factor) throws IOException {
		Bitmap dst = resizeBitmap(src, factor);

		File dstFile = null;
		if (dst != null) {
			dstFile = saveStickerBitmap(dst, dstFilePath);
			dst.recycle();
		}

		return dstFile;
	}	

	/*
	 * 计算需要压缩的尺寸
	 */
	private static int getClosestResampleSize(int cw, int ch, int maxDim) {
		int max = Math.max(cw, ch);

		int resample = 1;
		for (resample = 1; resample < Integer.MAX_VALUE; resample++) {
			if (resample * maxDim > max) {
				resample--;
				break;
			}
		}

		if (resample > 0) {
			return resample;
		}
		return 1;
	}

	/*
	 * 检查图片是否需要旋转
	 */
	private static int getExifOrientation(String filepath) {
		int degree = 0;
		ExifInterface exif = null;
		try {
			exif = new ExifInterface(filepath);
		} catch (IOException ex) {
			Print.e(TAG, "cannot read exif", ex);
		}
		
		if (exif != null) {
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
		}
		return degree;
	}

	/**
	 * (暂时未使用)
	 * @param src
	 * @param mode
	 * @param limit
	 * @return
	 */
	public static float get1DResizeFactor(Bitmap src, int mode, int limit) {
		float f = 1.0f;
		if (src != null) {
			float w = src.getWidth();
			float h = src.getHeight();

			switch (mode) {
			case RESIZE_MODE_WIDTH_LIMIT:
				if (w > limit) {
					f = limit / w;
				}
				break;
			case RESIZE_MODE_HEIGHT_LIMIT:
				if (h > limit) {
					f = limit / h;
				}
				break;
			}
		}

		return f;
	}

	/**
	 * 根据要求的最大宽wLimit和高hLimit，计算压缩比例
	 */
	public static float get2DResizeFactor(Bitmap src, int wLimit, int hLimit) {
		float f = 1.0f;
		if (src != null) {
			float w = src.getWidth();
			float h = src.getHeight();

			if (w < wLimit && h < hLimit) {
				return f;
			}
			float qw = wLimit / w;
			float qh = hLimit / h;

			if (qw > qh) {
				f = qh;
			} else {
				f = qw;
			}
		}
		return f;

	}

	/**
	 * 计算图片压缩比例，此方法用于只在一个方向上限制图片大小。对于小于限制的图片，不做压缩。
	 * @param String filePath 源文件路径
	 * @param int mode @see RESIZE_MODE_WIDTH_LIMIT @see RESIZE_MODE_HEIGHT_LIMIT
	 * @param int limit 限制值
	 * @return float factor 图片缩小系数
	 */
	public static float get1DResizeFactor(String filePath, int mode, int limit) {
		if (filePath == null){
			throw new IllegalArgumentException("Image file path should not be null");
		}
		
		Bitmap src = BitmapFactory.decodeFile(filePath);
		return get1DResizeFactor(src, mode, limit);

	}

	/**
	 * 计算图片压缩比例，此方法用于只在一个矩形区域内限制图片大小。对于小于限制的图片，不做压缩。
	 * @param String filePath 源文件路径
	 * @param int wLimit 宽度限制
	 * @param int hLimit 高度限制
	 * @return float factor 图片缩小系数
	 */
	public static float get2DResizeFactor(String filePath, int wLimit, int hLimit) {
		if (filePath == null){
			throw new IllegalArgumentException("Image file path should not be null");
		}

		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filePath, opts);
		float factor = 1.0f;
		float optsWidth = opts.outWidth;
		float optsHeight = opts.outHeight;
		if (optsWidth < wLimit && optsHeight < hLimit) {
			return factor;
		}
		float qw = wLimit / optsWidth;
		float qh = hLimit / optsHeight;

		if (qw > qh) {
			factor = qh;
		} else {
			factor = qw;
		}
		return factor;
	}

	/**
	 * 将图片保存为文件,并返回图片的文件
	 * @param Bitmap bmp 要保存的图片对象
	 * @param String dstFilePath 要保存的文件路径
	 * @return File 被保存的图片文件
	 */
	public static File saveBitmap(Bitmap bmp, String dstFilePath) throws IOException {
		if (bmp == null)
			throw new IllegalArgumentException("Image should not be null");
		
		if (dstFilePath == null)
			throw new IllegalArgumentException("File path should not be null");

		File file = new File(dstFilePath);	
		if (!file.exists()) {			
			file.createNewFile();
		}

		FileOutputStream fos = new FileOutputStream(file);

		if (bmp != null && fos != null) {
			BufferedOutputStream bos = new BufferedOutputStream(fos, 4096);
			bmp.compress(Bitmap.CompressFormat.JPEG, 100, bos);
			bos.close();
			fos.close();
		}

		return file;
	}

	/**
	 * 保存图片到SD卡中
	 * @param filePath 图片保存路径
	 * @param bitmap  图片内容
	 */
	public synchronized static void saveBitmap(String filePath, Bitmap bitmap) {
		if (!SDCardUtil.getExternalStorageCard()) {
			Print.w(TAG, "Sdcard is not mounted,cache Bitmap do nothing.");
			return;
		}
		OutputStream outputStream = null;
		File file = new File(filePath);
		try {
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			outputStream = new FileOutputStream(file);
			bitmap.compress(CompressFormat.PNG, 100, outputStream);
			outputStream.flush();
			Print.i(TAG, "download logo " + filePath);
		} catch (Exception e) {
			Print.w(TAG, "Exception", e);
			if (file.exists()) {
				file.delete();
			}
		} finally {
			if (null != outputStream) {
				try {
					outputStream.close();
				} catch (IOException e) {
				}
				outputStream = null;
			}

			if (null != bitmap && !bitmap.isRecycled()) {
				bitmap.recycle();
			}
			bitmap = null;
		}
	}

	/**
	 * 用于保存sticker的缩略图的图片信息
	 * @param bmp
	 * @param dstFilePath
	 * @return
	 * @throws IOException
	 */
	public static File saveStickerBitmap(Bitmap bmp, String dstFilePath) throws IOException {
		if (bmp == null)
			throw new IllegalArgumentException("Image should not be null");
		if (dstFilePath == null)
			throw new IllegalArgumentException("File path should not be null");

		File file = new File(dstFilePath);
		if (!file.exists()) {
			file.createNewFile();
		}

		FileOutputStream fos = new FileOutputStream(file);

		if (bmp != null && fos != null) {
			BufferedOutputStream bos = new BufferedOutputStream(fos, 4096);
			bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
			bos.close();
			fos.close();
		}
		return file;
	}

	/**
	 * 安全释放Bitmap占用的资源
	 * @param bitmap
	 */
	public static void safeReleaseBitmap(Bitmap bitmap) {
		if (null == bitmap)
			return;
		
		if (!bitmap.isRecycled()) {
			bitmap.recycle();
		}
		bitmap = null;
	}
	
	/**
	 * 把缩略图转换成数据流格式
	 * @param filepath
	 * @param width
	 * @param height
	 * @return
	 */
	public static byte[] getThumbData(String filepath, int width, int height){
		Bitmap bitmap = resizeBitmap(filepath, width, height);
		if(null == bitmap){
			return null;
		}
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bitmap.compress(CompressFormat.JPEG, 100, bos);
		safeReleaseBitmap(bitmap);
		return bos.toByteArray();
	}
	
	/**
	 * 把缩略图转换成数据流格式
	 * @param filepath
	 * @param width
	 * @param height
	 * @return
	 * @throws FileNotFoundException 
	 */
	public static byte[] getThumbData(String filepath){		
		File f = new File(filepath);  
        if(!f.exists()){  
            return null;
        }  
  
        ByteArrayOutputStream bos = new ByteArrayOutputStream((int)f.length());  
        BufferedInputStream in = null;  
        try{  
            in = new BufferedInputStream(new FileInputStream(f));  
            int buf_size = 1024;  
            byte[] buffer = new byte[buf_size];  
            int len = 0;  
            while(-1 != (len = in.read(buffer,0,buf_size))){  
                bos.write(buffer,0,len);  
            }  
            return bos.toByteArray();  
        }catch (IOException e) {  
            e.printStackTrace();  
            return null;
        }finally{  
            try{  
                in.close(); 
                bos.close();  
            }catch (IOException e) {  
                e.printStackTrace();  
            }  
            
        }  
	}
	
	/**
	 * 切割图片的四个角为圆角
	 * @param bitmap
	 * @param pixels
	 * @return
	 */
	public static Drawable cutImgToRoundCorner(Bitmap bitmap) {
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
			drawable =new BitmapDrawable(output);
		}
		// 释放资源
//		bitmap.recycle();
//		if(null != output){
//			output.recycle();
//		}
		
		return drawable;
	}
	
	/**
	 * 获取图片选择功能的路径
	 */
	public static String getChoosePicturePath(Context context, Uri uri) {
		if (null==context || null==uri || uri.getPath().length()==0) {
			return null;
		}

		String picPath = null;
		String scheme = uri.getScheme();
		// 抓取图片路径
		if ("file".equals(scheme)) {
			picPath = uri.getSchemeSpecificPart();
		} else if ("content".equals(scheme)) {
			String[] projection = new String[] { Media.DATA };
			Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
			if (cursor.moveToFirst()) {
				picPath = cursor.getString(0);
			}
			cursor.close();
		}
		return picPath;
	}	
	
	
}
