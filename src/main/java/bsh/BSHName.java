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

import java.lang.reflect.Type;

class BSHName extends SimpleNode {
    public String name;

    BSHName(int id) { super(id); }

    public Type _toType(CallStack callStack, Interpreter interpreter) throws EvalError {
        try {
            final NameSpace callingNS = callStack.top();
            final Name name = callingNS.getNameResolver(this.name);
            return name.toType();
        } catch ( ClassNotFoundException e ) {
            throw new EvalException( e.getMessage(), this, callStack, e );
        }
    }

    // TODO: não deveriamos precisar disso
    // // TODO: ver Type ao invés de Class<?>
    // // TODO: remover esse método ? o nome 'toClass' não faz sentido e vale lembrar que a implementação é no Name.toClass()!
    // public Class<?> _toClass(CallStack callStack, Interpreter interpreter) throws EvalError {
    //     try {
    //         final NameSpace callingNS = callStack.top();
    //         final Name name = callingNS.getNameResolver(this.name);
    //         return name._toClass();
    //     } catch ( ClassNotFoundException e ) {
    //         throw new EvalException( e.getMessage(), this, callStack, e );
    //     }
    //     // return Types.getRawType(this.toType(callStack, interpreter));
    // }

    // TODO: parecido com toClass, porém aqui podemos retornar qualquer Type
    // public Type toType(CallStack callStack, Interpreter interpreter) throws EvalError {
    //     try {
    //         final NameSpace callingNS = callStack.top();
    //         final Name name = callingNS.getNameResolver(this.name);
    //         // return name.toClass();
    //         return name.toClass();
    //     } catch (ClassNotFoundException e) {
    //         throw new EvalException( e.getMessage(), this, callStack, e );
    //     }
    // }

    public String toString() {
        return super.toString() + ": " + name;
    }
}

