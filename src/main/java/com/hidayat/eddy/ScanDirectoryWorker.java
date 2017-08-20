package com.hidayat.eddy;

import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class ScanDirectoryWorker extends SwingWorker<ArrayList<Path>, Void> {
    private String[] supportedExtensions;
    private File selectedDir;

    ScanDirectoryWorker(File selectedDir, String[] supportedExtensions) {
        this.selectedDir = selectedDir;
        this.supportedExtensions = supportedExtensions;
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

            String extension = FilenameUtils.getExtension(file.getName().toLowerCase());
            boolean isSupported = Arrays.asList(supportedExtensions).contains(extension);
            if (isSupported) {
                videoList.add(file.toPath());
            }
        }

        return videoList;
    }

    /**
     * This is the caller to findVideos
     *
     * @return Returns ArrayList
     * @throws Exception doInBackground may throw Exception
     */
    @Override
    protected ArrayList<Path> doInBackground() throws Exception {
        return findVideos(selectedDir);
    }
}
