# Installation & Setup


## Installation

[Install ClamAV on MacOS](https://www.liquidweb.com/blog/install-clamav/)

[Install ClamAV on AlmaLinux](https://truehost.com/support/knowledge-base/how-to-install-clamav-for-malware-scanning-on-linux/)



## ClamAV configuration

The Clam AV Java client communicates over the TCP Socket at port 3310 with the Clam daemon. 
Make sure the port is enabled in the clamd.conf configuration file.

Enable _TCPSocket_ and _TCPAddr_ in clamd.conf:

```
TCPSocket 3310

TCPAddr localhost
```


## ClamAV config files on MacOS

After installing ClamAV with the Homebrew Package Manager for macOS the configuration files
are available at:

/opt/homebrew/etc/clamav/clamd.conf

/opt/homebrew/etc/clamav/freshclam.conf



## ClamAV Documentation

[Install ClamAV](https://www.liquidweb.com/blog/install-clamav/)

[ClamAV Manual](https://docs.clamav.net/manual/Usage.html)

[Clamd Man Pages](https://linux.die.net/man/8/clamd)

[Install ClamAV on AlmaLinux](https://truehost.com/support/knowledge-base/how-to-install-clamav-for-malware-scanning-on-linux/)

[Clamd stuck at 100% CPU](https://anset.org/2019/08/30/fixing-clamd-stuck-at-100-cpu/)



## Clamd Help

```
clamd --help

                      Clam AntiVirus: Daemon 1.4.2
           By The ClamAV Team: https://www.clamav.net/about.html#credits
           (C) 2024 Cisco Systems, Inc.


    clamd [options]

    --help                   -h             Show this help
    --version                -V             Show version number
    --foreground             -F             Run in foreground; do not daemonize
    --debug                                 Enable debug mode
    --log=FILE               -l FILE        Log into FILE
    --config-file=FILE       -c FILE        Read configuration from FILE
    --fail-if-cvd-older-than=days           Return with a nonzero error code if virus database outdated
    --datadir=DIRECTORY                     Load signatures from DIRECTORY
    --pid=FILE               -p FILE        Write the daemon's pid to FILE
```
