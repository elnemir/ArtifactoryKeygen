package icu.lama.artifactory.agent.patches;

import javassist.CtClass;
import javassist.CtField;
import javassist.Modifier;
import javassist.NotFoundException;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@Deprecated
public class PatcherLicenseParser extends ClassPatch {
    public PatcherLicenseParser() {
        super("org.jfrog.license.api.a", "org.jfrog.license.api.LicenseManager");
    }

    private static final String STRING_TYPE = "java.lang.String";

    @Override
    byte[] onTransform(String className, CtClass clazz, byte[] classBuf) throws Throwable {
        var overrides = "";

        var publicKeyFieldC = tryGetDeclaredField(clazz, "c"); // 7.9.2 using c
        if (publicKeyFieldC != null && isStringType(publicKeyFieldC)) {
            publicKeyFieldC.setModifiers(Modifier.PRIVATE + Modifier.STATIC);
            overrides += "c = icu.lama.artifactory.agent.Constants.PUBLIC_KEY;";
        }

        var publicKeyFieldD = tryGetDeclaredField(clazz, "d"); // 7.59 changed to d; in 7.133.x "d" may be AtomicBoolean!
        if (publicKeyFieldD != null && isStringType(publicKeyFieldD)) {
            publicKeyFieldD.setModifiers(Modifier.PRIVATE + Modifier.STATIC);
            overrides += "d = icu.lama.artifactory.agent.Constants.PUBLIC_KEY;";
        }

        var publicKeyFieldNObf = tryGetDeclaredField(clazz, "jfrogPublicKey");
        if (publicKeyFieldNObf != null && isStringType(publicKeyFieldNObf)) {
            publicKeyFieldNObf.setModifiers(Modifier.PRIVATE + Modifier.STATIC);
            overrides += "jfrogPublicKey = icu.lama.artifactory.agent.Constants.PUBLIC_KEY;";
        }

        if (overrides.isEmpty()) {
            return classBuf;
        }
        var clinitMethod = Arrays.stream(clazz.getDeclaredBehaviors()).filter((it) -> "<clinit>".equals(it.getMethodInfo().getName())).findAny();
        if (clinitMethod.isEmpty()) {
            return classBuf;
        }
        clinitMethod.get().insertAfter(overrides);

        // val methodParseLicense = ctClass.getMethod("a", "(Ljava/lang/String;)Lorg/jfrog/license/api/License;")
        // methodParseLicense.setBody("""
        //     try {
        //         return b($1, icu.lama.artifactory.tools.Constants.PUBLIC_KEY);
        //     } catch (Exception e) {
        //         e.printStackTrace();
        //         throw e;
        //     }
        // """.trimIndent())

        clazz.detach();
        return clazz.toBytecode();
    }

    private static boolean isStringType(CtField field) {
        try {
            CtClass type = field.getType();
            return type != null && STRING_TYPE.equals(type.getName());
        } catch (Throwable e) {
            return false;
        }
    }

    private @Nullable CtField tryGetDeclaredField(CtClass clazz, String field) {
        try {
            return clazz.getDeclaredField(field);
        } catch (NotFoundException e) {
            return null;
        }
    }
}