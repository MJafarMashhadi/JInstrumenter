============= Release notes Ample 1.0 ===============
This is a reimplementation of the original engine. It no longer requires
instrumentation before a program can be analyzed.  Ample uses the agent
interface in JDK 1.5 to analyze patterns on the fly.  

Before you start:
================

- You need to edit all scripts found in "bin" and specify the installation 
  directory of Ample and the JDK. 
- In order for Ample to work in all environments (programs that use their
  own classloaders), we need to patch the system library and add a class
  java.lang.ample.CallSequenceSetRecorder . The package provides a script
  "bin/updatejre.sh" to do exactly this for you (tested only with Linux
  JDKs). The sole parameter for the script is the installation directory of
  the JDK you want to use.

Collecting Patterns:
===================

To collect patterns from a run, you need to invoke the virtual machine
such that the Ample agent is loaded prior to the execution of the program.
There is a script "bin/runample.sh" that sets up an appropriate classpath
and passes the required parameters. Remember to specify required values
in the script before you use it.

Note: You can pass two properties that influence the way Ample records
patterns:

ample.patternfile       the name of the file were patterns are stored (default "patterns.ser")
ample.windowsize        the size of the call window used (default 5)

Example: To store patterns with a window size of 5 in file "failingpatterns.ser",
run

  bin/runample.sh -Dample.patternfile=failingpatterns.ser -Dample.windowsize=5

Calcualting a Ranking:
=====================

To calculate a ranking from at least one failing and one or
more passing pattern sets, you can execute script "bin/rankpatterns.sh".
The first parameter for the script is the name of the file with failing
patterns, subsequent parameters are the passing pattern files. The ranking
is output to standard out.
