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

import java.lang.reflect.Array;
import java.lang.reflect.Type;

import bsh.internals.Types;

// TODO: implementar suporte à generics, wildcards, etc...
class BSHType extends SimpleNode
// implements BshClassManager.Listener
{
    private static final long serialVersionUID = 1L;
    // /**
    //     baseType is used during evaluation of full type and retained for the
    //     case where we are an array type.
    //     In the case where we are not an array this will be the same as type.
    // */
    // private Class<?> baseType;
    /**
        If we are an array type this will be non zero and indicate the
        dimensionality of the array.  e.g. 2 for String[][];
    */
    private int arrayDims;

    // TODO: type cache should be done by NameSpace not by BSHType
    // /**
    //     Internal cache of the type.  Cleared on classloader change.
    // */
    // private Class<?> type;

    // /** Flag to track if instance is already a listener */
    // private boolean isListener = false;

    // String descriptor;

    BSHType(int id) { super(id); }

    /**
        Used by the grammar to indicate dimensions of array types
        during parsing.
    */
    public void addArrayDimension() {
        arrayDims++;
    }

    // Node getTypeNode() {
    //     return jjtGetChild(0);
    // }

    // TODO: considerar aqui a questão de fields, parameters e variaveis que podem ter definição de dimensão depois do nome!

    // // TODO: implementar. A ideia é que esse cara retorne a classe em questão msm, sem BshLazyType nem nada, a classe direto.
    // public Class<?> toClass(int extraArrayDimensions, CallStack callStack, Interpreter interpreter) throws EvalError {
    //     // // return cached type if available
    //     // if (type != null)
    //     //     return type;

    //     // first node will either be PrimitiveType or AmbiguousName
    //     final Node baseTypeNode = this.jjtGetChild(0);

    //     if ( baseTypeNode instanceof BSHPrimitiveType )
    //         baseType = ((BSHPrimitiveType)baseTypeNode).type;
    //     else
    //         try {
    //             baseType = ((BSHName)baseTypeNode).toClass(callstack, interpreter);
    //         } catch (EvalError e) {
    //             // TODO: isso é um PÉSSIMO SUPORTE À GENERICS! Generics podem ter nomes maiores doq só uma única letra!
    //             // TODO: fazer testes unitários bem melhores de generics, incluindo generics 
    //             // Assuming generics raw type
    //             if (baseTypeNode.getText().trim().length() == 1 && e.getCause() instanceof ClassNotFoundException)
    //                 baseType = Object.class;
    //             else
    //                 throw e; // roll up unhandled error
    //         }

    //     if ( arrayDims > 0 ) {
    //         try {
    //             // Get the type by constructing a prototype array with
    //             // arbitrary (zero) length in each dimension.
    //             int[] dims = new int[arrayDims]; // int array default zeros
    //             // TODO: base type pode ser null ??
    //             Object obj = Array.newInstance(null == baseType ? Object.class : baseType, dims);
    //             type = obj.getClass();
    //         } catch(Exception e) {
    //             throw new EvalException("Couldn't construct array type", this, callstack, e);
    //         }
    //     } else
    //         type = baseType;

    //     // add listener to reload type if class is reloaded see #699
    //     if (!isListener) { // only add once
    //         interpreter.getClassManager().addListener(this);
    //         isListener = true;
    //     }

    //     return type;

    //     return null;
    // }

    // TODO: implementar. A ideia é que esse cara retorno qualquer type, porém deve incluir estrtura de generics!
    // public Class<?> toType(int extraArrayDimensions, CallStack callStack, Interpreter interpreter) throws EvalError {

    //     return null;
    // }

    public Type _toType(CallStack callstack, Interpreter interpreter) throws EvalError {
        // // return cached type if available
        // if (type != null) return type;

        // // first node will either be PrimitiveType or AmbiguousName
        // Node node = this.jjtGetChild(0);
        // if (node instanceof BSHPrimitiveType)
        //     baseType = ((BSHPrimitiveType)node).getType();
        // else
        //     try {
        //         baseType = ((BSHName)node)._toClass(callstack, interpreter);
        //     } catch (EvalError e) {
        //         // TODO: isso é um PÉSSIMO SUPORTE À GENERICS! Generics podem ter nomes maiores doq só uma única letra!
        //         // TODO: fazer testes unitários bem melhores de generics, incluindo generics 
        //         // Assuming generics raw type
        //         if (node.getText().trim().length() == 1 && e.getCause() instanceof ClassNotFoundException)
        //             baseType = Object.class;
        //         else
        //             throw e; // roll up unhandled error
        //     }

        // if (arrayDims > 0) {
        //     try {
        //         // Get the type by constructing a prototype array with
        //         // arbitrary (zero) length in each dimension.
        //         int[] dims = new int[arrayDims]; // int array default zeros
        //         // TODO: base type pode ser null ??
        //         Object obj = Array.newInstance(null == baseType ? Object.class : baseType, dims);
        //         type = obj.getClass();
        //     } catch(Exception e) {
        //         throw new EvalException("Couldn't construct array type", this, callstack, e);
        //     }
        // } else
        //     type = baseType;

        // // add listener to reload type if class is reloaded see #699
        // if (!isListener) { // only add once
        //     interpreter.getClassManager().addListener(this);
        //     isListener = true;
        // }

        // return type;

        // TODO: e quanto haver generics ?

        // first node will either be PrimitiveType or AmbiguousName
        final Node node = this.jjtGetChild(0);
        final Type baseType = node instanceof BSHPrimitiveType
                                    ? ((BSHPrimitiveType) node).type
                                    : ((BSHName) node)._toType(callstack, interpreter);


        // TODO: e os generics ?

        return this.arrayDims > 0
                ? Array.newInstance(Types.getRawType(baseType), new int[arrayDims]).getClass()
                : baseType;
    }

    // // TODO: terminar de ver essa 
    // public Class<?> _toRawType(CallStack callstack, Interpreter interpreter) throws EvalError {

    //     // first node will either be PrimitiveType or AmbiguousName
    //     final Node node = this.jjtGetChild(0);
    //     final Type baseType = node instanceof BSHPrimitiveType
    //                                 ? ((BSHPrimitiveType) node).type
    //                                 : ((BSHName) node)._toType(callstack, interpreter);
    //     final Class<?> rawBaseType = Types.getRawType(baseType);

    //     return this.arrayDims > 0
    //             ? Array.newInstance(rawBaseType, new int[arrayDims]).getClass()
    //             : rawBaseType;
    // }

    // // TODO: n deveria retornar um Type ao invés de Class<?>
    // public Class<?> getType(CallStack callstack, Interpreter interpreter) throws EvalError {
    //     // return cached type if available
    //     if ( type != null )
    //         return type;

    //     // first node will either be PrimitiveType or AmbiguousName
    //     Node node = this.jjtGetChild(0);
    //     if ( node instanceof BSHPrimitiveType )
    //         baseType = ((BSHPrimitiveType)node).getType();
    //     else
    //         try {
    //             baseType = ((BSHName)node).toClass(callstack, interpreter);
    //         } catch (EvalError e) {
    //             // TODO: isso é um PÉSSIMO SUPORTE À GENERICS! Generics podem ter nomes maiores doq só uma única letra!
    //             // TODO: fazer testes unitários bem melhores de generics, incluindo generics 
    //             // Assuming generics raw type
    //             if (node.getText().trim().length() == 1 && e.getCause() instanceof ClassNotFoundException)
    //                 baseType = Object.class;
    //             else
    //                 throw e; // roll up unhandled error
    //         }

    //     if ( arrayDims > 0 ) {
    //         try {
    //             // Get the type by constructing a prototype array with
    //             // arbitrary (zero) length in each dimension.
    //             int[] dims = new int[arrayDims]; // int array default zeros
    //             // TODO: base type pode ser null ??
    //             Object obj = Array.newInstance(null == baseType ? Object.class : baseType, dims);
    //             type = obj.getClass();
    //         } catch(Exception e) {
    //             throw new EvalException("Couldn't construct array type", this, callstack, e);
    //         }
    //     } else
    //         type = baseType;

    //     // add listener to reload type if class is reloaded see #699
    //     if (!isListener) { // only add once
    //         interpreter.getClassManager().addListener(this);
    //         isListener = true;
    //     }

    //     return type;
    // }

    // TODO: remover esses métodos abaixos

    // TODO: remover isso
    /**
        baseType is used during evaluation of full type and retained for the
        case where we are an array type.
        In the case where we are not an array this will be the same as type.
    */
    public Class<?> getBaseType() {
        return baseType;
    }

    // TODO: remover isso
    /**
        If we are an array type this will be non zero and indicate the
        dimensionality of the array.  e.g. 2 for String[][];
    */
    public int getArrayDims() {
        // return arrayDims;
        return null;
    }

    // /** Clear instance cache to reload types on class loader change #699 */
    // public void classLoaderChanged() {
    //     type = null;
    //     baseType = null;
    // }
}
