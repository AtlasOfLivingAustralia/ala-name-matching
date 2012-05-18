#!/bin/bash
# Make sure only tomcat can run our script
if [ $(whoami) != "tomcat" ]
then
   echo "This script must be run as the tomcat user."
   exit 1
fi

BIOCACHE_INSTALL=/usr/local/biocache
CLASSPATH=$BIOCACHE_INSTALL:$BIOCACHE_INSTALL/biocache.jar
java -Xmx16g -Xmx16g -cp $CLASSPATH -Dactors.corePoolSize=8 -Dactors.maxPoolSize=16 -Dactors.minPoolSize=8 au.org.ala.util.CommandLineTool