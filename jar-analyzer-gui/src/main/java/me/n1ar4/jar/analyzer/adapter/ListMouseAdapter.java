package me.n1ar4.jar.analyzer.adapter;

import me.n1ar4.jar.analyzer.form.JarAnalyzerForm;
import me.n1ar4.jar.analyzer.model.ResObj;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ListMouseAdapter extends MouseAdapter {

    private final JarAnalyzerForm form;

    public ListMouseAdapter(JarAnalyzerForm form) {
        this.form = form;
    }

    public void mousePressed(MouseEvent evt) {
        JList<?> list = (JList<?>) evt.getSource();
        if (SwingUtilities.isRightMouseButton(evt) || evt.isControlDown()) {
            int index = list.locationToIndex(evt.getPoint());
            ResObj res = (ResObj) list.getModel().getElementAt(index);
            JarAnalyzerForm.chainDataList.addElement(res);
            form.chanList.setModel(JarAnalyzerForm.chainDataList);
        }
    }

    public void mouseClicked(MouseEvent evt) {
        JList<?> list = (JList<?>) evt.getSource();
        if (evt.getClickCount() == 1) {
            JList<?> l = (JList<?>) evt.getSource();
            ListModel<?> m = l.getModel();
            int index = l.locationToIndex(evt.getPoint());
            if (index > -1) {
                ResObj res = (ResObj) m.getElementAt(index);
                l.setToolTipText(res.getMethod().getDescStd(res));

                ToolTipManager.sharedInstance().mouseMoved(
                        new MouseEvent(l, 0, 0, 0,
                                evt.getX(), evt.getY(), 0, false));
            }
        } else if (evt.getClickCount() == 2) {
            form.core(evt, list);
        }
    }
}