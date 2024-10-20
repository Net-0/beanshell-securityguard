package bsh.internals;

import java.lang.reflect.Type;

import bsh.Primitive;
// import bsh.Types;
import bsh.org.objectweb.asm.ClassWriter;
import bsh.org.objectweb.asm.FieldVisitor;
import bsh.org.objectweb.asm.MethodVisitor;
import bsh.org.objectweb.asm.Opcodes;

// TODO: inner classes ( classes dentro de classes ) possuem hoist no standard Java enquanto local classes não!
// TODO: redo the entire doc!
/**
 * It's an util class that generate classes that extend functional interfaces
 * where the implementation is basically a wrapper of {@link BshLambda}
 */
class BshClassWritter {

    // // TODO: get the FunctionalInterface args names too!
    // /**
    //  * Return a new generated class that wraps a bshLambda. Example of a class that is generated:
    //  *
    //  * <p>
    //  *
    //  * <pre>{@code
    //  * import java.util.function.Function;
    //  *
    //  * public class MyClass<T, R> implements Function<T, R> {
    //  *  private BshLambda bshLambda;
    //  *
    //  *  public MyClass(BshLambda bshLambda) {
    //  *      this.bshLambda = bshLambda;
    //  *  }
    //  *
    //  *  public R apply(T arg1) {
    //  *      return this.bshLambda.invokeObject(new Object[] { arg1 }, new Class[0], Object.class);
    //  *  }
    //  * }
    //  * </pre>
    //  */
    // protected static <T> Class<T> generateClass(Class<T> functionalInterface) {
    // }

    /**
     * Return the bytes of a class that wraps a bshLambda. Example of a class that is generated:
     *
     * <p>
     *
     * <pre>
     * import java.util.function.Function;
     *
     * public class MyClass implements Function {
     *  private BshLambda bshLambda;
     *
     *  public MyClass(BshLambda bshLambda) {
     *      this.bshLambda = bshLambda;
     *  }
     *
     *  public Object apply(Object arg1) {
     *      return this.bshLambda.invokeObject(new Object[] { arg1 }, new Class[0], Object.class);
     *  }
     * }
     * </pre>
     */
    protected static byte[] generateClassBytes(BshClass bshClass) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        // System.out.println("--------------------------------------------------------------------------------------------------");
        // System.out.println("BshClassWritter.generateClassBytes() -> className = " + bshClass.name);
        // System.out.println("BshClassWritter.generateClassBytes() -> modifiersString = " + Modifier.toString(bshClass.modifiers));
        // System.out.println("BshClassWritter.generateClassBytes() -> modifiers = " + Integer.toBinaryString(bshClass.modifiers));
        // System.out.println("BshClassWritter.generateClassBytes() -> modifiers test 1 = " + BshModifier.toBinaryString(bshClass.modifiers));
        // System.out.println("BshClassWritter.generateClassBytes() -> modifiers test 2 = " + BshModifier.toBinaryString(BshModifier.METHOD_MODIFIERS));
        // System.out.println("--------------------------------------------------------------------------------------------------");

        // 1000000000

        cw.visit(Opcodes.V1_8, bshClass.modifiers, bshClass.internalName, bshClass.getSignature(), Types.getInternalName(bshClass.superClass), Types.getInternalNames(bshClass.interfaces));

        // System.out.println("BshClassWritter.generateClassBytes(): ");
        // System.out.println(" - bshClass.staticInitializers.length: " + bshClass.staticInitializers.length);

        // Write static initilizer
        if (bshClass.staticInitializers.length != 0)
            BshClassWritter.writeStaticInitializer(cw, bshClass);

        // Write the fields
        for (int fi = 0; fi < bshClass.fields.length; fi++)
            BshClassWritter.writeField(cw, bshClass, fi);

        // Write the constructors
        for (int ci = 0; ci < bshClass.constructors.length; ci++)
            BshClassWritter.writeConstructor(cw, bshClass, ci);

        // Write the methods
        for (int mi = 0; mi < bshClass.methods.length; mi++)
            BshClassWritter.writeMethod(cw, bshClass, mi);

        // ClassByteCodeGenerator.writeInnerClass(cw, innerBshClass);

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static void writeStaticInitializer(ClassWriter cw, BshClass bshClass) {
        // TODO: add var-ags signature
        MethodVisitor cv = cw.visitMethod(BshModifier.STATIC, "<clinit>", "()V", null, null);

        cv.visitCode();

        // Load in the operand stack the 'bsh.BshConstructor' to be invoked
        cv.visitLdcInsn(Types.getASMType(bshClass.descriptor));
        cv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/internals/BshClass", "staticInitialize", "(Ljava/lang/Class;)V", false);

        // Default end
        cv.visitInsn(Opcodes.RETURN); // Return void
        cv.visitMaxs(0, 0); // Set the stack sizes (obs.: the ClassWritter should compute it by itself)
        cv.visitEnd();
    }

    private static void writeField(ClassWriter cw, BshClass bshClass, int fieldIndex) {
        final BshField bshField = bshClass.fields[fieldIndex];

        Object defaultValue = null; // TODO: test for default values to static and non-static fields!
        FieldVisitor fv = cw.visitField(bshField.getModifiers(), bshField.getName(), bshField.getDescriptor(), bshField.getSignature(), defaultValue);
        fv.visitEnd();
    }

    /**
     * Just write the constructor in the ClassWriter. Example of a class with the constructor that is written with this method:
     *
     * <p>
     *
     * <pre>
     * public class MyClass {
     *  private BshLambda bshLambda;
     *
     *  public MyClass(BshLambda bshLambda) {
     *      this.bshLambda = bshLambda;
     *  }
     * }
     * </pre>
     */
    private static void writeConstructor(ClassWriter cw, BshClass bshClass, int constructorIndex) {
        final BshConstructor bshConstructor = bshClass.constructors[constructorIndex];
        // final Modifiers[] paramMods = bshConstructor.paramModifiers;
        // final Type[] paramTypes = bshConstructor.genericParamTypes;
        // final String[] paramNames = bshConstructor.paramNames;

        // TODO: add var-ags signature
        MethodVisitor cv = cw.visitMethod(bshConstructor.getModifiers(), "<init>", bshConstructor.getDescriptor(), bshConstructor.getSignature(), bshConstructor.getExceptionInternalNames());

        // Declare the parameters signature
        for (BshParameter param: bshConstructor.getParameters())
            cv.visitParameter(param.getName(), param.getModifiers());

        cv.visitCode();

        switch (bshConstructor.constructorChainKind) {
            case BshConstructor.NO_CHAIN:
                // Default begin: Call the superclass constructor 'super()''
                cv.visitVarInsn(Opcodes.ALOAD, 0); // Load 'this' onto the stack
                cv.visitMethodInsn(Opcodes.INVOKESPECIAL, Types.getInternalName(bshClass.superClass), "<init>", "()V", false);

                // Invoke the initializers for each constructor that calls the super() constructor
                cv.visitLdcInsn(Types.getASMType(bshClass.descriptor)); // Load in the operand stack the 'bsh.BshConstructor' to be invoked
                cv.visitVarInsn(Opcodes.ALOAD, 0); // Load 'this' onto the stack
                cv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/internals/BshClass", "initialize", "(Ljava/lang/Class;Ljava/lang/Object;)V", false);
                break;
            case BshConstructor.THIS_CHAIN:
                // TODO: impl it!
                break;
            case BshConstructor.SUPER_CHAIN:
                // TODO: impl it!
                break;
        }

        // // TODO: improve this!
        // if (bshConstructor.constructorChainKind == BshConstructor.NO_CHAIN) {
        //     // Default begin: Call the superclass constructor 'super()''
        //     cv.visitVarInsn(Opcodes.ALOAD, 0); // Load 'this' onto the stack
        //     cv.visitMethodInsn(Opcodes.INVOKESPECIAL, Types.getInternalName(bshClass.superClass), "<init>", "()V", false);
        // }

        // // Invoke the initializers for each constructor that calls the super() constructor
        // if (bshConstructor.constructorChainKind == BshConstructor.SUPER_CHAIN || bshConstructor.constructorChainKind == BshConstructor.NO_CHAIN) {
        //     cv.visitLdcInsn(Types.getASMType(bshClass.descriptor)); // Load in the operand stack the 'bsh.BshConstructor' to be invoked
        //     cv.visitVarInsn(Opcodes.ALOAD, 0); // Load 'this' onto the stack
        //     cv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/internals/BshClass", "initialize", "(Ljava/lang/Class;Ljava/lang/Object;)V", false);
        //     cv.visitLdcInsn(constructorIndex);
        //     cv.visitInsn(Opcodes.AALOAD); // Get the 'BshConstructor' from the returned BshConstructor[] in the index 'constructorIndex'
        // }

        // Load in the operand stack the 'bsh.BshConstructor' to be invoked
        cv.visitLdcInsn(Types.getASMType(bshClass.descriptor));
        cv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/internals/BshClass", "getDeclaredConstructors", "(Ljava/lang/Class;)[Lbsh/internals/BshConstructor;", false);
        cv.visitLdcInsn(constructorIndex);
        cv.visitInsn(Opcodes.AALOAD); // Get the 'BshConstructor' from the returned BshConstructor[] in the index 'constructorIndex'
        
        // Define the 'thisArg' to call 'bsh.BshConstructor.construct()'
        cv.visitVarInsn(Opcodes.ALOAD, 0); // Load 'this' into stack

        // Define and create the Object[] array to store the 'args'
        cv.visitLdcInsn(bshConstructor.getParameterCount()); // Size of the array
        cv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

        // Load the parameters inside the Object[]
        int paramLocalVarIndex = 1;
        int paramIndex = 0;
        for (final BshParameter param: bshConstructor.getParameters()) {
            final Type paramType = param.getGenericType();
            cv.visitInsn(Opcodes.DUP);
            cv.visitLdcInsn(paramIndex++); // Load the array index to set the value
            if (paramType == char.class) {
                // Load a char argument and already convert it to a Character
                cv.visitVarInsn(Opcodes.ILOAD, paramLocalVarIndex);
                cv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
            } else if (paramType == boolean.class) {
                // Load a boolean argument and already convert it to a Boolean
                cv.visitVarInsn(Opcodes.ILOAD, paramLocalVarIndex);
                cv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            } else if (paramType == byte.class) {
                // Load a byte argument and already convert it to a Byte
                cv.visitVarInsn(Opcodes.ILOAD, paramLocalVarIndex);
                cv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
            } else if (paramType == short.class) {
                // Load a short argument and already convert it to a Short
                cv.visitVarInsn(Opcodes.ILOAD, paramLocalVarIndex);
                cv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
            } else if (paramType == int.class) {
                // Load an int argument and already convert it to an Integer
                cv.visitVarInsn(Opcodes.ILOAD, paramLocalVarIndex);
                cv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            } else if (paramType == long.class) {
                // Load a long argument and already convert it to a Long
                cv.visitVarInsn(Opcodes.LLOAD, paramLocalVarIndex);
                cv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            } else if (paramType == float.class) {
                // Load a float argument and already convert it to a Float
                cv.visitVarInsn(Opcodes.FLOAD, paramLocalVarIndex);
                cv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
            } else if (paramType == double.class) {
                // Load a double argument and already convert it to a Double
                cv.visitVarInsn(Opcodes.DLOAD, paramLocalVarIndex);
                cv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            } else {
                // Load an object argument
                cv.visitVarInsn(Opcodes.ALOAD, paramLocalVarIndex);
            }
            cv.visitInsn(Opcodes.AASTORE);

            paramLocalVarIndex += paramType == long.class || paramType == double.class ? 2 : 1;
        }

        // // Call the 'BshMethod'
        cv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "bsh/internals/BshConstructor", "construct", "(Ljava/lang/Object;[Ljava/lang/Object;)V", false);


        // cv.visitTableSwitchInsn(paramIndex, paramIndex, null, null);
        // new Label().

        // Default end
        cv.visitInsn(Opcodes.RETURN); // Return void
        cv.visitMaxs(0, 0); // Set the stack sizes (obs.: the ClassWritter should compute it by itself)
        cv.visitEnd();
    }

    /**
     * Write the method to implement the Functional-Interface. Some examples:
     *
     * <p>First Example:</p>
     * <pre>
     * import java.util.function.Function;
     *
     * public class MyFunction implements Function {
     *  private BshLambda bshLamba;
     *
     *  public Object apply(Object arg1) {
     *      return this.bshLambda.invokeObject(new Object[] { arg1 }, new Class[0], Object.class);
     *  }
     * }
     * </pre>
     *
     * <p>Second Example:</p>
     * <pre>
     * import java.util.function.BooleanSupplier;
     *
     * public class MyBooleanSupplier implements BooleanSupplier {
     *  private BshLambda bshLamba;
     *
     *  public boolean getAsBoolean() {
     *      return this.bshLambda.invokeBoolean(new Object[0], new Class[0]);
     *  }
     * }
     * </pre>
     *
     * <p>Third Example:</p>
     * <pre>
     * import java.util.concurrent.Callable;
     *
     * public class MyCallable implements Callable {
     *  private BshLambda bshLamba;
     *
     *  public Object call() throws Exception {
     *      return this.bshLambda.invokeObject(new Object[0], new Class[] { Exception.class }, Object.class);
     *  }
     * }
     * </pre>
     *
     * <p>Fourth Example:</p>
     * <pre>
     * import java.lang.Runnable;
     *
     * public class MyRunnable implements Runnable {
     *  private BshLambda bshLamba;
     *
     *  public void run() {
     *      return this.bshLambda.invoke(new Object[0], new Class[0]);
     *  }
     * }
     * </pre>
     */
    private static void writeMethod(ClassWriter cw, BshClass bshClass, int methodIndex) {
        final BshMethod bshMethod = bshClass.methods[methodIndex];
        final Type returnType = bshMethod.getGenericReturnType();

        MethodVisitor mv = cw.visitMethod(bshMethod.getModifiers(), bshMethod.getName(), bshMethod.getDescriptor(), bshMethod.getSignature(), bshMethod.getExceptionInternalNames());

        // Declare the parameters signature
        for (final BshParameter param: bshMethod.getParameters())
            mv.visitParameter(param.getName(), param.getModifiers());

        // Abstract methods have no body
        if (bshMethod.isAbstract()) return;

        mv.visitCode();

        // Load in the operand stack the 'bsh.BshMethod' to be invoked
        mv.visitLdcInsn(Types.getASMType(bshClass.descriptor));
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/internals/BshClass", "getDeclaredMethods", "(Ljava/lang/Class;)[Lbsh/internals/BshMethod;", false);
        mv.visitLdcInsn(methodIndex);
        mv.visitInsn(Opcodes.AALOAD); // Get the 'BshMethod' from the returned BshMethod[] in the index 'methodIndex'

        // Define the 'thisArg' to call 'bsh.BshMethod.invoke()'
        if (!bshMethod.isStatic())
            mv.visitVarInsn(Opcodes.ALOAD, 0); // Load 'this' into stack
        // else
        //     mv.visitInsn(Opcodes.ACONST_NULL); // Load 'null' into stack

        // Define and create the Object[] array to store the 'args'
        mv.visitLdcInsn(bshMethod.getParameterCount()); // Size of the array
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

        // Load the parameters inside the Object[]
        int paramLocalVarIndex = bshMethod.isStatic() ? 0 : 1;
        int paramIndex = 0;
        for (final BshParameter param: bshMethod.getParameters()) {
            final Type paramType = param.getGenericType();
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(paramIndex++); // Load the array index to set the value
            if (paramType == char.class) {
                // Load a char argument and already convert it to a Character
                mv.visitVarInsn(Opcodes.ILOAD, paramLocalVarIndex);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
            } else if (paramType == boolean.class) {
                // Load a boolean argument and already convert it to a Boolean
                mv.visitVarInsn(Opcodes.ILOAD, paramLocalVarIndex);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            } else if (paramType == byte.class) {
                // Load a byte argument and already convert it to a Byte
                mv.visitVarInsn(Opcodes.ILOAD, paramLocalVarIndex);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
            } else if (paramType == short.class) {
                // Load a short argument and already convert it to a Short
                mv.visitVarInsn(Opcodes.ILOAD, paramLocalVarIndex);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
            } else if (paramType == int.class) {
                // Load an int argument and already convert it to an Integer
                mv.visitVarInsn(Opcodes.ILOAD, paramLocalVarIndex);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            } else if (paramType == long.class) {
                // Load a long argument and already convert it to a Long
                mv.visitVarInsn(Opcodes.LLOAD, paramLocalVarIndex);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            } else if (paramType == float.class) {
                // Load a float argument and already convert it to a Float
                mv.visitVarInsn(Opcodes.FLOAD, paramLocalVarIndex);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
            } else if (paramType == double.class) {
                // Load a double argument and already convert it to a Double
                mv.visitVarInsn(Opcodes.DLOAD, paramLocalVarIndex);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            } else {
                // Load an object argument
                mv.visitVarInsn(Opcodes.ALOAD, paramLocalVarIndex);
            }
            mv.visitInsn(Opcodes.AASTORE);

            paramLocalVarIndex += paramType == long.class || paramType == double.class ? 2 : 1;
        }

        final Class<?> rawReturnType = Types.getRawType(returnType);
        final Class<?> invokeRawReturnType = rawReturnType.isPrimitive() ? Primitive.boxType(rawReturnType) : rawReturnType;

        if (bshMethod.isStatic())
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "bsh/internals/BshMethod", "invoke", "([Ljava/lang/Object;)Ljava/lang/Object;", false);
        else
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "bsh/internals/BshMethod", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);

        if (rawReturnType != void.class)
            mv.visitTypeInsn(Opcodes.CHECKCAST, Types.getInternalName(invokeRawReturnType));

        if (rawReturnType == void.class) {
            mv.visitInsn(Opcodes.POP);
            mv.visitInsn(Opcodes.RETURN);
        } else if (rawReturnType == boolean.class) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
            mv.visitInsn(Opcodes.IRETURN);
        } else if (rawReturnType == char.class) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
            mv.visitInsn(Opcodes.IRETURN);
        } else if (rawReturnType == byte.class) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
            mv.visitInsn(Opcodes.IRETURN);
        } else if (rawReturnType == short.class) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
            mv.visitInsn(Opcodes.IRETURN);
        } else if (rawReturnType == int.class) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
            mv.visitInsn(Opcodes.IRETURN);
        } else if (rawReturnType == long.class) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
            mv.visitInsn(Opcodes.LRETURN);
        } else if (rawReturnType == float.class) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
            mv.visitInsn(Opcodes.FRETURN);
        } else if (rawReturnType == double.class) {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
            mv.visitInsn(Opcodes.DRETURN);
        } else {
            mv.visitInsn(Opcodes.ARETURN);
        }

        mv.visitMaxs(0, 0); // The Writter must calculate the values by itself.
        mv.visitEnd();
    }

}
