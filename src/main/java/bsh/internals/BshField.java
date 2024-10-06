package bsh.internals;

import java.lang.reflect.Type;
import java.util.concurrent.Callable;

import bsh.InterpreterError;

public class BshField { // TODO: impl it
    private final int modifiers;
    private final Type genericType;
    private final Class<?> type;
    private final String name;
    // TODO: salvar o Node e dar um Node.eval() usando this.declaringClass.declaringInterpreter e this.declaringClass.declaringNameSpace
    private final Callable<?> initializer; // Execute when starting the field!

    private BshClass declaringClass;

    public BshField(int modifiers, Type genericType, String name, Callable<?> initializer) {
        // bsh.util.MyField;
        this.modifiers = modifiers;
        this.genericType = genericType;
        this.type = Types.getRawType(genericType);
        this.name = name;
        this.initializer = initializer;
    }

    protected final void setDeclaringClass(BshClass bshClass) {
        if (this.declaringClass != null) throw new IllegalStateException("Can't re-define the declaring class!");
        this.declaringClass = bshClass;
    }

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
        return Types.getASMSignature(this.genericType);
    }

    public final Object getDefaultValue() {
        try {
            return this.initializer.call();
        } catch (Throwable t) {
            throw new InterpreterError(t.getMessage(), t);
        }
    }

    // // Variables to execute the defaultValueNode
    // // private final Interpreter declaringInterpreter = null;
    // private final NameSpace declaringNameSpace = null;

    // Object getDefaultValue() { // TODO: see it better
    //     NameSpace nameSpace = new NameSpace(this.declaringNameSpace, "?"); // Create a new nameSpace to eval nodes
    //     Interpreter interpreter = new Interpreter(nameSpace);
    //     try {
    //         return this.initializer.eval(null, interpreter);
    //     } catch (EvalError e) {
    //         throw new InterpreterError("Error while declaring field default value", e);
    //     }
    // }
}
