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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import com.pes.androidmaterialcolorpickerdialog.ColorPicker;

public class SettingsActivity extends PreferenceActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    public static final String PREF_ANTIDIMMER = "antidimmer";
    public static final String PREF_ANTIDIMMER_ON_BATT = "antidimmer_on_batt";
    public static final String PREF_FRONTMOVER = "vis_in_front";
    public static final String PREF_COLOR="custcolor";

    private static final String PREF_AUTOCOLOR = "autocolor";
    private static final String PREF_ABOUT = "about";
    private static final String PREF_XPOSED = "xposed_working";
    private static final String PREF_HIDEAPP = "hideapp";

    public static final String INTENT_ACTION_PREFS_CHANGED
            = "pl.lawiusz.lockscreenvisualizerxposed.action.PREFS_CHANGED2";
    public static final String INTENT_ACTION_SUICIDE
            = "pl.lawiusz.lockscreenvisualizerxposed.action.SUICIDE";

    private static VisualizerView visualizerView;
    private static SharedPreferences prefs;
    private static int color;

    private final SharedPreferences.OnSharedPreferenceChangeListener listener
            = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Intent i = new Intent(INTENT_ACTION_PREFS_CHANGED);
            i.setPackage(MainXposedMod.SYSTEMUI_PACKAGE);
            i
                    .putExtra(PREF_ANTIDIMMER, prefs.getBoolean(PREF_ANTIDIMMER, false))
                    .putExtra(PREF_ANTIDIMMER_ON_BATT,
                            prefs.getBoolean(PREF_ANTIDIMMER_ON_BATT, false))
                    .putExtra(PREF_FRONTMOVER, prefs.getBoolean(PREF_FRONTMOVER, false))
                    .putExtra(PREF_COLOR, prefs.getInt(PREF_COLOR, -1));
            sendBroadcast(i);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        visualizerView = (VisualizerView) findViewById(R.id.visualizerview);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new GeneralPreferenceFragment())
                .commit();
    }
    @Override
    public void onPause(){
        super.onPause();
        if (visualizerView != null){
            visualizerView.setPlaying(false);
        }
    }
    @Override
    public void onResume(){
        super.onResume();
        final int color = prefs.getInt(PREF_COLOR, 0);
        if (prefs.getBoolean(PREF_AUTOCOLOR, true)){
            if (isPermRecordGranted(this) && isPermModAudioGranted(this)) {
                if (visualizerView != null) {
                    setUpVisualizer();
                }
            }
        } else {
            if (isPermRecordGranted(this) && isPermModAudioGranted(this)) {
                if (color == 0){
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
        return false;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return GeneralPreferenceFragment.class.getName().equals(fragmentName);
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
                Toast.makeText(this, R.string.no_perms,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static class GeneralPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            SettingsActivity activity = (SettingsActivity) getActivity();
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);
            prefs = getPreferenceManager().getSharedPreferences();
            prefs.registerOnSharedPreferenceChangeListener(activity.listener);
            activity.listener.onSharedPreferenceChanged(prefs, null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!isPermRecordGranted(getActivity()) || !isPermModAudioGranted(getActivity())) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.MODIFY_AUDIO_SETTINGS}, 3);
                }
            }

            Preference antidimmer = findPreference(PREF_ANTIDIMMER);
            Preference antidimmerBatt = findPreference(PREF_ANTIDIMMER_ON_BATT);
            Preference autocolor = findPreference(PREF_AUTOCOLOR);
            Preference frontMover = findPreference(PREF_FRONTMOVER);
            Preference hideApp = findPreference(PREF_HIDEAPP);
            Preference about = findPreference(PREF_ABOUT);
            Preference xposedStatus = findPreference(PREF_XPOSED);


            color = 0;
            color = prefs.getInt(PREF_COLOR, 0);
            if (prefs.getBoolean(PREF_AUTOCOLOR, true)) {
                autocolor.setSummary(R.string.auto_color_summary);

            } else {
                autocolor.setSummary(R.string.color_custom);
            }

            if (prefs.getBoolean(PREF_ANTIDIMMER, false)) {
                antidimmer.setSummary(R.string.antidimmer_enabled);
                antidimmerBatt.setEnabled(true);
            } else {
                antidimmer.setSummary(R.string.antidimmer_disabled);
                antidimmerBatt.setEnabled(false);
            }

            if (prefs.getBoolean(PREF_ANTIDIMMER_ON_BATT, false)){
                antidimmerBatt.setSummary(R.string.antidimmer_batt_enabled);
            } else {
                antidimmerBatt.setSummary(R.string.antidimmer_batt_disabled);
            }

            if (prefs.getBoolean(PREF_FRONTMOVER, false)) {
                frontMover.setSummary(R.string.visualizer_front_desc);
            } else {
                frontMover.setSummary(R.string.visualizer_behind_desc);
            }

            xposedStatus.setOnPreferenceClickListener((view) -> {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.setPackage("de.robv.android.xposed.installer");
                i.putExtra("section", "modules");
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                return true;
            });

            if (isXposedWorking()){
                xposedStatus.setSummary(R.string.xposed_ok);
            } else {
                xposedStatus.setSummary(R.string.xposed_err);
            }

            antidimmer.setOnPreferenceChangeListener((preference, newValue) -> {
                if ((Boolean)newValue){
                    preference.setSummary(R.string.antidimmer_enabled);
                    antidimmerBatt.setEnabled(true);
                } else {
                    preference.setSummary(R.string.antidimmer_disabled);
                    antidimmerBatt.setEnabled(false);
                }
                return true;
            });

            autocolor.setOnPreferenceChangeListener((preference, newValue) -> {
                if ((Boolean) newValue) {
                    preference.setSummary(R.string.auto_color_summary);
                    if (visualizerView != null) setUpVisualizer();
                } else {
                    preference.setSummary(R.string.color_custom);
                    final ColorPicker cp;
                    if (color != 0) {
                        cp = new ColorPicker(getActivity(),
                                Color.red(color), Color.green(color), Color.blue(color));
                    } else {
                        cp = new ColorPicker(getActivity(), 0, 64, 255);
                    }
                    cp.show();
                    Button okColor = (Button) cp.findViewById(R.id.okColorButton);
                    okColor.setOnClickListener(v -> {
                        int color1 = cp.getColor();
                        prefs.edit().putInt(PREF_COLOR, color1).apply();
                        if (visualizerView != null) {
                            setUpVisualizer(color1);
                        }
                        cp.dismiss();
                    });
                }
                return true;
            });


            frontMover.setOnPreferenceChangeListener((preference, newValue) -> {
                Toast.makeText(getActivity(),
                        R.string.restart_needed, Toast.LENGTH_SHORT).show();
                if ((Boolean)newValue){
                    preference.setSummary(R.string.visualizer_front_desc);
                } else {
                    preference.setSummary(R.string.visualizer_behind_desc);
                }
                return true;
            });
            boolean apphidden = prefs.getBoolean(PREF_HIDEAPP, false);
            String apphideSummary = apphidden ? getString(R.string.app_invisible)
                    : getString(R.string.app_visible);
            hideApp.setSummary(apphideSummary);
            int mode = apphidden ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
            getActivity().getPackageManager().setComponentEnabledSetting(
                    new ComponentName(getActivity(),
                            getClass().getPackage().getName()+".SettingsAlias"), mode,
                    PackageManager.DONT_KILL_APP);

            hideApp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int mode = (Boolean)newValue ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                    getActivity().getPackageManager().setComponentEnabledSetting(
                            new ComponentName(getActivity(),
                                    getClass().getPackage().getName()+".SettingsAlias"), mode,
                            PackageManager.DONT_KILL_APP);
                    String apphideSummary = (Boolean)newValue
                            ? getString(R.string.app_invisible)
                            : getString(R.string.app_visible);
                    hideApp.setSummary(apphideSummary);
                    return true;
                }
            });

            about.setOnPreferenceClickListener(preference -> {
                new About().show(getActivity());
                return true;
            });
            String version = BuildConfig.VERSION_NAME;
            about.setSummary(about.getSummary() + " v." + version);
            findPreference("restartsysui").setOnPreferenceClickListener(preference -> {
                Context context = getActivity();
                if (context == null) return false;
                Intent i = new Intent(INTENT_ACTION_SUICIDE);
                i.setPackage(MainXposedMod.SYSTEMUI_PACKAGE);
                context.sendBroadcast(i);
                return false;
            });
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
    } // end of fragment

    private static void setUpVisualizer(){
        setUpVisualizer(ContextCompat.getColor(visualizerView.getContext(), R.color.colorPrimary));
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
