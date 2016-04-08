package pl.lawiusz.lockscreenvisualizerxposed;

import de.robv.android.xposed.XposedBridge;

public class LLog {
    private LLog(){}
    private static final String TAG = "LXVISUALIZER";
    public static void d(String msg){
        XposedBridge.log("D/" + TAG + ": " + msg);
    }
    public static void e(String msg) {
        XposedBridge.log("E/" + TAG + ": " + msg);
    }
    public static void e(Throwable err){
        XposedBridge.log("E/" + TAG +": " + err.getMessage());
        XposedBridge.log(err);
    }
}
