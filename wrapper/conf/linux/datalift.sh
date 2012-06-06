#!/bin/bash
#
for i in bin/*.jar
do
  CLASSPATH="${CLASSPATH}:${i}"
done
export CLASSPATH
#
java org.datalift.wrapper.Wrapper
#
