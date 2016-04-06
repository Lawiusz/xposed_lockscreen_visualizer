package pl.lawiusz.lockscreenvisualizerxposed;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainXposedMod implements IXposedHookLoadPackage{
    public static final String MOD_PACKAGE = "pl.lawiusz.lockscreenvisualizerxposed";
    public static final String SYSTEMUI_PACKAGE = "com.android.systemui";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam)
            throws Throwable {

        if (lpparam.packageName.equals("android") &&
                lpparam.processName.equals("android")) {
            PermGrant.initAndroid(lpparam.classLoader);
        }

        if (lpparam.packageName.equals(SYSTEMUI_PACKAGE)) {
            KeyguardMod.init(lpparam.classLoader);
        }

    }
}