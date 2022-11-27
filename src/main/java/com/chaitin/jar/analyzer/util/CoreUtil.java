package com.chaitin.jar.analyzer.util;

import com.chaitin.jar.analyzer.core.ClassFile;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CoreUtil {
    private static final Logger logger = Logger.getLogger(CoreUtil.class);

    public static ArrayList<ClassFile> getAllClassesFromJar(String jarPath) {
        logger.info("get all classes");
        Path temp = Paths.get("temp");
        try {
            Files.createDirectory(temp);
        } catch (IOException ignored) {
        }
        Set<ClassFile> classFileSet = new HashSet<>(JarUtil.resolveNormalJarFile(jarPath));
        return new ArrayList<>(classFileSet);
    }
}
