package com.chaitin.jar.analyzer.core;

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
        String desc = this.method.getDesc();
        String in = desc.split("\\)")[0];
        if (in.equals("(")) {
            return 0;
        }
        if (!in.contains(";")) {
            return 1;
        }
        return in.split(";").length;
    }

    @Override
    public String toString() {
        String outputFormat = " %s \t %s \t %d";
        return String.format(outputFormat,
                className,
                method.getName(),
                getNumFromDesc());
    }
}
