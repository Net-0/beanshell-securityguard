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

import java.io.Serializable;

// TODO: ver otimizações para essa classe!
public class Variable implements Serializable, BshClassManager.Listener {
    // private static final int DECLARATION=0;
    // public static final int ASSIGNMENT=1;

    /** A null type means an untyped variable */
    String name;
    Class<?> type;
    // String typeDescriptor;
    private Object value;
    Modifiers modifiers;
    // LHS lhs;

    // Variable(String name, Class<?> type, LHS lhs) {
    //     this.name = name;
    //     this.lhs = lhs;
    //     this.type = type;
    // }

    // Variable(String name, Object value, Modifiers modifiers) throws UtilEvalError {
    //     this( name, (Class<?>) null/*type*/, value, modifiers );
    // }

    // /** This constructor is used in class generation. */
    // Variable(String name, String typeDescriptor, Object value, Modifiers modifiers) throws UtilEvalError {
    //     this( name, (Class<?>) null/*type*/, value, modifiers );
    //     this.typeDescriptor = typeDescriptor;
    // }

    // Variable(String name, Class<?> type, Modifiers modifiers) {
    //     this.name = name;
    //     this.type = type;
    //     this.modifiers = modifiers;
    // }

    Variable(String name, Class<?> type, Object value, Modifiers modifiers) throws UtilEvalError {
        this.name=name;
        this.type = type;
        this.modifiers = modifiers;
        // this.setValue(value, DECLARATION);

        // // prevent final variable re-assign
        // if (hasModifier("final")) {
        //     if (this.value != null)
        //         throw new UtilEvalError("Cannot re-assign final variable "+name+".");
        //     if (value == null)
        //         return;
        // }

        // TODO: should add isJavaCastable() test for strictJava
        // (as opposed to isJavaAssignable())
        // if (type != null && type != Object.class && value != null) {
        //     // final int ope = context == DECLARATION ? Types.CAST : Types.ASSIGNMENT;
        //     this.value = Types.castObject(value, type, Types.CAST);
        //     return;
        // }

        this.value = type != null && type != Object.class && value != null
                        ? this.value = Types.castObject(value, type, Types.CAST)
                        : value;
    }

    public Object getValue() { return this.value; }

    public Object setValue(Object value) throws UtilEvalError {
        // this.setValue(value, DECLARATION);

        // TODO: verificar melhor isso!
        // prevent final variable re-assign
        if (this.hasModifier("final")) {
            if (this.value != null)
                throw new UtilEvalError("Cannot re-assign final variable "+name+".");
            if (value == null) // TODO: pq temos isso?
                return null;
        }

        // TODO: should add isJavaCastable() test for strictJava
        // (as opposed to isJavaAssignable())
        if ( type != null && type != Object.class && value != null )
            return this.value = Types.castObject(value, type, Types.ASSIGNMENT);
        else
            return this.value = this.value == null ? Primitive.getDefaultValue(type) : value;
    }

    // TODO: see it better!
    /**
        Set the value of the typed variable.
        @param value should be an object or wrapped bsh Primitive type.
        if value is null the appropriate default value will be set for the
        type: e.g. false for boolean, zero for integer types.
    */
    // private void setValue(Object value, int context) throws UtilEvalError {
    //     // prevent final variable re-assign
    //     if (hasModifier("final")) {
    //         if (this.value != null)
    //             throw new UtilEvalError("Cannot re-assign final variable "+name+".");
    //         if (value == null)
    //             return;
    //     }

    //     // TODO: should add isJavaCastable() test for strictJava
    //     // (as opposed to isJavaAssignable())
    //     if ( type != null && type != Object.class && value != null ) {
    //         final int ope = context == DECLARATION ? Types.CAST : Types.ASSIGNMENT;
    //         this.value = Types.castObject(value, type, ope);
    //         return;
    //     }

    //     this.value = this.value == null && context != DECLARATION ? Primitive.getDefaultValue(type) : value;
    // }

    // void validateFinalIsSet(boolean isStatic) {
    //     if (!hasModifier("final") || this.value != null)
    //         return;
    //     if (isStatic == hasModifier("static"))
    //         throw new RuntimeException((isStatic ? "Static f" : "F")
    //                 +"inal variable "+name+" is not initialized.");
    // }

    public String getName() { return name; }
    public Class<?> getType() { return type; } // TODO: talvez adicionar um Types.getType(this.value) aqui ?

    // public String getTypeDescriptor() {
    //     if (null == typeDescriptor)
    //         typeDescriptor = Type.getDescriptor(type == null ? Object.class : type);
    //     return typeDescriptor;
    // }

    public Modifiers getModifiers() {
        if (this.modifiers == null)
            this.modifiers = new Modifiers(Modifiers.FIELD);
        return this.modifiers;
    }

    // private void setModifiers(Modifiers modifiers) {
    //     this.modifiers = modifiers;
    // }

    public boolean hasModifier(String name) {
        return getModifiers().hasModifier(name);
    }

    public void setConstant() {
        if (hasModifier("private") || hasModifier("protected"))
            throw new IllegalArgumentException("Illegal modifier for interface field "
                    + getName() + ". Only public static & final are permitted.");
        getModifiers().setConstant();
    }

    public String toString() {
        return "Variable: " + StringUtil.variableString(this) + ", value:" + value /*+ ", lhs = " + lhs*/;
    }

    /** {@inheritDoc} */
    @Override
    public void classLoaderChanged() {
        // TODO: see it!
        // if (Reflect.isGeneratedClass(type)) try {
        //     type = Reflect.getThisNS(type).getClass(type.getName());
        // } catch (UtilEvalError e) { /** should not happen on reload */ }
    }
}
