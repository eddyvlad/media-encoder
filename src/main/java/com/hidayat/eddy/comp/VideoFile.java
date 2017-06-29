package com.hidayat.eddy.comp;

import com.github.clun.movie.MovieMetadataParser;
import com.github.clun.movie.domain.Audio;
import com.github.clun.movie.domain.MovieMetadata;
import com.github.clun.movie.domain.Video;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

@SuppressWarnings("ConstantConditions")
public class VideoFile {
    private final Path path;
    private final FileTime fileTime;
    private final Integer width;
    private final Integer height;
    private final double frameRate;
    private final int bitRate;
    private int audioBitrate;
    // Maximum path depth to show when `toString()`
    @SuppressWarnings("FieldCanBeLocal")
    private final int toStringDepth = 3;

    public VideoFile(Path path) {
        this.path = path;

        BasicFileAttributes basicFileAttributes = null;
        try {
            basicFileAttributes = Files.readAttributes(this.path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        MovieMetadata movieMetadata = MovieMetadataParser.getInstance().parseFile(this.path.toString());

        // 2008-05-12T14:14:32Z
        fileTime = basicFileAttributes != null ? basicFileAttributes.creationTime() : null;
        // In pixel
        width = movieMetadata.getVideoWidth().get();
        // In pixel
        height = movieMetadata.getVideoHeight().get();
        // Take only the digits and discard the chars
        frameRate = Double.parseDouble(strExtractNumbers(movieMetadata.get(Video.FRAMERATE_STRING).get()));
        bitRate = Integer.parseInt(movieMetadata.get(Video.BITRATE).get());

        if ( movieMetadata.getAudioKeys().size() > 0 ) {
            // Take only the digits and discard the chars
            audioBitrate = (int) Double.parseDouble(strExtractNumbers(movieMetadata.get(Audio.BITRATE).get()));
        }
    }

    @Override
    public String toString() {
        String pathStr = path.toString();
        String[] split = pathStr.split(Pattern.quote(File.separator));

        int splitLength = split.length;

        StringBuilder shortPathStr = new StringBuilder();
        int depth = toStringDepth + 1;
        for (int i = splitLength - toStringDepth; i <= depth; i++) {
            shortPathStr.append(split[i]);
            if (i != depth) {
                shortPathStr.append(File.separator);
            }
        }

        return shortPathStr.toString();
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
