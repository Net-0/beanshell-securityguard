package bsh.internals;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import bsh.internals.type.BshLazyType;

public class Types {

    static boolean isPrimitive(Type type) {
        return type == boolean.class ||
               type == byte.class ||
               type == char.class ||
               type == short.class ||
               type == int.class ||
               type == long.class ||
               type == float.class ||
               type == double.class ||
               type == void.class;
    }

    // TODO: arrancar a dependência de bsh.org.objectweb.asm.Type
    
    // static bsh.org.objectweb.asm.Type getASMType(Type type) {
    //     return bsh.org.objectweb.asm.Type.getType(Types.getDescriptor(type));
    //     // return bsh.org.objectweb.asm.Type.getType(getRawType(type));
    // }

    // static bsh.org.objectweb.asm.Type getASMType(Class<?> type) {
    //     return bsh.org.objectweb.asm.Type.getType(type);
    // }

    static bsh.org.objectweb.asm.Type getASMType(String typeDescriptor) {
        return bsh.org.objectweb.asm.Type.getType(typeDescriptor);
    }

    static String getInternalName(Type type) {
        // return bsh.org.objectweb.asm.Type.getInternalName(getRawType(type));
        if (type instanceof Class<?>)
            return ((Class<?>) type).getName().replace('.', '/');

        if (type instanceof BshLazyType)
            return ((BshLazyType) type).getInternalName();
            // return 'L' + type.getTypeName().replace('.', '/') + ';';

        if (type instanceof ParameterizedType) // Handle parameterized types (like List<T>)
            return Types.getInternalName(((ParameterizedType) type).getRawType());

        // TODO: faz sentido puxar internal-name disso ?
        // // TODO: make a unit tests for it, it's always the first bound!!!!
        // if (type instanceof TypeVariable<?>) // Handle type variables (like T or R)
        //     return Types.getInternalName(((TypeVariable<?>) type).getBounds()[0]);

        // TODO: faz sentido puxar internal-name disso ?
        // // TODO: make unit test for it too!
        // if (type instanceof WildcardType) { // Handle wildcards (like ? extends Number)
        //     final WildcardType wildcard = (WildcardType) type;
        //     final Type bound = wildcard.getLowerBounds().length > 0 ? wildcard.getLowerBounds()[0] : wildcard.getUpperBounds()[0];
        //     return Types.getDescriptor(bound);
        // }

        throw new IllegalArgumentException("Can't resolve the raw type of this type because its Class is unknown: " + (type != null ? type.getClass() : null));
    }

    // static String getInternalName(Class<?> _class) {
    //     // return bsh.org.objectweb.asm.Type.getInternalName(type);
    //     return _class.getName().replace('.', '/');
    // }

    // TODO: review all 'Class<?>' params to methods, shouldn't they be 'java.lang.reflect.Type' ?
    static String[] getInternalNames(Type[] types) {
        final String[] internalNames = new String[types.length];
        for (int i = 0; i < types.length; i++)
            internalNames[i] = Types.getInternalName(types[i]);
        return internalNames;
    }

    // static String getDescriptor(Type type) {
    //     return bsh.org.objectweb.asm.Type.getDescriptor(getRawType(type));
    // }

    // static String getDescriptor(Type type) {
    //     // if (type == null) return null;
    //     if (type == null) throw new NullPointerException(""); // TODO: see its message!

    //     if (type instanceof Class<?>) // Handle a simple type ( like void or Integer )
    //         return bsh.org.objectweb.asm.Type.getDescriptor((Class<?>) type);

    //     if (type instanceof ParameterizedType) { // Handle parameterized types (like List<T>)
    //         ParameterizedType paramType = (ParameterizedType) type;
    //         return getDescriptor(paramType.getRawType());
    //     }

    //     // TODO: make a unit tests for it, it's always the first bound!!!!
    //     if (type instanceof TypeVariable<?>) { // Handle type variables (like T or R)
    //         TypeVariable<?> typeVar = ((TypeVariable<?>) type);
    //         return getDescriptor(typeVar.getBounds()[0]);
    //     }

    //     // TODO: make unit test for it too!
    //     if (type instanceof WildcardType) { // Handle wildcards (like ? extends Number)
    //         WildcardType wildcard = (WildcardType) type;
    //         Type bound = wildcard.getLowerBounds().length > 0 ? wildcard.getLowerBounds()[0] : wildcard.getUpperBounds()[0];
    //         return getDescriptor(bound);
    //     }

    //     throw new IllegalArgumentException("Can't resolve the raw type of this type because its Class is unknown: " + (type != null ? type.getClass() : null));
    // }

    static String getDescriptor(Class<?> _class) {
        // final Class<?> _class = (Class<?>) type;
        // _class.isArray()
        final StringBuilder sb = new StringBuilder();
        for (; _class.isArray(); _class = _class.getComponentType())
            sb.append('[');

        if (_class == int.class)
            sb.append('I');
        else if (_class == void.class)
            sb.append('V');
        else if (_class == boolean.class)
            sb.append('Z');
        else if (_class == byte.class)
            sb.append('B');
        else if (_class == char.class)
            sb.append('C');
        else if (_class == short.class)
            sb.append('S');
        else if (_class == double.class)
            sb.append('D');
        else if (_class == float.class)
            sb.append('F');
        else if (_class == long.class)
            sb.append('J');
        else
            sb.append('L').append(_class.getName().replace('.', '/')).append(';');

        return sb.toString();
    }

    static String getDescriptor(Type type) {
        // if (type == null) return null;
        // if (type == null) throw new NullPointerException(""); // TODO: see its message!
        if (type instanceof Class<?>) // Handle a simple type ( like void or Integer )
            // return bsh.org.objectweb.asm.Type.getDescriptor((Class<?>) type);
            return Types.getDescriptor((Class<?>) type);

        if (type instanceof BshLazyType)
            return ((BshLazyType) type).getDescriptor();
            // return 'L' + type.getTypeName().replace('.', '/') + ';';

        if (type instanceof ParameterizedType) // Handle parameterized types (like List<T>)
            return Types.getDescriptor(((ParameterizedType) type).getRawType());

        // TODO: make a unit tests for it, it's always the first bound!!!!
        if (type instanceof TypeVariable<?>) // Handle type variables (like T or R)
            return Types.getDescriptor(((TypeVariable<?>) type).getBounds()[0]);

        // TODO: make unit test for it too!
        if (type instanceof WildcardType) { // Handle wildcards (like ? extends Number)
            final WildcardType wildcard = (WildcardType) type;
            final Type bound = wildcard.getLowerBounds().length > 0 ? wildcard.getLowerBounds()[0] : wildcard.getUpperBounds()[0];
            return Types.getDescriptor(bound);
        }

        throw new IllegalArgumentException("Can't resolve the raw type of this type because its Class is unknown: " + (type != null ? type.getClass() : null));
    }

    // static String getMethodDescriptor(Method method) {
    //     return bsh.org.objectweb.asm.Type.getMethodDescriptor(method);
    // }

    static String getMethodDescriptor(Type returnType, Type  ...parametersTypes) {
        // final bsh.org.objectweb.asm.Type _returnType = bsh.org.objectweb.asm.Type.getType(getRawType(returnType));
        // final bsh.org.objectweb.asm.Type[] _argumentTypes = new bsh.org.objectweb.asm.Type[argumentTypes.length];
        // for (int i = 0; i < argumentTypes.length; i++) _argumentTypes[i] = bsh.org.objectweb.asm.Type.getType(getRawType(argumentTypes[i]));
        // return bsh.org.objectweb.asm.Type.getMethodDescriptor(_returnType, _argumentTypes);

        final StringBuilder sb = new StringBuilder();

        // 1. Add the method parameter types
        sb.append("(");
        for (Type param : parametersTypes)
            sb.append(Types.getDescriptor(param));
        sb.append(")");

        // 2. Add the return type
        sb.append(Types.getDescriptor(returnType));

        return sb.toString();
    }

    // static String getMethodDescriptor(Type returnType, Class<?>  ...argumentTypes) {
    //     final bsh.org.objectweb.asm.Type _returnType = bsh.org.objectweb.asm.Type.getType(getRawType(returnType));
    //     final bsh.org.objectweb.asm.Type[] _argumentTypes = new bsh.org.objectweb.asm.Type[argumentTypes.length];
    //     for (int i = 0; i < argumentTypes.length; i++) _argumentTypes[i] = bsh.org.objectweb.asm.Type.getType(argumentTypes[i]);
    //     return bsh.org.objectweb.asm.Type.getMethodDescriptor(_returnType, _argumentTypes);
    // }

    // Helper method to convert Type to ASM style signature
    /** Return the signature of a specific type to be used in ASM bytecodes */
    static String getSignature(Type type) {
        if (type instanceof Class<?> || type instanceof BshLazyType) // Handle a simple type ( like void or Integer )
            return Types.getDescriptor(type);

        // if (type instanceof BshLazyType)
        //     return 'L' + type.getTypeName().replace('.', '/') + ';';

        if (type instanceof ParameterizedType) { // Handle parameterized types (like List<T>)
            ParameterizedType paramType = (ParameterizedType) type;
            final StringBuilder sb = new StringBuilder();
            sb.append("L" + Types.getInternalName((Class<?>) paramType.getRawType())); // Base type
            sb.append("<");
            for (Type arg : paramType.getActualTypeArguments())
                sb.append(getSignature(arg));
            sb.append(">;");
            return sb.toString();
        }

        if (type instanceof TypeVariable<?>) // Handle type variables (like T or R)
            return "T" + ((TypeVariable<?>) type).getName() + ";";

        if (type instanceof WildcardType) { // Handle wildcards (like ? extends Number)
            WildcardType wildcard = (WildcardType) type;
            Type[] lowerBounds = wildcard.getLowerBounds();
            // TODO: não deveria ser usado um getASMSignature() ao invés do getDescriptor() ? Pois assim n vai manter a assinatura de ParameterizedType!
            return lowerBounds.length > 0
                    ? "-" + Types.getDescriptor((Class<?>) lowerBounds[0])
                    : "+" + Types.getDescriptor((Class<?>) wildcard.getUpperBounds()[0]);
        }

        throw new IllegalArgumentException("Can't get the signature of this type because its Class is unknown: " + (type != null ? type.getClass() : null));
    }

    // TODO: teste para evitar q o 'extends' de interface tenha interfaces duplicadas
    // TODO: teste para evitar q o 'implements' de classes e enums tenham interfaces duplicadas
    // TODO: testes com enums, interfaces e classes
    static String getClassSignature(TypeVariable<?>[] types, Type superClass, Type ...interfaces) {
        final StringBuilder sb = new StringBuilder();

        // 1. Extract type parameters (generics)
        if (types.length != 0) {
            sb.append("<");
            for (TypeVariable<?> typeParam : types) {
                sb.append(typeParam.getName()); // Add the type variable (e.g.: "T")

                for (Type bound : typeParam.getBounds()) // Add the bound of the type variable (e.g.: 'extends Number' => ":Ljava/lang/Number;")
                    sb.append(":").append(getSignature(bound));
            }
            sb.append(">");
        }

        // 2. Add the superclass in the signature
        sb.append(getSignature(superClass)); // All wrapper classes doesn't have a superclass, thus extends Object

        // 3. Add interfaces in the signature
        for (Type interface_: interfaces)
            sb.append(getSignature(interface_));

        return sb.toString();
    }

    // // TODO: testes com vários métodos
    // static String getASMMethodSignature(Method method) {
    //     return getASMMethodSignature(method.getTypeParameters(), method.getGenericParameterTypes(), method.getGenericReturnType(), method.getGenericExceptionTypes());
    // }

    // TODO: rever esse método
    static String getMethodSignature(TypeVariable<?>[] typeParams, Type[] params, Type returnType, Type[] exceptions) {
        final StringBuilder sb = new StringBuilder();

        // 1. Handle generic type parameters (if any)
        if (typeParams.length > 0) {
            sb.append("<");
            for (TypeVariable<?> typeParam : typeParams) {
                sb.append(typeParam.getName()); // Add type variable (e.g., "T")
                for (Type bound : typeParam.getBounds())
                    sb.append(":").append(getSignature(bound)); // Add the bound
            }
            sb.append(">");
        }

        // 2. Add the method parameter types
        sb.append("(");
        for (Type param : params) sb.append(getSignature(param));
        sb.append(")");

        // 3. Add the return type
        sb.append(getSignature(returnType));

        // 4. Add the exception types (if any)
        for (Type exceptionType : exceptions) sb.append("^").append(getSignature(exceptionType));

        return sb.toString();
    }

    // TODO: mudar o nome do método para 'getRawTypes' ?
    public static Class<?>[] getRawType(final Type[] types) {
        final Class<?>[] rawTypes = new Class[types.length];
        for (int i = 0; i < types.length; i++)
            rawTypes[i] = Types.getRawType(types[i]);
        return rawTypes;
    }

    // TODO: ver o getRawType para o BshLazyType
    public static Class<?> getRawType(Type type) {
        if (type == null) return null;
        if (type instanceof Class<?>) return (Class<?>) type; // Handle a simple type ( like void or Integer )

        // TODO: ver o getRawType para o BshLazyType

        if (type instanceof ParameterizedType) // Handle parameterized types (like List<T>)
            return Types.getRawType(((ParameterizedType) type).getRawType());

        // TODO: make a unit tests for it, it's always the first bound!!!!
        if (type instanceof TypeVariable<?>) // Handle type variables (like T or R)
            return Types.getRawType(((TypeVariable<?>) type).getBounds()[0]);

        // TODO: make unit test for it too!
        if (type instanceof WildcardType) { // Handle wildcards (like ? extends Number)
            WildcardType wildcard = (WildcardType) type;
            Type bound = wildcard.getLowerBounds().length > 0 ? wildcard.getLowerBounds()[0] : wildcard.getUpperBounds()[0];
            return Types.getRawType(bound);
        }

        throw new IllegalArgumentException("Can't resolve the raw type of this type because its Class is unknown: " + (type != null ? type.getClass() : null));
    }

    // if (type instanceof ParameterizedType) { // Handle parameterized types (like List<T>)
    //     ParameterizedType paramType = (ParameterizedType) type;
    //     StringBuilder paramSignature = new StringBuilder();
    //     paramSignature.append("L" + bsh.org.objectweb.asm.Type.getInternalName((Class<?>) paramType.getRawType())); // Base type
    //     paramSignature.append("<");
    //     for (Type arg : paramType.getActualTypeArguments()) {
    //         paramSignature.append(getASMSignature(arg));
    //     }
    //     paramSignature.append(">;");
    //     return paramSignature.toString();
    // }

    // if (type instanceof WildcardType) { // Handle wildcards (like ? extends Number)
    //     WildcardType wildcard = (WildcardType) type;
    //     Type[] lowerBounds = wildcard.getLowerBounds();
    //     return lowerBounds.length > 0
    //             ? "-" + bsh.org.objectweb.asm.Type.getDescriptor((Class<?>) lowerBounds[0])
    //             : "+" + bsh.org.objectweb.asm.Type.getDescriptor((Class<?>) wildcard.getUpperBounds()[0]);
    // }

}
