package me.n1ar4.jar.analyzer.asm;

import me.n1ar4.jar.analyzer.core.ClassReference;
import me.n1ar4.jar.analyzer.core.MethodReference;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;

import java.util.List;
import java.util.Map;

public class GreatMethodVisitor extends MethodVisitor {
    private final String searchContext;
    private final String ownerName;
    private final String methodName;
    private final String methodDesc;
    private final List<MethodReference> results;
    private final Map<ClassReference.Handle, ClassReference> classMap;
    private final Map<MethodReference.Handle, MethodReference> methodMap;

    public GreatMethodVisitor(int api, MethodVisitor methodVisitor, String searchContext,
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

        if (methodName.contains(searchContext)) {
            ClassReference.Handle ch = new ClassReference.Handle(this.ownerName);
            if (classMap.get(ch) != null) {
                MethodReference m = methodMap.get(new MethodReference.Handle(ch, methodName, desc));
                if (m != null) {
                    results.add(m);
                }
            }
        }
    }

    @Override
    public void visitParameter(String name, int access) {
        super.visitParameter(name, access);
        if (name.contains(searchContext)) {
            ClassReference.Handle ch = new ClassReference.Handle(ownerName);
            if (classMap.get(ch) != null) {
                MethodReference m = methodMap.get(new MethodReference.Handle(ch, methodName, methodDesc));
                if (m != null) {
                    results.add(m);
                }
            }
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        super.visitFieldInsn(opcode, owner, name, descriptor);
        if (owner.contains(searchContext) || name.contains(searchContext)) {
            ClassReference.Handle ch = new ClassReference.Handle(ownerName);
            if (classMap.get(ch) != null) {
                MethodReference m = methodMap.get(new MethodReference.Handle(ch, methodName, methodDesc));
                if (m != null) {
                    results.add(m);
                }
            }
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
                                       Object... bootstrapMethodArguments) {
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        if (name.contains(searchContext)) {
            ClassReference.Handle ch = new ClassReference.Handle(ownerName);
            if (classMap.get(ch) != null) {
                MethodReference m = methodMap.get(new MethodReference.Handle(ch, methodName, methodDesc));
                if (m != null) {
                    results.add(m);
                }
            }
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        if (owner.contains(searchContext) || name.contains(searchContext)) {
            ClassReference.Handle ch = new ClassReference.Handle(ownerName);
            if (classMap.get(ch) != null) {
                MethodReference m = methodMap.get(new MethodReference.Handle(ch, methodName, methodDesc));
                if (m != null) {
                    results.add(m);
                }
            }
        }
    }

    @Override
    public void visitLdcInsn(Object o) {
        if (o instanceof String) {
            MethodReference mr = null;
            ClassReference.Handle ch = new ClassReference.Handle(ownerName);
            if (classMap.get(ch) != null) {
                MethodReference m = methodMap.get(new MethodReference.Handle(ch, methodName, methodDesc));
                if (m != null) {
                    mr = m;
                }
            }
            if (((String) o).contains(searchContext)) {
                if (mr != null) {
                    results.add(mr);
                }
            }
        }
        super.visitLdcInsn(o);
    }
}
