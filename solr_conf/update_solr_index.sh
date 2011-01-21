# update via CSV file
# delete all first
echo "deleting existing index..."
curl 'http://localhost:8080/solr/update?commit=true -H "Content-Type: text/xml" --data-binary "<delete><query>id:[* TO *]</query></delete>"date'
echo "indexing from CSV file..."
date
curl  'http://localhost:8080/solr/update/csv?commit=true&f.state.split=true&f.state.separator=|&f.biogeographic_region.split=true&f.biogeographic_region.separator=|&f.places.split=true&f.places.separator=|&map=\N:&trim=true&overwrite=false&stream.file=/data/bie-staging/biocache/occurrences.csv'
date
# commit
echo "optimising indexes..."
curl http://localhost:8080/solr/update?stream.body=%3Coptimize/%3E
date
echo "finished"
