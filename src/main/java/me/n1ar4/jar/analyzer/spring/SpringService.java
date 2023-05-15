package me.n1ar4.jar.analyzer.spring;

import me.n1ar4.jar.analyzer.core.ClassFile;
import me.n1ar4.jar.analyzer.core.ClassReference;
import me.n1ar4.jar.analyzer.core.MethodReference;
import me.n1ar4.jar.analyzer.spring.asm.SpringClassVisitor;
import org.objectweb.asm.ClassReader;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SpringService {
    public static void start(Set<ClassFile> classFileList,
                             List<SpringController> controllers,
                             Map<ClassReference.Handle, ClassReference> classMap,
                             Map<MethodReference.Handle, MethodReference> methodMap) {
        for (ClassFile file : classFileList) {
            try {
                SpringClassVisitor mcv = new SpringClassVisitor(controllers, classMap, methodMap);
                ClassReader cr = new ClassReader(file.getFile());
                cr.accept(mcv, ClassReader.EXPAND_FRAMES);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}