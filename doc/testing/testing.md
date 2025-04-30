# Test


## Testing the Clam AV Java client 

Run the clamd daemon in the foreground

```
clamd --foreground --log=/path/to/file
```


## Refresh the Clam AV virus database

Just run

```
freshclam
```

## Manually scan directories

To scan the current working directory, you can execute the following command.

```
clamscan
```

To scan a particular file, you can execute the following command, substituting /path/to/file with the actual file path.

```
clamscan /path/to/file
```

You can execute the following command to scan all files in a directory recursively.

```
clamscan -r /path/to/directory
```
