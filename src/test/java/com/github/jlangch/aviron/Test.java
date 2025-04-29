package com.github.jlangch.aviron;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;


public class Test {

    public static void main(String[] args) throws Exception {
        Client client = new Client.Builder()
                                  .serverHostname("localhost")
                                  .serverFileSeparator(FileSeparator.JVM_PLATFORM)
                                  .build();
        
//        System.out.println(client.scan(Paths.get("/Users/juerg/Documents/ClamAV/files/Messung-Lichtgeschwindigkeit-RØMER.pdf")));
//        System.out.println(client.scan(Paths.get("/Users/juerg/Documents/ClamAV/files/cheatsheet.pdf")));
//        System.out.println(client.scan(Paths.get("/Users/juerg/Documents/ClamAV/files/eicar.txt")));
//        System.out.println(client.scan(Paths.get("/Users/juerg/Documents/ClamAV/files"), true));
//        System.out.println(client.scan(Paths.get("/Users/juerg/Documents/Tools/omniref/apache-tomcat-9.0.96/temp/filestore/000001/")));
//        System.out.println(client.parallelScan(Paths.get("/Users/juerg/Documents/Tools/omniref/apache-tomcat-9.0.96/temp/filestore/000001/")));
        System.out.println(client.parallelScan(Paths.get("/Users/juerg/Documents/Tools/omniref/apache-tomcat-9.0.96/temp/filestore/")));
//        System.out.println(client.stats());
        System.out.println(client.getLastCommandRunDetails());
        
//        byte[] data = Files.readAllBytes(Paths.get("/Users/juerg/Documents/ClamAV/files/Messung-Lichtgeschwindigkeit-RØMER.pdf"));
//        System.out.println(client.scan(new ByteArrayInputStream(data)));
//        System.out.println(client.scan(new ByteArrayInputStream(data), 4096));

    }
    
    
}
