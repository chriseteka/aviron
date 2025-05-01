package com.github.jlangch.aviron;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;


public class TestScan {

    /* 
     * Update virus database:       freshclam
     * Start clamd in foreground:   clamd --foreground
     */
    public static void main(String[] args) throws Exception {

        final String baseDir = "/Users/juerg/Documents/ClamAV/files/";

        final Client client = new Client.Builder()
                                      .serverHostname("localhost")
                                      .serverFileSeparator(FileSeparator.JVM_PLATFORM)
                                      .build();

        // scan single files
        System.out.println("[Scan Single Files]");
        System.out.println(client.scan(Paths.get(baseDir, "Messung-Lichtgeschwindigkeit-RØMER.pdf")));
        System.out.println(client.scan(Paths.get(baseDir, "cheatsheet.pdf")));
        System.out.println();
        System.out.println();

        // scan virus files
        System.out.println("[Scan Virus File]");
        System.out.println(client.scan(Paths.get(baseDir, "eicar.txt")));
        System.out.println();
        System.out.println();

        // scan dir
        System.out.println("[Scan Dir]");
        System.out.println(client.scan(Paths.get(baseDir), true));
        System.out.println();
        System.out.println();

        // parallel dir scan
        System.out.println("[Scan Parallel Dir]");
        System.out.println(client.parallelScan(Paths.get(baseDir)));
        System.out.println();
        System.out.println();

        // scan streamed data
        System.out.println("[Scan Streamed Data]");
        byte[] data = Files.readAllBytes(Paths.get(baseDir, "Messung-Lichtgeschwindigkeit-RØMER.pdf"));
        System.out.println(client.scan(new ByteArrayInputStream(data)));
        System.out.println(client.scan(new ByteArrayInputStream(data), 4096));
        System.out.println();
        System.out.println();
        
        System.out.println("[Scan Streamed Data 2]");
        try (InputStream is = new FileInputStream(new File(baseDir, "document.pdf"))) {
            System.out.println(client.scan(is));
        }
        System.out.println();
        System.out.println();
    }
}
