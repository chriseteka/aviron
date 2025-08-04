[![](https://github.com/jlangch/aviron/blob/master/doc/maven-central.svg)](https://central.sonatype.com/artifact/com.github.jlangch/aviron)
[![](https://github.com/jlangch/aviron/blob/master/doc/license.svg)](./LICENSE)
![Java Version](https://img.shields.io/badge/java-%3E%3D%201.8-success)

[![Release (latest by date)](https://img.shields.io/github/v/release/jlangch/aviron)](https://github.com/jlangch/aviron/releases/latest)
[![Release Date](https://img.shields.io/github/release-date/jlangch/aviron?color=blue)](https://github.com/jlangch/aviron/releases/latest)
[![GitHub commits since latest release (by date)](https://img.shields.io/github/commits-since/jlangch/aviron/latest)](https://github.com/jlangch/aviron/commits/)



# Aviron

**Aviron** is a zero dependency [ClamAV](https://www.clamav.net/) Java 
client library. It requires Java 8+.

ClamAV® is an open-source antivirus engine for detecting trojans, viruses, malware & other malicious threats.

**Aviron** features:

* Send documents through a socket from within a Java application to the 
  *clamd* daemon for on-the-fly virus scanning
* Send virus scan requests for files and directories to the *clamd* daemon
* Build virus scanners with support for quarantine. Infected files can be
  moved/copied to a quarantine directory
* Dynamically limit the *clamd* daemon CPU usage through day/time-based CPU
  limit profiles
* The quarantine and CPU limit features are optional
* File watchers for watching new, modified, or deleted files or directories.
* Available for Linux and MacOS (battle tested on Alma Linux)

The ClamAV tools ([clamd](https://linux.die.net/man/8/clamd), [clamdscan](https://linux.die.net/man/1/clamdscan), [clamscan](https://linux.die.net/man/1/clamscan)) 
are just virus scanners. 
They lack support for putting infected files into quarantine and do not have 
any CPU limiting features.


## Table of Content

* [Scan Examples](#scan-examples)
    * [Simple scanning](#simple-scanning)
    * [Scanning with quarantine support](#scanning-with-quarantine-support)


* [Defining Clamd CPU profiles](#defining-clamd-cpu-profiles)
    * [Simple day profile](#simple-day-profile)
    * [Weekday/weekend profile](#weekday-weekend-profile)
    * [Dynamic profile](#dynamic-profile)


* [File Watcher](#file-watcher)


* [Controlling the Clamd CPU usage](#controlling-the-clamd-cpu-usage)



## Change Log

[Change Log](ChangeLog.md)


## Scan Examples

Enable *TCPSocket* and *TCPAddr* configuration parameters in *clamd.conf*:

```
TCPSocket 3310

TCPAddr localhost
```

Start **clamd** as foreground process for easy testing (on Linux production systems **clamd** is run as a **systemd** service):

```sh
clamd --foreground
```


### Simple scanning

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


### Scanning with quarantine support

Infected files can be moved/copied implicitly to a quarantine directory. 
Whether an infected file is moved or copied can be controlled by the 
*quarantineFileAction* configuration parameter.

Note: 

In COPY mode unaltered infected files are copied only once to the quarantine 
directory no matter how many times they get rescanned. **Aviron** uses a hash code 
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
option. **Aviron** allows the definition of CPU profiles to control the *clamd* CPU
usage.


**Note:**

A 100% CPU limit corresponds to a full load of a single core *ARM* based CPU or a 
single *hyper-thread* on *INTEL* CPUs.


### Simple day profile

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
        // table at an hour resolution for simplicity!
        final String s = dynamicCpuLimit.formatProfilesAsTableByHour();
        System.out.println(s);
    }
```

CPU profile table:

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


### Weekday/weekend profile

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
        // table at an hour resolution for simplicity!
        final String s = dynamicCpuLimit.formatProfilesAsTableByHour();
        System.out.println(s);
    }
}
```

CPU profile table:

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

### Dynamic profile

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
        // table at an hour resolution for simplicity!
        final String s = dynamicCpuLimit.formatProfilesAsTableByHour();
        System.out.println(s);
    }
}
```

CPU profile table:

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


## File Watcher

This example demonstrates watching for file and directory changes.

On Linux the Aviron file watcher is based on Java's file **WatchService** 
that relies on *inotify*, a Linux kernel subsystem that reports file system
changes to applications.

On MacOS the Aviron file watcher is based on the **fspath** tool that uses
the FreeBSD's *kqueue* for file notifications.

In order run this example on MacOS the **fswatch** tool must be installed. The 
Java **WatchService** does not run properly on MacOS.

Installing *fswatch* via Homebrew:

```
    brew install fswatch
```

See:
* [fswatch Installation](https://formulae.brew.sh/formula/fswatch)
* [fswatch Github](https://github.com/emcrisostomo/fswatch)
* [fswatch Manual](https://emcrisostomo.github.io/fswatch/doc/1.17.1/fswatch.html)


```java
import java.nio.file.Path;

import com.github.jlangch.aviron.ex.FileWatcherException;
import com.github.jlangch.aviron.filewatcher.FileWatcher_FsWatch;
import com.github.jlangch.aviron.filewatcher.FileWatcher_JavaWatchService;
import com.github.jlangch.aviron.filewatcher.IFileWatcher;
import com.github.jlangch.aviron.filewatcher.events.FileWatchErrorEvent;
import com.github.jlangch.aviron.filewatcher.events.FileWatchFileEvent;
import com.github.jlangch.aviron.filewatcher.events.FileWatchTerminationEvent;
import com.github.jlangch.aviron.impl.util.OS;
import com.github.jlangch.aviron.util.DemoFilestore;


public class FileWatcherExample {

    public static void main(String[] args) {
        try {
            new FileWatcherExample().run();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void run() throws Exception {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            demoFS.createFilestoreSubDir("000");
            demoFS.createFilestoreSubDir("001");

            final Path mainDir = demoFS.getFilestoreDir().toPath();
            final boolean registerAllSubDirs = true;

            try(final IFileWatcher fw = createPlatformFileWatcher(mainDir, registerAllSubDirs)) {
                fw.setFileListener(this::onFileEvent);
                fw.setErrorListener(this::onErrorEvent);
                fw.setTerminationListener(this::onTerminationEvent);

                fw.start();

                printf("Ready to watch «%s»%n%n", mainDir);

                // wait a bit between actions, otherwise fswatch discards event
                // due to optimizations in regard of the file delete at the end!

                demoFS.touchFilestoreFile("000", "test1.data");      // created
                sleep(1);

                demoFS.appendToFilestoreFile("000", "test1.data");   // modified
                sleep(1);

                demoFS.deleteFilestoreFile("000", "test1.data");     // deleted

                // wait for all events to be processed before closing the watcher
                sleep(3);
            }

            // wait to receive the termination event
            sleep(1);
        }
    }


    private IFileWatcher createPlatformFileWatcher(
            final Path mainDir, 
            final boolean registerAllSubDirs
    ) {
        if (OS.isLinux()) {
            return new FileWatcher_JavaWatchService(mainDir, registerAllSubDirs);
        }
        else if (OS.isMacOSX()) {
            return new FileWatcher_FsWatch(
                         mainDir,
                         registerAllSubDirs,
                         null, // default fswatch monitor
                         FileWatcher_FsWatch.HOMEBREW_FSWATCH_PROGRAM);
        }
        else {
            throw new FileWatcherException(
                    "FileWatcher is not supported on platforms other than Linux/MacOS!");
        }
    }

    private void onFileEvent(final FileWatchFileEvent event) {
        if (event.isFile()) {
            printf("File Event: %-8s %s%n", event.getType(), event.getPath());
        }
        else if (event.isDir()) {
            printf("Dir Event:  %-8s %s%n", event.getType(), event.getPath());
        }
    }

    private void onErrorEvent(final FileWatchErrorEvent event) {
        printf("Error:      %s %s%n", event.getPath(), event.getException().getMessage());
    }

    private void onTerminationEvent(final FileWatchTerminationEvent event) {
        printf("Terminated: %s%n", event.getPath());
    }

    private void printf(final String format, final Object... args) {
        synchronized(lock) {
            System.out.printf(format, args);
        }
    }

    private void sleep(final int seconds) {
        try { Thread.sleep(seconds * 1000); } catch(Exception ex) {}
    }


    private final Object lock = new Object();
}
```


## Controlling the Clamd CPU usage

This example demonstrates how a demo filestore can be continuously scanned 
honoring a *clamd* CPU profile.

In the examples package you find two variants:
* ClamdCpuLimiterExample1 (explicit cpu limit update)
* ClamdCpuLimiterExample2 (scheduled cpu limit update)


Our demo file store used for virus scanning looks like:

```
 * demo/
 *   |
 *   +-- filestore/
 *   |     |
 *   |     +-- 000
 *   |     |     \_ file1.doc
 *   |     |     \_ file2.doc
 *   |     |     :
 *   |     |     \_ fileN.doc
 *   |     +-- 001
 *   |     |     \_ file1.doc
 *   |     |     :
 *   |     |     \_ fileN.doc
 *   |     :
 *   |     +-- NNN
 *   |           \_ file1.doc
 *   |
 *   +-- quarantine/
 *         \_ eicar.txt
 *         \_ eicar.txt.virus
```

and the demo code:

```java
import static com.github.jlangch.aviron.impl.util.CollectionUtils.toList;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.jlangch.aviron.Client;
import com.github.jlangch.aviron.FileSeparator;
import com.github.jlangch.aviron.admin.ClamdAdmin;
import com.github.jlangch.aviron.admin.ClamdCpuLimiter;
import com.github.jlangch.aviron.admin.CpuProfile;
import com.github.jlangch.aviron.admin.DynamicCpuLimit;
import com.github.jlangch.aviron.events.QuarantineEvent;
import com.github.jlangch.aviron.events.QuarantineFileAction;
import com.github.jlangch.aviron.util.DemoFilestore;
import com.github.jlangch.aviron.util.IDirCycler;

public class ClamdCpuLimiterExample1 {

    public static void main(String[] args) {
        try {
            new ClamdCpuLimiterExample1().scan();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void scan() throws Exception {
        try(DemoFilestore demoFS = new DemoFilestore()) {
            demoFS.populateWithDemoFiles(5, 10);  // 5 sub dirs, each with 10 files

            // demoFS.createEicarAntiMalwareTestFile("000");

            final Client client = new Client.Builder()
                                            .serverHostname("localhost")
                                            .serverFileSeparator(FileSeparator.UNIX)
                                            .quarantineFileAction(QuarantineFileAction.MOVE)
                                            .quarantineDir(demoFS.getQuarantineDir())
                                            .quarantineEventListener(this::onQuarantineEvent)
                                            .build();

            // Use the same day profile for Mon - Sun
            final CpuProfile everyday = CpuProfile.of(
                                            "weekday",
                                            toList(
                                                "00:00-05:59 @ 100%",
                                                "06:00-08:59 @  50%",
                                                "09:00-17:59 @   0%",
                                                "18:00-21:59 @  50%",
                                                "22:00-23:59 @ 100%"));

            final String clamdPID = ClamdAdmin.getClamdPID();

            final ClamdCpuLimiter limiter = new ClamdCpuLimiter(new DynamicCpuLimit(everyday));

            // get a IDirCycler to cycle sequentially through the demo file 
            // store directories:  "000" ⇨ "001" ⇨ ... ⇨ "NNN" ⇨ "000" ⇨ ... 
            final IDirCycler fsDirCycler = demoFS.getFilestoreDirCycler();

            // inital CPU limit after startup
            initialCpuLimit(limiter, clamdPID);

            // scan the file store directories in an endless loop until we get 
            // killed or stopped
            while(!stop.get()) {
                // update clamd CPU limit 
                final int limit = updateCpuLimit(limiter, clamdPID);

                if (limit >= MIN_SCAN_LIMIT_PERCENT) {
                    // scan next filestore directory
                    final File dir = fsDirCycler.nextDir();
                    System.out.println(client.scan(dir.toPath(), true));
                }
                else {
                    Thread.sleep(30_000);  // wait 30s
                }
            }
        }
    }

    private void initialCpuLimit(final ClamdCpuLimiter limiter, final String clamdPID) {
        limiter.activateClamdCpuLimit(clamdPID);
        System.out.println(String.format(
                            "Initial clamd CPU limit: %d%%",
                            limiter.getLastSeenLimit()));
    }

    private int updateCpuLimit(final ClamdCpuLimiter limiter, final String clamdPID) {
        // note: applied only if the new limit differs from the last one
        final int lastSeenLimit = limiter.getLastSeenLimit();
        if (limiter.activateClamdCpuLimit(clamdPID)) {
            final int newLimit = limiter.getLastSeenLimit();
            System.out.println(String.format(
                                "Adjusted clamd CPU limit: %d%% -> %d%%",
                                lastSeenLimit, newLimit));
            return newLimit;
        }
        else {
            return lastSeenLimit;
        }
    }

    private void onQuarantineEvent(final QuarantineEvent event) {
        if (event.getException() != null) {
            System.out.println("Error " + event.getException().getMessage());
        }
        else {
            System.out.println("File " + event.getInfectedFile() + " moved to quarantine");
        }
    }


    private static final int MIN_SCAN_LIMIT_PERCENT = 20;

    private final AtomicBoolean stop = new AtomicBoolean(false);
}
```


## Getting the latest release

You can can pull it from the central Maven repositories:

```java
<dependency>
  <groupId>com.github.jlangch</groupId>
  <artifactId>aviron</artifactId>
  <version>1.6.0</version>
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
