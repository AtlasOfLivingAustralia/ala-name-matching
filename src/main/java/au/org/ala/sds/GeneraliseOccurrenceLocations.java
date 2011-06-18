/***************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 ***************************************************************************/
package au.org.ala.sds;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.checklist.lucene.SearchResultException;
import au.org.ala.sds.dao.RawOccurrenceDao;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.util.GeneralisedLocation;
import au.org.ala.sds.validation.ConservationOutcome;
import au.org.ala.sds.validation.FactCollection;
import au.org.ala.sds.validation.ServiceFactory;
import au.org.ala.sds.validation.ValidationOutcome;
import au.org.ala.sds.validation.ValidationService;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class GeneraliseOccurrenceLocations {

    protected static final Logger logger = Logger.getLogger(GeneraliseOccurrenceLocations.class);

    private static RawOccurrenceDao rawOccurrenceDao;
    private static BasicDataSource occurrenceDataSource;
    private static CBIndexSearch cbIdxSearcher;
    private static SensitiveSpeciesFinder sensitiveSpeciesFinder;

    public static void main(String[] args) throws Exception {
        cbIdxSearcher = new CBIndexSearch("/data/lucene/namematching");
        sensitiveSpeciesFinder = SensitiveSpeciesFinderFactory.getSensitiveSpeciesFinder("file:///data/sds/sensitive-species.xml", cbIdxSearcher);
        occurrenceDataSource = new BasicDataSource();
        occurrenceDataSource.setDriverClassName("com.mysql.jdbc.Driver");
//        occurrenceDataSource.setUrl("jdbc:mysql://ala-biocachedb2.vm.csiro.au/portal");
        occurrenceDataSource.setUrl("jdbc:mysql://localhost/portal");
        occurrenceDataSource.setUsername("root");
//        occurrenceDataSource.setPassword("sun6800");
        occurrenceDataSource.setPassword("password");
        run(args.length == 1 ? args[0] : null);
    }

    private static void run(String startAt) throws SQLException, SearchResultException {
        Connection conn = occurrenceDataSource.getConnection();
        PreparedStatement pst = conn.prepareStatement(
                "SELECT id, scientific_name, latitude, longitude, generalised_metres, raw_latitude, raw_longitude FROM raw_occurrence_record LIMIT ?,?");
        int offset = startAt == null ? 0 : Integer.parseInt(startAt);
        int stride = 10000;
        int recCount = 0;
        pst.setInt(2, stride);
        ResultSet rs;

        for (pst.setInt(1, offset); true; offset += stride, pst.setInt(1, offset)) {
            rs = pst.executeQuery();
            if (!rs.isBeforeFirst()) {
                break;
            }
            while (rs.next()) {
                recCount++;

                String rawScientificName = (rs.getString("scientific_name"));
                int id = rs.getInt("id");
                String latitude = rs.getString("latitude");
                String longitude = rs.getString("longitude");
                String generalised_metres = rs.getString("generalised_metres");
                String raw_latitude = rs.getString("raw_latitude");
                String raw_longitude = rs.getString("raw_longitude");

                if (StringUtils.isEmpty(rawScientificName)) continue;
                if (StringUtils.isEmpty(latitude) || StringUtils.isEmpty(longitude)) continue;

               // See if it's sensitive
                SensitiveTaxon ss = sensitiveSpeciesFinder.findSensitiveSpecies(rawScientificName);
                if (ss != null) {
                    FactCollection facts = new FactCollection();
                    facts.add(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
                    facts.add(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);

                    ValidationService service = ServiceFactory.createValidationService(ss);
                    ValidationOutcome outcome = service.validate(facts);

                    GeneralisedLocation genLoc = ((ConservationOutcome) outcome).getGeneralisedLocation();

                    String speciesName = ss.getTaxonName();
                    if (StringUtils.isNotEmpty(ss.getCommonName())) {
                        speciesName += " [" + ss.getCommonName() + "]";
                    }

                    if (genLoc.isGeneralised()) {
                       if (StringUtils.isEmpty(generalised_metres)) {
                            logger.info("Generalising location for " + id + " '" + rawScientificName + "' using Name='" + speciesName +
                                         "', Lat=" + genLoc.getGeneralisedLatitude() +
                                         ", Long=" + genLoc.getGeneralisedLongitude());
                            //rawOccurrenceDao.updateLocation(id, genLoc.getGeneralisedLatitude(), genLoc.getGeneralisedLongitude(), genLoc.getGeneralisationInMetres(), latitude, longitude);
                        } else {
                            if (generalised_metres != genLoc.getGeneralisationInMetres()) {
                                logger.info("Re-generalising location for " + id + " '" + rawScientificName + "' using Name='" + speciesName +
                                             "', Lat=" + genLoc.getGeneralisedLatitude() +
                                             ", Long=" + genLoc.getGeneralisedLongitude());
                                //rawOccurrenceDao.updateLocation(id, genLoc.getGeneralisedLatitude(), genLoc.getGeneralisedLongitude(), genLoc.getGeneralisationInMetres());
                            }
                        }
                    } else {
                        logger.info("Not generalising location for " + id + " '" + rawScientificName + "' using Name='" + speciesName +
                                    "', Lat=" + genLoc.getGeneralisedLatitude() +
                                    ", Long=" + genLoc.getGeneralisedLongitude() + " - " + genLoc.getDescription());
                    }
                } else {
                    // See if was sensitive but not now
                    if (StringUtils.isNotEmpty(generalised_metres)) {
                        logger.info("De-generalising location for " + id + " '" + rawScientificName + "', Lat=" + raw_latitude + ", Long=" + raw_longitude);
                        //rawOccurrenceDao.updateLocation(id, raw_latitude, raw_longitude, null, null, null);
                    }
                }
            }
            rs.close();
            logger.info("Processed " + recCount + " occurrence records.");
        }

        rs.close();
        pst.close();
        conn.close();
    }

}
