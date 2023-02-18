package com.chaitin.jar.analyzer.model;

import com.chaitin.jar.analyzer.core.ClassFile;
import com.chaitin.jar.analyzer.form.JarAnalyzerForm;
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

    public String getJarFileName() {
        for (ClassFile cf : JarAnalyzerForm.classFileList) {
            String temp = this.resObj.getClassName().replace(".", "/");
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
        String outputFormat = " method: %s \t path: %s";
        return String.format(outputFormat, this.springMapping.getMethodName().getName(),
                this.getSpringMapping().getPath());
    }
}
