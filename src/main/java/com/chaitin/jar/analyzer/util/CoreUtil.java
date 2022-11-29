package com.chaitin.jar.analyzer.util;

import com.chaitin.jar.analyzer.core.ClassFile;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CoreUtil {
    private static final Logger logger = Logger.getLogger(CoreUtil.class);

    public static List<ClassFile> getAllClassesFromJars(List<String> jarPathList) {
        logger.info("get all classes");
        Set<ClassFile> classFileSet = new HashSet<>();
        Path temp = Paths.get("temp");
        DirUtil.removeDir(new File("temp"));
        try {
            Files.createDirectory(temp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (String jarPath : jarPathList) {
            classFileSet.addAll(JarUtil.resolveNormalJarFile(jarPath));
        }
        return new ArrayList<>(classFileSet);
    }
}
