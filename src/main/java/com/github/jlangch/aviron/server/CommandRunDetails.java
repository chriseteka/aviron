package com.github.jlangch.aviron.server;


public class CommandRunDetails {

    public CommandRunDetails(
            final String command, 
            final String response,
            final long elapsedMillis
    ) {
        this.command = command.replace(str('\n'), "[n")
                              .replace(str('\0'), "[0");

        this.response = response;
        this.elapsedMillis = elapsedMillis;
    }


    public String getCommand() {
        return command;
    }

    public String getResponse() {
        return response;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public String getElapsedMillisFormatted() {
        if (elapsedMillis < 1000) {
            return String.format("%dms", elapsedMillis);
        }
        else if (elapsedMillis < 60000){
            return String.format("%ds %dms", elapsedMillis / 1000, elapsedMillis % 1000);
        }
        else {
            final long seconds = elapsedMillis / 1000;
            return String.format("%dm %ds", seconds / 60, seconds % 60);
        }
    }


    @Override
    public String toString() {
        return "CommandRunDetails (" + getElapsedMillisFormatted() + "):\n" +
               command + "\n\n" +
               response;
    }


    private static String str(final char ch) {     
        return String.format("%s", ch);   // handles properly '\0' !!!
    }


    private final String command;
    private final String response;
    private final long elapsedMillis;
}
