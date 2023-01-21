package com.chaitin.jar.analyzer.spel;

import java.util.HashMap;
import java.util.Map;

public class MethodEL {
    private String classNameContains;
    private Map<Integer, String> paramTypes;
    private String nameContains;
    private String returnType;
    private int paramsNum;
    boolean isStatic;

    // -------------------- GETTER/SETTER -------------------- //
    public String getClassNameContains() {
        return classNameContains;
    }

    public void setClassNameContains(String classNameContains) {
        this.classNameContains = classNameContains;
    }

    public Map<Integer, String> getParamTypes() {
        return paramTypes;
    }

    public void setParamTypes(Map<Integer, String> paramTypes) {
        this.paramTypes = paramTypes;
    }

    public String getNameContains() {
        return nameContains;
    }

    public void setNameContains(String nameContains) {
        this.nameContains = nameContains;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public int getParamsNum() {
        return paramsNum;
    }

    public void setParamsNum(int paramsNum) {
        this.paramsNum = paramsNum;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public MethodEL (){
        this.paramTypes = new HashMap<>();
    }

    // -------------------- EL -------------------- //

    @SuppressWarnings("unused")
    public MethodEL nameContains(String str) {
        this.nameContains = str;
        return this;
    }

    @SuppressWarnings("unused")
    public MethodEL classNameContains(String str) {
        this.classNameContains = str;
        return this;
    }

    @SuppressWarnings("unused")
    public MethodEL returnType(String str) {
        this.returnType = str;
        return this;
    }

    @SuppressWarnings("unused")
    public MethodEL paramTypeMap(int index, String type) {
        this.paramTypes.put(index, type);
        return this;
    }

    @SuppressWarnings("unused")
    public MethodEL paramsNum(int i) {
        this.paramsNum = i;
        return this;
    }

    @SuppressWarnings("unused")
    public MethodEL isStatic(boolean flag) {
        this.isStatic = flag;
        return this;
    }
}
