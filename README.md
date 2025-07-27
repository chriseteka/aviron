[![](https://github.com/jlangch/aviron/blob/master/doc/maven-central.svg)](https://central.sonatype.com/artifact/com.github.jlangch/aviron)
[![](https://github.com/jlangch/aviron/blob/master/doc/license.svg)](./LICENSE)
![Java Version](https://img.shields.io/badge/java-%3E%3D%201.8-success)

[![Release (latest by date)](https://img.shields.io/github/v/release/jlangch/aviron)](https://github.com/jlangch/aviron/releases/latest)
[![Release Date](https://img.shields.io/github/release-date/jlangch/aviron?color=blue)](https://github.com/jlangch/aviron/releases/latest)
[![GitHub commits since latest release (by date)](https://img.shields.io/github/commits-since/jlangch/aviron/latest)](https://github.com/jlangch/aviron/commits/)



# Aviron

Aviron is a zero dependency ClamAV Java client. It requires Java 8+.


## Table of Content

* [Examples](#examples)
* [Defining Clamd CPU profiles](#defining-clamd-cpu-profiles)
* [Controlling the Clamd CPU usage](#controlling-the-clamd-cpu-usage)


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

    public static void main(String[] args) {
        try {
            new Scan().scan();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void scan() throws Exception {
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

Infected files can be moved/copied implicitly to a quarantine directory. 
Whether an infected file is moved or copied can be controlled by the 
*quarantineFileAction* configuration parameter.

Note: 

In COPY mode unaltered infected files are copied only once to the quarantine 
directory no matter how many times they get rescanned. Aviron uses a hash code 
of the file's data to check whether the file has changed since the last copy 
action.

```java
import java.io.*;
import java.nio.file.*;
import java.util.*;

import com.github.jlangch.aviron.Client;
import com.github.jlangch.aviron.FileSeparator;
import com.github.jlangch.aviron.dto.QuarantineFile;
import com.github.jlangch.aviron.events.QuarantineFileAction;
import com.github.jlangch.aviron.events.QuarantineEvent;


public class ScanQuarantine {

    public static void main(String[] args) throws Exception {
        try {
            new Scan().scan();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void scan() throws Exception {
       final String baseDir = "/data/files/";
        final String quarantineDir = "/data/quarantine/";

        // Note: The file separator depends on the server's type (Unix, Windows)
        //       clamd is running on!
        final Client client = new Client.Builder()
                                        .serverHostname("localhost")
                                        .serverFileSeparator(FileSeparator.UNIX)
                                        .quarantineFileAction(QuarantineFileAction.MOVE)
                                        .quarantineDir(quarantineDir)
                                        .quarantineEventListener(this::eventListener)
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
    
    private void eventListener(final QuarantineEvent event) {
        if (event.getException() != null) {
            System.out.println("Error " + event.getException().getMessage());
        }
        else {
            System.out.println("File " + event.getInfectedFile() + " moved to quarantine");
        }
    }
}
```

## Defining Clamd CPU profiles

Sometimes running the *clamd* daemon constantly at 100% CPU time is not an 
option. Aviron allows the definition if CPU profiles to control the clamd CPU
usage.


### A simply daily profile used for Mon - Sun

```java
import static com.github.jlangch.aviron.impl.util.CollectionUtils.toList;

import com.github.jlangch.aviron.admin.CpuProfile;
import com.github.jlangch.aviron.admin.DynamicCpuLimit;


public class DynamicCpuLimitExample1 {

    public static void main(String[] args) {
        try {
            new DynamicCpuLimitExample1().test();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void test() throws Exception {
        // Use the same day profile for Mon - Sun
        final CpuProfile everyday = CpuProfile.of(
                                        "weekday",
                                        toList(
                                            "00:00-05:59 @ 100%",
                                            "06:00-08:59 @  50%",
                                            "09:00-17:59 @   0%",
                                            "18:00-21:59 @  50%",
                                            "22:00-23:59 @ 100%"));

        final DynamicCpuLimit dynamicCpuLimit = new DynamicCpuLimit(everyday);

        // Even though the profile has a minute resolution the 
        // 'formatProfilesAsTableByHour' function prints the overview
        //  table at an hour resolution for simplicity!
        final String s = dynamicCpuLimit.formatProfilesAsTableByHour();
        System.out.println(s);
    }
```

```
---------------------------------------------------------
Time        Mon    Tue    Wed    Thu    Fri    Sat    Sun
---------------------------------------------------------
00:00      100%   100%   100%   100%   100%   100%   100%
01:00      100%   100%   100%   100%   100%   100%   100%
02:00      100%   100%   100%   100%   100%   100%   100%
03:00      100%   100%   100%   100%   100%   100%   100%
04:00      100%   100%   100%   100%   100%   100%   100%
05:00      100%   100%   100%   100%   100%   100%   100%
06:00       50%    50%    50%    50%    50%    50%    50%
07:00       50%    50%    50%    50%    50%    50%    50%
08:00       50%    50%    50%    50%    50%    50%    50%
09:00        0%     0%     0%     0%     0%     0%     0%
10:00        0%     0%     0%     0%     0%     0%     0%
11:00        0%     0%     0%     0%     0%     0%     0%
12:00        0%     0%     0%     0%     0%     0%     0%
13:00        0%     0%     0%     0%     0%     0%     0%
14:00        0%     0%     0%     0%     0%     0%     0%
15:00        0%     0%     0%     0%     0%     0%     0%
16:00        0%     0%     0%     0%     0%     0%     0%
17:00        0%     0%     0%     0%     0%     0%     0%
18:00       50%    50%    50%    50%    50%    50%    50%
19:00       50%    50%    50%    50%    50%    50%    50%
20:00       50%    50%    50%    50%    50%    50%    50%
21:00       50%    50%    50%    50%    50%    50%    50%
22:00      100%   100%   100%   100%   100%   100%   100%
23:00      100%   100%   100%   100%   100%   100%   100%
---------------------------------------------------------
```


### A weekday/weekend profile

```java
import static com.github.jlangch.aviron.impl.util.CollectionUtils.toList;

import java.util.List;

import com.github.jlangch.aviron.admin.CpuProfile;
import com.github.jlangch.aviron.admin.DynamicCpuLimit;
import com.github.jlangch.aviron.impl.util.CollectionUtils;


public class DynamicCpuLimitExample2 {

    public static void main(String[] args) {
        try {
            new DynamicCpuLimitExample2().test();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void test() throws Exception {
        final CpuProfile weekday = CpuProfile.of(
                                        "weekday",
                                        toList(
                                            "00:00-05:59 @ 100%",
                                            "06:00-08:59 @  50%",
                                            "09:00-17:59 @   0%",
                                            "18:00-21:59 @  50%",
                                            "22:00-23:59 @ 100%"));

        final CpuProfile weekend = CpuProfile.of(
                                        "weekend",
                                        toList(
                                            "00:00-05:59 @ 100%",
                                            "06:00-08:59 @  60%",
                                            "09:00-17:59 @  40%",
                                            "18:00-21:59 @  60%",
                                            "22:00-23:59 @ 100%"));

        // Use the profiles wor weekdays (Mon-Fri) and weekend (Sat,Sun)
        final List<CpuProfile> profiles = CollectionUtils.toList(
                                            weekday,  // Mon
                                            weekday,  // Tue
                                            weekday,  // Wed
                                            weekday,  // Thu
                                            weekday,  // Fri
                                            weekend,  // Sat
                                            weekend); // Sun

        final DynamicCpuLimit dynamicCpuLimit = new DynamicCpuLimit(profiles);

        // Even though the profile has a minute resolution the 
        // 'formatProfilesAsTableByHour' function prints the overview
        //  table at an hour resolution for simplicity!
        final String s = dynamicCpuLimit.formatProfilesAsTableByHour();
        System.out.println(s);
    }
}
```

```
---------------------------------------------------------
Time        Mon    Tue    Wed    Thu    Fri    Sat    Sun
---------------------------------------------------------
00:00      100%   100%   100%   100%   100%   100%   100%
01:00      100%   100%   100%   100%   100%   100%   100%
02:00      100%   100%   100%   100%   100%   100%   100%
03:00      100%   100%   100%   100%   100%   100%   100%
04:00      100%   100%   100%   100%   100%   100%   100%
05:00      100%   100%   100%   100%   100%   100%   100%
06:00       50%    50%    50%    50%    50%    60%    60%
07:00       50%    50%    50%    50%    50%    60%    60%
08:00       50%    50%    50%    50%    50%    60%    60%
09:00        0%     0%     0%     0%     0%    40%    40%
10:00        0%     0%     0%     0%     0%    40%    40%
11:00        0%     0%     0%     0%     0%    40%    40%
12:00        0%     0%     0%     0%     0%    40%    40%
13:00        0%     0%     0%     0%     0%    40%    40%
14:00        0%     0%     0%     0%     0%    40%    40%
15:00        0%     0%     0%     0%     0%    40%    40%
16:00        0%     0%     0%     0%     0%    40%    40%
17:00        0%     0%     0%     0%     0%    40%    40%
18:00       50%    50%    50%    50%    50%    60%    60%
19:00       50%    50%    50%    50%    50%    60%    60%
20:00       50%    50%    50%    50%    50%    60%    60%
21:00       50%    50%    50%    50%    50%    60%    60%
22:00      100%   100%   100%   100%   100%   100%   100%
23:00      100%   100%   100%   100%   100%   100%   100%
---------------------------------------------------------
```

### A dynamic profile

```java
import java.time.LocalDateTime;
import java.util.function.Function;

import com.github.jlangch.aviron.admin.DynamicCpuLimit;


public class DynamicCpuLimitExample3 {

    public static void main(String[] args) {
        try {
            new DynamicCpuLimitExample3().test();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void test() throws Exception {
        final Function<LocalDateTime,Integer> limitFn = 
                (t) -> { final int hour = t.getHour();
                         final int day = t.getDayOfWeek().getValue();
                         if (hour < 8)   return 100;
                         if (hour >= 20) return 100;
                         else            return day > 5 ? 60 : 30; };

        final DynamicCpuLimit dynamicCpuLimit = new DynamicCpuLimit(limitFn);

        // Even though the profile has a minute resolution the 
        // 'formatProfilesAsTableByHour' function prints the overview
        //  table at an hour resolution for simplicity!
        final String s = dynamicCpuLimit.formatProfilesAsTableByHour();
        System.out.println(s);
    }
}
```

```
---------------------------------------------------------
Time        Mon    Tue    Wed    Thu    Fri    Sat    Sun
---------------------------------------------------------
00:00      100%   100%   100%   100%   100%   100%   100%
01:00      100%   100%   100%   100%   100%   100%   100%
02:00      100%   100%   100%   100%   100%   100%   100%
03:00      100%   100%   100%   100%   100%   100%   100%
04:00      100%   100%   100%   100%   100%   100%   100%
05:00      100%   100%   100%   100%   100%   100%   100%
06:00      100%   100%   100%   100%   100%   100%   100%
07:00      100%   100%   100%   100%   100%   100%   100%
08:00       30%    30%    30%    30%    30%    60%    60%
09:00       30%    30%    30%    30%    30%    60%    60%
10:00       30%    30%    30%    30%    30%    60%    60%
11:00       30%    30%    30%    30%    30%    60%    60%
12:00       30%    30%    30%    30%    30%    60%    60%
13:00       30%    30%    30%    30%    30%    60%    60%
14:00       30%    30%    30%    30%    30%    60%    60%
15:00       30%    30%    30%    30%    30%    60%    60%
16:00       30%    30%    30%    30%    30%    60%    60%
17:00       30%    30%    30%    30%    30%    60%    60%
18:00       30%    30%    30%    30%    30%    60%    60%
19:00       30%    30%    30%    30%    30%    60%    60%
20:00      100%   100%   100%   100%   100%   100%   100%
21:00      100%   100%   100%   100%   100%   100%   100%
22:00      100%   100%   100%   100%   100%   100%   100%
23:00      100%   100%   100%   100%   100%   100%   100%
---------------------------------------------------------
```


## Controlling the Clamd CPU usage

*documentation work in progress...*


## Getting the latest release

You can can pull it from the central Maven repositories:

```java
<dependency>
  <groupId>com.github.jlangch</groupId>
  <artifactId>aviron</artifactId>
  <version>1.5.2</version>
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
