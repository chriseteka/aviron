package com.github.jlangch.aviron.ex;


public class UnknownCommandException extends AvironException {

    public UnknownCommandException(final String command) {
        super(String.format("Unknown command: %s", command));
    }


    private static final long serialVersionUID = 1L;
}