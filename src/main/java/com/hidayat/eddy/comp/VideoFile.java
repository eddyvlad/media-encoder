package com.hidayat.eddy.comp;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.regex.Pattern;

@SuppressWarnings("ConstantConditions")
public class VideoFile {
    private Path path;
    private FileTime fileTime;
    private Integer width;
    private Integer height;
    private double frameRate;
    private long bitRate;
    private long audioBitrate;
    private FFmpeg ffmpeg;
    private FFmpegProbeResult ffProbe;
    private FFmpegStream videoStream;
    private FFmpegStream audioStream;
    // Maximum path depth to show when `toString()`
    @SuppressWarnings("FieldCanBeLocal")
    private final int toStringDepth = 3;


    public VideoFile(Path path) throws IOException {
        this.path = path;

        BasicFileAttributes basicFileAttributes = null;
        try {
            basicFileAttributes = Files.readAttributes(this.path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            initFFmpeg();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 2008-05-12T14:14:32Z
        fileTime = basicFileAttributes != null ? basicFileAttributes.creationTime() : null;
        // In pixel
        width = videoStream.width;
        // In pixel
        height = videoStream.height;
        frameRate = videoStream.r_frame_rate.doubleValue();
        bitRate = videoStream.bit_rate;
        audioBitrate = audioStream.bit_rate;
    }

    private void initFFmpeg() throws IOException {
        String currentDir = System.getProperty("user.dir");
        String OS = System.getProperty("os.name");
        String subDir = "/ffmpeg";
        String osDir = "/win/";
        String ffmpegName = "ffmpeg.exe";
        String ffprobeName = "ffprobe.exe";

        if (OS.toLowerCase().contains("mac")) {
            osDir = "/mac/";
            ffmpegName = "ffmpeg";
            ffprobeName = "ffprobe";
        }

        File ffmpegPath = new File(currentDir + subDir + osDir + ffmpegName);
        File ffprobePath = new File(currentDir + subDir + osDir + ffprobeName);

        ffmpeg = new FFmpeg(ffmpegPath.getPath());
        ffProbe = new FFprobe(ffprobePath.getPath()).probe(path.toString());

        // Find video stream
        videoStream = null;
        audioStream = null;
        for (FFmpegStream stream : ffProbe.getStreams()) {
            if (Objects.equals(stream.codec_type.name(), "VIDEO")) {
                videoStream = stream;
            } else if (Objects.equals(stream.codec_type.name(), "AUDIO")) {
                audioStream = stream;
            }
        }
    }

    @Override
    public String toString() {
        String pathStr = path.toString();
        String[] split = pathStr.split(Pattern.quote(File.separator));

        int splitLength = split.length;

        StringBuilder shortPath = new StringBuilder();
        for (int i = (splitLength-toStringDepth-1); i < splitLength; i++) {
            shortPath.append(split[i]);
            if ( i+1 < splitLength ) {
                shortPath.append(File.separator);
            }
        }

        return shortPath.toString();
    }

    /**
     * Remove any chars and return digits
     *
     * @param string Value of the meta data (may include chars)
     * @return String with the chars removed
     */
    private String strExtractNumbers(String string) {
        return string.replaceAll("([0-9]+)\\s.+", "$1");
    }
}
