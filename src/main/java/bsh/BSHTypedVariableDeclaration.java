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
 * Author of Learning Java, O'Reilly & Associates                            *
 *                                                                           *
 *****************************************************************************/


package bsh;

import bsh.internals.BshField;

class BSHTypedVariableDeclaration extends SimpleNode {
    private static final long serialVersionUID = 1L;
    public Modifiers modifiers = new Modifiers(Modifiers.FIELD);
    private BSHVariableDeclarator[] bvda;

    BSHTypedVariableDeclaration(int id) { super(id); }

    protected BSHType getTypeNode() {
        return ((BSHType)jjtGetChild(0));
    }

    Class<?> evalType(CallStack callstack, Interpreter interpreter) throws EvalError {
        BSHType typeNode = getTypeNode();
        return typeNode.getType( callstack, interpreter );
    }

    BSHVariableDeclarator[] getDeclarators() {
        if (null != bvda) return bvda;
        int n = jjtGetNumChildren();
        int start=1;
        bvda = new BSHVariableDeclarator[n-start];
        for (int i = start; i < n; i++)
            bvda[i-start] = (BSHVariableDeclarator)jjtGetChild(i);
        return bvda;
    }

    /** evaluate the type and one or more variable declarations, e.g.: int a, b=5, c; */
    public Object eval(CallStack callStack, Interpreter interpreter) throws EvalError {
        Object value = Primitive.VOID;
        try {
            final NameSpace nameSpace = callStack.top();
            final BSHType typeNode = getTypeNode();
            final Class<?> type = typeNode.getType(callStack, interpreter);

            // BSHVariableDeclarator[] bvda = getDeclarators();
            // for (int i = 0; i < bvda.length; i++) {
                // BSHVariableDeclarator dec = bvda[i];
            for (final BSHVariableDeclarator dec: this.getDeclarators()) {

                // Type node is passed down the chain for array initializers
                // which need it under some circumstances
                value = dec.eval(typeNode, modifiers, callStack, interpreter);
                try {
                    // TODO: see it!
                    // LHS lhs = null;
                    // if ( namespace.isClass )
                    //     if ( null != namespace.classInstance )
                    //         lhs = new LHS(
                    //             namespace.classInstance,
                    //             Reflect.resolveJavaField(namespace.classStatic, dec.name, modifiers.hasModifier("static"))
                    //         );
                    //     else
                    //         lhs = new LHS(
                    //             namespace.classStatic,
                    //             Reflect.resolveJavaField(namespace.classStatic, dec.name, modifiers.hasModifier("static"))
                    //         );

                    // TODO: see it!
                    // if ( null != lhs && null != lhs.field ) {
                    //     Variable var = new Variable(dec.name, type, lhs);
                    //     var.modifiers = modifiers;
                    //     var.setValue(value, Variable.ASSIGNMENT);
                    //     namespace.setVariableImpl(var);
                    // } else {
                        // if (interpreter.getStrictJava() && value instanceof Primitive && ((Primitive) value).isNumber())
                        //     value = Primitive.castNumberStrictJava(type, ((Primitive) value).numberValue());
                        // namespace.setTypedVariable(dec.name, type, value, modifiers);
                        // if (!namespace.isMethod) // TODO: see it later!
                        //     interpreter.getClassManager().addListener(namespace.getVariableImpl(dec.name, false));
                    // }
                    // if (!namespace.isClass) // TODO: see it later?
                        // value = namespace.getVariable(dec.name);

                    // TODO: isso seria strictJava ?
                    // TODO: ver uma mensagem de erro!
                    if (nameSpace.hasLocalVariable(dec.name))
                        throw new EvalError("", this, callStack);

                    // System.out.println(")))))))))))))))))))))");
                    // System.out.println("BSHTypedVariableDeclaration.eval(): ");
                    // System.out.println(" - nameSpace: " + nameSpace);
                    // System.out.println(" - dec.name: " + dec.name);
                    // System.out.println(" - value: " + value);
                    // System.out.println(")))))))))))))))))))))");
                    // TODO: e a questão do 'type' ? Como fica para variáveis q possuem os 2 tipos de assinatura de array ?
                    value = nameSpace.setLocalVariable(dec.name, type, value, this.modifiers);
                } catch (UtilEvalError e) {
                    throw e.toEvalError(this, callStack);
                }
            }
        } catch (EvalError e) {
            throw e.reThrow( "Typed variable declaration" );
        }
        return value;
    }

    protected BshField[] toFields(CallStack callstack, Interpreter interpreter) throws EvalError {
        BSHVariableDeclarator[] variables = this.getDeclarators();
        BshField[] fields = new BshField[variables.length];
        BSHType typeNode = this.getTypeNode();
        for (int i = 0; i < variables.length; i++)
            fields[i] = variables[i].toField(typeNode, modifiers, callstack, interpreter);
        return fields;
    }

    protected void initStaticFields(CallStack callStack, Interpreter interpreter) throws EvalError {
        final NameSpace nameSpace = callStack.top();
        // System.out.println("BSHTypedVariableDeclaration.initStaticFields(): ");
        // System.out.println(" - nameSpace: " + nameSpace.getName());
        final Class<?> _class = nameSpace.declaringClass.toClass();
        final BSHType typeNode = getTypeNode();
        
        for (final BSHVariableDeclarator varNode: this.getDeclarators()) {
            final Object value = varNode.eval(typeNode, this.modifiers, callStack, interpreter);
            try {
                Reflect.setStaticField(_class, varNode.name, value, callStack);
            } catch (UtilEvalError e) {
                throw e.toEvalError(this, callStack);
            } catch (NoSuchFieldException e) {
                throw new EvalError(e.getMessage(), this, callStack);
            }
        }
    }

    protected void initFields(CallStack callStack, Interpreter interpreter) throws EvalError {
        final BSHType typeNode = getTypeNode();
        // final This _this = callStack.top().getThis();
        final This _this = callStack.top()._this; // TODO: testar isso ?

        for (final BSHVariableDeclarator varNode: this.getDeclarators()) {
            final Object value = varNode.eval(typeNode, this.modifiers, callStack, interpreter);
            try {
                Reflect.setField(_this, varNode.name, value, callStack, interpreter.getStrictJava());
            } catch (UtilEvalError e) {
                throw e.toEvalError(this, callStack);
            } catch (NoSuchFieldException e) {
                throw new EvalError(e.getMessage(), this, callStack);
            }
        }
    }

    public String toString() {
        return super.toString() + ": " + modifiers;
    }
}
