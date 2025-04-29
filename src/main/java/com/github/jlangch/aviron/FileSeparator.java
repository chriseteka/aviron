package com.github.jlangch.aviron;

import java.io.File;
import java.nio.file.Path;


public enum FileSeparator {

    UNIX('/') {
        @Override
        public String toServerPath(final Path path) {
            return path.toString().replace(WINDOWS.separator, UNIX.separator);
        }
    },

    WINDOWS('\\') {
        @Override
        public String toServerPath(final Path path) {
            return path.toString().replace(UNIX.separator, WINDOWS.separator);
        }
    },

    JVM_PLATFORM(File.separatorChar) {
        @Override
        public String toServerPath(final Path path) {
            return path.toString();
        }
    };

    public abstract String toServerPath(final Path path);


    private FileSeparator(final char separator) {
        this.separator = separator;
    }

    
    private final char separator;
}