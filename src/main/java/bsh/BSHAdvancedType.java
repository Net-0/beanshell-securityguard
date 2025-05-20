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

// TODO: implementar suporte à generics, wildcards, etc...
class BSHAdvancedType extends SimpleNode implements BshClassManager.Listener {
    private static final long serialVersionUID = 1L;
    /**
        baseType is used during evaluation of full type and retained for the
        case where we are an array type.
        In the case where we are not an array this will be the same as type.
    */
    private Class<?> baseType;

    // // TODO: ver isso
    // /** Internal cache of the type.  Cleared on classloader change. */
    // private Class<?> type;

    // /** Flag to track if instance is already a listener */
    // private boolean isListener = false;

    String descriptor;

    BSHAdvancedType(int id) { super(id); }

    // /**
    //  Used by the grammar to indicate dimensions of array types
    //     during parsing.
    // */
    // public void addArrayDimension() {
    //     arrayDims++;
    // }

    Node getTypeNode() {
        return jjtGetChild(0);
    }

    public Class<?> getType(CallStack callstack, Interpreter interpreter) throws EvalError {
        // // return cached type if available
        // if ( type != null )
        //     return type;

        // // first node will either be PrimitiveType or AmbiguousName
        // Node node = getTypeNode();
        // if ( node instanceof BSHPrimitiveType )
        //     baseType = ((BSHPrimitiveType)node).getType();
        // else
        //     try {
        //         baseType = ((BSHName)node).toClass(callstack, interpreter);
        //     } catch (EvalError e) {
        //         // Assuming generics raw type
        //         if (node.getText().trim().length() == 1 && e.getCause() instanceof ClassNotFoundException)
        //             baseType = Object.class;
        //         else
        //             throw e; // roll up unhandled error
        //     }

        // if ( arrayDims > 0 ) {
        //     try {
        //         // Get the type by constructing a prototype array with
        //         // arbitrary (zero) length in each dimension.
        //         int[] dims = new int[arrayDims]; // int array default zeros
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
        return null;
    }

    /**
        baseType is used during evaluation of full type and retained for the
        case where we are an array type.
        In the case where we are not an array this will be the same as type.
    */
    public Class<?> getBaseType() {
        return baseType;
    }

    /** Clear instance cache to reload types on class loader change #699 */
    public void classLoaderChanged() {
        // type = null;
        // baseType = null;
    }
}
