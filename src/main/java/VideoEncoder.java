import com.github.clun.movie.MovieMetadataParser;
import com.github.clun.movie.domain.Audio;
import com.github.clun.movie.domain.MovieMetadata;
import com.github.clun.movie.domain.Video;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;

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
public class VideoEncoder {
    private VideoEncoder(String path) {
        ArrayList<Path> videoList = this.findVideos(path);

        if (videoList.size() > 0) {
            int i = 0;
            for (Path videoPath : videoList) {
                readInfo(videoPath);
                i++;
                break;
            }
        }
    }

    public static void main(String[] args) {
        String path = args[0];
        System.out.println(path);

        new VideoEncoder(path);
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

        double videoFrameRate = Double.parseDouble(movieMetadata.get(Video.FRAMERATE_STRING).get()
                .replaceAll("([0-9]+)\\s.+", "$1"));

        long audioBitDepth = Long.parseLong(movieMetadata.get(Audio.BITDEPTH_STRING).get()
                .replaceAll("([0-9]+)\\s.+", "$1"));

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

        // TODO: Set created time & metadata

        System.out.println("Done");
    }

    private ArrayList<Path> findVideos(String path) {
        ArrayList<Path> videoList = new ArrayList<>();
        return findVideos(path, videoList);
    }

    private ArrayList<Path> findVideos(String path, ArrayList<Path> videoList) {
        File dir = new File(path);
        File[] files = dir.listFiles();
        assert files != null;

        for (File file : files) {
            if (file.isDirectory()) {
                videoList.addAll(findVideos(file.getPath()));
            }

            if (file.getName().endsWith("avi")) {
                videoList.add(file.toPath());
            }
        }

        return videoList;
    }
}
