package com.chaitin.jar.analyzer.core;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;

import java.util.Map;
import java.util.Set;

public class Discovery {
    private static final Logger logger = LogManager.getLogger(Discovery.class);

    public static void start(Set<ClassFile> classFileList,
                             Set<ClassReference> discoveredClasses,
                             Set<MethodReference> discoveredMethods,
                             Map<ClassReference.Handle, ClassReference> classMap,
                             Map<MethodReference.Handle, MethodReference> methodMap) {
        logger.info("start discovery information");
        for (ClassFile file : classFileList) {
            try {
                DiscoveryClassVisitor dcv = new DiscoveryClassVisitor(discoveredClasses, discoveredMethods);
                ClassReader cr = new ClassReader(file.getFile());
                cr.accept(dcv, ClassReader.EXPAND_FRAMES);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (ClassReference clazz : discoveredClasses) {
            classMap.put(clazz.getHandle(), clazz);
        }
        for (MethodReference method : discoveredMethods) {
            methodMap.put(method.getHandle(), method);
        }
    }
}
