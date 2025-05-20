package bsh.internals;

import java.lang.reflect.Type;
import bsh.NameSpace;
import bsh.UtilEvalError;

public class BshParameter {
    // TODO: ver esses acessores ai
    protected final int modifiers;
    public final String name;
    protected final Type type;
    protected final boolean isVarArgs; // TODO: guardar em modifiers ?

    // TODO: tlvz tirar o 'IMPLICIT_'
    public static final BshParameter IMPLICIT_ENUM_NAME_PARAMETER = new BshParameter(BshModifier.NO_MODIFIERS, String.class, "name", false, false);
    public static final BshParameter IMPLICIT_ENUM_ORDINAL_PARAMETER = new BshParameter(BshModifier.NO_MODIFIERS, int.class, "ordinal", false, false);

    public BshParameter(int modifiers, Type type, String name, boolean isLooseTyped, boolean isVarArgs) {
        this.modifiers = modifiers;
        this.type = type;
        this.name = name;
        this.isVarArgs = isVarArgs;
    }

    // TODO: remover todos esses métodos se possível
    public final boolean isFinal() {
        return BshModifier.isFinal(this.modifiers);
    }

    protected final boolean isVarArgs() {
        return this.isVarArgs;
    }

    protected final boolean isLooseTyped() {
        return this.type == null;
    }

    protected final int getModifiers() {
        return this.modifiers;
    }

    public final Type getType() {
        return this.type == null ? Object.class : this.type;
    }

    // public final Class<?> getType() {
    //     return this.type == null ? Object.class : Types.getRawType(this.type);
    //     // return Types.getRawType(genericType);
    //     // return this.genericType == null ? Object.class : this.genericType;
    // }

    protected final String getName() {
        return this.name;
    }

    // // TODO: remover isso, mas antes precisa corrigir o BshLocalMethod para n depender disso!
    // protected final void setInto(NameSpace nameSpace, Object value) {
    //     try {
    //         // TODO: fazer testes e verificar o modifiers para a variável local
    //         nameSpace.setLocalVariable(this.name, this.getType(), value, null);
    //         // if (this.isLooseTyped())
    //         //     nameSpace.setLocalVariable(this.name, value, false);
    //         // else
    //         //     nameSpace.setTypedVariable(this.name, this.type, value, this.modifiers);

    //         // System.out.println("setting parameter -> " + this.name + " " + value);
    //         // System.out.println("nameSpace.getVariableNames() after setting parameter -> " + Arrays.asList(nameSpace.getVariableNames()));
    //     } catch (UtilEvalError e) {
    //         throw new IllegalArgumentException("Can't set the parameter", e);
    //     }
    // }

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
