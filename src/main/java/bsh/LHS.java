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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

import bsh.Types.MapEntry;

/**
    An LHS is a wrapper for an variable, field, or property.  It ordinarily
    holds the "left hand side" of an assignment and may be either resolved to
    a value or assigned a value.
    <p>

    There is one special case here termed METHOD_EVAL where the LHS is used
    in an intermediate evaluation of a chain of suffixes and wraps a method
    invocation.  In this case it may only be resolved to a value and cannot be
    assigned.  (You can't assign a value to the result of a method call e.g.
    "foo() = 5;").
    <p>
*/
@SuppressWarnings({ "unchecked" })
class LHS implements ParserConstants, Serializable {
    private static final long serialVersionUID = 1L;

    NameSpace nameSpace;
    /** The assignment should be to a local variable */
    // private boolean localVar;

    // TODO: verificar cada um desses!
    /** Identifiers for the various types of LHS. */
    static final int
        VARIABLE = 0,
        FIELD = 1,
        PROPERTY = 2,
        INDEX = 3,
        METHOD_EVAL = 4,
        // LOOSETYPE_FIELD = 5,
        MAP_ENTRY = 6;

    int type;

    private String varName;
    private Object key;
    // Invocable field;
    private Object object;
    private Entry<Object, Object> entry;
    private int index;
    // Variable var;

    private CallStack callStack; // TODO: see it; note: make a clone of the given callStack!
    private boolean strictJava; // TODO: see it

    /**
        @param localVar if true the variable is set directly in the This
        reference's local scope.  If false recursion to look for the variable
        definition in parent's scope is allowed. (e.g. the default case for
        undefined vars going to global).
    */
    // TODO: see it later
    // LHS(NameSpace nameSpace, String varName, boolean localVar) {
    LHS(NameSpace nameSpace, String varName, boolean strictJava) {
        type = VARIABLE;
        // this.localVar = localVar;
        this.varName = varName;
        this.nameSpace = nameSpace;
        this.strictJava = strictJava;
    }

    // LHS(NameSpace nameSpace, String varName) {
    //     type = LOOSETYPE_FIELD;
    //     this.varName = varName;
    //     this.nameSpace = nameSpace;
    // }

    // /** Static field LHS Constructor. This simply calls Object field constructor with null object. */
    // LHS( Invocable field ) {
    //     type = FIELD;
    //     this.object = field.getDeclaringClass();
    //     this.field = field;
    //     this.varName = field.getName();
    // }

    // /** Object field LHS Constructor. */
    // LHS( Object object, Invocable field ) {
    //     if ( object == null)
    //         throw new NullPointerException("constructed empty LHS");

    //     type = FIELD;
    //     this.object = object;
    //     this.field = field;
    //     if (null != field)
    //         this.varName = field.getName();
    // }

    /** Object property LHS Constructor with Object property. */
    LHS(Object object, Object key, CallStack callStack, boolean strictJava) {
        if( object == null )
            throw new NullPointerException("constructed empty LHS");

        type = PROPERTY;
        this.object = object;
        this.key = key;
        this.callStack = callStack.copy();
        this.strictJava = strictJava;
    }

    /** Map Entry type LHS Constructor.
     * The provided key is returned along with its value as a Map.Entry
     * entity during assignment. */
	LHS(Entry<?, ?> entry, boolean strictJava) {
        type = MAP_ENTRY; // TODO: remover isso daqui!!!!!
        this.entry = (Entry<Object, Object>) entry;
        this.strictJava = strictJava;
    }

    // LHS(Entry<?, ?>[] entries) {}
    // LHS(Map<?, ?> map, Object key) {}

    // TODO: verificar isso
    /** Array index LHS Constructor. */
    LHS(Object array, int index, boolean strictJava) {
        type = INDEX;
        this.object = array;
        this.index = index;
        this.strictJava = strictJava;
    }

    // public Object getValue() throws UtilEvalError {
    //     // TODO: ver isso
    //     // if ( type == FIELD ) {
    //     //     // Validate if can get this field
    //     //     if (Reflect.isStatic(field))
    //     //         Interpreter.mainSecurityGuard.canGetStaticField(field.getDeclaringClass(), field.getName());
    //     //     else
    //     //         Interpreter.mainSecurityGuard.canGetField(object, field.getName());
    //     // }

    //     return this.getValueImpl();
    // }

    public Object getValue() throws EvalError, UtilEvalError, ReflectError {
        if ( type == VARIABLE ) {
            // TODO: ver isso
            // return nameSpace.getVariableOrProperty(varName, null);
            // Object value = this.nameSpace.getVariable(varName);
            // if (value != Primitive.VOID) return value; // TODO: ver isto!

            // return Reflect.getNameSpaceProperty(nameSpace, varName);
            return Reflect.getNameSpaceVariable(this.nameSpace, this.varName, this.callStack, this.strictJava);
        }

        // if ( type == FIELD )
        //     // TODO: ver isso
        //     throw new RuntimeException("Not implemented yet!");
        //     // try {
        //     //     return Objects.requireNonNull(field, "get value, field cannot be null").invokeImpl(object);
        //     // } catch( ReflectiveOperationException e2 ) {
        //     //     throw new UtilEvalError("Can't read field: " + field, e2);
        //     // }

        if (type == PROPERTY) {
            if (this.strictJava) {
                if (this.key instanceof String)
                    try {
                        return Reflect.getField(this.object, (String) this.key, this.callStack, this.strictJava);
                    } catch (NoSuchFieldException e) {
                        throw new UtilEvalError(e.getMessage());
                    }
                throw new UtilEvalError("Can't get the field because the key can't be a field name!");
            }

            try {
                return Reflect.getField(this.object, (String) this.key, this.callStack, this.strictJava);
            } catch (NoSuchFieldException e) { // TODO: see it better later
                return Reflect.getObjectProperty(this.object, this.key, this.callStack, this.strictJava);
            }
        }

        if ( type == INDEX )
            try {
                return BshArray.getIndex(object, index);
            } catch(Exception e) {
                throw new UtilEvalError("Array access: " + e, e);
            }

        // if ( type == LOOSETYPE_FIELD )
        //     return nameSpace.getVariable( varName );

        throw new InterpreterError("LHS type");
    }

    // TODO: ver isso!
    public String getName() {
        // if ( null != field )
        //     return field.getName();
        // if ( null != var )
        //     return var.getName();
        return varName;
    }

    // TODO: ver isso
    public Class<?> getType() {
        // TODO: ver isso
        // if ( null != field )
        //     return field.getReturnType();
        // if ( null != getVariable() )
        //     return var.getType();
        try {
            return Types.getType(getValue());
        } catch (EvalError | UtilEvalError e) {
            return null;
        }
    }

    /** Overloaded assign with false as strict java.
     * @param val value to assign
     * @return result based on type
     * @throws UtilEvalError on exception */
    // public Object assign(Object val) throws UtilEvalError {
    //     return this.assign(val, false);
    // }

    /** Assign a value to the LHS. */
    public Object assign(Object val) throws EvalError, UtilEvalError, ReflectError {
        // switch (type) {
        //     case VARIABLE: System.out.printf("LHS -> setting VARIABLE -> val = %s\n", val);
        //     case FIELD: System.out.printf("LHS -> setting FIELD -> val = %s\n", val);
        //     case PROPERTY: System.out.printf("LHS -> setting PROPERTY -> val = %s\n", val);
        //     case INDEX: System.out.printf("LHS -> setting INDEX -> val = %s\n", val);
        //     case METHOD_EVAL: System.out.printf("LHS -> setting METHOD_EVAL -> val = %s\n", val);
        //     // case LOOSETYPE_FIELD: System.out.printf("LHS -> setting LOOSETYPE_FIELD -> val = %s\n", val);
        //     case MAP_ENTRY: System.out.printf("LHS -> setting MAP_ENTRY -> val = %s\n", val);
        // }

        // TODO: remover esses getValue() e getValueImpl() desnecessários! Fazer testes para garantir q não estamos fazendo chamadas demais e para coisas q n deveríamos!

        if (type == VARIABLE) {
            // if (this.nameSpace.hasVariable(this.varName)) {
            //     this.nameSpace.setVariable(this.varName, val, this.strictJava);
            //     return val;
            // } else {
            //     // TODO: isso n deveria ser !strictJava ?
            //     Reflect.setNameSpaceProperty(this.nameSpace, this.varName, val); // TODO: e se não houver um setter também ?
            //     return Reflect.getNameSpaceProperty(this.nameSpace, this.varName); // TODO: retornar Primitive.VOID quando não houver um getter ?
            // }
            return Reflect.setNameSpaceVariable(this.nameSpace, this.varName, val, callStack, this.strictJava);
            // return getValueImpl(); // TODO: see it later!
        }
        // else if (type == FIELD)
        //     // TODO: ver isso
        //     throw new RuntimeException("Not implemented yet!");
        //     // try {
        //     //     Objects.requireNonNull(field, "assign value, field cannot be null").invokeImpl( object, val);
        //     //     return getValueImpl();
        //     // } catch (ReflectiveOperationException e) {
        //     //     throw new UtilEvalError("LHS ("+field.getName()+") can't access field: "+e, e);
        //     // }
        else if (type == PROPERTY) {
            if (this.strictJava) {
                if (this.key instanceof String)
                    try {
                        return Reflect.setField(this.object, (String) this.key, val, this.callStack, this.strictJava);
                    } catch (NoSuchFieldException e) {
                        throw new UtilEvalError(e.getMessage());
                    }
                throw new UtilEvalError("Can't set the field because the key isn't a String!");
            }

            try {
                return Reflect.setField(this.object, (String) this.key, val, this.callStack, this.strictJava);
            } catch (NoSuchFieldException e) { // TODO: see it better later
                Reflect.setObjectProperty(this.object, this.key, val, this.callStack, this.strictJava);
                return Reflect.getObjectProperty(this.object, this.key, this.callStack, this.strictJava);
            }

            // try {
            //     if (key instanceof String)
            //         return  Reflect.setObjectProperty(object, (String) key, val);
            //     return  Reflect.setObjectProperty(object, key, val);
            // } catch(ReflectError e) {
            //     Interpreter.debug("Assignment: " + e.getMessage());
            //     throw new UtilEvalError("No such property: " + key, e);
            // }
        }
        else if (type == INDEX)
            try {
                if (object.getClass().isArray() && val != null)
                    try {
                        val = Types.castObject(val, Types.arrayElementType(object.getClass()), Types.ASSIGNMENT);
                    } catch (Exception e) { /* ignore cast exceptions */ }

                BshArray.setIndex(object, index, val);
            } catch ( UtilTargetError e1 ) { // pass along target error
                if ( IndexOutOfBoundsException.class.isAssignableFrom(e1.getCause().getClass()) )
                    throw new UtilEvalError("Error array set index: "+e1.getMessage(), e1);
                throw e1;
            } catch ( Exception e ) {
                throw new UtilEvalError("Assignment: " + e.getMessage(), e);
            // } else if ( type == LOOSETYPE_FIELD ) {
            //     Modifiers mods = new Modifiers(Modifiers.FIELD);
            //     mods.addModifier("public");
            //     if ( nameSpace.isInterface )
            //         mods.setConstant();
            //     nameSpace.setTypedVariable(varName, Types.getType(val), val, mods);
            //     return val;
            }
        else if (type == MAP_ENTRY) // TODO: e se quisermos setar um field de uma entry ? Fazer testes para isso tb!
            return this.entry.setValue(val); // TODO: fazer um teste unitário verificando q o retorno do .assign() vai ser o valor anterior
        else
            throw new InterpreterError("unknown lhs type");
        return val;
    }

    public String toString() {
        return "LHS: "
            // +((field!=null)? "field = "+field.toString():"") // TODO: ver isso!
            +(varName!=null ? " varName = "+varName: "")
            +(nameSpace!=null ? " nameSpace = "+nameSpace.toString(): "");
    }

    /** Reflect Field is not serializable, hide it.
     * @param s serializer
     * @throws IOException mandatory throwing exception */
    private synchronized void writeObject(final ObjectOutputStream s) throws IOException {
        // TODO: ver isso
        // if ( null != field ) {
        //     this.object = field.getDeclaringClass();
        //     this.varName = field.getName();
        //     this.field = null;
        // }
        // s.defaultWriteObject();
    }

    /** Fetch field removed from serializer.
     * @param in secializer.
     * @throws IOException mandatory throwing exception
     * @throws ClassNotFoundException mandatory throwing exception  */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if ( null == this.object )
            return;
        Class<?> cls = this.object.getClass();
        if ( this.object instanceof Class )
            cls = (Class<?>) this.object; // TODO: ver a questão dos getter e setter criados ( desativar para strictJava )
        // TODO: ver isso
        // this.field = BshClassManager.memberCache.get(cls).findField(varName);
    }
}

