#!/bin/sh
while sleep 1; do echo `date +"%s"` `stat -f%z /tmp/log.bin` `stat -f%z /tmp/metadata.bin`; done

