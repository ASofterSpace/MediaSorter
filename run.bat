@echo off

cd /D %~dp0

java -classpath "%~dp0\bin" -Xms16m -Xmx1024m com.asofterspace.mediaSorter.Main %*

pause
