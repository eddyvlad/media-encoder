package com.hidayat.eddy;

import com.hidayat.eddy.renderers.PathListRenderer;
import com.hidayat.eddy.components.VideoItem;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("ConstantConditions")
public class MediaEncoder extends JPanel {
    private String[] supportedExtensions = {"avi", "3gp", "wmv", "mov", "mpg"};
    private JPanel mainPanel;
    private JTextField dirField;
    private JButton browseButton;
    private JButton ok;
    private JScrollPane scrollPane;
    private JList<VideoItem> pathList;
    private JPanel statusPanel;
    private JLabel statusLabel;
    private JButton backupOriginalVideos;

    private MediaEncoder() {
        browseButton.addActionListener(e -> browseBtnActionListener());
        ok.addActionListener(e -> okActionListener());
        backupOriginalVideos.addActionListener(e -> backupOriginalVideos());
    }

    public static void main(String[] args) {
        JFrame jFrame = new JFrame("Media Encoder");
        jFrame.setContentPane(new MediaEncoder().mainPanel);
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.pack();

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        // Middle center the window
        jFrame.setLocation(dim.width / 2 - jFrame.getSize().width / 2, dim.height / 2 - jFrame.getSize().height / 2);
        jFrame.setVisible(true);
    }

    private void setSelectedDir(File selectedDir) {
        dirField.setText(selectedDir.toString());
        pathList.removeAll();

        // Wait
        SwingWorker worker = new ScanDirectoryWorker(selectedDir, supportedExtensions);
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
        DefaultListModel<VideoItem> videos = new DefaultListModel<>();
        pathList.setAutoscrolls(true);
        pathList.setCellRenderer(new PathListRenderer<>());
        pathList.setModel(videos);

        SwingWorker swingWorker = new SwingWorker<DefaultListModel<VideoItem>, Void>() {
            @Override
            protected DefaultListModel<VideoItem> doInBackground() throws Exception {
                int index = 0;
                for (Path path : videoList) {
                    try {
                        videos.addElement(new VideoItem(path));
                        pathList.ensureIndexIsVisible(index);
                        index++;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                return videos;
            }
        };

        swingWorker.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            if (evt.getNewValue() == SwingWorker.StateValue.DONE) {
                pathList.setSelectionInterval(0, videos.getSize() - 1);
            }
        });

        swingWorker.execute();
    }

    /**
     * Move the old converted videos to a backup directory
     */
    private void backupOriginalVideos() {
        Iterator<VideoItem> selectedValuesList = pathList.getSelectedValuesList().iterator();

        SwingWorker swingWorker = new SwingWorker<Iterator<VideoItem>, Void>() {
            @Override
            protected Iterator<VideoItem> doInBackground() throws Exception {
                selectedValuesList.forEachRemaining((VideoItem videoFile) -> {
                    Path thisPath = videoFile.path;
                    Path rootName = thisPath.getName(0);
                    String newRootName = rootName + " - backup";

                    String ps = thisPath.getFileSystem().getSeparator();
                    StringBuilder newPathStr = new StringBuilder();
                    newPathStr.append(thisPath.getRoot())
                            .append(newRootName)
                            .append(ps);

                    int nameCount = thisPath.getNameCount();
                    for (int i = 1; i < nameCount; i++) {
                        newPathStr.append(thisPath.getName(i));
                        if (i + 1 < nameCount) {
                            newPathStr.append(ps);
                        }
                    }

                    Path newPath = Paths.get(newPathStr.toString());
                    try {
                        Files.createDirectories(newPath.getParent());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    try {
                        Files.move(thisPath, newPath);
                    } catch (FileAlreadyExistsException ignored) {
                        // Ignore files already exists
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                    ((DefaultListModel<VideoItem>) pathList.getModel()).removeElement(videoFile);
                });

                return selectedValuesList;
            }
        };

        swingWorker.execute();
    }

    private void okActionListener() {
        scrollPane.setVisible(false);

        JProgressBar jProgressBar = new JProgressBar();
        jProgressBar.setLayout(new BorderLayout());
        jProgressBar.setStringPainted(true);
        jProgressBar.setPreferredSize(new Dimension(statusPanel.getWidth(), 100));

        JLabel jLabel = new JLabel();

        statusPanel.setLayout(new BorderLayout());
        statusPanel.add(jLabel, BorderLayout.NORTH);
        statusPanel.add(jProgressBar, BorderLayout.AFTER_LAST_LINE);
        statusPanel.updateUI();

        SwingWorker worker = new VideoConversionWorker(pathList, jLabel, jProgressBar);
        worker.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            switch ((SwingWorker.StateValue) evt.getNewValue()) {
                case PENDING:
                    break;
                case STARTED:
                    ok.setVisible(false);
                    break;
                case DONE:
                    scrollPane.setVisible(true);
                    ok.setVisible(true);
                    statusPanel.remove(jLabel);
                    statusPanel.remove(jProgressBar);
                    setFileTime();
                    backupOriginalVideos();
                    break;
            }
        });

        worker.execute();
    }

    private void setFileTime() {
        List<VideoItem> selectedVideos = pathList.getSelectedValuesList();
        for (VideoItem videoItem : selectedVideos) {
            try {
                videoItem.setOriginalFileTime();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void browseBtnActionListener() {
        String userHomeDir = System.getProperty("user.home");

        JFileChooser choose = new JFileChooser();
        choose.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        choose.setAcceptAllFileFilterUsed(false);
        choose.setCurrentDirectory(new File(userHomeDir));
        int result = choose.showOpenDialog(browseButton);

        // When user open a file/dir
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = choose.getSelectedFile();
            setSelectedDir(selectedDir);
        }
    }
}
