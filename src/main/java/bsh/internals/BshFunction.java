package bsh.internals;

import bsh.CallStack;
import bsh.EvalError;
import bsh.TargetError;

interface BshInvocable<R> {
    R invoke(CallStack callStack) throws EvalError;

    // TODO: cusotmizar a logica
    default R invoke(CallStack callStack, Class<R> returnType, Class<?>[] exceptionTypes) throws TargetError {
        return null;
    }
}

@FunctionalInterface
public interface BshFunction<T, R> {
    R invokeImpl(T obj) throws EvalError;
}
