# Change Log


All notable changes to this project will be documented in this file.



## [1.5.0] - 2025-07-26

- Added a ClamdCpuLimiter to dynamically limit the CPU usage of a clamd daemon.



## [1.4.0] - 2025-07-24

- Added method Client::isQuarantineActive()
- Added method Client::listQuarantineFiles()
- Added method Client::removeQuarantineFile()
- Added method Client::removeAllQuarantineFiles()
- The quarantine COPY action will copy the same infected file only once to the 
  quarantine directory under multiple rescans. The full filename and a hash of 
  the file's data are used to identify file equality!
- Reorganized quarantine code



## [1.3.3] - 2025-07-21

- Added method Client::printConfig() to print a client's configuration.
- Fixed a possible race condition



## [1.3.2] - 2025-07-20

- Improved admin functions to control the clamd cpu limit



## [1.3.1] - 2025-07-19

- Added a quarantine handler that allows to move/copy infected files to a 
  quarantine directory



## [1.3.0] - 2025-07-15

- Added method Client::builder() to simplify building a new client



## [1.2.3] - 2025-06-03

- Simplified publishing to Sonatype's Central Maven repository



## [1.2.2] - 2025-06-01

- Fixed auto publishing to Sonatype's Central Maven repository



## [1.2.1] - 2025-05-31

- Changed the Maven publish process for optional auto deployment to Sonatype's 
  new Central Maven repository without disclosing any credentials to 3rd parties.



## [1.2.0] - 2025-05-29

- Migrated to Central Sonatype Maven publishing



## [1.1.0] - 2025-05-05

- Added admin functions to control the CPU limit of the clamd daemon



## [1.0.0] - 2025-05-01

- tested the client -> ready for v1.0.0



## [0.2.0] - 2025-04-30

- overhauled build and documentation
- fixed Maven publish (classes jar was missing)



## [0.1.0] - 2025-04-29

- initial release
