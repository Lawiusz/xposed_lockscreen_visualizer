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

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

@SuppressWarnings("WeakerAccess")
public class MainXposedMod implements IXposedHookLoadPackage {
    static final String MOD_PACKAGE = "pl.lawiusz.lockscreenvisualizerxposed";
    static final String SYSTEMUI_PACKAGE = "com.android.systemui";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam)
            throws Throwable {

        if (lpparam.packageName.equals("android") && lpparam.processName.equals("android")) {
            PermGrant.init(lpparam.classLoader);
        }

        if (lpparam.packageName.equals(SYSTEMUI_PACKAGE)) {
            KeyguardMod.init(lpparam.classLoader);
        }
        if (lpparam.packageName.equals(MOD_PACKAGE)){
            SelfMod.init(lpparam.classLoader);
        }

    }
}