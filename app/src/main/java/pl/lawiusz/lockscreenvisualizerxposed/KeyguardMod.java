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
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

class KeyguardMod {
    private static final String CLASS_PHONE_STATUSBAR = "com.android.systemui.statusbar.phone.PhoneStatusBar";
    private static VisualizerView mVisualizerView;
    private static Context mContext;

    public static void init(final ClassLoader loader){
        try {
            final Class<?> phoneStatusBar = XposedHelpers.findClass(CLASS_PHONE_STATUSBAR, loader);

            final String packageName = "pl.lawiusz.lockscreenvisualizerxposed";
            final XSharedPreferences xPreferences = new XSharedPreferences(packageName, MainActivity.PREFS_PUBLIC);
            xPreferences.makeWorldReadable();

            XposedHelpers.findAndHookMethod(phoneStatusBar, "makeStatusBarView", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    Context modContext = mContext.createPackageContext(MainXposedMod.MOD_PACKAGE,
                            Context.CONTEXT_IGNORE_SECURITY);
                    VisualizerWrapper wrapper = null;
                    ViewGroup kgStatusView = (ViewGroup) XposedHelpers.getObjectField(
                            param.thisObject, "mBackdrop");

                        if (kgStatusView != null) {
                            wrapper = new VisualizerWrapper(mContext, modContext, kgStatusView);
                        }
                    KeyguardStateMonitor monitor = KeyguardStateMonitor.getInstance(loader);
                    assert wrapper != null;
                    mVisualizerView = wrapper.getVisualizerView();
                    if (mVisualizerView != null) {
                        mVisualizerView.setKeyguardMonitor(monitor);
                        mVisualizerView.setPlaying(true);
                        mVisualizerView.setVisible();
                    } else {
                        XposedBridge.log("VisualizerView is null!");
                    }

                }
            });

            XposedHelpers.findAndHookMethod(phoneStatusBar, "updateMediaMetaData", boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Bitmap backdropBitmap = null;
                    boolean mKeyguardFadingAway = XposedHelpers.getBooleanField(param.thisObject, "mKeyguardFadingAway");
                    Boolean mScreenOn = (Boolean) XposedHelpers.getObjectField(param.thisObject, "mScreenOn");
                    MediaController mMediaController = (MediaController) XposedHelpers.getObjectField(param.thisObject, "mMediaController");
                    MediaMetadata mMediaMetadata = (MediaMetadata) XposedHelpers.getObjectField(param.thisObject, "mMediaMetadata");
                    int mState = XposedHelpers.getIntField(param.thisObject, "mState");
                    boolean keyguardVisible = (mState != StatusBarState.SHADE);

                    if (mMediaMetadata != null) {
                        backdropBitmap = mMediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
                        if (backdropBitmap == null) {
                            backdropBitmap = mMediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                            // might still be null
                        }
                    }
                    if (backdropBitmap == null){
                        // use default wallpaper
                        if (mContext == null){
                            mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        }
                        WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
                        Drawable wallpaperDrawable = wallpaperManager.getDrawable();
                        backdropBitmap = ((BitmapDrawable)wallpaperDrawable).getBitmap();
                    }

                    if (mVisualizerView != null) {
                        if (!mKeyguardFadingAway && keyguardVisible && backdropBitmap != null && mScreenOn) {
                            // if there's album art, ensure visualizer is visible
                            mVisualizerView.setVisible();
                            boolean playing = mMediaController != null
                                    && mMediaController.getPlaybackState() != null
                                    && mMediaController.getPlaybackState().getState()
                                    == PlaybackState.STATE_PLAYING;
                            mVisualizerView.setPlaying(playing);
                            boolean antidimmerEnabled = xPreferences.getBoolean(MainActivity.PREF_ANTIDIMMER, false);
                            if (playing && antidimmerEnabled){
                                mVisualizerView.setKeepScreenOn(true);
                            } else {
                                mVisualizerView.setKeepScreenOn(false);
                            }

                        }
                        if (keyguardVisible && backdropBitmap != null) {
                            // always use current backdrop to color eq
                            mVisualizerView.setBitmap(backdropBitmap);
                        } else if (backdropBitmap == null) {
                            XposedBridge.log("lol, no bitmap to use with visualizer");
                        }
                    }
                }
            });

        } catch (Throwable e){
            XposedBridge.log(e);
        }

    }
}
