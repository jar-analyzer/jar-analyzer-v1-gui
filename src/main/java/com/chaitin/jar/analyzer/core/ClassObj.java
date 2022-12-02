package com.chaitin.jar.analyzer.core;

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

    @Override
    public String toString() {
        return this.className;
    }
}
