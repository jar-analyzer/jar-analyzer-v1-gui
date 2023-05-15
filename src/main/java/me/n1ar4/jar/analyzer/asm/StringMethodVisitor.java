package me.n1ar4.jar.analyzer.asm;

import me.n1ar4.jar.analyzer.core.ClassReference;
import me.n1ar4.jar.analyzer.core.MethodReference;
import org.objectweb.asm.MethodVisitor;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class StringMethodVisitor extends MethodVisitor {
    private final boolean flag;
    private final String searchContext;
    private final String ownerName;
    private final String methodName;
    private final String methodDesc;
    private final List<MethodReference> results;
    private final Map<ClassReference.Handle, ClassReference> classMap;
    private final Map<MethodReference.Handle, MethodReference> methodMap;

    public StringMethodVisitor(int api, MethodVisitor methodVisitor, String searchContext,
                               boolean flag, String owner, String methodName, String desc,
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
        this.flag = flag;
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

            if (flag && ((String) o).contains(searchContext)) {
                if (mr != null) {
                    results.add(mr);
                }
            }
            if (!flag) {
                boolean matches = Pattern.matches(searchContext, (String) o);
                if (matches && mr != null) {
                    results.add(mr);
                }
            }
        }
        super.visitLdcInsn(o);
    }
}
