/*
    Copyright (C) 2016-2017 Lawiusz Fras

    This file is part of lockscreenvisualizerxposed.

    lockscreenvisualizerxposed is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    lockscreenvisualizerxposed is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with lockscreenvisualizerxposed; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package pl.lawiusz.lockscreenvisualizerxposed;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

class KeyguardStateMonitor {
    private static final String CLASS_KG_MONITOR
            = "com.android.systemui.statusbar.policy.KeyguardMonitor";
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
                        protected void afterHookedMethod(final MethodHookParam param)
                                throws Throwable {
                            boolean showing = XposedHelpers
                                    .getBooleanField(param.thisObject, "mShowing");
                            boolean secured = XposedHelpers
                                    .getBooleanField(param.thisObject, "mSecure");
                            if (showing != mIsShowing || secured != mIsSecured) {
                                mIsShowing = showing;
                                mIsSecured = secured;
                                notifyStateChanged();
                            }
                        }
                    });

        } catch (Throwable t) {
            LLog.e(t);
        }
    }

    static KeyguardStateMonitor getInstance(ClassLoader loader){
        if (mInstance == null){
            mInstance = new KeyguardStateMonitor(loader);
        }
        return  mInstance;
    }

    @SuppressWarnings("Convert2streamapi")
    private void notifyStateChanged() {
        synchronized (mListeners) {
            for (Listener l : mListeners) {
                l.onKeyguardStateChanged();
            }
        }
    }

    void registerListener(Listener l) {
        if (l == null) return;
        synchronized (mListeners) {
            if (!mListeners.contains(l)) {
                mListeners.add(l);
            }
        }
    }

    void unregisterListener(Listener l) {
        if (l == null) return;
        synchronized (mListeners) {
            if (mListeners.contains(l)) {
                mListeners.remove(l);
            }
        }
    }

    boolean isShowing() {
        return mIsShowing;
    }

    interface Listener {
        void onKeyguardStateChanged();
    }
}
