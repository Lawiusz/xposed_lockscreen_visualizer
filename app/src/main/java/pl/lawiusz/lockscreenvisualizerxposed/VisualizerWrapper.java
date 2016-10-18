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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

class VisualizerWrapper {

    private static VisualizerView visualizerView;
    private static boolean ready = false;

    static void init(Context theirContext, Context modContext, ViewGroup theirContainer){
        if (ready){
            LLog.d("VisualizerWrapper ready, no need to reinit!");
            return;
        }
        if (isModAudioPermGranted(theirContext) && isRecordPermGranted(theirContext)) {
            LLog.d("All needed permissions granted!");
            LayoutInflater inflater = LayoutInflater.from(modContext);
            ViewGroup mRootView = (ViewGroup) inflater.inflate(R.layout.visualizer_scrim, theirContainer, true);
            visualizerView = new VisualizerView(theirContext);
            visualizerView.setXposedMode(true);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.BOTTOM);
            visualizerView.setLayoutParams(layoutParams);
            mRootView.addView(visualizerView);
            ready = true;
        } else {
            StringBuilder error = new StringBuilder();
            error.append("The following required permissions are not granted: ");
            if (!isModAudioPermGranted(theirContext)){
                error.append("MODIFY_AUDIO_SETTINGS ");
            }
            if (!isRecordPermGranted(theirContext)){
                error.append("RECORD_AUDIO");
            }
            error.append("!");
            ready = false;
            throw new SecurityException(error.toString());
        }
    }
    static VisualizerView getVisualizerView(){
        if (ready) {
            return visualizerView;
        } else return null;
    }
    private static boolean isModAudioPermGranted(Context theirContext){
        return theirContext.checkPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED;
    }
    private static boolean isRecordPermGranted(Context theirContext){
        return theirContext.checkPermission(Manifest.permission.RECORD_AUDIO, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED;
    }
}
