package bsh.internals;

import bsh.EvalError;

@FunctionalInterface
public interface BshFunction<T, R> {
    R apply(T obj) throws EvalError;
}
