A JGit SDK-backed FileSystem driver for Hadoop
==============================================

This is an experimental FileSystem for Hadoop that uses the JGit SDK. 
This has not been heavily tested yet. Use at your own risk.

Features:

- Clones each given repo+branch once and uses a background thread to fetch updates
- Proxies through to a read-only local filesystem driver for high speed
- Default packaging uses an uber-jar for easy deployment


Build Instructions
------------------

Build using maven:

```shell
$ mvn package -DskipTests=true
```

Copy jar and various dependencies to your hadoop libs dir 
(run 'hadoop classpath' to find appropriate lib dir):

```shell
$ cp target/hadoop-jgit-fs-0.1.jar /usr/lib/hadoop/lib/
```

Add the following keys to your core-site.xml file:

```xml
<!-- necessary for Hadoop to load our filesystem driver -->
<property>
  <name>fs.git.impl</name>
  <value>com.simiacryptus.hadoop_jgit.GitFileSystem</value>
</property>
```

You should now be able to run commands:

```shell
$ hadoop fs -ls jgit://bucketname/foo
```

Tunable parameters
------------------

These may or may not improve performance. The defaults were set without 
much testing.

- **fs.jgit.fetch.lazy** - Frequency (in seconds) of foreground fetches 
- **fs.jgit.fetch.eager** - Frequency (in seconds) of background fetches
- **fs.jgit.dismount.seconds** - Idle time (in seconds) to dismount repo driver
- **fs.jgit.dismount.delete** - If true, files will be removed when repo driver dismounts
- **fs.jgit.datadir** - Data directory to use for local storage
- **fs.jgit.auth.user** - Username for authentication (Optional)
- **fs.jgit.auth.pass** - Password for authentication (Optional)

Caveats
-------

This is currently implemented as a FileSystem and not a AbstractFileSystem.

Changes
-------

0.1

- Created