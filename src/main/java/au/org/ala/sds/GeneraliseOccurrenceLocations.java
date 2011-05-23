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

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.checklist.lucene.SearchResultException;
import au.org.ala.checklist.lucene.model.NameSearchResult;
import au.org.ala.data.util.RankType;
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

    protected RawOccurrenceDao rawOccurrenceDao;
    protected DataSource occurrenceDataSource;
    protected CBIndexSearch cbIdxSearcher;
    protected SensitiveSpeciesFinder sensitiveSpeciesFinder;

    public static void main(String[] args) throws Exception {
        ApplicationContext context = new ClassPathXmlApplicationContext("spring-config.xml");
        GeneraliseOccurrenceLocations app = context.getBean(GeneraliseOccurrenceLocations.class);
        app.run(args.length == 1 ? args[0] : null);
    }

    private void run(String startAt) throws SQLException, SearchResultException {
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

                if (isEmpty(rawScientificName)) continue;
                if (isEmpty(latitude) || isEmpty(longitude)) continue;

                // Standardise name
                String speciesName = getSpeciesName(rawScientificName);

                // See if it's sensitive
                SensitiveTaxon ss = sensitiveSpeciesFinder.findSensitiveSpecies(speciesName);
                if (ss != null) {
                    FactCollection facts = new FactCollection();
                    facts.add(FactCollection.DECIMAL_LATITUDE_KEY, latitude);
                    facts.add(FactCollection.DECIMAL_LONGITUDE_KEY, longitude);

                    ValidationService service = ServiceFactory.createValidationService(ss);
                    ValidationOutcome outcome = service.validate(facts);

                    GeneralisedLocation genLoc = ((ConservationOutcome) outcome).getGeneralisedLocation();
                    if (isEmpty(generalised_metres)) {
                        logger.debug("Generalising location for " + id + " '" + rawScientificName + "' using Name='" + speciesName +
                                     "', Lat=" + genLoc.getGeneralisedLatitude() +
                                     ", Long=" + genLoc.getGeneralisedLongitude());
                        rawOccurrenceDao.updateLocation(id, genLoc.getGeneralisedLatitude(), genLoc.getGeneralisedLongitude(), genLoc.getGeneralisationInMetres(), latitude, longitude);
                    } else {
                        if (generalised_metres != genLoc.getGeneralisationInMetres()) {
                            logger.debug("Re-generalising location for " + id + " '" + rawScientificName + "' using Name='" + speciesName +
                                         "', Lat=" + genLoc.getGeneralisedLatitude() +
                                         ", Long=" + genLoc.getGeneralisedLongitude());
                            rawOccurrenceDao.updateLocation(id, genLoc.getGeneralisedLatitude(), genLoc.getGeneralisedLongitude(), genLoc.getGeneralisationInMetres());
                        }
                    }
                } else {
                    // See if was sensitive but not now
                    if (!isEmpty(generalised_metres)) {
                        logger.debug("De-generalising location for " + id + " '" + rawScientificName + "' using Name='" + speciesName + "', Lat=" + raw_latitude + ", Long=" + raw_longitude);
                        rawOccurrenceDao.updateLocation(id, raw_latitude, raw_longitude, null, null, null);
                    }
                }
            }
            rs.close();
            logger.debug("Processed " + recCount + " occurrence records.");
        }

        rs.close();
        pst.close();
        conn.close();
    }

    private String getSpeciesName(String name) throws SearchResultException {
        NameSearchResult match = cbIdxSearcher.searchForRecord(name, RankType.SPECIES);
        if (match != null) {
            return match.getRankClassification().getSpecies();
        } else {
            return name;
        }
    }

    public void setRawOccurrenceDao(RawOccurrenceDao rawOccurrenceDao) {
        this.rawOccurrenceDao = rawOccurrenceDao;
    }

    public void setOccurrenceDataSource(DataSource occurrenceDataSource) {
        this.occurrenceDataSource = occurrenceDataSource;
    }

    public boolean isEmpty(String str) {
        return (str == null || str.equals(""));
    }

}
