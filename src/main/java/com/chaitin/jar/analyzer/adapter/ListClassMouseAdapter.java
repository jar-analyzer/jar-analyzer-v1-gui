package com.chaitin.jar.analyzer.adapter;

import com.chaitin.jar.analyzer.form.JarAnalyzerForm;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ListClassMouseAdapter extends MouseAdapter {
    private final JarAnalyzerForm form;

    public ListClassMouseAdapter(JarAnalyzerForm form) {
        this.form = form;
    }

    public void mouseClicked(MouseEvent evt) {
        JList<?> list = (JList<?>) evt.getSource();
        if (evt.getClickCount() == 2) {
            form.coreClass(evt, list);
        }
    }
}