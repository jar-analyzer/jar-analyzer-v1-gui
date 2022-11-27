package com.chaitin.jar.analyzer.form;

import com.chaitin.jar.analyzer.core.*;
import com.chaitin.jar.analyzer.util.CoreUtil;
import com.chaitin.jar.analyzer.util.DirUtil;
import com.formdev.flatlaf.FlatLightLaf;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.PlainTextOutput;
import jsyntaxpane.syntaxkits.JavaSyntaxKit;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.*;

public class JarAnalyzerForm {
    private JButton startAnalysisButton;
    private JButton allCleanButton;
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
    private JLabel jarInfoResultText;
    private JScrollPane sourceScroll;
    private JList<ResObj> sourceList;
    private JList<ResObj> callList;
    private JScrollPane callScroll;
    private JList<ResObj> chanList;
    private JPanel configPanel;
    private JScrollPane chanScroll;
    private JLabel currentLabel;

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
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int option = fileChooser.showOpenDialog(new JFrame());
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String absPath = file.getAbsolutePath();
                totalJars++;
                new Thread(() -> {
                    classFileList.addAll(CoreUtil.getAllClassesFromJar(absPath));
                    System.out.println(classFileList.size());
                    Discovery.start(classFileList, discoveredClasses,
                            discoveredMethods, classMap, methodMap);
                    jarInfoResultText.setText(String.format(
                            "total jars: %d   total classes: %s   total methods: %s",
                            totalJars, discoveredClasses.size(), discoveredMethods.size()
                    ));
                    MethodCall.start(classFileList, methodCalls);
                    inheritanceMap = InheritanceUtil.derive(classMap);
                    Map<MethodReference.Handle, Set<MethodReference.Handle>> implMap =
                            InheritanceUtil.getAllMethodImplementations(inheritanceMap, methodMap);
                    for (Map.Entry<MethodReference.Handle, Set<MethodReference.Handle>> entry :
                            implMap.entrySet()) {
                        MethodReference.Handle k = entry.getKey();
                        Set<MethodReference.Handle> v = entry.getValue();
                        HashSet<MethodReference.Handle> calls = methodCalls.get(k);
                        calls.addAll(v);
                    }
                }).start();
                JOptionPane.showMessageDialog(null, "Analyzing...");
            }
        });

        startAnalysisButton.addActionListener(e -> {
            DefaultListModel<ResObj> searchList = new DefaultListModel<>();

            String className = classText.getText().replace(".", "/").trim();
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
                    if (c.equals(className) || s.equals(shortClassName)) {
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
        new Thread(() -> {
            Decompiler.decompile(
                    finalClassPath,
                    new PlainTextOutput(writer));
            try {
                writer.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String total = bao.toString();
            editorPane.setText(total);

            String methodName = res.getMethod().getName();
            if (methodName.equals("<init>")) {
                String[] c = res.getClassName().split("/");
                methodName = c[c.length - 1];
            }

            for (int i = total.indexOf(methodName);
                 i >= 0; i = total.indexOf(methodName, i + 1)) {
                if (total.charAt(i - 1) == '.') {
                    if (total.charAt(i + methodName.length()) == '(') {
                        editorPane.setCaretPosition(i);
                    }
                }
            }

        }).start();

        JOptionPane.showMessageDialog(null, "Decompiling...");

        DefaultListModel<ResObj> sourceDataList = new DefaultListModel<>();
        DefaultListModel<ResObj> callDataList = new DefaultListModel<>();

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

        currentLabel.setText(String.format("  Current: %s", res));
        currentLabel.setToolTipText(res.getMethod().getDescStd());

        sourceList.setModel(sourceDataList);
        callList.setModel(callDataList);
    }

    public JarAnalyzerForm() {
        DirUtil.removeDir(new File("temp"));
        editorPane.setEditorKit(new JavaSyntaxKit());
        loadFont();
        loadJar();

        allCleanButton.addActionListener(e -> {
            classFileList.clear();
            discoveredClasses.clear();
            discoveredMethods.clear();
            classMap.clear();
            methodMap.clear();
            methodCalls.clear();
            inheritanceMap = null;
            totalJars = 0;

            classText.setText(null);
            methodText.setText(null);
            editorPane.setText(null);
            jarInfoResultText.setText(null);
            currentLabel.setText(null);

            DefaultListModel<?> call = (DefaultListModel<?>) callList.getModel();
            call.removeAllElements();
            DefaultListModel<?> chan = (DefaultListModel<?>) chanList.getModel();
            chan.removeAllElements();
            DefaultListModel<?> result = (DefaultListModel<?>) resultList.getModel();
            result.removeAllElements();
            DefaultListModel<?> source = (DefaultListModel<?>) sourceList.getModel();
            source.removeAllElements();
            chainDataList.removeAllElements();
        });

        ToolTipManager.sharedInstance().setDismissDelay(10000);
        ToolTipManager.sharedInstance().setInitialDelay(300);

        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent evt) {
                JList<?> list = (JList<?>) evt.getSource();
                if (SwingUtilities.isRightMouseButton(evt)) {
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
        });

        chanList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent evt) {
                JList<?> list = (JList<?>) evt.getSource();
                if (SwingUtilities.isRightMouseButton(evt)) {
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

        callList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent evt) {
                JList<?> list = (JList<?>) evt.getSource();
                if (SwingUtilities.isRightMouseButton(evt)) {
                    int index = list.locationToIndex(evt.getPoint());
                    ResObj res = (ResObj) list.getModel().getElementAt(index);
                    chainDataList.addElement(res);
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

        sourceList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent evt) {
                JList<?> list = (JList<?>) evt.getSource();
                if (SwingUtilities.isRightMouseButton(evt)) {
                    int index = list.locationToIndex(evt.getPoint());
                    ResObj res = (ResObj) list.getModel().getElementAt(index);
                    chainDataList.addElement(res);
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

    private void loadFont() {
        try {
            Font customFont = Font.createFont(Font.TRUETYPE_FONT,
                    Objects.requireNonNull(JarAnalyzerForm.class.getClassLoader()
                            .getResourceAsStream("Consolas.ttf")));
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(customFont);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
        startAnalysisButton = new JButton();
        startAnalysisButton.setText("Start Analysis");
        topPanel.add(startAnalysisButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        allCleanButton = new JButton();
        allCleanButton.setText("All Clean");
        topPanel.add(allCleanButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        selectJarFileButton = new JButton();
        selectJarFileButton.setText("Select Jar File");
        topPanel.add(selectJarFileButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        editorPanel = new JPanel();
        editorPanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        editorPanel.setBackground(new Color(-725535));
        jarAnalyzerPanel.add(editorPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        editorScroll = new JScrollPane();
        editorScroll.setBackground(new Color(-725535));
        editorPanel.add(editorScroll, new GridConstraints(0, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(600, 500), new Dimension(600, 500), new Dimension(600, 500), 0, false));
        editorScroll.setBorder(BorderFactory.createTitledBorder(null, "Decompile Java Code", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        editorPane = new JEditorPane();
        editorScroll.setViewportView(editorPane);
        sourceScroll = new JScrollPane();
        sourceScroll.setBackground(new Color(-725535));
        editorPanel.add(sourceScroll, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(600, -1), null, null, 0, false));
        sourceScroll.setBorder(BorderFactory.createTitledBorder(null, "Who Call Target", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        sourceList = new JList();
        sourceScroll.setViewportView(sourceList);
        callScroll = new JScrollPane();
        callScroll.setBackground(new Color(-725535));
        editorPanel.add(callScroll, new GridConstraints(1, 1, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(600, -1), null, null, 0, false));
        callScroll.setBorder(BorderFactory.createTitledBorder(null, "Target Call Whom", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        callList = new JList();
        callScroll.setViewportView(callList);
        currentLabel = new JLabel();
        currentLabel.setText("");
        editorPanel.add(currentLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        authorPanel = new JPanel();
        authorPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        authorPanel.setBackground(new Color(-725535));
        jarAnalyzerPanel.add(authorPanel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        authorLabel = new JLabel();
        authorLabel.setText("Author: 4ra1n (github.com/4ra1n) from Chaitin Tech");
        authorPanel.add(authorLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        configPanel = new JPanel();
        configPanel.setLayout(new GridLayoutManager(3, 4, new Insets(0, 0, 0, 0), -1, -1));
        configPanel.setBackground(new Color(-725535));
        jarAnalyzerPanel.add(configPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        methodLabel = new JLabel();
        methodLabel.setText("   Input Target Method:");
        configPanel.add(methodLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        methodText = new JTextField();
        configPanel.add(methodText, new GridConstraints(2, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        classLabel = new JLabel();
        classLabel.setText("   Input Target Class:");
        configPanel.add(classLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        classText = new JTextField();
        configPanel.add(classText, new GridConstraints(1, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        jarInfoLabel = new JLabel();
        jarInfoLabel.setText("   Jar Information:");
        configPanel.add(jarInfoLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jarInfoResultText = new JLabel();
        jarInfoResultText.setText("");
        configPanel.add(jarInfoResultText, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        resultPane = new JPanel();
        resultPane.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        resultPane.setBackground(new Color(-725535));
        jarAnalyzerPanel.add(resultPane, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        resultScroll = new JScrollPane();
        resultScroll.setBackground(new Color(-725535));
        resultPane.add(resultScroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(600, 200), null, null, 0, false));
        resultScroll.setBorder(BorderFactory.createTitledBorder(null, "Search Result", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        resultList = new JList();
        resultScroll.setViewportView(resultList);
        chanScroll = new JScrollPane();
        chanScroll.setBackground(new Color(-725535));
        resultPane.add(chanScroll, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(600, 200), null, null, 0, false));
        chanScroll.setBorder(BorderFactory.createTitledBorder(null, "Your Chain", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        chanList = new JList();
        final DefaultListModel defaultListModel1 = new DefaultListModel();
        chanList.setModel(defaultListModel1);
        chanScroll.setViewportView(chanList);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return jarAnalyzerPanel;
    }
}
