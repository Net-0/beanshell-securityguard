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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bsh.ClassGenerator.Type;

/**
*/
class BSHClassDeclaration extends SimpleNode
{
    /**
        The class instance initializer method name.
        A BshMethod by this name is installed by the class delcaration into
        the static class body namespace.
        It is called once to initialize the static members of the class space
        and each time an instances is created to initialize the instance
        members.
    */
    static final String CLASSINITNAME = "_bshClassInit";

    String name;
    Modifiers modifiers = new Modifiers(Modifiers.CLASS);
    String[] interfacesNames;
    String superClassName;
    Type type;
    private Class<?> generatedClass;

    BSHClassDeclaration(int id) { super(id); }

    /**
    */
    public synchronized Object eval(final CallStack callstack, final Interpreter interpreter ) throws EvalError {
        if (generatedClass == null) {
            generatedClass = generateClass(callstack, interpreter);
        }
        return generatedClass;
    }


    private Class<?> generateClass(final CallStack callstack, final Interpreter interpreter) throws EvalError {
        try {
            int child = 0;
            NameSpace nameSpace = callstack.top();
    
            // resolve superclass if any
            Class<?> superClass = null;
            final List<BshMethod> meths = new ArrayList<>(0);
            if ( this.superClassName != null ) {
                superClass = nameSpace.getClassStrict(this.superClassName);
                // if (superClass == null) throw new EvalError("", this, )
                if (Reflect.isGeneratedClass(superClass)) {
                    // Validate final classes should not be extended
                    if (Reflect.getClassModifiers(superClass).hasModifier("final"))
                        throw new EvalError("Cannot inherit from final class "
                            + superClass.getName(), null, null);
                    // Collect final methods from all super class namespaces
                    meths.addAll(Stream.of(Reflect.getDeclaredMethods(superClass))
                        .filter(m->m.hasModifier("final")&&!m.hasModifier("private"))
                        .collect(Collectors.toList()));
                }
            }
    
            // Get interfaces
            Class<?>[] interfaces = new Class[this.interfacesNames.length];
            for( int i=0; i < interfaces.length; i++) {
                // BSHAmbiguousName node = (BSHAmbiguousName)jjtGetChild(child++);
                // interfaces[i] = node.toClass(callstack, interpreter);
                String interfaceName = this.interfacesNames[i];
                interfaces[i] = nameSpace.getClassStrict(interfaceName);
                if (!interfaces[i].isInterface())
                    throw new EvalError("Type: "+interfaceName+" is not an interface!", this, callstack );
            }
    
            BSHBlock block = (BSHBlock) jjtGetChild(child);
    
            if (type == Type.INTERFACE) // this should ideally happen in the parser
                    modifiers.changeContext(Modifiers.INTERFACE);
    
            Class<?> clas = ClassGenerator.getClassGenerator().generateClass(
                name, modifiers, interfaces, superClass, block, type,
                callstack, interpreter );
    
            // Validate final methods should not be overridden
            for (BshMethod m : meths)
               if (null != Reflect.getDeclaredMethod(clas, m.getName(), m.getParameterTypes()))
                   throw new EvalError("Cannot override "+m.getName()+"() in " +
                       StringUtil.typeString(superClass) + " overridden method is final", null, null);
    
            return clas;
        } catch (UtilEvalError e) {
            throw e.toEvalError(this, callstack);
        }
    }

    public String toString() {
        return super.toString() + ": " + name;
    }
}
