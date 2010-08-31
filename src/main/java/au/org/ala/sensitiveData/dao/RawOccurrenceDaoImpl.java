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
package au.org.ala.sensitiveData.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Component;

import au.org.ala.sensitiveData.model.RawOccurrenceRecord;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
@Component
public class RawOccurrenceDaoImpl extends JdbcDaoSupport implements RawOccurrenceDao {
	
	protected static final Logger logger = Logger.getLogger(RawOccurrenceDaoImpl.class);
	
	public static final String SELECT_ALL_OCCURRENCES = "SELECT id, scientific_name, latitude, longitude, lat_long_precision FROM raw_occurrence_record";
	
	public static final String UPDATE_OCCURRENCE = "UPDATE raw_occurrence_record " + "" +
												   "SET latitude=?, longitude=?, generalised_metres=?, raw_latitude=?, raw_longitude=? " +
												   "WHERE id=?";

	public static final String REUPDATE_OCCURRENCE = "UPDATE raw_occurrence_record " + "" +
	   												 "SET latitude=?, longitude=?, generalised_metres=? " +
	   												 "WHERE id=?";

	@Inject
	public RawOccurrenceDaoImpl(DataSource dataSource) {
		this.setDataSource(dataSource);
		logger.debug("Occurrence data source set - " + dataSource.toString());
	}
	
	@Override
	public void updateLocation(int id, String rawLat, String rawLong, int generalisedMetres, String generalisedLat, String generalisedLong) {
		int rows = getJdbcTemplate().update(UPDATE_OCCURRENCE, new Object [] { generalisedLat, generalisedLong, Integer.toString(generalisedMetres), rawLat, rawLong, new Integer(id) });
		if (rows == 0) {
			logger.warn("No rows updated for id=" + id);
		}
	}

	@Override
	public void updateLocation(int id, String generalisedLat, String generalisedLong, int generalisedMetres) {
		int rows = getJdbcTemplate().update(REUPDATE_OCCURRENCE, new Object [] { generalisedLat, generalisedLong, Integer.toString(generalisedMetres), new Integer(id) });
		if (rows == 0) {
			logger.warn("No rows updated for id=" + id);
		}
	}

	@Override
	public List<RawOccurrenceRecord> getOccurrences() {
		return (List<RawOccurrenceRecord>) getJdbcTemplate().query(
				SELECT_ALL_OCCURRENCES,
				new Object [] {},
				new RowMapper<RawOccurrenceRecord>() {
					public RawOccurrenceRecord mapRow(ResultSet rs, int row) throws SQLException {
						RawOccurrenceRecord occ = new RawOccurrenceRecord();
						occ.setId(rs.getInt("id"));
						occ.setScientificName(rs.getString("scientific_name"));
						occ.setLatitude(rs.getString("latitude"));
						occ.setLongitude(rs.getString("longitude"));
						occ.setLatLongPrecision(rs.getString("lat_long_precision"));
						return occ;
					}
				});
	}

	
}
