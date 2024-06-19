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

class BSHImportDeclaration extends SimpleNode {
    private static final long serialVersionUID = 1L;
    String name;
    boolean importPackage;
    boolean staticImport;
    boolean superImport;

    BSHImportDeclaration(int id) { super(id); }

    @Override
    public Object eval(CallStack callStack, Interpreter interpreter) throws EvalError {
        try {
            NameSpace nameSpace = callStack.top();
            if (!name.contains("."))
                throw new EvalError("Import can be resolved because the name is invalid", this, callStack);

            // e.g.: import *;
            if (superImport) {
                nameSpace.doSuperImport();
                return Primitive.VOID;
            }

            if ( staticImport ) {
                if ( importPackage ) {
                    // e.g.: import static java.util.stream.Collectors.*;
                    Class<?> clas = nameSpace.getClassStrict(this.name);
                    nameSpace.importStatic(clas);
                    return Primitive.VOID;
                }

                // e.g.: import static java.util.stream.Collectors.toList;
                this.importStaticMember(nameSpace, callStack);
                return Primitive.VOID;
            }

            if ( importPackage ) nameSpace.importPackage(this.name); // e.g.: import java.util.*;
            else nameSpace.importClass(this.name); // e.g.: import java.util.ArrayList;

            return Primitive.VOID;
        } catch ( UtilEvalError e ) {
            throw e.toEvalError(this, callStack);
        }
    }

    private void importStaticMember(NameSpace nameSpace, CallStack callStack) {
        Object obj = null;
        Class<?> clas = null;
        final int lastDot = this.name.lastIndexOf('.');
        final String className = this.name.substring(0, lastDot);
        final String memberName = this.name.substring(lastDot+1);

        try { // import static method from class
            clas = nameSpace.getClassStrict(className);
            obj = Reflect.staticMethodImport(clas, memberName);
        } catch (Exception e) {}

        try { // import static field from class
            if (null != clas && null == obj)
                obj = Reflect.getLHSStaticField(clas, memberName);
        } catch (Exception e) {}

        // TODO: see it?
        // try { // import static method from Name
        //     if (null == obj)
        //         obj = ambigName.toObject( callStack, interpreter );
        // } catch (Exception e) { /* ignore try field instead */ }

        // TODO: see it
        // do we have a method
        if (obj instanceof BshMethod) {
            nameSpace.setMethod((BshMethod) obj);
            return;
        }

        // TODO: see it?
        // if ( !(obj instanceof LHS) )
        //     // import static field from Name
        //     obj = ambigName.toLHS( callStack, interpreter );

        // TODO: see it
        // do we have a field
        if ( obj instanceof LHS && ((LHS) obj).isStatic() ) {
            nameSpace.setVariableImpl( ((LHS) obj).getVariable() );
            return;
        }

        // no static member found
        //throw new EvalError(this.name + " is not a static member of a class", this, callStack );
    }

    @Override
    public String toString() {
        return super.toString() + ": static=" + staticImport + ", *=" + importPackage + ", super import=" + superImport;
    }
}

