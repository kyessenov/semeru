#!/bin/bash
java -Xmx10G -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -jar sbt-launch.jar "$@"
