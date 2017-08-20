package com.hidayat.eddy;

import com.hidayat.eddy.components.VideoItem;
import net.bramp.ffmpeg.progress.Progress;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VideoConversionWorker extends SwingWorker {
    private JList<VideoItem> pathList;
    private JLabel jLabel;
    private JProgressBar jProgressBar;

    VideoConversionWorker(JList<VideoItem> pathList, JLabel jLabel, JProgressBar jProgressBar) {
        this.pathList = pathList;
        this.jLabel = jLabel;
        this.jProgressBar = jProgressBar;
    }

    @Override
    protected Object doInBackground() throws Exception {
        List<VideoItem> selectedValue = pathList.getSelectedValuesList();
        // Calculate total duration
        double totalDurationSec = 0;
        for (VideoItem videoFile : selectedValue) {
            totalDurationSec += videoFile.ffProbeResult.getFormat().duration;
        }

        jProgressBar.setMaximum((int) totalDurationSec);
        jProgressBar.setMinimum(0);

        // Increment total duration here
        int outTimeSec = 0;
        for (VideoItem videoFile : selectedValue) {
            int finalOutTimeSec = outTimeSec;
            videoFile.convertToMp4((Progress progress) -> {
                int currentOutTimeSec = (int) TimeUnit.MICROSECONDS.toSeconds(progress.out_time_ms) + finalOutTimeSec;
                jProgressBar.setValue(currentOutTimeSec);
                jLabel.setText(videoFile.path.getFileName().toString());
                videoFile.setProgress(progress);
            });

            outTimeSec+= videoFile.ffProbeResult.getFormat().duration;
            // Set date modified to original
        }

        return pathList;
    }
}
