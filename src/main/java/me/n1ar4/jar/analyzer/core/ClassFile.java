package me.n1ar4.jar.analyzer.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class ClassFile {
    private final String className;
    private final Path path;
    public String jarName;

    public ClassFile(String className, Path path) {
        this.className = className;
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClassFile classFile = (ClassFile) o;
        return Objects.equals(className, classFile.className);
    }

    @Override
    public int hashCode() {
        return className != null ? className.hashCode() : 0;
    }

    @SuppressWarnings("unused")
    public String getClassName() {
        return className;
    }

    public byte[] getFile() {
        try {
            return Files.readAllBytes(this.path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}