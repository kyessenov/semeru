#!/bin/bash
java -Xmx8G -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -jar sbt-launch.jar "$@"
