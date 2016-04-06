package pl.lawiusz.lockscreenvisualizerxposed;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    public static final String PREF_ANTIDIMMER = "antidimmer";
    public static final String PREF_BRIGHTNESS = "visualizer_brightness";
    public static final String PREFS_PUBLIC = "public";
    private SharedPreferences preferences;
    private Switch antidimmerSwitch;
    private TextView antidimmerSummary;

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
        final VisualizerView visualizerView = (VisualizerView) findViewById(R.id.visualizer_view);
        antidimmerSwitch = (Switch) findViewById(R.id.antidimmer_switch);
        antidimmerSummary = (TextView) findViewById(R.id.antidimmer_summary);
        final SeekBar brightnessSeekBar = (SeekBar) findViewById(R.id.brightness_slider);
        final TextView brightnessValueTv = (TextView) findViewById(R.id.tv_brightness_value);

        final int brightness = preferences.getInt(PREF_BRIGHTNESS, 5);
        // Set-up visualizer preview
        assert visualizerView != null;
        visualizerView.setVisible(true);
        visualizerView.setPlaying(true);
        visualizerView.setBitmap(getColorBitmap());

        assert brightnessValueTv != null;
        brightnessValueTv.setText(String.valueOf(brightness));
        assert brightnessSeekBar != null;
        brightnessSeekBar.setProgress(brightness);
        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                brightnessValueTv.setText(String.valueOf(progress));
                visualizerView.setBrightness(progress);
                preferences.edit().putInt(PREF_BRIGHTNESS, progress).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        assert antidimmerSwitch != null;
        boolean enabled = preferences.getBoolean(PREF_ANTIDIMMER, false);
        antidimmerSwitch.setChecked(enabled);
        antidimmerSwitch.setOnClickListener(onSwitchClick);
        assert antidimmerSummary != null;
        antidimmerSummary.setOnClickListener(onSwitchClick);
        if (enabled) {
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
    private View.OnClickListener onSwitchClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean enabled = antidimmerSwitch.isChecked();
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
