package org.ljl.dcm4che_android;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.dcm4che3.android.Raster;
import org.dcm4che3.android.RasterUtil;
import org.dcm4che3.android.imageio.dicom.DicomImageReader;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

public class MainActivity extends Activity {

	Button btnLoad;
	
	EditText editText;
	ImageView imageView;
	String testFileName ;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		bindViews();

	}

	void bindViews() {
		btnLoad = (Button) findViewById(R.id.btnLoad);
		editText = (EditText) findViewById(R.id.editText);
		imageView = (ImageView) findViewById(R.id.imageView);
		btnLoad.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				btnLoadClicked();

			}
		});
	}

	void btnLoadClicked() {
		//testFileName=getExternalCacheDir().getAbsolutePath()+"/test1.dcm";
		testFileName =this.getCacheDir().getAbsolutePath()+"/test1.dcm";
		File file = new File(testFileName);
		if (file.exists())
			file.delete();
		InputStream is;
		try {
			is = getAssets().open("test1.dcm");
			copyFile(is, file);
			readDicom(testFileName);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	void readDicom(String fileName) {
		DicomImageReader dr = new DicomImageReader();
		StringBuffer sb = new StringBuffer();
		try {
			sb.append("dicom=");
			sb.append(fileName);
			sb.append("\n");

			dr.open(new File(fileName));
			int f = dr.getNumImages();
			sb.append("Frames=");
			sb.append(f);
			sb.append(",");
			int w = dr.getWidth();
			sb.append("width=");
			sb.append(w);
			sb.append(",");
			int h = dr.getHeight();
			sb.append("height=");
			sb.append(h);
			Attributes ds = dr.getAttributes();
			String wc = ds.getString(Tag.WindowCenter);
			String ww = ds.getString(Tag.WindowWidth);

			sb.append(",ww=");
			sb.append(ww);
			sb.append(",wc=");
			sb.append(wc);

			Raster raster = dr.applyWindowCenter(0, 400, 40);
			Bitmap bmp=RasterUtil.gray8ToBitmap(raster.getWidth(), raster.getHeight(), raster.getByteData());

			imageView.setImageBitmap(bmp);

		} catch (Exception e) {
			sb.append(e.getMessage());
			e.printStackTrace();
		}

		editText.setText(sb.toString());
	}

	void copyFile(InputStream is, File dstFile) {
		try {

			BufferedInputStream bis = new BufferedInputStream(is);
			BufferedOutputStream bos = new BufferedOutputStream(
					new FileOutputStream(dstFile), 1024);
			byte buf[] = new byte[1024];
			int c = 0;
			c = bis.read(buf);
			while (c > 0) {
				bos.write(buf, 0, c);
				c = bis.read(buf);
			}
			bis.close();
			bos.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
