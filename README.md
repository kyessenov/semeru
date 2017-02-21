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
The user of this tool records a short demonstration of a framework feature, and the tool generates the code
necessary to reproduce this feature by matching the demo trace against a large body of collected framework usage traces

Build Instructions
==================

Semeru is implemented in Scala and Java, and relies on MySQL, Neo4J and Lucene for storage backends.

1. You will need to add tools.jar to the build path to access JVM instrumentation classes.

        ln -s path-to-JDK/tools.jar lib

2. Use the build tool to compile the code:

        ./sbt update compile
    
3. Tests require a working database set-up. Run the tests as follows:
     
        ./sbt test

4. Web interface is the recommended way to interact with the tool. Launch the web interface as follows:

        ./sbt run
    
   You should be able to access it then by visiting `localhost:8080`.

_Note_:
SBT has an Eclipse plug-in to automatically generate a project for Scala IDE.

    ./sbt eclipse

_Note_:
To enable debugging in Scala IDE, create a new Remote Scala/Java Application debug configuration. Launch `sbt` as follows:

    ./sbt -jvm-debug 8000
    
## Dependencies

1. You need a JVM. 64-bit HotSpot JVM version 1.8.0_111 has been tested.

2. You need MySQL version >= 5.7. Allow password-less local root access.

3. Various caches are built from MySQL canonical traces in `var` directory under the repository root. These caches are created on-demand. Please provide sufficient storage for the pre-processing output.

_Note_:
A base set of traces used by the test cases and experiments can be downloaded from [a backup of MySQL data directory](http://groups.csail.mit.edu/cap/semeru/semeru-dataset.tar.gz). Use the data dump as the data directory in MySQL, and give the tool some time (~ several hours) to recreate the caches.

## Trace collection and ingestion

_We strongly recommend using SSD drives to collect and store execution traces_.

Collector communicates with the processor via two files by default:

1. `/tmp/log.bin` -- for raw trace events.

2. `/tmp/metadata.bin` -- for declarations of classes, fields, and methods, extracted from the byte code.

Trae processor ingests the data into MySQL by a bulk import of CSV files. The temporary CSV files are placed into `csv` directory under the repository root.

## Code structure

Below is a brief overview of the code organization by directory:

1. `config/` contains configuration for the trace collector for various Java applications - which trace events to record, which classes to treat as library or user or skip completely due to excessive collection overhead.

2. `data/` contains test artifacts and data files used by the word synonym engine and javadoc extraction engine.

3. `jni/` contains machine learning native libraries (not currently in-use).

4. `logs/` contains trace recording logs from large applications (for record keeping).

5. `project/` is used by SBT to configure the build environment.

6. `target/` contains the build output.

7. `web/` contains the implementation of the web UI. Scalatra sources are in `web/WEB-INF/` directory. The configuration for the UI (such as the list of traces and trace groups) is in `web/config.json` file.

8. `var/` contains Neo4J graph databases, Lucene indexes, and caches per each trace.

9. `src/main/java` is the root directory for Java instrumentation agent.

10. `src/main/scala` is the root directory for the ingestion and analysis workflow code.

    1. Start with `src/main/scala/TraceModel.scala` to understand the core model classes.
    
    2. Look at `src/main/scala/analysis/Analysis.scala` to look at the various trace analyses implemented by the system.
    
    3. Additionally, take a look at the following directories: `db` has database backend adaptors, `ingest` is the trace ingestion code, `web` is the web UI server code, `experiments` contains scripts to execute trace collection on various pre-declared applications.

## Example: trace collection

_Note_:
The scripts to collect traces expect to have `semeru-data` directory next to `semeru` directory that contains JAR files for the subject applications. 

Execute the following command to collect jEdit complete traces:

    ./sbt runMain edu.mit.csail.cap.query.experiments.Experiments jedit false jedit_test

launches jEdit in full-collection mode (`false`) and stores the trace data into `jedit_test` trace database. 

To record a demo trace, pass `true` to the script. Then use `manage.sh` to communicate with the injected instrumentation agent over a network socket.
