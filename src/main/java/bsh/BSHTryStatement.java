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
import bsh.legacy.*;
import bsh.congo.parser.Node;


public class BSHTryStatement extends SimpleNode
{
    final int blockId;
    BSHTryWithResources tryWithResources = null;

    public BSHTryStatement(int id)
    {
        super(id);
        blockId = BlockNameSpace.blockCount.incrementAndGet();
    }

    public Object eval( CallStack callstack, Interpreter interpreter)
        throws EvalError
    {
        int i = 0;

        if (getChild(i) instanceof BSHTryWithResources) {
            this.tryWithResources = ((BSHTryWithResources) getChild(i++));
            this.tryWithResources.eval(callstack, interpreter);
        }

        BSHBlock tryBlock = (BSHBlock) getChild(i++);

        List<BSHMultiCatch> catchParams = new ArrayList<>();
        List<BSHBlock> catchBlocks = new ArrayList<>();

        int nchild = getChildCount();
        Node node = null;
        while( i < nchild && (node = getChild(i++)) instanceof BSHMultiCatch )
        {
            catchParams.add((BSHMultiCatch) node);
            catchBlocks.add((BSHBlock) getChild(i++));
            node = null;
        }
        // finally block
        BSHBlock finallyBlock = null;
        if(node != null)
            finallyBlock = (BSHBlock)node;

        Throwable thrown = null;
        Object ret = null;

        /*
            Evaluate the contents of the try { } block and catch any resulting
            TargetErrors generated by the script.
            We save the callstack depth and if an exception is thrown we pop
            back to that depth before contiuing.  The exception short circuited
            any intervening method context pops.

            Note: we the stack info... what do we do with it?  append
            to exception message?
        */
        int callstackDepth = callstack.depth();
        try {
            Interpreter.debug("Evaluate try block");
            try {
                ret = tryBlock.eval(callstack, interpreter);
            } catch ( OutOfMemoryError ome ) {
                throw new TargetError(ome.toString(), ome, tryBlock, callstack, false);
            }
        }
        catch( TargetError e ) {
            Interpreter.debug("Exception from try block: ", e);
            thrown = e.getTarget();
            // clean up call stack grown due to exception interruption
            while ( callstack.depth() > callstackDepth )
                callstack.pop();
        } finally {
            // unwrap the target error
            while ( null != thrown && thrown.getCause() instanceof TargetError )
                thrown = ((TargetError) thrown.getCause()).getTarget();

            // try block finished auto close try-with-resources
            if (null != this.tryWithResources) {
                Interpreter.debug("Try with resources: autoClose");
                List<Throwable> tlist = this.tryWithResources.autoClose();
                for (Throwable t: tlist) // Java 9/10 treats this differently from 8
                    if (null != thrown && thrown != t)
                        thrown.addSuppressed(t);
            }
        }


        // If we have an exception, find a catch
        try {
            if (thrown != null)
            {
                Interpreter.debug("Try catch thrown: ", thrown);
                Class<?> thrownType = thrown.getClass();
                int n = catchParams.size();
                for(i=0; i<n; i++)
                {
                    // Get catch block
                    BSHMultiCatch mc = catchParams.get(i);
                    Modifiers modifiers = new Modifiers(Modifiers.PARAMETER);
                    if (mc.isFinal())
                        modifiers.addModifier("final");

                    mc.eval( callstack, interpreter );

                    if ( mc.isUntyped() && interpreter.getStrictJava() )
                        throw new EvalError(
                            "(Strict Java) Untyped catch block", this, callstack );

                    // If the param is typed check assignability
                    Class<?> mcType = null;
                    if ( !mc.isUntyped() ) {
                        boolean found = false;
                        for ( Class<?> cType: mc.getTypes() )
                            if ( true == ( found = Types.isBshAssignable(cType, thrownType) ) ) {
                                mcType = cType;
                                break;
                            }
                        if ( !found )
                            continue;
                    }
                    // Found match, execute catch block
                    BSHBlock cb = catchBlocks.get(i);

                    // Prepare to execute the block.
                    // We must create a new BlockNameSpace to hold the catch
                    // parameter and swap it on the stack after initializing it.

                    NameSpace enclosingNameSpace = callstack.top();
                    BlockNameSpace cbNameSpace = new BlockNameSpace(callstack.top(), blockId);

                    try {
                        if ( mcType == BSHMultiCatch.UNTYPED )
                            // set an untyped variable directly in the block
                            cbNameSpace.setBlockVariable( mc.name, thrown );
                        else
                            // set a typed variable (directly in the block)
                            cbNameSpace.setTypedVariable(
                                mc.name, mcType, thrown, modifiers);
                    } catch ( UtilEvalError e ) {
                        throw new InterpreterError(
                            "Unable to set var in catch block namespace." );
                    }

                    // put cbNameSpace on the top of the stack
                    callstack.swap( cbNameSpace );
                    try {
                        ret = cb.eval( callstack, interpreter, true );
                    } finally {
                        // put it back
                        callstack.swap( enclosingNameSpace );
                    }

                    thrown = null;  // handled exception
                    break;
                }
            }
        } finally {
            // evaluate finally block
            if( finallyBlock != null ) {
                Object result = finallyBlock.eval(callstack, interpreter);
                if( result instanceof ReturnControl )
                    return result;
            }
        }
        // exception fell through, throw it upward...
        if( null != thrown )
            throw new TargetError(thrown, this, callstack);

        // no exception return
        return ret instanceof ReturnControl ? ret : Primitive.VOID;
    }
}
