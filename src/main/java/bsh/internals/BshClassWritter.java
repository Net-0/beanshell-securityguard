package bsh.internals;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import bsh.Primitive;
import bsh.Reflect;
// import bsh.Types;
import bsh.org.objectweb.asm.ClassWriter;
import bsh.org.objectweb.asm.FieldVisitor;
import bsh.org.objectweb.asm.Handle;
import bsh.org.objectweb.asm.Label;
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
        final boolean isEnum = BshModifier.isEnum(bshClass.modifiers);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        // System.out.println("--------------------------------------------------------------------------------------------------");
        // System.out.println("BshClassWritter.generateClassBytes() -> className = " + bshClass.name);
        // System.out.println("BshClassWritter.generateClassBytes() -> modifiersString = " + Modifier.toString(bshClass.modifiers));
        // System.out.println("BshClassWritter.generateClassBytes() -> modifiers = " + Integer.toBinaryString(bshClass.modifiers));
        // System.out.println("BshClassWritter.generateClassBytes() -> modifiers test 1 = " + BshModifier.toBinaryString(bshClass.modifiers));
        // System.out.println("BshClassWritter.generateClassBytes() -> modifiers test 2 = " + BshModifier.toBinaryString(BshModifier.METHOD_MODIFIERS));
        // System.out.println("--------------------------------------------------------------------------------------------------");

        cw.visit(Opcodes.V1_8, bshClass.modifiers, bshClass.internalName, bshClass.getSignature(), bshClass.superInternalName, Types.getInternalNames(bshClass.interfaces));

        // System.out.println("BshClassWritter.generateClassBytes(): ");
        // System.out.println(" - bshClass.staticInitializers.length: " + bshClass.staticInitializers.length);

        // TODO: ver os staticInitializers que são menores
        // Write static initilizer
        // if (bshClass.staticInitializers.length != 0)
        //     BshClassWritter.writeStaticInitializer(cw, bshClass);

        // // Write the initializer
        // if (bshClass.initializers.length != 0)
        //     BshClassWritter.writeInitializer(cw, bshClass);

        // Write the enum constants
        for (int eci = 0; eci < bshClass.enumConstants.length; eci++)
            BshClassWritter.writeEnumConstant(cw, bshClass, eci);

        // TODO: validar o static initializer para fields tb ??
        // Write the static initializer
        // if (bshClass.staticInitializers.length != 0 || bshClass.enumConstants.length != 0)
        BshClassWritter.writeStaticInitializer(cw, bshClass);

        // Write the fields
        for (int fi = 0; fi < bshClass.fields.length; fi++)
            BshClassWritter.writeField(cw, bshClass, fi);

        // Write the constructors
        for (int ci = 0; ci < bshClass.constructors.length; ci++)
            BshClassWritter.writeConstructor(cw, bshClass, ci);

        final String valueOf_descriptor = "(Ljava/lang/String;)" + bshClass.descriptor;
        final String values_descriptor = "()[" + bshClass.descriptor;

        // Write the methods
        for (int mi = 0; mi < bshClass.methods.length; mi++) {
            final BshMethod bshMethod = bshClass.methods[mi];

            // TODO: BshClass tem q ser um Type
            if (isEnum && bshMethod.name.equals("values") && bshMethod.descriptor.equals(values_descriptor))
                BshClassWritter.writeEnumMethodValues(cw, bshClass);
            else if (isEnum && bshMethod.name.equals("valueOf") && bshMethod.descriptor.equals(valueOf_descriptor))
                BshClassWritter.writeEnumMethodValueOf(cw, bshClass);
            else
                BshClassWritter.writeMethod(cw, bshClass, mi);
        }

        // ClassByteCodeGenerator.writeInnerClass(cw, innerBshClass);

        cw.visitEnd();
        return cw.toByteArray();
    }

    // // TODO: add var-ags signature
    // private static void writeInitializer(ClassWriter cw, BshClass bshClass) {
    //     MethodVisitor cv = cw.visitMethod(BshModifier.PRIVATE, "<init>", "()V", null, null);

    //     cv.visitCode();

    //     for (int i = 0; i < bshClass.initializers.length; i++) {
    //         final BshInitializer initializer = bshClass.initializers[i];

    //         // Load in the operand stack the 'bsh.internals.BshMethod' to be invoked
    //         cv.visitLdcInsn(Types.getASMType(bshClass.descriptor));
    //         cv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/internals/BshClass", "getDeclaredInitializers", "(Ljava/lang/Class;)[Lbsh/internals/BshInitializer;", false);
    //         cv.visitLdcInsn(i);
    //         cv.visitInsn(Opcodes.AALOAD); // Get the 'BshMethod' from the returned BshMethod[] in the index 'methodIndex'

    //         // Call bsh.internals.BshInitializer.initialize()
    //         cv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "bsh/internals/BshInitializer", "initialize", "()Ljava/lang/Object;", false);

    //         if (initializer.field != null)
    //             // Set the static field
    //             cv.visitFieldInsn(Opcodes.PUTSTATIC, bshClass.internalName, initializer.field.getName(), initializer.field.getDescriptor());
    //         else
    //             // Just remove the value from the operand stack
    //             cv.visitInsn(Opcodes.POP);
    //     }

    //     // Default end
    //     cv.visitInsn(Opcodes.RETURN); // Return void
    //     cv.visitMaxs(0, 0); // Set the stack sizes (obs.: the ClassWritter should compute it by itself)
    //     cv.visitEnd();
    // }

    private static void writeStaticInitializer(ClassWriter cw, BshClass bshClass) {
        // TODO: add var-ags signature
        MethodVisitor cv = cw.visitMethod(BshModifier.STATIC, "<clinit>", "()V", null, null);

        cv.visitCode();

        // final boolean haveSomethingToInitialize = bshClass.enumConstants.length != 0 || bshClass.staticInitializers.length != 0;
        // if (!haveSomethingToInitialize) {
        //     // Default end
        //     cv.visitInsn(Opcodes.RETURN); // Return void
        //     cv.visitMaxs(0, 0); // Set the stack sizes (obs.: the ClassWritter should compute it by itself)
        //     cv.visitEnd();
        // }

        final int bshClassVarIndex = 0;
        final int enumConstantInstaceVarIndex = 1;
        final int enumConstructorArgsVarIndex = 2;
        final int enumConstructorArgsTypesVarIndex = 3;

        // Load 'bsh.internals.BshClass' as a local variable
        cv.visitLdcInsn(Types.getASMType(bshClass.descriptor));
        cv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/internals/BshClass", "fromGeneratedClass", "(Ljava/lang/Class;)Lbsh/internals/BshClass;", false);
        cv.visitVarInsn(Opcodes.ASTORE, bshClassVarIndex);

        // Write the enum constant initializers
        for (int eci = 0; eci < bshClass.enumConstants.length; eci++) {
            final BshEnumConstant bshEnumConstant = bshClass.enumConstants[eci];

            // Create the instance for this constructor
            cv.visitTypeInsn(Opcodes.NEW, bshClass.internalName);
            cv.visitVarInsn(Opcodes.ASTORE, enumConstantInstaceVarIndex);

            // TODO: fazer Opcodes.DUP para evitar variáveis ?
            cv.visitVarInsn(Opcodes.ALOAD, bshClassVarIndex); // Load the 'bsh.internals.BshClass' for the 'bsh.internals.BshClass.findConstructorIndex()' call

            cv.visitVarInsn(Opcodes.ALOAD, bshClassVarIndex); // Load the 'bsh.internals.BshClass' for the first call
            cv.visitFieldInsn(Opcodes.GETFIELD, "bsh/internals/BshClass", "enumConstants", "[Lbsh/internals/BshEnumConstant;");
            cv.visitLdcInsn(eci);
            cv.visitInsn(Opcodes.AALOAD); // Get the 'BshEnumConstant' instance
            cv.visitFieldInsn(Opcodes.GETFIELD, "bsh/internals/BshEnumConstant", "argsSupplier", "Lbsh/internals/BshEnumConstant$ArgsSupplier;");

            // Call ...
            cv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "bsh/internals/BshEnumConstant$ArgsSupplier", "apply", "()[Ljava/lang/Object;", true);
            cv.visitVarInsn(Opcodes.ASTORE, enumConstructorArgsVarIndex);

            // Call Reflect.getTypes() to get the types of the arguments and store it as a local variable
            cv.visitVarInsn(Opcodes.ALOAD, enumConstructorArgsVarIndex);
            cv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/Reflect", "getTypes", "([Ljava/lang/Object;)[Ljava/lang/Class;", false);
            cv.visitVarInsn(Opcodes.ASTORE, enumConstructorArgsTypesVarIndex);

            // Call the Reflect.findMostSpecificInvocableIndex() to get the desired constructor index
            cv.visitVarInsn(Opcodes.ALOAD, enumConstructorArgsTypesVarIndex); // Load the types of the arguments
            // Load the constructors of the enum
            cv.visitVarInsn(Opcodes.ALOAD, bshClassVarIndex);
            cv.visitFieldInsn(Opcodes.GETFIELD, "bsh/internals/BshClass", "constructors", "[Lbsh/internals/BshConstructor;");
            cv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/Reflect", "findMostSpecificInvocableIndex", "([Ljava/lang/Class;[Lbsh/internals/BshConstructor;)I", false);

            // int index = Reflect.findMostSpecificInvocableIndex(argsTypes, this.constructors);

            // final Class<?>[] argsTypes = Reflect.getTypes(args);

            // cv.visitVarInsn(Opcodes.ALOAD, enumConstructorArgsVarIndex);
            // cv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "bsh/internals/BshClass", "findConstructorIndex", "([Ljava/lang/Object;)I", false);

            // cv.visitInsn(Opcodes.POP);
            // Call the constructor
            ASMHelper.writeSwtichCaseToCallConstructor(cv, enumConstantInstaceVarIndex, enumConstructorArgsVarIndex, enumConstructorArgsTypesVarIndex, bshEnumConstant.argsLength, bshClass.internalName, true, bshClass.constructors, BshConstructor::getParameterTypes, BshConstructor::isVarArgs);

            // Store the enum instance
            cv.visitVarInsn(Opcodes.ALOAD, enumConstantInstaceVarIndex);
            cv.visitFieldInsn(Opcodes.PUTSTATIC, bshClass.internalName, bshEnumConstant.name, bshClass.descriptor);
        }

        // // Write the static fields initializers
        // for (int fi = 0; fi < bshClass.fields.length; fi++) {
        //     final BshField bshField = bshClass.fields[fi];
        //     if (!bshField.isStatic()) continue;

        //     cv.visitVarInsn(Opcodes.ALOAD, bshClassVarIndex);
        //     cv.visitFieldInsn(Opcodes.GETFIELD, "bsh/internals/BshClass", "fields", "[Lbsh/internals/BshField;");
        //     cv.visitLdcInsn(fi);
        //     cv.visitInsn(Opcodes.AALOAD);
        //     cv.visitFieldInsn(Opcodes.GETFIELD, "bsh/internals/BshField", "initializer", "Lbsh/internals/function/BshMethodFunction;");

        //     // Call ...
        //     cv.visitLdcInsn(Types.getASMType(bshClass.descriptor));
        //     cv.visitInsn(Opcodes.ACONST_NULL);
        //     cv.visitLdcInsn(0); // Size of the array
        //     cv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
        //     cv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "bsh/internals/function/BshMethodFunction", "apply", "(Ljava/lang/Class;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", true);
        //     // cv.visitTypeInsn(Opcodes.CHECKCAST, "[Ljava/lang/Object;");
        //     // cv.visitVarInsn(Opcodes.ASTORE, chainArgsVarIndex);

        //     ASMHelper.writeCast(cv, bshField.type);

        //     // Set the static field
        //     cv.visitFieldInsn(Opcodes.PUTSTATIC, bshClass.internalName, bshField.name, bshField.getDescriptor());
        // }

        // Write the static initializers
        for (int i = 0; i < bshClass.staticInitializers.length; i++) {

            // Load in the operand stack the 'bsh.internals.BshClass.InitializerFunction' to be invoked
            cv.visitVarInsn(Opcodes.ALOAD, bshClassVarIndex);
            cv.visitFieldInsn(Opcodes.GETFIELD, "bsh/internals/BshClass", "staticInitializers", "[Lbsh/internals/BshClass$InitializerFunction;");
            cv.visitLdcInsn(i);
            cv.visitInsn(Opcodes.AALOAD);

            // Call BshClass$InitializerFunction.initialize()
            cv.visitVarInsn(Opcodes.ALOAD, bshClassVarIndex);
            cv.visitInsn(Opcodes.ACONST_NULL);
            cv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "bsh/internals/BshClass$InitializerFunction", "initialize", "(Lbsh/internals/BshClass;Ljava/lang/Object;)V", true);

            // // Load in the operand stack the 'bsh.internals.BshMethod' to be invoked
            // cv.visitLdcInsn(Types.getASMType(bshClass.descriptor));
            // cv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/internals/BshClass", "getDeclaredStaticInitializers", "(Ljava/lang/Class;)[Lbsh/internals/BshInitializer;", false);
            // cv.visitLdcInsn(i);
            // cv.visitInsn(Opcodes.AALOAD); // Get the 'BshMethod' from the returned BshMethod[] in the index 'methodIndex'

            // // call BshInitia        lizer.initialize()
            // cv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "bsh/internals/BshInitializer", "initialize", "()Ljava/lang/Object;", false);

            // if (staticInitializer.field != null) {
            //     // Set the static field
            //     cv.visitTypeInsn(Opcodes.CHECKCAST, bshClass.internalName);
            //     cv.visitFieldInsn(Opcodes.PUTSTATIC, bshClass.internalName, staticInitializer.field.getName(), staticInitializer.field.getDescriptor());
            // } else {
            //     // Just remove the value from the operand stack
            //     cv.visitInsn(Opcodes.POP);
            // }
        }

        // Default end
        cv.visitInsn(Opcodes.RETURN);
        cv.visitMaxs(0, 0); // Set the stack sizes (obs.: the ClassWritter should compute it by itself)
        cv.visitEnd();
    }

    private static void writeField(ClassWriter cw, BshClass bshClass, int fieldIndex) {
        final BshField bshField = bshClass.fields[fieldIndex];

        Object defaultValue = null; // TODO: test for default values to static and non-static fields!
        FieldVisitor fv = cw.visitField(bshField.getModifiers(), bshField.getName(), bshField.getDescriptor(), bshField.getSignature(), defaultValue);
        fv.visitEnd();
    }

    private static void writeEnumConstant(ClassWriter cw, BshClass bshClass, int enumConstantIndex) {
        final BshEnumConstant bshEnumConstant = bshClass.enumConstants[enumConstantIndex];

        Object defaultValue = null; // TODO: test for default values to static and non-static fields!
        // TODO: ver um signature melhor doq 'bshClass.descriptor'
        FieldVisitor fv = cw.visitField(BshModifier.ENUM_CONSTANT_MODIFIERS, bshEnumConstant.name, bshClass.descriptor, bshClass.descriptor, defaultValue);
        fv.visitEnd();
    }

    // TODO: como fica os initializers ? como iniciamos um field ? e um bloco de código ?
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
        final boolean isEnum = BshModifier.isEnum(bshClass.modifiers);

        // final Type[] genericParamsTypes = bshConstructor.genericParametersTypes;
        // final boolean isVarArgs = isVarArgsPredicate.test(constructor);

        // // TODO: add var-ags signature
        // final String constructorDescriptor = Types.getMethodDescriptor(void.class, genericParamsTypes);
        // final String constructorSignature = Types.getASMMethodSignature(bshConstructor.typeParams, genericParamsTypes, void.class, bshConstructor.genericExceptionTypes);

        MethodVisitor cv = cw.visitMethod(bshConstructor.modifiers, "<init>", bshConstructor.descriptor, bshConstructor.signature, Types.getInternalNames(bshConstructor.genericExceptionTypes));

        int paramsLocalVarEndIndex = 0;

        // Declare the parameters signature and also calculates the paramsLocalVarEndIndex
        for (BshParameter param: bshConstructor.parameters) {
            cv.visitParameter(param.getName(), param.getModifiers()); 
            paramsLocalVarEndIndex += param.getType() == double.class || param.getType() == long.class ? 2 : 1;
        }

        final int bshClassVarIndex = paramsLocalVarEndIndex + 1;
        final int chainArgsVarIndex = paramsLocalVarEndIndex + 2;
        final int chainArgsTypesVarIndex = paramsLocalVarEndIndex + 3;
        final int bshConstructorVarIndex = paramsLocalVarEndIndex + 4;

        cv.visitCode();

        // Load and store the 'bsh.internals.BshClass'
        cv.visitLdcInsn(Types.getASMType(bshClass.descriptor));
        cv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/internals/BshClass", "fromGeneratedClass", "(Ljava/lang/Class;)Lbsh/internals/BshClass;", false);
        cv.visitVarInsn(Opcodes.ASTORE, bshClassVarIndex);

        cv.visitVarInsn(Opcodes.ALOAD, bshClassVarIndex); // Load the 'bsh.internals.BshClass' for the 'bsh.internals.BshClass.findConstructorIndex()' call

        cv.visitVarInsn(Opcodes.ALOAD, bshClassVarIndex); // Load the 'bsh.internals.BshClass' for the first call
        cv.visitFieldInsn(Opcodes.GETFIELD, "bsh/internals/BshClass", "constructors", "[Lbsh/internals/BshConstructor;");
        cv.visitLdcInsn(constructorIndex);
        cv.visitInsn(Opcodes.AALOAD); // Get the 'BshConstructor' instance
        cv.visitVarInsn(Opcodes.ASTORE, bshConstructorVarIndex);

        // Get the supplier of the arguments for the constructor chain
        cv.visitVarInsn(Opcodes.ALOAD, bshConstructorVarIndex);
        cv.visitFieldInsn(Opcodes.GETFIELD, "bsh/internals/BshConstructor", "chainArgsSupplier", "Lbsh/internals/BshConstructor$ChainArgsSupplier;");

        // Call ...
        // Write: new CallStack()
        cv.visitTypeInsn(Opcodes.NEW, "bsh/CallStack");
        cv.visitInsn(Opcodes.DUP);
        cv.visitMethodInsn(Opcodes.INVOKESPECIAL, "bsh/CallStack", "<init>", "()V", false);

        cv.visitLdcInsn(Types.getASMType(bshClass.descriptor));
        cv.visitInsn(Opcodes.ACONST_NULL);
        ASMHelper.writeCollectArgumentsToArray(cv, false, bshConstructor.parameters);
        cv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "bsh/internals/BshConstructor$ChainArgsSupplier", "apply", "(Lbsh/CallStack;Ljava/lang/Class;Ljava/lang/Object;[Ljava/lang/Object;)[Ljava/lang/Object;", true);
        cv.visitVarInsn(Opcodes.ASTORE, chainArgsVarIndex);

        // System.out.println("------------------------------------------------------------");
        // System.out.println("bshConstructor.descriptor: " + bshConstructor.getDescriptor());
        // System.out.println("bshConstructor.superChain: " + bshConstructor.superChain);
        // System.out.println("bshClass.internalName: " + bshClass.internalName);
        // System.out.println("bshConstructor.chainArgsLength: " + bshConstructor.chainArgsLength);
        // System.out.println("bshClass.constructors: " + bshClass.constructors);
        // System.out.println("------------------------------------------------------------");

        // Call Reflect.getTypes() to get the types of the arguments and store it as a local variable
        cv.visitVarInsn(Opcodes.ALOAD, chainArgsVarIndex);
        cv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/Reflect", "getTypes", "([Ljava/lang/Object;)[Ljava/lang/Class;", false);
        cv.visitVarInsn(Opcodes.ASTORE, chainArgsTypesVarIndex);

        // System.out.println("-----------------------------------------------");
        if (bshConstructor.superChain) {
            // cv.visitVarInsn(Opcodes.ALOAD, chainArgsVarIndex);
            // cv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "bsh/internals/BshClass", "findSuperConstructorIndex", "([Ljava/lang/Object;)I", false);

            // Call the Reflect.findMostSpecificInvocableIndex() to get the desired constructor index
            cv.visitVarInsn(Opcodes.ALOAD, chainArgsTypesVarIndex); // Load the types of the arguments
            // Load the constructors of the enum
            cv.visitVarInsn(Opcodes.ALOAD, bshClassVarIndex);
            cv.visitFieldInsn(Opcodes.GETFIELD, "bsh/internals/BshClass", "superConstructors", "[Ljava/lang/reflect/Constructor;");
            cv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/Reflect", "findMostSpecificInvocableIndex", "([Ljava/lang/Class;[Ljava/lang/reflect/Constructor;)I", false);

            // final String ownerInternalName = Types.getInternalName(bshClass.superClass);
            // System.out.println("chainArgsVarIndex: " + chainArgsVarIndex);
            // System.out.println("chainArgsTypesVarIndex: " + chainArgsTypesVarIndex);
            // System.out.println("bshConstructor.getParameterTypes(): " + Arrays.asList(bshConstructor.getParameterTypes()));
            // System.out.println("bshConstructor.chainArgsLength: " + bshConstructor.chainArgsLength);
            // System.out.println("ownerInternalName: " + ownerInternalName);
            // System.out.println("isEnum: " + isEnum);
            // System.out.println("bshClass.superConstructors: " + bshClass.superConstructors);
            ASMHelper.writeSwtichCaseToCallConstructor(cv, 0, chainArgsVarIndex, chainArgsTypesVarIndex, bshConstructor.chainArgsLength, bshClass.superInternalName, isEnum, bshClass.superConstructors, Constructor::getParameterTypes, Constructor::isVarArgs);
        } else {
            // cv.visitVarInsn(Opcodes.ALOAD, chainArgsVarIndex);
            // cv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "bsh/internals/BshClass", "findConstructorIndex", "([Ljava/lang/Object;)I", false);

            // Call the Reflect.findMostSpecificInvocableIndex() to get the desired constructor index
            cv.visitVarInsn(Opcodes.ALOAD, chainArgsTypesVarIndex); // Load the types of the arguments
            // Load the constructors of the enum
            cv.visitVarInsn(Opcodes.ALOAD, bshClassVarIndex);
            cv.visitFieldInsn(Opcodes.GETFIELD, "bsh/internals/BshClass", "constructors", "[Lbsh/internals/BshConstructor;");
            cv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/Reflect", "findMostSpecificInvocableIndex", "([Ljava/lang/Class;[Lbsh/internals/BshConstructor;)I", false);

            ASMHelper.writeSwtichCaseToCallConstructor(cv, 0, chainArgsVarIndex, chainArgsTypesVarIndex, bshConstructor.chainArgsLength, bshClass.internalName, isEnum, bshClass.constructors, BshConstructor::getParameterTypes, BshConstructor::isVarArgs);
        }
        // System.out.println("-----------------------------------------------");

        // switch (bshConstructor.constructorChainKind) {
        //     case BshConstructor.NO_CHAIN:
        //         // Default begin: Call the superclass constructor 'super()''
        //         cv.visitVarInsn(Opcodes.ALOAD, 0); // Load 'this' onto the stack
        //         cv.visitMethodInsn(Opcodes.INVOKESPECIAL, Types.getInternalName(bshClass.superClass), "<init>", "()V", false);

        //         // Invoke the initializers for each constructor that calls the super() constructor
        //         cv.visitLdcInsn(Types.getASMType(bshClass.descriptor)); // Load in the operand stack the 'bsh.BshConstructor' to be invoked
        //         cv.visitVarInsn(Opcodes.ALOAD, 0); // Load 'this' onto the stack
        //         cv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/internals/BshClass", "initialize", "(Ljava/lang/Class;Ljava/lang/Object;)V", false);
        //         break;
        //     case BshConstructor.THIS_CHAIN:
        //         // TODO: impl it!
        //         break;
        //     case BshConstructor.SUPER_CHAIN:
        //         // TODO: impl it!
        //         break;
        // }

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
        // cv.visitLdcInsn(Types.getASMType(bshClass.descriptor));
        // cv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/internals/BshClass", "getDeclaredConstructors", "(Ljava/lang/Class;)[Lbsh/internals/BshConstructor;", false);

        // cv.visitVarInsn(Opcodes.ALOAD, bshClassVarIndex); // Load the 'bsh.internals.BshClass' for the first call
        // cv.visitFieldInsn(Opcodes.GETFIELD, "bsh/internals/BshClass", "constructors", "[Lbsh/internals/BshConstructor;");
        // cv.visitLdcInsn(constructorIndex);
        // cv.visitInsn(Opcodes.AALOAD); // Get the 'BshConstructor' from the returned BshConstructor[] in the index 'constructorIndex'

        // TODO: reescrever tudo usando o 'body' de bsh.internals.BshConstructor
        // // Call the 'bsh.internals.BshConstructor.construct()'
        // cv.visitVarInsn(Opcodes.ALOAD, bshConstructorVarIndex);
        // cv.visitVarInsn(Opcodes.ALOAD, 0); // Load the 'this'
        // ASMHelper.writeCollectParametersToArray(cv, false, bshConstructor.parameters); // Load an Object[] with all parameters
        // cv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "bsh/internals/BshConstructor", "construct", "(Ljava/lang/Object;[Ljava/lang/Object;)V", false);

        // Write the initializers
        for (int i = 0; i < bshClass.initializers.length; i++) {
            // Load in the operand stack the 'bsh.internals.BshClass.InitializerFunction' to be invoked
            cv.visitVarInsn(Opcodes.ALOAD, bshClassVarIndex);
            cv.visitFieldInsn(Opcodes.GETFIELD, "bsh/internals/BshClass", "initializers", "[Lbsh/internals/BshClass$InitializerFunction;");
            cv.visitLdcInsn(i);
            cv.visitInsn(Opcodes.AALOAD);

            // Call BshClass$InitializerFunction.initialize()
            cv.visitVarInsn(Opcodes.ALOAD, bshClassVarIndex);
            cv.visitVarInsn(Opcodes.ALOAD, 0); // Load 'this' onto the stack
            cv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "bsh/internals/BshClass$InitializerFunction", "initialize", "(Lbsh/internals/BshClass;Ljava/lang/Object;)V", true);
        }

        // TODO: ver o "fromGeneratedClass", salvar em variável e reutilizar

        // Load in the operand stack the 'bsh.internals.BshConstructor' to be invoked
        // cv.visitLdcInsn(Types.getASMType(bshClass.descriptor));
        // cv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/internals/BshClass", "fromGeneratedClass", "(Ljava/lang/Class;)Lbsh/internals/BshClass;", false);
        cv.visitVarInsn(Opcodes.ALOAD, bshClassVarIndex);
        cv.visitFieldInsn(Opcodes.GETFIELD, "bsh/internals/BshClass", "constructors", "[Lbsh/internals/BshConstructor;");
        // cv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/internals/BshClass", "getDeclaredMethods", "(Ljava/lang/Class;)[Lbsh/internals/BshConstructor;", false);
        cv.visitLdcInsn(constructorIndex);
        cv.visitInsn(Opcodes.AALOAD); // Get the 'BshConstructor' from the returned BshConstructor[] in the index 'methodIndex'
        cv.visitFieldInsn(Opcodes.GETFIELD, "bsh/internals/BshConstructor", "body", "Lbsh/internals/BshConstructor$BodyFunction;");
        // Write: new CallStack()
        cv.visitTypeInsn(Opcodes.NEW, "bsh/CallStack");
        cv.visitInsn(Opcodes.DUP);
        cv.visitMethodInsn(Opcodes.INVOKESPECIAL, "bsh/CallStack", "<init>", "()V", false);
        //
        cv.visitLdcInsn(Types.getASMType(bshClass.descriptor));
        cv.visitVarInsn(Opcodes.ALOAD, 0); // Load 'this' into stack
        ASMHelper.writeCollectArgumentsToArray(cv, false, bshConstructor.parameters);
        cv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "bsh/internals/BshConstructor$BodyFunction", "apply", "(Lbsh/CallStack;Ljava/lang/Class;Ljava/lang/Object;[Ljava/lang/Object;)V", true);


        // Default end
        cv.visitInsn(Opcodes.RETURN); // Return void
        cv.visitMaxs(0, 0); // Set the stack sizes (obs.: the ClassWritter should compute it by itself)
        cv.visitEnd();

        // System.out.println("------------------------------------------------------------");
        // System.out.println("constructor visitor: \n" + cv);
        // System.out.println("------------------------------------------------------------");
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
        // final Type returnType = bshMethod.genericReturnType;
        final boolean isStatic = BshModifier.isStatic(bshMethod.modifiers);

        MethodVisitor mv = cw.visitMethod(bshMethod.modifiers, bshMethod.name, bshMethod.descriptor, bshMethod.signature, Types.getInternalNames(bshMethod.genericExceptionTypes));

        // Declare the parameters signature
        for (final BshParameter param: bshMethod.parameters)
            mv.visitParameter(param.getName(), param.getModifiers());

        // Abstract methods have no body
        if (BshModifier.isAbstract(bshMethod.modifiers)) return;

        mv.visitCode();

        // Load in the operand stack the 'bsh.internals.BshMethod' to be invoked
        mv.visitLdcInsn(Types.getASMType(bshClass.descriptor));
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/internals/BshClass", "fromGeneratedClass", "(Ljava/lang/Class;)Lbsh/internals/BshClass;", false);
        mv.visitFieldInsn(Opcodes.GETFIELD, "bsh/internals/BshClass", "methods", "[Lbsh/internals/BshMethod;");
        // mv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/internals/BshClass", "getDeclaredMethods", "(Ljava/lang/Class;)[Lbsh/internals/BshMethod;", false);
        mv.visitLdcInsn(methodIndex);
        mv.visitInsn(Opcodes.AALOAD); // Get the 'BshMethod' from the returned BshMethod[] in the index 'methodIndex'
        mv.visitFieldInsn(Opcodes.GETFIELD, "bsh/internals/BshMethod", "body", "Lbsh/internals/BshMethod$BodyFunction;");
        // Write: new CallStack()
        mv.visitTypeInsn(Opcodes.NEW, "bsh/CallStack");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "bsh/CallStack", "<init>", "()V", false);
        //
        mv.visitLdcInsn(Types.getASMType(bshClass.descriptor));
        if (!isStatic)
            mv.visitVarInsn(Opcodes.ALOAD, 0); // Load 'this' into stack
        else
            mv.visitInsn(Opcodes.ACONST_NULL); // Load 'null' into stack
        ASMHelper.writeCollectArgumentsToArray(mv, isStatic, bshMethod.parameters);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "bsh/internals/BshMethod$BodyFunction", "apply", "(Lbsh/CallStack;Ljava/lang/Class;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", true);

        // final Class<?> rawReturnType = Types.getRawType(returnType);
        // final Class<?> invokeRawReturnType = rawReturnType.isPrimitive() ? Primitive.boxType(rawReturnType) : rawReturnType;

        // if (returnType != void.class)
        //     mv.visitTypeInsn(Opcodes.CHECKCAST, Types.getInternalName(invokeRawReturnType));

        // mv.visitInsn(Opcodes.ACONST_NULL);
        // mv.visitInsn(Opcodes.ARETURN);

        if (bshMethod.genericReturnType == void.class) {
            mv.visitInsn(Opcodes.POP);
            mv.visitInsn(Opcodes.RETURN);
        } else {
            ASMHelper.writeAssignToType(mv, bshMethod.genericReturnType);
            if (bshMethod.genericReturnType == boolean.class) mv.visitInsn(Opcodes.IRETURN);
            else if (bshMethod.genericReturnType == char.class) mv.visitInsn(Opcodes.IRETURN);
            else if (bshMethod.genericReturnType == byte.class) mv.visitInsn(Opcodes.IRETURN);
            else if (bshMethod.genericReturnType == short.class) mv.visitInsn(Opcodes.IRETURN);
            else if (bshMethod.genericReturnType == int.class) mv.visitInsn(Opcodes.IRETURN);
            else if (bshMethod.genericReturnType == long.class) mv.visitInsn(Opcodes.LRETURN);
            else if (bshMethod.genericReturnType == float.class) mv.visitInsn(Opcodes.FRETURN);
            else if (bshMethod.genericReturnType == double.class) mv.visitInsn(Opcodes.DRETURN);
            else mv.visitInsn(Opcodes.ARETURN);
        }

        // if (rawReturnType == void.class) {
        //     mv.visitInsn(Opcodes.POP);
        //     mv.visitInsn(Opcodes.RETURN);
        // } else if (rawReturnType == boolean.class) {
        //     mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
        //     mv.visitInsn(Opcodes.IRETURN);
        // } else if (rawReturnType == char.class) {
        //     mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
        //     mv.visitInsn(Opcodes.IRETURN);
        // } else if (rawReturnType == byte.class) {
        //     mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
        //     mv.visitInsn(Opcodes.IRETURN);
        // } else if (rawReturnType == short.class) {
        //     mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
        //     mv.visitInsn(Opcodes.IRETURN);
        // } else if (rawReturnType == int.class) {
        //     mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
        //     mv.visitInsn(Opcodes.IRETURN);
        // } else if (rawReturnType == long.class) {
        //     mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
        //     mv.visitInsn(Opcodes.LRETURN);
        // } else if (rawReturnType == float.class) {
        //     mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
        //     mv.visitInsn(Opcodes.FRETURN);
        // } else if (rawReturnType == double.class) {
        //     mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
        //     mv.visitInsn(Opcodes.DRETURN);
        // } else {
        //     mv.visitInsn(Opcodes.ARETURN);
        // }

        mv.visitMaxs(0, 0); // The Writter must calculate the values by itself.
        mv.visitEnd();
    }

    private static void writeEnumMethodValues(ClassWriter cw, BshClass bshClass) {
        final int mods = BshModifier.PUBLIC | BshModifier.STATIC | BshModifier.FINAL;
        final String descriptor = "()[" + bshClass.descriptor;
        MethodVisitor mv = cw.visitMethod(mods, "values", descriptor, null, null);

        mv.visitCode();

        mv.visitLdcInsn(bshClass.enumConstants.length); // Size of the array
        mv.visitTypeInsn(Opcodes.ANEWARRAY, bshClass.internalName);
        
        for (int eci = 0; eci < bshClass.enumConstants.length; eci++) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(eci); // Load the array index to set the value
            mv.visitFieldInsn(Opcodes.GETSTATIC, bshClass.internalName, bshClass.enumConstants[eci].name, bshClass.descriptor);
            mv.visitInsn(Opcodes.AASTORE);
        }

        mv.visitInsn(Opcodes.ARETURN);

        mv.visitMaxs(0, 0); // The Writter must calculate the values by itself.
        mv.visitEnd();
    }

    // TODO: fazer testes unitários para as 2 exceptions que podem ser lançadas e tb garantindo que o método está ok!
    private static void writeEnumMethodValueOf(ClassWriter cw, BshClass bshClass) {
        final int mods = BshModifier.PUBLIC | BshModifier.STATIC | BshModifier.FINAL;
        final String descriptor = "(Ljava/lang/String;)" + bshClass.descriptor;
        MethodVisitor mv = cw.visitMethod(mods, "valueOf", descriptor, null, null);

        // TODO: manter signature ? verificar se nas JDKs mais atuais é gerado signature para o parâmetro
        // Declare the parameter signature
        mv.visitParameter("name", BshModifier.NO_MODIFIERS);

        mv.visitCode();

        final Label preSwitchLabel = new Label();

        // Check if argument "name" is null
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitJumpInsn(Opcodes.IFNONNULL, preSwitchLabel);
        // If null: throw new NullPointerException("Name is null")
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/NullPointerException");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("Name is null");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/NullPointerException", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(Opcodes.ATHROW);

        mv.visitLabel(preSwitchLabel);

        // Load the hash code of the enum constant name to be found
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);

        final Label defaultCaseLabel = new Label();
        final Label[] labels = new Label[bshClass.enumConstants.length];
        final int[] nameHashes = new int[bshClass.enumConstants.length];
        // final Label afterSwtichLabel = new Label();

        // Create the labels and get the name hashes
        for (int i = 0; i < bshClass.enumConstants.length; i++) {
            labels[i] = new Label();
            nameHashes[i] = bshClass.enumConstants[i].name.hashCode();
        }

        // mv.visitTableSwitchInsn(0, labels.length-1, defaultCaseLabel, labels);
        mv.visitLookupSwitchInsn(defaultCaseLabel, nameHashes, labels);

        for (int i = 0; i < bshClass.enumConstants.length; i++) {
            mv.visitLabel(labels[i]);
            mv.visitFieldInsn(Opcodes.GETSTATIC, bshClass.internalName, bshClass.enumConstants[i].name, bshClass.descriptor);
            mv.visitInsn(Opcodes.ARETURN);
            // mv.visitJumpInsn(Opcodes.GOTO, defaultCaseLabel);
        }

        mv.visitLabel(defaultCaseLabel);
        // If not found the enum constant: throw new IllegalArgumentException("No enum constant " + this.getClass().getCanonicalName() + "." + name)
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("No enum constant " + bshClass.canonicalName + ".");
        // mv.visitVarInsn(Opcodes.ALOAD, 0);
        // mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(Opcodes.ATHROW);

        // mv.visitInsn(Opcodes.ACONST_NULL);
        // mv.visitInsn(Opcodes.ARETURN);

        mv.visitMaxs(0, 0); // The Writter must calculate the values by itself.
        mv.visitEnd();
    }


    // private void generateEnumSupport(String fqClassName, String className, String classDescript, ClassWriter cw) {
    //     // generate enum values() method delegated to static This.enumValues.
    //     MethodVisitor cv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "values", "()["+classDescript, null, null);
    //     pushBshStatic(fqClassName, className, cv);
    //     cv.visitMethodInsn(INVOKEVIRTUAL, "bsh/This", "enumValues", "()[Ljava/lang/Object;", false);
    //     generatePlainReturnCode("["+classDescript, cv);
    //     cv.visitMaxs(0, 0);
    //     // generate Enum.valueOf delegate method
    //     cv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "valueOf", "(Ljava/lang/String;)"+classDescript, null, null);
    //     cv.visitLdcInsn(Type.getType(classDescript));
    //     cv.visitVarInsn(ALOAD, 0);
    //     cv.visitMethodInsn(INVOKESTATIC, "java/lang/Enum", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;", false);
    //     generatePlainReturnCode(classDescript, cv);
    //     cv.visitMaxs(0, 0);
    //     // // generate default private constructor and initInstance call
    //     // cv = cw.visitMethod(ACC_PRIVATE, "<init>", "(Ljava/lang/String;I)V", null, null);
    //     // cv.visitVarInsn(ALOAD, 0);
    //     // cv.visitVarInsn(ALOAD, 1);
    //     // cv.visitVarInsn(ILOAD, 2);
    //     // cv.visitMethodInsn(INVOKESPECIAL, "java/lang/Enum", "<init>", "(Ljava/lang/String;I)V", false);
    //     // cv.visitVarInsn(ALOAD, 0);
    //     // cv.visitLdcInsn(className);
    //     // generateParameterReifierCode(new String[0], false/*isStatic*/, cv);
    //     // cv.visitMethodInsn(INVOKESTATIC, "bsh/This", "initInstance", "(Lbsh/GeneratedClass;Ljava/lang/String;[Ljava/lang/Object;)V", false);
    //     // cv.visitInsn(RETURN);
    //     // cv.visitMaxs(0, 0);
    // }

    // TODO: fazer um teste especificamente tentando pegar o caso de erro ClassGeneratorUtil::isPrimitive

    // TODO: rever todas essas merds de nomes
    private static class ASMHelper {

        // TODO: arrumar o Types.castObject() para ter o isJavaStrict e utilizar aqui o Interpreter para ter o isJavaStrict
        private static void writeAssignToType(MethodVisitor mv, Type argType) {
            // TODO: tlvz melhorar esse algoritmo ? tlvz fazer Primitive.boxType() aceitar Type e reduzir a tratativa ?
            final boolean isPrimitive = Types.isPrimitive(argType);
            // TODO: ver uma solução mais elegante para esta merda de cast
            final Type assignType = isPrimitive ? Primitive.boxType((Class<?>) argType) : argType;

            // // Load the argument
            // mv.visitVarInsn(Opcodes.ALOAD, argsVariableNum);
            // mv.visitLdcInsn(argIndex);
            // mv.visitInsn(Opcodes.AALOAD);

            // Load the 'java.lang.Class<?>' to cast the value
            mv.visitLdcInsn(Types.getASMType(Types.getDescriptor(assignType)));

            // Invoke 'Reflect.cast()' to do the cast
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/Reflect", "assign", "(Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;", false);

            // Make a checkcast just to don't have a bytecode verification error
            mv.visitTypeInsn(Opcodes.CHECKCAST, Types.getInternalName(assignType));

            // Convert the boxed value to the primitive value
            if (isPrimitive) {
                if (argType == boolean.class)
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                else if (argType == char.class)
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
                else if (argType == byte.class)
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
                else if (argType == short.class)
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
                else if (argType == int.class)
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                else if (argType == long.class)
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
                else if (argType == float.class)
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
                else if (argType == double.class)
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
            }
        }

        // TODO: arrumar o comentário tb
        private static void writeCollectArgumentsArrayToStack(MethodVisitor mv, int argsVariableNum, Class<?>[] paramsTypes, boolean isVarArgs) {
            final int typesOffset = isVarArgs ? 1 : 0;

            // Write the instructions for the initial arguments
            for (int i = 0; i < paramsTypes.length - typesOffset; i++) {
                // Load the argument
                mv.visitVarInsn(Opcodes.ALOAD, argsVariableNum);
                mv.visitLdcInsn(i);
                mv.visitInsn(Opcodes.AALOAD);
                ASMHelper.writeAssignToType(mv, paramsTypes[i]);
            }

            // Nothing else to do
            if (!isVarArgs) return;

            // TODO: ver isso
            throw new RuntimeException("Tem que implementar essa merda");
        }

        private static void writeCollectArgumentsToArray(MethodVisitor mv, boolean isStaticMethod, BshParameter[] parameters) {
            // Define and create the Object[] array to store the parameters
            mv.visitLdcInsn(parameters.length); // Size of the array
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
            // Load the parameters inside the Object[]
            int paramLocalVarIndex = isStaticMethod ? 0 : 1;
            int paramIndex = 0;
            for (final BshParameter param: parameters) {
                final Type paramType = param.type;
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
        }

        private static <T> void writeSwtichCaseToCallConstructor(MethodVisitor mv, int thisVariableIndex, int argsVariableIndex, int argsTypesVariableIndex, int argsLength, String constructorOwnerInternalName, boolean isEnum, T[] constructors, Function<T, Class<?>[]> paramsTypesGetter, Predicate<T> isVarArgsPredicate) {
            // Switch case to construct and set the enum constant
            // final Label defaultLabel = new Label();
            final Label defaultCaseLabel = new Label();
            final Label[] labels = new Label[constructors.length];
            final Label afterSwtichLabel = new Label();

            for (int i = 0; i < labels.length; i++) labels[i] = new Label();

            mv.visitTableSwitchInsn(0, labels.length-1, defaultCaseLabel, labels);

            for (int ci = 0; ci < constructors.length; ci++) {
                final T constructor = constructors[ci];

                final Class<?>[] paramsTypes = paramsTypesGetter.apply(constructor);
                final boolean isVarArgs = isVarArgsPredicate.test(constructor);
                final String descriptor = Types.getMethodDescriptor(void.class, paramsTypes);

                // TODO: como fica se não tiver nenhum var args ? ( no caso seria uma array vazia como parâmetro para o método )
                // // If it can't be an option, avoid writing useless instructions
                // if (paramsTypes.length != argsLength && (!isVarArgs || argsLength < (paramsTypes.length-1)))
                //     continue;

                mv.visitLabel(labels[ci]);

                // final String descriptor = Types.getMethodDescriptor(void.class, paramsTypes);

                if (paramsTypes.length == argsLength || (isVarArgs && argsLength >= paramsTypes.length)) {
                    // Call the super initializer ( super constructor )
                    mv.visitVarInsn(Opcodes.ALOAD, thisVariableIndex); // Load the 'this' arg to be used by the initializer
                    ASMHelper.writeCollectArgumentsArrayToStack(mv, argsVariableIndex, paramsTypes, isVarArgs);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, constructorOwnerInternalName, "<init>", descriptor, false);
                    mv.visitJumpInsn(Opcodes.GOTO, afterSwtichLabel);
                } else {
                    mv.visitJumpInsn(Opcodes.GOTO, defaultCaseLabel);
                    // // Call the default super initializer
                    // mv.visitVarInsn(Opcodes.ALOAD, thisVariableIndex); // Load the 'this' arg to be used by the initializer
                    // mv.visitMethodInsn(Opcodes.INVOKESPECIAL, constructorOwnerInternalName, "<init>", "()V", false);
                }

            }

            // The first case is the '-1', when we don't find a constructor
            // Note: the default case also is that we don't find a constructor
            mv.visitLabel(defaultCaseLabel);

            // TODO: implementar isso para ajustar a mensagem de erro para construtores de Enums
            // if (isEnum) {
            //     Arrays.copyOfRange(constructors, 2, argsLength);
            // } else { ... }

            // Allocate the instance of NoSuchMethodError
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/NoSuchMethodError");
            mv.visitInsn(Opcodes.DUP); // Duplicate reference for constructor

            // Create the error message. It basically write the bytecode for this:
            //  "No such constructor: MyClass(" + String.join(", ", Reflect.getTypesNames(argsTypes)) + ")"
            mv.visitLdcInsn("No such constructor: " + constructorOwnerInternalName.replace('/', '.') + "(");
            mv.visitLdcInsn(", ");
            mv.visitVarInsn(Opcodes.ALOAD, argsTypesVariableIndex);
            // System.out.println("---------------------------------------------------");
            // System.out.println("ASMHelper.writeSwtichCaseToCallConstructor() -> constructorOwnerInternalName: " + constructorOwnerInternalName);
            // System.out.println("ASMHelper.writeSwtichCaseToCallConstructor() -> isEnum: " + isEnum);
            // System.out.println("ASMHelper.writeSwtichCaseToCallConstructor() -> argsLength: " + argsLength);
            // Thread.dumpStack();
            // System.out.println("---------------------------------------------------");
            if (isEnum) {
                mv.visitLdcInsn(2);
                mv.visitLdcInsn(argsLength);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "copyOfRange", "([Ljava/lang/Object;II)[Ljava/lang/Object;", false);
            }
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "bsh/Reflect", "getTypesNames", "([Ljava/lang/reflect/Type;)[Ljava/lang/String;", false);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "join", "(Ljava/lang/CharSequence;[Ljava/lang/CharSequence;)Ljava/lang/String;", false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false);
            mv.visitLdcInsn(")");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false);

            // Call the constructor for the NoSuchMethodError and then throw it
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/NoSuchMethodError", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitInsn(Opcodes.ATHROW);
            // mv.visitJumpInsn(Opcodes.GOTO, afterSwtichLabel); // Note: o GOTO é inútil se lançamos exception xD

            // // Call the default super initializer in the default case
            // mv.visitLabel(defaultLabel);
            // // mv.visitVarInsn(Opcodes.ALOAD, thisVariableIndex); // Load the 'this' arg to be used by the initializer
            // // mv.visitMethodInsn(Opcodes.INVOKESPECIAL, constructorOwnerInternalName, "<init>", "()V", false);
            // mv.visitJumpInsn(Opcodes.GOTO, afterSwtichLabel);

            // mv.visitTableSwitchInsn(-1, labels.length-1, defaultLabel, labels);
            mv.visitLabel(afterSwtichLabel); // Just to end the swtich-case instructions
        }
    }
}
