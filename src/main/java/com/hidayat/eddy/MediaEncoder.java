package com.hidayat.eddy;

import com.hidayat.eddy.comp.PathListRenderer;
import com.hidayat.eddy.comp.VideoFile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

        ok.addActionListener((ActionEvent e) -> {

            List<VideoFile> selectedValuesList = pathList.getSelectedValuesList();
            System.out.println(e);
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
            try {
                videos.addElement(new VideoFile(path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        pathList.setCellRenderer(new PathListRenderer<>());
        pathList.setModel(videos);
        pathList.setSelectionInterval(0, videos.getSize() - 1);
    }
}
