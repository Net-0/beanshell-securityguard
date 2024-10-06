package bsh.internals;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.function.Supplier;

// import bsh.BSHClassDeclaration;
import bsh.CallStack;
// import bsh.InterpreterError;
// import bsh.Modifiers;
import bsh.NameSpace;
// import bsh.Node;
// import bsh.ReturnControl;
// import bsh.TargetError;
import bsh.This;

/*
class MyGeneratedClass {

    public MyGeneratedClass {
        Object[] chainArgs = BshClass.getDeclaredConstructors(MyGeneratedClass.class)[10].getChainArgs();pq fazendo as contas, eu só
        BshClass.getDeclaredConstructors(MyGeneratedClass.class)[10].construct(this); // Call the constructor implementation
    }

}
*/

public class BshConstructor {

    public static enum ConstructorChainKind { THIS, SUPER }

    private final int modifiers;
    private final TypeVariable<?>[] typeParams; // TODO: see it!
    private final BshParameter[] parameters;
    private final Class<?>[] exceptionTypes;
    private final Type[] genericExceptionTypes; // TODO: create the generics!

    private final ConstructorChainKind constructorChainKind; // Note: the constructor chain can just be "this", "super" or null
    // private final int constructorChainArgsCount;
    private final Supplier<Object[]> constructorChainArgs;
    private final BshFunction<CallStack, Void> body;

    private BshClass declaringClass;

    public BshConstructor(int modifiers, BshParameter[] parameters, Type[] genericExceptionTypes, ConstructorChainKind constructorChainKind, Supplier<Object[]> constructorChainArgs, BshFunction<CallStack, Void> body) {
        this.modifiers = modifiers;
        this.typeParams = new TypeVariable<?>[0];
        this.parameters = parameters;
        this.exceptionTypes = Types.getRawType(genericExceptionTypes);
        this.genericExceptionTypes = genericExceptionTypes;
        this.constructorChainKind = constructorChainKind;
        this.constructorChainArgs = constructorChainArgs;
        this.body = body;
    }

    protected final void setDeclaringClass(BshClass bshClass) {
        if (this.declaringClass != null) throw new IllegalStateException("Can't re-define the declaring class!");
        this.declaringClass = bshClass;
    }

    protected boolean isVarArgs() {
        return this.parameters.length != 0 && this.parameters[this.parameters.length-1].isVarArgs();
    }

    protected int getModifiers() {
        return this.modifiers;
    }

    protected final Class<?>[] getParameterTypes() {
        final Class<?>[] paramTypes = new Class[this.parameters.length];
        for (int i = 0; i < this.parameters.length; i++)
            paramTypes[i] = this.parameters[i].getType();
        return paramTypes;
    }

    protected final int getParameterCount() {
        return this.parameters.length;
    }

    protected final Type[] getGenericParameterTypes() {
        final Class<?>[] paramTypes = new Class[this.parameters.length];
        for (int i = 0; i < this.parameters.length; i++)
            paramTypes[i] = this.parameters[i].getType();
        return paramTypes;
    }

    protected final BshParameter[] getParameters() {
        return this.parameters;
    }

    protected String getDescriptor() {
        return Types.getMethodDescriptor(void.class, this.getParameterTypes());
    }

    protected String getSignature() {
        return Types.getASMMethodSignature(this.typeParams, this.getGenericParameterTypes(), void.class, this.genericExceptionTypes);
    }

    protected String[] getExceptionInternalNames() {
        return Types.getInternalNames(this.exceptionTypes);
    }

    // TODO: fazer um teste, fields q n existem, podem ser declarados ? Eles ficam acessíveis fora do constructor ? E para variables ( sem  o 'this.' ) ??
    // TODO: fazer um teste, classes declaradas dentro de BshConstructor e BshMethod são acessíveis fora deles???
    // TODO: add 'synchronized' support for this and BshMethod!!!!
    // TODO: method to get the first node being an 'BSHSuperConstructorCall'
    // TODO: '.construct()' must evaluete everything but not the first node when it be an 'BSHSuperConstructorCall'

    // TODO: tem como passar a 'callStack' de um node para cá ?
    public void construct(Object thisArg, Object ...args) throws Throwable {
        final This _this = this.declaringClass.createThis(thisArg);
        // TODO: ver o callerInfoNode para esse NameSpace!
        final NameSpace nameSpace = new NameSpace(this.declaringClass.nameSpace, this.declaringClass.name + ".<init>", _this);
        final CallStack callStack = new CallStack(nameSpace);

        if (this.parameters.length != args.length)
            throw new IllegalArgumentException("Invalid number of arguments");

        for (int i = 0; i < args.length; i++)
            this.parameters[i].setInto(nameSpace, args[i]);

        this.body.invokeImpl(callStack);
        _this.setConstructing(false);
    }
    
}
