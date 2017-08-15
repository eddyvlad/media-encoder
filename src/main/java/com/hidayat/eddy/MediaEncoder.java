package com.hidayat.eddy;

import com.github.clun.movie.MovieMetadataParser;
import com.github.clun.movie.domain.Audio;
import com.github.clun.movie.domain.MovieMetadata;
import com.github.clun.movie.domain.Video;
import com.hidayat.eddy.comp.PathListRenderer;
import com.hidayat.eddy.comp.VideoFile;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ConstantConditions")
public class MediaEncoder extends JPanel {
    private String[] supportedExtensions = {"avi", "3gp"};
    private JPanel mainPanel;
    private JTextField dirField;
    private JButton browseButton;
    private JButton ok;
    private JList<VideoFile> pathList;
    private JScrollPane pathListPane;
    private JProgressBar progress;
    private JPanel statusPanel;
    private JLabel statusLabel;
    private File selectedDir;
    private ArrayList<Path> videoList;

    private MediaEncoder() {
        browseButton.addActionListener(e -> {
            String userHomeDir = System.getProperty("user.home");

            JFileChooser choose = new JFileChooser();
            choose.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            choose.setAcceptAllFileFilterUsed(false);
            choose.setCurrentDirectory(new File(userHomeDir));
            int result = choose.showOpenDialog(browseButton);

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

    private void setSelectedDir(File selectedDir) {
        this.selectedDir = selectedDir;
        dirField.setText(this.selectedDir.toString());

        // Wait
        SwingWorker worker = new ScanDirectory<ArrayList, Void>(selectedDir, supportedExtensions);
        worker.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            switch ((SwingWorker.StateValue) evt.getNewValue()) {
                case PENDING:
                    break;
                case STARTED:
                    dirField.setEnabled(false);
                    browseButton.setEnabled(false);
                    statusLabel.setVisible(true);
                    ok.setEnabled(false);
                    break;
                case DONE:
                    // Done waiting
                    dirField.setEnabled(true);
                    browseButton.setEnabled(true);
                    statusLabel.setVisible(false);
                    ok.setEnabled(true);

                    try {
                        //noinspection unchecked
                        ArrayList<Path> videoList = (ArrayList<Path>) worker.get();

                        if (videoList.size() == 0) {
                            StringBuilder stringBuilder = new StringBuilder();
                            for (int i = 0; i < supportedExtensions.length; i++) {
                                stringBuilder.append(supportedExtensions[i]);
                                if (i + 1 < supportedExtensions.length) {
                                    stringBuilder.append(",");
                                }
                            }

                            statusLabel.setVisible(true);
                            statusLabel.setText("Unable to find any files with extension " + stringBuilder.toString());
                        }

                        setPathList(videoList);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        });

        worker.execute();
    }

    private void setPathList(ArrayList<Path> videoList) {
        DefaultListModel<VideoFile> videos = new DefaultListModel<>();

        for (Path path : videoList) {
            videos.addElement(new VideoFile(path));
        }

        pathList.setCellRenderer(new PathListRenderer<>());
        pathList.setModel(videos);
        pathList.setSelectionInterval(0, videos.getSize() - 1);
    }
}
