package org.dcm4che3.android;

/**
 * 点阵表示，不同的数据类型使用不同的数组，调用不同的方法读取数组
 * @author ljl
 *
 */

public class Raster {

	/** Tag for unsigned byte data. */
	public static final int TYPE_BYTE = 0;

	/** Tag for unsigned short data. */
	public static final int TYPE_USHORT = 1;

	/** Tag for signed short data. Placeholder for future use. */
	public static final int TYPE_SHORT = 2;

	/** Tag for int data. */
	public static final int TYPE_INT = 3;

	/** Tag for float data. Placeholder for future use. */
	public static final int TYPE_FLOAT = 4;

	/** Tag for double data. Placeholder for future use. */
	public static final int TYPE_DOUBLE = 5;

	/* Tag for int64 data */
	public static final int TYPE_LONG = 6;

	/** Tag for undefined data. */
	public static final int TYPE_UNDEFINED = 32;

	protected int width;// 宽度
	protected int height;// 高度
	protected int dataType;// 数据类型
	
	protected int iLength;
	protected int iSize;

	protected byte[] bData;// byte类型
	protected short[] sData;// short类型和ushort类型
	protected int[] iData;// int类型
	protected long[] lData;//int64类型
    protected float[] fData;//浮点类型
    protected double[] dData;//双精浮点类型

	public int getDataType() {
		return dataType;
	}

	
	public int getWidth() {
		return width;
	}

	
	public int getHeight() {
		return height;
	}


	public byte[] getByteData() {
		return bData;
	}

	
	public short[] getShortData() {
		return sData;
	}

	
	public int[] getIntData() {
		return iData;
	}
	
	public float[] getFloatData(){
		return fData;
	}
	
	public double[] getDoubleData(){
		return dData;
	}

	
	public Raster(int width, int height,int dataType) {
		super();
		this.width = width;
		this.height = height;
		this.dataType = dataType;
		iLength= width*height;
		switch (dataType) {
		case TYPE_SHORT:
			sData = new short[iLength];
			iSize = iLength*2;
			
			break;
		case TYPE_USHORT:
			sData = new short[iLength];
			iSize = iLength*2;
			break;
		case TYPE_INT:
			iData = new int[iLength];
			iSize = iLength*4;
			break;
		case TYPE_FLOAT:
		    fData= new float[iLength];
		    iSize = iLength*8;
		    break;
		case TYPE_DOUBLE:
		    dData= new double[iLength];
		    iSize = iLength*8;
		    break;
		case TYPE_LONG:
			lData = new long[iLength];
			iSize = iLength*8;
			break;
	
		default:
			bData = new byte[iLength];
			iSize = iLength;
			break;
		}
	}
	
	/**
	 * 返回各种类型的数组的大小
	 * @return
	 */
	public int length(){
		return iLength;
	
	}
	
	/**
	 * 返回数据的byte数
	 * @return
	 */
	public int size(){
		return iSize;
	}
	
	
	
	public int getVal(int index){
		int v=0;
		switch (dataType) {
		case TYPE_SHORT:
			v= sData[index];
			break;
		case TYPE_USHORT:
			v=RasterUtil.uShortToInt(sData[index]);
			break;
		case TYPE_INT:
			v= iData[index];
			break;
		default:
			v= bData[index];
			break;
		}
		return v;
	}
	
	public void setVal(int index,int v){
		
		switch (dataType) {
		case TYPE_SHORT:
			sData[index]=(short)v;
			
			break;
		case TYPE_USHORT:
			sData[index]=RasterUtil.intToUShort(v);
			
			break;
		case TYPE_INT:
			iData[index]= v;
			break;
		default:
			bData[index]=(byte)v;
			break;
		}
		
	}

}
