package com.github.jlangch.aviron.util;

import java.util.function.Supplier;


public class Lazy<T> implements Supplier<T> {

    public Lazy(final Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public synchronized T get() {
        if (value == null) {
            value = supplier.get();
        }
        return value;
    }


    private final Supplier<T> supplier;
    private volatile T value;
}
