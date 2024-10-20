package bsh.internals;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;

// import bsh.BSHClassDeclaration;
import bsh.CallStack;
import bsh.NameSpace;
// import bsh.Types;
import bsh.This;

// TODO: adicionar Serializable e Cloneable ?
public final class BshMethod {

    // TODO: verificar os fields de type, pois podem ser loose-typed!
    // TODO: change protected to private ?
    private final int modifiers;
    private final Type genericReturnType;
    private final Class<?> returnType;
    private final TypeVariable<?>[] typeParams; // TODO: see it!
    private final String name;
    private final BshParameter[] parameters;
    private final Class<?>[] exceptionTypes;
    private final Type[] genericExceptionTypes; // TODO: create the generics!
    // protected final boolean isVarArgs; // TODO: add it!

    // TODO: trocar o Node[] por um BSHBlock ? Manteria tudo centralizado!!!! Mas precisaria corrigir o strictJava do BSHBlock
    private final BshFunction<CallStack, ?> body; // Scripted method body

    private BshClass declaringClass;

    public BshMethod(final int modifiers, final Type genericReturnType, final String name, final BshParameter[] parameters, final Type[] genericExceptionTypes, final BshFunction<CallStack, ?> body) {
        this.modifiers = modifiers;
        this.returnType = Types.getRawType(genericReturnType);
        this.genericReturnType = genericReturnType;
        this.typeParams = new TypeVariable<?>[0];
        this.name = name;
        this.parameters = parameters;
        this.exceptionTypes = Types.getRawType(genericExceptionTypes);
        this.genericExceptionTypes = genericExceptionTypes;
        this.body = body;
    }

    protected final void setDeclaringClass(BshClass bshClass) {
        if (this.declaringClass != null) throw new IllegalStateException("Can't re-define the declaring class!");
        this.declaringClass = bshClass;
    }

    protected final boolean isAbstract() {
        return BshModifier.isAbstract(this.modifiers);
    }

    protected final boolean isStatic() {
        return BshModifier.isStatic(this.modifiers);
    }

    protected final boolean isVarArgs() {
        return this.parameters.length != 0 && this.parameters[this.parameters.length-1].isVarArgs();
    }

    protected final int getModifiers() {
        return this.modifiers;
    }

    protected final Class<?> getReturnType() {
        return this.returnType == null ? Object.class : this.returnType;
    }

    protected final Type getGenericReturnType() {
        return this.genericReturnType == null ? Object.class : this.genericReturnType;
    }

    protected final String getName() {
        return this.name;
    }

    // protected final Class<?>[] getParameterTypes() {
    //     final Class<?>[] paramTypes = new Class[this.parameters.length];
    //     for (int i = 0; i < this.parameters.length; i++)
    //         paramTypes[i] = this.parameters[i].getType();
    //     return paramTypes;
    // }

    protected final Type[] getGenericParameterTypes() {
        final Type[] paramTypes = new Class[this.parameters.length];
        for (int i = 0; i < this.parameters.length; i++)
            paramTypes[i] = this.parameters[i].getGenericType();
        return paramTypes;
    }

    protected final int getParameterCount() {
        return this.parameters.length;
    }

    protected final BshParameter[] getParameters() {
        return this.parameters;
    }

    protected final String getDescriptor() {
        return Types.getMethodDescriptor(this.getReturnType(), this.getGenericParameterTypes());
    }

    protected final String getSignature() {
        return Types.getASMMethodSignature(this.typeParams, this.getGenericParameterTypes(), this.getGenericReturnType(), this.genericExceptionTypes);
    }

    protected final String[] getExceptionInternalNames() {
        return Types.getInternalNames(this.exceptionTypes);
    }

    // TODO: adicionar um 'expressionNode' ou 'declaringNode' para ser usado quando criar o InterpreterError, para mostrar nas mensagens em que trecho houve problema ?
    // TODO: fazer um teste, fields q n existem, podem ser declarados ? Eles ficam acessíveis fora do constructor ? E para variables ( sem  o 'this.' ) ??
    // TODO: fazer um teste, classes declaradas dentro de BshConstructor e BshMethod são acessíveis fora deles???
    // TODO: add 'synchronized' support for this and BshMethod!!!!
    // TODO: method to get the first node being an 'BSHSuperConstructorCall'
    // TODO: '.construct()' must evaluete everything but not the first node when it be an 'BSHSuperConstructorCall'

    // TODO: ajustar essa classe para remover o BshLocalMethod; adicionar um .bound(Object thisArg) para retornar um BshMethod com um thisArg default assim como no JS ?

    public final Object invoke(Object ...args) throws Throwable {
        final String fullName = String.format("%s.%s", this.declaringClass.name, this.name);
        // TODO: ver o callerInfoNode para esse NameSpace!
        final NameSpace nameSpace = new NameSpace(this.declaringClass.nameSpace, fullName, this.declaringClass);
        final CallStack callStack = new CallStack(nameSpace);

        if (this.parameters.length != args.length)
            throw new IllegalArgumentException("Invalid number of arguments");

        for (int i = 0; i < args.length; i++)
            this.parameters[i].setInto(nameSpace, args[i]);

        return body.apply(callStack);
    }

    public final Object invoke(Object thisArg, Object ...args) throws Throwable {
        // System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        final String fullName = String.format("%s.%s", this.declaringClass.name, this.name);
        final This _this = this.isStatic() ? null : this.declaringClass.getThis(thisArg);
        // final Interpreter interpreter = this.declaringClass.declaringInterpreter;
        // TODO: ver o callerInfoNode para esse NameSpace!
        final NameSpace nameSpace = new NameSpace(this.declaringClass.nameSpace, fullName, this.declaringClass, _this);

        final CallStack callStack = new CallStack(nameSpace);
        // System.out.println(">>>>>>>>>>>>>>>>>>>>>");
        // System.out.println("args: " + Arrays.asList(args));
        // System.out.println("parameters: ");
        for (BshParameter param: this.getParameters())
            System.out.println(" - " + param.getName());
        

        if (this.parameters.length != args.length)
            throw new IllegalArgumentException("Invalid number of arguments");

        for (int i = 0; i < args.length; i++)
            this.parameters[i].setInto(nameSpace, args[i]);

        // System.out.println("nameSpace.getVariableNames(): " + Arrays.asList(nameSpace.getVariableNames()));
        // System.out.println(">>>>>>>>>>>>>>>>>>>>>");

        try {
            return body.apply(callStack);
        } catch (Throwable t) {
            throw t;
        } finally {
            callStack.pop();
        }
    }

}
