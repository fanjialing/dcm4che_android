package org.dcm4che3.android;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Bitmap.Config;

public class RasterUtil {
	public static int rgb565to24(short v){
		int i=uShortToInt(v);
		byte r=(byte) (i & 0xF800 >>11);
		byte g =(byte) ( i & 0x07E0 >>5);
		byte b =(byte) ( i & 0x001F);
		return Color.rgb(r, g, b);
		
	}
	
	public static int rgb24to565(byte r, byte g, byte b)
	{
		int i= ((r << 8) & 0xF800) | 
	            ((g << 3) & 0x7E0)  |
	            ((b >> 3));
		return i;
	}
	
	
	/**
	 * 将int类型的ushort存储于short中
	 * @param ushort
	 * @return
	 */
	public static short intToUShort(int ushort){
		if(ushort>32767 && ushort<65536){
			ushort=ushort-65536; 
		}
		return (short)ushort;
	}
	
	/**
	 * 将存储于short中的无类型short转化为int类型
	 * @param ushort
	 * @return
	 */
	public static int uShortToInt(short ushort){
		int v = ushort;
		if(v<0){
			v = v+65536;
		}
		return v;
	}
	
	
	/**
	 * 将int类型的ushort存储于short中
	 * @param ushort
	 * @return
	 */
	public static int longToUInt(long lInt){
		if(lInt>Integer.MAX_VALUE && lInt<0xFFFFFFFFL){
			lInt=lInt-0xFFFFFFFFL; 
		}
		return (int)lInt;
	}
	
	/**
	 * 将存储于short中的无类型short转化为int类型
	 * @param ushort
	 * @return
	 */
	public static long uIntToLong(int i){
		long v = i;
		
		if(v<0){
			v = v + 0xFFFFFFFFL;
		}
		return v;
	}
	
	public static Bitmap gray8ToBitmap(int width,int height,byte[] data){
		int ilength= data.length;
		int[] colors= new int[ilength];
		for(int i=0;i<ilength;i++){
			int v = data[i] & 0xFF;//必须与0xFF进行位运算，不然取得的值就有符号了
			colors[i]= Color.rgb(v, v, v);
		}
		//System.arraycopy(grays, 0,colors, 0, ilength);
		Bitmap bmp = Bitmap.createBitmap( colors,width, height, Config.ARGB_8888);
		return bmp;
	}
	
	public static Bitmap ARGBToBitmap(int width,int height,int[] data){
		int ilength= data.length;
		int[] colors= new int[ilength];
		System.arraycopy(data, 0,colors, 0, ilength);
		Bitmap bmp = Bitmap.createBitmap( width, height, Config.ARGB_8888);
		return bmp;
	}
	

	//rgb 565格式
	public static Bitmap RGB16ToBitmap(int width,int height,short[] data){
		int ilength= data.length;
		int[] colors= new int[ilength];
		for(int i=0;i<ilength;i++){
			colors[i] =rgb565to24(data[i]);
		}
		Bitmap bmp = Bitmap.createBitmap( width, height, Config.ARGB_8888);
		
		return bmp;
		
	}
	
	public static Bitmap rasterToBitmap(Raster raster){
		switch (raster.dataType) {
		case Raster.TYPE_BYTE:
			return gray8ToBitmap(raster.getWidth(), raster.getHeight(), raster.getByteData());
		case Raster.TYPE_USHORT:
			return RGB16ToBitmap(raster.getWidth(), raster.getHeight(), raster.getShortData());
		case Raster.TYPE_INT:
			return ARGBToBitmap(raster.getWidth(), raster.getHeight(), raster.getIntData());
		default:
			return null;
		}
		
	}

}
