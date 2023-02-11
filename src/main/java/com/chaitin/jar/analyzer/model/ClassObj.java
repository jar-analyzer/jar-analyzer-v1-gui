package com.chaitin.jar.analyzer.model;

import com.chaitin.jar.analyzer.core.ClassFile;
import com.chaitin.jar.analyzer.core.ClassReference;
import com.chaitin.jar.analyzer.form.JarAnalyzerForm;

public class ClassObj {
    private final String className;

    private final ClassReference.Handle handle;

    public ClassObj(String className, ClassReference.Handle handle) {
        this.className = className;
        this.handle = handle;
    }

    public ClassReference.Handle getHandle() {
        return handle;
    }

    public String getClassName() {
        return this.className;
    }

    private String getJarFileName() {
        for (ClassFile cf : JarAnalyzerForm.classFileList) {
            String temp = this.className.replace(".", "/");
            temp += ".class";
            String target = cf.getClassName();
            if (target.contains("BOOT-INF")) {
                target = target.substring(17);
            }
            if (target.contains("WEB-INF")) {
                target = target.substring(16);
            }
            if (target.equals(temp)) {
                return cf.jarName;
            }
        }
        return "unknown";
    }

    @Override
    public String toString() {
        return this.className + " (" + getJarFileName() + ")";
    }
}
