package bsh.internals;

import bsh.EvalError;

@FunctionalInterface
public interface BshConsumer<T> {
    void consume(T obj) throws EvalError;
}
