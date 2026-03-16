package icu.lama.artifactory.agent.patches;

import javassist.CtClass;
import javassist.CtMethod;

/**
 * Patches org.jfrog.license.legacy.LegacyLicenseManager so that when load() fails
 * (e.g. "last block incomplete in decryption" for non-legacy license format),
 * the method returns null instead of throwing. This allows LicenseManager.loadLicense
 * to fall back to the new format path (base64 JSON + signature) used by Keygen.
 */
public class LegacyLicenseManagerPatch extends ClassPatch {

    public LegacyLicenseManagerPatch() {
        super("org.jfrog.license.legacy.LegacyLicenseManager");
    }

    @Override
    byte[] onTransform(String className, CtClass clazz, byte[] classBuf) throws Throwable {
        CtClass throwableClass = clazz.getClassPool().get("java.lang.Throwable");
        boolean patched = false;
        for (CtMethod m : clazz.getDeclaredMethods()) {
            if (!"load".equals(m.getMethodInfo().getName())) {
                continue;
            }
            // Only patch methods that return a reference type (can return null)
            if (m.getReturnType().isPrimitive()) {
                continue;
            }
            try {
                m.addCatch("return null;", throwableClass);
                patched = true;
            } catch (Throwable t) {
                System.err.println("Artifactory Agent :: LegacyLicenseManagerPatch: could not addCatch to " + m.getLongName() + ": " + t.getMessage());
            }
        }
        if (!patched) {
            return classBuf;
        }
        clazz.detach();
        return clazz.toBytecode();
    }
}
