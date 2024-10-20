package bsh.internals;

import java.util.StringJoiner;

public interface BshModifier {

    // 32-bits representation of the internal Java modifiers
    public static final int PUBLIC                  = 0b00000000000000000000000000000001;
    public static final int PRIVATE                 = 0b00000000000000000000000000000010;
    public static final int PROTECTED               = 0b00000000000000000000000000000100;
    public static final int STATIC                  = 0b00000000000000000000000000001000;
    public static final int FINAL                   = 0b00000000000000000000000000010000;
    public static final int SYNCHRONIZED            = 0b00000000000000000000000000100000;
    public static final int VOLATILE                = 0b00000000000000000000000001000000, // Field only
                            BRIDGE                  = 0b00000000000000000000000001000000; // Method only // TODO: BRIDGE tem relação com o TypeReference do jackson!
    // TODO: fazer teste unitário previnindo o uso de "transient" para BshMethod e BshConstructor!
    public static final int TRANSIENT               = 0b00000000000000000000000010000000, // Field only  // TODO: as classes já teriam suporte próprio ao TRANSIENT ???
                            VARARGS                 = 0b00000000000000000000000010000000; // Method only
    public static final int NATIVE                  = 0b00000000000000000000000100000000;
    public static final int INTERFACE               = 0b00000000000000000000001000000000;
    public static final int ABSTRACT                = 0b00000000000000000000010000000000;
    public static final int STRICT                  = 0b00000000000000000000100000000000; // TODO: esse é o "strictfp"!!! // TODO: precisa implementar um suporte para essa budega ?
    public static final int SYNTHETIC               = 0b00000000000000000001000000000000;
    public static final int ANNOTATION              = 0b00000000000000000010000000000000;
    public static final int ENUM                    = 0b00000000000000000100000000000000;
    public static final int MANDATED                = 0b00000000000000001000000000000000; // TODO: ver esta merda também!

    // 32-bits representation of all available Java modifiers for each stuff
    public static final int CLASS_MODIFIERS         = 0b00000000000000000000110000011111; // PUBLIC | PRIVATE | PROTECTED | STATIC | FINAL | ABSTRACT | STRICT
    public static final int INTERFACE_MODIFIERS     = 0b00000000000000000000111000001111; // PUBLIC | PRIVATE | PROTECTED | STATIC | INTERFACE | ABSTRACT | STRICT
    public static final int CONSTRUCTOR_MODIFIERS   = 0b00000000000000000000000010000111; // PUBLIC | PRIVATE | PROTECTED | VARARGS
    public static final int METHOD_MODIFIERS        = 0b00000000000000001001110111111111; // PUBLIC | PRIVATE | PROTECTED | ABSTRACT | STATIC | FINAL | SYNCHRONIZED | NATIVE | STRICT | BRIDGE | VARARGS | SYNTHETIC | MANDATED
    public static final int FIELD_MODIFIERS         = 0b00000000000000000001000011011111; // PUBLIC | PRIVATE | PROTECTED | STATIC | FINAL | VOLATILE | TRANSIENT | SYNTHETIC
    public static final int PARAMETER_MODIFIERS     = 0b00000000000000000000000000010000; // FINAL
    public static final int ACCESS_MODIFIERS        = 0b00000000000000000000000000000111; // PUBLIC | PRIVATE | PROTECTED
    public static final int ENUM_CONSTANT_MODIFIERS = 0b00000000000000000100000000011001; // PUBLIC | STATIC | FINAL | ENUM

    // 32-bits representation without any modifier
    public static final int NO_MODIFIERS            = 0b00000000000000000000000000000000;

    public static boolean isPublic(int mod) { return (mod & PUBLIC) != 0; }
    public static boolean isPrivate(int mod) { return (mod & PRIVATE) != 0; }
    public static boolean isProtected(int mod) { return (mod & PROTECTED) != 0; }
    public static boolean isStatic(int mod) { return (mod & STATIC) != 0; }
    public static boolean isFinal(int mod) { return (mod & FINAL) != 0; }
    public static boolean isSynchronized(int mod) { return (mod & SYNCHRONIZED) != 0; }
    public static boolean isVolatile(int mod) { return (mod & VOLATILE) != 0; }
    public static boolean isBridge(int mod) { return (mod & BRIDGE) != 0; }
    public static boolean isTransient(int mod) { return (mod & TRANSIENT) != 0; }
    public static boolean isVarArgs(int mod) { return (mod & VARARGS) != 0; }
    public static boolean isNative(int mod) { return (mod & NATIVE) != 0; }
    public static boolean isInterface(int mod) { return (mod & INTERFACE) != 0; }
    public static boolean isAbstract(int mod) { return (mod & ABSTRACT) != 0; }
    public static boolean isStrict(int mod) { return (mod & STRICT) != 0; }
    public static boolean isSynthetic(int mod) { return (mod & SYNTHETIC) != 0; }
    public static boolean isAnnotation(int mod) { return (mod & ANNOTATION) != 0; }
    public static boolean isEnum(int mod) { return (mod & ENUM) != 0; }
    public static boolean isMandated(int mod) { return (mod & MANDATED) != 0; }

    public static String toString(int mod) {
        StringJoiner sj = new StringJoiner(" ");
        if (BshModifier.isPublic(mod)) sj.add("public");
        if (BshModifier.isProtected(mod)) sj.add("protected");
        if (BshModifier.isPrivate(mod)) sj.add("private");
        if (BshModifier.isAbstract(mod)) sj.add("abstract");
        if (BshModifier.isStatic(mod)) sj.add("static");
        if (BshModifier.isFinal(mod)) sj.add("final");
        if (BshModifier.isTransient(mod)) sj.add("transient");
        if (BshModifier.isVolatile(mod)) sj.add("volatile");
        if (BshModifier.isSynchronized(mod)) sj.add("synchronized");
        if (BshModifier.isNative(mod)) sj.add("native");
        if (BshModifier.isStrict(mod)) sj.add("strictfp");
        if (BshModifier.isInterface(mod)) sj.add("interface");
        return sj.toString();
    }

    public static String toBinaryString(int mod) {
        final char[] bits = new char[32];
        for (int i = 0, j = bits.length-1; i < bits.length; i++, j--)
            bits[j] = ((mod >> i) & 1) == 1 ? '1' : '0';
        return "0b" + new String(bits);
    }
}
