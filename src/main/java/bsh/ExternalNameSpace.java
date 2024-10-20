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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;


/**
    A namespace which maintains an external map of values held in variables in
    its scope.  This mechanism provides a standard collections based interface
    to the namespace as well as a convenient way to export and view values of
    the namespace without the ordinary BeanShell wrappers.
    </p>

    Variables are maintained internally in the normal fashion to support
    meta-information (such as variable type and visibility modifiers), but
    exported and imported in a synchronized way.  Variables are exported each
    time they are written by BeanShell.  Imported variables from the map appear
    in the BeanShell namespace as untyped variables with no modifiers and
    shadow any previously defined variables in the scope.
    <p/>

    Note: this class is inherentely dependent on Java 1.2 (for Map), however
    it is not used directly by the core as other than type NameSpace, so no
    dependency is introduced.
*/
/*
    Implementation notes:

    It would seem that we should have been accomplished this by overriding the
    getImportedVar() method of NameSpace, which behaves in a similar way
    for fields of classes and objects.  However we need more control here to
    be able to bump up the precedence and remove items that have been removed
    via the map.  So we override getVariableImp().  We should reevaluate this
    at some point.  All of NameSpace is a mess.

    The primary abstraction here is that we override createVariable() to
    create LHS Variables bound to the map for this namespace.

    Methods:

    bsh methods are not currently exported to the
    external namespace.  All that would be required to add this is to override
    setMethod() and provide a friendlier view than vector (currently used) for
    overloaded forms (perhaps a map by method SignatureKey).

*/
public class ExternalNameSpace extends NameSpace {
    private Map<String,Object> externalMap;

    public ExternalNameSpace() {
        this(null, "External Map Namespace", null);
    }

    public ExternalNameSpace(NameSpace parent, String name, Map<String,Object> externalMap) {
        super(parent, name);
        this.externalMap = externalMap == null ? new HashMap<>() : externalMap;
    }

    /** Get the map view of this namespace. */
    public Map<String,Object> getMap() { return this.externalMap; }

    /**
        Set the external Map which to which this namespace synchronizes.
        The previous external map is detached from this namespace.  Previous
        map values are retained in the external map, but are removed from the
        BeanShell namespace.
    */
    public void setMap(Map<String,Object> map) {
        // Detach any existing namespace to preserve it, then clear this
        // namespace and set the new one
        this.clear();
        this.externalMap = map;
    }

    @Override
    public Object getLocalVariable(String name) throws NoSuchElementException {
        return this.externalMap.get(name);
    }

    @Override
    public Object setLocalVariable(String name, Class<?> type, Object value, Modifiers mods) throws UtilEvalError {
        return this.externalMap.put(name, value);
    }

    // TODO: ver esses métodos!
    @Override
    protected ExternalNameSpace clone() {
        final ExternalNameSpace clone = (ExternalNameSpace) super.clone();
        clone.externalMap = new HashMap<>(this.externalMap);
        return clone;
    }

    // // TODO: ver isso
    // @Override
    // protected void getAllNamesAux(List<String> vec) { super.getAllNamesAux(vec); }

    // @Override
    // public Class<?> getClass(String name) {
    //     // TODO Auto-generated method stub
    //     return super.getClass(name);
    // }

    // @Override
    // protected Variable getVariableImpl(String name, boolean recurse) throws UtilEvalError {
    //     // TODO Auto-generated method stub
    //     return super.getVariableImpl(name, recurse);
    // }

    @Override
    public void unsetVariable(String name) {
        super.unsetVariable(name);
        this.externalMap.remove(name);
    }

    // TODO: ver isso
    @Override
    public String[] getAllNames() {
        return ExternalNameSpace.concatNames(super.getAllNames(), this.externalMap.keySet());
    }

    // TODO: testar isso!
    @Override
    public String[] getVariableNames() {
        return ExternalNameSpace.concatNames(super.getVariableNames(), this.externalMap.keySet());
    }

    private static final String[] concatNames(final String[] names1, final Set<String> names2) {
        final String[] names = new String[names1.length + names2.size()];
        names2.toArray(names); // Copy into the begin of 'names'
        System.arraycopy(names1, 0, names, names2.size(), names1.length); // Copy into the end of 'names'
        return names;
    }

    // /*
    //     Notes: This implementation of getVariableImpl handles the following
    //     cases:
    //     1) var in map not in local scope - var was added through map
    //     2) var in map and in local scope - var was added through namespace
    //     3) var not in map but in local scope - var was removed via map
    //     4) var not in map and not in local scope - non-existent var

    //     Note: It would seem that we could simply override getImportedVar()
    //     in NameSpace, rather than this higher level method.  However we need
    //     more control here to change the import precedence and remove variables
    //     if they are removed via the extenal map.
    // */
    // protected Variable getVariableImpl(String name, boolean recurse) throws UtilEvalError {
    //     // check the external map for the variable name
    //     Object value = this.externalMap.get(name);

    //     if (value == null && this.externalMap.containsKey(name))
    //         value = Primitive.NULL;

    //     // Variable var;
    //     if (value == null) {
    //         // The var is not in external map and it should therefore not be
    //         // found in local scope (it may have been removed via the map).
    //         // Clear it prophalactically.
    //         super.unsetVariable(name);

    //         // Search parent for var if applicable.
    //         // var = super.getVariableImpl(name, recurse);
    //         return super.getVariableImpl(name, recurse);
    //     } else {
    //         // Var in external map may be found in local scope with type and
    //         // modifier info.
    //         Variable localVar = super.getVariableImpl(name, false);

    //         // If not in local scope then it was added via the external map,
    //         // we'll wrap it and pass it along.  Else we'll use the one we
    //         // found.
    //         // if ( localVar == null )
    //         //     var = createVariable( name, null/*type*/, value, null/*mods*/ );
    //         // else
    //         //     var = localVar;
    //         return localVar == null ? createVariable(name, null, value, null) : localVar;
    //     }

    //     // return var;
    // }

    // private static final CallStack EMPTY_STACK = new CallStack();

    // @Override
    // protected Variable createVariable(String name, Class<?> type, Object value, Modifiers mods) {
    //     // TODO: o strictJava deveria ser apenas 'false' mesmo ?
    //     LHS lhs = new LHS(externalMap, name, EMPTY_STACK, false);
    //     // Is this race condition worth worrying about?
    //     // value will appear in map before it's really in the interpreter
    //     try {
    //         lhs.assign(value);
    //     } catch ( UtilEvalError e) {
    //         throw new InterpreterError(e.toString());
    //     }
    //     return new Variable(name, type, lhs);

    //     return new VariableProxy(this.externalMap, name, type, value, mods);
    // }

    /**
        Clear all variables, methods, and imports from this namespace and clear
        all values from the external map (via Map clear()).
    */
    public void clear() {
        super.clear();
        this.externalMap.clear();
    }

}

