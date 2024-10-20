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

import java.util.concurrent.atomic.AtomicInteger;

import bsh.internals.BshLocalMethod;
import bsh.util.ReferenceCache;
import bsh.util.ReferenceCache.Type;

/**
    A specialized namespace for Blocks (e.g. the body of a "for" statement).
    The Block acts like a child namespace but only for typed variables
    declared within it (block local scope) or untyped variables explicitly set
    in it via setBlockVariable().  Otherwise variable assignment
    (including untyped variable usage) acts like it is part of the containing
    block.
    <p>
*/
/*
    Note: This class essentially just delegates most of its methods to its
    parent.  The setVariable() indirection is very small.  We could probably
    fold this functionality back into the base NameSpace as a special case.
    But this has changed a few times so I'd like to leave this abstraction for
    now.
*/
class BlockNameSpace extends NameSpace {
    /** Atomic block count of unique block instances. */
    public static final AtomicInteger blockCount = new AtomicInteger();

    /** Atomic reuse count per instance. */
    public final AtomicInteger used = new AtomicInteger(1);

    /** Unique key for cached name spaces. */
    private static final class UniqueBlock {
        NameSpace ns;
        int id;
        /** Unique block consists of a namespace and unique id.
         * @param ns the Namespace parent
         * @param id a unique id for block namespaces */
        UniqueBlock(NameSpace ns, int id) {
            this.ns = ns;
            this.id = id;
        }

        /** Return a calculated hash code from name space and block id.
         * {@inheritDoc} */
        @Override
        public int hashCode() { return ns.hashCode() + id; }
    }

    /** Weak reference cache for reusable block namespaces */
    private static final ReferenceCache<UniqueBlock, BlockNameSpace> blockspaces
        = new ReferenceCache<UniqueBlock, BlockNameSpace>(Type.Weak, Type.Weak, 100) {
            /** Create block namespace based on unique block key as required */
            protected BlockNameSpace create(UniqueBlock key) {
                return new BlockNameSpace(key.ns, key.id);
            }
    };

    /** Static method to get a unique block name space. With the
     * supplied namespace as parent and unique block id obtained
     * from blockCount.
     * @param parent name space
     * @param blockId unique id for block
     * @return new or cached instance of a unique block name space */
    public static BlockNameSpace getInstance(NameSpace parent, int blockId ) {
        BlockNameSpace ns = BlockNameSpace.blockspaces.get(new UniqueBlock(parent, blockId));
        if (1 < ns.used.getAndIncrement()) ns.clear();
        return ns;
    }

    /** Public constructor to create a non cached instance.
     * @param parent name space
     * @param blockId unique id for block */
    public BlockNameSpace(NameSpace parent, int blockId) {
        super( parent, parent.getName()+ "/BlockNameSpace" + blockId );
        this.isMethod = parent.isMethod;
    }

    // TODO: implementar os métodos de getLocalVariable() e setLocalVariable()!

    // /**
    //     Override the standard namespace behavior to make assignments
    //     happen in our parent (enclosing) namespace, unless the variable has
    //     already been assigned here via a typed declaration or through
    //     the special setBlockVariable() (used for untyped args in try/catch).
    //     <p>
    //     i.e. only allow typed var declaration to happen in this namespace.
    //     Typed vars are handled in the ordinary way local scope.  All untyped
    //     assignments are delegated to the enclosing context.
    // */
    // /*
    //     Note: it may see like with the new 1.3 scoping this test could be
    //     removed, but it cannot.  When recurse is false we still need to set the
    //     variable in our parent, not here.
    // */
    // @Override
    // public Variable setVariableImpl(String name, Object value, boolean strictJava, boolean recurse) throws UtilEvalError {
    //     // System.out.printf("setVariable() -> name = \"%s\", weHaveVar = %s\n", name, weHaveVar(name));
    //     if ( this.hasLocalVariable(name) )
    //         // set the var here in the block namespace
    //         return super.setVariableImpl( name, value, strictJava, false );
    //     else
    //         // set the var in the enclosing (parent) namespace
    //         return getParent().setVariableImpl( name, value, strictJava, recurse );
    // }

    @Override
    public Object setLocalVariable(String name, Class<?> type, Object value, Modifiers mods) throws UtilEvalError {
        if (this.hasLocalVariable(name))
            return super.setLocalVariable(name, type, value, mods);
        else
            return this.getParent().setLocalVariable(name, type, value, mods);
    }

    // /**
    //     Set an untyped variable in the block namespace.
    //     The BlockNameSpace would normally delegate this set to the parent.
    //     Typed variables are naturally set locally.
    //     This is used in try/catch block argument.
    // */
    public void setBlockVariable(String name, Object value) throws UtilEvalError {
        super.setLocalVariable(name, null, value, null);
    }

    /**
        We have the variable: either it was declared here with a type, giving
        it block local scope or an untyped var was explicitly set here via
        setBlockVariable().
    */
    // private boolean weHaveVar(String name) {
    //     // super.variables.containsKey( name ) not any faster, I checked
    //     // return super.getVariableImpl(name, false) != null;
    //     return this.hasLocalVariable(name);
    // }

    // /** do we need this? */
    // private NameSpace getNonBlockParent() {
    //     NameSpace parent = super.getParent();
    //     if ( parent instanceof BlockNameSpace )
    //         return ((BlockNameSpace)parent).getNonBlockParent();
    //     else
    //         return parent;
    // }

    // /**
    //     Get a 'this' reference is our parent's 'this' for the object closure.
    //     e.g. Normally a 'this' reference to a BlockNameSpace (e.g. if () { } )
    //     resolves to the parent namespace (e.g. the namespace containing the
    //     "if" statement).
    //     @see #getBlockThis( Interpreter )
    // */
    // public This getThis( Interpreter declaringInterpreter ) {
    //     return getNonBlockParent().getThis( declaringInterpreter );
    // }

    // // TODO: see it
    // /**
    //     super is our parent's super
    // */
    // public This getSuper( Interpreter declaringInterpreter ) {
    //     return getNonBlockParent().getSuper( declaringInterpreter );
    // }

    // // TODO: see it
    // /**
    //     delegate import to our parent
    // */
    // @Override
    // public void importClass(String name) {
    //     getParent().importClass( name );
    // }

    // // TODO: see it
    // /**
    //     delegate import to our parent
    // */
    // @Override
    // public void importPackage(String name) {
    //     getParent().importPackage( name );
    // }

    @Override
    public void importClass(String name) {
        this.getParent().importClass(name);
    }

    @Override
    public void importCommands(String name) {
        this.getParent().importCommands(name);
    }

    @Override
    public void importPackage(String name) {
        this.getParent().importPackage(name);
    }

    @Override
    public void doSuperImport() throws UtilEvalError {
        this.getParent().doSuperImport();
    }

    @Override
    public void loadDefaultImports() {
        this.getParent().loadDefaultImports();
    }

    // // TODO: see it
    // @Override
    // public void setMethod(BshLocalMethod method) {
    //     getParent().setMethod( method );
    // }

    @Override
    public void setMethod(BshLocalMethod method) {
        this.getParent().setMethod(method);
    }
}

