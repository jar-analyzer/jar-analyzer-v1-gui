package com.chaitin.jar.analyzer.adapter;

import com.chaitin.jar.analyzer.core.MethodReference;
import com.chaitin.jar.analyzer.form.JarAnalyzerForm;
import com.chaitin.jar.analyzer.model.ClassObj;
import com.chaitin.jar.analyzer.model.MappingObj;
import com.chaitin.jar.analyzer.model.ResObj;
import com.chaitin.jar.analyzer.spring.SpringController;
import com.chaitin.jar.analyzer.spring.SpringMapping;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ControllerMouseAdapter extends MouseAdapter {

    private final JarAnalyzerForm form;

    public ControllerMouseAdapter(JarAnalyzerForm form) {
        this.form = form;
    }

    @Override
    public void mouseClicked(MouseEvent evt) {
        JList<?> list = (JList<?>) evt.getSource();
        if (evt.getClickCount() == 2) {
            JList<?> l = (JList<?>) evt.getSource();
            ListModel<?> m = l.getModel();
            int index = l.locationToIndex(evt.getPoint());
            if (index > -1) {
                ClassObj res = (ClassObj) m.getElementAt(index);
                for (SpringController controller : JarAnalyzerForm.controllers) {
                    if (controller.getClassName().equals(res.getHandle())) {
                        DefaultListModel<MappingObj> mappingDataList = new DefaultListModel<>();
                        for (SpringMapping mapping : controller.getMappings()) {
                            for (MethodReference.Handle mh : JarAnalyzerForm.methodMap.keySet()) {
                                if (mh.equals(mapping.getMethodName())) {
                                    MappingObj mappingObj = new MappingObj();
                                    mappingObj.setResObj(new ResObj(mh, mh.getClassReference().getName()));
                                    mappingObj.setSpringMapping(mapping);
                                    mappingDataList.addElement(mappingObj);
                                }
                            }
                        }
                        form.mappingJList.setModel(mappingDataList);
                    }
                }
                form.useSpringBootJarCheckBox.setSelected(true);
                JarAnalyzerForm.springBootJar = true;
                form.coreClass(evt, list);
            }
        }
    }
}
