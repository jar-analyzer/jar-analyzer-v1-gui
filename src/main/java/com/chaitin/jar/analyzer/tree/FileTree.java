package com.chaitin.jar.analyzer.tree;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;

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
        showHiddenFiles = true;
        showFiles = true;
        initComponents();
        initListeners();
        repaint();
    }

    private void initComponents() {
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
        roots = new File[]{new File("temp")};
        rootNode = new DefaultMutableTreeNode(new FileTreeNode(roots[0]));
        populateSubTree(rootNode);
        if (fileTreeModel != null && rootNode != null) {
            fileTreeModel.setRoot(rootNode);
        }
    }

    private void populateSubTree(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();
        if (userObject instanceof FileTreeNode) {
            FileTreeNode fileTreeNode = (FileTreeNode) userObject;
            File[] files = fileTreeNode.file.listFiles();
            if (files == null) {
                return;
            }
            Arrays.sort(Objects.requireNonNull(files));
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
                    subNode.add(new DefaultMutableTreeNode("fake"));
                }
                node.add(subNode);
            }
        }
    }

    protected DefaultMutableTreeNode rootNode;
    protected DefaultTreeModel fileTreeModel;
    protected boolean showHiddenFiles;
    protected boolean showFiles;
}
