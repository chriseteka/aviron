package com.github.jlangch.aviron;


public class TestDebug {

    /* 
     * Update virus database:       freshclam
     * Start clamd in foreground:   clamd --foreground
     */
    public static void main(String[] args) throws Exception {
        Client client = new Client.Builder()
                                  .serverHostname("localhost")
                                  .serverFileSeparator(FileSeparator.JVM_PLATFORM)
                                  .build();
        
        System.out.println("[Stats]");
        System.out.println(client.stats());
        System.out.println();
        System.out.println();
 
        System.out.println("[Last Command]");
        System.out.println(client.getLastCommandRunDetails());
        System.out.println();
        System.out.println();
    }

}
