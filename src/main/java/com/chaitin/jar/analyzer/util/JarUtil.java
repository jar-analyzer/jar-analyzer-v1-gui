package com.chaitin.jar.analyzer.util;

import com.chaitin.jar.analyzer.core.ClassFile;
import com.chaitin.jar.analyzer.form.JarAnalyzerForm;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class JarUtil {
    private static final Logger logger = Logger.getLogger(JarUtil.class);
    private static final Set<ClassFile> classFileSet = new HashSet<>();

    public static List<ClassFile> resolveNormalJarFile(String jarPath) {
        try {
            Path tmpDir = Paths.get("temp/");
            try {
                Files.createDirectory(tmpDir);
            } catch (Exception ignored) {
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> DirUtil.removeDir(tmpDir.toFile())));
            resolve(jarPath, tmpDir);
            return new ArrayList<>(classFileSet);
        } catch (Exception e) {
            logger.error("error ", e);
        }
        return new ArrayList<>();
    }

    private static void resolve(String jarPathStr, Path tmpDir) {
        Path jarPath = Paths.get(jarPathStr);
        if (!Files.exists(jarPath)) {
            logger.error("jar not exist");
            return;
        }
        try {
            if (jarPathStr.toLowerCase(Locale.ROOT).endsWith(".class")) {
                ClassFile classFile = new ClassFile(jarPathStr, jarPath);
                classFile.jarName = "class";
                classFileSet.add(classFile);
            }
            if (jarPathStr.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                InputStream is = Files.newInputStream(jarPath);
                JarInputStream jarInputStream = new JarInputStream(is);
                JarEntry jarEntry;
                while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                    Path fullPath = tmpDir.resolve(jarEntry.getName());
                    if (!jarEntry.isDirectory()) {
                        if (!jarEntry.getName().endsWith(".class")) {
                            if (jarEntry.getName().endsWith(".jar") && JarAnalyzerForm.innerJars) {
                                Path dirName = fullPath.getParent();
                                if (!Files.exists(dirName)) {
                                    Files.createDirectories(dirName);
                                }
                                try {
                                    Files.createFile(fullPath);
                                } catch (Exception ignored) {
                                }
                                OutputStream outputStream = Files.newOutputStream(fullPath);
                                IOUtil.copy(jarInputStream, outputStream);
                                doInternal(fullPath, tmpDir);
                                outputStream.close();
                            }
                            continue;
                        }
                        Path dirName = fullPath.getParent();
                        if (!Files.exists(dirName)) {
                            Files.createDirectories(dirName);
                        }
                        OutputStream outputStream = Files.newOutputStream(fullPath);
                        IOUtil.copy(jarInputStream, outputStream);
                        outputStream.close();
                        ClassFile classFile = new ClassFile(jarEntry.getName(), fullPath);
                        String splitStr;
                        if (OSUtil.isWindows()) {
                            splitStr = "\\\\";
                        } else {
                            splitStr = "/";
                        }
                        String[] splits = jarPathStr.split(splitStr);
                        classFile.jarName = splits[splits.length - 1];

                        classFileSet.add(classFile);
                    }
                }
                is.close();
                jarInputStream.close();
            }
        } catch (Exception e) {
            logger.error("error ", e);
        }
    }

    private static void doInternal(Path jarPath, Path tmpDir) {
        try {
            InputStream is = Files.newInputStream(jarPath);
            JarInputStream jarInputStream = new JarInputStream(is);
            JarEntry jarEntry;
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                Path fullPath = tmpDir.resolve(jarEntry.getName());
                if (!jarEntry.isDirectory()) {
                    if (!jarEntry.getName().endsWith(".class")) {
                        continue;
                    }
                    Path dirName = fullPath.getParent();
                    if (!Files.exists(dirName)) {
                        Files.createDirectories(dirName);
                    }
                    OutputStream outputStream = Files.newOutputStream(fullPath);
                    IOUtil.copy(jarInputStream, outputStream);
                    outputStream.close();
                    ClassFile classFile = new ClassFile(jarEntry.getName(), fullPath);
                    String splitStr;
                    if (OSUtil.isWindows()) {
                        splitStr = "\\\\";
                    } else {
                        splitStr = "/";
                    }
                    String[] splits = jarPath.toString().split(splitStr);
                    classFile.jarName = splits[splits.length - 1];

                    classFileSet.add(classFile);
                }
            }
            is.close();
            jarInputStream.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}