package com.chaitin.jar.analyzer.tree;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;

@SuppressWarnings("unused")
public class FileTree extends JTree {
    public FileTree() {
        try {
            refresh();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void refresh() {
        fileTreeModel = (DefaultTreeModel) treeModel;
        showHiddenFiles = false;
        showFiles = true;
        navigateOSXApps = false;
        initComponents();
        initListeners();
        repaint();
    }

    public DefaultTreeModel getFileTreeModel() {
        return fileTreeModel;
    }

    public File getSelectedFile() {
        TreePath treePath = getSelectionPath();
        if (treePath == null) {
            return null;
        }
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        FileTreeNode fileTreeNode = (FileTreeNode) treeNode.getUserObject();
        return fileTreeNode.file;
    }

    public File[] getSelectedFiles() {
        TreePath[] treePaths = getSelectionPaths();
        if (treePaths == null) {
            return null;
        }
        File[] files = new File[treePaths.length];
        for (int i = 0; i < treePaths.length; i++) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treePaths[i].getLastPathComponent();
            FileTreeNode fileTreeNode = (FileTreeNode) treeNode.getUserObject();
            files[i] = fileTreeNode.file;
        }
        return files;
    }

    private void initComponents() {
        if (Constants.isWindows) {
            fsv = FileSystemView.getFileSystemView();
        }
        initRoot();
        setEditable(false);
    }

    private void initListeners() {
        addTreeExpansionListener(new TreeExpansionListener() {
            public void treeCollapsed(TreeExpansionEvent event) {
            }

            public void treeExpanded(TreeExpansionEvent event) {
                TreePath path = event.getPath();
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                treeNode.removeAllChildren();
                populateSubTree(treeNode);
                fileTreeModel.nodeStructureChanged(treeNode);
            }
        });
    }

    private void initRoot() {
        File[] roots;
        if (Constants.isWindows) {
            roots = fsv.getFiles(new File("temp"), showHiddenFiles);
        } else {
            roots = new File[]{new File("temp")};
        }
        if (roots == null) {
            return;
        }
        if (roots.length == 1) {
            rootNode = new DefaultMutableTreeNode(new FileTreeNode(roots[0]));
            populateSubTree(rootNode);
        } else if (roots.length > 1) {
            rootNode = new DefaultMutableTreeNode("jar");
            for (File root : roots) {
                rootNode.add(new DefaultMutableTreeNode(root));
            }
        } else {
            rootNode = new DefaultMutableTreeNode("error");
        }
        if(fileTreeModel != null && rootNode!=null){
            fileTreeModel.setRoot(rootNode);
        }
    }

    public boolean isDeleteEnabled() {
        return allowDelete;
    }

    public boolean isNavigateOSXApps() {
        return navigateOSXApps;
    }

    public boolean isShowFiles() {
        return showFiles;
    }

    public boolean isShowHiddenFiles() {
        return showHiddenFiles;
    }

    private void populateSubTree(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();
        if (userObject instanceof FileTreeNode) {
            FileTreeNode fileTreeNode = (FileTreeNode) userObject;
            File[] files = fileTreeNode.file.listFiles();
            if (Constants.isWindows) {
                Arrays.sort(Objects.requireNonNull(files), (f1, f2) -> {
                    boolean f1IsDir = f1.isDirectory();
                    boolean f2IsDir = f2.isDirectory();
                    if (f1IsDir == f2IsDir) {
                        return f1.compareTo(f2);
                    }
                    if (f1IsDir) {
                        return -1;
                    }
                    return 1;
                });
            } else {
                if(files==null){
                    return;
                }
                Arrays.sort(Objects.requireNonNull(files));
            }
            for (File file : files) {
                if (file.isFile() && !showFiles) {
                    continue;
                }
                if (!showHiddenFiles && file.isHidden()) {
                    continue;
                }
                FileTreeNode subFile = new FileTreeNode(file);
                DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(subFile);
                if (file.isDirectory()) {
                    if (!Constants.isOSX || navigateOSXApps || !file.getName().endsWith(".app")) {
                        subNode.add(new DefaultMutableTreeNode("fake"));
                    }
                }
                node.add(subNode);
            }
        }
    }

    public void setCurrentFile(File currFile) {
        if (currFile == null || !currFile.exists()) {
            return;
        }
        String path = currFile.getPath();
        String[] pathParts;
        if (Constants.isWindows) {
            pathParts = path.split("\\\\");
        } else {
            pathParts = path.split(File.separator);
        }
        DefaultMutableTreeNode currNode = rootNode;
        for (String part : pathParts) {
            int childCount = currNode.getChildCount();
            for (int i = 0; i < childCount; i++) {
                DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) currNode.getChildAt(i);
                FileTreeNode fileTreeNode = (FileTreeNode) childNode.getUserObject();
                if (fileTreeNode.file.getName().equals(part)) {
                    TreePath treePath = new TreePath(childNode.getPath());
                    expandPath(treePath);
                    selectionModel.setSelectionPath(treePath);
                    currNode = childNode;
                    break;
                }
            }
        }
    }

    public void setDeleteEnabled(boolean allowDelete) {
        this.allowDelete = allowDelete;
    }

    public void setShowFiles(boolean showFiles) {
        if (this.showFiles != showFiles) {
            this.showFiles = showFiles;
            initRoot();
        }
    }

    public void setShowHiddenFiles(boolean showHiddenFiles) {
        if (showHiddenFiles != this.showHiddenFiles) {
            this.showHiddenFiles = showHiddenFiles;
            initRoot();
        }
    }

    public void setNavigateOSXApps(boolean navigateOSXApps) {
        this.navigateOSXApps = navigateOSXApps;
    }

    protected DefaultMutableTreeNode rootNode;
    protected DefaultTreeModel fileTreeModel;
    protected FileSystemView fsv;
    protected boolean showHiddenFiles;
    protected boolean showFiles;
    protected boolean allowDelete;
    protected boolean navigateOSXApps;
}
