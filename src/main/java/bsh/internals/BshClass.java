package bsh.internals;

import java.lang.reflect.Constructor;
// import java.lang.reflect.Field;
// import java.lang.reflect.InvocationTargetException;
// import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
// import java.nio.file.Files;
// import java.nio.file.Paths;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.HashMap;
// import java.util.List;
import java.util.WeakHashMap;
// import java.util.stream.Collectors;

import bsh.BshClassManager;
import bsh.CallStack;
import bsh.EvalError;
import bsh.Interpreter;
// import bsh.Modifiers;
import bsh.NameSpace;
import bsh.Reflect;
import bsh.This;
import bsh.internals.type.BshLazyType;

// TODO: fazer teste para verificar se uma classe declarada em um Interpreter é acesível em outro
// TODO: como ficam inner classes ? e.g., new ClassLoader() { void doSomething() { this.defineClass(); } } 
// TODO: add support for static blocks!
// TODO: validate duplicated fields when creating the Class<?>. Note: validate also for static and non-static!
// TODO: same validation of fields for methods but with signature validation!
// TODO: verificar esse comentário e se sim, fazer testes para isso -> interfaces still cannot have constructors, fields, private methods, or implementations of equals/hashCode/toString

// TODO: adicionar generics para o .toClass() ter generic tb ?
public final class BshClass {

    @FunctionalInterface
    public static interface InitializerFunction<T> {
        void initialize(BshClass bashClass, T thisArg) throws EvalError;
    }

    // TODO: make some test to verify memory leak!
    // - Note1: maybe a for loop where the iterations are calculated base on O.S. RAM and the size of bytes created by this class
    // - Note2: (test it) the Class<?> inside a ClassLoader is just garbage-collected when the ClassLoader itself is garbage collected!!!!!
    protected static final WeakHashMap<Class<?>, BshClass> storage = new WeakHashMap<>();

    protected final int modifiers;

    // protected final BshClass declaringClass = null; // Null if it's not an innerClass
    // protected final String packageName; // Package name
    // protected final String simpleName; // Simple class name
    public final String name; // Full name
    // TODO: fazer esse Name bonitinho considerando todas as inner classes
    protected final String canonicalName;
    protected final String internalName; // Internal name
    protected final String descriptor;

    protected final TypeVariable<?>[] typeParameters; // Declared generic types
    // TODO: renomear para 'superType' ?
    protected final Type superClass; // Super class being extended
    protected final String superInternalName;
    protected final Type[] interfaces; // Interfaces being implemented

    public final BshEnumConstant[] enumConstants; // Declared enum constants of this class
    public final BshField[] fields; // Declared fields of this class
    public final BshConstructor[] constructors; // Declared constructors of this class
    public final BshMethod[] methods; // Declared methods of this class

    public final InitializerFunction<?>[] staticInitializers;
    public final InitializerFunction<?>[] initializers;

    // TODO: How write innerClasses ? And inner classes of innerClasse ?
    // protected final BshClass[] innerClasses = new BshClass[0]; // Declared inner classes

    public final Constructor<?>[] superConstructors;

    // Variables to execute methods
    // protected final Interpreter declaringInterpreter;
    // TODO: não faz + sentido guardarmos só o 'declaringNameSpace' ? pq ter um nameSpace próprio para essa generated class ?
    // protected final NameSpace nameSpace; // TODO: ver isso, faz sentido ? E o 'this' ? Onde fica, como chamar, pode setar valores além do normal dentro de constructors e methods ???
    protected final BshClassManager bcm;

    protected final Set<String> finalFieldsSet = new HashSet<>(); // Static fields
    private final WeakHashMap<Object, This> instances = new WeakHashMap<>();

    // TODO: ver para esses campos serem gerados ao construir o objeto!
    // protected final byte[] classBytes;
    // protected final Class<?> _class;

    // private final WeakHashMap<This, Set<String>> thisFinalFieldsAlreadySet = new WeakHashMap<>();

    // // TODO: substituir tlvz por uma referência para o BshClass ?
    // private final NameSpace declaringNameSpace;

    // private final List<Field> availableFields = new ArrayList<>();
    // private final List<Method> availableMethods = new ArrayList<>();

    public BshClass(int modifiers, String name, TypeVariable<?>[] typeParameters, Type superClass, Type[] interfaces, BshEnumConstant[] enumConstants, BshField[] fields, BshConstructor[] constructors, BshMethod[] methods, InitializerFunction<?>[] staticInitializers, InitializerFunction<?>[] initializers, BshClassManager bcm) {
        this.modifiers = modifiers;
        this.name = name;
        // TODO: ver esse canonical name
        this.canonicalName = name;
        this.internalName = name.replace('.', '/');
        this.descriptor = "L" + this.internalName + ";";
        this.typeParameters = typeParameters;
        this.superClass = superClass;
        this.superInternalName = Types.getInternalName(superClass);
        this.interfaces = interfaces;
        this.enumConstants = enumConstants;
        this.fields = fields;
        this.constructors = constructors;
        this.methods = methods;
        this.staticInitializers = staticInitializers;
        this.initializers = initializers;
        this.superConstructors = Types.getRawType(superClass).getDeclaredConstructors(); // TODO: talvez os superConstructors deveriam ser lazy loaded ? Para possibilitar referências + dinâmicas de super classes ?
        // this.declaringInterpreter = declaringInterpreter;
        // this.nameSpace = nameSpace;
        this.bcm = bcm;

        // for (BshInitializer staticInitializer: this.staticInitializers) staticInitializer.setDeclaringClass(this);
        // for (BshInitializer initializer: this.initializers) initializer.setDeclaringClass(this);
        // for (BshField field: this.fields) field.setDeclaringClass(this);
        // for (BshConstructor constructor: this.constructors) constructor.setDeclaringClass(this);
        // for (BshMethod method: this.methods) method.setDeclaringClass(this);

        // this.availableFields.addAll(Arrays.asList(this._class.getDeclaredFields()));
        // for (Class<?> s = Types.getRawType(superClass); s != null; s = s.getSuperclass())
        //     for (Field f: s.getDeclaredFields())
        //         if (Reflect.isPublic(f) || Reflect.isProtected(f))
        //             this.availableFields.add(f);

        // this.availableMethods.addAll(Arrays.asList(this._class.getDeclaredMethods()));
        // for (Class<?> s = Types.getRawType(superClass); s != null; s = s.getSuperclass())
        //     for (Method m: s.getDeclaredMethods())
        //         if (Reflect.isPublic(m) || Reflect.isProtected(m))
        //             this.availableMethods.add(m);
    }

    protected String getSignature() {
        return Types.getClassSignature(this.typeParameters, this.superClass, this.interfaces);
    }

    // protected boolean isEnum() { // TODO: ver para usar isso
    //     return this.modifiers.hasModifier("enum");
    // }

    // protected boolean isInterface() { // TODO: ver para usar isso
    //     return this.modifiers.hasModifier("interface");
    // }

    public static boolean isGeneratedClass(Class<?> _class) { // TODO: there is some need for it ?
        return BshClass.storage.containsKey(_class);
    }

    public static BshClass fromGeneratedClass(Class<?> generatedClass) {
        final BshClass bshClass = BshClass.storage.get(generatedClass);
        if (bshClass == null) throw new IllegalArgumentException("The given class isn't a generated class!");
        return bshClass;
    }

    // protected static BshClass getBshClass(Class<?> _class) {
    //     return BshClass.storage.get(_class);
    // }

    // // Note: it should be called just by BshConstructor!
    // protected This createThis(Object thisArg) {

    // }

    // private final WeakHashMap<Object, This> thisCache = new WeakHashMap<>();
    // private final This createThis(Object thisArg) {
    //     final This _this = new This(thisArg, this.declaringInterpreter.getStrictJava());
    //     thisCache.put(thisArg, _this);
    //     return _this;
    // }
    public This getThisFromObject(Object thisArg) {
        if (thisArg == null) return null;
        // TODO: implementar pelo menos um 'instanceof' para validar o tipo do 'thisArg' ?
        // TODO: implementar um BshThis e um NameSpaceThis para separar melhor essas picas ai
        this.instances.putIfAbsent(thisArg, new This(thisArg));
        return this.instances.get(thisArg);
        // final This _this = thisCache.get(thisArg);
        // if (_this == null) throw new IllegalArgumentException("Invalid 'thisArg'!");
        // return _this;
    }

    // TODO: tirar esse método e fazer a validação no BshConstructor usando o BshClass#staticFinalFiedlsSet
    // TODO: controlar isso por meio do 'This' e do body de BshConstructor, para tirar esse lixo daqui
    /** Verify if a static field can be set, if yes, it also already lock the field to don't be able to be set later */
    public static boolean canSetStaticFinalField(Class<?> _class, String fieldName) {
        final BshClass bshClass = BshClass.storage.get(_class);
        return bshClass != null && bshClass.finalFieldsSet != null && bshClass.finalFieldsSet.add(fieldName);
    }

    // /** Verify if a field can be set, if yes, it also already lock the field to don't be able to be set later */
    // public static boolean canSetFinalField(Class<?> _class, Object thisArg, String fieldName) {
    //     if (!This.isObjectWrapper(thisArg)) return false;
    //     final BshClass bshClass = BshClass.storage.get(_class);
    //     if (bshClass == null) return false;
    //     final Set<String> finalFieldsAlreadySet = bshClass.thisFinalFieldsAlreadySet.get((This) thisArg);
    //     return finalFieldsAlreadySet != null && finalFieldsAlreadySet.add(fieldName);
    // }

    // private final WeakHashMap<Object, List<String>>

    // // TODO: fazer teste para o 'EvalError' do .staticInitialize() tb ?
    // // Note: execute static blocks, initialize static fields and execute body-statement on non strictJava mode
    // public static void staticInitialize(Class<?> generatedClass) throws EvalError {
    //     final BshClass bshClass = BshClass.storage.get(generatedClass);
    //     if (bshClass == null) throw new IllegalArgumentException("The given class isn't a generated class!");

    //     final NameSpace nameSpace = new NameSpace(bshClass.nameSpace, bshClass.name, bshClass);
    //     final CallStack callStack = new CallStack(nameSpace);

    //     for (final BshConsumer<CallStack> staticInitializer: bshClass.staticInitializers)
    //         staticInitializer.consume(callStack);
    // }

    // // TODO: remover isso!
    // // TODO: fazer teste para o 'EvalError' do .initialize() tb ?
    public static <T> void initialize(Class<T> generatedClass, T thisArg) throws EvalError {
        // final BshClass bshClass = BshClass.storage.get(generatedClass);
        // if (bshClass == null) throw new IllegalArgumentException("The given class isn't a generated class!");

        // final This _this = bshClass.createThis(thisArg);
        // final NameSpace nameSpace = new NameSpace(bshClass.nameSpace, "static " + bshClass.name, bshClass, _this);
        // final CallStack callStack = new CallStack(nameSpace);

        // for (final BshConsumer<CallStack> initiliazer: bshClass.initializers)
        //     initiliazer.consume(callStack);
    }

    // public final int findConstructorIndex(Object[] args) {
    //     final Class<?>[] argsTypes = Reflect.getTypes(args);
    //     int index = Reflect.findMostSpecificInvocableIndex(argsTypes, this.constructors);
    //     if (index != -1) return index;

    //     // TODO: adicionar condição com 'isEnum' para não considerar os 2 primeiros argTypes
    //     // TODO: criar um Reflect.findMostSpecificInvocableIndex() para BshConstructor e mover esse 'throw new NoSuchMethodError()' para o case padrão de chamar construtores
    //     final String msg = String.format("No such constructor: %s(%s)", _class.getName(), String.join(", ", Reflect.getTypesNames(argsTypes)));
    //     throw new NoSuchMethodError(msg);
    // }

    // // TODO: precisamos disso ?
    // public final int findSuperConstructorIndex(Object[] args) {
    //     final Class<?>[] argsTypes = Reflect.getTypes(args);
    //     int index = Reflect.findMostSpecificInvocableIndex(argsTypes, this.superConstructors);
    //     if (index != -1) return index;

    //     // TODO: adicionar condição com 'isEnum' para não considerar os 2 primeiros argTypes
    //     // TODO: criar um Reflect.findMostSpecificInvocableIndex() para BshConstructor e mover esse 'throw new NoSuchMethodError()' para o case padrão de chamar construtores
    //     final String msg = String.format("No such constructor: %s(%s)", _class.getName(), String.join(", ", Reflect.getTypesNames(argsTypes)));
    //     throw new NoSuchMethodError(msg);
    // }

    // // TODO: implementar no class generator para escrever um binário com essa lógica para diminuir os métodos públicos
    // public static int getThisChainConstructorIndex(Class<?> generatedClass, Object[] args) {
    //     final BshClass bshClass = BshClass.storage.get(generatedClass);
    //     if (bshClass == null) throw new IllegalArgumentException("The given class isn't a generated class!");
    //     final Class<?>[] argsTypes = Reflect.getTypes(args);
    //     final List<BshConstructor> constructors = Arrays.asList(bshClass.constructors);
    //     return Reflect.findMostSpecificInvocableIndex(argsTypes, constructors, BshConstructor::getParameterTypes, BshConstructor::isVarArgs);
    // }

    // // TODO: implementar no class generator para escrever um binário com essa lógica para diminuir os métodos públicos
    // public static int getSuperChainConstructorIndex(Class<?> generatedClass, Object[] args) {
    //     BshClass bshClass = BshClass.storage.get(generatedClass);
    //     if (bshClass == null) throw new IllegalArgumentException("The given class isn't a generated class!");
    //     Class<?>[] argsTypes = Reflect.getTypes(args);
    //     List<Constructor<?>> constructors = Arrays.asList(bshClass.superConstructors);
    //     return Reflect.findMostSpecificInvocableIndex(argsTypes, constructors, Constructor::getParameterTypes, Constructor::isVarArgs);
    // }

    // TODO: remover esses 'getDeclared...', só vão aumentar mais a latência de algumas operações!

    // public final static BshInitializer[] getDeclaredInitializers(Class<?> generatedClass) {
    //     final BshClass bshClass = BshClass.storage.get(generatedClass);
    //     if (bshClass == null) throw new IllegalArgumentException("The given class isn't a generated class!");
    //     return bshClass.initializers;
    // }

    // public final static BshInitializer[] getDeclaredStaticInitializers(Class<?> generatedClass) {
    //     final BshClass bshClass = BshClass.storage.get(generatedClass);
    //     if (bshClass == null) throw new IllegalArgumentException("The given class isn't a generated class!");
    //     System.out.println("bshClass.staticInitializers.length: " + bshClass.staticInitializers.length);
    //     try {
    //         System.out.println("bshClass.staticInitializers[0].initialize(): " + bshClass.staticInitializers[0].initialize());
    //     } catch (Throwable t) {
    //         t.printStackTrace();
    //     }
    //     return bshClass.staticInitializers;
    // }

    // public final static BshField[] getDeclaredFields(Class<?> generatedClass) {
    //     final BshClass bshClass = BshClass.storage.get(generatedClass);
    //     if (bshClass == null) throw new IllegalArgumentException("The given class isn't a generated class!");
    //     return bshClass.fields;
    // }

    // public final static BshConstructor[] getDeclaredConstructors(Class<?> generatedClass) {
    //     final BshClass bshClass = BshClass.storage.get(generatedClass);
    //     if (bshClass == null) throw new IllegalArgumentException("The given class isn't a generated class!");
    //     return bshClass.constructors;
    // }

    // public final static BshMethod[] getDeclaredMethods(Class<?> generatedClass) {
    //     final BshClass bshClass = BshClass.storage.get(generatedClass);
    //     if (bshClass == null) throw new IllegalArgumentException("The given class isn't a generated class!");
    //     return bshClass.methods;
    // }

    // private static class ByteClassLoader extends ClassLoader {
    //     public Class<?> classFromBytes(String className, byte[] classBytes) {
    //         return defineClass(className, classBytes, 0, classBytes.length);
    //     }
    // }

    // TODO: ver a possibilidade de fazer isso para tirar o bcm de BshClass
    // public byte[] toByteCode() {
    //     final byte[] byteCode = BshClassWritter.generateClassBytes(this);
    //     try { // TODO: esse método está sendo chamado 3 vezes ??????????????
    //         Files.write(Paths.get("/home/net0/git/beanshell-securityguard/" + UUID.randomUUID() + ".class"), byteCode);
    //     } catch (Throwable t) {}
    //     return byteCode;
    // }

    // TODO: impl it!
    // private WeakReference<Class<?>> classRef = new WeakReference<Class<?>>(null);
    private Class<?> _class; // Re-utilizar esse carinha para evitar ficar recarregando em memória ?
    public Class<?> toClass() {
        // TODO: see it later!
        final byte[] byteCode = BshClassWritter.generateClassBytes(this);
        try { // TODO: esse método está sendo chamado 3 vezes ??????????????
            Files.write(Paths.get("/home/net0/git/beanshell-securityguard/" + UUID.randomUUID() + ".class"), byteCode);
        } catch (Throwable t) {}
        // System.out.println("BshClass.toClass() -> bcm = " + bcm);
        this._class = bcm.defineClass(internalName, byteCode);
        BshClass.storage.put(_class, this);
        return this._class;
    }

    private int innerClassNum = 1;

    // TODO: terminar de ver isso
    // TODO: e os métodos gerados na classe de enum pai ? mantemos eles ?
    // TODO: validar assinatura e todas as parafernalhas dessa inner BshClass
    protected BshClass toInnerBshClass(BshEnumConstant bshEnumConstant) {
        final BshConstructor[] constructors = new BshConstructor[this.constructors.length];
        for (int i = 0; i < constructors.length; i++)
            constructors[i] = new BshConstructor(
                BshModifier.NO_MODIFIERS, // TODO: ver os mods
                this.constructors[i].parameters,
                this.constructors[i].genericExceptionTypes,
                true,
                this.constructors[i].parameters.length,
                (callStack, _class, thisArg, args) -> args,
                (callStack, _class, thisArg, args) -> {}
            );

        return new BshClass(
            BshModifier.NO_MODIFIERS, // TODO: ver os mods
            this.name + "$" + innerClassNum++,
            this.typeParameters,
            new BshLazyType(this.name, 0),
            new Type[0],
            new BshEnumConstant[0],
            bshEnumConstant.innerFields,
            constructors,
            bshEnumConstant.innerMethods,
            new BshClass.InitializerFunction[0],
            new BshClass.InitializerFunction[0],
            this.bcm
        );
    }

    // // TODO: fazer testes unitários validando os 4 métodos abaixas em vários cenários!
    // /** Called before the class be initialized */
    // public void beforeStaticInitialize() {
    //     this.finalFieldsSet = new HashSet<>();
    // }

    // /** Called after the class be completelly initialized */
    // public void afterStaticInitialize() {
    //     this.finalFieldsSet = null;
    // }

    // /** Called before the instance be initialized */
    // public void beforeInitialize(This _this) {
    //     this.thisFinalFieldsAlreadySet.put(_this, new HashSet<>());
    // }

    // /** Called after the instance be completelly initialized */
    // public void afterInitialize(This _this) {
    //     this.thisFinalFieldsAlreadySet.remove(_this);
    // }

    // protected boolean isInstance(Object obj) {
    //     return this._class.isInstance(obj);
    // }

    // // Note: it's an internal get field, it means that this just should be called by a method inside this class!
    // protected Object getField(Object thisArg, String name) {
    //     if (!this._class.isInstance(thisArg))
    //         throw new IllegalArgumentException();

    //     try {
    //         for (Field f : this.availableFields) {
    //             if (!Reflect.isStatic(f) && f.getName().equals(name)) {
    //                 if (!Reflect.isPublic(f)) f.setAccessible(true);
    //                 Object value = f.get(thisArg);
    //                 if (!Reflect.isPublic(f)) f.setAccessible(false);
    //                 return Primitive.wrap(value, f.getType());
    //             }
    //         }
    //     } catch (IllegalAccessException e) {}

    //     return null;
    // }

    // protected boolean setField(Object thisArg, String name, Object value) {
    //     if (!this._class.isInstance(thisArg))
    //         throw new IllegalArgumentException();

    //     try {
    //         for (Field f : this.availableFields) {
    //             if (!Reflect.isStatic(f) && f.getName().equals(name)) {
    //                 if (!Reflect.isPublic(f)) f.setAccessible(true);
    //                 f.set(thisArg, Primitive.unwrap(value));
    //                 if (!Reflect.isPublic(f)) f.setAccessible(false);
    //                 return true;
    //             }
    //         }
    //     } catch (IllegalAccessException e) {}

    //     return false;
    // }

    // protected Object invokeMethod(Object thisArg, String name, Object ...args) throws TargetError {
    //     if (!this._class.isInstance(thisArg))
    //         throw new IllegalArgumentException();

    //     Class<?>[] argsTypes = Types.getTypes(args);

    //     try {
    //         List<Method> methods = this.availableMethods.stream()
    //                                     .filter(m -> m.getName().equals(name))
    //                                     .collect(Collectors.toList());

    //         int match = Reflect.findMostSpecificMethodIndex(argsTypes, methods);
    //         if (match < 0) return null;

    //         Reflect.invokeObjectMethod(methods, name, argsTypes, declaringInterpreter, null, null)

    //         Method m = methods.get(match);
            
    //         if (!Reflect.isPublic(m)) m.setAccessible(true);
    //         Object result = m.invoke(thisArg, args); // TODO: testar se isso funciona bem com var-args!
    //         if (!Reflect.isPublic(m)) m.setAccessible(false);

    //         return Primitive.wrap(result, m.getReturnType());
    //     } catch (IllegalAccessException e) {
    //     } catch (InvocationTargetException te) {
    //         throw new TargetError("", te.getTargetException(), null, null, true);
    //     }

    //     return false;
    // }

    // public static void main(String[] args) {
    //     // Example to invoke the method in the wrapper:
    //     BshClass.getDeclaredMethods(null)[1].invoke(new Object[0]);

    //     // Example to set the default value of the field:
    //     BshClass.getDeclaredFields(null)[1].evalDefaultValue();
    // }

}
