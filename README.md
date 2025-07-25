[![](https://github.com/jlangch/aviron/blob/master/doc/maven-central.svg)](https://central.sonatype.com/artifact/com.github.jlangch/aviron)
[![](https://github.com/jlangch/aviron/blob/master/doc/license.svg)](./LICENSE)
![Java Version](https://img.shields.io/badge/java-%3E%3D%201.8-success)

[![Release (latest by date)](https://img.shields.io/github/v/release/jlangch/aviron)](https://github.com/jlangch/aviron/releases/latest)
[![Release Date](https://img.shields.io/github/release-date/jlangch/aviron?color=blue)](https://github.com/jlangch/aviron/releases/latest)
[![GitHub commits since latest release (by date)](https://img.shields.io/github/commits-since/jlangch/aviron/latest)](https://github.com/jlangch/aviron/commits/)



# Aviron

Aviron is a zero dependency Clam AV Java client.


## Change Log

[Change Log](ChangeLog.md)


## Examples

Enable *TCPSocket* and *TCPAddr* configuration parameters in *clamd.conf*:

```
TCPSocket 3310

TCPAddr localhost
```

Start **clamd** as foreground process for easy testing (on Linux production systems **clamd** is run as a **systemd** service):

```sh
clamd --foreground
```


### Simple scanning example:

```java
import java.io.*;
import java.nio.file.*;

import com.github.jlangch.aviron.Client;
import com.github.jlangch.aviron.FileSeparator;

public class Scan {

    public static void main(String[] args) throws Exception {
        final String baseDir = "/data/files/";

        // Note: The file separator depends on the server's type (Unix, Windows)
        //       clamd is running on!
        final Client client = new Client.Builder()
                                        .serverHostname("localhost")
                                        .serverFileSeparator(FileSeparator.UNIX)
                                        .build();

        System.out.println("Reachable: " + client.isReachable());

        // Scanning -----------------------------------------------------------

        // scan single file
        System.out.println(client.scan(Paths.get(baseDir, "document.pdf")));

        // scan dir (recursive)
        System.out.println(client.scan(Paths.get(baseDir), true));

        // scan streamed data
        try (InputStream is = new FileInputStream(new File(baseDir, "document.pdf"))) {
            System.out.println(client.scan(is));
        }
    }
}
```

### Extended example with quarantine support:

Infected files are move/copied implicitly to a quarantine directory. Whether
an infected file is moved or copied can be controlled by the *quarantineFileAction*
configuration parameter.

```java
import java.io.*;
import java.nio.file.*;
import java.util.*;

import com.github.jlangch.aviron.Client;
import com.github.jlangch.aviron.FileSeparator;
import com.github.jlangch.aviron.dto.QuarantineFile;
import com.github.jlangch.aviron.events.QuarantineFileAction;
import com.github.jlangch.aviron.events.QuarantineEvent;

public class Scan {

    public static void main(String[] args) throws Exception {
        final String baseDir = "/data/files/";
        final String quarantineDir = "/data/quarantine/";

        // Note: The file separator depends on the server's type (Unix, Windows)
        //       clamd is running on!
        final Client client = new Client.Builder()
                                        .serverHostname("localhost")
                                        .serverFileSeparator(FileSeparator.UNIX)
                                        .quarantineFileAction(QuarantineFileAction.MOVE)
                                        .quarantineDir(quarantineDir)
                                        .quarantineEventListener(l -> listener(l))
                                        .build();

        System.out.println("Reachable: " + client.isReachable());

        // Scanning -----------------------------------------------------------

        // scan single file
        System.out.println(client.scan(Paths.get(baseDir, "document.pdf")));

        // scan dir (recursive)
        System.out.println(client.scan(Paths.get(baseDir), true));

        // scan streamed data
        try (InputStream is = new FileInputStream(new File(baseDir, "document.pdf"))) {
            System.out.println(client.scan(is));
        }

        // Quarantine Management ----------------------------------------------

        // list quarantine files
        final List<QuarantineFile> files = client.listQuarantineFiles();
        System.out.println(String.format("%d quarantined files", files.size()));
         
        // show quarantine file details and remove the file
        if (!files.isEmpty()) {
            final QuarantineFile qf = files.get(0);
            System.out.println(qf);
            client.removeQuarantineFile(qf);
        }

         // remove all quarantine files
         client.removeAllQuarantineFiles();
    }

    private void listener(final QuarantineEvent event) {
        if (event.getException() != null) {
            System.out.println("Error " + event.getException().getMessage());
        }
        else {
            System.out.println("File " + event.getInfectedFile() + " moved to quarantine");
        }
    }
}
```


## Getting the latest release

You can can pull it from the central Maven repositories:

```java
<dependency>
  <groupId>com.github.jlangch</groupId>
  <artifactId>aviron</artifactId>
  <version>1.4.0</version>
</dependency>
```

## Contributing

I accept Pull Requests via GitHub. There are some guidelines which will make applying PRs easier for me:

- No tabs! Please use spaces for indentation.
- Respect the existing code style for each file.
- Create minimal diffs - disable on save actions like reformat source code or organize imports. If you feel the source code should be reformatted create a separate PR for this change.
- Provide JUnit tests for your changes and make sure your changes don't break any existing tests by running gradle.


## License

This code is licensed under the [Apache License v2](LICENSE).
