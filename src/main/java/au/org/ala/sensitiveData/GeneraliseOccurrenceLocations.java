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
package au.org.ala.sensitiveData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.checklist.lucene.SearchResultException;
import au.org.ala.checklist.lucene.model.NameSearchResult;
import au.org.ala.data.util.RankType;
import au.org.ala.sensitiveData.dao.RawOccurrenceDao;
import au.org.ala.sensitiveData.model.SensitiveSpecies;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
@Component
public class GeneraliseOccurrenceLocations {

	protected static final Logger logger = Logger.getLogger(GeneraliseOccurrenceLocations.class);

	@Inject
	protected RawOccurrenceDao rawOccurrenceDao;
	@Inject
	protected DataSource occurrenceDataSource;
	@Inject
	protected CBIndexSearch CBIdxSearcher;
	@Inject
	protected SensitiveSpeciesFinder sensitiveSpeciesFinder;
	
	public static void main(String[] args) throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext("spring-config.xml");
		GeneraliseOccurrenceLocations app = context.getBean(GeneraliseOccurrenceLocations.class);
		app.run(args.length == 1 ? args[0] : null);
	}

	private void run(String startAt) throws SQLException, SearchResultException {
		Connection conn = occurrenceDataSource.getConnection();
		PreparedStatement pst = conn.prepareStatement(
				"SELECT id, scientific_name, latitude, longitude, lat_long_precision, generalised_metres, raw_latitude, raw_longitude FROM raw_occurrence_record LIMIT ?,?");
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
				String lat_long_precision = rs.getString("lat_long_precision");
				String generalised_metres = rs.getString("generalised_metres");
				String raw_latitude = rs.getString("raw_latitude");
				String raw_longitude = rs.getString("raw_longitude");

				if (isEmpty(rawScientificName)) continue;
				if (isEmpty(latitude) || isEmpty(longitude)) continue;
				
				// Standardise name
				String speciesName = getSpeciesName(rawScientificName);

				// See if it's sensitive
				SensitiveSpecies ss = sensitiveSpeciesFinder.findSensitiveSpecies(speciesName);
				if (ss != null) {
					if (isEmpty(generalised_metres)) {
						String newLocation[] = sensitiveSpeciesFinder.generaliseLocation(ss, latitude, longitude);
						logger.debug("Generalising location for " + id + " '" + rawScientificName + "' using Name='" + speciesName + "', Lat=" + newLocation[0] + ", Long=" + newLocation[1]);
						rawOccurrenceDao.updateLocation(id, latitude, longitude, ss.getSensitivityCategory().getGeneralisationInMetres(), newLocation[0], newLocation[1]);
					} else {
						int existingGeneralisation = Integer.parseInt(generalised_metres);
						int ssGeneralisation = ss.getSensitivityCategory().getGeneralisationInMetres();
						if (existingGeneralisation != ssGeneralisation) {
							String newLocation[] = sensitiveSpeciesFinder.generaliseLocation(ss, latitude, longitude);
							logger.debug("Re-generalising location for " + id + " '" + rawScientificName + "' using Name='" + speciesName + "', Lat=" + newLocation[0] + ", Long=" + newLocation[1]);
							rawOccurrenceDao.updateLocation(id, newLocation[0], newLocation[1], ss.getSensitivityCategory().getGeneralisationInMetres());
						}
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
		List<NameSearchResult> matches = CBIdxSearcher.searchForRecords(name, RankType.SPECIES);
		if (matches != null) {
			return matches.get(0).getRankClassification().getSpecies();
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
