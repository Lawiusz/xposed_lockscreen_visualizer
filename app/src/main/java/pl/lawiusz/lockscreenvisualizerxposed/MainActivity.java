package pl.lawiusz.lockscreenvisualizerxposed;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {

    public static final String PREF_ANTIDIMMER = "antidimmer";
    public static final String PREFS_PUBLIC = "public";
    private SharedPreferences preferences;
    private Switch antidimmerSwitch;
    private TextView antidimmerSummary;
    private VisualizerView visualizerView;

    @SuppressLint("SetWorldReadable")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences(PREFS_PUBLIC, MODE_PRIVATE);
        File prefsDir = new File(getApplicationInfo().dataDir, "shared_prefs");
        File prefsFile = new File(prefsDir, PREFS_PUBLIC+ ".xml");
        if (prefsFile.exists()) {
            if(!prefsFile.setReadable(true, false)){
                Log.e("LXVISUALIZER", "Error accessing shared preferences!");
            }
        } else Log.e("LXVISUALIZER", "Invalid shared preferences file!");
        visualizerView = (VisualizerView) findViewById(R.id.visualizer_view);
        antidimmerSwitch = (Switch) findViewById(R.id.antidimmer_switch);
        antidimmerSummary = (TextView) findViewById(R.id.antidimmer_summary);

        // Set-up visualizer preview
        assert visualizerView != null;
        visualizerView.setVisible();
        visualizerView.setPlaying(true);
        visualizerView.setBitmap(getColorBitmap());

        assert antidimmerSwitch != null;
        boolean antidimmerEnabled = preferences.getBoolean(PREF_ANTIDIMMER, false);
        visualizerView.setKeepScreenOn(antidimmerEnabled);
        antidimmerSwitch.setChecked(antidimmerEnabled);
        antidimmerSwitch.setOnClickListener(onSwitchClick);
        assert antidimmerSummary != null;
        antidimmerSummary.setOnClickListener(onSwitchClick);
        if (antidimmerEnabled) {
            antidimmerSummary.setText(getString(R.string.antidimmer_enabled));
        } else {
            antidimmerSummary.setText(getString(R.string.antidimmer_disabled));
        }
    }
    private Bitmap getColorBitmap(){
        int color;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            color = getColor(R.color.colorPrimary);
        } else {
            //noinspection deprecation
            color = getResources().getColor(R.color.colorPrimary);
        }
        Bitmap bmp = Bitmap.createBitmap(2,2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(color);
        return bmp;
    }
    private final View.OnClickListener onSwitchClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean enabled = antidimmerSwitch.isChecked();
            visualizerView.setKeepScreenOn(enabled);
            preferences.edit().putBoolean(PREF_ANTIDIMMER, enabled).apply();
            if (enabled) {
                antidimmerSummary.setText(getString(R.string.antidimmer_enabled));
            } else {
                antidimmerSummary.setText(getString(R.string.antidimmer_disabled));
            }
            Toast.makeText(MainActivity.this, getString(R.string.restart_needed), Toast.LENGTH_SHORT).show();
        }
    };
}
