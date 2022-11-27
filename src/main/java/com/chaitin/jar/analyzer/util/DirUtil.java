package com.chaitin.jar.analyzer.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DirUtil {
    private static final List<String> filenames = new ArrayList<>();

    @SuppressWarnings("unused")
    public static List<String> GetFiles(String path) {
        filenames.clear();
        return getFiles(path);
    }

    private static List<String> getFiles(String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                return filenames;
            }
            for (File value : files) {
                if (value.isDirectory()) {
                    getFiles(value.getPath());
                } else {
                    filenames.add(value.getAbsolutePath());
                }
            }
        } else {
            filenames.add(file.getAbsolutePath());
        }
        return filenames;
    }

    public static boolean removeDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = removeDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }
}
