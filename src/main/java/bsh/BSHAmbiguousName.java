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

class BSHAmbiguousName extends SimpleNode {
    public String name;

    BSHAmbiguousName(int id) { super(id); }

    public Name getName(NameSpace namespace) { // TODO: faz + sentido termos o 'Name' pois isso seria cache ( + performático ) ?
        return namespace.getNameResolver( name );
    }

    // TODO: precisamos criar uma instância de bsh.Name para isso ??
    //  - Note: temos o BSHName e o BSHAmbiguousName, cada um não pode tratar sua lógica por si só ?
    public Object toObject(CallStack callstack, Interpreter interpreter) throws EvalError {
        try {
            return getName(callstack.top()).toObject(callstack, interpreter);
        } catch ( UtilEvalError e ) {
            throw e.toEvalError( this, callstack );
        }
    }

    // Object toObject(CallStack callstack, Interpreter interpreter, boolean forceClass) throws EvalError {
    //     // TODO: ver isso
    //     throw new RuntimeException("Not implemented yet!");
    //     // try {
    //     //     // return getName( callstack.top() ).toObject( callstack, interpreter, forceClass );
    //     // } catch ( UtilEvalError e ) {
    //     //     throw e.toEvalError( this, callstack );
    //     // }
    // }

    // TODO: ainda precisamos disso ? agr temos o BSHName
    // TODO: remover esse método ? o nome 'toClass' não faz sentido e vale lembrar que a implementação é no Name.toClass()!
    public Class<?> toClass(CallStack callstack, Interpreter interpreter) throws EvalError {
        try {
            return getName( callstack.top() ).toClass();
        } catch ( ClassNotFoundException e ) {
            throw new EvalException( e.getMessage(), this, callstack, e );
        }
    }

    // TODO: remover isso, um set ambiguous name pode ser resolvido simplesmente dando um .substring() e removendo a última parte do ambiguousName e dando um Reflect.setField()!!!!
    public LHS toLHS(CallStack callstack, Interpreter interpreter) throws EvalError {
        try {
            return getName( callstack.top() ).toLHS( callstack, interpreter );
        } catch ( UtilEvalError e ) {
            throw e.toEvalError( this, callstack );
        }
    }

    // TODO: mudar a descrição, BSHAmbiguousName não é um node executável, é uma expressão à ser resolvida e usada!
    /*
        The interpretation of an ambiguous name is context sensitive.
        We disallow a generic eval( ).
    */
    public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
        // throw new InterpreterError("Don't know how to eval an ambiguous name! Use toObject() if you want an object." );
        return this.toObject(callstack, interpreter);
    }

    public String toString() {
        return super.toString() + ": " + name;
    }
}

