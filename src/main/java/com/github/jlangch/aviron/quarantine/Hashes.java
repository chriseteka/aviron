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
package com.github.jlangch.aviron.quarantine;

import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Base64;

import com.github.jlangch.aviron.ex.HashException;


/**
 * Computes hashes from files and verifies file hashes to detect modified
 * files.
 *
 * <p>
 * Supports any algorithms supplied by the Java VM like MD5, SHA-1,
 * SHA-512, ..
 *
 * <p>
 * Note: MD5 is known as a weak cryptographic algorithm
 */
public class Hashes {

    public static String hashFile(
            final String algorithm,
            final String salt,
            final File inputFile
    ) {
        try {
            // Read file data
            final byte[] fileData = Files.readAllBytes(inputFile.toPath());

            // Hash
            return hashData(algorithm, salt, fileData);
        }
        catch(Exception ex) {
            throw new HashException("Failed to compute hash for file!");
        }
    }

    public static String hashData(
            final String algorithm,
            final String salt,
            final byte[] data
    ) {
        try {
            // Init digest
            final MessageDigest md = MessageDigest.getInstance(algorithm);
            md.reset();

            // Supply data
            md.update(salt.getBytes("UTF-8"));
            md.update(data);

            // Get digest
            return encodeBase64(md.digest());
        }
        catch(Exception ex) {
            throw new HashException("Failed to compute hash for binary data!");
        }
    }

    public static boolean verifyFileHash(
            final String algorithm,
            final String salt,
            final File inputFile,
            final String hash
    ) {
        try {
            // Read file data
            final byte[] fileData = Files.readAllBytes(inputFile.toPath());

            // Verify hash
            return verifyDataHash(algorithm, salt, fileData, hash);
        }
        catch(Exception ex) {
            throw new HashException("Failed to compute hash for verification!");
        }
    }

    public static boolean verifyDataHash(
            final String algorithm,
            final String salt,
            final byte[] fileData,
            final String hash
    ) {
        // Hash file data
        final String fileDataHash = hashData(algorithm, salt, fileData);

        // Verify  digest
        return hash.equals(fileDataHash);
    }



    public static String encodeBase64(final byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] decodeBase64(final String data) {
        return Base64.getDecoder().decode(data);
    }
}
