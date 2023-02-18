package com.chaitin.jar.analyzer.model;

import com.chaitin.jar.analyzer.core.ClassFile;
import com.chaitin.jar.analyzer.core.MethodReference;
import com.chaitin.jar.analyzer.form.JarAnalyzerForm;
import org.objectweb.asm.Type;

public class MethodObj {
    private final String className;
    private final MethodReference.Handle method;

    public MethodObj(MethodReference.Handle m, String className) {
        this.className = className;
        this.method = m;
    }

    public String getClassName() {
        return this.className;
    }

    public MethodReference.Handle getMethod() {
        return this.method;
    }

    private String getNumFromDesc() {
        Type methodType = Type.getMethodType(this.method.getDesc());
        Type[] argTypes = methodType.getArgumentTypes();
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int ix = 0; ix < argTypes.length; ix++) {
            String[] temp = argTypes[ix].getClassName().split("\\.");
            sb.append(temp[temp.length - 1]);
            if (ix != argTypes.length - 1) {
                sb.append(",");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    public String getJarFileName() {
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
        String outputFormat = "%s %s";
        return String.format(outputFormat,
                method.getName(),
                getNumFromDesc());
    }
}
