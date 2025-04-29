package com.github.jlangch.aviron.commands;


public enum CommandFormat {

    NULL_CHAR('z', '\0'),

    NEW_LINE('n', '\n');


    private CommandFormat(final char prefix, final char terminator) {
        this.prefix = prefix;
        this.terminator = terminator;
    }

    public char getPrefix() {
        return prefix;
    }

    public char getTerminator() {
        return terminator;
    }


    private final char prefix;
    private final char terminator;
}
