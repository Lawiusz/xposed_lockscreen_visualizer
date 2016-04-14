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

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
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

    //private static int timesLogged = 0;

    private static boolean mmScreenOn = false;

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
                            VisualizerWrapper.init(mContext, modContext, mBackdrop);
                        }
                    KeyguardStateMonitor monitor = KeyguardStateMonitor.getInstance(loader);
                    mVisualizerView = VisualizerWrapper.getVisualizerView();
                    if (mVisualizerView != null) {
                        mVisualizerView.setKeyguardMonitor(monitor);
                    } else {
                        LLog.e("VisualizerView is null!");
                    }

                }
            });
            hookUpdateMediaMetaData(phoneStatusBar, xPreferences);
        } catch (Throwable e){
            LLog.e(e);
        }

    }

    private static void hookUpdateMediaMetaData(final Class phoneStatusBar,
                                                 final XSharedPreferences xPreferences) {

        XposedHelpers.findAndHookMethod(phoneStatusBar, "notifyNavigationBarScreenOn",
                boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            mmScreenOn = (boolean) param.args[0];
                            if (!mmScreenOn && mVisualizerView != null){
                                mVisualizerView.setPlaying(false);
                            } else {
                                XposedHelpers.callMethod(param.thisObject, "updateMediaMetaData", true);
                            }
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

                        MediaController mMediaController = (MediaController)
                                XposedHelpers.getObjectField(param.thisObject, "mMediaController");
                        MediaMetadata mMediaMetadata = (MediaMetadata)
                                XposedHelpers.getObjectField(param.thisObject, "mMediaMetadata");

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
                        boolean shouldDisplayVisualizer = !mKeyguardFadingAway && backdropBitmap != null
                                && mmScreenOn;

                        if (mVisualizerView != null) {
                            if (shouldDisplayVisualizer) {
                                boolean playing = mMediaController != null
                                        && mMediaController.getPlaybackState() != null
                                        && mMediaController.getPlaybackState().getState()
                                        == PlaybackState.STATE_PLAYING;
                                if (playing){
                                    if (!xPreferences.getBoolean(PREF_AUTOCOLOR, true)) {
                                        color = xPreferences.getInt(PREF_COLOR, 1234567890);
                                        if (color != 1234567890) {
                                            mVisualizerView.setColor(color);
                                        } else {
                                            mVisualizerView.setBitmap(backdropBitmap);
                                            LLog.e("No color to use with visualizer. Falling back to backdrop");
                                        }
                                    } else {
                                        mVisualizerView.setBitmap(backdropBitmap);
                                    }
                                }
                                mVisualizerView.setPlaying(playing);
                                //if (playing && timesLogged <=5) {
                                //    LLog.d(mVisualizerView.getDebugValues());
                                //    timesLogged++;
                                //}
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

                            } else {
                                mVisualizerView.setPlaying(false);
                            }
                        }
                    }
        });
    }

}
