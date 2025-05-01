package com.github.jlangch.aviron;


public class TestManagementFunctions {

    /* 
     * Update virus database:       freshclam
     * Start clamd in foreground:   clamd --foreground
     */
    public static void main(String[] args) throws Exception {
        Client client = new Client.Builder()
                                  .serverHostname("localhost")
                                  .serverFileSeparator(FileSeparator.JVM_PLATFORM)
                                  .build();
        
        System.out.println("[Ping]");
        System.out.println(client.ping());
        System.out.println();
        System.out.println();
        
        System.out.println("[Version]");
        System.out.println(client.version());
        System.out.println();
        System.out.println();
        
        System.out.println("[Stats]");
        System.out.println(client.stats());
        System.out.println();
        System.out.println();
        
        System.out.println("[Reload Virus Databases]");
        client.reloadVirusDatabases();
        System.out.println("Done.");
        System.out.println();
        System.out.println();
        
        System.out.println("[Reachable]");
        System.out.println(client.isReachable());
        System.out.println();
        System.out.println();
        
        System.out.println("[Reachable (Timeout)]");
        System.out.println(client.isReachable(2000));
        System.out.println();
        System.out.println();
        
        System.out.println("[clamd PID]");
        System.out.println(client.getClamdPID());
        System.out.println();
        System.out.println();
    }

}
