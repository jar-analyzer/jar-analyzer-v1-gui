package com.chaitin.jar.analyzer.tree;

import java.io.File;

public class FileTreeNode {
    public FileTreeNode(File file) {
        if (file == null) {
            throw new IllegalArgumentException("Null file not allowed");
        }
        this.file = file;
    }

    public String toString() {
        return file.getName();
    }

    public File file;
}
