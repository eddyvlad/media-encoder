package com.hidayat.eddy.comp;

import javax.swing.*;
import java.awt.*;

public class PathListRenderer<T> extends JLabel implements javax.swing.ListCellRenderer<T> {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        VideoFile entry = (VideoFile) value;
        setText((index + 1) + ". " + entry.toString());

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        setOpaque(true);
        return this;
    }
}
