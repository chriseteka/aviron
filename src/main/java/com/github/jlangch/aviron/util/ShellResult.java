package com.github.jlangch.aviron.util;


public class ShellResult {

    public ShellResult(
            final String stdout,
            final String stderr,
            final int exitCode
    ) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.exitCode = exitCode;
    }


    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public int getExitCode() {
        return exitCode;
    }


    @Override
    public String toString() {
        if (exitCode == 0) {
            return stdout;
        }
        else {
            final String err = StringUtils.trimToNull(stderr);
            final String out = StringUtils.trimToNull(stdout);
            
            StringBuilder sb = new StringBuilder();
            
            if (out != null) {
                sb.append(out);
                sb.append("\n\n\n");
            }
            
            if (err != null) {
                sb.append("Exit code: " + exitCode + "\n");
                sb.append("Error:\n\n");
                sb.append(stderr);
            }
            
            return sb.toString();
        }
    }
    

    private final String stdout;
    private final String stderr;
    private final int exitCode;
}
