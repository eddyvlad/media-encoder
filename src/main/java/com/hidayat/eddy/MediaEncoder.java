package com.hidayat.eddy;

import com.github.clun.movie.MovieMetadataParser;
import com.github.clun.movie.domain.Audio;
import com.github.clun.movie.domain.MovieMetadata;
import com.github.clun.movie.domain.Video;
import com.hidayat.eddy.comp.CheckBoxList;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ConstantConditions")
public class MediaEncoder {
    private JPanel mainPanel;
    private JTextField dirPath;
    private JButton browse;
    private JButton ok;
    private JList filesList;
    private File selectedDir;
    private ArrayList<Path> videoList;

    private MediaEncoder() {
        browse.addActionListener(e -> {
            JFileChooser choose = new JFileChooser();
            choose.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            choose.setAcceptAllFileFilterUsed(false);
            int result = choose.showOpenDialog(browse);

            // User open a file/dir
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedDir = choose.getSelectedFile();

                setSelectedDir(selectedDir);
            }
        });

        ok.addActionListener(e -> {

        });
    }

    public static void main(String[] args) {
        JFrame jFrame = new JFrame("MediaEncoder.form");
        jFrame.setContentPane(new MediaEncoder().mainPanel);
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        // Middle center the window
        jFrame.setLocation(dim.width / 2 - jFrame.getSize().width / 2, dim.height / 2 - jFrame.getSize().height / 2);

        jFrame.pack();
        jFrame.setVisible(true);
    }

    private void openDirectory(File dir) {
        videoList = this.findVideos(dir);

        if (videoList.size() > 0) {
            int i = 0;
            for (Path videoPath : videoList) {
                readInfo(videoPath);
                i++;
                break;
            }
        }
    }

    private void readInfo(Path videoPath) {
        BasicFileAttributes basicFileAttributes = null;
        try {
            basicFileAttributes = Files.readAttributes(videoPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        MovieMetadata movieMetadata = MovieMetadataParser.getInstance().parseFile(videoPath.toString());

        // 2008-05-12T14:14:32Z
        FileTime fileTime = basicFileAttributes.creationTime();
        // In pixel
        Integer videoWith = movieMetadata.getVideoWidth().get();
        // In pixel
        Integer videoHeight = movieMetadata.getVideoHeight().get();
        // Take only the digits and discard the chars
        double videoFrameRate = Double.parseDouble(movieMetadata.get(Video.FRAMERATE_STRING).get()
                .replaceAll("([0-9]+)\\s.+", "$1"));
        // Take only the digits and discard the chars
        long audioBitDepth = Long.parseLong(movieMetadata.get(Audio.BITDEPTH_STRING).get()
                .replaceAll("([0-9]+)\\s.+", "$1"));
        // Take only the digits and discard the chars
        long audioBitrate = Long.parseLong(movieMetadata.get(Audio.BITRATE).get()
                .replaceAll("([0-9]+)\\s.+", "$1"));

        System.out.println(videoPath);
        System.out.println(fileTime);
        System.out.println(videoFrameRate);
        System.out.println(videoWith + "x" + videoHeight + "@");
        System.out.println(audioBitDepth);
        System.out.println(audioBitrate);

        FFmpeg fFmpeg = null;
        FFprobe fFprobe = null;
        try {
            fFmpeg = new FFmpeg("D:\\Projects\\MediaEncoder\\assets\\ffmpeg.exe");
            fFprobe = new FFprobe("D:\\Projects\\MediaEncoder\\assets\\ffprobe.exe");
        } catch (IOException e) {
            e.printStackTrace();
        }

        FFmpegProbeResult probe = null;
        try {
            probe = fFprobe.probe(videoPath.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        long bitRate = probe.getFormat().bit_rate;
        String newFileName = videoPath.toString().replace(".avi", ".mp4");

        FFmpegBuilder builder = new FFmpegBuilder();
        builder.setInput(videoPath.toString())
                .addOutput(newFileName)
                .setFormat("mp4")
                .setAudioChannels(2)
                .setAudioCodec("aac")
                .setAudioQuality(9)
                .setVideoCodec("libx264")
                .setVideoFrameRate(videoFrameRate)
                .setVideoBitRate(bitRate)
                .setVideoResolution(videoWith, videoHeight)
                .setStrict(FFmpegBuilder.Strict.NORMAL)
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(fFmpeg, fFprobe);
        FFmpegProbeResult finalProbe = probe;
        executor.createJob(builder, new ProgressListener() {

            // Using the FFmpegProbeResult determine the duration of the input
            final double duration_ms = TimeUnit.SECONDS.toMicros((long) finalProbe.getFormat().duration);

            @Override
            public void progress(Progress progress) {
                double percentage = (progress.out_time_ms / duration_ms) * 100;

                // Print out interesting information about the progress
                System.out.println(String.format(
                        "[%.0f%%] frame:%d time:%s s fps:%.0f speed:%.2fx",
                        percentage,
                        progress.frame,
                        TimeUnit.MILLISECONDS.toSeconds(progress.out_time_ms),
                        progress.fps.doubleValue(),
                        progress.speed
                ));
            }
        }).run();


        System.out.println("Done");
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

    private void setSelectedDir(File selectedDir) {
        this.selectedDir = selectedDir;
        dirPath.setText(selectedDir.toString());

        videoList = this.findVideos(selectedDir);
        setVideoList(videoList);

        // Enable ok button
        this.ok.setEnabled(true);
    }

    private void setVideoList(ArrayList<Path> videoList) {
        this.videoList = videoList;

        int index = 0;
        for (Path path : videoList) {
            JCheckBox checkBox = new JCheckBox();
            checkBox.setSelected(true);
            checkBox.setText(path.toString());

            filesList.add(checkBox, index);
            index++;
        }

        System.out.println(filesList);
    }
}
