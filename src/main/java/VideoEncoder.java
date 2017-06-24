import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.List;

public class VideoEncoder {
    protected List<String> videoList;

    private VideoEncoder(String path) {
        this.findVideos(path);

        if (videoList.size() > 0) {
            this.readInfo();
        }
    }

    private void readInfo() {

    }

    public static void main(String[] args) {
        String path = args[0];
        System.out.println(path);

        new VideoEncoder(path);
    }

    private void findVideos(String path) {
        File dir = new File(path);
        File[] files = dir.listFiles();
        assert files != null;

        for (File file : files) {
            if (file.isDirectory()) {
                this.findVideos(file.getPath());
            }

            if (file.getName().endsWith("avi")) {
                videoList.add(file.getPath());
            }
        }
    }
}
