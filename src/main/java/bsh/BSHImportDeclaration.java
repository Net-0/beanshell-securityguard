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
    public boolean importPackage;
    public boolean staticImport;
    public boolean superImport;

    BSHImportDeclaration(int id) { super(id); }

    public Object eval(final CallStack callStack, final Interpreter interpreter) throws EvalError {
        final NameSpace nameSpace = callStack.top();
        // TODO: how improve super import ?
        if (superImport) {
            if (interpreter.getStrictJava())
                throw new EvalError("Super import is not java strict!", this, callStack);

            try {
                nameSpace.doSuperImport();
            } catch ( UtilEvalError e ) {
                throw e.toEvalError( this, callStack  );
            }

            return Primitive.VOID;
        }

        final String name = ((BSHName) this.jjtGetChild(0)).name;

        // TODO: rever os imports static: mudar a implementação e remover os códigos antigos
        if (staticImport) {
            if (importPackage)
                nameSpace.importStaticClass(name);
            else
                nameSpace.importStaticMember(name);
        } else { // import package
            if (importPackage)
                nameSpace.importPackage(name);
            else
                nameSpace.importClass(name);
        }

        return Primitive.VOID;
    }

    @Override
    public String toString() {
        return super.toString() + ": static=" + staticImport + ", *=" + importPackage + ", super import=" + superImport;
    }
}

