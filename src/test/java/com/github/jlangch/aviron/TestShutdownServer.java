package com.github.jlangch.aviron;


public class TestShutdownServer {

    /* 
     * Update virus database:       freshclam
     * Start clamd in foreground:   clamd --foreground
     */
    public static void main(String[] args) throws Exception {
        Client client = new Client.Builder()
                                  .serverHostname("localhost")
                                  .serverFileSeparator(FileSeparator.JVM_PLATFORM)
                                  .build();
        
        System.out.println("[Shutdown Server]");
        client.shutdownServer();
        System.out.println("Shutdown.");
        System.out.println();
        System.out.println();
        
        Thread.sleep(1000);
        
        System.out.println("[Reachable]");
        System.out.println(client.isReachable());
        System.out.println();
        System.out.println();
        
        System.out.println("[Reachable (Timeout)]");
        System.out.println(client.isReachable(2000));
        System.out.println();
        System.out.println();
        
        System.out.println("[clamd PID]");
        System.out.println(Admin.getClamdPID());
        System.out.println();
        System.out.println();

    }

}
