#!/bin/bash
#
runDir=`dirname $0`
#
# Start the wrapper, setting JVM library path in case some modules
# rely on native libraries.
java -Djava.library.path="${runDir}/bin/lib:$HOME/.datalift/lib" \
     -jar "${runDir}/bin/datalift-wrapper.jar" "$@" "${runDir}"
#
