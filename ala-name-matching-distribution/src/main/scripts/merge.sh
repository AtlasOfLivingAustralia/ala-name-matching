#!/bin/sh
SCRIPT_HOME=`dirname $0`
JAVA_OPTIONS="${JAVA_OPTIONS} -Xmx6G"
exec java ${JAVA_OPTIONS} -cp "$SCRIPT_HOME/lib/*" au.org.ala.names.index.TaxonomyBuilder $*