jar xf biocache-jms-1.0-SNAPSHOT-assembly.jar lib lib

export CLASSPATH=.:biocache-jms-1.0-SNAPSHOT-assembly.jar

echo "JMS Listener : running ActiveMQ JMS Listener $('date')"
nohup java -Xmx1g -Xms1g -classpath $CLASSPATH org.ala.jms.service.JmsListenerService &

