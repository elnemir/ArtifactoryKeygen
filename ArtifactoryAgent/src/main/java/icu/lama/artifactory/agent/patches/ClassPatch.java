package icu.lama.artifactory.agent.patches;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;

abstract public class ClassPatch implements ClassFileTransformer {
    public final List<String> targetClasses;

    public ClassPatch(String... targetClasses) {
        this.targetClasses = Arrays.asList(targetClasses);
    }

    private static final String PACKAGE_PREFIX = "org.jfrog.license";

    @Override
    public final byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (classfileBuffer == null || classfileBuffer.length == 0) {
            return null;
        }
        String clazz = className.replace("/", ".");
        if (!clazz.startsWith(PACKAGE_PREFIX) || !targetClasses.contains(clazz)) {
            return null;
        }
        if (loader == null) {
            return null;
        }

        try {
            String sourceInfo = "unknown";
            if (protectionDomain != null && protectionDomain.getCodeSource() != null && protectionDomain.getCodeSource().getLocation() != null) {
                sourceInfo = protectionDomain.getCodeSource().getLocation().toString();
            }
            System.out.println("Artifactory Agent :: Patching class: " + clazz);
        } catch (Throwable ignored) { }

        ClassPool ctPool = new ClassPool();
        try {
            ctPool.appendClassPath(new LoaderClassPath(loader));
        } catch (Throwable t) {
            System.err.println("Artifactory Agent :: Failed to set classpath for " + clazz + ": " + t.getMessage());
            return null;
        }

        try {
            CtClass ctClass = ctPool.makeClass(new ByteArrayInputStream(classfileBuffer));
            byte[] result;
            if (classBeingRedefined == null) {
                result = this.onTransform(clazz, ctClass, classfileBuffer);
            } else {
                result = this.onRetransform(clazz, ctClass, classfileBuffer, classBeingRedefined);
            }
            return result != null ? result : classfileBuffer;
        } catch (Throwable t) {
            System.err.println("Artifactory Agent :: Failed to patch " + clazz + ": " + t.getMessage());
            t.printStackTrace();
        }
        return classfileBuffer;
    }

    byte[] onTransform(String className, CtClass clazz, byte[] classBuf) throws Throwable {
        return classBuf;
    }

    byte[] onRetransform(String className, CtClass clazz, byte[] classBuf, Class<?> classBeingRedefined) throws Throwable {
        return classBuf;
    }
}
