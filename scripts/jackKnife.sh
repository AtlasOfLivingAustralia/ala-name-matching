CP="-Dactors.corePoolSize=2 -Dactors.maxPoolSize=4 -Dactors.minPoolSize=2 -cp .:biocache.jar"
EXPORTS="/data2/jackKnife/exports"
SOLR=/data/solr/bio-proto/data/index

echo "Exporting list of species & subspecies records...."
java -cp $CP au.org.ala.util.ExportForOutliers $SOLR $EXPORTS
sort -T $EXPORTS $EXPORTS/species-unsorted.txt > $EXPORTS/species-sorted.txt
sort -T $EXPORTS $EXPORTS/subspecies-unsorted.txt > $EXPORTS/subspecies-sorted.txt

echo "Start species outlier tests"
java -Xmx4g -Xms4g $CP au.au.biocache.outliers.SpeciesOutlierTests -fd $EXPORTS/species-sorted.txt -hfd "taxonConceptID uuid decimalLatitude decimalLongitude el882 el889 el887 el865 el894" -if $EXPORTS/species-idsToReindex.txt

echo "Start subspecies outlier tests"
java -Xmx4g -Xms4g $CP au.au.biocache.outliers.SpeciesOutlierTests -fd $EXPORTS/subspecies-sorted.txt -hfd "taxonConceptID uuid decimalLatitude decimalLongitude el882 el889 el887 el865 el894" -if $EXPORTS/subspecies-idsToReindex.txt

echo "Finished"
