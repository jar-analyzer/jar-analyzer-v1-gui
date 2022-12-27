package com.chaitin.jar.analyzer.adapter;

import com.chaitin.jar.analyzer.core.ClassReference;
import com.chaitin.jar.analyzer.core.MethodReference;
import com.chaitin.jar.analyzer.form.JarAnalyzerForm;
import com.chaitin.jar.analyzer.model.ClassObj;
import com.chaitin.jar.analyzer.model.MappingObj;
import com.chaitin.jar.analyzer.model.ResObj;
import com.chaitin.jar.analyzer.util.OSUtil;
import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.PlainTextOutput;
import org.benf.cfr.reader.Main;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.objectweb.asm.Type;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MappingMouseAdapter extends MouseAdapter {

    private final JarAnalyzerForm form;

    public MappingMouseAdapter(JarAnalyzerForm form) {
        this.form = form;
    }

    public void mousePressed(MouseEvent evt) {
        JList<?> list = (JList<?>) evt.getSource();
        if (SwingUtilities.isRightMouseButton(evt) || evt.isControlDown()) {
            int index = list.locationToIndex(evt.getPoint());
            MappingObj res = (MappingObj) list.getModel().getElementAt(index);
            JarAnalyzerForm.chainDataList.addElement(res.getResObj());
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
                MappingObj res = (MappingObj) m.getElementAt(index);
                l.setToolTipText(res.getResObj().getMethod().getDescStd());

                ToolTipManager.sharedInstance().mouseMoved(
                        new MouseEvent(l, 0, 0, 0,
                                evt.getX(), evt.getY(), 0, false));
            }
        } else if (evt.getClickCount() == 2) {
            int index = list.locationToIndex(evt.getPoint());
            MappingObj res = (MappingObj) list.getModel().getElementAt(index);

            String className = res.getResObj().getClassName();
            String classPath = className.replace("/", File.separator);
            if (!JarAnalyzerForm.springBootJar) {
                classPath = String.format("temp%s%s.class", File.separator, classPath);
            } else {
                if (classPath.contains("springframework")) {
                    classPath = String.format("temp%s%s.class", File.separator, classPath);
                } else {
                    classPath = String.format("temp%sBOOT-INF%sclasses%s%s.class",
                            File.separator, File.separator, File.separator, classPath);
                }
            }

            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            OutputStreamWriter ows = new OutputStreamWriter(bao);
            BufferedWriter writer = new BufferedWriter(ows);

            String finalClassPath = classPath;

            String[] temp;
            if (OSUtil.isWindows()) {
                temp = finalClassPath.split(File.separator + File.separator);
            } else {
                temp = finalClassPath.split(File.separator);
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < temp.length - 1; i++) {
                sb.append(temp[i]);
                sb.append(File.separator);
            }
            String javaDir = sb.toString();
            String tempName = temp[temp.length - 1].split("\\.class")[0];
            String javaPath = sb.append(tempName).append(".java").toString();
            Path javaPathPath = Paths.get(javaPath);

            new Thread(() -> {
                String total;
                JarAnalyzerForm.historyDataList.addElement(res.getResObj());
                if (form.procyonRadioButton.isSelected()) {
                    Decompiler.decompile(
                            finalClassPath,
                            new PlainTextOutput(writer));
                    try {
                        writer.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    total = bao.toString();
                    if (total == null || total.trim().equals("")) {
                        total = JarAnalyzerForm.tips;
                    } else {
                        total = "// Procyon \n" + total;
                    }
                } else if (form.quiltFlowerRadioButton.isSelected()) {
                    String[] args = new String[]{
                            finalClassPath,
                            javaDir
                    };
                    try {
                        Files.delete(javaPathPath);
                    } catch (IOException ignored) {
                    }
                    ConsoleDecompiler.main(args);
                    try {
                        total = new String(Files.readAllBytes(javaPathPath));
                        if (total.trim().equals("")) {
                            total = JarAnalyzerForm.tips;
                        } else {
                            total = "// QuiltFlower \n" + total;
                        }
                    } catch (Exception ignored) {
                        total = "";
                    }
                    try {
                        Files.delete(javaPathPath);
                    } catch (IOException ignored) {
                    }
                } else if (form.cfrRadioButton.isSelected()) {
                    String[] args = new String[]{
                            finalClassPath,
                            "--outputpath",
                            "temp"
                    };
                    try {
                        Files.delete(javaPathPath);
                    } catch (IOException ignored) {
                    }
                    Main.main(args);
                    try {
                        if (JarAnalyzerForm.springBootJar) {
                            total = new String(Files.readAllBytes(
                                    Paths.get(String.format("temp%s%s",File.separator,
                                            javaPathPath.toString().substring(22)))
                            ));
                        }else{
                            total = new String(Files.readAllBytes(javaPathPath));
                        }
                    } catch (Exception ignored) {
                        total = "";
                    }
                    if (total.trim().equals("")) {
                        total = JarAnalyzerForm.tips;
                    }
                    try {
                        Files.delete(javaPathPath);
                    } catch (IOException ignored) {
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Error!");
                    return;
                }
                form.editorPane.setText(total);

                String methodName = res.getResObj().getMethod().getName();
                if (methodName.equals("<init>")) {
                    String[] c = res.getResObj().getClassName().split("/");
                    methodName = c[c.length - 1];
                }

                for (int i = total.indexOf(methodName);
                     i >= 0; i = total.indexOf(methodName, i + 1)) {
                    if (total.charAt(i + methodName.length()) == '(') {
                        int paramNum = Type.getMethodType(
                                res.getResObj().getMethod().getDesc()).getArgumentTypes().length;
                        int curNum = 1;
                        for (int j = i + methodName.length(); ; j++) {
                            if (total.charAt(j) == ')') {
                                if (total.charAt(j - 1) == '(') {
                                    curNum = 0;
                                }
                                if (curNum == paramNum) {
                                    form.editorPane.setCaretPosition(i);
                                }
                                break;
                            } else if (total.charAt(j) == ',') {
                                curNum++;
                            }
                        }
                    }
                }
            }).start();

            JOptionPane.showMessageDialog(null, "Decompiling...");

            DefaultListModel<ResObj> sourceDataList = new DefaultListModel<>();
            DefaultListModel<ResObj> callDataList = new DefaultListModel<>();
            DefaultListModel<ClassObj> subDataList = new DefaultListModel<>();
            DefaultListModel<ClassObj> superDataList = new DefaultListModel<>();

            MethodReference.Handle handle = res.getResObj().getMethod();
            HashSet<MethodReference.Handle> callMh = JarAnalyzerForm.methodCalls.get(handle);
            for (MethodReference.Handle m : callMh) {
                callDataList.addElement(new ResObj(m, m.getClassReference().getName()));
            }
            for (Map.Entry<MethodReference.Handle,
                    HashSet<MethodReference.Handle>> entry : JarAnalyzerForm.methodCalls.entrySet()) {
                MethodReference.Handle mh = entry.getKey();
                HashSet<MethodReference.Handle> mSet = entry.getValue();
                for (MethodReference.Handle m : mSet) {
                    if (m.getClassReference().getName().equals(className)) {
                        if (m.getName().equals(res.getResObj().getMethod().getName())) {
                            sourceDataList.addElement(new ResObj(
                                    mh, mh.getClassReference().getName()));
                        }
                    }
                }
            }

            Set<ClassReference.Handle> subClasses = JarAnalyzerForm.inheritanceMap.getSubClasses(handle.getClassReference());
            if (subClasses != null && subClasses.size() != 0) {
                for (ClassReference.Handle c : subClasses) {
                    ClassObj obj = new ClassObj(c.getName(), c);
                    subDataList.addElement(obj);
                }
            }

            Set<ClassReference.Handle> superClasses = JarAnalyzerForm.inheritanceMap.getSuperClasses(handle.getClassReference());
            if (superClasses != null && superClasses.size() != 0) {
                for (ClassReference.Handle c : superClasses) {
                    ClassObj obj = new ClassObj(c.getName(), c);
                    superDataList.addElement(obj);
                }
            }

            JarAnalyzerForm.curRes = res.getResObj();
            form.currentLabel.setText(res.toString());
            form.currentLabel.setToolTipText(res.getResObj().getMethod().getDescStd());

            form.sourceList.setModel(sourceDataList);
            form.callList.setModel(callDataList);
            form.subList.setModel(subDataList);
            form.superList.setModel(superDataList);
            form.historyList.setModel(JarAnalyzerForm.historyDataList);
        }
    }
}
