/*
    This file is part of lockscreenvisualizerxposed.

    lockscreenvisualizerxposed is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
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

import de.robv.android.xposed.XposedBridge;

class LLog {
    private LLog(){}
    private static final String TAG = "LXVISUALIZER";
    static void d(String msg){
        if (BuildConfig.DEBUG) {
            XposedBridge.log("D/" + TAG + ": " + msg);
        }
    }
    static void e(String msg) {
        XposedBridge.log("E/" + TAG + ": " + msg);
    }
    static void e(Throwable err){
        XposedBridge.log("E/" + TAG +": " + err.getMessage());
        XposedBridge.log(err);
    }
}
