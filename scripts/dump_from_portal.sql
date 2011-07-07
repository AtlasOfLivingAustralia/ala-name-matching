-- TO DO we need to work out how we are going to correctly hanle type status and identifiers
set @@session.max_sp_recursion_depth=255;
delimiter $$
DROP PROCEDURE IF EXISTS dump_raw_records$$

CREATE PROCEDURE dump_raw_records(low INT, high INT)
    BEGIN

        DECLARE newlow INT;
        set @cmd = concat('select \'portalId\', \'dataProviderUid\', \'dataResourceUid\', \'institutionCode\', \'collectionCode\', \'catalogNumber\','
                   ,' \'scientificName\', \'scientificNameAuthorship\', \'taxonRank\', \'kingdom\', \'phylum\',\'classs\', \'order\','
                   ,' \'family\', \'genus\', \'specificEpithet\', \'infraspecificEpithet\', \'decimalLatitude\', \'decimalLongitude\',\'coordinateUncertaintyInMeters\','
                   ,' \'maximumElevationInMeters\', \'minimumElevationInMeters\',\'elevationPrecision\', \'maximumDepthInMeters\', \'minimumDepthInMeters\', \'depthPrecision\','
                   ,' \'continent\', \'country\', \'stateProvince\', \'county\', \'recordedBy\','
                   ,' \'locality\', \'year\', \'month\', \'day\', \'basisOfRecord\', \'identifierBy\','
                   ,' \'dateIdentified\',   \'occurrenceRemarks\', \'citation\','
                   ,' \'locationRemarks\', \'occurrenceID\', \'vernacularName\', \'identificationQualifier\','
,' \'individualCount\', \'geodeticDatum\', \'generalisedMeters\', \'alaUserId\','
,' \'originalDecimalLatitude\', \'originalDecimalLongitude\',  \'eventDate\', \'eventTime\','
,' \'typeStatus\''
,' UNION'
,' select ror.id, dp.uid, dr.uid, IFNULL(ror.institution_code, \'\'), IFNULL(ror.collection_code,\'\'), IFNULL(ror.catalogue_number,\'\'),'
,' IFNULL(ror.scientific_name, \'\'), IFNULL(ror.author, \'\'), IFNULL(ror.rank, \'\'), IFNULL(ror.kingdom, \'\'), IFNULL(ror.phylum, \'\'), IFNULL(ror.class, \'\'), IFNULL(ror.order_rank, \'\'),'
,' IFNULL(ror.family, \'\'), IFNULL(ror.genus, \'\'), IFNULL(ror.species, \'\'), IFNULL(ror.subspecies, \'\'), IFNULL(ror.latitude, \'\'), IFNULL(ror.longitude, \'\'), IFNULL(ror.lat_long_precision, \'\'),'
,' IFNULL(ror.max_altitude, \'\'), IFNULL(ror.min_altitude, \'\'), IFNULL(ror.altitude_precision, \'\'), IFNULL(ror.max_depth, \'\'), IFNULL(ror.min_depth, \'\'), IFNULL(ror.depth_precision, \'\'),'
,' IFNULL(ror.continent_ocean, \'\'), IFNULL(ror.country, \'\'), IFNULL(ror.state_province, \'\'), IFNULL(ror.county, \'\'), IFNULL(ror.collector_name, \'\'),'
,' IFNULL(ror.locality,\'\'), IFNULL(ror.year, \'\'), IFNULL(ror.month, \'\'), IFNULL(ror.day, \'\'), IFNULL(ror.basis_of_record, \'\'), IFNULL(ror.identifier_name, \'\'),'
,' IFNULL(ror.identification_date, \'\'), IFNULL(ror.occurrence_remarks, \'\'), IFNULL(ror.citation,\'\'),'
,' IFNULL(ror.location_remarks, \'\'), IFNULL(ror.record_number, \'\'), IFNULL(ror.vernacular_name, \'\'), IFNULL(ror.identification_qualifier, \'\'),'
,' IFNULL(ror.individual_count, \'\'), IFNULL(ror.geodetic_datum, \'\'), IFNULL(ror.generalised_metres, \'\'), IFNULL(ror.user_id, \'\'),'
,' IFNULL(ror.raw_latitude, \'\'), IFNULL(ror.raw_longitude, \'\'), IFNULL(ror.event_date, \'\'), IFNULL(ror.event_time, \'\'),'
,' IFNULL(get_type_status(ror.id), \'\')'
,' from raw_occurrence_record ror'
,' join data_resource dr on ror.data_resource_id = dr.id'
,' join data_provider dp on ror.data_provider_id = dp.id'
,' where dr.release_flag and dp.id <> 143' -- don\'t export OZCAM records
,' and ror.id >= ',low,' and ror.id < ', high
,' INTO outfile \'/data/biocache/occurrences/raw_occurrences.csv.',low,'\''
,' FIELDS TERMINATED BY \',\' OPTIONALLY ENCLOSED BY \'"\' LINES TERMINATED BY \'\n\'; -- ESCAPED BY \'"\';');

PREPARE stmt1 FROM @cmd;
EXECUTE stmt1;
Deallocate prepare stmt1;
                select concat('Finished processing ', low ,' ', now()) as debug;

                select IFNULL(min(id),0) into newlow
                from occurrence_record where id >= high;


                IF newlow>0 THEN
                        call dump_raw_records(newlow, newlow + 1000000);
                END IF;


        END;
        $$


DELIMITER ;

call dump_raw_records(1, 1000000);

-- Get the EOL Flickr records - they are in a different format to many of the other records.

select 'portalId', 'scientificName', 'recordedBy','kingdom', 'phylum', 'classs','order',
'family', 'genus', 'specificEpithet', 'infraspecificEpithet','decimalLatitude','decimalLongitude',
'coordinateUncertaintyInMeters','country', 'stateProvince','county','locality','year','month','day',
'basisOfRecord','vernacularName','photoPageUrl','associatedMedia' UNION
select ror.id, scientific_name ,author as collector, kingdom, phylum,class, order_rank,
family, genus, species,subspecies, latitude, longitude, lat_long_precision as wtf,
country, state_province, county, locality, year, month, day, basis_of_record,
vernacular_name, guid, ir.html_for_display  from raw_occurrence_record ror left join image_record ir on ror.id = ir.occurrence_id where ror.data_resource_id = 8921
into outfile '/data/biocache/dr360.csv' FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\n'