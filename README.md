A JGit SDK-backed FileSystem driver for Hadoop
==============================================

This is an experimental FileSystem for Hadoop that uses the JGit SDK. 
This has not been heavily tested yet. Use at your own risk.

Features:

- Nothing Yet


Build Instructions
------------------

Build using maven:

```shell
$ mvn package -DskipTests=true
```

Copy jar and various dependencies to your hadoop libs dir 
(run 'hadoop classpath' to find appropriate lib dir):

```shell
$ cp target/hadoop-jgit-0.0.5.jar \
     target/lib/httpcore-4.2.jar \
     target/lib/httpclient-4.2.jar \
     target/lib/jackson-databind-2.1.1.jar \
     target/lib/jackson-core-2.1.1.jar \
     target/lib/jackson-annotations-2.1.1.jar \
     target/lib/joda-time-2.3.jar \
     /usr/lib/hadoop/lib/
```

Note: These are dependencies that are necessary for CDH 5 which is based on
Hadoop 2.2.0. There is a chance you'll need other dependencies for different
versions located in the target/lib dir.

Also, by default this builds against Hadoop 2.2.0. If you wish to build 
against a different version, edit the pom.xml file.

Add the following keys to your core-site.xml file:

```xml
<!-- omit for IAM role based authentication -->
<property>
  <name>fs.jgit.access.key</name>
  <value>...</value>
</property>

<!-- omit for IAM role based authentication -->
<property>
  <name>fs.jgit.secret.key</name>
  <value>...</value>
</property>

<property>
  <name>fs.jgit.buffer.dir</name>
  <value>${hadoop.tmp.dir}/jgit</value>
</property>

<!-- necessary for Hadoop to load our filesystem driver -->
<property>
  <name>fs.jgit.impl</name>
  <value>org.apache.hadoop.fs.jgit.jgitFileSystem</value>
</property>
```

You probably want to add this to your log4j.properties file:

```ini
log4j.logger.org.apache.hadoop.fs.jgit.jgitFileSystem=WARN
```
You should now be able to run commands:

```shell
$ hadoop fs -ls jgit://bucketname/foo
```


Tunable parameters
------------------

These may or may not improve performance. The defaults were set without 
much testing.

- fs.jgit.foo - Some placeholder configuration parameter

Caveats
-------

This is currently implemented as a FileSystem and not a AbstractFileSystem.

Changes
-------

0.0.1

- Created