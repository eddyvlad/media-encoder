package com.hidayat.eddy;

import com.hidayat.eddy.comp.VideoFile;
import net.bramp.ffmpeg.FFmpegUtils;
import net.bramp.ffmpeg.progress.Progress;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VideoConversionWorker extends SwingWorker {
    private JList<VideoFile> pathList;

    public VideoConversionWorker(JList<VideoFile> pathList) {
        this.pathList = pathList;
    }

    @Override
    protected Object doInBackground() throws Exception {
        List<VideoFile> selectedValue = pathList.getSelectedValuesList();
        for (VideoFile videoFile : selectedValue) {
            videoFile.convertToMp4((Progress progress) -> {
                double durationMs = videoFile.ffProbeResult.getFormat().duration * TimeUnit.SECONDS.toMicros(1);
                double percentage = 100 / durationMs * progress.out_time_ms;

                System.out.println(String.format(
                        "[%.0f%%] frame:%d time:%s ms fps:%.0f speed:%.2fx",
                        percentage,
                        progress.frame,
                        FFmpegUtils.millisecondsToString(progress.out_time_ms),
                        progress.fps.doubleValue(),
                        progress.speed
                ));

                videoFile.setProgress(progress);
            });
        }

        return pathList;
    }
}
