package org.dcm4che3.android.imageio.dicom;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;


import org.dcm4che3.android.Raster;
import org.dcm4che3.android.image.LookupTable;
import org.dcm4che3.android.image.LookupTableFactory;
import org.dcm4che3.android.image.Overlays;
import org.dcm4che3.android.image.PhotometricInterpretation;
import org.dcm4che3.android.image.StoredValue;
import org.dcm4che3.android.imageio.stream.ImageInputStreamAdapter;
import org.dcm4che3.android.stream.FileImageInputStream;
import org.dcm4che3.android.stream.ImageInputStream;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.BulkData;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;



import org.dcm4che3.io.BulkDataDescriptor;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;


public class DicomImageReader { 
	
private ImageInputStream iis;

private Attributes ds;

private DicomMetaData metadata;

private int frames;

private int width;

private int height;

private BulkData pixeldata;

private final VR.Holder pixeldataVR = new VR.Holder();


private int samples;


private int bitsAllocated;

private int bitsStored;

private boolean banded;
private int dataType;

private int frameLength;

private PhotometricInterpretation pmi;

public int getBitsStored() {
	return bitsStored;
}

public boolean getBanded() {
	return banded;
}

public BulkData getPixeldata() {
	return pixeldata;
}

public int getSamples() {
	return samples;
}

public int getBitsAllocated() {
	return bitsAllocated;
}

public int getFrameLength() {
	return frameLength;
}

public PhotometricInterpretation getPmi() {
	return pmi;
}


public void open(File src) throws Exception{
	resetInternalState();
	iis = new FileImageInputStream(src);
	readMetadata();
}

public int getNumImages() {
    return frames;
}


public int getWidth() {
   
   return width;
}


public int getHeight()  {
    return height;
}

public int getDataType(){
	
	return dataType;
}



public Attributes getAttributes(){
	
	return ds;
}


public DicomMetaData getMetadata()  {
    return metadata;
}


public boolean canReadRaster() {
    return true;
}


public Raster readRaster(int frameIndex)
        throws Exception {
    readMetadata();
    checkIndex(frameIndex);
        iis.setByteOrder(ds.bigEndian()
                ? ByteOrder.BIG_ENDIAN
                : ByteOrder.LITTLE_ENDIAN);
        iis.seek(pixeldata.offset + frameIndex * frameLength);
        Raster raster = new Raster(width,height,dataType);
        if(dataType==Raster.TYPE_BYTE){
        	byte[] data = raster.getByteData();
        	iis.readFully(data,0,data.length);
        }else{
        	short[] data = raster.getShortData();
            iis.readFully(data, 0, data.length);
        }
        
       return raster;
      
}


public byte[] extractOverlay(int gg0000, Raster raster) {
    Attributes attrs = metadata.getAttributes();

    if (attrs.getInt(Tag.OverlayBitsAllocated | gg0000, 1) == 1)
        return null;

    int ovlyRows = attrs.getInt(Tag.OverlayRows | gg0000, 0);
    int ovlyColumns = attrs.getInt(Tag.OverlayColumns | gg0000, 0);
    int bitPosition = attrs.getInt(Tag.OverlayBitPosition | gg0000, 0);

    int mask = 1<<bitPosition;
    int length = ovlyRows * ovlyColumns;

    byte[] ovlyData = new byte[(((length+7)>>>3)+1)&(~1)] ;
    Overlays.extractFromPixeldata(raster, mask, ovlyData, 0, length);
    return ovlyData;
}





public void applyOverlay(int gg0000, Raster raster,
        int frameIndex, DicomImageReadParam param, int outBits, byte[] ovlyData) {
    Attributes ovlyAttrs = metadata.getAttributes();
    int grayscaleValue = 0xffff;
    if (param instanceof DicomImageReadParam) {
        DicomImageReadParam dParam = (DicomImageReadParam) param;
        Attributes psAttrs = dParam.getPresentationState();
        if (psAttrs != null) {
            if (psAttrs.containsValue(Tag.OverlayData | gg0000))
                ovlyAttrs = psAttrs;
            grayscaleValue = Overlays.getRecommendedDisplayGrayscaleValue(
                    psAttrs, gg0000);
        } else
            grayscaleValue = dParam.getOverlayGrayscaleValue();
    }
    Overlays.applyOverlay(ovlyData != null ? 0 : frameIndex, raster,
            ovlyAttrs, gg0000, grayscaleValue >>> (16-outBits), ovlyData);
}

public int[] getActiveOverlayGroupOffsets(DicomImageReadParam param) {
    if (param instanceof DicomImageReadParam) {
        DicomImageReadParam dParam = (DicomImageReadParam) param;
        Attributes psAttrs = dParam.getPresentationState();
        if (psAttrs != null)
            return Overlays.getActiveOverlayGroupOffsets(psAttrs);
        else
            return Overlays.getActiveOverlayGroupOffsets(
                    metadata.getAttributes(),
                    dParam.getOverlayActivationMask());
    }
    return Overlays.getActiveOverlayGroupOffsets(
            metadata.getAttributes(),
            0xffff);
}

public Raster applyLUTs(Raster raster,
        int frameIndex, DicomImageReadParam dParam, int outBits) {
    Attributes imgAttrs = metadata.getAttributes();
    StoredValue sv = StoredValue.valueOf(imgAttrs);
    LookupTableFactory lutParam = new LookupTableFactory(sv);
   if(dParam==null)
            dParam=new DicomImageReadParam();
    Attributes psAttrs = dParam.getPresentationState();
    if (psAttrs != null) {
        lutParam.setModalityLUT(psAttrs);
        lutParam.setVOI(
                selectVOILUT(psAttrs,
                        imgAttrs.getString(Tag.SOPInstanceUID),
                        frameIndex+1),
                0, 0, false);
        lutParam.setPresentationLUT(psAttrs);
    } else {
        Attributes sharedFctGroups = imgAttrs.getNestedDataset(
                Tag.SharedFunctionalGroupsSequence);
        Attributes frameFctGroups = imgAttrs.getNestedDataset(
                Tag.PerFrameFunctionalGroupsSequence, frameIndex);
        lutParam.setModalityLUT(
                selectFctGroup(imgAttrs, sharedFctGroups, frameFctGroups,
                        Tag.PixelValueTransformationSequence));
        if (dParam.getWindowWidth() != 0) {
            lutParam.setWindowCenter(dParam.getWindowCenter());
            lutParam.setWindowWidth(dParam.getWindowWidth());
        } else
            lutParam.setVOI(
                selectFctGroup(imgAttrs, sharedFctGroups, frameFctGroups,
                        Tag.FrameVOILUTSequence),
                dParam.getWindowIndex(),
                dParam.getVOILUTIndex(),
                dParam.isPreferWindow());
        if (dParam.isAutoWindowing())
            lutParam.autoWindowing(imgAttrs, raster);
        lutParam.setPresentationLUT(imgAttrs);
    }
    LookupTable lut = lutParam.createLUT(outBits);
    Raster destRaster= new Raster(width, height, Raster.TYPE_INT);
    lut.lookup(raster, destRaster);
    return destRaster;
}

/**
 * 对读取的数据调窗转化为8位灰度图
 * @param frameIndex
 * @param ww
 * @param wc
 * @return
 * @throws Exception
 */
public Raster applyWindowCenter(int frameIndex,int ww,int wc) throws Exception{
	checkIndex(frameIndex);
	Raster raster = readRaster(frameIndex);
    Attributes imgAttrs = metadata.getAttributes();
    StoredValue sv = StoredValue.valueOf(imgAttrs);
    LookupTableFactory lutParam = new LookupTableFactory(sv);
    DicomImageReadParam dParam = new DicomImageReadParam();
    Attributes psAttrs = dParam.getPresentationState();
    dParam.setWindowCenter(wc);
    dParam.setWindowWidth(ww);
    if (psAttrs != null) {
        lutParam.setModalityLUT(psAttrs);
        lutParam.setVOI(
                selectVOILUT(psAttrs,
                        imgAttrs.getString(Tag.SOPInstanceUID),
                        frameIndex+1),
                0, 0, false);
        lutParam.setPresentationLUT(psAttrs);
    } else {
        Attributes sharedFctGroups = imgAttrs.getNestedDataset(
                Tag.SharedFunctionalGroupsSequence);
        Attributes frameFctGroups = imgAttrs.getNestedDataset(
                Tag.PerFrameFunctionalGroupsSequence, frameIndex);
        lutParam.setModalityLUT(
                selectFctGroup(imgAttrs, sharedFctGroups, frameFctGroups,
                        Tag.PixelValueTransformationSequence));
        if (dParam.getWindowWidth() != 0) {
            lutParam.setWindowCenter(dParam.getWindowCenter());
            lutParam.setWindowWidth(dParam.getWindowWidth());
        } else
            lutParam.setVOI(
                selectFctGroup(imgAttrs, sharedFctGroups, frameFctGroups,
                        Tag.FrameVOILUTSequence),
                dParam.getWindowIndex(),
                dParam.getVOILUTIndex(),
                dParam.isPreferWindow());
      
        lutParam.setPresentationLUT(imgAttrs);
    }
    LookupTable lut = lutParam.createLUT(8);
    Raster destRaster= new Raster(width, height, Raster.TYPE_BYTE);
    lut.lookup(raster, destRaster);
    return destRaster;
}

private Attributes selectFctGroup(Attributes imgAttrs,
        Attributes sharedFctGroups, 
        Attributes frameFctGroups,
        int tag) {
    if (frameFctGroups == null) {
        return imgAttrs;
    }
    Attributes group = frameFctGroups.getNestedDataset(tag);
    if (group == null && sharedFctGroups != null) {
        group = sharedFctGroups.getNestedDataset(tag);
    }
    return group != null ? group : imgAttrs;
}

private Attributes selectVOILUT(Attributes psAttrs, String iuid, int frame) {
    Sequence voiLUTs = psAttrs.getSequence(Tag.SoftcopyVOILUTSequence);
    if (voiLUTs != null)
        for (Attributes voiLUT : voiLUTs) {
            Sequence refImgs = voiLUT.getSequence(Tag.ReferencedImageSequence);
            if (refImgs == null || refImgs.isEmpty())
                return voiLUT;
            for (Attributes refImg : refImgs) {
                if (iuid.equals(refImg.getString(Tag.ReferencedSOPInstanceUID))) {
                    int[] refFrames = refImg.getInts(Tag.ReferencedFrameNumber);
                    if (refFrames == null)
                        return voiLUT;

                    for (int refFrame : refFrames)
                        if (refFrame == frame)
                            return voiLUT;
                }
            }
        }
    return null;
}

private void readMetadata() throws Exception {
    if (metadata != null)
        return;
    
    if (iis == null)
        throw new IllegalStateException("Input not set");
    @SuppressWarnings("resource")
	DicomInputStream dis = new DicomInputStream(new ImageInputStreamAdapter(iis));
    dis.setIncludeBulkData(IncludeBulkData.URI);
    dis.setBulkDataDescriptor(BulkDataDescriptor.PIXELDATA);
    dis.setURI("java:iis"); // avoid copy of pixeldata to temporary file
    Attributes fmi = dis.readFileMetaInformation();
    Attributes ds = dis.readDataset(-1, -1);
    setMetadata(new DicomMetaData(fmi, ds));
}

private void setMetadata(DicomMetaData metadata) throws Exception {
    this.metadata = metadata;
    this.ds = metadata.getAttributes();
    Object pixeldata = ds.getValue(Tag.PixelData, pixeldataVR );
    if (pixeldata != null) {
        frames = ds.getInt(Tag.NumberOfFrames, 1);
        width = ds.getInt(Tag.Columns, 0);
        height = ds.getInt(Tag.Rows, 0);
        samples = ds.getInt(Tag.SamplesPerPixel, 1);
        banded = samples > 1 && ds.getInt(Tag.PlanarConfiguration, 0) != 0;
        bitsAllocated = ds.getInt(Tag.BitsAllocated, 8);
        bitsStored = ds.getInt(Tag.BitsStored, bitsAllocated);
        dataType = bitsAllocated <= 8 ? Raster.TYPE_BYTE 
                                      : Raster.TYPE_USHORT;
        pmi = PhotometricInterpretation.fromString(
                ds.getString(Tag.PhotometricInterpretation, "MONOCHROME2"));
        if (pixeldata instanceof BulkData) {
            this.frameLength = pmi.frameLength(width, height, samples, bitsAllocated);
            this.pixeldata = (BulkData) pixeldata;
        } else {
        	throw new Exception("不支持压缩等其它格式");
           
        }
    }
   
}



private void resetInternalState() {
    metadata = null;
    ds = null;
    frames = 0;
    width = 0;
    height = 0;
    pixeldata = null;
    pmi = null;
}

private void checkIndex(int frameIndex) {
    if (frames == 0)
        throw new IllegalStateException("Missing Pixel Data");
    
    if (frameIndex < 0 || frameIndex >= frames)
        throw new IndexOutOfBoundsException("imageIndex: " + frameIndex);
}


public void close() throws IOException {
    resetInternalState();
    if(iis!=null)
    	iis.close();
}

}