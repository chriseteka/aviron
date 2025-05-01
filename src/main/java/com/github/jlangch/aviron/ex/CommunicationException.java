package com.github.jlangch.aviron.ex;


public class CommunicationException extends AvironException {

    public CommunicationException(final Throwable cause) {
        super("Error while communicating with the ClamAV server", cause);
    }


    private static final long serialVersionUID = 1L;
}