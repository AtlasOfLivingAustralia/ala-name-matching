cd ../portal-core
mvn -Dmaven.test.skip=true clean install source:jar eclipse:eclipse
mvn install:install-file -Dfile=target/portal-core-1.0-SNAPSHOT-sources.jar -DartifactId=portal-core -DgroupId=portal -Dpackaging=zip -Dversion=1.0-SNAPSHOT

cd ../portal-index
mvn -Dmaven.test.skip=true clean install source:jar eclipse:eclipse
mvn install:install-file -Dfile=target/portal-index-1.0-SNAPSHOT-sources.jar -DartifactId=portal-index -DgroupId=portal -Dpackaging=zip -Dversion=1.0-SNAPSHOT

cd ../portal-service
mvn -Dmaven.test.skip=true clean install source:jar eclipse:eclipse
mvn install:install-file -Dfile=target/portal-service-1.0-SNAPSHOT-sources.jar -DartifactId=portal-service -DgroupId=portal -Dpackaging=zip -Dversion=1.0-SNAPSHOT

cd ../portal-webservices/binding
mvn -Dmaven.test.skip=true clean install eclipse:eclipse

cd ..
mvn -Dmaven.test.skip=true clean install source:jar eclipse:eclipse
mvn install:install-file -Dfile=target/portal-webservices-1.0-SNAPSHOT-sources.jar -DartifactId=portal-webservices -DgroupId=portal -Dpackaging=zip -Dversion=1.0-SNAPSHOT

cd ../portal-web
mvn clean install jar:jar source:jar eclipse:eclipse
mvn install:install-file -Dfile=target/portal.jar -DgroupId=portal -DartifactId=portal-web-jar -Dversion=1.0-SNAPSHOT -Dpackaging=jar 

cd ../ala-portal-web
mvn clean install eclipse:eclipse
