#!/bin/bash

cd `dirname "$0"`

java -classpath "`dirname "$0"`/bin" -Xms16m -Xmx1024m com.asofterspace.mediaSorter.Main "$@"
