package com.chaitin.jar.analyzer.form;

import com.chaitin.jar.analyzer.adapter.*;
import com.chaitin.jar.analyzer.core.*;
import com.chaitin.jar.analyzer.model.ClassObj;
import com.chaitin.jar.analyzer.model.MappingObj;
import com.chaitin.jar.analyzer.model.ResObj;
import com.chaitin.jar.analyzer.spring.SpringController;
import com.chaitin.jar.analyzer.spring.SpringService;
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
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class JarAnalyzerForm {
    public static final String tips = "IMPORTANT: MISSING JARS \n" +
            "1. maybe: missing rt.jar or other dependencies\n" +
            "2. maybe: do not select Use SpringBoot Jar\n";
    public static boolean deleteLogs = false;
    public static boolean innerJars = false;
    public static boolean springBootJar = false;
    public JButton startAnalysisButton;
    public JPanel jarAnalyzerPanel;
    public JPanel topPanel;
    public JButton selectJarFileButton;
    public JPanel editorPanel;
    public JScrollPane editorScroll;
    public JLabel authorLabel;
    public JPanel authorPanel;
    public JEditorPane editorPane;
    public JList<ResObj> resultList;
    public JPanel resultPane;
    public JScrollPane resultScroll;
    public JTextField classText;
    public JTextField methodText;
    public JLabel methodLabel;
    public JLabel classLabel;
    public JLabel jarInfoLabel;
    public JTextField jarInfoResultText;
    public JScrollPane sourceScroll;
    public JList<ResObj> sourceList;
    public JList<ResObj> callList;
    public JScrollPane callScroll;
    public JList<ResObj> chanList;
    public JPanel configPanel;
    public JScrollPane chanScroll;
    public JTextField currentLabel;
    public static ResObj curRes;
    public JRadioButton procyonRadioButton;
    public JRadioButton cfrRadioButton;
    public JRadioButton quiltFlowerRadioButton;
    public JPanel opPanel;
    public JPanel dcPanel;
    public JPanel curPanel;
    public JLabel curLabel;
    public JLabel progressLabel;
    public JProgressBar progress;
    public JTabbedPane callPanel;
    public JScrollPane subScroll;
    public JScrollPane superScroll;
    public JList<ClassObj> subList;
    public JList<ClassObj> superList;
    public JList<ResObj> historyList;
    public JScrollPane historyScroll;
    public JButton showASMCodeButton;
    public JButton showByteCodeButton;
    public JRadioButton callSearchRadioButton;
    public JRadioButton directSearchRadioButton;
    public JPanel searchSelPanel;
    public JLabel callSearchLabel;
    public JLabel directSearchLabel;
    public JPanel actionPanel;
    public JButton analyzeSpringButton;
    public JPanel springPanel;
    public JScrollPane controllerPanel;
    public JScrollPane mappingsPanel;
    public JList<ClassObj> controllerJList;
    public JList<MappingObj> mappingJList;
    public JCheckBox useSpringBootJarCheckBox;
    public JCheckBox innerJarsCheckBox;
    private JCheckBox deleteLogsWhenExitCheckBox;
    public static List<SpringController> controllers = new ArrayList<>();

    public static final DefaultListModel<ResObj> historyDataList = new DefaultListModel<>();

    public static Set<ClassFile> classFileList = new HashSet<>();
    public static final Set<ClassReference> discoveredClasses = new HashSet<>();
    public static final Set<MethodReference> discoveredMethods = new HashSet<>();
    public static final Map<ClassReference.Handle, ClassReference> classMap = new HashMap<>();
    public static final Map<MethodReference.Handle, MethodReference> methodMap = new HashMap<>();
    public static final HashMap<MethodReference.Handle,
            HashSet<MethodReference.Handle>> methodCalls = new HashMap<>();
    public static InheritanceMap inheritanceMap;

    public static int totalJars = 0;

    public void loadJar() {
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

            if (callSearchRadioButton.isSelected()) {
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

                if (searchList.size() == 0 || searchList.isEmpty()) {
                    JOptionPane.showMessageDialog(null,
                            "No results!\n" +
                                    "1. chose Direct Search / Call Search\n" +
                                    "2. maybe select Use SpringBoot Jar");
                }

                resultList.setModel(searchList);
            }

            if (directSearchRadioButton.isSelected()) {
                for (Map.Entry<MethodReference.Handle, MethodReference> entry : methodMap.entrySet()) {
                    MethodReference.Handle h = entry.getKey();
                    String c = h.getClassReference().getName();
                    String[] st = c.split("/");
                    String s = st[st.length - 1];
                    if (className.equals("ALL")) {
                        if (h.getName().equals(methodName)) {
                            searchList.addElement(new ResObj(h, h.getClassReference().getName()));
                        }
                    } else if (h.getClassReference().getName().equals(className) ||
                            s.equals(shortClassName)) {
                        if (h.getName().equals(methodName)) {
                            searchList.addElement(new ResObj(h, h.getClassReference().getName()));
                        }
                    }
                }
                resultList.setModel(searchList);
            }
        });
    }

    public static final DefaultListModel<ResObj> chainDataList = new DefaultListModel<>();

    @SuppressWarnings("all")
    public void coreClass(MouseEvent evt, JList<?> list) {
        int index = list.locationToIndex(evt.getPoint());
        ClassObj res = (ClassObj) list.getModel().getElementAt(index);
        String className = res.getClassName();
        String classPath = className.replace("/", File.separator);
        if (springBootJar) {
            if (classPath.contains("springframework")) {
                classPath = String.format("temp%s%s.class", File.separator, classPath);
            } else {
                classPath = String.format("temp%sBOOT-INF%sclasses%s%s.class",
                        File.separator, File.separator, File.separator, classPath);
            }
        } else {
            classPath = String.format("temp%s%s.class", File.separator, classPath);
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
                if (total == null || total.trim().equals("")) {
                    total = tips;
                } else {
                    total = "// Procyon \n" + total;
                }
            } else if (quiltFlowerRadioButton.isSelected()) {
                String[] args = new String[]{
                        finalClassPath,
                        javaDir
                };
                ConsoleDecompiler.main(args);
                try {
                    total = new String(Files.readAllBytes(javaPathPath));
                    if (total.trim().equals("")) {
                        total = tips;
                    } else {
                        total = "// QuiltFlower \n" + total;
                        Files.delete(javaPathPath);
                    }
                } catch (Exception ignored) {
                    total = tips;
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
                if (total.trim().equals("")) {
                    total = tips;
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

    @SuppressWarnings("all")
    public void core(MouseEvent evt, JList<?> list) {
        int index = list.locationToIndex(evt.getPoint());
        ResObj res = (ResObj) list.getModel().getElementAt(index);

        String className = res.getClassName();
        String classPath = className.replace("/", File.separator);
        if (!springBootJar) {
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
                if (total == null || total.trim().equals("")) {
                    total = tips;
                } else {
                    total = "// Procyon \n" + total;
                }
            } else if (quiltFlowerRadioButton.isSelected()) {
                String[] args = new String[]{
                        finalClassPath,
                        javaDir
                };
                ConsoleDecompiler.main(args);
                try {
                    total = new String(Files.readAllBytes(javaPathPath));
                    if (total.trim().equals("")) {
                        total = tips;
                    } else {
                        total = "// QuiltFlower \n" + total;
                        Files.delete(javaPathPath);
                    }
                } catch (Exception ignored) {
                    total = tips;
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
                if (total.trim().equals("")) {
                    total = tips;
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

        curRes = res;
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
        callSearchRadioButton.setSelected(true);

        editorPane.setEditorKit(new JavaSyntaxKit());
        loadJar();

        ToolTipManager.sharedInstance().setDismissDelay(10000);
        ToolTipManager.sharedInstance().setInitialDelay(300);

        resultList.addMouseListener(new ListMouseAdapter(this));
        callList.addMouseListener(new ListMouseAdapter(this));
        sourceList.addMouseListener(new ListMouseAdapter(this));
        subList.addMouseListener(new ListClassMouseAdapter(this));
        superList.addMouseListener(new ListClassMouseAdapter(this));
        historyList.addMouseListener(new ListMouseAdapter(this));
        mappingJList.addMouseListener(new MappingMouseAdapter(this));
        controllerJList.addMouseListener(new ControllerMouseAdapter(this));

        innerJarsCheckBox.addActionListener(e -> {
            innerJars = innerJarsCheckBox.isSelected();
            if (!innerJars) {
                return;
            }
            JOptionPane.showMessageDialog(null,
                    "What is Inner Jar:\n" +
                            "There may be many dependent jars in the jar.\n" +
                            "If you chose to analyze these jars will be time-consuming");
        });
        useSpringBootJarCheckBox.addActionListener(e -> {
            springBootJar = useSpringBootJarCheckBox.isSelected();
            if (!springBootJar) {
                return;
            }
            JOptionPane.showMessageDialog(null,
                    "What is Use SpringBoot Jar:\n" +
                            "The class file written by the user in SpringBoot is located in the BOOT-INF directory" +
                            " and needs special processing. " +
                            "If you need to analyze the Jar of SpringBoot, you need to check this item.");
        });
        deleteLogsWhenExitCheckBox.setSelected(true);
        deleteLogs = true;
        deleteLogsWhenExitCheckBox.addActionListener(e ->
                deleteLogs = deleteLogsWhenExitCheckBox.isSelected());

        analyzeSpringButton.addActionListener(e -> {
            springBootJar = true;
            useSpringBootJarCheckBox.setSelected(true);
            controllers.clear();
            SpringService.start(classFileList, controllers, classMap, methodMap);

            DefaultListModel<ClassObj> controllerDataList = new DefaultListModel<>();
            for (SpringController controller : controllers) {
                for (ClassReference.Handle c : classMap.keySet()) {
                    if (c.equals(controller.getClassName())) {
                        controllerDataList.addElement(
                                new ClassObj(c.getName(), c));
                    }
                }
            }
            controllerJList.setModel(controllerDataList);
        });

        chanList.addMouseListener(new ChanMouseAdapter(this));

        showByteCodeButton.addActionListener(e -> {
            if (curRes == null) {
                JOptionPane.showMessageDialog(null, "Error!");
                return;
            }
            JFrame frame = new JFrame("Show Bytecode");
            String className = curRes.getClassName();
            className = className.replace(File.separator, ".");
            frame.setContentPane(new BytecodeForm(className, false).parentPanel);
            frame.pack();
            frame.setVisible(true);
        });

        showASMCodeButton.addActionListener(e -> {
            if (curRes == null) {
                JOptionPane.showMessageDialog(null, "Error!");
                return;
            }
            JFrame frame = new JFrame("Show ASM Code");
            String className = curRes.getClassName();
            className = className.replace(File.separator, ".");
            frame.setContentPane(new BytecodeForm(className, true).parentPanel);
            frame.pack();
            frame.setVisible(true);
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
        opPanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        opPanel.setBackground(new Color(-725535));
        topPanel.add(opPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        searchSelPanel = new JPanel();
        searchSelPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        searchSelPanel.setBackground(new Color(-528927));
        opPanel.add(searchSelPanel, new GridConstraints(0, 1, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        searchSelPanel.setBorder(BorderFactory.createTitledBorder(null, "Search Options", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        callSearchRadioButton = new JRadioButton();
        callSearchRadioButton.setBackground(new Color(-528927));
        callSearchRadioButton.setText("Call Search");
        searchSelPanel.add(callSearchRadioButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        callSearchLabel = new JLabel();
        callSearchLabel.setText("   Only Search Method Call ");
        searchSelPanel.add(callSearchLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        directSearchLabel = new JLabel();
        directSearchLabel.setText("   Directly Search Class And Method");
        searchSelPanel.add(directSearchLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        directSearchRadioButton = new JRadioButton();
        directSearchRadioButton.setBackground(new Color(-528927));
        directSearchRadioButton.setText("Direct Search");
        searchSelPanel.add(directSearchRadioButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        actionPanel = new JPanel();
        actionPanel.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        actionPanel.setBackground(new Color(-528927));
        opPanel.add(actionPanel, new GridConstraints(0, 0, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        actionPanel.setBorder(BorderFactory.createTitledBorder(null, "Action", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        selectJarFileButton = new JButton();
        selectJarFileButton.setText("Select Jar File / Dir");
        actionPanel.add(selectJarFileButton, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        analyzeSpringButton = new JButton();
        analyzeSpringButton.setText("Analyze Spring");
        actionPanel.add(analyzeSpringButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        startAnalysisButton = new JButton();
        startAnalysisButton.setText("Start Search");
        actionPanel.add(startAnalysisButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useSpringBootJarCheckBox = new JCheckBox();
        useSpringBootJarCheckBox.setBackground(new Color(-528927));
        useSpringBootJarCheckBox.setText("Use SpringBoot Jar");
        actionPanel.add(useSpringBootJarCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        innerJarsCheckBox = new JCheckBox();
        innerJarsCheckBox.setBackground(new Color(-528927));
        innerJarsCheckBox.setText("Inner Jars");
        actionPanel.add(innerJarsCheckBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dcPanel = new JPanel();
        dcPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        dcPanel.setBackground(new Color(-725535));
        topPanel.add(dcPanel, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        dcPanel.setBorder(BorderFactory.createTitledBorder(null, "Decompile Options", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
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
        curPanel.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        curPanel.setBackground(new Color(-725535));
        editorPanel.add(curPanel, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        curLabel = new JLabel();
        curLabel.setText("   Current:");
        curPanel.add(curLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        currentLabel = new JTextField();
        currentLabel.setEditable(false);
        curPanel.add(currentLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        showASMCodeButton = new JButton();
        showASMCodeButton.setText("Show ASM Code");
        curPanel.add(showASMCodeButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showByteCodeButton = new JButton();
        showByteCodeButton.setText("Show Bytecode");
        curPanel.add(showByteCodeButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deleteLogsWhenExitCheckBox = new JCheckBox();
        deleteLogsWhenExitCheckBox.setBackground(new Color(-725535));
        deleteLogsWhenExitCheckBox.setText("Delete Logs When Exit");
        curPanel.add(deleteLogsWhenExitCheckBox, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        callPanel = new JTabbedPane();
        callPanel.setBackground(new Color(-528927));
        callPanel.setForeground(new Color(-16777216));
        editorPanel.add(callPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(700, -1), null, null, 0, false));
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
        subScroll = new JScrollPane();
        subScroll.setBackground(new Color(-528927));
        callPanel.addTab("All Subclasses", subScroll);
        subList = new JList();
        subScroll.setViewportView(subList);
        superScroll = new JScrollPane();
        superScroll.setBackground(new Color(-528927));
        callPanel.addTab("All Superclasses", superScroll);
        superList = new JList();
        superScroll.setViewportView(superList);
        historyScroll = new JScrollPane();
        callPanel.addTab("Decompile History", historyScroll);
        historyList = new JList();
        historyScroll.setViewportView(historyList);
        springPanel = new JPanel();
        springPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        springPanel.setBackground(new Color(-528927));
        editorPanel.add(springPanel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        controllerPanel = new JScrollPane();
        controllerPanel.setBackground(new Color(-528927));
        springPanel.add(controllerPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        controllerPanel.setBorder(BorderFactory.createTitledBorder(null, "Spring Controllers", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        controllerJList = new JList();
        final DefaultListModel defaultListModel1 = new DefaultListModel();
        controllerJList.setModel(defaultListModel1);
        controllerPanel.setViewportView(controllerJList);
        mappingsPanel = new JScrollPane();
        mappingsPanel.setBackground(new Color(-528927));
        springPanel.add(mappingsPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        mappingsPanel.setBorder(BorderFactory.createTitledBorder(null, "Spring Mappings", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        mappingJList = new JList();
        mappingsPanel.setViewportView(mappingJList);
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
        final DefaultListModel defaultListModel2 = new DefaultListModel();
        chanList.setModel(defaultListModel2);
        chanScroll.setViewportView(chanList);
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(procyonRadioButton);
        buttonGroup.add(cfrRadioButton);
        buttonGroup.add(quiltFlowerRadioButton);
        buttonGroup = new ButtonGroup();
        buttonGroup.add(directSearchRadioButton);
        buttonGroup.add(callSearchRadioButton);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return jarAnalyzerPanel;
    }

}
