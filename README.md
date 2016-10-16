```
     ____
    / ___|  ___ _ __ ___   ___ _ __ _   _
    \___ \ / _ \ '_ ` _ \ / _ \ '__| | | |
     ___) |  __/ | | | | |  __/ |  | |_| |
    |____/ \___|_| |_| |_|\___|_|   \__,_|
```

About
=====

Programming assistant using dynamic traces.

This repository contains the trace collection and aggregation tool, and a series of analysis
built on top of it to produce symbolic code from a demo trace query.

Essentially, this is programming-by-demonstration paradigm applied to software frameworks.
The user of this records a short demonstration of a framework feature, and the tool generates the code
to reproduce this feature by matching the trace against a large body of collected traces
of usages of the framework.


Build Instructions
==================

You will need to add tools.jar to the build path to access JVM instrumentation classes.

    $ ln -s path-to-JDK/tools.jar lib
    $ ./sbt update compile

SBT has an Eclipse plug-in to automatically generate a project for Scala IDE.

    $ ./sbt eclipse

To clean the build state:

    $ ./sbt clean

To enable debugging:

    $ ./sbt -jvm-debug 8000

In Scala IDE, create a new Remote Scala/Java Application debug configuration.

## Temporary files

Strongly recommended to run on an SSD drive!

Collector communicates with the processor via two files by default:
+ /tmp/log.bin -- trace events
+ /tmp/metadata.bin -- definitions of classes, fields, and methods

Processor ingests the data by bulk import of CSV files. By default, these files are placed into `csv` directory.

## Overview of the sources

Java sources contain instrumentation code. Scala code in "ingest" directory builds Java agent JAR.

Inside "experiments" there is a main class to launch external Java applications and collect traces from them. For example:

    ./sbt runMain edu.mit.csail.cap.query.experiments.Experiments jedit false jedit_all

launches jEdit in full-collection mode and stores the trace data into "jedit_all" database. Configurations for applications are in "config" directory.

## Testing

### Unit tests

    ./sbt test

### Query web interface. 

    ./sbt run
