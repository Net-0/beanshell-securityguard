package bsh.internals;

import bsh.CallStack;
import bsh.EvalError;

public class BshEnumConstant { // TODO: impl it

    @FunctionalInterface
    public static interface ArgsSupplier<T> {
        // TODO: tlvz ver os nomes dos métodos das functional interfaces ?
        Object[] apply() throws EvalError;
    }

    // private final Type type;
    protected final String name;
    // protected final BshFunction<CallStack, ?> initializer; // TODO: isso deveria evitar chamar setters!
    public final int argsLength;
    public final ArgsSupplier<?> argsSupplier;

    // private BshClass declaringClass;

    // TODO: ver isso
    protected final boolean innerMembers = false;
    // TODO: esses membros n podem ser statics!
    protected final BshField[] innerFields = new BshField[0];
    protected final BshMethod[] innerMethods = new BshMethod[0];

    public BshEnumConstant(String name, int argsLength, ArgsSupplier<?> argsSupplier) {
        // this.modifiers = modifiers;
        // this.type = type;
        this.name = name;
        // this.initializer = initializer;
        this.argsLength = argsLength;
        this.argsSupplier = argsSupplier;
    }

    // protected final void setDeclaringClass(BshClass bshClass) {
    //     if (this.declaringClass != null)
    //         throw new IllegalStateException("Can't re-define the declaring class!");
    //     this.declaringClass = bshClass;
    // }

    // protected final boolean isStatic() {
    //     return BshModifier.isStatic(this.modifiers);
    // }

    // protected final boolean isEnumConstant() {
    //     return this.modifiers == BshModifier.ENUM_CONSTANT_MODIFIERS;
    // }

    // protected final int getModifiers() {
    //     return this.modifiers;
    // }

    // protected final String getName() {
    //     return this.name;
    // }

    // protected final String getDescriptor() {
    //     return this.declaringClass != null ? this.declaringClass.descriptor : null;
    // }

    // protected final String getSignature() {
    //     return this.declaringClass != null ? this.declaringClass.descriptor : null;
    // }

    @Override
    public String toString() {
        return this.name;
    }

    // public final Object getDefaultValue() {
    // try {
    // return this.initializer.call();
    // } catch (Throwable t) {
    // throw new InterpreterError(t.getMessage(), t);
    // }
    // }

    // // Variables to execute the defaultValueNode
    // // private final Interpreter declaringInterpreter = null;
    // private final NameSpace declaringNameSpace = null;

    // Object getDefaultValue() { // TODO: see it better
    // NameSpace nameSpace = new NameSpace(this.declaringNameSpace, "?"); // Create
    // a new nameSpace to eval nodes
    // Interpreter interpreter = new Interpreter(nameSpace);
    // try {
    // return this.initializer.eval(null, interpreter);
    // } catch (EvalError e) {
    // throw new InterpreterError("Error while declaring field default value", e);
    // }
    // }
}
