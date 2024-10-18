package bsh.internals;

import java.lang.reflect.Constructor;
// import java.lang.reflect.Field;
// import java.lang.reflect.InvocationTargetException;
// import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

// TODO: fazer teste para verificar se uma classe declarada em um Interpreter é acesível em outro
// TODO: como ficam inner classes ? e.g., new ClassLoader() { void doSomething() { this.defineClass(); } } 
// TODO: add support for static blocks!
// TODO: validate duplicated fields when creating the Class<?>. Note: validate also for static and non-static!
// TODO: same validation of fields for methods but with signature validation!
// TODO: verificar esse comentário e se sim, fazer testes para isso -> interfaces still cannot have constructors, fields, private methods, or implementations of equals/hashCode/toString

public class BshClass {

    // TODO: make some test to verify memory leak!
    // - Note1: maybe a for loop where the iterations are calculated base on O.S. RAM and the size of bytes created by this class
    // - Note2: (test it) the Class<?> inside a ClassLoader is just garbage-collected when the ClassLoader itself is garbage collected!!!!!
    protected static final WeakHashMap<Class<?>, BshClass> storage = new WeakHashMap<>();

    protected final int modifiers;

    // protected final BshClass declaringClass = null; // Null if it's not an innerClass
    // protected final String packageName; // Package name
    // protected final String simpleName; // Simple class name
    protected final String name; // Full name
    protected final String internalName; // Internal name
    protected final String descriptor;

    protected final TypeVariable<?>[] typeParameters; // Declared generic types
    protected final Type superClass; // Super class being extended
    protected final Type[] interfaces; // Interfaces being implemented

    protected final BshField[] fields; // Declared fields of this class
    protected final BshConstructor[] constructors; // Declared constructors of this class
    protected final BshMethod[] methods; // Declared methods of this class

    protected final BshConsumer<CallStack>[] staticInitializers;
    protected final BshConsumer<CallStack>[] initializers;

    // TODO: How write innerClasses ? And inner classes of innerClasse ?
    // protected final BshClass[] innerClasses = new BshClass[0]; // Declared inner classes

    protected final Constructor<?>[] superConstructors;

    // Variables to execute methods
    protected final Interpreter declaringInterpreter;
    // TODO: não faz + sentido guardarmos só o 'declaringNameSpace' ? pq ter um nameSpace próprio para essa generated class ?
    protected final NameSpace nameSpace; // TODO: ver isso, faz sentido ? E o 'this' ? Onde fica, como chamar, pode setar valores além do normal dentro de constructors e methods ???
    protected final BshClassManager bcm;

    private boolean staticIniting = true;
    private final Set<String> staticFinalFieldsAlreadySet = new HashSet<>(); // TODO: dar um .clear() when 'constructing = false'

    // // TODO: substituir tlvz por uma referência para o BshClass ?
    // private final NameSpace declaringNameSpace;

    // private final List<Field> availableFields = new ArrayList<>();
    // private final List<Method> availableMethods = new ArrayList<>();

    public BshClass(int modifiers, String name, TypeVariable<?>[] typeParameters, Type superClass, Type[] interfaces, BshField[] fields, BshConstructor[] constructors, BshMethod[] methods, BshConsumer<CallStack>[] staticInitializers, BshConsumer<CallStack>[] initializers, Interpreter declaringInterpreter, NameSpace nameSpace, BshClassManager bcm) {
        this.modifiers = modifiers;
        this.name = name;
        this.internalName = name.replace('.', '/');
        this.descriptor = "L" + this.internalName + ";";
        this.typeParameters = typeParameters;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.fields = fields;
        this.constructors = constructors;
        this.methods = methods;
        this.staticInitializers = staticInitializers;
        this.initializers = initializers;
        this.superConstructors = Types.getRawType(superClass).getDeclaredConstructors(); // TODO: talvez os superConstructors deveriam ser lazy loaded ? Para possibilitar referências + dinâmicas de super classes ?
        this.declaringInterpreter = declaringInterpreter;
        this.nameSpace = nameSpace;
        this.bcm = bcm;

        for (BshField field: this.fields) field.setDeclaringClass(this);
        for (BshConstructor constructor: this.constructors) constructor.setDeclaringClass(this);
        for (BshMethod method: this.methods) method.setDeclaringClass(this);

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
        return Types.getASMClassSignature(this.typeParameters, this.superClass, this.interfaces);
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

    // protected static BshClass getBshClass(Class<?> _class) {
    //     return BshClass.storage.get(_class);
    // }

    // // Note: it should be called just by BshConstructor!
    // protected This createThis(Object thisArg) {

    // }

    private final WeakHashMap<Object, This> thisCache = new WeakHashMap<>();
    private final This createThis(Object thisArg) {
        final This _this = new This(thisArg, this.declaringInterpreter.getStrictJava());
        thisCache.put(thisArg, _this);
        return _this;
    }
    protected This getThis(Object thisArg) {
        final This _this = thisCache.get(thisArg);
        if (_this == null) throw new IllegalArgumentException("Invalid 'thisArg'!");
        return _this;
    }

    public static boolean canSetStaticFinalField(Class<?> _class, String fieldName) {
        final BshClass bshClass = BshClass.storage.get(_class);
        return bshClass != null && bshClass.staticIniting && bshClass.staticFinalFieldsAlreadySet.add(fieldName);
    }

    public static boolean canSetFinalField(Class<?> _class, Object thisArg, String fieldName) {
        if (!This.isObjectWrapper(thisArg)) return false;
        final BshClass bshClass = BshClass.storage.get(_class);
        return bshClass != null && BshConstructor.canSetFinalField((This) thisArg, fieldName);
    }

    // private final WeakHashMap<Object, List<String>>

    // TODO: fazer teste para o 'EvalError' do .staticInitialize() tb ?
    // Note: execute static blocks, initialize static fields and execute body-statement on non strictJava mode
    public static void staticInitialize(Class<?> generatedClass) throws EvalError {
        final BshClass bshClass = BshClass.storage.get(generatedClass);
        if (bshClass == null) throw new IllegalArgumentException("The given class isn't a generated class!");

        final NameSpace nameSpace = new NameSpace(bshClass.nameSpace, bshClass.name, bshClass);
        final CallStack callStack = new CallStack(nameSpace);

        bshClass.staticIniting = true;

        for (final BshConsumer<CallStack> staticInitializer: bshClass.staticInitializers)
            staticInitializer.consume(callStack);

        bshClass.staticIniting = false;
        bshClass.staticFinalFieldsAlreadySet.clear();
    }

    // TODO: fazer teste para o 'EvalError' do .initialize() tb ?
    public static <T> void initialize(Class<T> generatedClass, T thisArg) throws EvalError {
        final BshClass bshClass = BshClass.storage.get(generatedClass);
        if (bshClass == null) throw new IllegalArgumentException("The given class isn't a generated class!");

        final This _this = bshClass.createThis(thisArg);
        final NameSpace nameSpace = new NameSpace(bshClass.nameSpace, "static " + bshClass.name, bshClass, _this);
        final CallStack callStack = new CallStack(nameSpace);

        for (final BshConsumer<CallStack> initiliazer: bshClass.initializers)
            initiliazer.consume(callStack);
    }

    public static BshField[] getDeclaredFields(Class<?> generatedClass) {
        final BshClass bshClass = BshClass.storage.get(generatedClass);
        if (bshClass == null) throw new IllegalArgumentException("The given class isn't a generated class!");
        return bshClass.fields;
    }

    public static int getThisChainConstructorIndex(Class<?> generatedClass, Object[] args) {
        final BshClass bshClass = BshClass.storage.get(generatedClass);
        if (bshClass == null) throw new IllegalArgumentException("The given class isn't a generated class!");
        final Class<?>[] argsTypes = Reflect.getTypes(args);
        final List<BshConstructor> constructors = Arrays.asList(bshClass.constructors);
        return Reflect.findMostSpecificInvocableIndex(argsTypes, constructors, BshConstructor::getParameterTypes, BshConstructor::isVarArgs);
    }

    public static int getSuperChainConstructorIndex(Class<?> generatedClass, Object[] args) {
        BshClass bshClass = BshClass.storage.get(generatedClass);
        if (bshClass == null) throw new IllegalArgumentException("The given class isn't a generated class!");
        Class<?>[] argsTypes = Reflect.getTypes(args);
        List<Constructor<?>> constructors = Arrays.asList(bshClass.superConstructors);
        return Reflect.findMostSpecificInvocableIndex(argsTypes, constructors, Constructor::getParameterTypes, Constructor::isVarArgs);
    }

    public static BshConstructor[] getDeclaredConstructors(Class<?> generatedClass) {
        BshClass bshClass = BshClass.storage.get(generatedClass);
        if (bshClass == null) throw new IllegalArgumentException("The given class isn't a generated class!");
        return bshClass.constructors;
    }

    public static BshMethod[] getDeclaredMethods(Class<?> generatedClass) {
        BshClass bshClass = BshClass.storage.get(generatedClass);
        if (bshClass == null) throw new IllegalArgumentException("The given class isn't a generated class!");
        return bshClass.methods;
    }

    // private static class ByteClassLoader extends ClassLoader {
    //     public Class<?> classFromBytes(String className, byte[] classBytes) {
    //         return defineClass(className, classBytes, 0, classBytes.length);
    //     }
    // }

    // TODO: impl it!
    // private WeakReference<Class<?>> classRef = new WeakReference<Class<?>>(null);
    private Class<?> _class;
    public Class<?> toClass() {
        // TODO: see it later!
        final byte[] byteCode = BshClassWritter.generateClassBytes(this);
        // try {
        //     Files.write(Paths.get("/home/net0/git/beanshell-securityguard/test-class.class"), byteCode);
        // } catch (Throwable t) {}
        // System.out.println("BshClass.toClass() -> bcm = " + bcm);
        this._class = bcm.defineClass(internalName, byteCode);
        BshClass.storage.put(_class, this);
        return this._class;
    }

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
