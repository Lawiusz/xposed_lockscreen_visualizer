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

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.pes.androidmaterialcolorpickerdialog.ColorPicker;

import java.io.File;

public class SettingsActivity extends PreferenceActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "LXVISUALIZER";
    private static final String PREF_ANTIDIMMER = "antidimmer";
    private static final String PREF_FRONTMOVER = "vis_in_front";
    private static final String PREF_AUTOCOLOR = "autocolor";
    private static final String PREF_COLOR="custcolor";
    private static final String PREF_ABOUT = "about";
    private static final String PREF_XPOSED = "xposed_working";
    private static final String PREFS_PUBLIC = "pl.lawiusz.lockscreenvisualizerxposed_preferences";
    private static VisualizerView visualizerView;
    private static SharedPreferences prefsPublic;
    private static boolean prefsPublicSucc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsPublic = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_main);
        prefsPublic.edit().putBoolean("placeholder", false).apply();
        visualizerView = (VisualizerView) findViewById(R.id.visualizerview);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new GeneralPreferenceFragment()).commit();
    }
    @Override
    public void onPause(){
        super.onPause();
        if (visualizerView != null){
            visualizerView.setPlaying(false);
        }
        if (!prefsPublicSucc) {
            File prefsDir = new File(getApplicationInfo().dataDir, "shared_prefs");
            File prefsFile = new File(prefsDir, PREFS_PUBLIC + ".xml");

            if (prefsFile.exists()) {
                if (!prefsFile.setReadable(true, false)) {
                    Log.e(TAG, "Error accessing shared preferences!");
                } else {
                    prefsPublicSucc = true;
                }
            } else Log.e(TAG, "No shared preferences file!");
        }
    }
    @Override
    public void onResume(){
        super.onResume();
        final int color = prefsPublic.getInt(PREF_COLOR, 1234567890);
        if (prefsPublic.getBoolean(PREF_AUTOCOLOR, true)){
            if (isPermRecordGranted(this) && isPermModAudioGranted(this)) {
                if (visualizerView != null)setUpVisualizer();
            }
        } else {
            if (isPermRecordGranted(this) && isPermModAudioGranted(this)) {
                if (color == 1234567890){
                    if (visualizerView != null) setUpVisualizer();
                } else {
                    if (visualizerView != null) setUpVisualizer(color);
                }
            }
        }
        if (visualizerView != null) {
            visualizerView.setPlaying(true);
        }
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    @SuppressWarnings("SameReturnValue")
    private static Boolean isXposedWorking(){
        return Boolean.FALSE;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults){
        if (requestCode == 3){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (isPermModAudioGranted(this) && isPermRecordGranted(this)) {
                        if (visualizerView != null) {
                            setUpVisualizer();
                        }
                    }
                }
            } else {
                Toast.makeText(this, R.string.no_perms, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {

        @SuppressLint("SetWorldReadable")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Activity currentActivity = getActivity();
            File prefsDir = new File(currentActivity.getApplicationInfo().dataDir, "shared_prefs");
            File prefsFile = new File(prefsDir, PREFS_PUBLIC+ ".xml");

            if (prefsFile.exists()) {
                if(!prefsFile.setReadable(true, false)){
                    Log.e(TAG, "Error accessing shared preferences!");
                } else {
                    prefsPublicSucc = true;
                }
            } else Log.e(TAG, "No shared preferences file!");

            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!isPermRecordGranted(currentActivity) || !isPermModAudioGranted(currentActivity)) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.MODIFY_AUDIO_SETTINGS}, 3);
                }
            }

            final Preference antidimmer = findPreference(PREF_ANTIDIMMER);
            final Preference autocolor = findPreference(PREF_AUTOCOLOR);
            final Preference frontMover = findPreference(PREF_FRONTMOVER);
            final Preference about = findPreference(PREF_ABOUT);
            final Preference xposedStatus = findPreference(PREF_XPOSED);
            final int color = prefsPublic.getInt(PREF_COLOR, 1234567890);
            if (prefsPublic.getBoolean(PREF_AUTOCOLOR, true)){
                autocolor.setSummary(R.string.auto_color_summary);

            } else {
                autocolor.setSummary(R.string.color_custom);
            }

            if (prefsPublic.getBoolean(PREF_ANTIDIMMER, false)){
                antidimmer.setSummary(R.string.antidimmer_enabled);
            } else {
                antidimmer.setSummary(R.string.antidimmer_disabled);
            }

            if (prefsPublic.getBoolean(PREF_FRONTMOVER, false)){
                frontMover.setSummary(R.string.visualizer_front_desc);
            } else {
                frontMover.setSummary(R.string.visualizer_behind_desc);
            }

            if (isXposedWorking()){
                xposedStatus.setSummary(R.string.xposed_ok);
            } else {
                xposedStatus.setSummary(R.string.xposed_err);
            }

            antidimmer.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Toast.makeText(currentActivity, R.string.restart_needed, Toast.LENGTH_SHORT).show();
                    if ((Boolean)newValue){
                        preference.setSummary(R.string.antidimmer_enabled);
                    } else {
                        preference.setSummary(R.string.antidimmer_disabled);
                    }
                    return true;
                }
            });

            autocolor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Toast.makeText(currentActivity, R.string.restart_needed, Toast.LENGTH_SHORT).show();
                    if ((Boolean)newValue){
                        preference.setSummary(R.string.auto_color_summary);
                        if (visualizerView != null) setUpVisualizer();
                    } else {
                        preference.setSummary(R.string.color_custom);
                        final ColorPicker cp;
                        if (color != 1234567890){
                           cp = new ColorPicker(currentActivity, Color.red(color), Color.green(color), Color.blue(color));
                        } else {
                            cp = new ColorPicker(currentActivity, 0, 64, 255);
                        }
                        cp.show();
                        Button okColor = (Button)cp.findViewById(R.id.okColorButton);
                        okColor.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                int color = cp.getColor();
                                prefsPublic.edit().putInt(PREF_COLOR, color).apply();
                                if (visualizerView != null){
                                    setUpVisualizer(color);
                                }
                                cp.dismiss();
                            }
                        });

                    }
                    return true;
                }
            });

            frontMover.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Toast.makeText(currentActivity, R.string.restart_needed, Toast.LENGTH_SHORT).show();
                    if ((Boolean)newValue){
                        preference.setSummary(R.string.visualizer_front_desc);
                    } else {
                        preference.setSummary(R.string.visualizer_behind_desc);
                    }
                    return true;
                }
            });

            about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    About.show(currentActivity);
                    return true;
                }
            });
            String version = BuildConfig.VERSION_NAME;
            about.setSummary(about.getSummary() + " v." + version);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    private static void setUpVisualizer(){
        int color;
        Context context = visualizerView.getContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            color = context.getColor(R.color.colorPrimary);
        } else {
            //noinspection deprecation
            color = context.getResources().getColor(R.color.colorPrimary);
        }
        visualizerView.setColor(color);
    }

    private static void setUpVisualizer(int color){
        visualizerView.setColor(color);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static boolean isPermRecordGranted(Activity activity) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static boolean isPermModAudioGranted(Activity activity) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || activity.checkSelfPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS)
                    == PackageManager.PERMISSION_GRANTED;
    }
}
