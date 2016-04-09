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

import android.annotation.TargetApi;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Build;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

class KeyguardMod {

    private static final String PREF_ANTIDIMMER = "antidimmer";
    private static final String PREF_FRONTMOVER = "vis_in_front";
    private static final String PREF_AUTOCOLOR = "autocolor";
    private static final String PREF_COLOR="custcolor";

    private static final String CLASS_PHONE_STATUSBAR =
            "com.android.systemui.statusbar.phone.PhoneStatusBar";

    private static final String FIELD_VIS_BEHIND ="mBackdrop";
    private static final String FIELD_VIS_FRONT = "mKeyguardBottomArea";

    private static VisualizerView mVisualizerView;
    private static Context mContext;
    private static ViewGroup mBackdrop;

    private static int timesLogged = 0;

    private static boolean mmScreenOn;

    public static void init(final ClassLoader loader){
        try {
            final Class<?> phoneStatusBar = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR, loader);

            final String packageName = "pl.lawiusz.lockscreenvisualizerxposed";
            final XSharedPreferences xPreferences = new XSharedPreferences(packageName);
            xPreferences.makeWorldReadable();
            if (xPreferences.getBoolean("placeholder", true)) {
                LLog.e("Failed to load XSharedPreferences!");
            }

            XposedHelpers.findAndHookMethod(phoneStatusBar, "makeStatusBarView",
                    new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject,
                            "mContext");
                    Context modContext = mContext.createPackageContext(MainXposedMod.MOD_PACKAGE,
                            Context.CONTEXT_IGNORE_SECURITY);
                    VisualizerWrapper wrapper = null;

                    String lField;
                    if (xPreferences.getBoolean(PREF_FRONTMOVER, false)){
                        lField = FIELD_VIS_FRONT;
                    } else {
                        lField = FIELD_VIS_BEHIND;
                    }

                    // field mBackdrop => Visualizer goes behind keyguard ui
                    // field mKeyguardBottomArea => Visualizer goes in front of keyguard ui
                    mBackdrop = (ViewGroup) XposedHelpers.getObjectField(
                            param.thisObject, lField);

                        if (mBackdrop != null) {
                            wrapper = new VisualizerWrapper(mContext, modContext, mBackdrop);
                        }
                    KeyguardStateMonitor monitor = KeyguardStateMonitor.getInstance(loader);
                    assert wrapper != null;
                    mVisualizerView = wrapper.getVisualizerView();
                    if (mVisualizerView != null) {
                        mVisualizerView.setKeyguardMonitor(monitor);
                        mVisualizerView.setVisible();
                    } else {
                        LLog.e("VisualizerView is null!");
                    }

                }
            });
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
                hookUpdateMediaMetaDataL(phoneStatusBar, xPreferences);
            } else {
                hookUpdateMediaMetaDataM(phoneStatusBar, xPreferences);
            }


        } catch (Throwable e){
            LLog.e(e);
        }

    }

    @TargetApi(Build.VERSION_CODES.M)
    private static void hookUpdateMediaMetaDataM(final Class phoneStatusBar,
                                                 final XSharedPreferences xPreferences) throws Throwable {

        XposedHelpers.findAndHookMethod(phoneStatusBar, "notifyNavigationBarScreenOn",
                boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            mmScreenOn = (boolean) param.args[0];
                    }
                });

        XposedHelpers.findAndHookMethod(phoneStatusBar, "updateMediaMetaData",
                boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Bitmap backdropBitmap = null;
                        int color;
                        boolean mKeyguardFadingAway = XposedHelpers.getBooleanField(param.thisObject,
                                "mKeyguardFadingAway");
                        Boolean mScreenOn;
                        try {
                            mScreenOn = (Boolean) XposedHelpers.getObjectField(param.thisObject,
                                    "mScreenOn");
                        } catch (Throwable e){
                            mScreenOn = mmScreenOn;
                        }
                        MediaController mMediaController = (MediaController)
                                XposedHelpers.getObjectField(param.thisObject, "mMediaController");
                        MediaMetadata mMediaMetadata = (MediaMetadata)
                                XposedHelpers.getObjectField(param.thisObject, "mMediaMetadata");
                        int mState = XposedHelpers.getIntField(param.thisObject, "mState");
                        boolean keyguardVisible = (mState != StatusBarState.SHADE);

                        if (mMediaMetadata != null) {
                            backdropBitmap = mMediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
                            if (backdropBitmap == null) {
                                backdropBitmap =
                                        mMediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                                // might still be null
                            }
                        }
                        if (backdropBitmap == null){
                            // use default wallpaper
                            if (mContext == null){
                                mContext = (Context) XposedHelpers.getObjectField(param.thisObject,
                                        "mContext");
                            }
                            WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
                            Drawable wallpaperDrawable = wallpaperManager.getDrawable();
                            backdropBitmap = ((BitmapDrawable)wallpaperDrawable).getBitmap();
                        }

                        if (mVisualizerView != null) {
                            if (!mKeyguardFadingAway && keyguardVisible && backdropBitmap != null
                                    && mScreenOn) {
                                // if there's album art, ensure visualizer is visible
                                mVisualizerView.setVisible();
                                boolean playing = mMediaController != null
                                        && mMediaController.getPlaybackState() != null
                                        && mMediaController.getPlaybackState().getState()
                                        == PlaybackState.STATE_PLAYING;
                                mVisualizerView.setPlaying(playing);
                                if (BuildConfig.DEBUG && playing && timesLogged <=5) {
                                    LLog.d(mVisualizerView.getDebugValues());
                                    timesLogged++;
                                }
                                boolean antidimmerEnabled =
                                        xPreferences.getBoolean(PREF_ANTIDIMMER, false);
                                if (playing && antidimmerEnabled){
                                    mVisualizerView.setKeepScreenOn(true);
                                    if (mBackdrop != null){
                                        mBackdrop.setKeepScreenOn(true);
                                    }
                                } else {
                                    mVisualizerView.setKeepScreenOn(false);
                                    if (mBackdrop != null){
                                        mBackdrop.setKeepScreenOn(false);
                                    }
                                }

                            }
                            if (keyguardVisible) {
                                if (!xPreferences.getBoolean(PREF_AUTOCOLOR, true)) {
                                    color = xPreferences.getInt(PREF_COLOR, 1234567890);
                                    if (color != 1234567890) {
                                        mVisualizerView.setColor(color);
                                    } else if (backdropBitmap != null) {
                                        mVisualizerView.setBitmap(backdropBitmap);
                                        LLog.e("No color to use with visualizer. Falling back to backdrop");
                                    } else {
                                        LLog.e("No bitmap to use with visualizer. Falling back to default color");
                                        mVisualizerView.setColor(32767);
                                    }
                                } else if (backdropBitmap != null) {
                                    mVisualizerView.setBitmap(backdropBitmap);
                                } else {
                                    LLog.e("No bitmap to use with visualizer. Falling back to default color");
                                    mVisualizerView.setColor(32767);
                                }
                            }
                        }
                    }
                });
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void hookUpdateMediaMetaDataL(final Class phoneStatusBar,
                                                 final XSharedPreferences xPreferences) throws Throwable{
        XposedHelpers.findAndHookMethod(phoneStatusBar, "updateMediaMetaData",
                boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Bitmap backdropBitmap = null;
                int color;
                boolean mKeyguardFadingAway = XposedHelpers.getBooleanField(param.thisObject,
                        "mKeyguardFadingAway");
                Boolean mScreenOn = (Boolean) XposedHelpers.getObjectField(param.thisObject,
                        "mScreenOn");
                MediaController mMediaController = (MediaController)
                        XposedHelpers.getObjectField(param.thisObject, "mMediaController");
                MediaMetadata mMediaMetadata = (MediaMetadata)
                        XposedHelpers.getObjectField(param.thisObject, "mMediaMetadata");
                int mState = XposedHelpers.getIntField(param.thisObject, "mState");
                boolean keyguardVisible = (mState != StatusBarState.SHADE);

                if (mMediaMetadata != null) {
                    backdropBitmap = mMediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
                    if (backdropBitmap == null) {
                        backdropBitmap =
                                mMediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                        // might still be null
                    }
                }
                if (backdropBitmap == null){
                    // use default wallpaper
                    if (mContext == null){
                        mContext = (Context) XposedHelpers.getObjectField(param.thisObject,
                                "mContext");
                    }
                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
                    Drawable wallpaperDrawable = wallpaperManager.getDrawable();
                    backdropBitmap = ((BitmapDrawable)wallpaperDrawable).getBitmap();
                }

                if (mVisualizerView != null) {
                    if (!mKeyguardFadingAway && keyguardVisible && mScreenOn) {
                        mVisualizerView.setVisible();
                        boolean playing = mMediaController != null
                                && mMediaController.getPlaybackState() != null
                                && mMediaController.getPlaybackState().getState()
                                == PlaybackState.STATE_PLAYING;
                        mVisualizerView.setPlaying(playing);
                        if (BuildConfig.DEBUG && playing && timesLogged <=5) {
                            LLog.d(mVisualizerView.getDebugValues());
                            timesLogged++;
                        }
                        boolean antidimmerEnabled =
                                xPreferences.getBoolean(PREF_ANTIDIMMER, false);
                        if (playing && antidimmerEnabled){
                            mVisualizerView.setKeepScreenOn(true);
                            if (mBackdrop != null){
                                mBackdrop.setKeepScreenOn(true);
                            }
                        } else {
                            mVisualizerView.setKeepScreenOn(false);
                            if (mBackdrop != null){
                                mBackdrop.setKeepScreenOn(false);
                            }
                        }

                    }
                    if (keyguardVisible) {
                        if (!xPreferences.getBoolean(PREF_AUTOCOLOR, true)) {
                            color = xPreferences.getInt(PREF_COLOR, 1234567890);
                            if (color != 1234567890) {
                                mVisualizerView.setColor(color);
                            } else if (backdropBitmap != null) {
                                mVisualizerView.setBitmap(backdropBitmap);
                                LLog.e("No color to use with visualizer. Falling back to backdrop");
                            } else {
                                LLog.e("No bitmap to use with visualizer. Falling back to default color");
                                mVisualizerView.setColor(32767);
                            }
                        } else if (backdropBitmap != null) {
                            mVisualizerView.setBitmap(backdropBitmap);
                        } else {
                            LLog.e("No bitmap to use with visualizer. Falling back to default color");
                            mVisualizerView.setColor(32767);
                        }
                    }
                }
            }
        });
    }
}
