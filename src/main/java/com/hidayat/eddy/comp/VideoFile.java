package com.hidayat.eddy.comp;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.progress.Progress;
import org.apache.commons.io.FilenameUtils;

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
    public Path path;
    private FileTime fileTime;
    private FFmpeg ffmpeg;
    private FFprobe ffProbe;
    public FFmpegProbeResult ffProbeResult;
    private FFmpegStream videoStream;
    private FFmpegStream audioStream;
    // Maximum path depth to show when `toString()`
    @SuppressWarnings("FieldCanBeLocal")
    private final int toStringDepth = 3;
    private Progress progress;


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
        ffProbe = new FFprobe(ffprobePath.getPath());
        ffProbeResult = ffProbe.probe(path.toString());

        // Find video stream
        videoStream = null;
        audioStream = null;
        for (FFmpegStream stream : ffProbeResult.getStreams()) {
            if (Objects.equals(stream.codec_type.name(), FFmpegStream.CodecType.VIDEO.name())) {
                videoStream = stream;
            } else if (Objects.equals(stream.codec_type.name(), FFmpegStream.CodecType.AUDIO.name())) {
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
     * @param progressListener
     */
    public void convertToMp4(net.bramp.ffmpeg.progress.ProgressListener progressListener) {
        String newPath = FilenameUtils.removeExtension(path.toString()) + ".mp4";
        int maxSampleRate = Math.max(FFmpeg.AUDIO_SAMPLE_44100, audioStream.sample_rate);

        FFmpegBuilder builder = new FFmpegBuilder();
        builder.setInput(path.toAbsolutePath().toString())
                .overrideOutputFiles(true)
                .addOutput(newPath)
                .setVideoBitRate(videoStream.bit_rate)
                .setVideoCodec("libx264")
                .setVideoFrameRate(videoStream.r_frame_rate)
                .setVideoResolution(videoStream.width, videoStream.height)
                .setAudioChannels(audioStream.channels)
                .setAudioCodec("aac")
                .setAudioBitRate(audioStream.bit_rate)
                .setAudioSampleRate(maxSampleRate)
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffProbe);
        FFmpegJob job = executor.createJob(builder, progressListener);
        job.run();
    }

    public void setProgress(Progress progress) {
        this.progress = progress;
    }
}
