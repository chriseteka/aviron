[![](https://github.com/jlangch/aviron/blob/master/doc/maven-central.svg)](https://search.maven.org/search?q=a:%22aviron%22%20AND%20g:%22com.github.jlangch%22)
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

Enable _TCPSocket_ and _TCPAddr_ configuration parameters in _clamd.conf_:

```
TCPSocket 3310

TCPAddr localhost
```

Start _clamd_ as foreground process for testing:

```sh
clamd --foreground
```


Java example:

```java
final String baseDir = "/data/files/";

final Client client = new Client.Builder()
                              .serverHostname("localhost")
                              .serverFileSeparator(FileSeparator.UNIX)
                              .build();

System.out.println("Reachable: " + client.isReachable());

// scan single file
System.out.println(client.scan(Paths.get(baseDir, "document.pdf")));

// scan dir (recursive)
System.out.println(client.scan(Paths.get(baseDir), true));

// scan streamed data
final byte[] data = Files.readAllBytes(Paths.get(baseDir, "document.pdf"));
System.out.println(client.scan(new ByteArrayInputStream(data)));
```


## Getting the latest release

You can can pull it from the central Maven repositories:

```java
<dependency>
  <groupId>com.github.jlangch</groupId>
  <artifactId>aviron</artifactId>
  <version>1.0.0</version>
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
