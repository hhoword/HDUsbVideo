package com.bofan.hdusbvideo;

import java.util.Iterator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class SplanshActivity extends Activity{

	private Button button1;
	private UsbManager localUsbManager;
	private boolean isExist = false;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_splansh);
		button1 = (Button)findViewById(R.id.button1);
		button1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(isExist){
					Intent intent = new Intent(SplanshActivity.this, MainActivity.class);
					startActivity(intent);
				}else{
					Toast.makeText(SplanshActivity.this, "没有找到USB设备", Toast.LENGTH_SHORT).show();
				}
			}
		});
		localUsbManager = (UsbManager)getSystemService("usb");
		Iterator localIterator = localUsbManager.getDeviceList().values().iterator();
		if (!localIterator.hasNext()){
			isExist = false;
			return;
		}
		isExist = true;
		UsbDevice localUsbDevice = (UsbDevice)localIterator.next();
		if ((localUsbDevice.getVendorId() == 60186) || ((localUsbDevice.getVendorId() == 7040) && (localUsbDevice.getProductId() == 58893))){
			if (localUsbManager.hasPermission(localUsbDevice)){
				UsbInterface usbInterface = localUsbDevice.getInterface(0);  
				//USBEndpoint为读写数据所需的节点  
				//				UsbEndpoint inEndpoint = usbInterface.getEndpoint(0);  //读数据节点  
				//				UsbEndpoint outEndpoint = usbInterface.getEndpoint(1); //写数据节点  
				//				UsbDeviceConnection connection = localUsbManager.openDevice(localUsbDevice);  
				//				connection.claimInterface(usbInterface, true); 
				if(localUsbManager.hasPermission(localUsbDevice)){
					UsbDeviceConnection mUsbDeviceConnection=localUsbManager.openDevice(localUsbDevice);
					if(mUsbDeviceConnection==null){
						Toast.makeText(this, "未成功连接设备", Toast.LENGTH_SHORT).show();
						return ;
					}
					if(mUsbDeviceConnection.claimInterface(usbInterface, true)){
						//	                    Toast.makeText(this, "设备连接成功", Toast.LENGTH_SHORT).show();
						//						int a = mUsbDeviceConnection.controlTransfer(0x40, 0xB2, 0, 0, null, 0, 0);
						//						int b = mUsbDeviceConnection.controlTransfer(0x40, 0xB4, 0, 0, null, 0, 0);
						//						if(a<0||b<0){
						//							Toast.makeText(this, "控制传输失败",Toast.LENGTH_SHORT).show();
						//							return ;
						//						}
						byte[] arrayOfByte1 = new byte[1];
						byte[] arrayOfByte2 = new byte[2];
						byte[] arrayOfByte3 = new byte[1];
						byte[] arrayOfByte4 = new byte[2];
						byte[] arrayOfByte5 = new byte[1];
						a(mUsbDeviceConnection, 18, arrayOfByte1);
						if ((0x40 & (0xFF & arrayOfByte1[0])) >> 6 == 0) {
							arrayOfByte4[0] = 40;
							arrayOfByte4[1] = 0;
							a(mUsbDeviceConnection, 160, arrayOfByte4, arrayOfByte5);
							if ((0xFF & arrayOfByte5[0]) == 185){
								a(mUsbDeviceConnection, 128, 223);
								a(mUsbDeviceConnection, 132, arrayOfByte1);
								arrayOfByte2[0] = 6;
								arrayOfByte2[1] = -1;
								b(mUsbDeviceConnection, 144, arrayOfByte2);
								arrayOfByte3[0] = 6;
								a(mUsbDeviceConnection, 144, arrayOfByte3, arrayOfByte5);
								arrayOfByte2[0] = 7;
								arrayOfByte2[1] = -1;
								b(mUsbDeviceConnection, 144, arrayOfByte2);
								arrayOfByte3[0] = 7;
								a(mUsbDeviceConnection, 144, arrayOfByte3, arrayOfByte5);
							}
						}else  if ((0xFF & arrayOfByte5[0]) == 185) {
						      a(mUsbDeviceConnection, 128, 31);
						      a(mUsbDeviceConnection, 132, arrayOfByte1);
						      Log.d("Suspend Service", "ACTION_SCREEN_ON 0x80 : 0x" + Integer.toHexString(0xFF & arrayOfByte1[0]));
						      arrayOfByte2[0] = 6;
						      arrayOfByte2[1] = 0;
						      b(mUsbDeviceConnection, 144, arrayOfByte2);
						      arrayOfByte3[0] = 6;
						      a(mUsbDeviceConnection, 144, arrayOfByte3, arrayOfByte5);
						      Log.d("Suspend Service", "ACTION_SCREEN_ON IT6604 0x06 : 0x" + Integer.toHexString(0xFF & arrayOfByte5[0]));
						      arrayOfByte2[0] = 7;
						      arrayOfByte2[1] = 0;
						      b(mUsbDeviceConnection, 144, arrayOfByte2);
						      arrayOfByte3[0] = 7;
						      a(mUsbDeviceConnection, 144, arrayOfByte3, arrayOfByte5);
						      Log.d("Suspend Service", "ACTION_SCREEN_ON IT6604 0x07 : 0x" + Integer.toHexString(0xFF & arrayOfByte5[0]));
						    }
						mUsbDeviceConnection.close();
					}else{
						mUsbDeviceConnection.close();
						return ;
					}
				}else{
					PendingIntent localPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent("com.empia.USB_PERMISSION"), 0);
					IntentFilter localIntentFilter = new IntentFilter("com.empia.USB_PERMISSION");
					registerReceiver(mUsbReceiver, localIntentFilter);
					//				this.bU = true;
					localUsbManager.requestPermission(localUsbDevice, localPendingIntent);
				}
			}
		}
	}

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

		@SuppressLint("NewApi")
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if ("com.empia.USB_PERMISSION".equals(action)) {
				synchronized (this) {
					UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						if(device != null){
							//call method to set up device communication
							if (localUsbManager.hasPermission(device)){
								if ((device.getVendorId() == 60186) || ((device.getVendorId() == 7040) && (device.getProductId() == 58893))){
									UsbInterface usbInterface = device.getInterface(0);  
									//USBEndpoint为读写数据所需的节点  
									//UsbDeviceConnection connection = localUsbManager.openDevice(device);  
									//connection.claimInterface(usbInterface, true);  
									if(localUsbManager.hasPermission(device)){
										UsbDeviceConnection mUsbDeviceConnection=localUsbManager.openDevice(device);
										if(mUsbDeviceConnection==null){
											Toast.makeText(context, "未成功连接设备", Toast.LENGTH_SHORT).show();
											return ;
										}
										if(mUsbDeviceConnection.claimInterface(usbInterface, true)){
											//Toast.makeText(this, "设备连接成功", Toast.LENGTH_SHORT).show();
											//int a = mUsbDeviceConnection.controlTransfer(0x40, 0xB2, 0, 0, null, 0, 0);
											//int b = mUsbDeviceConnection.controlTransfer(0x40, 0xB4, 0, 0, null, 0, 0);
											//if(a<0||b<0){
											//	Toast.makeText(context, "控制传输失败",Toast.LENGTH_SHORT).show();
											//	return ;
											//}
										}else{
											mUsbDeviceConnection.close();
											return ;
										}
									}
								}
							}
						}
					}
					else {
						//						Log.d(TAG, "permission denied for device " + device);
					}
				}
			}
		}

	};

	@SuppressLint("NewApi")
	private int a(UsbDeviceConnection paramUsbDeviceConnection, int paramInt1, int paramInt2)
	{
		byte[] arrayOfByte = new byte[1];
		if (paramUsbDeviceConnection == null)
			return -1;
		arrayOfByte[0] = ((byte)paramInt2);
		return paramUsbDeviceConnection.controlTransfer(64, 0, 0, paramInt1, arrayOfByte, 1, 100);
	}

	@SuppressLint("NewApi")
	private int a(UsbDeviceConnection paramUsbDeviceConnection, int paramInt, byte[] paramArrayOfByte)
	{
		if (paramUsbDeviceConnection == null)
			return -1;
		return paramUsbDeviceConnection.controlTransfer(192, 0, 0, paramInt, paramArrayOfByte, 1, 100);
	}

	private static int j = 4;
	private static int k = 64;
	private static int l = 2;
	@SuppressLint("NewApi")
	private int a(UsbDeviceConnection paramUsbDeviceConnection, int paramInt, byte[] paramArrayOfByte1, byte[] paramArrayOfByte2)
	{
		byte[] arrayOfByte1 = new byte[1];
		byte[] arrayOfByte2;
		if ((paramArrayOfByte1.length > 0) && (paramArrayOfByte2.length > 0)){
			if (paramUsbDeviceConnection != null){
				a(paramUsbDeviceConnection, 6, arrayOfByte1);
				a(paramUsbDeviceConnection, 6, 0xFB & (0xFF & arrayOfByte1[0]) | k);
				int n = 100 + ((0xFF & paramArrayOfByte1[1]) << 8 | 0xFF & paramArrayOfByte1[0]);
				arrayOfByte2 = new byte[paramArrayOfByte1.length];
				arrayOfByte2[0] = ((byte)((0xFF00 & n) >> 8));
				arrayOfByte2[1] = ((byte)(n & 0xFF));
				if (paramInt != 160){
					paramUsbDeviceConnection.controlTransfer(64, l, 0, paramInt, paramArrayOfByte1, paramArrayOfByte1.length, 100);
					return paramUsbDeviceConnection.controlTransfer(192, l, 0, paramInt, paramArrayOfByte2, paramArrayOfByte2.length, 100);
				}else{
					a(paramUsbDeviceConnection, 6, 0x0 | k | j);
					paramUsbDeviceConnection.controlTransfer(64, l, 0, paramInt, arrayOfByte2, arrayOfByte2.length, 100);
					return paramUsbDeviceConnection.controlTransfer(192, l, 0, paramInt, paramArrayOfByte2, paramArrayOfByte2.length, 100);
				}
			}
		}
		return -1;
	}

	@SuppressLint("NewApi")
	private int b(UsbDeviceConnection paramUsbDeviceConnection, int paramInt, byte[] paramArrayOfByte){
		if (paramArrayOfByte.length > 0){
			a(paramUsbDeviceConnection, 6, 0x0 | k | j);
			if (paramUsbDeviceConnection != null);
		}else{
			return -1;
		}
		return paramUsbDeviceConnection.controlTransfer(64, l, 0, paramInt, paramArrayOfByte, paramArrayOfByte.length, 100);
	}

}
