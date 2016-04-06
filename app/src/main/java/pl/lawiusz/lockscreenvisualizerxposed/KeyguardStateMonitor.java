package pl.lawiusz.lockscreenvisualizerxposed;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class KeyguardStateMonitor {
    public static final String CLASS_KG_MONITOR = "com.android.systemui.statusbar.policy.KeyguardMonitor";
    private boolean mIsShowing;
    private boolean mIsSecured;
    private final List<Listener> mListeners = new ArrayList<>();
    private static KeyguardStateMonitor mInstance;

    private KeyguardStateMonitor(ClassLoader loader) {
        createHooks(loader);
    }

    private void createHooks(ClassLoader loader) {
        try {
            XposedHelpers.findAndHookMethod(CLASS_KG_MONITOR, loader,
                    "notifyKeyguardState", boolean.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                            boolean showing = XposedHelpers.getBooleanField(param.thisObject, "mShowing");
                            boolean secured = XposedHelpers.getBooleanField(param.thisObject, "mSecure");
                            if (showing != mIsShowing || secured != mIsSecured) {
                                mIsShowing = showing;
                                mIsSecured = secured;
                                notifyStateChanged();
                            }
                        }
                    });

        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    public static KeyguardStateMonitor getInstance(ClassLoader loader){
        if (mInstance == null){
            mInstance = new KeyguardStateMonitor(loader);
        }
        return  mInstance;
    }

    private void notifyStateChanged() {
        synchronized (mListeners) {
            for (Listener l : mListeners) {
                l.onKeyguardStateChanged();
            }
        }
    }

    public void registerListener(Listener l) {
        if (l == null) return;
        synchronized (mListeners) {
            if (!mListeners.contains(l)) {
                mListeners.add(l);
            }
        }
    }

    public void unregisterListener(Listener l) {
        if (l == null) return;
        synchronized (mListeners) {
            if (mListeners.contains(l)) {
                mListeners.remove(l);
            }
        }
    }

    public boolean isShowing() {
        return mIsShowing;
    }

    public interface Listener {
        void onKeyguardStateChanged();
    }
}
