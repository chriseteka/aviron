package com.github.jlangch.aviron.ex;


public class ScanFailureException extends ClamavException {
   
    public ScanFailureException(final String responseString) {
        super(String.format("Scan failure: %s", responseString));
    }


    private static final long serialVersionUID = 1L;
}