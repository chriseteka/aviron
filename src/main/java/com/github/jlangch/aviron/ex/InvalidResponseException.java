package com.github.jlangch.aviron.ex;

public class InvalidResponseException extends AvironException {
    
    public InvalidResponseException(final String responseString) {
        super(String.format("Invalid response: %s", responseString));
    }


    private static final long serialVersionUID = 1L;
}