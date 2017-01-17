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

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.BatteryManager;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

class KeyguardMod {

    private static final String CLASS_PHONE_STATUSBAR =
            "com.android.systemui.statusbar.phone.PhoneStatusBar";
    private static final String FIELD_VIS_BEHIND ="mBackdrop";
    private static final String FIELD_VIS_FRONT = "mKeyguardBottomArea";

    private static VisualizerView mVisualizerView;
    //private static int timesLogged = 0;
    private static boolean mScreenOn;
    private static boolean registeredReceiver;
    private static Bitmap currentBitmap;
    private static SharedPreferences prefs;

    private static boolean keepScreenOn;
    private static boolean keepScreenOnBatt;
    private static boolean moveVisToFront;
    private static int visCustomColor = -1;

    private static final BroadcastReceiver settingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SettingsActivity.INTENT_ACTION_PREFS_CHANGED.equals(intent.getAction())){
                keepScreenOn = intent.getBooleanExtra(SettingsActivity.PREF_ANTIDIMMER, false);
                keepScreenOnBatt = intent
                        .getBooleanExtra(SettingsActivity.PREF_ANTIDIMMER_ON_BATT, false);
                moveVisToFront = intent.getBooleanExtra(SettingsActivity.PREF_FRONTMOVER, false);
                visCustomColor = intent.getIntExtra(SettingsActivity.PREF_COLOR, -1);
                if (prefs == null){
                    prefs = context.getSharedPreferences(MainXposedMod.MOD_PACKAGE, 0);
                }
                prefs.edit()
                        .putBoolean(SettingsActivity.PREF_ANTIDIMMER, keepScreenOn)
                        .putBoolean(SettingsActivity.PREF_ANTIDIMMER_ON_BATT, keepScreenOnBatt)
                        .putBoolean(SettingsActivity.PREF_FRONTMOVER, moveVisToFront)
                        .putInt(SettingsActivity.PREF_COLOR, visCustomColor)
                        .apply();

                if (mVisualizerView != null){
                    mVisualizerView.setKeepScreenOn(keepScreenOn && (keepScreenOnBatt
                            || isDevicePluggedIn(context)));
                    if (visCustomColor != -1){
                        mVisualizerView.setColor(visCustomColor);
                    } else if (currentBitmap != null) {
                        mVisualizerView.setBitmap(currentBitmap);
                    }
                }
            } else if (SettingsActivity.INTENT_ACTION_SUICIDE.equals(intent.getAction())){
                try {
                    // ensure that SharedPreferences are saved to disk
                    Class.forName("android.app.QueuedWork")
                            .getDeclaredMethod("waitToFinish")
                            .invoke(null);
                } catch (ReflectiveOperationException e) {
                    LLog.e(e);
                }
                System.exit(0);
            }
        }
    };

    static void init(final ClassLoader loader){
        try {
            Class<?> phoneStatusBar = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR, loader);
            XposedHelpers.findAndHookMethod(phoneStatusBar, "makeStatusBarView",
                    new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Context systemContext = (Context) XposedHelpers
                            .getObjectField(param.thisObject, "mContext");
                    Context modContext = systemContext.createPackageContext(
                            MainXposedMod.MOD_PACKAGE, Context.CONTEXT_IGNORE_SECURITY);

                    prefs = systemContext.getSharedPreferences(MainXposedMod.MOD_PACKAGE, 0);
                    keepScreenOn = prefs.getBoolean(SettingsActivity.PREF_ANTIDIMMER, false);
                    keepScreenOnBatt = prefs.
                            getBoolean(SettingsActivity.PREF_ANTIDIMMER_ON_BATT, false);
                    moveVisToFront = prefs.getBoolean(SettingsActivity.PREF_FRONTMOVER, false);
                    visCustomColor = prefs.getInt(SettingsActivity.PREF_COLOR, -1);

                    String backdropName = moveVisToFront ? FIELD_VIS_FRONT : FIELD_VIS_BEHIND;
                    // field mBackdrop => Visualizer goes behind keyguard ui
                    // field mKeyguardBottomArea => Visualizer goes in front of keyguard ui
                    ViewGroup backdrop = (ViewGroup) XposedHelpers.getObjectField(param.thisObject,
                            backdropName);
                    if (backdrop != null) {
                            VisualizerWrapper.init(systemContext, modContext, backdrop);
                    }
                    KeyguardStateMonitor monitor = KeyguardStateMonitor.getInstance(loader);
                    mVisualizerView = VisualizerWrapper.getVisualizerView();
                    if (mVisualizerView != null) {
                        mVisualizerView.setKeyguardMonitor(monitor);
                    } else {
                        LLog.e("VisualizerView is null!");
                    }
                    if (!registeredReceiver) {
                        IntentFilter filter
                                = new IntentFilter(SettingsActivity.INTENT_ACTION_PREFS_CHANGED);
                        filter.addAction(SettingsActivity.INTENT_ACTION_SUICIDE);
                        systemContext.registerReceiver(settingsReceiver, filter);
                        registeredReceiver = true;
                    }
                }
            });

            XposedHelpers.findAndHookMethod((Class) phoneStatusBar, "notifyNavigationBarScreenOn",
                    boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                mScreenOn = (boolean) param.args[0];
                                if (!mScreenOn && mVisualizerView != null){
                                    mVisualizerView.setPlaying(false);
                                } else {
                                    XposedHelpers.callMethod(param.thisObject,
                                            "updateMediaMetaData", true);
                                }
                        }
            });

            XposedHelpers.findAndHookMethod((Class) phoneStatusBar, "updateMediaMetaData",
                    boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            boolean mKeyguardFadingAway = XposedHelpers
                                    .getBooleanField(param.thisObject, "mKeyguardFadingAway");

                            MediaController mMediaController = (MediaController) XposedHelpers
                                    .getObjectField(param.thisObject, "mMediaController");
                            MediaMetadata mMediaMetadata = (MediaMetadata) XposedHelpers
                                    .getObjectField(param.thisObject, "mMediaMetadata");

                            if (mMediaMetadata != null) {
                                currentBitmap = mMediaMetadata
                                        .getBitmap(MediaMetadata.METADATA_KEY_ART);
                                if (currentBitmap == null) {
                                    currentBitmap = mMediaMetadata.getBitmap(
                                            MediaMetadata.METADATA_KEY_ALBUM_ART);
                                    // might still be null
                                }
                            }
                            if (currentBitmap == null){
                                // use default wallpaper
                                Context theirCtx = (Context) XposedHelpers.getObjectField(
                                        param.thisObject, "mContext");
                                WallpaperManager wallpaperManager
                                        = WallpaperManager.getInstance(theirCtx);
                                Drawable wallpaperDrawable = wallpaperManager.getDrawable();
                                currentBitmap = ((BitmapDrawable)wallpaperDrawable).getBitmap();
                            }
                            boolean shouldDisplayVisualizer = !mKeyguardFadingAway
                                    && currentBitmap != null
                                    && mScreenOn;

                            if (mVisualizerView != null) {
                                if (shouldDisplayVisualizer) {
                                    boolean playing = mMediaController != null
                                            && mMediaController.getPlaybackState() != null
                                            && mMediaController.getPlaybackState().getState()
                                            == PlaybackState.STATE_PLAYING;
                                    if (playing){
                                        if (visCustomColor != -1) {
                                            mVisualizerView.setColor(visCustomColor);
                                        } else {
                                            mVisualizerView.setBitmap(currentBitmap);
                                        }
                                    }
                                    mVisualizerView.setPlaying(playing);
                                    if (playing && keepScreenOn
                                            && (keepScreenOnBatt ||
                                            isDevicePluggedIn(mVisualizerView.getContext()))){
                                        mVisualizerView.setKeepScreenOn(true);
                                    } else {
                                        mVisualizerView.setKeepScreenOn(false);
                                    }

                                } else {
                                    mVisualizerView.setPlaying(false);
                                }
                            }
                        }
            });
        } catch (Throwable e){
            LLog.e(e);
        }
    }
    private static boolean isDevicePluggedIn(Context context){
        if (context == null) return false;
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        if (batteryStatus == null) return false;
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
    }

}
