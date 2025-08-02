/*                 _                 
 *       /\       (_)            
 *      /  \__   ___ _ __ ___  _ __  
 *     / /\ \ \ / / | '__/ _ \| '_ \ 
 *    / ____ \ V /| | | | (_) | | | |
 *   /_/    \_\_/ |_|_|  \___/|_| |_|
 *
 *
 * Copyright 2025 Aviron
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jlangch.aviron.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 
 * The demo file store layout:
 * 
 * <pre>
 * demo/
 *   |
 *   +-- filestore/
 *   |     |
 *   |     +-- 0000
 *   |     |     \_ file1.doc
 *   |     |     \_ file2.doc
 *   |     |     :
 *   |     |     \_ fileN.doc
 *   |     +-- 0001
 *   |     |     \_ file1.doc
 *   |     |     :
 *   |     |     \_ fileN.doc
 *   |     :
 *   |     +-- NNNN
 *   |           \_ file1.doc
 *   |
 *   +-- quarantine/
 *         \_ file1.doc
 *         \_ file1.doc.virus
 * </pre>
 * 
 * <p>The demo filestore is created on the temp directory and completely removed by 
 * the close() method.
 * 
 * <p>Example:
 * 
 * <pre>
 * try (DemoFilestore fs = new DemoFilestore()) {
 *    final File rootDir = fs.getRootDir();
 *    final File filestoreDir = fs.getFilestoreDir();
 *    final File quarantineDir = fs.getQuarantineDir();
 *
 *    // create subirs
 *    fs.createFilestoreSubDir("0000");
 *    fs.createFilestoreSubDir("0001");
 *    fs.createFilestoreSubDir("0002");
 *
 *    // subdirs
 *    final List&lt;File&gt; subDirs = fs.listFilestoreSubDirs();
 *
 *    // populate with some data files
 *    fs.touchFilestoreFile("0000",  "file1.txt");
 *
 *    fs.touchFilestoreFile("0000",  "file2.txt");
 *    fs.appendFilestoreFile("0000", "file2.txt");
 *
 *    fs.createFilestoreFile("0000", "file3.txt");
 *
 *    fs.createFilestoreFile("0000", "file4.txt");
 *    fs.deleteFilestoreFile("0000", "file4.txt");
 * }
 * </pre>
 */
public class DemoFilestore implements Closeable {

    public DemoFilestore() {
        this.rootDir = createTempDir();
        this.filestoreDir = new File(rootDir, "filestoreDir");
        this.quarantineDir = new File(rootDir, "quarantine");

        this.filestoreDir.mkdir();
        this.quarantineDir.mkdir();

        this.dirCycler = new DirCycler(this.filestoreDir);
    }



    public File getRootDir() {
        return rootDir;
    }

    public File getFilestoreDir() {
        return filestoreDir;
    }

    public File getQuarantineDir() {
        return quarantineDir;
    }

    @Override
    public void close() {
        try {
            Files.walk(rootDir.toPath())
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        }
        catch(IOException ex) {
            throw new RuntimeException("Failed to remove demo filestore", ex);
        }
    }

    public void populateWithDemoFiles(final int dirs, final int filesPerDir) {
        // limit dirs and filesPerDir to protect the caller
        for(int d=0; d<Math.min(100, dirs); d++) {
            final String dir = String.format("%04d", d);

            createFilestoreSubDir(dir);

            for(int f=0; f<Math.min(100, filesPerDir); f++) {
                createFilestoreFile(dir, String.format(dir, "file-%04d.txt", "TEST"));
            }
        }
    }

    public File createFilestoreSubDir(final String name) {
       final File dir = new File(filestoreDir, name);
       dir.mkdir();
       return dir;
    }

    public List<File> listFilestoreSubDirs() {
        return Arrays.stream(filestoreDir.listFiles())
                     .filter(f -> f.isDirectory())
                     .sorted()
                     .collect(Collectors.toList());
    }

    public File createFilestoreFile(final String subDir, final String filename) {
        return createFilestoreFile(subDir, filename, "TEST"); 
     }

    public File createFilestoreFile(final String subDir, final String filename, final String data) {
        final String fsFile = new File(subDir).getName() + "/" + new File(filename).getName();
        final File file = new File(filestoreDir,fsFile);
        try {
            Files.write(file.toPath(), toBytes(data));
            return file;
        }
        catch(IOException ex) {
            throw new RuntimeException("Failed to create file store file " + fsFile, ex);
        }
     }

    public File touchFilestoreFile(final String subDir, final String filename) {
        final String fsFile = new File(subDir).getName() + "/" + new File(filename).getName();
        final File file = new File(filestoreDir,fsFile);
        try {
            final Path path = file.toPath();
            if (Files.exists(path)) {
                Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis()));
            }
            else {
                Files.createFile(path);
            }
            return file;
        }
        catch(IOException ex) {
            throw new RuntimeException("Failed to touch file store file "+ fsFile, ex);
        }
    }

    public File appendToFilestoreFile(final String subDir, final String filename) {
        final String fsFile = new File(subDir).getName() + "/" + new File(filename).getName();
        final File file = new File(filestoreDir,fsFile);
        if (file.isFile()) {
            try {
                Files.write(file.toPath(), toBytes(",TEST"), StandardOpenOption.APPEND);
                return file;
            }
            catch(IOException ex) {
                throw new RuntimeException(
                        "Failed to append to the file store file " + fsFile, ex);
            }
        }
        else {
            throw new RuntimeException(
                    "Failed to append to file store file "+ fsFile + "! It does not exist!");
        }
     }

    public void deleteFilestoreFile(final String subDir, final String filename) {
        final String fsFile = new File(subDir).getName() + "/" + new File(filename).getName();
        final File file = new File(filestoreDir,fsFile);
        try {
            if (file.isFile()) {
                if (!file.delete()) {
                    throw new RuntimeException("Failed to remove file store file "+ fsFile);
                }
            }
        }
        catch(Exception ex) {
            throw new RuntimeException("Failed to remove file store file "+ fsFile);
       }
    }

    /**
     * Create an eicar anti malware test file in file store's subdirectory.
     * 
     * @param subDir a subdirectory to write the test file to.
     * 
     * @see <a href="https://www.eicar.org/download-anti-malware-testfile/">Eicar</a>
     */
    public void createEicarAntiMalwareTestFile(final String subDir) {
        final String fsFile = new File(subDir).getName() + "/eicar.txt";
        final File file = new File(filestoreDir,fsFile);
        try {
            Files.write(file.toPath(), toBytes(eicar.replace("_", "")));
        }
        catch(IOException ex) {
            throw new RuntimeException(
                    "Failed to append to the file store file " + fsFile, ex);
        }
    }

    public long countFilestoreFiles() {
        try {
            return Files.walk(filestoreDir.toPath())
                        .map(Path::toFile)
                        .filter(f -> f.isFile())
                        .count();
        }
        catch(IOException ex) {
            throw new RuntimeException("Failed count filestore files", ex);
        }
    }

    public long countQuarantineFiles() {
        return Arrays.stream(quarantineDir.listFiles())
                .filter(f -> f.isFile())
                .filter(f -> !f.getName().endsWith(".virus"))
                .count();
    }

    public void clearQuarantine() {
        try {
            Files.walk(quarantineDir.toPath())
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
            
            quarantineDir.mkdir();
        }
        catch(IOException ex) {
            throw new RuntimeException("Failed to remove demo filestore", ex);
        }
    }

    public IDirCycler getFilestoreDirCycler() {
        return dirCycler;
    }

    private File createTempDir() {
        try {
           return Files.createTempDirectory("demo_").toFile();
        }
        catch(IOException ex) {
            throw new RuntimeException("Failed to create demo file store", ex);
        }
    }

    private byte[] toBytes(final String text) {
        return text.getBytes(Charset.defaultCharset());
    }


    private static class DirCycler implements IDirCycler {
         public DirCycler(final File rootDir) {
             this.rootDir = rootDir;
         }

         @Override
         public File rootDir() {
             return rootDir;
         }

         @Override
         public File nextDir() {
             if (subDirs.isEmpty()) {
                 refresh();  // check if new filestore dirs arrived
             }

             if (subDirs.isEmpty()) {
                 return null;  // still empty
             }

             int dirIdx = lastDirIdx + 1;
             if (dirIdx >= subDirs.size()) {
                 // we past the last directory -> refresh to reflect dir changes
                 refresh();
                 if (subDirs.isEmpty()) {
                     return null;  // empty now
                 }
                 dirIdx = 0;
             }

             lastDirIdx = dirIdx;
             return subDirs.get(dirIdx);
         }

         @Override
         public void refresh() {
             subDirs.clear();
             subDirs.addAll(dirs());
             lastDirIdx = -1;
         }

         @Override
         public String lastDirName() {
             return lastDirIdx < 0 || lastDirIdx >= subDirs.size()-1
                     ? null
                     : subDirs.get(lastDirIdx).getName();
         }

         @Override
         public void restoreLastDirName(final String name) {
             lastDirIdx = getIndexOf(name);
         }

         private List<File> dirs() {
             return Arrays.stream(rootDir.listFiles())
                          .filter(f -> f.isDirectory())
                          .sorted()
                          .collect(Collectors.toList());
         }

         private int getIndexOf(final String name) {
             for(int ii=0; ii<subDirs.size(); ii++) {
                 if (subDirs.get(ii).getName().equals(name)) {
                     return ii;
                 }
             }
             return -1;
         }

        private final File rootDir;
        private int lastDirIdx = -1;
        private final List<File> subDirs = new ArrayList<>();
    }


    // eicar test file. it's encoded -> Github :-).
    //
    // see https://www.eicar.org/download-anti-malware-testfile/
    private final static String eicar = "X_5_O_!_P_%_@_A_P_[_4_\\_P_Z_X_5_4_(_P_^_"  +
                                        ")_7_C_C_)_7_}_$_E_I_C_A_R_-_S_T_A_N_D_A_R_" +
                                        "D_-_A_N_T_I_V_I_R_U_S_-_T_E_S_T_-_F_I_L_E_" +
                                        "!_$_H_+_H_*";

    private final File rootDir;
    private final File filestoreDir;
    private final File quarantineDir;

    private final IDirCycler dirCycler;
}
