package com.chaitin.jar.analyzer.spel;

import com.chaitin.jar.analyzer.core.ClassReference;
import com.chaitin.jar.analyzer.core.MethodReference;
import com.chaitin.jar.analyzer.form.JarAnalyzerForm;
import com.chaitin.jar.analyzer.model.ResObj;
import org.objectweb.asm.Type;

import javax.swing.*;
import java.util.Map;
import java.util.Set;

public class MethodELProcessor {
    private final ClassReference.Handle ch;
    private final MethodReference mr;
    private final DefaultListModel<ResObj> searchList;
    private final MethodEL condition;

    public MethodELProcessor(ClassReference.Handle ch, MethodReference mr,
                             DefaultListModel<ResObj> searchList, MethodEL condition) {
        this.ch = ch;
        this.mr = mr;
        this.searchList = searchList;
        this.condition = condition;
    }

    public void process() {
        Set<ClassReference.Handle> subs = JarAnalyzerForm.inheritanceMap.getSuperClasses(ch);
        Set<ClassReference.Handle> supers =  JarAnalyzerForm.inheritanceMap.getSubClasses(ch);

        String classCon = condition.getClassNameContains();
        String mnCon = condition.getNameContains();
        String retCon = condition.getReturnType();
        Map<Integer, String> paramMap = condition.getParamTypes();

        String isSubOf = condition.getIsSubClassOf();
        String isSuperOf = condition.getIsSuperClassOf();

        String start = condition.getStartWith();
        String endWith = condition.getEndWith();

        Integer i = condition.getParamsNum();
        Boolean f = condition.isStatic();
        int paramNum = Type.getMethodType(mr.getDesc()).getArgumentTypes().length;
        String ret = Type.getReturnType(mr.getDesc()).getClassName();

        boolean aa = true;
        boolean ab = true;
        boolean ac = true;
        boolean ad = true;
        boolean ae = true;
        boolean af = true;

        boolean sb = true;
        boolean sp = true;

        boolean sw = true;
        boolean ew = true;

        if (classCon != null && !classCon.equals("")) {
            aa = ch.getName().contains(classCon);
        }

        if (mnCon != null && !mnCon.equals("")) {
            ab = mr.getName().contains(mnCon);
        }

        if (start != null && !start.equals("")) {
            sw = mr.getName().startsWith(start);
        }

        if (endWith != null && !endWith.equals("")) {
            ew = mr.getName().endsWith(endWith);
        }

        if (i != null) {
            ac = i == paramNum;
        }

        if (retCon != null && !retCon.equals("")) {
            ad = ret.equals(retCon);
        }

        if (f != null) {
            ae = f == mr.isStatic();
        }

        if (isSubOf != null && !isSubOf.equals("")) {
            if (subs != null && subs.size() != 0) {
                boolean t = false;
                for (ClassReference.Handle h : subs) {
                    if (h.getName().equals(isSubOf)) {
                        t = true;
                        break;
                    }
                }
                if (!t) {
                    sb = false;
                }
            } else {
                sb = false;
            }
        }
        if (isSuperOf != null && !isSuperOf.equals("")) {
            if (supers != null && supers.size() != 0) {
                boolean t = false;
                for (ClassReference.Handle h : supers) {
                    if (h.getName().equals(isSuperOf)) {
                        t = true;
                        break;
                    }
                }
                if (!t) {
                    sp = false;
                }
            } else {
                sb = false;
            }
        }
        Type[] argTypes = Type.getArgumentTypes(mr.getDesc());
        for (int ix = 0; ix < argTypes.length; ix++) {
            String temp = paramMap.get(ix);
            if (temp == null) {
                continue;
            }
            if (!paramMap.get(ix).equals(argTypes[ix].getClassName())) {
                af = false;
                break;
            }
        }
        if (aa && ab && ac && ad && ae && af && sb && sp && sw && ew) {
            searchList.addElement(new ResObj(mr.getHandle(), ch.getName()));
        }
    }
}
