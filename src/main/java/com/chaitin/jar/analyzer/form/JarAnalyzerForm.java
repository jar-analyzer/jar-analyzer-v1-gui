package com.chaitin.jar.analyzer.form;

import com.chaitin.jar.analyzer.core.*;
import com.chaitin.jar.analyzer.util.CoreUtil;
import com.chaitin.jar.analyzer.util.DirUtil;
import com.chaitin.jar.analyzer.util.OSUtil;
import com.formdev.flatlaf.FlatLightLaf;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.PlainTextOutput;
import jsyntaxpane.syntaxkits.JavaSyntaxKit;
import org.benf.cfr.reader.Main;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.objectweb.asm.Type;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class JarAnalyzerForm {

    private class ListMouseAdapter extends MouseAdapter {
        public void mousePressed(MouseEvent evt) {
            JList<?> list = (JList<?>) evt.getSource();
            if (SwingUtilities.isRightMouseButton(evt) || evt.isControlDown()) {
                int index = list.locationToIndex(evt.getPoint());
                ResObj res = (ResObj) list.getModel().getElementAt(index);
                chainDataList.addElement(res);
                chanList.setModel(chainDataList);
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
                    l.setToolTipText(res.getMethod().getDescStd());

                    ToolTipManager.sharedInstance().mouseMoved(
                            new MouseEvent(l, 0, 0, 0,
                                    evt.getX(), evt.getY(), 0, false));
                }
            } else if (evt.getClickCount() == 2) {
                core(evt, list);
            }
        }
    }

    private class ListClassMouseAdapter extends MouseAdapter {
        public void mouseClicked(MouseEvent evt) {
            JList<?> list = (JList<?>) evt.getSource();
            if (evt.getClickCount() == 2) {
                coreClass(evt, list);
            }
        }
    }

    private JButton startAnalysisButton;
    private JPanel jarAnalyzerPanel;
    private JPanel topPanel;
    private JButton selectJarFileButton;
    private JPanel editorPanel;
    private JScrollPane editorScroll;
    private JLabel authorLabel;
    private JPanel authorPanel;
    private JEditorPane editorPane;
    private JList<ResObj> resultList;
    private JPanel resultPane;
    private JScrollPane resultScroll;
    private JTextField classText;
    private JTextField methodText;
    private JLabel methodLabel;
    private JLabel classLabel;
    private JLabel jarInfoLabel;
    private JTextField jarInfoResultText;
    private JScrollPane sourceScroll;
    private JList<ResObj> sourceList;
    private JList<ResObj> callList;
    private JScrollPane callScroll;
    private JList<ResObj> chanList;
    private JPanel configPanel;
    private JScrollPane chanScroll;
    private JTextField currentLabel;
    private JRadioButton procyonRadioButton;
    private JRadioButton cfrRadioButton;
    private JRadioButton quiltFlowerRadioButton;
    private JPanel opPanel;
    private JPanel dcPanel;
    private JPanel curPanel;
    private JLabel curLabel;
    private JLabel progressLabel;
    private JProgressBar progress;
    private JTabbedPane callPanel;
    private JTabbedPane classInfoPanel;
    private JScrollPane subScroll;
    private JScrollPane superScroll;
    private JList<ClassObj> subList;
    private JList<ClassObj> superList;
    private JList<ResObj> historyList;
    private JScrollPane historyScroll;

    private static final DefaultListModel<ResObj> historyDataList = new DefaultListModel<>();

    public static Set<ClassFile> classFileList = new HashSet<>();
    private static final Set<ClassReference> discoveredClasses = new HashSet<>();
    private static final Set<MethodReference> discoveredMethods = new HashSet<>();
    private static final Map<ClassReference.Handle, ClassReference> classMap = new HashMap<>();
    private static final Map<MethodReference.Handle, MethodReference> methodMap = new HashMap<>();
    private static final HashMap<MethodReference.Handle,
            HashSet<MethodReference.Handle>> methodCalls = new HashMap<>();
    private static InheritanceMap inheritanceMap;

    private static int totalJars = 0;

    private void loadJar() {
        selectJarFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int option = fileChooser.showOpenDialog(new JFrame());
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String absPath = file.getAbsolutePath();
                totalJars++;
                progress.setValue(0);
                new Thread(() -> {
                    List<String> jarPathList = new ArrayList<>();
                    if (Files.isDirectory(Paths.get(absPath))) {
                        List<String> data = DirUtil.GetFiles(absPath);
                        for (String d : data) {
                            if (d.endsWith(".jar")) {
                                jarPathList.add(d);
                            }
                        }
                    } else {
                        jarPathList.add(absPath);
                    }
                    progress.setValue(20);
                    classFileList.addAll(CoreUtil.getAllClassesFromJars(jarPathList));
                    progress.setValue(50);
                    System.out.println(classFileList.size());
                    Discovery.start(classFileList, discoveredClasses,
                            discoveredMethods, classMap, methodMap);
                    jarInfoResultText.setText(String.format(
                            "total jars: %d   total classes: %s   total methods: %s",
                            totalJars, discoveredClasses.size(), discoveredMethods.size()
                    ));
                    progress.setValue(80);
                    MethodCall.start(classFileList, methodCalls);
                    inheritanceMap = Inheritance.derive(classMap);
                    Map<MethodReference.Handle, Set<MethodReference.Handle>> implMap =
                            Inheritance.getAllMethodImplementations(inheritanceMap, methodMap);
                    for (Map.Entry<MethodReference.Handle, Set<MethodReference.Handle>> entry :
                            implMap.entrySet()) {
                        MethodReference.Handle k = entry.getKey();
                        Set<MethodReference.Handle> v = entry.getValue();
                        HashSet<MethodReference.Handle> calls = methodCalls.get(k);
                        calls.addAll(v);
                    }
                    progress.setValue(100);
                }).start();
            }
        });

        startAnalysisButton.addActionListener(e -> {
            DefaultListModel<ResObj> searchList = new DefaultListModel<>();

            String inputClass = classText.getText();
            String className;
            if (inputClass != null && !inputClass.trim().equals("")) {
                className = inputClass.trim().replace(".", "/").trim();
            } else {
                className = "ALL";
            }
            String shortClassName;
            if (className.contains("/")) {
                String[] temp = className.split("/");
                shortClassName = temp[temp.length - 1];
            } else {
                shortClassName = className;
            }
            String methodName = methodText.getText().trim();

            for (Map.Entry<MethodReference.Handle,
                    HashSet<MethodReference.Handle>> entry : methodCalls.entrySet()) {
                MethodReference.Handle k = entry.getKey();
                HashSet<MethodReference.Handle> v = entry.getValue();

                for (MethodReference.Handle h : v) {
                    String c = h.getClassReference().getName();
                    String[] st = c.split("/");
                    String s = st[st.length - 1];
                    if (className.equals("ALL")) {
                        if (h.getName().equals(methodName)) {
                            searchList.addElement(new ResObj(k, k.getClassReference().getName()));
                        }
                    } else if (c.equals(className) || s.equals(shortClassName)) {
                        if (h.getName().equals(methodName)) {
                            searchList.addElement(new ResObj(k, k.getClassReference().getName()));
                        }
                    }
                }
            }

            resultList.setModel(searchList);
        });
    }

    private static final DefaultListModel<ResObj> chainDataList = new DefaultListModel<>();

    private void coreClass(MouseEvent evt, JList<?> list) {
        int index = list.locationToIndex(evt.getPoint());
        ClassObj res = (ClassObj) list.getModel().getElementAt(index);
        String className = res.getClassName();
        String classPath = className.replace("/", File.separator);
        classPath = String.format("temp%s%s.class", File.separator, classPath);

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
            if (procyonRadioButton.isSelected()) {
                Decompiler.decompile(
                        finalClassPath,
                        new PlainTextOutput(writer));
                try {
                    writer.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                total = bao.toString();
                total = "// Procyon \n" + total;
            } else if (quiltFlowerRadioButton.isSelected()) {
                String[] args = new String[]{
                        finalClassPath,
                        javaDir
                };
                ConsoleDecompiler.main(args);
                try {
                    total = new String(Files.readAllBytes(javaPathPath));
                    total = "// QuiltFlower \n" + total;
                    Files.delete(javaPathPath);
                } catch (Exception ignored) {
                    total = "";
                }
            } else if (cfrRadioButton.isSelected()) {
                String[] args = new String[]{
                        finalClassPath,
                        "--outputpath",
                        "temp"
                };
                Main.main(args);
                try {
                    total = new String(Files.readAllBytes(javaPathPath));
                    Files.delete(javaPathPath);
                } catch (Exception ignored) {
                    total = "";
                }
            } else {
                JOptionPane.showMessageDialog(null, "Error!");
                return;
            }
            editorPane.setText(total);
            editorPane.setCaretPosition(0);
        }).start();

        DefaultListModel<ClassObj> subDataList = new DefaultListModel<>();
        DefaultListModel<ClassObj> superDataList = new DefaultListModel<>();

        Set<ClassReference.Handle> subClasses = inheritanceMap.getSubClasses(res.getHandle());
        if (subClasses != null && subClasses.size() != 0) {
            for (ClassReference.Handle c : subClasses) {
                ClassObj obj = new ClassObj(c.getName(), c);
                subDataList.addElement(obj);
            }
        }

        Set<ClassReference.Handle> superClasses = inheritanceMap.getSuperClasses(res.getHandle());
        if (superClasses != null && superClasses.size() != 0) {
            for (ClassReference.Handle c : superClasses) {
                ClassObj obj = new ClassObj(c.getName(), c);
                superDataList.addElement(obj);
            }
        }

        currentLabel.setText(res.toString());

        subList.setModel(subDataList);
        superList.setModel(superDataList);
    }

    private void core(MouseEvent evt, JList<?> list) {
        int index = list.locationToIndex(evt.getPoint());
        ResObj res = (ResObj) list.getModel().getElementAt(index);

        String className = res.getClassName();
        String classPath = className.replace("/", File.separator);
        classPath = String.format("temp%s%s.class", File.separator, classPath);

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
            historyDataList.addElement(res);
            if (procyonRadioButton.isSelected()) {
                Decompiler.decompile(
                        finalClassPath,
                        new PlainTextOutput(writer));
                try {
                    writer.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                total = bao.toString();
                total = "// Procyon \n" + total;
            } else if (quiltFlowerRadioButton.isSelected()) {
                String[] args = new String[]{
                        finalClassPath,
                        javaDir
                };
                ConsoleDecompiler.main(args);
                try {
                    total = new String(Files.readAllBytes(javaPathPath));
                    total = "// QuiltFlower \n" + total;
                    Files.delete(javaPathPath);
                } catch (Exception ignored) {
                    total = "";
                }
            } else if (cfrRadioButton.isSelected()) {
                String[] args = new String[]{
                        finalClassPath,
                        "--outputpath",
                        "temp"
                };
                Main.main(args);
                try {
                    total = new String(Files.readAllBytes(javaPathPath));
                    Files.delete(javaPathPath);
                } catch (Exception ignored) {
                    total = "";
                }
            } else {
                JOptionPane.showMessageDialog(null, "Error!");
                return;
            }
            editorPane.setText(total);

            String methodName = res.getMethod().getName();
            if (methodName.equals("<init>")) {
                String[] c = res.getClassName().split("/");
                methodName = c[c.length - 1];
            }

            for (int i = total.indexOf(methodName);
                 i >= 0; i = total.indexOf(methodName, i + 1)) {
                if (total.charAt(i + methodName.length()) == '(') {
                    int paramNum = Type.getMethodType(
                            res.getMethod().getDesc()).getArgumentTypes().length;
                    int curNum = 1;
                    for (int j = i + methodName.length(); ; j++) {
                        if (total.charAt(j) == ')') {
                            if (total.charAt(j - 1) == '(') {
                                curNum = 0;
                            }
                            if (curNum == paramNum) {
                                editorPane.setCaretPosition(i);
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

        MethodReference.Handle handle = res.getMethod();
        HashSet<MethodReference.Handle> callMh = methodCalls.get(handle);
        for (MethodReference.Handle m : callMh) {
            callDataList.addElement(new ResObj(m, m.getClassReference().getName()));
        }
        for (Map.Entry<MethodReference.Handle,
                HashSet<MethodReference.Handle>> entry : methodCalls.entrySet()) {
            MethodReference.Handle mh = entry.getKey();
            HashSet<MethodReference.Handle> mSet = entry.getValue();
            for (MethodReference.Handle m : mSet) {
                if (m.getClassReference().getName().equals(className)) {
                    if (m.getName().equals(res.getMethod().getName())) {
                        sourceDataList.addElement(new ResObj(
                                mh, mh.getClassReference().getName()));
                    }
                }
            }
        }

        Set<ClassReference.Handle> subClasses = inheritanceMap.getSubClasses(handle.getClassReference());
        if (subClasses != null && subClasses.size() != 0) {
            for (ClassReference.Handle c : subClasses) {
                ClassObj obj = new ClassObj(c.getName(), c);
                subDataList.addElement(obj);
            }
        }

        Set<ClassReference.Handle> superClasses = inheritanceMap.getSuperClasses(handle.getClassReference());
        if (superClasses != null && superClasses.size() != 0) {
            for (ClassReference.Handle c : superClasses) {
                ClassObj obj = new ClassObj(c.getName(), c);
                superDataList.addElement(obj);
            }
        }

        currentLabel.setText(res.toString());
        currentLabel.setToolTipText(res.getMethod().getDescStd());

        sourceList.setModel(sourceDataList);
        callList.setModel(callDataList);
        subList.setModel(subDataList);
        superList.setModel(superDataList);
        historyList.setModel(historyDataList);
    }

    public JarAnalyzerForm() {
        DirUtil.removeDir(new File("temp"));

        quiltFlowerRadioButton.setSelected(true);

        editorPane.setEditorKit(new JavaSyntaxKit());
        loadJar();

        ToolTipManager.sharedInstance().setDismissDelay(10000);
        ToolTipManager.sharedInstance().setInitialDelay(300);

        resultList.addMouseListener(new ListMouseAdapter());
        callList.addMouseListener(new ListMouseAdapter());
        sourceList.addMouseListener(new ListMouseAdapter());
        subList.addMouseListener(new ListClassMouseAdapter());
        superList.addMouseListener(new ListClassMouseAdapter());
        historyList.addMouseListener(new ListMouseAdapter());

        chanList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent evt) {
                JList<?> list = (JList<?>) evt.getSource();
                if (SwingUtilities.isRightMouseButton(evt) || evt.isControlDown()) {
                    int index = list.locationToIndex(evt.getPoint());
                    ResObj res = (ResObj) list.getModel().getElementAt(index);
                    chainDataList.removeElement(res);
                    chanList.setModel(chainDataList);
                }
            }

            @Override
            public void mouseClicked(MouseEvent evt) {
                JList<?> list = (JList<?>) evt.getSource();
                if (evt.getClickCount() == 1) {
                    JList<?> l = (JList<?>) evt.getSource();
                    ListModel<?> m = l.getModel();
                    int index = l.locationToIndex(evt.getPoint());
                    if (index > -1) {
                        ResObj res = (ResObj) m.getElementAt(index);
                        l.setToolTipText(res.getMethod().getDescStd());

                        ToolTipManager.sharedInstance().mouseMoved(
                                new MouseEvent(l, 0, 0, 0,
                                        evt.getX(), evt.getY(), 0, false));
                    }
                } else if (evt.getClickCount() == 2) {
                    core(evt, list);
                }
            }
        });
    }

    public static void start() {
        FlatLightLaf.setup();
        JFrame frame = new JFrame("Jar Analyzer");
        frame.setContentPane(new JarAnalyzerForm().jarAnalyzerPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        jarAnalyzerPanel = new JPanel();
        jarAnalyzerPanel.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        jarAnalyzerPanel.setBackground(new Color(-725535));
        topPanel = new JPanel();
        topPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        topPanel.setBackground(new Color(-725535));
        jarAnalyzerPanel.add(topPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        opPanel = new JPanel();
        opPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        opPanel.setBackground(new Color(-725535));
        topPanel.add(opPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        selectJarFileButton = new JButton();
        selectJarFileButton.setText("Select Jar File");
        opPanel.add(selectJarFileButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        startAnalysisButton = new JButton();
        startAnalysisButton.setText("Start Analysis");
        opPanel.add(startAnalysisButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dcPanel = new JPanel();
        dcPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        dcPanel.setBackground(new Color(-725535));
        topPanel.add(dcPanel, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        procyonRadioButton = new JRadioButton();
        procyonRadioButton.setBackground(new Color(-725535));
        procyonRadioButton.setText("Procyon");
        dcPanel.add(procyonRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cfrRadioButton = new JRadioButton();
        cfrRadioButton.setBackground(new Color(-725535));
        cfrRadioButton.setText("CFR");
        dcPanel.add(cfrRadioButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        quiltFlowerRadioButton = new JRadioButton();
        quiltFlowerRadioButton.setBackground(new Color(-725535));
        quiltFlowerRadioButton.setText("QuiltFlower");
        dcPanel.add(quiltFlowerRadioButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        editorPanel = new JPanel();
        editorPanel.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        editorPanel.setBackground(new Color(-725535));
        jarAnalyzerPanel.add(editorPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        editorScroll = new JScrollPane();
        editorScroll.setBackground(new Color(-725535));
        editorPanel.add(editorScroll, new GridConstraints(0, 0, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(600, 400), new Dimension(600, 500), null, 0, false));
        editorScroll.setBorder(BorderFactory.createTitledBorder(null, "Decompile Java Code", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        editorPane = new JEditorPane();
        editorScroll.setViewportView(editorPane);
        curPanel = new JPanel();
        curPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        curPanel.setBackground(new Color(-725535));
        editorPanel.add(curPanel, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        curLabel = new JLabel();
        curLabel.setText("Current:");
        curPanel.add(curLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        currentLabel = new JTextField();
        currentLabel.setEditable(false);
        curPanel.add(currentLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        callPanel = new JTabbedPane();
        callPanel.setBackground(new Color(-528927));
        callPanel.setForeground(new Color(-16777216));
        editorPanel.add(callPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(600, -1), null, null, 0, false));
        sourceScroll = new JScrollPane();
        sourceScroll.setBackground(new Color(-725535));
        callPanel.addTab("Who Call Target", sourceScroll);
        sourceScroll.setBorder(BorderFactory.createTitledBorder(null, "", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        sourceList = new JList();
        sourceScroll.setViewportView(sourceList);
        callScroll = new JScrollPane();
        callScroll.setBackground(new Color(-725535));
        callPanel.addTab("Target Call Whom", callScroll);
        callScroll.setBorder(BorderFactory.createTitledBorder(null, "", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        callList = new JList();
        callScroll.setViewportView(callList);
        historyScroll = new JScrollPane();
        callPanel.addTab("Decompile History", historyScroll);
        historyList = new JList();
        historyScroll.setViewportView(historyList);
        classInfoPanel = new JTabbedPane();
        classInfoPanel.setBackground(new Color(-528927));
        classInfoPanel.setForeground(new Color(-16777216));
        editorPanel.add(classInfoPanel, new GridConstraints(1, 1, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        subScroll = new JScrollPane();
        subScroll.setBackground(new Color(-528927));
        classInfoPanel.addTab("All Subclasses", subScroll);
        subList = new JList();
        subScroll.setViewportView(subList);
        superScroll = new JScrollPane();
        superScroll.setBackground(new Color(-528927));
        classInfoPanel.addTab("All Superclasses", superScroll);
        superList = new JList();
        superScroll.setViewportView(superList);
        authorPanel = new JPanel();
        authorPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        authorPanel.setBackground(new Color(-725535));
        jarAnalyzerPanel.add(authorPanel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        authorLabel = new JLabel();
        authorLabel.setText("Author: 4ra1n (github.com/4ra1n) from Chaitin Tech");
        authorPanel.add(authorLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        configPanel = new JPanel();
        configPanel.setLayout(new GridLayoutManager(4, 4, new Insets(0, 0, 0, 0), -1, -1));
        configPanel.setBackground(new Color(-725535));
        jarAnalyzerPanel.add(configPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        methodLabel = new JLabel();
        methodLabel.setText("   Input Target Method:");
        configPanel.add(methodLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        methodText = new JTextField();
        configPanel.add(methodText, new GridConstraints(3, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        classLabel = new JLabel();
        classLabel.setText("   Input Target Class:");
        configPanel.add(classLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        classText = new JTextField();
        configPanel.add(classText, new GridConstraints(2, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        jarInfoLabel = new JLabel();
        jarInfoLabel.setText("   Jar Information:");
        configPanel.add(jarInfoLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jarInfoResultText = new JTextField();
        jarInfoResultText.setEditable(false);
        jarInfoResultText.setEnabled(true);
        configPanel.add(jarInfoResultText, new GridConstraints(1, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        progressLabel = new JLabel();
        progressLabel.setText("   Load Jar Progress:");
        configPanel.add(progressLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        progress = new JProgressBar();
        progress.setBackground(new Color(-725535));
        progress.setForeground(new Color(-9524737));
        progress.setString("");
        progress.setStringPainted(true);
        progress.setToolTipText("");
        progress.setValue(0);
        configPanel.add(progress, new GridConstraints(0, 1, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        resultPane = new JPanel();
        resultPane.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        resultPane.setBackground(new Color(-725535));
        jarAnalyzerPanel.add(resultPane, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        resultScroll = new JScrollPane();
        resultScroll.setBackground(new Color(-725535));
        resultPane.add(resultScroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(600, 100), null, null, 0, false));
        resultScroll.setBorder(BorderFactory.createTitledBorder(null, "Search Result", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        resultList = new JList();
        resultScroll.setViewportView(resultList);
        chanScroll = new JScrollPane();
        chanScroll.setBackground(new Color(-725535));
        resultPane.add(chanScroll, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(600, 100), null, null, 0, false));
        chanScroll.setBorder(BorderFactory.createTitledBorder(null, "Your Chain", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        chanList = new JList();
        final DefaultListModel defaultListModel1 = new DefaultListModel();
        chanList.setModel(defaultListModel1);
        chanScroll.setViewportView(chanList);
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(procyonRadioButton);
        buttonGroup.add(cfrRadioButton);
        buttonGroup.add(quiltFlowerRadioButton);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return jarAnalyzerPanel;
    }

}
