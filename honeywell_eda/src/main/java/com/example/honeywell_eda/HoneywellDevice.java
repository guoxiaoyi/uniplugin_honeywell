package com.example.honeywell_eda;


import android.content.Context;
import android.support.annotation.NonNull;

import com.alibaba.fastjson.JSONObject;
import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeDeviceConnectionEvent;
import com.honeywell.aidc.BarcodeFailureEvent;
import com.honeywell.aidc.BarcodeReadEvent;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.BarcodeReaderInfo;
import com.honeywell.aidc.ScannerUnavailableException;
import com.honeywell.aidc.TriggerStateChangeEvent;
import com.honeywell.aidc.UnsupportedPropertyException;

import java.util.List;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniDestroyableModule;
import io.dcloud.feature.uniapp.utils.UniLogUtils;

public class HoneywellDevice  extends UniDestroyableModule
        implements AidcManager.BarcodeDeviceListener, BarcodeReader.BarcodeListener, BarcodeReader.TriggerListener {

    Context mContext;

    private AidcManager mAidcManager;
    private BarcodeReader mBarcodeReader;
    private BarcodeReader mInternalScannerReader;
    private UniJSCallback uniJSCallback;

    public HoneywellDevice(@NonNull Context context, UniJSCallback jsCallback) {
        mContext = context;
        uniJSCallback = jsCallback;
    }

    public void init() {
        AidcManager.create(mContext, new MyCreatedCallback());
    }

    @Override
    public void onBarcodeDeviceConnectionEvent(BarcodeDeviceConnectionEvent event) {
        UniLogUtils.w("onBarcodeDeviceConnectionEvent");
    }

    @UniJSMethod (uiThread = false)
    public void onBarcodeEvent(BarcodeReadEvent barcodeReadEvent) {
        JSONObject data = new JSONObject();
        data.put("detail", barcodeReadEvent.getBarcodeData());
        uniJSCallback.invokeAndKeepAlive(data);
        UniLogUtils.w("Enter onBarcodeEvent ==> " + barcodeReadEvent.getBarcodeData());
    }

    @Override
    public void onFailureEvent(BarcodeFailureEvent barcodeFailureEvent) {

    }

    @Override
    public void onTriggerEvent(TriggerStateChangeEvent triggerStateChangeEvent) {
        UniLogUtils.w("onTriggerEvent");
        if (triggerStateChangeEvent.getState() == true) {
            try {
                UniLogUtils.w("开启扫描");
                mBarcodeReader.light(true);         //开启补光
                mBarcodeReader.aim(true);           //开启瞄准线
                mBarcodeReader.decode(true);        //开启解码
            } catch (Exception e) {
                UniLogUtils.w("开启扫描失败");
            }
        } else if (triggerStateChangeEvent.getState() == false) {
            try {
                UniLogUtils.w("onTriggerEvent FALSE");
                mBarcodeReader.light(false);        //关闭补光
                mBarcodeReader.aim(false);        //关闭瞄准线
                mBarcodeReader.decode(false);    //关闭解码
            } catch (Exception e) {
                UniLogUtils.w("关闭扫描失败");
            }
        }

    }

    @Override
    public void destroy() {

    }

    class MyCreatedCallback implements AidcManager.CreatedCallback {
        MyCreatedCallback() {
        }

        @Override
        public void onCreated(AidcManager aidcManager) {
            UniLogUtils.w("onSubProcessCreate onCreated!!!!");
            mAidcManager = aidcManager;
            mAidcManager.addBarcodeDeviceListener(HoneywellDevice.this);
            initAllBarcodeReaderAndSetDefault();
        }
    }
    void initAllBarcodeReaderAndSetDefault() {
        List<BarcodeReaderInfo> readerList = mAidcManager.listBarcodeDevices();
        UniLogUtils.w("initAllBarcodeReaderAndSetDefault readerList = "+readerList);

        mInternalScannerReader = null;

        for (BarcodeReaderInfo reader : readerList) {
            if ("dcs.scanner.imager".equals(reader.getName())) {
                mInternalScannerReader = initBarcodeReader(mInternalScannerReader, reader.getName());
            }
        }
        UniLogUtils.w("initAllBarcodeReaderAndSetDefault mInternalScannerReader = "+mInternalScannerReader);

        if (mInternalScannerReader != null) {
            mBarcodeReader = mInternalScannerReader;
        }
        else {
            UniLogUtils.w("No reader find");
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
                mBarcodeReader.setProperty(BarcodeReader.PROPERTY_DATA_PROCESSOR_LAUNCH_BROWSER, false);
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
                UniLogUtils.w("Call DCS interface claim()");
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
            }
            try {
                mReader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE, BarcodeReader.TRIGGER_CONTROL_MODE_CLIENT_CONTROL);

            } catch (UnsupportedPropertyException e2) {
                e2.printStackTrace();
            }
        }
        return mReader;
    }
}
