#!/bin/sh

AMPLE_ROOT=/Users/mjafar/Downloads/ample-1.0/target/

java \
    -Xbootclasspath/a:asm-7.0.jar:asm-analysis-7.0.jar:asm-commons-7.0.jar:asm-tree-7.0.jar:asm-util-7.0.jar:commons-collections-1.0.jar:commons-io-1.0.jar:commons-lang-2.1.jar:log4j-1.2.13.jar:util-0.1.jar:$AMPLE_ROOTample-1.0.jar \
    -javaagent:$AMPLE_ROOTample-1.0.jar \
    -jar $1

