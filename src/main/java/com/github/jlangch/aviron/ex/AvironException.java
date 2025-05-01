package com.github.jlangch.aviron.ex;


public class AvironException extends RuntimeException {

    public AvironException(final String message) {
        super(message);
    }

    public AvironException(final Throwable cause) {
        super(cause);
    }

    public AvironException(String message, final Throwable cause) {
        super(message, cause);
    }


    private static final long serialVersionUID = 1L;
}