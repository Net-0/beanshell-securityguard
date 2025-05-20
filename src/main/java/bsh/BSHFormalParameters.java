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

import java.util.Arrays;
import java.util.List;

import bsh.internals.BshParameter;

class BSHFormalParameters extends SimpleNode {
    // private String[] paramNames;
    // private Modifiers[] paramModifiers;
    // private boolean listener; // TODO: ver isso
    /** For loose type parameters the paramTypes are null. */
    // unsafe caching of types
    // Class<?>[] paramTypes;
    // int numArgs;
    // String[] typeDescriptors;
    // boolean isVarArgs;

    BSHFormalParameters(int id) { super(id); }

    // void insureParsed() {
    //     if ( paramNames != null ) return;

    //     this.numArgs = jjtGetNumChildren();
    //     String[] paramNames = new String[numArgs];
    //     Modifiers[] paramModifiers = new Modifiers[numArgs];

    //     for(int i=0; i<numArgs; i++)
    //     {
    //         BSHFormalParameter param = (BSHFormalParameter)jjtGetChild(i);
    //         isVarArgs = param.isVarArgs;
    //         paramNames[i] = param.name;
    //         paramModifiers[i] = new Modifiers(Modifiers.PARAMETER);
    //         if (param.isFinal)
    //             paramModifiers[i].addModifier("final");
    //     }
    //     this.paramNames = paramNames;
    //     this.paramModifiers = paramModifiers;
    // }

    // TODO: revisar esses métodos!
    // public Modifiers [] getParamModifiers() {
    //     insureParsed();
    //     return paramModifiers;
    // }

    // public String [] getParamNames() {
    //     insureParsed();
    //     return paramNames;
    // }

    // public String[] getTypeDescriptors(CallStack callstack, Interpreter interpreter, String defaultPackage) {
    //     if ( typeDescriptors != null )
    //         return typeDescriptors;

    //     insureParsed();
    //     String [] typeDesc = new String[numArgs];

    //     for(int i=0; i<numArgs; i++)
    //     {
    //         BSHFormalParameter param = (BSHFormalParameter)jjtGetChild(i);
    //         typeDesc[i] = param.getTypeDescriptor(
    //             callstack, interpreter, defaultPackage );
    //     }

    //     this.typeDescriptors = typeDesc;
    //     return typeDesc;
    // }

    // TODO: BSHFormalParameters não deveria ter um .eval()!
    // /**
    //     Evaluate the types.
    //     Note that type resolution does not require the interpreter instance.
    // */
    // public Object eval(CallStack callstack, Interpreter interpreter) throws EvalError {
    //     if ( paramTypes != null )
    //         return paramTypes;

    //     insureParsed();
    //     Class<?>[] paramTypes = new Class[numArgs];

    //     for (int i=0; i<numArgs; i++) {
    //         BSHFormalParameter param = (BSHFormalParameter)jjtGetChild(i);
    //         paramTypes[i] = (Class<?>)param.eval( callstack, interpreter );
    //     }

    //     return this.paramTypes = paramTypes;
    // }

    // Returns if some parameter is loose-typed
    protected boolean isLooseTyped() {
        // final BSHFormalParameter[] nodes = this.jjtGetChildren();
        // for (final BSHFormalParameter node: nodes)
        //     if (node.isLooseTyped())
        //         return true;
        for (final Node node: this.jjtGetChildren())
            if (((BSHFormalParameter) node).isLooseTyped())
                return true;
        return false;
    }

    protected BshParameter[] toParameters(CallStack callStack, Interpreter interpreter, boolean isEnumConstructor) throws EvalError {
        final int offset = isEnumConstructor ? 2 : 0;
        final Node[] nodes = this.jjtGetChildren();
        final BshParameter[] params = new BshParameter[nodes.length + offset];

        if (isEnumConstructor) {
            params[0] = BshParameter.IMPLICIT_ENUM_NAME_PARAMETER;
            params[1] = BshParameter.IMPLICIT_ENUM_ORDINAL_PARAMETER;
        }

        for (int i = 0; i < nodes.length; i++)
            params[i + offset] = ((BSHFormalParameter) nodes[i]).toParameter(callStack, interpreter);
        return params;
    }

    // // TODO: ver isso
    // /** Property getter for listener.
    //  * @return boolean return the listener */
    // public boolean isListener() {
    //     return listener;
    // }

    // // TODO: ver isso
    // /** Property setter for listener.
    //  * @param listener the listener to set */
    // public void setListener(boolean listener) {
    //     this.listener = listener;
    // }

    // // TODO: ver isso
    // /** {@inheritDoc} */
    // @Override
    // public void classLoaderChanged() {
    //     // paramTypes = null;
    // }

}

