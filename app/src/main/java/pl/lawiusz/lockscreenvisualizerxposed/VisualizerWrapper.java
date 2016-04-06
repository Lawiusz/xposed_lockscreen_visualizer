package pl.lawiusz.lockscreenvisualizerxposed;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class VisualizerWrapper {
    private Context theirContext;
    private Context modContext;
    private ViewGroup theirContainer;

    private VisualizerView visualizerView;

    public VisualizerWrapper(Context theirContext, Context modContext, ViewGroup container){
        this.theirContext = theirContext;
        this.modContext = modContext;
        this.theirContainer = container;
        init();
    }
    private void init(){
        if (isModAudioPermGranted() && isRecordPermGranted()) {
            LayoutInflater inflater = LayoutInflater.from(modContext);
            ViewGroup mRootView = (ViewGroup) inflater.inflate(R.layout.visualizer_scrim, theirContainer, true);
            visualizerView = new VisualizerView(theirContext);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1024, Gravity.BOTTOM);
            visualizerView.setLayoutParams(layoutParams);
            mRootView.addView(visualizerView);
        } else {
            StringBuilder error = new StringBuilder();
            error.append("The following required permissions are not granted: ");
            if (!isModAudioPermGranted()){
                error.append("MODIFY_AUDIO_SETTINGS ");
            }
            if (!isRecordPermGranted()){
                error.append("RECORD_AUDIO");
            }
            throw new SecurityException(error.toString());
        }
    }
    public VisualizerView getVisualizerView(){
        return visualizerView;
    }
    private boolean isModAudioPermGranted(){
        return theirContext.checkPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED;
    }
    private boolean isRecordPermGranted(){
        return theirContext.checkPermission(Manifest.permission.RECORD_AUDIO, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED;
    }
}
