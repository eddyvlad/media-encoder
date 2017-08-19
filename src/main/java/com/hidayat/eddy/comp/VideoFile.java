package com.hidayat.eddy.comp;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.job.TwoPassFFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.progress.Progress;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@SuppressWarnings("ConstantConditions")
public class VideoFile {
    // Maximum path depth to show when `toString()`
    @SuppressWarnings("FieldCanBeLocal")
    private final int toStringDepth = 3;
    public Path path;
    public FFmpegProbeResult ffProbeResult;
    private FileTime fileTime;
    private FFmpeg ffmpeg;
    private FFprobe ffProbe;
    private FFmpegStream videoStream;
    private FFmpegStream audioStream;
    private Progress progress;
    /**
     * The path to the converted file
     */
    private File newPath;


    public VideoFile(Path path) throws IOException {
        this.path = path;

        BasicFileAttributes basicFileAttributes = null;
        try {
            basicFileAttributes = Files.readAttributes(this.path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 2008-05-12T14:14:32Z
        fileTime = basicFileAttributes != null ? basicFileAttributes.creationTime() : null;

        if (basicFileAttributes.lastModifiedTime().compareTo(fileTime) < 0) {
            fileTime = basicFileAttributes.lastModifiedTime();
        }

        try {
            initFFmpeg();
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileTime newFileTime = getCreatedTimeFromMeta();
        if (newFileTime != null && newFileTime.compareTo(fileTime) < 0) {
            fileTime = newFileTime;
        }

        FileTime fileNameDate = getDateTimeFromFileName();
        if (fileNameDate != null && fileNameDate.compareTo(fileTime) < 0) {
            fileTime = fileNameDate;
        }
    }

    private FileTime getDateTimeFromFileName() {
        String[] dateFormats = new String[]{
                "yyyyMMdd-HHmmss",
                "dd-MM-yy'_'HHmm"
        };

        for (String dateFormat : dateFormats) {
            FileTime dateTimeFromFileName;
            try {
                dateTimeFromFileName = getFileTimeFromFormat(dateFormat);
            } catch (ParseException e) {
                continue;
            }

            return dateTimeFromFileName;
        }

        return null;
    }

    private FileTime getFileTimeFromFormat(String format) throws ParseException {
        String fileName = path.getFileName().toString();
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Singapore"));
        Date parse = dateFormat.parse(fileName);

        return FileTime.fromMillis(parse.getTime());
    }

    private FileTime getCreatedTimeFromMeta() {
        if (videoStream.tags != null && videoStream.tags.containsKey("creation_time")) {
            String creationTimeStr = videoStream.tags.get("creation_time");
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
            dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date parseDateTime = null;
            try {
                parseDateTime = dateFormatter.parse(creationTimeStr);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(parseDateTime);
            int year = calendar.get(Calendar.YEAR);

            if (year > 2000) {
                // Convert to filetime
                return FileTime.from(parseDateTime.getTime(), TimeUnit.MILLISECONDS);
            }
        }
        return null;
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
        for (int i = (splitLength - toStringDepth - 1); i < splitLength; i++) {
            shortPath.append(split[i]);
            if (i + 1 < splitLength) {
                shortPath.append(File.separator);
            }
        }

        return shortPath.toString();
    }

    /**
     * @param progressListener Progress Listener
     */
    public void convertToMp4(net.bramp.ffmpeg.progress.ProgressListener progressListener) {
        newPath = new File(FilenameUtils.removeExtension(path.toString()) + ".mp4");

        FFmpegBuilder builder = new FFmpegBuilder();
        builder.setInput(ffProbeResult)
                .overrideOutputFiles(true)
                .setPass(2)
                .setPassDirectory(path.getParent().toString());

        FFmpegOutputBuilder outputBuilder = builder.addOutput(newPath.toString());
        /*outputBuilder.setVideoCodec("libx264")
                .setVideoResolution(videoStream.width, videoStream.height)
                .setVideoFrameRate(videoStream.r_frame_rate)
                .setVideoBitRate(videoStream.bit_rate)
                .setAudioChannels(audioStream.channels)
                .setAudioCodec("aac")
                .setAudioBitRate(audioStream.bit_rate)
                .setAudioSampleRate(audioStream.sample_rate)
                .addMetaTag("creation_time", fileTime.toString())
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL);*/

        outputBuilder.setVideoCodec("libx264")
                .setFormat("mp4")
                .addMetaTag("creation_time", fileTime.toString())
                .setAudioCodec("aac")
                .setAudioChannels(audioStream.channels)
                .setTargetSize(ffProbeResult.getFormat().size);

        outputBuilder.done();
        TwoPassFFmpegJob twoPassFFmpegJob = new TwoPassFFmpegJob(ffmpeg, builder, progressListener);
        twoPassFFmpegJob.run();
        /*FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffProbe);
        FFmpegJob job = executor.createJob(builder, progressListener);
        job.run();*/
    }

    public void setProgress(Progress progress) {
        this.progress = progress;

        if (progress.progress.equals("end")) {
            //noinspection ResultOfMethodCallIgnored
            newPath.setLastModified(fileTime.toMillis());

            try {
                Files.getFileAttributeView(path, BasicFileAttributeView.class).setTimes(fileTime, null, fileTime);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
