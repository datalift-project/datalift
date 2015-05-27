#!/bin/bash
#
runDir=`dirname $0`
#
# Start the wrapper, setting JVM library path in case some modules
# rely on native libraries.
java -Djava.library.path="${runDir}/bin/lib:$HOME/.datalift/lib" \
     -Xms256M -Xmx3G -Xverify:all \
     -Djava.net.useSystemProxies=true \
     -jar "${runDir}/bin/datalift-wrapper.jar" "$@" "${runDir}"
#
# Supplementary options for 64bit JVMs:
#     -XX:+UseCompressedOops \
#
# Supplementary options for Java 8 Update 20 and later:
#     -XX:+UseCompressedClassPointers \
#     -XX:+UseG1GC -XX:+UseStringDeduplication \
#
