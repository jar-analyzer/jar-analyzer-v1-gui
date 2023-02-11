package com.chaitin.jar.analyzer.model;

import com.chaitin.jar.analyzer.core.ClassFile;
import com.chaitin.jar.analyzer.core.MethodReference;
import com.chaitin.jar.analyzer.form.JarAnalyzerForm;
import org.objectweb.asm.Type;

public class ResObj {
    private final String className;
    private final MethodReference.Handle method;

    public ResObj(MethodReference.Handle m, String className) {
        this.className = className;
        this.method = m;
    }

    public String getClassName() {
        return this.className;
    }

    public MethodReference.Handle getMethod() {
        return this.method;
    }

    private int getNumFromDesc() {
        Type methodType = Type.getMethodType(this.method.getDesc());
        return methodType.getArgumentTypes().length;
    }

    private String getJarFileName() {
        for (ClassFile cf : JarAnalyzerForm.classFileList) {
            String temp = this.className.replace(".", "/");
            temp += ".class";
            String target = cf.getClassName();
            if(target.contains("BOOT-INF")){
                target =target.substring(17);
            }
            if(target.contains("WEB-INF")){
                target =target.substring(16);
            }
            if (target.equals(temp)) {
                return cf.jarName;
            }
        }
        return "unknown";
    }

    @Override
    public String toString() {
        String outputFormat = "%s %s (params:%d) (%s)";
        return String.format(outputFormat,
                className,
                method.getName(),
                getNumFromDesc(),
                getJarFileName());
    }
}
