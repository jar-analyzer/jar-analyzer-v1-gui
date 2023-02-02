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
        Set<ClassReference.Handle> supers = JarAnalyzerForm.inheritanceMap.getSubClasses(ch);

        String classCon = condition.getClassNameContains();
        String mnCon = condition.getNameContains();
        String retCon = condition.getReturnType();
        Map<Integer, String> paramMap = condition.getParamTypes();

        String methodAnno = condition.getMethodAnno();
        String classAnno = condition.getClassAnno();

        String isSubOf = condition.getIsSubClassOf();
        String isSuperOf = condition.getIsSuperClassOf();

        String start = condition.getStartWith();
        String endWith = condition.getEndWith();

        String hasField = condition.getField();

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
        boolean ag = true;
        boolean ah = true;
        boolean ai = true;

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

        if (classAnno != null && !classAnno.equals("")) {
            if (JarAnalyzerForm.classMap.get(ch).getAnnotations() == null ||
                    JarAnalyzerForm.classMap.get(ch).getAnnotations().size() == 0) {
                ag = false;
            } else {
                boolean fc = false;
                for (String a : JarAnalyzerForm.classMap.get(ch).getAnnotations()) {
                    if (a.contains(classAnno)) {
                        fc = true;
                        break;
                    }
                }
                if (!fc) {
                    ag = false;
                }
            }
        }

        if (methodAnno != null && !methodAnno.equals("")) {
            if (mr.getAnnotations() == null || mr.getAnnotations().size() == 0) {
                ah = false;
            } else {
                boolean fm = false;
                for (String a : mr.getAnnotations()) {
                    if (a.contains(methodAnno)) {
                        fm = true;
                        break;
                    }
                }
                if (!fm) {
                    ah = false;
                }
            }
        }

        if (start != null && !start.equals("")) {
            sw = mr.getName().startsWith(start);
        }

        if (endWith != null && !endWith.equals("")) {
            ew = mr.getName().endsWith(endWith);
        }

        if (hasField != null && !hasField.equals("")) {
            boolean ff = false;
            for (ClassReference.Member m : JarAnalyzerForm.classMap.get(ch).getMembers()) {
                if (m.getName().contains(hasField)) {
                    ff = true;
                    break;
                }
            }
            if (!ff) {
                ai = false;
            }
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
        if (aa && ab && ac && ad && ae && af && ag && ah && ai && sb && sp && sw && ew) {
            searchList.addElement(new ResObj(mr.getHandle(), ch.getName()));
        }
    }
}
