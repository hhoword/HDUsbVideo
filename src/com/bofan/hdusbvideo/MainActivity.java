package com.bofan.hdusbvideo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.usb.UsbManager;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.empia_lib.jar.EMPIA_LIB;

public class MainActivity extends Activity implements OnClickListener,SurfaceHolder.Callback{

	private SurfaceView surfaceView;  
	private Button btnStart;
	private SurfaceHolder surfaceHolder;
	private EMPIA_LIB empialib = null;
	private boolean RUN_THREAD = true;
	private boolean isWrite = false;
	private UsbManager localUsbManager;
	int width = 1280;  
	int height = 720;  
	int framerate = 20;  
	//	int bitrate = 2500000;  
	//	byte[] h264 = new byte[width*height*3/2];
	//	private Bitmap bitmap;  
	private MediaCodec mediaCodec;
	private FileOutputStream fos;
	private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.CHINA);

	public static final int EMPIA_VIDEOIN_COMPOSITE = 0x00;
	public static final int EMPIA_VIDEOIN_COMPONENT = 0x01;
	public static final int EMPIA_VIDEOIN_COMPONENT_P = 0x02;
	public static final int EMPIA_VIDEOIN_SVIDEO = 0x03;
	public static final int EMPIA_VIDEOIN_HDMI = 0x04    ;

	public static final int EMPIA_VIDEOSTD_NTSC   = 0x01;
	public static final int EMPIA_VIDEOSTD_PAL    = 0x02;
	public static final int EMPIA_VIDEOSTD_SECAM  = 0x03;	


	@SuppressLint("NewApi")
	public void onCreate(Bundle savedInstanceState) {  
		super.onCreate(savedInstanceState);  
		requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
		// 设置横屏显示
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main);    
		Log.i("bofan", "create");
		surfaceView=(SurfaceView) this.findViewById(R.id.surfaceView1);
		btnStart =(Button)this.findViewById(R.id.btnStart);
		localUsbManager = (UsbManager)getSystemService("usb");
		empialib = new EMPIA_LIB(localUsbManager);
		empialib.init();
		//设置SurfaceView自己不管理的缓冲区  
		surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); 
		surfaceHolder = surfaceView.getHolder(); // 绑定SurfaceView，取得SurfaceHolder对象  
		surfaceHolder.setFixedSize(width, height); // 预览大小設置  
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);  
		surfaceView.getHolder().addCallback(this);     
		btnStart.setOnClickListener(this);

	}  

	@Override  
	public void onClick(View v) {   
		switch (v.getId()) {
		case R.id.btnStart:
			if(isWrite){
				isWrite = false;
				btnStart.setText("开启录像");
				try {
					fos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Toast.makeText(this, "保存数据成功", Toast.LENGTH_SHORT).show();
			}else{
				isWrite = true;
				btnStart.setText("停止录像");
			}
			break;

		default:
			break;
		}
	}  

	@Override  
	protected void onPause() {    
		super.onPause();  
	}  

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		RUN_THREAD = false;
		if(isWrite){
			isWrite = false;
			try {
				if(fos!=null)
					fos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		empialib.capture_stop();
	}

	@SuppressLint("NewApi")
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		this.surfaceHolder = holder;
		Log.i("bofan", "surface create");
		empialib.set_inputsource(EMPIA_VIDEOIN_COMPOSITE);
		empialib.set_videostandard(EMPIA_VIDEOSTD_NTSC, false);

		empialib.set_brightness(0x70);
		empialib.set_contrast(0x20);
		empialib.set_saturation(0x35);

		boolean res =  empialib.capture_start();
		Log.i("bofan", "empialib start:"+res);

		try {
			mediaCodec = MediaCodec.createDecoderByType("video/avc");
			MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);  
			mediaCodec.configure(mediaFormat, surfaceHolder.getSurface(), null, 0);  
			mediaCodec.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  

		if(res){   	    
			DrawThread drawThread = new DrawThread();
			drawThread.start();
		}

	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub

	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub

	}  

	public class DrawThread extends Thread{
		@SuppressLint("NewApi")
		@Override
		public void run() {
			// TODO Auto-generated method stub
			int len = 0;
			byte[] databuf = new byte[width * height];
			long[] VideoReturnVal = new long[4];
			//			Canvas canvas = null;   //Canvas的引用  
			int mCount = 0;
			ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
			ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
			while(RUN_THREAD){
				VideoReturnVal = empialib.read_video_data(databuf);
				len = (int)VideoReturnVal[0]; 
				if(len > 0){
					//					onFrame(databuf, 0, len);

					int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
					if (inputBufferIndex >= 0) {
						ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
						inputBuffer.clear();
						inputBuffer.put(databuf);
						// long sample_time = ;
						mediaCodec.queueInputBuffer(inputBufferIndex, 0, len, mCount * 1000000 / 20, 0);
						++mCount;
					} else {
						Log.d("bofan", "dequeueInputBuffer error");
					}

					ByteBuffer outputBuffer = null;
					MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
					int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
					while (outputBufferIndex >= 0) {
						outputBuffer = outputBuffers[outputBufferIndex];
						mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
						outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);

					}
					if (outputBufferIndex >= 0) {
						mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
					} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
						outputBuffers = mediaCodec.getOutputBuffers();
						Log.d("Fuck", "outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
					} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
						// Subsequent data will conform to new format.
						Log.d("Fuck", "outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
					}
					if(isWrite){
						wirteToFile(databuf);
					}
				}
				Log.d("eMPIAh264Webcam", "Video pts : " + VideoReturnVal[1]);
				Log.d("eMPIAh264Webcam", "get_brightness : " + empialib.get_brightness());
				Log.d("eMPIAh264Webcam", "get_contrast : " + empialib.get_contrast());
				Log.d("eMPIAh264Webcam", "get_saturation : " + empialib.get_saturation());
			}	

		}
	}
	/*int mCount=0;
	@SuppressLint("NewApi") 
	public void onFrame(byte[] buf, int offset, int length) {  
		ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();  
		int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);  
		if (inputBufferIndex >= 0) {  
			ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];  
			inputBuffer.clear();  
			inputBuffer.put(buf, offset, length);  
			mediaCodec.queueInputBuffer(inputBufferIndex, 0, length, mCount * 1000000 / framerate, 0);  
			mCount++;  
		}  

		MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();  
		int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,0);  
		while (outputBufferIndex >= 0) {  
			mediaCodec.releaseOutputBuffer(outputBufferIndex, true);  
			outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);  
		}  
	} */
	
	List<String> sdList = getExtSDCardPath();
	String path;
	private void wirteToFile(byte[] buffer){
		try {
			String time = formatter.format(new Date());
			String fileName =  time  + ".mp4";
			if(sdList.size()>2){
				path = sdList.get(1)+ "/高清摄像头";
			}else{
				path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/高清摄像头";
			}
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				File dir = new File(path);

				if (!dir.exists()) {
					dir.mkdirs();
				}
				if(fos == null){
					fos = new FileOutputStream(path +"/"+ fileName);
				}
				fos.write(buffer);
				if(!isWrite){
					fos.close();
					Toast.makeText(MainActivity.this, "保存数据成功",Toast.LENGTH_SHORT).show();
					Log.e("bofan", "保存数据成功");
				}
			}else{
				Log.e("bofan", "没有找到sd卡");
				/*new Handler().post(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						Toast.makeText(MainActivity.this, "没有找到sd卡", Toast.LENGTH_SHORT).show();
					}
				});*/
			}

		} catch (Exception e) {
			Log.e("bofan", "an error occured while writing file...", e);
			new Handler().post(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					Toast.makeText(MainActivity.this, "保存数据失败", Toast.LENGTH_SHORT).show();
				}
			});
		}

	}
	
	/**
	 * 获取外置SD卡路径
	 * @return	应该就一条记录或空
	 */
	public List<String> getExtSDCardPath()
	{
		List<String> lResult = new ArrayList<String>();
		try {
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec("mount");
			InputStream is = proc.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("extSdCard"))
				{
					String [] arr = line.split(" ");
					String path = arr[1];
					File file = new File(path);
					if (file.isDirectory())
					{
						lResult.add(path);
					}
				}
			}
			isr.close();
		} catch (Exception e) {
		}
		return lResult;
	}
}
