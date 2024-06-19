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

import bsh.org.objectweb.asm.Type;

class BSHType extends SimpleNode implements BshClassManager.Listener {

    private static final long serialVersionUID = 1L;

    /**
        baseType is used during evaluation of full type and retained for the
        case where we are an array type.
        In the case where we are not an array this will be the same as type.
    */
    Class<?> baseType;

    /**
        If we are an array type this will be non zero and indicate the
        dimensionality of the array.  e.g. 2 for String[][];
    */
    int arrayDimensions;

    /** Internal cache of the type.  Cleared on classloader change. */
    Class<?> type;

    /** Flag to track if instance is already a listener */
    private boolean isListener = false;

    /** It's the baseType name */
    String baseName;
    // String descriptor;

    BSHType(int id) { super(id); }

    // /**
    //     Used by the grammar to indicate dimensions of array types
    //     during parsing.
    // */
    // public void addArrayDimension() { arrayDimensions++; }

    // Node getTypeNode() { return jjtGetChild(0); }

    /**
         Returns a class descriptor for this type.
         If the type is an ambiguous name (object type) evaluation is
         attempted through the namespace in order to resolve imports.
         If it is not found and the name is non-compound we assume the default
         package for the name.
    */
    public String getTypeDescriptor(CallStack callStack, Interpreter interpreter, String defaultPackage ) {
        return null;
        // // return cached type if available
        // if ( descriptor != null )
        //     return descriptor;

        // String descriptor;
        // //  first node will either be PrimitiveType or AmbiguousName
        // Node node = getTypeNode();
        // if ( node instanceof BSHPrimitiveType )
        //     descriptor = getTypeDescriptor( ((BSHPrimitiveType)node).type );
        // else
        // {
        //     String clasName = ((BSHAmbiguousName)node).text;
        //     String innerClass = callStack.top().importedClasses.get(clasName);

        //     Class<?> clas = null;
        //     if ( innerClass == null ) try {
        //         clas = ((BSHAmbiguousName)node).toClass( callStack, interpreter );
        //     } catch ( EvalError e ) {
        //         // Lets assume we have a generics raw type
        //         if (clasName.length() == 1)
        //             clasName = "java.lang.Object";
        //     } else
        //         clasName = innerClass.replace('.', '$');

        //     if ( clas != null ) {
        //         descriptor = getTypeDescriptor( clas );
        //     } else {
        //         if ( defaultPackage == null || Name.isCompound( clasName ) )
        //             descriptor = "L" + clasName.replace('.','/') + ";";
        //         else
        //             descriptor =
        //                 "L"+defaultPackage.replace('.','/')+"/"+clasName + ";";
        //     }
        // }

        // for(int i=0; i<arrayDimensions; i++)
        //     descriptor = "["+descriptor;

        // this.descriptor = descriptor;
        // return descriptor;
    }

    public Class<?> getType( CallStack callStack, Interpreter interpreter ) throws EvalError {
        try {
            // return cached type if available
            if (this.type != null ) return this.type;
    
            this.baseType = this.evalBaseType(callStack.top());
    
            if (this.arrayDimensions > 0) {
                Object array = Array.newInstance(this.baseType, new int[this.arrayDimensions]);
                this.type = array.getClass();
            } else {
                this.type = this.baseType;
            }
    
            // add listener to reload type if class is reloaded see #699
            if (!this.isListener) { // only add once
                interpreter.getClassManager().addListener(this);
                this.isListener = true;
            }
    
            return this.type;
        } catch (UtilEvalError e) {
            throw e.toEvalError(this, callStack);
        }
    }

    private Class<?> evalBaseType(NameSpace nameSpace) throws UtilEvalError {
        System.out.println("BSHType.evalBaseType() ("+this+") -> this.baseName: " + this.baseName);
        switch (this.baseName) {
            case "boolean": return Boolean.TYPE;
            case "char": return Character.TYPE;
            case "byte": return Byte.TYPE;
            case "short": return Short.TYPE;
            case "int": return Integer.TYPE;
            case "long": return Long.TYPE;
            case "float": return Float.TYPE;
            case "double": return Double.TYPE;
            default: {
                try {
                    return nameSpace.getClassStrict(this.baseName);
                } catch (UtilEvalError e) {
                    // TODO: mustn't all generic types be declared in class NameSpace and be resolved by it ?
                    // Assuming generics raw type
                    if (!this.baseName.contains(".")) return Object.class;
                    throw e;
                }
            }
        }
    }

    /**
        baseType is used during evaluation of full type and retained for the
        case where we are an array type.
        In the case where we are not an array this will be the same as type.
    */
    public Class<?> getBaseType() {
        if (this.baseType != null) return this.baseType;

        switch (this.baseName) {
            case "boolean": this.baseType = Boolean.TYPE; break;
            case "char": this.baseType = Character.TYPE; break;
            case "byte": this.baseType = Byte.TYPE; break;
            case "short": this.baseType = Short.TYPE; break;
            case "int": this.baseType = Integer.TYPE; break;
            case "long": this.baseType = Long.TYPE; break;
            case "float": this.baseType = Float.TYPE; break;
            case "double": this.baseType = Double.TYPE; break;
            // TODO: ver isso
            // default: this.baseType = //t=<COMPLEX_IDENTIFIER>
        }

        return baseType;
    }

    /** Clear instance cache to reload types on class loader change #699 */
    public void classLoaderChanged() {
        type = null;
        baseType = null;
    }

    // TODO: delete this method
    public static String getTypeDescriptor( Class<?> clas )
    {
        return Type.getDescriptor(clas);
        // if ( clas == Boolean.TYPE ) return "Z";
        // if ( clas == Character.TYPE ) return "C";
        // if ( clas == Byte.TYPE ) return "B";
        // if ( clas == Short.TYPE ) return "S";
        // if ( clas == Integer.TYPE ) return "I";
        // if ( clas == Long.TYPE ) return "J";
        // if ( clas == Float.TYPE ) return "F";
        // if ( clas == Double.TYPE ) return "D";
        // if ( clas == Void.TYPE ) return "V";

        // String name = clas.getName().replace('.','/');

        // if ( name.startsWith("[") || name.endsWith(";") )
        //     return name;
        // else
        //     return "L"+ name.replace('.','/') +";";
    }

    public ClassIdentifier getClassIdentifier(NameSpace nameSpace) throws EvalError {
        CallStack callStack = new CallStack(nameSpace);
        Interpreter interpreter = new Interpreter(nameSpace);
        Class<?> clazz = this.getType(callStack, interpreter);
        return new ClassIdentifier(clazz);
    }
}
