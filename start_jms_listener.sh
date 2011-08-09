#
# mvn assembly:assembly to include classpath in manifest.mf
#
#
jar xf biocache-jms-1.0-SNAPSHOT-assembly.jar lib lib

export CLASSPATH=biocache-jms-1.0-SNAPSHOT-assembly.jar:.:./lib

echo "JMS Listener : running ActiveMQ JMS Listener $('date')"
java -Xmx1g -Xms1g -classpath $CLASSPATH org.ala.jms.service.JmsListenerService

