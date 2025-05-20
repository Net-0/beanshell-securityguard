package bsh.internals;

import java.lang.reflect.Type;

public class BshField { // TODO: impl it
    protected final int modifiers;
    protected final Type type;
    
    // TODO: mudar o acessor, é public apenas para testar algo
    public final String name;

    // protected final BshFunction<CallStack, ?> initializer; // TODO: isso deveria evitar chamar setters!
    // public final BshFunction<?, ?> initializer;

    // private BshClass declaringClass;

    public BshField(int modifiers, Type type, String name) {
        this.modifiers = modifiers;
        this.type = type;
        this.name = name;
        // this.initializer = initializer;
    }

    // protected final void setDeclaringClass(BshClass bshClass) {
    //     if (this.declaringClass != null)
    //         throw new IllegalStateException("Can't re-define the declaring class!");
    //     this.declaringClass = bshClass;
    // }

    protected final boolean isStatic() {
        return BshModifier.isStatic(this.modifiers);
    }

    // protected final boolean isEnumConstant() {
    //     return this.modifiers == BshModifier.ENUM_CONSTANT_MODIFIERS;
    // }

    protected final int getModifiers() {
        return this.modifiers;
    }

    protected final String getName() {
        return this.name;
    }

    protected final String getDescriptor() {
        return Types.getDescriptor(this.type);
    }

    protected final String getSignature() {
        return Types.getSignature(this.type);
    }

    @Override
    public String toString() {
        return this.getName(); // TODO: ver um melhor
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
