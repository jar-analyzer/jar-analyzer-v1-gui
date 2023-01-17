package com.chaitin.jar.analyzer.asm;

import com.chaitin.jar.analyzer.core.ClassReference;
import com.chaitin.jar.analyzer.core.MethodReference;
import org.objectweb.asm.*;

import java.util.List;
import java.util.Map;

public class StringClassVisitor extends ClassVisitor {
    private final boolean flag;
    private String name;
    private final String searchContext;
    private final List<MethodReference> results;
    private final Map<ClassReference.Handle, ClassReference> classMap;
    private final Map<MethodReference.Handle, MethodReference> methodMap;

    public StringClassVisitor(boolean flag,String searchContext,
                              List<MethodReference> results,
                              Map<ClassReference.Handle, ClassReference> classMap,
                              Map<MethodReference.Handle, MethodReference> methodMap) {
        super(Opcodes.ASM9);
        this.flag = flag;
        this.searchContext = searchContext;
        this.results = results;
        this.classMap = classMap;
        this.methodMap = methodMap;
    }

    @Override
    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.name = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        return new StringMethodVisitor(api, mv, this.searchContext, flag,
                this.name, name, desc, results, classMap, methodMap);
    }
}

