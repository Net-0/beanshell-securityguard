package bsh.internals;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;

import bsh.NameSpace;
import bsh.UtilEvalError;

public class BshParameter {
    private final int modifiers;
    private final String name;
    private final Class<?> type;
    private final Type genericType;
    private final boolean isVarArgs;
    // protected final boolean isVarArgs; // TODO: resolver isso

    public BshParameter(Parameter parameter) {
        this(parameter.getModifiers(), parameter.getName(), parameter.getParameterizedType(), parameter.isVarArgs());
    }

    public BshParameter(int modifiers, String name, Type genericType, boolean isVarArgs) {
        // genericType = genericType == null ? Object.class : genericType;
        this.modifiers = modifiers;
        this.name = name;
        this.type = Types.getRawType(genericType);
        this.genericType = genericType;
        this.isVarArgs = isVarArgs;
    }

    protected final boolean isFinal() {
        return BshModifier.isFinal(this.modifiers);
    }

    protected final boolean isVarArgs() {
        return this.isVarArgs;
    }

    protected final boolean isLooseTyped() {
        return this.genericType == null;
    }

    protected final int getModifiers() {
        return this.modifiers;
    }

    protected final Class<?> getType() {
        return this.type == null ? Object.class : this.type;
    }

    protected final Type getGenericType() {
        return this.genericType == null ? Object.class : this.genericType;
    }

    protected final String getName() {
        return this.name;
    }

    protected final void setInto(NameSpace nameSpace, Object value) {
        try {
            // TODO: fazer testes e verificar o modifiers para a variável local
            nameSpace.setLocalVariable(this.name, this.type, value, null);
            // if (this.isLooseTyped())
            //     nameSpace.setLocalVariable(this.name, value, false);
            // else
            //     nameSpace.setTypedVariable(this.name, this.type, value, this.modifiers);

            // System.out.println("setting parameter -> " + this.name + " " + value);
            // System.out.println("nameSpace.getVariableNames() after setting parameter -> " + Arrays.asList(nameSpace.getVariableNames()));
        } catch (UtilEvalError e) {
            throw new IllegalArgumentException("Can't set the parameter", e);
        }
    }

    // protected static BshParameter[] paramsFrom(int[] paramsModifiers, String[] paramsNames, Type[] genericParamsTypes) {
    //     final BshParameter[] params = new BshParameter[paramsNames.length];
    //     for (int i = 0; i < paramsNames.length; i++)
    //         params[i] = new BshParameter(paramsModifiers[i], paramsNames[i], genericParamsTypes[i], i);
    //     return params;
    // }

    // protected int getModifiers() {
    //     return this.modifiers.getParameterModifiers();
    // }

}
