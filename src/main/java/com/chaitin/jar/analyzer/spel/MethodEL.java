package com.chaitin.jar.analyzer.spel;

public class MethodEL {
    private String  classNameContains;
    private String  nameContains;
    private String returnContains;
    private int paramsNum;

    public String getClassNameContains() {
        return classNameContains;
    }

    public String getNameContains() {
        return nameContains;
    }

    public String getReturnContains() {
        return returnContains;
    }

    public int getParamsNum() {
        return paramsNum;
    }

    public MethodEL nameContains(String  str){
        this.nameContains = str;
        return this;
    }
    public MethodEL classNameContains(String str){
        this.classNameContains = str;
        return this;
    }
    public MethodEL returnContains(String str){
        this.returnContains = str;
        return this;
    }
    public MethodEL paramsNum(int i){
        this.paramsNum = i;
        return this;
    }
}
