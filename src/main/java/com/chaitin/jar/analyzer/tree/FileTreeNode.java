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
        String name = file.getName();
        if (!Constants.isWindows) {
            return name;
        }
        if (name.length() == 0) {
            return file.getPath();
        }
        return name;
    }

    public File file;
}
