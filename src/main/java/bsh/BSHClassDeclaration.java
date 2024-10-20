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
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
// import java.util.stream.Collectors;
// import java.util.stream.Stream;
// import static bsh.ClassGenerator.Type;

import bsh.internals.BshClass;
import bsh.internals.BshConstructor;
import bsh.internals.BshConsumer;
import bsh.internals.BshField;
import bsh.internals.BshFunction;
import bsh.internals.BshMethod;
import bsh.internals.BshModifier;

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
    private Class<?> getSuperClass(CallStack callStack, Interpreter interpreter) throws EvalError {
        if (this.isEnum) return Enum.class; // TODO: Enum recebe um generic, implementar um suporte para isso!
        if (this.isInterface || !this.extend) return Object.class;

        final BSHName superClassNode = this.jjtGetChild(0);
        final Class<?> superClass = superClassNode.toClass(callStack, interpreter);
        if (Reflect.isFinal(superClass))
            throw new EvalException("Cannot inherit from final class " + superClass.getName(), null, null);

        // Validate if can extend this class
        try {
            Interpreter.mainSecurityGuard.canExtends(superClass);
        } catch (UtilEvalError e) {
            throw e.toEvalError(this, callStack);
        }
        return superClass == null ? Object.class : superClass;
    }

    private Class<?>[] getInterfaces(CallStack callStack, Interpreter interpreter) throws EvalError {
        final Class<?>[] interfaces = new Class[numInterfaces];
        final Node[] children = this.jjtGetChildren();
        final int offset = this.extend ? 1 : 0;

        for (int i=0; i < numInterfaces; i++) {
            BSHName node = (BSHName) children[i + offset];
            interfaces[i] = node.toClass(callStack, interpreter);

            if (!interfaces[i].isInterface())
                throw new EvalException("Type: "+node.name+" is not an interface!", this, callStack );

            // Validate if can implement this interface
            try {
                Interpreter.mainSecurityGuard.canImplements(interfaces[i]);
            } catch (UtilEvalError e) {
                throw e.toEvalError(this, callStack);
            }
        }
        return interfaces;
    }

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
        final Class<?>[] interfaces = this.getInterfaces(callStack, interpreter);

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

        final ArrayList<BshField> fields = new ArrayList<>();
        final ArrayList<BshMethod> methods = new ArrayList<>();
        final ArrayList<BshConstructor> constructors = new ArrayList<>();
        final List<BshConsumer<CallStack>> staticInitializers = new ArrayList<>();
        final List<BshConsumer<CallStack>> initializers = new ArrayList<>();

        // for (int i = offset; i < children.length; i++) {
            // Node n = children[i];
        for (Node n: block.jjtGetChildren()) {

            if (n instanceof BSHTypedVariableDeclaration) { // TODO: make test for 'loose typed' fields!
                final BSHTypedVariableDeclaration node = (BSHTypedVariableDeclaration) n;
                fields.addAll(Arrays.asList(node.toFields(callStack, interpreter)));

                if (node.modifiers.hasModifier("static")) // TODO: rever esse modifiers.hasModifier("static")
                    staticInitializers.add((cs) -> node.initStaticFields(cs, interpreter));
                else
                    initializers.add((cs) -> node.initFields(cs, interpreter));

            } else if (n instanceof BSHMethodDeclaration) { // TODO: make tests for 'loose typed' methods!
                // TODO: testes unitários para métodos abstract!!
                // TODO: testes unitários para construtores abstract ??
                final BSHMethodDeclaration node = (BSHMethodDeclaration) n;

                // TODO: lançar erro caso não seja nem um método e nem um constructor ?
                if (node.isValidMethod(callStack, interpreter))
                    methods.add(node.toMethod(callStack, interpreter));
                if (node.isValidConstructor(this.simpleName))
                    constructors.add(node.toConstructor(callStack, interpreter));

            } else if (n instanceof BSHClassDeclaration) { // TODO: fazer testes para inner classes e referências internas às inner classes
                // TODO: see it later...
            } else if (n instanceof BSHEnumConstant) {
                // TODO: see it later...
                final BSHEnumConstant node = (BSHEnumConstant) n;
                fields.add(node.toField(name, callStack, interpreter));

                staticInitializers.add((cs) -> node.initConstant(cs, interpreter));
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
            Constructor<?> constructor = BshClassManager.memberCache.get(superClass).findConstructor(Reflect.ZERO_TYPES, callingNS);
            if (constructor == null) {
                String msg = String.format("Implicit super constructor %s() is undefined for default constructor. Must define an explicit constructor", superClass.getName());
                throw new EvalError(msg, this, callStack);
            }

            if (constructor.getGenericExceptionTypes().length != 0) {
                // TODO: usar constructor.getGenericExceptionTypes() porém precisamos ver como ter um prettyName para isso??
                final Class<?>[] exceptions = constructor.getExceptionTypes();
                final String[] exceptionNames = new String[exceptions.length];
                for (int i = 0; i < exceptions.length; i++)
                    exceptionNames[i] = exceptions[i].getTypeName();

                final String prefix = constructor.getGenericExceptionTypes().length == 1 ? "type" : "types";
                final String msg = String.format("Default constructor cannot handle exception %s %s thrown by implicit super constructor. Must define an explicit constructor", prefix, String.join(", ", exceptionNames));
                throw new EvalError(msg, this, callStack);
            }

            constructors.add(BshConstructor.defaultConstructor());
        }

        BshClassManager bcm = interpreter.getClassManager();
        NameSpace nameSpace = new NameSpace(callStack.top(), ""); // TODO: esse 'name' faz sentido ?

        BshClass bshClass = new BshClass(
            mods,
            name,
            new TypeVariable<?>[0],
            superClass,
            interfaces,
            fields.toArray(new BshField[0]),
            constructors.toArray(new BshConstructor[0]),
            methods.toArray(new BshMethod[0]),
            staticInitializers.toArray(new BshConsumer[0]),
            initializers.toArray(new BshConsumer[0]),
            interpreter,
            nameSpace,
            bcm
        );

        return bshClass.toClass();
    }

    public String toString() {
        return super.toString() + ": " + this.simpleName;
    }
}
