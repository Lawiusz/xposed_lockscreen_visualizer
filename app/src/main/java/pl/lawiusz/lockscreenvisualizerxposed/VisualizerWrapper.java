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
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

class VisualizerWrapper {
    private final Context theirContext;
    private final Context modContext;
    private final ViewGroup theirContainer;

    private VisualizerView visualizerView;

    public VisualizerWrapper(Context theirContext, Context modContext, ViewGroup container){
        this.theirContext = theirContext;
        this.modContext = modContext;
        this.theirContainer = container;
        init();
    }
    private void init(){
        if (isModAudioPermGranted() && isRecordPermGranted()) {
            Log.d("LXVISUALIZER", "All needed permissions granted!");
            LayoutInflater inflater = LayoutInflater.from(modContext);
            ViewGroup mRootView = (ViewGroup) inflater.inflate(R.layout.visualizer_scrim, theirContainer, true);
            visualizerView = new VisualizerView(theirContext);
            visualizerView.setXposedMode(true);
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
            error.append("!");
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
