#!/bin/sh
java -Xverify:none -javaagent:agent.jar=config/demo_swing $@
