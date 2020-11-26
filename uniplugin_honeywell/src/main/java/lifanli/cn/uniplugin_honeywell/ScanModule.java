package lifanli.cn.uniplugin_honeywell;

import android.app.Activity;
import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniDestroyableModule;

public class ScanModule extends UniDestroyableModule {

    @UniJSMethod(uiThread = true)
    public void addScanListener(UniJSCallback jsCallback){
        if (mUniSDKInstance.getContext() instanceof Activity) {
            HoneywellDevice honeywellDevice = new HoneywellDevice(mUniSDKInstance.getContext(), jsCallback);
            honeywellDevice.init();
        }
    }

    @UniJSMethod(uiThread = true)
    public void dismiss() {
        destroy();
    }

    @Override
    public void destroy() {

    }
}
