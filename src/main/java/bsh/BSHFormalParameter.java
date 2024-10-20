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

import bsh.internals.BshModifier;
import bsh.internals.BshParameter;

/**
    A formal parameter declaration.
    For loose variable declaration type is null.
*/
class BSHFormalParameter extends SimpleNode {
    public String name;
    boolean isFinal = false;
    boolean isVarArgs = false;

    // TODO: ver isso, isso é mt importante!! Pelo jeito isso n funcionaria:
    //  - void main(String args[])
    //  - void main(String[] argss[])
    int dimensions = 0;

    BSHFormalParameter(int id) { super(id); }

    // public String getTypeDescriptor(CallStack callstack, Interpreter interpreter, String defaultPackage) {
    //     if ( jjtGetNumChildren() > 0 )
    //         return (isVarArgs ? "[" : "") +
    //         ((BSHType)jjtGetChild(0)).getTypeDescriptor(callstack, interpreter, defaultPackage);
    //     else
    //         // this will probably not get used
    //         return  (isVarArgs ? "[" : "") +"Ljava/lang/Object;";  // Object type
    // }

    // TODO: remover isso, um BSHFormalParameter não deveria ter um .eval()
    // /** Evaluate the type. */
    // public Object eval( CallStack callstack, Interpreter interpreter) throws EvalError {
    //     if ( jjtGetNumChildren() > 0 )
    //         type = ((BSHType)jjtGetChild(0)).getType( callstack, interpreter );
    //     else
    //         type = null;

    //     if (isVarArgs) // TODO: isso não estoura NullPointerException para loose-typed parameter ?
    //         type = Array.newInstance(type, 0).getClass();

    //     return type;
    // }

    protected final boolean isLooseTyped() {
        return this.jjtGetNumChildren() == 0;
    }

    private final BSHType getTypeNode() {
        return (BSHType) this.jjtGetChild(0);
    }

    protected BshParameter toParameter(CallStack callstack, Interpreter interpreter) throws EvalError {
        // TODO: implementar generics
        final Class<?> baseType = !this.isLooseTyped() ? this.getTypeNode().getType(callstack, interpreter) : null;
        final Class<?> type = this.isVarArgs ? Types.arrayType(baseType) : baseType;
        final int modifiers = this.isFinal ? BshModifier.FINAL : 0;

        return new BshParameter(modifiers, this.name, type, this.isVarArgs);
    }

    @Override
    public String toString() {
        return super.toString() + ": " + name + ", final=" + isFinal + ", varargs=" + isVarArgs;
    }
}

