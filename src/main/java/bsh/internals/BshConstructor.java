package bsh.internals;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

// import bsh.BSHClassDeclaration;
import bsh.CallStack;
import bsh.EvalError;
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

    // public static final byte NO_CHAIN = 0,
    //                          THIS_CHAIN = 1,
    //                          SUPER_CHAIN = 2;

    @FunctionalInterface
    public static interface ChainArgsSupplier<T> {
        Object[] apply(CallStack callStack, Class<T> _class, T thisArg, Object ...args) throws EvalError;
    }

    @FunctionalInterface
    public static interface BodyFunction<T> {
        void apply(CallStack callStack, Class<T> _class, T thisArg, Object ...args) throws EvalError;
    }

    // TODO: tirar o 'generic' dos nomes dos fields de types
    protected final int modifiers;
    protected final TypeVariable<?>[] typeParams; // TODO: see it!
    protected final BshParameter[] parameters;
    // protected final Class<?>[] exceptionTypes;
    protected final Type[] genericExceptionTypes; // TODO: create the generics!

    protected final Type[] genericParametersTypes;

    // protected final byte constructorChainKind; // Note: the constructor chain can just be "this", "super" or null
    // // protected final int constructorChainArgsCount;
    // protected final BshSupplier<Object[]> constructorChainArgs;
    protected final boolean superChain;
    protected final int chainArgsLength;
    // TODO: fazer teste, os nodes de args do constructor chain n devem ter acesso ao 'this' pq ele ainda está unitialized
    public final ChainArgsSupplier<?> chainArgsSupplier;
    public final BodyFunction<?> body;

    protected final String descriptor;
    protected final String signature;

    public static final BshConstructor DEFAULT_CONSTRUCTOR = new BshConstructor(
        BshModifier.PUBLIC,
        new BshParameter[0],
        new Type[0],
        true,
        0,
        (callStack, _class, thisArg, args) -> new Object[0],
        (callStack, _class, thisArg, args) -> {}
    );

    public static final BshConstructor DEFAULT_ENUM_CONSTRUCTOR = new BshConstructor(
        BshModifier.PRIVATE,
        new BshParameter[] { BshParameter.IMPLICIT_ENUM_NAME_PARAMETER, BshParameter.IMPLICIT_ENUM_ORDINAL_PARAMETER },
        new Type[0],
        true,
        2,
        (callStack, _class, thisArg, args) -> new Object[] { args[0], args[1] },
        (callStack, _class, thisArg, args) -> {}
    );

    // private BshClass declaringClass;

    public BshConstructor(int modifiers, BshParameter[] parameters, Type[] genericExceptionTypes, boolean superChain, int chainArgsLength, ChainArgsSupplier<?> chainArgsSupplier, BodyFunction<?> body) {
        this.modifiers = modifiers;
        this.typeParams = new TypeVariable<?>[0];
        this.parameters = parameters;
        // this.exceptionTypes = Types.getRawType(genericExceptionTypes);
        this.genericExceptionTypes = genericExceptionTypes;
        this.superChain = superChain;
        this.chainArgsLength = chainArgsLength;
        this.chainArgsSupplier = chainArgsSupplier;
        this.body = body;

        this.genericParametersTypes = new Type[parameters.length];
        for (int i = 0; i < this.parameters.length; i++)
            this.genericParametersTypes[i] = this.parameters[i].getType();

        // final String constructorDescriptor = Types.getMethodDescriptor(void.class, genericParamsTypes);
        // final String constructorSignature = Types.getASMMethodSignature(bshConstructor.typeParams, genericParamsTypes, void.class, bshConstructor.genericExceptionTypes);

        this.descriptor = Types.getMethodDescriptor(void.class, this.genericParametersTypes);
        this.signature = Types.getMethodSignature(this.typeParams, this.genericParametersTypes, void.class, this.genericExceptionTypes);
    }

    // TODO: remover todos os métodos, calcular tudo no construtor e ler os campos, para diminuirmos o tamanho num geral
    // protected final void setDeclaringClass(BshClass bshClass) {
    //     if (this.declaringClass != null) throw new IllegalStateException("Can't re-define the declaring class!");
    //     this.declaringClass = bshClass;
    // }

    public boolean isVarArgs() {
        return this.parameters.length != 0 && this.parameters[this.parameters.length-1].isVarArgs();
    }

    // protected int getModifiers() {
    //     return this.modifiers;
    // }

    // TODO: ver para
    // TODO: ver melhor esses rawType, tlvz seja melhor ter um cache no BshParameter ?
    public final Class<?>[] getParameterTypes() {
        // final Class<?>[] paramTypes = new Class[this.parameters.length];
        // for (int i = 0; i < this.parameters.length; i++)
        //     paramTypes[i] = this.parameters[i].getType();
        // return paramTypes;
        return Types.getRawType(this.genericParametersTypes);
    }

    // // TODO: tlvz salvar o valor e o getter ser somente um retorno desse "cache" ?
    // protected final Type[] getGenericParametersTypes() {
    //     final Type[] paramTypes = new Class[this.parameters.length];
    //     for (int i = 0; i < this.parameters.length; i++)
    //         paramTypes[i] = this.parameters[i].getGenericType();
    //     return paramTypes;
    // }

    // protected final int getParameterCount() {
    //     return this.parameters.length;
    // }

    // protected final BshParameter[] getParameters() {
    //     return this.parameters;
    // }

    // protected String getDescriptor() {
    //     return Types.getMethodDescriptor(void.class, this.getGenericParametersTypes());
    // }

    // protected String getSignature() {
    //     return Types.getASMMethodSignature(this.typeParams, this.getGenericParametersTypes(), void.class, this.genericExceptionTypes);
    // }

    // protected String[] getExceptionInternalNames() {
    //     return Types.getInternalNames(this.exceptionTypes);
    // }

    // protected final boolean canBeAnOption(int argsLength) {
    //     return this.parameters.length == argsLength || (this.isVarArgs() && argsLength > this.parameters.length);
    // }

    // protected This createThis(Object thisArg) {
    //     final This _this = new This(thisArg, this.declaringInterpreter.getStrictJava());
    //     thisCache.put(thisArg, _this);
    //     thisFinalFieldsAlreadySet.put(_this, new HashSet<>());
    //     return _this;
    // }

    // TODO: fazer um teste, fields q n existem, podem ser declarados ? Eles ficam acessíveis fora do constructor ? E para variables ( sem  o 'this.' ) ??
    // TODO: fazer um teste, classes declaradas dentro de BshConstructor e BshMethod são acessíveis fora deles???
    // TODO: add 'synchronized' support for this and BshMethod!!!!
    // TODO: method to get the first node being an 'BSHSuperConstructorCall'
    // TODO: '.construct()' must evaluete everything but not the first node when it be an 'BSHSuperConstructorCall'

    // // TODO: renomear para initialize() ?
    // // TODO: tem como passar a 'callStack' de um node para cá ?
    // public void construct(Object thisArg, Object ...args) throws Throwable {
    //     final This _this = this.declaringClass.getThis(thisArg);
    //     // TODO: ver o callerInfoNode para esse NameSpace!
    //     // TODO: e se um constructor chamar outro ? como fica a stackTrace ??
    //     final NameSpace nameSpace = new NameSpace(this.declaringClass.nameSpace, this.declaringClass.name + ".<init>", this.declaringClass, _this);
    //     final CallStack callStack = new CallStack(nameSpace);

    //     if (this.parameters.length != args.length)
    //         throw new IllegalArgumentException("Invalid number of arguments");

    //     for (int i = 0; i < args.length; i++)
    //         this.parameters[i].setInto(nameSpace, args[i]);

    //     this.body.apply(callStack);
    // }
    
}
