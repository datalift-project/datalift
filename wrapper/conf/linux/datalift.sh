#!/bin/bash
#
curDir=`pwd`
runDir=`dirname $0`
cd "$runDir"
#
# Add all wrapper JARs to classpath.
for i in bin/*.jar
do
  CLASSPATH="${CLASSPATH}:${i}"
done
export CLASSPATH
#
# Add native libraries directories to JVM library path,
# in case some modules rely on native libraries.
JAVA_OPTS="${JAVA_OPTS} -Djava.library.path=bin/lib:$HOME/.datalift/lib"
#
# Start wrapper.
java ${JAVA_OPTS} org.datalift.wrapper.Wrapper
#
cd "$curDir"
#
