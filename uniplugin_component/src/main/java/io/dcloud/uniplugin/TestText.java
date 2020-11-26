package io.dcloud.uniplugin;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.ui.action.BasicComponentData;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXComponentProp;
import com.taobao.weex.ui.component.WXVContainer;

import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeDeviceConnectionEvent;
import com.honeywell.aidc.BarcodeFailureEvent;
import com.honeywell.aidc.BarcodeReadEvent;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.BarcodeReaderInfo;
import com.honeywell.aidc.ScannerNotClaimedException;
import com.honeywell.aidc.ScannerUnavailableException;
import com.honeywell.aidc.TriggerStateChangeEvent;
import com.honeywell.aidc.UnsupportedPropertyException;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestText extends WXComponent<TextView>
        implements BarcodeReader.BarcodeListener, BarcodeReader.TriggerListener, AidcManager.BarcodeDeviceListener{

    public TestText(WXSDKInstance instance, WXVContainer parent, BasicComponentData basicComponentData) {
        super(instance, parent, basicComponentData);
    }
    public static final String TAG = "example_demo";
    private AidcManager mAidcManager;
    private BarcodeReader mBarcodeReader;
    private BarcodeReader mInternalScannerReader;
    private boolean mKeyPressed = false;
    private TextView mTextView;

    private Handler handler;


    @Override
    protected TextView initComponentHostView(Context context) {
        TextView textView = new TextView(context);

        AidcManager.create(context, new MyCreatedCallback());
        handler = new Handler(context.getMainLooper());

        return textView;
    }

    @WXComponentProp(name = "tel")
    public void setTel(String telNumber) {
        getHostView().setText("setTel: " + telNumber);
        Map<String, Object> params = new HashMap<>();
        Map<String, Object> number = new HashMap<>();
        number.put("tel", telNumber);
        params.put("detail", number);
        fireEvent("onTel", params);
    }

    private void runOnUiThread(Runnable r) {
        handler.post(r);
    }

    @JSMethod
    public void clearTel() {
        getHostView().setText("");
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
    }

    @Override
    public void onActivityPause() {
        super.onActivityPause();
    }

    @Override
    public void onActivityDestroy() {
        super.onActivityDestroy();
    }

    class MyCreatedCallback implements AidcManager.CreatedCallback {
        MyCreatedCallback() {
        }

        @Override
        public void onCreated(AidcManager aidcManager) {
            Log.d(TAG, "MyCreatedCallback onCreate !!!");
            mAidcManager = aidcManager;
            mAidcManager.addBarcodeDeviceListener(TestText.this);
            initAllBarcodeReaderAndSetDefault();
        }
    }

    void initAllBarcodeReaderAndSetDefault() {
        List<BarcodeReaderInfo> readerList = mAidcManager.listBarcodeDevices();
        Log.d(TAG, "initAllBarcodeReaderAndSetDefault readerList = "+readerList);
        mInternalScannerReader = null;

        for (BarcodeReaderInfo reader : readerList) {
            if ("dcs.scanner.imager".equals(reader.getName())) {
                mInternalScannerReader = initBarcodeReader(mInternalScannerReader, reader.getName());
            }
        }

        Log.d(TAG, "initAllBarcodeReaderAndSetDefault mInternalScannerReader = "+mInternalScannerReader);

        if (mInternalScannerReader != null) {
            mBarcodeReader = mInternalScannerReader;
        }
        else {
            Log.d(TAG, "No reader find");
        }
        if (mBarcodeReader != null) {
            try {
                mBarcodeReader.addBarcodeListener(this);
                mBarcodeReader.addTriggerListener(this);
            }
            catch (Throwable e2) {
                e2.printStackTrace();
            }
            try {
                mBarcodeReader.setProperty(BarcodeReader.PROPERTY_NOTIFICATION_GOOD_READ_ENABLED, true);
                mBarcodeReader.setProperty(BarcodeReader.PROPERTY_NOTIFICATION_BAD_READ_ENABLED, true);

                mBarcodeReader.setProperty(BarcodeReader.PROPERTY_CODE_39_ENABLED, true);
                mBarcodeReader.setProperty(BarcodeReader.PROPERTY_EAN_13_ENABLED, true);
                mBarcodeReader.setProperty(BarcodeReader.PROPERTY_EAN_8_ENABLED, true);
                mBarcodeReader.setProperty(BarcodeReader.PROPERTY_CODE_39_FULL_ASCII_ENABLED, true);
                mBarcodeReader.setProperty(BarcodeReader.PROPERTY_CODE_93_ENABLED, true);
                mBarcodeReader.setProperty(BarcodeReader.PROPERTY_QR_CODE_ENABLED, true);
                mBarcodeReader.setProperty(BarcodeReader.PROPERTY_DATA_PROCESSOR_LAUNCH_BROWSER, false);
                mBarcodeReader.setProperty(BarcodeReader.PROPERTY_QR_CODE_MAXIMUM_LENGTH, 256);
                mBarcodeReader.setProperty(BarcodeReader.PROPERTY_QR_CODE_MINIMUM_LENGTH, 32);

            } catch (UnsupportedPropertyException e) {
                e.printStackTrace();
            }

        }
    }

    BarcodeReader initBarcodeReader(BarcodeReader mReader, String mReaderName) {
        if (mReader == null) {
            if (mReaderName == null) {
                mReader = mAidcManager.createBarcodeReader();
            } else {
                mReader = mAidcManager.createBarcodeReader(mReaderName);
            }
            try {
                mReader.claim();
                Log.d(TAG, "Call DCS interface claim() " + mReaderName);
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
            }
            try {
                mReader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE, BarcodeReader.TRIGGER_CONTROL_MODE_CLIENT_CONTROL);
                mReader.setProperty(BarcodeReader.PROPERTY_QR_CODE_ENABLED, true);
                mReader.setProperty(BarcodeReader.PROPERTY_QR_CODE_MAXIMUM_LENGTH, 256);
                mReader.setProperty(BarcodeReader.PROPERTY_QR_CODE_MINIMUM_LENGTH, 32);

            } catch (UnsupportedPropertyException e2) {
                e2.printStackTrace();
            }
        }
        return mReader;
    }

    public void onBarcodeDeviceConnectionEvent(BarcodeDeviceConnectionEvent event) {
        Log.d(TAG, event.getBarcodeReaderInfo() + " Connection status: " + event.getConnectionStatus());
    }

    public void onBarcodeEvent(final BarcodeReadEvent event) {
        runOnUiThread(new Runnable() {
            public void run() {
                Log.d(TAG, "Enter onBarcodeEvent ==> " + event.getBarcodeData());
                String barcodeDate = new String(event.getBarcodeData().getBytes(event.getCharset()));
                Log.d(TAG, "Enter onBarcodeEvent ==> " + barcodeDate);
                setTel(barcodeDate);
//
//                getHostView().setText("tel12222: " + barcodeDate);
//
//                Map<String, Object> params = new HashMap<>();
//                Map<String, Object> number = new HashMap<>();
//                number.put("tel", barcodeDate);
//                //目前uni限制 参数需要放入到"detail"中 否则会被清理
//                params.put("detail", barcodeDate);
//
//                fireEvent("onTel", params);
            }
        });
    }

    public void onFailureEvent(final BarcodeFailureEvent event) {
        Log.d(TAG, "Enter onFailureEvent ===> " + event.getTimestamp());
    }

    public void onTriggerEvent(TriggerStateChangeEvent event) {
        Log.d(TAG, "onTriggerEvent looger");

        if (event.getState() == true) {
            try {
                mBarcodeReader.light(true);        //开启补光
                mBarcodeReader.aim(true);        //开启瞄准线
                mBarcodeReader.decode(true);        //开启解码
            } catch (Exception e) {

            }
        } else if (event.getState() == false) {
            try {
                mBarcodeReader.light(false);        //关闭补光
                mBarcodeReader.aim(false);        //关闭瞄准线
                mBarcodeReader.decode(false);    //关闭解码
            } catch (Exception e) {

            }
        }
        Log.d(TAG, "OnTriggerEvent status: " + event.getState());
        Log.d(TAG, "完成");
    }


    void doScan(boolean do_scan) {
        Log.d(TAG, "123123");
        try {
            if (do_scan) {
                Log.d(TAG, "Start a new Scan!");
            } else {
                Log.d(TAG, "Cancel last Scan!");
            }
            mBarcodeReader.decode(do_scan);
        } catch (ScannerNotClaimedException e) {
            Log.e(TAG, "catch ScannerNotClaimedException",e);
            e.printStackTrace();
        } catch (ScannerUnavailableException e2) {
            Log.e(TAG, "catch ScannerUnavailableException",e2);
            e2.printStackTrace();
        } catch (Exception e3) {
            e3.printStackTrace();
        }
    }
}
