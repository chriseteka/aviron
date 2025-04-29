package com.github.jlangch.aviron.ex;


public class ClamavException extends RuntimeException {

    public ClamavException(final String message) {
        super(message);
    }

    public ClamavException(final Throwable cause) {
        super(cause);
    }

    public ClamavException(String message, final Throwable cause) {
        super(message, cause);
    }


    private static final long serialVersionUID = 1L;
}