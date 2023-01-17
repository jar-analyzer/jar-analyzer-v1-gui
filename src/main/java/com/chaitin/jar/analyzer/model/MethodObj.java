package com.chaitin.jar.analyzer.model;

import com.chaitin.jar.analyzer.core.MethodReference;
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

    private int getNumFromDesc() {
        Type methodType = Type.getMethodType(this.method.getDesc());
        return methodType.getArgumentTypes().length;
    }

    @Override
    public String toString() {
        String outputFormat = "%s \t params: %d";
        return String.format(outputFormat,
                method.getName(),
                getNumFromDesc());
    }
}
