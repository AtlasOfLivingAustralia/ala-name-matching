start_time=$(date +%s)

jar xf sensitive-species-assembly.jar lib lib

export CLASSPATH=sensitive-species.jar

echo "Generalising Occurrence Location data for Sensitive Species $('date')"
java -Xmx2g -Xms2g -Dlog4j.configuration=log4j.properties -cp $CLASSPATH au.org.ala.sensitiveData.GeneraliseOccurrenceLocations

finish_time=$(date +%s)

echo "Time taken: $(( $((finish_time - start_time)) /3600 )) hours $(( $((finish_time - start_time)) /60 )) minutes."
