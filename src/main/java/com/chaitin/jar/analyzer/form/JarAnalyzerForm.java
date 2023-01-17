package com.chaitin.jar.analyzer.form;

import com.chaitin.jar.analyzer.adapter.*;
import com.chaitin.jar.analyzer.asm.GreatClassVisitor;
import com.chaitin.jar.analyzer.asm.StringClassVisitor;
import com.chaitin.jar.analyzer.core.*;
import com.chaitin.jar.analyzer.model.ClassObj;
import com.chaitin.jar.analyzer.model.MappingObj;
import com.chaitin.jar.analyzer.model.ResObj;
import com.chaitin.jar.analyzer.spring.SpringController;
import com.chaitin.jar.analyzer.spring.SpringService;
import com.chaitin.jar.analyzer.tree.FileTree;
import com.chaitin.jar.analyzer.util.CoreUtil;
import com.chaitin.jar.analyzer.util.DirUtil;
import com.chaitin.jar.analyzer.util.OSUtil;
import com.formdev.flatlaf.FlatDarkLaf;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.PlainTextOutput;
import jsyntaxpane.syntaxkits.JavaSyntaxKit;
import okhttp3.*;
import org.benf.cfr.reader.Main;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.List;


public class JarAnalyzerForm {
    private static JarAnalyzerForm instance;
    public static final String tips = "重要提示：丢失依赖 \n" +
            "1. 也许你忘记加载rt.jar或者其他依赖jar包了\n" +
            "2. 也许你不应该选择分析SpringBoot选项\n";
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
    private JLabel searchClassLabel;
    private JLabel searchMethodLabel;
    private JRadioButton strRadioButton;
    private JRadioButton greatRadioButton;
    private JTextField otherText;
    private JLabel otherSearch;
    private JLabel otherTip;
    private JPanel treePanel;
    private FileTree trees;
    private JScrollPane treeScroll;
    private JRadioButton binaryRadioButton;
    private JRadioButton strRegexRadioButton;
    private JList<ResObj> allMethodList;
    private JPanel allMethodPanel;
    private JScrollPane allMethodScroll;
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
    private static final List<String> jarPathList = new ArrayList<>();
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

                    refreshTree();

                    progress.setValue(50);
                    Discovery.start(classFileList, discoveredClasses,
                            discoveredMethods, classMap, methodMap);
                    jarInfoResultText.setText(String.format(
                            "Jar包数量: %d   类的数量: %s   方法的数量: %s",
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
                    JOptionPane.showMessageDialog(this.jarAnalyzerPanel,
                            "没有结果!\n" +
                                    "1. 也许你选错了：直接搜索/调用搜索\n" +
                                    "2. 也许你应该勾选分析SpringBoot");
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

            if (strRadioButton.isSelected()) {
                List<MethodReference> mList = new ArrayList<>();
                String search = otherText.getText();
                if (search == null || search.trim().equals("")) {
                    JOptionPane.showMessageDialog(this.jarAnalyzerPanel, "请输入其他搜索内容");
                    return;
                }
                for (ClassFile file : classFileList) {
                    try {
                        StringClassVisitor dcv = new StringClassVisitor(true,
                                search, mList, classMap, methodMap);
                        ClassReader cr = new ClassReader(file.getFile());
                        cr.accept(dcv, ClassReader.EXPAND_FRAMES);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                if (mList.size() == 0) {
                    JOptionPane.showMessageDialog(this.jarAnalyzerPanel, "没有搜到结果");
                    resultList.setModel(null);
                    return;
                }

                Set<MethodReference> mSet = new HashSet<>(mList);

                for (MethodReference mr : mSet) {
                    searchList.addElement(new ResObj(mr.getHandle(), mr.getClassReference().getName()));
                }
                resultList.setModel(searchList);
            }

            if (strRegexRadioButton.isSelected()) {
                List<MethodReference> mList = new ArrayList<>();
                String search = otherText.getText();
                if (search == null || search.trim().equals("")) {
                    JOptionPane.showMessageDialog(this.jarAnalyzerPanel, "请输入其他搜索内容");
                    return;
                }
                for (ClassFile file : classFileList) {
                    try {
                        StringClassVisitor dcv = new StringClassVisitor(false,
                                search, mList, classMap, methodMap);
                        ClassReader cr = new ClassReader(file.getFile());
                        cr.accept(dcv, ClassReader.EXPAND_FRAMES);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                if (mList.size() == 0) {
                    JOptionPane.showMessageDialog(this.jarAnalyzerPanel, "没有搜到结果");
                    resultList.setModel(null);
                    return;
                }

                Set<MethodReference> mSet = new HashSet<>(mList);

                for (MethodReference mr : mSet) {
                    searchList.addElement(new ResObj(mr.getHandle(), mr.getClassReference().getName()));
                }
                resultList.setModel(searchList);
            }

            if (binaryRadioButton.isSelected()) {

                String search = otherText.getText();
                if (search == null || search.trim().equals("")) {
                    JOptionPane.showMessageDialog(this.jarAnalyzerPanel, "请输入其他搜索内容");
                    return;
                }

                for (String jarPath : jarPathList) {
                    try {
                        Path path = Paths.get(jarPath);
                        if (Files.size(path) > 1024 * 1024 * 50) {
                            FileInputStream fis = new FileInputStream(path.toFile());
                            byte[] searchContext = search.getBytes();
                            byte[] data = new byte[16384];
                            while (fis.read(data, 0, data.length) != -1) {
                                for (int i = 0; i < data.length - searchContext.length + 1; ++i) {
                                    boolean found = true;
                                    for (int j = 0; j < searchContext.length; ++j) {
                                        if (data[i + j] != searchContext[j]) {
                                            found = false;
                                            break;
                                        }
                                    }
                                    if (found) {
                                        fis.close();
                                        JOptionPane.showMessageDialog(this.jarAnalyzerPanel, "搜索到此字符串");
                                        return;
                                    }
                                }
                            }
                            fis.close();
                        } else {
                            byte[] searchContext = search.getBytes();
                            byte[] data = Files.readAllBytes(path);
                            for (int i = 0; i < data.length - searchContext.length + 1; ++i) {
                                boolean found = true;
                                for (int j = 0; j < searchContext.length; ++j) {
                                    if (data[i + j] != searchContext[j]) {
                                        found = false;
                                        break;
                                    }
                                }
                                if (found) {
                                    JOptionPane.showMessageDialog(this.jarAnalyzerPanel, "搜索到此字符串");
                                    return;
                                }
                            }
                        }
                        JOptionPane.showMessageDialog(this.jarAnalyzerPanel, "找不到");
                        return;
                    } catch (Exception ignored) {
                    }
                }
            }

            if (greatRadioButton.isSelected()) {
                List<MethodReference> mList = new ArrayList<>();
                String search = otherText.getText();
                if (search == null || search.trim().equals("")) {
                    JOptionPane.showMessageDialog(this.jarAnalyzerPanel, "请输入其他搜索内容");
                    return;
                }
                for (ClassFile file : classFileList) {
                    try {
                        GreatClassVisitor dcv = new GreatClassVisitor(search, mList, classMap, methodMap);
                        ClassReader cr = new ClassReader(file.getFile());
                        cr.accept(dcv, ClassReader.EXPAND_FRAMES);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                if (mList.size() == 0) {
                    JOptionPane.showMessageDialog(this.jarAnalyzerPanel, "没有搜到结果");
                    resultList.setModel(null);
                    return;
                }

                Set<MethodReference> mSet = new HashSet<>(mList);

                for (MethodReference mr : mSet) {
                    searchList.addElement(new ResObj(mr.getHandle(), mr.getClassReference().getName()));
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
    }

    private void coreClassInternal(ClassObj res) {
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
                try {
                    Files.delete(javaPathPath);
                } catch (IOException ignored) {
                }
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
                try {
                    Files.delete(javaPathPath);
                } catch (IOException ignored) {
                }
                Main.main(args);
                try {
                    if (JarAnalyzerForm.springBootJar) {
                        total = new String(Files.readAllBytes(
                                Paths.get(String.format("temp%s%s", File.separator,
                                        javaPathPath.toString().substring(22)))
                        ));
                    } else {
                        total = new String(Files.readAllBytes(javaPathPath));
                    }
                } catch (Exception ignored) {
                    total = "";
                }
                if (total.trim().equals("")) {
                    total = tips;
                }
                try {
                    Files.delete(javaPathPath);
                } catch (IOException ignored) {
                }
            } else {
                JOptionPane.showMessageDialog(this.jarAnalyzerPanel, "无法反编译");
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
        callList.setModel(new DefaultListModel<>());
        sourceList.setModel(new DefaultListModel<>());
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
                try {
                    Files.delete(javaPathPath);
                } catch (IOException ignored) {
                }
                ConsoleDecompiler.main(args);
                try {
                    total = new String(Files.readAllBytes(javaPathPath));
                    if (total.trim().equals("")) {
                        total = tips;
                    } else {
                        total = "// QuiltFlower \n" + total;
                    }
                } catch (Exception ignored) {
                    total = tips;
                }
                try {
                    Files.delete(javaPathPath);
                } catch (IOException ignored) {
                }
            } else if (cfrRadioButton.isSelected()) {
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
                                Paths.get(String.format("temp%s%s", File.separator,
                                        javaPathPath.toString().substring(22)))
                        ));
                    } else {
                        total = new String(Files.readAllBytes(javaPathPath));
                    }
                } catch (Exception ignored) {
                    total = "";
                }
                if (total.trim().equals("")) {
                    total = tips;
                }
                try {
                    Files.delete(javaPathPath);
                } catch (IOException ignored) {
                }
            } else {
                JOptionPane.showMessageDialog(this.jarAnalyzerPanel, "无法反编译");
                return;
            }
            editorPane.setText(total);

            // 目标方法名是构造
            String methodName = res.getMethod().getName();
            if (methodName.equals("<init>")) {
                String[] c = res.getClassName().split("/");
                // 方法名应该是类名
                methodName = c[c.length - 1];
            }

            // 以第一处方法名索引开始搜索
            for (int i = total.indexOf(methodName);
                // 循环找直到找不到为止
                 i >= 0; i = total.indexOf(methodName, i + 1)) {
                // 如果方法名上一位是空格且下一位是(字符
                // 认为找到的方法的定义
                if (total.charAt(i - 1) == ' ' &&
                        total.charAt(i + methodName.length()) == '(') {
                    // 得到方法参数数量
                    int paramNum = Type.getMethodType(
                            res.getMethod().getDesc()).getArgumentTypes().length;
                    int curNum = 1;
                    for (int j = i + methodName.length() + 1; ; j++) {
                        // 遇到结尾
                        if (total.charAt(j) == ')') {
                            // 参数为0个的情况
                            if (total.charAt(j - 1) == '(') {
                                curNum = 0;
                            }
                            // 参数匹配认为找到了
                            if (curNum == paramNum) {
                                editorPane.setCaretPosition(i);
                                break;
                            }
                            break;
                        } else if (total.charAt(j) == ',') {
                            // 已遍历参数数量+1
                            curNum++;
                        }
                    }
                }
            }
        }).start();

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

    private void refreshTree() {
        try {
            trees.refresh();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public JarAnalyzerForm() {
        DirUtil.removeDir(new File("temp"));

        refreshTree();
        MouseListener ml = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int selRow = trees.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = trees.getPathForLocation(e.getX(), e.getY());
                if (selRow != -1) {
                    if (e.getClickCount() == 2) {
                        if (selPath == null) {
                            JOptionPane.showMessageDialog(jarAnalyzerPanel, "选择错误");
                            return;
                        }

                        String sel = selPath.toString();
                        sel = sel.substring(1, sel.length() - 1);
                        String[] selArray = sel.split(",");
                        List<String> pathList = new ArrayList<>();
                        for (String s : selArray) {
                            s = s.trim();
                            pathList.add(s);
                        }

                        String[] path = pathList.toArray(new String[0]);
                        String filePath = String.join(File.separator, path);

                        if (!filePath.endsWith(".class")) {
                            return;
                        }

                        Path thePath = Paths.get(filePath);
                        if (!Files.exists(thePath)) {
                            JOptionPane.showMessageDialog(jarAnalyzerPanel, "文件不存在");
                            return;
                        }

                        StringBuilder classNameBuilder = new StringBuilder();
                        for (int i = 1; i < path.length; i++) {
                            classNameBuilder.append(path[i]).append("/");
                        }
                        String className = classNameBuilder.toString();
                        className = className.substring(0, className.length() - 7);

                        ClassObj obj = new ClassObj(className, new ClassReference.Handle(className));

                        coreClassInternal(obj);

                    }
                }
            }
        };
        trees.addMouseListener(ml);

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

        directSearchRadioButton.addActionListener(e -> JOptionPane.showMessageDialog(this.jarAnalyzerPanel,
                "什么是直接搜索:\n" +
                        "直接搜索某个类的某个方法在哪里定义"));

        callSearchRadioButton.addActionListener(e -> JOptionPane.showMessageDialog(this.jarAnalyzerPanel,
                "什么是搜索调用:\n" +
                        "搜索某个类的某个方法在哪些地方被调用"));

        innerJarsCheckBox.addActionListener(e -> {
            innerJars = innerJarsCheckBox.isSelected();
            if (!innerJars) {
                return;
            }
            JOptionPane.showMessageDialog(this.jarAnalyzerPanel,
                    "什么是处理内部依赖Jar:\n" +
                            "在一个Jar中也许存在很多的依赖Jar\n" +
                            "如果你选择把这些Jar也都加入分析任务中这将会比较耗时");
        });
        useSpringBootJarCheckBox.addActionListener(e -> {
            springBootJar = useSpringBootJarCheckBox.isSelected();
            if (!springBootJar) {
                return;
            }
            JOptionPane.showMessageDialog(this.jarAnalyzerPanel,
                    "什么是分析SpringBot:\n" +
                            "在SpringBoot项目中需要分析的类位于BOOT-INF目录（不同于普通Jar）\n" +
                            "如果你输入的是SpringBoot的Jar包那么你需要勾选此项");
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
                JOptionPane.showMessageDialog(this.jarAnalyzerPanel, "当前的方法为空");
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
                JOptionPane.showMessageDialog(this.jarAnalyzerPanel, "当前的方法为空");
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

    private static JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createAboutMenu());
        menuBar.add(createVersionMenu());
        return menuBar;
    }

    private static JMenu createAboutMenu() {
        try {
            JMenu aboutMenu = new JMenu("帮助");
            JMenuItem bugItem = new JMenuItem("报告bug");
            InputStream is = JarAnalyzerForm.class.getClassLoader().getResourceAsStream("issue.png");
            if (is == null) {
                return null;
            }
            ImageIcon imageIcon = new ImageIcon(ImageIO.read(is));
            bugItem.setIcon(imageIcon);
            aboutMenu.add(bugItem);
            bugItem.addActionListener(e -> {
                try {
                    Desktop desktop = Desktop.getDesktop();
                    URI oURL = new URI("https://github.com/4ra1n/jar-analyzer/issues/new");
                    desktop.browse(oURL);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            JMenuItem authorItem = new JMenuItem("项目地址");
            is = JarAnalyzerForm.class.getClassLoader().getResourceAsStream("address.png");
            if (is == null) {
                return null;
            }
            imageIcon = new ImageIcon(ImageIO.read(is));
            authorItem.setIcon(imageIcon);
            aboutMenu.add(authorItem);
            authorItem.addActionListener(e -> {
                try {
                    Desktop desktop = Desktop.getDesktop();
                    URI oURL = new URI("https://github.com/4ra1n/jar-analyzer");
                    desktop.browse(oURL);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            JMenuItem normalItem = new JMenuItem("常见问题");
            is = JarAnalyzerForm.class.getClassLoader().getResourceAsStream("normal.png");
            if (is == null) {
                return null;
            }
            imageIcon = new ImageIcon(ImageIO.read(is));
            normalItem.setIcon(imageIcon);
            aboutMenu.add(normalItem);
            normalItem.addActionListener(e -> {
                try {
                    Desktop desktop = Desktop.getDesktop();
                    URI oURL = new URI("https://github.com/4ra1n/jar-analyzer/issues/43");
                    desktop.browse(oURL);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            return aboutMenu;
        } catch (Exception ex) {
            return null;
        }
    }

    private static JMenu createVersionMenu() {
        try {
            JMenu verMenu = new JMenu("版本");
            JMenuItem jarItem = new JMenuItem(Const.JarAnalyzerVersion);
            InputStream is = JarAnalyzerForm.class.getClassLoader().getResourceAsStream("ver.png");
            if (is == null) {
                return null;
            }
            ImageIcon imageIcon = new ImageIcon(ImageIO.read(is));
            jarItem.setIcon(imageIcon);

            JMenuItem downItem = new JMenuItem("验证最新版");
            downItem.setIcon(imageIcon);
            downItem.addActionListener(e -> {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("https://api.github.com/repos/4ra1n/jar-analyzer/releases/latest")
                        .addHeader("Connection", "close")
                        .build();

                JOptionPane.showMessageDialog(instance.jarAnalyzerPanel, Const.GithubTip);
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    @SuppressWarnings("all")
                    public void onFailure(Call call, IOException e) {
                        JOptionPane.showMessageDialog(instance.jarAnalyzerPanel, e.toString());
                    }

                    @Override
                    @SuppressWarnings("all")
                    public void onResponse(Call call, Response response) {
                        try {
                            if (response.body() == null) {
                                JOptionPane.showMessageDialog(instance.jarAnalyzerPanel, "网络错误");
                            }
                            String body = response.body().string();
                            String ver = body.split("\"tag_name\":")[1].split(",")[0];
                            ver = ver.substring(1, ver.length() - 1);

                            String output;
                            output = String.format("%s: %s\n%s: %s",
                                    "您当前的版本", Const.CurVersion,
                                    "目前最新版本", ver);
                            JOptionPane.showMessageDialog(instance.jarAnalyzerPanel, output);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(instance.jarAnalyzerPanel, ex.toString());
                        }
                    }
                });
            });

            verMenu.add(jarItem);
            verMenu.add(downItem);
            return verMenu;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void start() {
        FlatDarkLaf.setup();
        JFrame frame = new JFrame("Jar Analyzer");
        instance = new JarAnalyzerForm();
        frame.setContentPane(instance.jarAnalyzerPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setJMenuBar(createMenuBar());
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
        jarAnalyzerPanel.setLayout(new GridLayoutManager(5, 2, new Insets(0, 0, 0, 0), -1, -1));
        jarAnalyzerPanel.setBackground(new Color(-12828863));
        topPanel = new JPanel();
        topPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        topPanel.setBackground(new Color(-12828863));
        jarAnalyzerPanel.add(topPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        opPanel = new JPanel();
        opPanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        opPanel.setBackground(new Color(-12828863));
        topPanel.add(opPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        searchSelPanel = new JPanel();
        searchSelPanel.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        searchSelPanel.setBackground(new Color(-12828863));
        opPanel.add(searchSelPanel, new GridConstraints(0, 1, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        searchSelPanel.setBorder(BorderFactory.createTitledBorder(null, "搜索选项", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        callSearchRadioButton = new JRadioButton();
        callSearchRadioButton.setBackground(new Color(-12828863));
        callSearchRadioButton.setText("搜索调用");
        searchSelPanel.add(callSearchRadioButton, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        directSearchRadioButton = new JRadioButton();
        directSearchRadioButton.setBackground(new Color(-12828863));
        directSearchRadioButton.setText("直接搜索");
        searchSelPanel.add(directSearchRadioButton, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        strRadioButton = new JRadioButton();
        strRadioButton.setText("字符串包含搜索");
        searchSelPanel.add(strRadioButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        greatRadioButton = new JRadioButton();
        greatRadioButton.setText("无脑搜索");
        searchSelPanel.add(greatRadioButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        binaryRadioButton = new JRadioButton();
        binaryRadioButton.setText("二进制搜索");
        searchSelPanel.add(binaryRadioButton, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        strRegexRadioButton = new JRadioButton();
        strRegexRadioButton.setText("字符串正则搜索");
        searchSelPanel.add(strRegexRadioButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        actionPanel = new JPanel();
        actionPanel.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        actionPanel.setBackground(new Color(-12828863));
        opPanel.add(actionPanel, new GridConstraints(0, 0, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        actionPanel.setBorder(BorderFactory.createTitledBorder(null, "操作", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        selectJarFileButton = new JButton();
        selectJarFileButton.setText("选择Jar文件或目录");
        actionPanel.add(selectJarFileButton, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        analyzeSpringButton = new JButton();
        analyzeSpringButton.setText("分析Spring框架");
        actionPanel.add(analyzeSpringButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        startAnalysisButton = new JButton();
        startAnalysisButton.setText("开始搜索");
        actionPanel.add(startAnalysisButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useSpringBootJarCheckBox = new JCheckBox();
        useSpringBootJarCheckBox.setBackground(new Color(-12828863));
        useSpringBootJarCheckBox.setText("分析SpringBoot");
        actionPanel.add(useSpringBootJarCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        innerJarsCheckBox = new JCheckBox();
        innerJarsCheckBox.setBackground(new Color(-12828863));
        innerJarsCheckBox.setText("处理内部依赖Jar");
        actionPanel.add(innerJarsCheckBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dcPanel = new JPanel();
        dcPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 5), -1, -1));
        dcPanel.setBackground(new Color(-12828863));
        topPanel.add(dcPanel, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        dcPanel.setBorder(BorderFactory.createTitledBorder(null, "反编译组件选择", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        procyonRadioButton = new JRadioButton();
        procyonRadioButton.setBackground(new Color(-12828863));
        procyonRadioButton.setText("Procyon");
        dcPanel.add(procyonRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cfrRadioButton = new JRadioButton();
        cfrRadioButton.setBackground(new Color(-12828863));
        cfrRadioButton.setText("CFR");
        dcPanel.add(cfrRadioButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        quiltFlowerRadioButton = new JRadioButton();
        quiltFlowerRadioButton.setBackground(new Color(-12828863));
        quiltFlowerRadioButton.setText("QuiltFlower");
        dcPanel.add(quiltFlowerRadioButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        editorPanel = new JPanel();
        editorPanel.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 5), -1, -1));
        editorPanel.setBackground(new Color(-12828863));
        jarAnalyzerPanel.add(editorPanel, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        editorScroll = new JScrollPane();
        editorScroll.setBackground(new Color(-12828863));
        editorPanel.add(editorScroll, new GridConstraints(0, 0, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(600, 400), new Dimension(600, 500), null, 0, false));
        editorScroll.setBorder(BorderFactory.createTitledBorder(null, "反编译Java代码", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        editorPane = new JEditorPane();
        editorPane.setBackground(new Color(-1));
        editorPane.setEditable(false);
        editorScroll.setViewportView(editorPane);
        curPanel = new JPanel();
        curPanel.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 5), -1, -1));
        curPanel.setBackground(new Color(-12828863));
        editorPanel.add(curPanel, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        curLabel = new JLabel();
        curLabel.setText("   当前类和方法");
        curPanel.add(curLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        currentLabel = new JTextField();
        currentLabel.setEditable(false);
        curPanel.add(currentLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        showASMCodeButton = new JButton();
        showASMCodeButton.setText("显示方法ASM代码");
        curPanel.add(showASMCodeButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showByteCodeButton = new JButton();
        showByteCodeButton.setText("显示方法的字节码");
        curPanel.add(showByteCodeButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deleteLogsWhenExitCheckBox = new JCheckBox();
        deleteLogsWhenExitCheckBox.setBackground(new Color(-12828863));
        deleteLogsWhenExitCheckBox.setText("退出时删除日志信息");
        curPanel.add(deleteLogsWhenExitCheckBox, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        callPanel = new JTabbedPane();
        callPanel.setBackground(new Color(-9408398));
        callPanel.setForeground(new Color(-16777216));
        editorPanel.add(callPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(600, -1), null, null, 0, false));
        sourceScroll = new JScrollPane();
        sourceScroll.setBackground(new Color(-12828863));
        callPanel.addTab("谁调用了当前方法", sourceScroll);
        sourceScroll.setBorder(BorderFactory.createTitledBorder(null, "", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        sourceList = new JList();
        sourceScroll.setViewportView(sourceList);
        callScroll = new JScrollPane();
        callScroll.setBackground(new Color(-12828863));
        callPanel.addTab("当前方法调用了谁", callScroll);
        callScroll.setBorder(BorderFactory.createTitledBorder(null, "", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        callList = new JList();
        callScroll.setViewportView(callList);
        subScroll = new JScrollPane();
        subScroll.setBackground(new Color(-12828863));
        callPanel.addTab("当前类所有子类", subScroll);
        subList = new JList();
        subScroll.setViewportView(subList);
        superScroll = new JScrollPane();
        superScroll.setBackground(new Color(-12828863));
        callPanel.addTab("当前类所有父类", superScroll);
        superList = new JList();
        superScroll.setViewportView(superList);
        historyScroll = new JScrollPane();
        callPanel.addTab("历史", historyScroll);
        historyList = new JList();
        historyScroll.setViewportView(historyList);
        springPanel = new JPanel();
        springPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        springPanel.setBackground(new Color(-12828863));
        editorPanel.add(springPanel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        controllerPanel = new JScrollPane();
        controllerPanel.setBackground(new Color(-12828863));
        springPanel.add(controllerPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        controllerPanel.setBorder(BorderFactory.createTitledBorder(null, "Spring Controllers", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        controllerJList = new JList();
        final DefaultListModel defaultListModel1 = new DefaultListModel();
        controllerJList.setModel(defaultListModel1);
        controllerPanel.setViewportView(controllerJList);
        mappingsPanel = new JScrollPane();
        mappingsPanel.setBackground(new Color(-12828863));
        springPanel.add(mappingsPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        mappingsPanel.setBorder(BorderFactory.createTitledBorder(null, "Spring Mappings", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        mappingJList = new JList();
        mappingsPanel.setViewportView(mappingJList);
        authorPanel = new JPanel();
        authorPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        authorPanel.setBackground(new Color(-12828863));
        jarAnalyzerPanel.add(authorPanel, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        authorLabel = new JLabel();
        authorLabel.setText("github.com/4ra1n/jar-analyzer");
        authorPanel.add(authorLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        configPanel = new JPanel();
        configPanel.setLayout(new GridLayoutManager(5, 5, new Insets(0, 0, 0, 5), -1, -1));
        configPanel.setBackground(new Color(-12828863));
        jarAnalyzerPanel.add(configPanel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        methodLabel = new JLabel();
        methodLabel.setText("   输入搜索方法");
        configPanel.add(methodLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        methodText = new JTextField();
        configPanel.add(methodText, new GridConstraints(3, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        classLabel = new JLabel();
        classLabel.setText("   输入搜索类");
        configPanel.add(classLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        classText = new JTextField();
        configPanel.add(classText, new GridConstraints(2, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        jarInfoLabel = new JLabel();
        jarInfoLabel.setText("   Jar信息");
        configPanel.add(jarInfoLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        progressLabel = new JLabel();
        progressLabel.setText("   分析Jar进度");
        configPanel.add(progressLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        progress = new JProgressBar();
        progress.setBackground(new Color(-12828863));
        progress.setForeground(new Color(-9524737));
        progress.setString("");
        progress.setStringPainted(true);
        progress.setToolTipText("");
        progress.setValue(0);
        configPanel.add(progress, new GridConstraints(0, 1, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        searchClassLabel = new JLabel();
        searchClassLabel.setText("可以输入类全名（如java.lang.Runtime）或直接输入类名（如Runtime）");
        configPanel.add(searchClassLabel, new GridConstraints(2, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        searchMethodLabel = new JLabel();
        searchMethodLabel.setText("方法名直接输入名称即可（如exec）不需要输入描述信息");
        configPanel.add(searchMethodLabel, new GridConstraints(3, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jarInfoResultText = new JTextField();
        jarInfoResultText.setEditable(false);
        jarInfoResultText.setEnabled(true);
        configPanel.add(jarInfoResultText, new GridConstraints(1, 1, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        otherSearch = new JLabel();
        otherSearch.setText("   其他搜索");
        configPanel.add(otherSearch, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        otherText = new JTextField();
        configPanel.add(otherText, new GridConstraints(4, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        otherTip = new JLabel();
        otherTip.setText("选择字符串搜索和无脑搜索时输入该项（其他情况无需输入）");
        configPanel.add(otherTip, new GridConstraints(4, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        resultPane = new JPanel();
        resultPane.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 5), -1, -1));
        resultPane.setBackground(new Color(-12828863));
        jarAnalyzerPanel.add(resultPane, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        resultScroll = new JScrollPane();
        resultScroll.setBackground(new Color(-12828863));
        resultPane.add(resultScroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(600, 100), null, null, 0, false));
        resultScroll.setBorder(BorderFactory.createTitledBorder(null, "搜索结果", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        resultList = new JList();
        resultScroll.setViewportView(resultList);
        chanScroll = new JScrollPane();
        chanScroll.setBackground(new Color(-12828863));
        resultPane.add(chanScroll, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(500, 100), null, null, 0, false));
        chanScroll.setBorder(BorderFactory.createTitledBorder(null, "你的链", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        chanList = new JList();
        final DefaultListModel defaultListModel2 = new DefaultListModel();
        chanList.setModel(defaultListModel2);
        chanScroll.setViewportView(chanList);
        treePanel = new JPanel();
        treePanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 5, 0, 0), -1, -1));
        jarAnalyzerPanel.add(treePanel, new GridConstraints(0, 0, 4, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        treeScroll = new JScrollPane();
        treePanel.add(treeScroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(300, -1), null, null, 0, false));
        trees = new FileTree();
        treeScroll.setViewportView(trees);
        allMethodPanel = new JPanel();
        allMethodPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        treePanel.add(allMethodPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        allMethodScroll = new JScrollPane();
        allMethodPanel.add(allMethodScroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 200), null, null, 0, false));
        allMethodScroll.setBorder(BorderFactory.createTitledBorder(null, "当前类的所有方法", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        allMethodList = new JList();
        allMethodScroll.setViewportView(allMethodList);
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(procyonRadioButton);
        buttonGroup.add(cfrRadioButton);
        buttonGroup.add(quiltFlowerRadioButton);
        buttonGroup = new ButtonGroup();
        buttonGroup.add(directSearchRadioButton);
        buttonGroup.add(callSearchRadioButton);
        buttonGroup.add(strRadioButton);
        buttonGroup.add(greatRadioButton);
        buttonGroup.add(binaryRadioButton);
        buttonGroup.add(strRegexRadioButton);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return jarAnalyzerPanel;
    }

}
