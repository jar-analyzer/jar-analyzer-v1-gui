package com.chaitin.jar.analyzer.asm;

import com.chaitin.jar.analyzer.core.ClassReference;
import com.chaitin.jar.analyzer.core.MethodReference;
import org.objectweb.asm.MethodVisitor;

import java.util.List;
import java.util.Map;

public class StringMethodVisitor extends MethodVisitor {
    private final String searchContext;
    private final String ownerName;
    private final String methodName;
    private final String methodDesc;
    private final List<MethodReference> results;
    private final Map<ClassReference.Handle, ClassReference> classMap;
    private final Map<MethodReference.Handle, MethodReference> methodMap;

    public StringMethodVisitor(int api, MethodVisitor methodVisitor, String searchContext,
                               String owner, String methodName, String desc,
                               List<MethodReference> results,
                               Map<ClassReference.Handle, ClassReference> classMap,
                               Map<MethodReference.Handle, MethodReference> methodMap) {
        super(api, methodVisitor);
        this.searchContext = searchContext;
        this.ownerName = owner;
        this.methodName = methodName;
        this.methodDesc = desc;
        this.results = results;
        this.classMap = classMap;
        this.methodMap = methodMap;
    }

    @Override
    public void visitLdcInsn(Object o) {
        if (o instanceof String) {
            if (((String) o).contains(searchContext)) {
                ClassReference.Handle ch = new ClassReference.Handle(ownerName);
                if (classMap.get(ch) != null) {
                    MethodReference m = methodMap.get(new MethodReference.Handle(ch, methodName, methodDesc));
                    if (m != null) {
                        results.add(m);
                    }
                }
            }
        }
        super.visitLdcInsn(o);
    }
}
