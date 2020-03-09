package com.example.taptap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.graphics.Bitmap;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.example.taptap.ui.main.SectionsPagerAdapter;
import com.google.android.material.tabs.TabLayout;

import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

import SecuGen.FDxSDKPro.*;

public class MainActivity extends AppCompatActivity implements SGFingerPresentEvent {
    private EditText mEditLog;
    private static final String TAG = "SecuGen USB";
    private PendingIntent mPermissionIntent;
    private IntentFilter filter;
    private IntentFilter filter2;
    private JSGFPLib sgfplib;
    private boolean usbPermissionRequested;
    private boolean bSecuGenDeviceOpened;
    private int mImageWidth;
    private int mImageHeight;
    private int mImageDPI;
    private byte[] mRegisterTemplate;
    private byte[] mVerifyTemplate;
    private byte[] mRegisterImage;
    private int[] mMaxTemplateSize;
    private boolean mLed;
    private int nCaptureModeN;
    private SGAutoOnEventNotifier autoOn;
    private boolean mAutoOnEnabled;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    long dwTimeStart = 0, dwTimeEnd = 0, dwTimeElapsed = 0;
    AlertDialog.Builder dlgAlert;
    AlertDialog alert;
    private  UsbDevice usbDevice;
    long error;
//    private void debugMessage(String message) {
//       // this.mEditLog.append(message);
//        //this.mEditLog.invalidate(); 
//    }


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Toast.makeText(getApplicationContext(), "USB Device Inserted Now", Toast.LENGTH_SHORT).show();
            //error = sgfplib.Init(SGFDxDeviceName.SG_DEV_AUTO);
            alert.cancel();
            error = sgfplib.Init(SGFDxDeviceName.SG_DEV_AUTO);
            usbDevice = sgfplib.GetUsbDevice();
            askSGPermissions();
            //Log.d(TAG,"Enter mUsbReceiver.onReceive()");
//            Toast.makeText(getApplicationContext(), "error code of Init: "+error, Toast.LENGTH_LONG).show();

        }

    };

    private final BroadcastReceiver mUsbPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(ACTION_USB_PERMISSION.equals(action)){ //TODO Check for USB Permission intent
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            Log.d(TAG, "Vendor ID : " + device.getVendorId() + "\n");//
                            Log.d(TAG, "Product ID: " + device.getProductId() + "\n");
                            Toast.makeText(getApplicationContext(), "Vendor ID : " + device.getVendorId() + "\n" + "Product ID: " + device.getProductId() + "\n", Toast.LENGTH_LONG).show();

//                            debugMessage("USB BroadcastReceiver VID : " + device.getVendorId() + "\n");
//                            debugMessage("USB BroadcastReceiver PID: " + device.getProductId() + "\n");

//                            Toast.makeText(getApplicationContext(),"Hello Javatpoint",Toast.LENGTH_LONG).show();
                            checkUSBDeviceAndPermissionGranted();
                        } else
                            Log.d(TAG, "mUsbReceiver.onReceive() Device is null");
                    } else
                        Log.d(TAG, "mUsbReceiver.onReceive() permission denied for device " + device);
                }
            }
        }


    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
//Toast.makeTe
//        Toast.makeText(getApplicationContext(), "This context value : "+ this, Toast.LENGTH_LONG).show();
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
//        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(UsbManager.ACTION_USB_DEVICE_ATTACHED), 0);
        filter2 = new IntentFilter(ACTION_USB_PERMISSION);
       filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);

        sgfplib = new JSGFPLib((UsbManager) getSystemService(Context.USB_SERVICE));
        autoOn = new SGAutoOnEventNotifier(sgfplib, this);
        Log.d(TAG,"JSGFPLib version: " + sgfplib.GetJSGFPLibVersion() + "\n");
        mLed = false;
        mAutoOnEnabled = true;
        //autoOn = new SGAutoOnEventNotifier(sgfplib, this);
        nCaptureModeN = 0;
        Log.d(TAG, "Exit onCreate()");
        mMaxTemplateSize=new int[1];

        //Instantiate dialog box
        dlgAlert = new AlertDialog.Builder(this);

        //Finally call register the broadcast to our filter(requirement) and start checking usb device
        registerReceiver(mUsbReceiver, filter);
        registerReceiver(mUsbPermissionReceiver, filter2);
        error = sgfplib.Init(SGFDxDeviceName.SG_DEV_AUTO);
        usbDevice = sgfplib.GetUsbDevice();
        checkUSBDeviceAndPermissionGranted();
    }



    public void SGFingerPresentCallback (){
        //autoOn.stop();
        //byte []  tempFingerPrint;
        CaptureFingerPrint();
        Log.d(TAG, "Finger Present here");
        //long res=0;
       // res = sgfplib.GetImageEx(tempFingerPrint,2000, 50);
        //fingerDetectedHandler.sendMessage(new Message());
    }

    public void CaptureFingerPrint(){
       mRegisterImage = new byte[mImageWidth*mImageHeight];
//        long result = sgfplib.GetImage(buffer);
        if (mRegisterImage != null)
            mRegisterImage = null;
        mRegisterImage = new byte[mImageWidth*mImageHeight];

        //this.mCheckBoxMatched.setChecked(false);
        dwTimeStart = System.currentTimeMillis();
        long result = sgfplib.GetImageEx(mRegisterImage, 2000, 100);
        //DumpFile("register.raw", mRegisterImage);
        dwTimeEnd = System.currentTimeMillis();
        dwTimeElapsed = dwTimeEnd-dwTimeStart;
        Log.d(TAG,"GetImage() ret:" + result + " [" + dwTimeElapsed +"ms "+ dwTimeStart +"ms "+ dwTimeEnd+ "ms]\n");

        runThroughUIThread("", new Callable<Void>() {
                    public Void call() {
                        ImageView mImageViewFingerprint = findViewById(R.id.RegisterImage);
                        mImageViewFingerprint.setImageBitmap(this.toGrayscale(mRegisterImage));
                        return null;
                    }

                    Bitmap toGrayscale(byte[] mImageBuffer)
                    {
                        byte[] Bits = new byte[mImageBuffer.length * 4];
                        for (int i = 0; i < mImageBuffer.length; i++) {
                            Bits[i * 4] = Bits[i * 4 + 1] = Bits[i * 4 + 2] = mImageBuffer[i]; // Invert the source bits
                            Bits[i * 4 + 3] = -1;// 0xff, that's the alpha.
                        }

                        Bitmap bmpGrayscale = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
                        bmpGrayscale.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
                        return bmpGrayscale;
                    }
                }
        );
//        ImageView mImageViewFingerprint = findViewById(R.id.RegisterImage);
//        mImageViewFingerprint.setImageBitmap(this.toGrayscale(mRegisterImage));
        dwTimeStart = System.currentTimeMillis();
//            result = sgfplib.SetTemplateFormat(SecuGen.FDxSDKPro.SGFDxTemplateFormat.TEMPLATE_FORMAT_ISO19794);
        result = sgfplib.SetTemplateFormat(SecuGen.FDxSDKPro.SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);
        dwTimeEnd = System.currentTimeMillis();
        dwTimeElapsed = dwTimeEnd-dwTimeStart;
//            debugMessage("SetTemplateFormat(ISO19794) ret:" +  result + " [" + dwTimeElapsed + "ms]\n");
        Log.d(TAG,"SetTemplateFormat(SG400) ret:" +  result + " [" + dwTimeElapsed + "ms]\n");

//        String NFIQString = "";
        int quality1[] = new int[1];
        result = sgfplib.GetImageQuality(mImageWidth, mImageHeight, mRegisterImage, quality1);
        //long nfiq = sgfplib.ComputeNFIQ(mRegisterImage, mImageWidth, mImageHeight);
        long nfiq = sgfplib.ComputeNFIQEx(mRegisterImage, mImageWidth, mImageHeight,mImageDPI);
//        NFIQString =  new String("NFIQ="+ nfiq);
        Log.d(TAG,"NFIQ: "+nfiq);
        Log.d(TAG,"GetImageQuality() ret:" +  result + "quality :" + quality1[0]+ "\n");
        //return mRegisterImage;
        if (nfiq < 3)
            runThroughUIThread("", new Callable<Void>() {
                public Void call() {
                    Toast.makeText(getApplicationContext(), "Quality level not appropriate..., Try Again", Toast.LENGTH_SHORT).show();
                    return null;
                }
            });
        else
            runThroughUIThread("", new Callable<Void>() {
                public Void call() {
                    Toast.makeText(getApplicationContext(), "Fingerprint Registered", Toast.LENGTH_LONG).show();
                    return null;
                }
            });

    }

    public void runThroughUIThread(String simpleParam, final Callable<Void> methodParam) {
        //your logic code [...]

        //call methodParam
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        methodParam.call();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
    }

//    private void setImage(final byte [] mRegisterImage){
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                ImageView mImageViewFingerprint = findViewById(R.id.RegisterImage);
//                mImageViewFingerprint.setImageBitmap(this.toGrayscale(mRegisterImage));
//            }
//
//            Bitmap toGrayscale(byte[] mImageBuffer)
//            {
//                byte[] Bits = new byte[mImageBuffer.length * 4];
//                for (int i = 0; i < mImageBuffer.length; i++) {
//                    Bits[i * 4] = Bits[i * 4 + 1] = Bits[i * 4 + 2] = mImageBuffer[i]; // Invert the source bits
//                    Bits[i * 4 + 3] = -1;// 0xff, that's the alpha.
//                }
//
//                Bitmap bmpGrayscale = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
//                bmpGrayscale.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
//                return bmpGrayscale;
//            }
//        });
//
//    }


//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
////        setIntent(intent);
//        Toast.makeText(getApplicationContext(), "At NewIntent "+UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())+"  :"+ intent.getAction(), Toast.LENGTH_LONG).show();
//        Log.d(TAG,"At NewIntent "+UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())+"  :"+ intent.getAction());
//        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
//            Log.d(TAG,"Usb device inserted");
//            registerReceiver(mUsbReceiver, filter);
//        }
//    }

    protected void onResume() {
        super.onResume();
//        this.onNewIntent(this.getIntent());

        Log.d(TAG, "Enter onResume()");

    }

    protected void checkUSBDeviceAndPermissionGranted(){

    if (error != SGFDxErrorCode.SGFDX_ERROR_NONE) {
        //AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);

        if (error == SGFDxErrorCode.SGFDX_ERROR_DEVICE_NOT_FOUND)
            dlgAlert.setMessage("The attached fingerprint device is not supported on Android");
        else
            dlgAlert.setMessage("Fingerprint device failed!");
        dlgAlert.setTitle("SecuGen Fingerprint SDK");
        dlgAlert.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // finish();
                        return;
                    }
                }
        );
        dlgAlert.setCancelable(false);
//        dlgAlert.create()
        alert = dlgAlert.create();
        alert.setCanceledOnTouchOutside(true);
        alert.show();
    } else {
//        UsbDevice usbDevice = sgfplib.GetUsbDevice();
        if (usbDevice == null) {
            //AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
            dlgAlert.setMessage("SecuGen fingerprint sensor not found!");
            dlgAlert.setTitle("SecuGen Fingerprint SDK");
            dlgAlert.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            return;
                        }
                    }
            );
            dlgAlert.setCancelable(false);

//            dlgAlert.create().show();
            alert = dlgAlert.create();
            alert.setCanceledOnTouchOutside(true);
            alert.show();
        } else {
            boolean hasPermission = sgfplib.GetUsbManager().hasPermission(usbDevice);
            if (!hasPermission) {
                if (!usbPermissionRequested) {
                    Log.e(TAG, "Requesting USB Permission\n ");
                    //Log.d(TAG, "Call GetUsbManager().requestPermission()");
                    askSGPermissions();
//                    usbPermissionRequested = true;
//                    sgfplib.GetUsbManager().requestPermission(usbDevice, mPermissionIntent);
                } else {
                    //wait up to 20 seconds for the system to grant USB permission
                    hasPermission = sgfplib.GetUsbManager().hasPermission(usbDevice);
                    Log.e(TAG, "Waiting for USB Permission\n");
                    int i = 0;
                    while ((hasPermission == false) && (i <= 40)) {
                        ++i;

                        hasPermission = sgfplib.GetUsbManager().hasPermission(usbDevice);
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //Log.d(TAG, "Waited " + i*50 + " milliseconds for USB permission");
                    }

                }
            }
            if (hasPermission)
                Log.d(TAG, "Opening SecuGen Device\n");
            error = sgfplib.OpenDevice(0);
            Log.d(TAG, "OpenDevice() ret: " + error + "\n");
            if (error == SGFDxErrorCode.SGFDX_ERROR_NONE) {
                getDeviceInfo();
            }
            else {
                Log.d(TAG, "Waiting for USB Permission\n");
            }

        }
    }

}


    protected void getDeviceInfo(){
            long error;
            bSecuGenDeviceOpened = true;
            SecuGen.FDxSDKPro.SGDeviceInfoParam deviceInfo = new SecuGen.FDxSDKPro.SGDeviceInfoParam();
            error = sgfplib.GetDeviceInfo(deviceInfo);

            Log.d(TAG, "GetDeviceInfo() ret: " + error + "\n");
            mImageWidth = deviceInfo.imageWidth;
            mImageHeight = deviceInfo.imageHeight;
            mImageDPI = deviceInfo.imageDPI;
            Log.d(TAG, "Image width: " + mImageWidth + "\n");
            Log.d(TAG, "Image height: " + mImageHeight + "\n");
            Log.d(TAG, "Image resolution: " + mImageDPI + "\n");
            Log.d(TAG, "Serial Number: " + new String(deviceInfo.deviceSN()) + "\n");
            //sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_ISO19794);
            //sgfplib.GetMaxTemplateSize(mMaxTemplateSize);
            //Log.d(TAG,"")("TEMPLATE_FORMAT_ISO19794 SIZE: " + mMaxTemplateSize[0] + "\n");
            sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);
            sgfplib.GetMaxTemplateSize(mMaxTemplateSize);
            Log.d(TAG, "TEMPLATE_FORMAT_SG400 SIZE: " + mMaxTemplateSize[0] + "\n");
            mRegisterTemplate = new byte[(int) mMaxTemplateSize[0]];
            mVerifyTemplate = new byte[(int) mMaxTemplateSize[0]];
            //EnableControls();
            //boolean smartCaptureEnabled = this.mToggleButtonSmartCapture.isChecked();
            //if (smartCaptureEnabled)
            //sgfplib.WriteData(SGFDxConstant.WRITEDATA_COMMAND_ENABLE_SMART_CAPTURE, (byte)1);
            //else
            sgfplib.WriteData(SGFDxConstant.WRITEDATA_COMMAND_ENABLE_SMART_CAPTURE, (byte) 1);
            if (mAutoOnEnabled) {
                autoOn.start();

            }
            //CaptureFingerPrint();

    }
    protected void askSGPermissions(){
        usbPermissionRequested = true;
        sgfplib.GetUsbManager().requestPermission(usbDevice, mPermissionIntent);
    }
}

