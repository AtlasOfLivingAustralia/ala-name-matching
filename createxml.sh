cd target
jar xf sds-1.0-SNAPSHOT-assembly.jar lib lib
java -classpath sds-1.0-SNAPSHOT.jar:lib au.org.ala.sds.util.SensitiveSpeciesXmlBuilder
cd ..