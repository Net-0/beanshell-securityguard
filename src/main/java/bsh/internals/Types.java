package bsh.internals;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

public class Types {
    
    static bsh.org.objectweb.asm.Type getASMType(Type type) {
        return bsh.org.objectweb.asm.Type.getType(getRawType(type));
    }

    static bsh.org.objectweb.asm.Type getASMType(Class<?> type) {
        return bsh.org.objectweb.asm.Type.getType(type);
    }

    static bsh.org.objectweb.asm.Type getASMType(String typeDescriptor) {
        return bsh.org.objectweb.asm.Type.getType(typeDescriptor);
    }

    static String getInternalName(Type type) {
        return bsh.org.objectweb.asm.Type.getInternalName(getRawType(type));
    }

    // TODO: review all 'Class<?>' params to methods, shouldn't they be 'java.lang.reflect.Type' ?
    static String[] getInternalNames(Type[] types) {
        final String[] internalNames = new String[types.length];
        for (int i = 0; i < types.length; i++) internalNames[i] = getInternalName(types[i]);
        return internalNames;
    }

    static String getDescriptor(Type type) {
        return bsh.org.objectweb.asm.Type.getDescriptor(getRawType(type));
    }

    // static String getMethodDescriptor(Method method) {
    //     return bsh.org.objectweb.asm.Type.getMethodDescriptor(method);
    // }

    static String getMethodDescriptor(Type returnType, Type  ...argumentTypes) {
        final bsh.org.objectweb.asm.Type _returnType = bsh.org.objectweb.asm.Type.getType(getRawType(returnType));
        final bsh.org.objectweb.asm.Type[] _argumentTypes = new bsh.org.objectweb.asm.Type[argumentTypes.length];
        for (int i = 0; i < argumentTypes.length; i++) _argumentTypes[i] = bsh.org.objectweb.asm.Type.getType(getRawType(argumentTypes[i]));
        return bsh.org.objectweb.asm.Type.getMethodDescriptor(_returnType, _argumentTypes);
    }

    // Helper method to convert Type to ASM style signature
    /** Return the signature of a specific type to be used in ASM bytecodes */
    static String getASMSignature(Type type) {
        if (type instanceof Class<?>) return bsh.org.objectweb.asm.Type.getDescriptor((Class<?>) type); // Handle a simple type ( like void or Integer )

        if (type instanceof ParameterizedType) { // Handle parameterized types (like List<T>)
            ParameterizedType paramType = (ParameterizedType) type;
            StringBuilder paramSignature = new StringBuilder();
            paramSignature.append("L" + bsh.org.objectweb.asm.Type.getInternalName((Class<?>) paramType.getRawType())); // Base type
            paramSignature.append("<");
            for (Type arg : paramType.getActualTypeArguments()) {
                paramSignature.append(getASMSignature(arg));
            }
            paramSignature.append(">;");
            return paramSignature.toString();
        }

        if (type instanceof TypeVariable<?>) // Handle type variables (like T or R)
            return "T" + ((TypeVariable<?>) type).getName() + ";";

        if (type instanceof WildcardType) { // Handle wildcards (like ? extends Number)
            WildcardType wildcard = (WildcardType) type;
            Type[] lowerBounds = wildcard.getLowerBounds();
            return lowerBounds.length > 0
                    ? "-" + bsh.org.objectweb.asm.Type.getDescriptor((Class<?>) lowerBounds[0])
                    : "+" + bsh.org.objectweb.asm.Type.getDescriptor((Class<?>) wildcard.getUpperBounds()[0]);
        }

        throw new IllegalArgumentException("Can't get the signature of this type because its Class is unknown: " + (type != null ? type.getClass() : null));
    }

    // TODO: teste para evitar q o 'extends' de interface tenha interfaces duplicadas
    // TODO: teste para evitar q o 'implements' de classes e enums tenham interfaces duplicadas
    // TODO: testes com enums, interfaces e classes
    static String getASMClassSignature(TypeVariable<?>[] types, Type superClass, Type ...interfaces) {
        StringBuilder signature = new StringBuilder();

        // 1. Extract type parameters (generics)
        if (types.length != 0) {
            signature.append("<");
            for (TypeVariable<?> typeParam : types) {
                signature.append(typeParam.getName()); // Add the type variable (e.g.: "T")

                for (Type bound : typeParam.getBounds()) // Add the bound of the type variable (e.g.: 'extends Number' => ":Ljava/lang/Number;")
                    signature.append(":").append(getASMSignature(bound));
            }
            signature.append(">");
        }

        // 2. Add the superclass in the signature
        signature.append(getASMSignature(superClass)); // All wrapper classes doesn't have a superclass, thus extends Object

        // 3. Add interfaces in the signature
        for (Type interface_: interfaces) signature.append(getASMSignature(interface_));

        return signature.toString();
    }

    // // TODO: testes com vários métodos
    // static String getASMMethodSignature(Method method) {
    //     return getASMMethodSignature(method.getTypeParameters(), method.getGenericParameterTypes(), method.getGenericReturnType(), method.getGenericExceptionTypes());
    // }

    static String getASMMethodSignature(TypeVariable<?>[] typeParams, Type[] params, Type returnType, Type[] exceptions) {
        StringBuilder signature = new StringBuilder();

        // 1. Handle generic type parameters (if any)
        if (typeParams.length > 0) {
            signature.append("<");
            for (TypeVariable<?> typeParam : typeParams) {
                signature.append(typeParam.getName()); // Add type variable (e.g., "T")
                for (Type bound : typeParam.getBounds())
                    signature.append(":").append(getASMSignature(bound)); // Add the bound
            }
            signature.append(">");
        }

        // 2. Add the method parameter types
        signature.append("(");
        for (Type param : params) signature.append(getASMSignature(param));
        signature.append(")");

        // 3. Add the return type
        signature.append(getASMSignature(returnType));

        // 4. Add the exception types (if any)
        for (Type exceptionType : exceptions) signature.append("^").append(getASMSignature(exceptionType));

        return signature.toString();
    }

    static Class<?>[] getRawType(final Type[] types) {
        final Class<?>[] rawTypes = new Class[types.length];
        for (int i = 0; i < types.length; i++)
            rawTypes[i] = Types.getRawType(types[i]);
        return rawTypes;
    }

    static Class<?> getRawType(Type type) {
        if (type == null) return null;
        if (type instanceof Class<?>) return (Class<?>) type; // Handle a simple type ( like void or Integer )

        if (type instanceof ParameterizedType) { // Handle parameterized types (like List<T>)
            ParameterizedType paramType = (ParameterizedType) type;
            return getRawType(paramType.getRawType());
        }

        // TODO: make a unit tests for it, it's always the first bound!!!!
        if (type instanceof TypeVariable<?>) { // Handle type variables (like T or R)
            TypeVariable<?> typeVar = ((TypeVariable<?>) type);
            return getRawType(typeVar.getBounds()[0]);
        }

        // TODO: make unit test for it too!
        if (type instanceof WildcardType) { // Handle wildcards (like ? extends Number)
            WildcardType wildcard = (WildcardType) type;
            Type bound = wildcard.getLowerBounds().length > 0 ? wildcard.getLowerBounds()[0] : wildcard.getUpperBounds()[0];
            return getRawType(bound);
        }

        throw new IllegalArgumentException("Can't raw type of this type because its Class is unknown: " + (type != null ? type.getClass() : null));
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

    // // if (type instanceof TypeVariable<?>) // Handle type variables (like T or R)
    // //     return "T" + ((TypeVariable<?>) type).getName() + ";";
    // static TypeVariable<?> createTypeVariable(String name, Type[] bounds) {
    //     // TODO: see it!
    //     // bounds = bounds == null || bounds.length == 0 ? new Type[] { Object.class } : bounds;

    //     return new TypeVariable<GenericDeclaration>() {
    //         public String getName() { return name; }
    //         public <T extends Annotation> T getAnnotation(Class<T> annotationClass) { return null; }
    //         public Annotation[] getAnnotations() { return new Annotation[0]; }
    //         public Annotation[] getDeclaredAnnotations() { return new Annotation[0]; }
    //         public AnnotatedType[] getAnnotatedBounds() { throw new UnsupportedOperationException("Unimplemented method 'getAnnotatedBounds'"); }
    //         public Type[] getBounds() { return bounds; }
    //         public GenericDeclaration getGenericDeclaration() { throw new UnsupportedOperationException("Unimplemented method 'getGenericDeclaration'"); };
    //     };
    // }
    // static ParameterizedType createParameterizedType(Type rawType, Type[] typeArguments) {
    //     return new ParameterizedType() {
    //         public Type[] getActualTypeArguments() { return typeArguments; }
    //         public Type getRawType() { return rawType; }
    //         public Type getOwnerType() { return null; }
    //     };
    // }

    // if (type instanceof WildcardType) { // Handle wildcards (like ? extends Number)
    //     WildcardType wildcard = (WildcardType) type;
    //     Type[] lowerBounds = wildcard.getLowerBounds();
    //     return lowerBounds.length > 0
    //             ? "-" + bsh.org.objectweb.asm.Type.getDescriptor((Class<?>) lowerBounds[0])
    //             : "+" + bsh.org.objectweb.asm.Type.getDescriptor((Class<?>) wildcard.getUpperBounds()[0]);
    // }

}
