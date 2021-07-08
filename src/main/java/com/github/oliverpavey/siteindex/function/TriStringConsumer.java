package com.github.oliverpavey.siteindex.function;

/**
 * General purpose functional interface not included with JRE.
 */
@FunctionalInterface
public interface TriStringConsumer {

    /**
     * Pass three String values into consumer (typically a lambda)
     *
     * @param v1 First parameter
     * @param v2 Second parameter
     * @param v3 Third parameter
     */
    void accept(String v1, String v2, String v3);
}
