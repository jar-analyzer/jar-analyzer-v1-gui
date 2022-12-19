package com.chaitin.jar.analyzer.model;

import com.chaitin.jar.analyzer.spring.SpringMapping;

public class MappingObj {
    private ResObj resObj;
    private SpringMapping springMapping;

    public SpringMapping getSpringMapping() {
        return springMapping;
    }

    public void setSpringMapping(SpringMapping springMapping) {
        this.springMapping = springMapping;
    }

    public ResObj getResObj() {
        return resObj;
    }

    public void setResObj(ResObj resObj) {
        this.resObj = resObj;
    }

    @Override
    public String toString() {
        String outputFormat = " method: %s \t path: %s";
        return String.format(outputFormat,this.springMapping.getMethodName().getName(),
                this.getSpringMapping().getPath());
    }
}
