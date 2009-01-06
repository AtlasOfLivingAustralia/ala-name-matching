cd ../portal-core
mvn -Dmaven.test.skip=true clean install source:jar
mvn install:install-file -Dfile=target/portal-core-1.0-SNAPSHOT-sources.jar -DartifactId=portal-core -DgroupId=portal -Dpackaging=zip -Dversion=1.0-SNAPSHOT

cd ../portal-index
mvn -Dmaven.test.skip=true clean install source:jar
mvn install:install-file -Dfile=target/portal-index-1.0-SNAPSHOT-sources.jar -DartifactId=portal-index -DgroupId=portal -Dpackaging=zip -Dversion=1.0-SNAPSHOT

cd ../portal-service
mvn -Dmaven.test.skip=true clean install source:jar
mvn install:install-file -Dfile=target/portal-service-1.0-SNAPSHOT-sources.jar -DartifactId=portal-service -DgroupId=portal -Dpackaging=zip -Dversion=1.0-SNAPSHOT

cd ../portal-webservices/binding
mvn -Dmaven.test.skip=true clean install

cd ..
mvn -Dmaven.test.skip=true clean install source:jar
mvn install:install-file -Dfile=target/portal-webservices-1.0-SNAPSHOT-sources.jar -DartifactId=portal-webservices -DgroupId=portal -Dpackaging=zip -Dversion=1.0-SNAPSHOT

cd ../portal-web
mvn clean install

cd ../ala-portal-web
mvn clean install
