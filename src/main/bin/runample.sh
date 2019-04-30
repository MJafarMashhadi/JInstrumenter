#!/bin/sh

# Installation directory
INSTALL=..
#/Users/mjafar/Downloads/jinstrumenter-1.0

#========================== No need to edit anything from here =========================

LIBDIR=${INSTALL}/lib

for jar in $( find $LIBDIR -name "*.jar" | sort -r )
do
    CLASSPATH=$jar:${CLASSPATH}
done

# remove last ':'
CLASSPATH=${CLASSPATH:0:${#CLASSPATH} - 1}

$echo $CLASSPATH

java -Xmx1000M -Xbootclasspath/a:$CLASSPATH -javaagent:$INSTALL/lib/jinstrumenter-1.0.jar "$@"
