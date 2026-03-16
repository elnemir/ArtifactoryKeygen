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

    @Override
    public final byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        var clazz = className.replace("/", ".");
        if (!targetClasses.contains(clazz)) {
            return classfileBuffer;
        }
        var sourceInfo = protectionDomain != null && protectionDomain.getCodeSource() != null && protectionDomain.getCodeSource().getLocation() != null
                ? protectionDomain.getCodeSource().getLocation().toString()
                : "unknown";
        System.out.println("Artifactory Agent :: Patching class: " + clazz + " (source: " + sourceInfo + ")");

        ClassPool ctPool = new ClassPool();
        if (loader != null) {
            ctPool.appendClassPath(new LoaderClassPath(loader));
        } else {
            ctPool.appendSystemPath();
        }

        try {
            CtClass ctClass = ctPool.makeClass(new ByteArrayInputStream(classfileBuffer));
            if (classBeingRedefined == null) {
                return this.onTransform(clazz, ctClass, classfileBuffer);
            } else {
                return this.onRetransform(clazz, ctClass, classfileBuffer, classBeingRedefined);
            }
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
