/*****************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one                *
 * or more contributor license agreements.  See the NOTICE file              *
 * distributed with this work for additional information                     *
 * regarding copyright ownership.  The ASF licenses this file                *
 * to you under the Apache License, Version 2.0 (the                         *
 * "License"); you may not use this file except in compliance                *
 * with the License.  You may obtain a copy of the License at                *
 *                                                                           *
 *     http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                           *
 * Unless required by applicable law or agreed to in writing,                *
 * software distributed under the License is distributed on an               *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY                    *
 * KIND, either express or implied.  See the License for the                 *
 * specific language governing permissions and limitations                   *
 * under the License.                                                        *
 *                                                                           *
 *                                                                           *
 * This file is part of the BeanShell Java Scripting distribution.           *
 * Documentation and updates may be found at http://www.beanshell.org/       *
 * Patrick Niemeyer (pat@pat.net)                                            *
 * Author of Learning Java, O'Reil0b0001
*/

package bsh;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.text.DateFormat.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
// import java.util.stream.Collectors;
// import java.util.stream.Stream;
// import static bsh.ClassGenerator.Type;
import java.util.stream.Collectors;

import bsh.internals.BshClass;
import bsh.internals.BshClass.InitializerFunction;
import bsh.internals.type.BshLazyType;
import bsh.internals.BshConstructor;
import bsh.internals.BshEnumConstant;
// import bsh.internals.BshConsumer;
import bsh.internals.BshField;
import bsh.internals.BshMethod;
import bsh.internals.BshModifier;
import bsh.internals.BshParameter;

// TODO: precisamos de teste unitário para o body de uma classe ? anteriormente era um BSHBlock, portanto aceitava quase qualquer coisa ???
// TODO: teste unitário para o 'static' no BSHBlock do body ?
// TODO: como ficam os métodos abstratos de uma interface ? Eles não possuem body :V

// TODO: documentar essa classe
class BSHClassDeclaration extends SimpleNode {
    // /**
    //     The class instance initializer method name.
    //     A BshMethod by this name is installed by the class delcaration into
    //     the static class body namespace.
    //     It is called once to initialize the static members of the class space
    //     and each time an instances is created to initialize the instance
    //     members.
    // */
    // final static String CLASSINITNAME = "_bshClassInit";

    boolean isInterface;
    boolean isEnum;
    String simpleName;
    Modifiers modifiers = new Modifiers(Modifiers.CLASS); // TODO: rever esses Modifiers!
    int numInterfaces;
    boolean extend;
    // Type type;
    private Class<?> generatedClass;

    BSHClassDeclaration(int id) { super(id); }

    private int getModifiers() {
        // TODO: ver esse cara, pq só o 'CLASS_MODIFIERS' ? e o 'INTERFACE_MODIFIERS' ?
        int mods = this.modifiers.getModifiers() & BshModifier.CLASS_MODIFIERS;
        if (this.isInterface)
            mods |= BshModifier.INTERFACE | BshModifier.ABSTRACT;
        else if (this.isEnum)
            mods |= BshModifier.ENUM;
        return mods;
    }

    private String getName(NameSpace declaringNS) {
        // TODO: fazer teste para inner classes!
        if (this.parent instanceof BSHClassDeclaration)
            return ((BSHClassDeclaration) this.parent).getName(declaringNS) + "$" + this.simpleName;

        if (declaringNS.getPackage() != null)
            return declaringNS.getPackage() + "." + this.simpleName;

        return this.simpleName;
    }

    // TODO: o superNode deveria ser um BSHType -> isso unificaria o suporte à generics, porém teriamos q validar aqui se o superClass n é um primitive, interface ou outra coisa
    private Type getSuperClass(CallStack callStack, Interpreter interpreter) throws EvalError {
        if (this.isEnum) return Enum.class; // TODO: Enum recebe um generic, implementar um suporte para isso!
        if (this.isInterface || !this.extend) return Object.class;

        final BSHName superClassNode = this.jjtGetChild(0);
        // TODO: verificar se deveria ser um BSHName
        final Type superClass = superClassNode._toType(callStack, interpreter);
        // TODO: verificar isso
        // if (Reflect.isFinal(superClass))
        //     throw new EvalException("Cannot inherit from final class " + superClass.getName(), null, null);

        // TODO: verificar isso
        // // Validate if can extend this class
        // try {
        //     Interpreter.mainSecurityGuard.canExtends(superClass);
        // } catch (UtilEvalError e) {
        //     throw e.toEvalError(this, callStack);
        // }
        return superClass == null ? Object.class : superClass;
    }

    private Type[] getInterfaces(CallStack callStack, Interpreter interpreter) throws EvalError {
        final Type[] interfaces = new Type[numInterfaces];
        final Node[] children = this.jjtGetChildren();
        final int offset = this.extend ? 1 : 0;

        for (int i=0; i < numInterfaces; i++) {
            BSHName node = (BSHName) children[i + offset];
            // TODO: ver isso, não deveria ser um BSHType ou BSHCompositeType ?
            interfaces[i] = node._toType(callStack, interpreter);

            // TODO: verificar a implementação de um Types.isInterface(Type type)
            // if (!interfaces[i].isInterface())
            //     throw new EvalException("Type: "+node.name+" is not an interface!", this, callStack );

            // TODO: verificar isso tb
            // // Validate if can implement this interface
            // try {
            //     Interpreter.mainSecurityGuard.canImplements(interfaces[i]);
            // } catch (UtilEvalError e) {
            //     throw e.toEvalError(this, callStack);
            // }
        }
        return interfaces;
    }

    // TODO: como fica o 'super()' quando o super class n tem um construtor com essa signature visível ?
    // TODO: enums possuem o constructo default como private ??
    // TODO: verificar otimizações para essa classe e outras partes do código: usar 'final', usar constantes para arrays imutáveis e lambdas sem funcionalidades ?

    // // TODO: tirar o 'declaringClass' de BshConstructor pq se n isso aqui vai dar merda
    // private static final BshConstructor DEFAULT_CONSTRUCTOR = new BshConstructor(BshModifier.PUBLIC, new BshParameter[0], new Type[0], true, 0, (thisArg, args) -> new Object[0], (thisArg, args) -> null);
    // private static final BshConstructor DEFAULT_ENUM_CONSTRUCTOR = new BshConstructor(
    //     BshModifier.PRIVATE,
    //     new BshParameter[] {
    //         new BshParameter(BshModifier.NO_MODIFIERS, String.class, "name", false),
    //         new BshParameter(BshModifier.NO_MODIFIERS, int.class, "ordinal", false)
    //     },
    //     new Type[0],
    //     true,
    //     0,
    //     (thisArg, args) -> new Object[] { args[0], args[1] },
    //     (thisArg, args) -> null
    // );

    // TODO: como vai ficar o super() ?
    // private BshConstructor defaultConstructor() {

    //     if (!this.isEnum)
    //         return new BshConstructor(BshModifier.PUBLIC, new BshParameter[0], new Type[0], true, 0, (thisArg, args) -> new Object[0], (thisArg, args) -> null);

            
    //     return new BshConstructor(
    //         BshModifier.PRIVATE,
    //         new BshParameter[] {
    //             new BshParameter(BshModifier.NO_MODIFIERS, String.class, "name", false),
    //             new BshParameter(BshModifier.NO_MODIFIERS, int.class, "ordinal", false)
    //         },
    //         new Type[0],
    //         true,
    //         0,
    //         (thisArg, args) -> new Object[] { args[0], args[1] },
    //         (thisArg, args) -> null
    //     );

    //     // return this.isEnum ? BSHClassDeclaration.DEFAULT_ENUM_CONSTRUCTOR : BSHClassDeclaration.DEFAULT_CONSTRUCTOR;

    //     // TODO: fazer cache dos constructors default dps de remover o 'declaringClass' dos membros de BshClass

    //     // if (!this.isEnum)
    //     //     return new BshConstructor(BshModifier.PUBLIC, new BshParameter[0], new Type[0], true, 0, () -> new Object[0], (cs) -> null);
    //     // final int mods = this.isEnum ? BshModifier.PRIVATE : BshModifier.PUBLIC;
        
    //     // return new BshConstructor(mods, new BshParameter[0], new Type[0], true, 0, () -> new Object[0], (cs) -> null);

    //     // // private final String name;
    //     // // private final int ordinal;
    //     // final BshParameter nameParameter = new BshParameter(BshModifier.FINAL, String.class, "name", false);
    //     // final BshParameter ordinalParameter = new BshParameter(BshModifier.FINAL, int.class, "ordinal", false);

    //     // // TODO: os dois primeiros parâmetros de qualquer constructor de Enum VÃO ser 'name' e 'ordinal'
    //     // // TODO: talvez tratar isso no BshClassWritter ao invés de aqui ?

    //     // return new BshConstructor(
    //     //     BshModifier.PRIVATE,
    //     //     new BshParameter[] { nameParameter, ordinalParameter },
    //     //     new Type[0],
    //     //     true,
    //     //     0,
    //     //     () -> new Object[0],
    //     //     (cs) -> null
    //     // );
    // }

    /** */
    public synchronized Object eval(final CallStack callstack, final Interpreter interpreter ) throws EvalError {
        if (generatedClass == null) {
            generatedClass = generateClass(callstack, interpreter);
        }
        return generatedClass;
    }

    // TODO: adicionar generics nas classes
    // TODO: enums n podem possuir generics!!!!
    // TODO: adicionar validação de 'access modifers' no Modifiers!!!
    // public private static void ab() {}

    private Class<?> generateClass(final CallStack callStack, final Interpreter interpreter) throws EvalError {
        // int child = 0;

        // resolve superclass if any
        final int mods = this.getModifiers();
        final String name = this.getName(callStack.top());
        final Class<?> superClass = this.getSuperClass(callStack, interpreter);
        final Type[] interfaces = this.getInterfaces(callStack, interpreter);

        final NameSpace nameSpace = callStack.top();
        final Type bshClassType = new BshLazyType(name, 0);

        // final List<BshLocalMethod> meths = new ArrayList<>(0);

        // if (this.isInterface) {
        //     mods |= BshModifier.INTERFACE;
        //     superClass = Object.class;
        // } else if (this.isEnum) {
        //     mods |= BshModifier.ENUM;
        //     superClass = Enum.class;
        // } else {

        //     if (this.extend) {
        //         BSHAmbiguousName superNode = (BSHAmbiguousName)jjtGetChild(child++);
        //         superClass = superNode.toClass(callStack, interpreter);
    
        //         // Validate if can extend this class
        //         try {
        //             Interpreter.mainSecurityGuard.canExtends(superClass);
        //         } catch (UtilEvalError e) {
        //             throw e.toEvalError(this, callStack);
        //         }
    
        //         if (Reflect.isFinal(superClass))
        //             throw new EvalException("Cannot inherit from final class " + superClass.getName(), null, null);

        //     }

        //     // TODO: fazer a validação para evitar @Override de final methods!
        //     // TODO: a validação de final methods não deveria ser para todos os methods herdados ao invés de somente o superClass ???
        //     // if (Reflect.isGeneratedClass(superClass)) {
        //     //     // Validate final classes should not be extended
        //     //     if (Reflect.getClassModifiers(superClass).hasModifier("final"))
        //     //         throw new EvalException("Cannot inherit from final class " + superClass.getName(), null, null);
        //     //     // Collect final methods from all super class namespaces
        //     //     meths.addAll(Stream.of(Reflect.getDeclaredMethods(superClass))
        //     //         .filter(m->m.hasModifier("final")&&!m.hasModifier("private"))
        //     //         .collect(Collectors.toList()));
        //     // }
        // }

        // BSHBlock block = (BSHBlock) jjtGetChild(child);
        final int offset = this.extend ? 1+this.numInterfaces : this.numInterfaces;
        // Node[] children = this.jjtGetChildren();

        final BSHBlock block = this.jjtGetChild(offset);

        final ArrayList<BshEnumConstant> enumConstants = new ArrayList<>();
        final ArrayList<BshField> fields = new ArrayList<>();
        final ArrayList<BshMethod> methods = new ArrayList<>();
        final ArrayList<BshConstructor> constructors = new ArrayList<>();
        // TODO: testar a questão de ordem de initializers ( considerar os initializers de fields )
        final List<BshClass.InitializerFunction<?>> staticInitializers = new ArrayList<>();
        final List<BshClass.InitializerFunction<?>> initializers = new ArrayList<>();

        // TODO: validar que o BSHMethodDeclaration não pode definir um desses métodos implicitos de Enums!
        // TODO: se colocar em Reflect para invocar direto o BshMethod, validar que o body não pode ser null!
        // Add the inherit enum methods
        if (isEnum) {
            final int enumMethodsMods = BshModifier.PUBLIC | BshModifier.STATIC | BshModifier.FINAL;
            final BshMethod valuesMethod = new BshMethod(enumMethodsMods, new BshLazyType(name, 1), "values", new BshParameter[0], new Type[0], null);
            final BshMethod valueOfMethod = new BshMethod(enumMethodsMods, bshClassType, "valueOf", new BshParameter[] { BshParameter.IMPLICIT_ENUM_NAME_PARAMETER }, new Type[0], null);
            methods.add(valuesMethod);
            methods.add(valueOfMethod);
        }

        int enumConstantOrdinal = 0;

        final CallStack _callStack = callStack.copy();

        nameSpace.addType(bshClassType); // Add the type to be able to reference to the class as a type already

        // for (int i = offset; i < children.length; i++) {
            // Node n = children[i];
        for (Node n: block.jjtGetChildren()) {

            if (n instanceof BSHTypedVariableDeclaration) { // TODO: make test for 'loose typed' fields!
                final BSHTypedVariableDeclaration node = (BSHTypedVariableDeclaration) n;
                final BshField[] _fields = node.toFields(callStack, interpreter);
                fields.addAll(Arrays.asList(_fields));
                // final List<BshField> nodeFields = Arrays.asList(node.toFields(callStack, interpreter));
                // final List<BshInitializer> nodeInitializers = nodeFields.stream().map(BshInitializer::new).collect(Collectors.toList());

                // BshFunction<?, ?> _fieldsInitializer = (_class, thisArg) -> {
                //     for (BshField field: _fields) {
                //         field.initializer.apply((Class) _class, (Object) thisArg, _callStack, interpreter);
                //     }
                //     return null;
                // };

                // TDOO: como ficam as callStacks ? e o name space da classe ?
                if (node.modifiers.hasModifier("static")) // TODO: rever esse modifiers.hasModifier("static")
                    staticInitializers.add((bshClass, thisArg) -> node.initStaticFields(bshClass, _callStack.copy(), interpreter));
                else
                    initializers.add((bshClass, thisArg) -> node.initFields(bshClass, thisArg, _callStack.copy(), interpreter));

                // if (node.modifiers.hasModifier("static"))
                //     staticInitializers.addAll(nodeInitializers);
                // else
                //     initializers.addAll(nodeInitializers);

            } else if (n instanceof BSHMethodDeclaration) { // TODO: make tests for 'loose typed' methods!
                // TODO: testes unitários para métodos abstract!!
                // TODO: testes unitários para construtores abstract ??
                final BSHMethodDeclaration node = (BSHMethodDeclaration) n;

                // TODO: tlvz utilizar Lists e implementar um .equals() customizado para BshConstructor e BshMethod para fazer essa validação ?
                // TODO: utilizar HashMaps ou HashSet e implementar hashCode no BshConstructor e BshMethod para fazer a validação de não haver métodos duplicados ?

                // TODO: lançar erro caso não seja nem um método e nem um constructor ?
                if (node.isValidMethod(callStack, interpreter))
                    methods.add(node.toMethod(mods, callStack, interpreter));
                if (node.isValidConstructor(this.simpleName)) // TODO: considerar mods no caso se for enum ? no caso, só poderia se fosse private ou n tivesse acessor
                    constructors.add(node.toConstructor(callStack, interpreter, this.isEnum));

            } else if (n instanceof BSHClassDeclaration) { // TODO: fazer testes para inner classes e referências internas às inner classes
                // TODO: see it later...
                // TODO: adicionar o nameSpace.addType() aqui também!
            } else if (n instanceof BSHEnumConstant) {
                // TODO: see it later...
                final BSHEnumConstant node = (BSHEnumConstant) n;
                final BshEnumConstant enumConstant = node.toEnumConstant(callStack, interpreter, enumConstantOrdinal++);
                enumConstants.add(enumConstant);
                // fields.add(field);
                // staticInitializers.add(new BshInitializer(field));

                // staticInitializers.add((cs) -> node.initConstant(cs, interpreter));
            } else if (interpreter.getStrictJava()) {
                // TODO: lançar erro ? fazer eval ?
            } else if (n instanceof BSHBlock) {
                // TODO: impl it
            } else {
                System.out.println("Node --------->>> " + n.getClass().getName());
            }
        }

        // TODO: fazer teste unitários para construtores: acesso à construtores private e protected, construtores sendo chamados por inner classes, construtores gerados automaticamente
        // TODO: fazer testes para referências à inner classes

        // Try to add a default constructor
        if (constructors.size() == 0) {
            final NameSpace callingNS = callStack.top();
            final Constructor<?> defaultSuperConstructor = BshClassManager.memberCache.get(superClass).findConstructor(Reflect.ZERO_TYPES, callingNS);

            if (!this.isEnum) {
                if (defaultSuperConstructor == null) {
                    String msg = String.format("Implicit super constructor %s() is undefined for default constructor. Must define an explicit constructor", superClass.getName());
                    throw new EvalError(msg, this, callStack);
                }

                // TODO: testar bem isso
                if (defaultSuperConstructor.getGenericExceptionTypes().length != 0) {
                    // TODO: usar constructor.getGenericExceptionTypes() porém precisamos ver como ter um prettyName para isso??
                    final Class<?>[] exceptions = defaultSuperConstructor.getExceptionTypes();
                    final String[] exceptionNames = new String[exceptions.length];
                    for (int i = 0; i < exceptions.length; i++)
                        exceptionNames[i] = exceptions[i].getTypeName();
    
                    final String prefix = defaultSuperConstructor.getGenericExceptionTypes().length == 1 ? "type" : "types";
                    final String msg = String.format("Default constructor cannot handle exception %s %s thrown by implicit super constructor. Must define an explicit constructor", prefix, String.join(", ", exceptionNames));
                    throw new EvalError(msg, this, callStack);
                }
            }

            // TODO: o defaultConstructor de uma enum n deveria ser private ?
            constructors.add(this.isEnum ? BshConstructor.DEFAULT_ENUM_CONSTRUCTOR : BshConstructor.DEFAULT_CONSTRUCTOR);
        }

        // TODO: todos os construtores de uma enum n deveriam ser private ?
        // TODO: construtores de enum n podem ter super()!

        // nameSpace.removeType(bshClassType); // We don't need a dull class type anymore

        final BshClassManager bcm = interpreter.getClassManager();
        // final NameSpace bshClassNameSpace = new NameSpace(callStack.top(), ""); // TODO: esse 'name' faz sentido ?

        System.out.println("------------------------------------------");
        fields.forEach(f -> System.out.println("BshField -> " + f.name));
        System.out.println("------------------------------------------");

        // System.out.println("BSHClassDeclaration -> constructors: ");
        // for (BshConstructor constructor: constructors)
        //     System.out.println("- constructor.getTypes(): " + Arrays.asList(constructor.getParameterTypes()));

        // TODO: eu n preciso implementar os métodos "padrão" de enums ?
        BshClass bshClass = new BshClass(
            mods,
            name,
            new TypeVariable<?>[0],
            superClass,
            interfaces,
            enumConstants.toArray(new BshEnumConstant[0]),
            fields.toArray(new BshField[0]),
            constructors.toArray(new BshConstructor[0]),
            methods.toArray(new BshMethod[0]),
            staticInitializers.toArray(new BshClass.InitializerFunction[0]),
            initializers.toArray(new BshClass.InitializerFunction[0]),
            bcm
        );

        return bshClass.toClass();
    }

    public String toString() {
        return super.toString() + ": " + this.simpleName;
    }
}
