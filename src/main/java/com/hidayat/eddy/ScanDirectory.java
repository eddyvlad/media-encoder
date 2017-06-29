package com.hidayat.eddy;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

public class ScanDirectory<T, V> extends SwingWorker<ArrayList<Path>, Void> {
    private final File selectedDir;

    ScanDirectory(File selectedDir) {
        this.selectedDir = selectedDir;
    }

    private ArrayList<Path> findVideos(File dir) {
        ArrayList<Path> videoList = new ArrayList<>();
        return findVideos(dir, videoList);
    }

    private ArrayList<Path> findVideos(File dir, ArrayList<Path> videoList) {
        File[] files = dir.listFiles();
        assert files != null;

        for (File file : files) {
            if (file.isDirectory()) {
                videoList.addAll(findVideos(file));
            }

            if (file.getName().endsWith("avi")) {
                videoList.add(file.toPath());
            }
        }

        return videoList;
    }

    @Override
    protected ArrayList<Path> doInBackground() throws Exception {
        return findVideos(selectedDir);
    }
}
