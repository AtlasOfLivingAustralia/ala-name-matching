/***************************************************************************
 * Copyright (C) 2009 Atlas of Living Australia
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
package org.ala.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.ala.dao.GeoRegionDAO;
import org.ala.model.GeoRegion;
import org.ala.model.GeoRegionType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * JDBC Implementation
 *
 * @author Dave Martin (David.Martin@csiro.au)
 */
public class GeoRegionDAOImpl extends JdbcDaoSupport implements GeoRegionDAO {

	GeoRegionRowMapper geoRegionRowMapper = new GeoRegionRowMapper();

	/**
	 * @see org.ala.dao.GeoRegionDAO#getGeoRegionFor(java.lang.Long)
	 */
	public GeoRegion getGeoRegionFor(Long geoRegionId) {
        GeoRegion geoRegion = (GeoRegion) getJdbcTemplate().query(
				"SELECT gr.id,gr.name,gr.acronym,gr.region_type,gr.iso_country_code,gr.occurrence_count,gr.occurrence_coordinate_count, " +
				"gr.min_latitude,gr.max_latitude,gr.min_longitude,gr.max_longitude, grt.id region_type, grt.name region_type_name " +
				"from geo_region gr " +
				"inner join geo_region_type grt on grt.id=gr.region_type " +
				"where id=?",
                new Object[] {geoRegionId},
                new RowMapperResultSetExtractor(geoRegionRowMapper));
        return geoRegion;
	}

	/**
	 * @see org.ala.dao.GeoRegionDAO#getGeoRegionsForGeoRegionType(java.lang.Long, java.lang.Long)
	 */
	public List<GeoRegion> getGeoRegionsForGeoRegionType(
			Long minGeoRegionTypeId, Long maxGeoRegionTypeId) {
        List<GeoRegion> geoRegions = (List) getJdbcTemplate().query(
				"SELECT gr.id,gr.name,gr.acronym,gr.region_type,gr.iso_country_code,gr.occurrence_count,gr.occurrence_coordinate_count, " +
				"gr.min_latitude,gr.max_latitude,gr.min_longitude,gr.max_longitude, grt.id region_type, grt.name region_type_name " +
				"from geo_region gr " +
				"inner join geo_region_type grt on grt.id=gr.region_type " +
				"where region_type>=? and region_type<=? order by region_type_name, gr.name",
                new Object[] {minGeoRegionTypeId, maxGeoRegionTypeId},
                new RowMapperResultSetExtractor(geoRegionRowMapper));
        return geoRegions;
	}

	/**
	 * @see org.ala.dao.GeoRegionDAO#getGeoRegionsForOccurrenceRecord(java.lang.Long)
	 */
	public List<GeoRegion> getGeoRegionsForOccurrenceRecord(
			Long occurrenceRecordId) {
        List<GeoRegion> geoRegions = (List) getJdbcTemplate().query(
				"SELECT gr.id,gr.name,gr.acronym,gr.region_type,gr.iso_country_code,gr.occurrence_count,gr.occurrence_coordinate_count, " +
				"gr.min_latitude,gr.max_latitude,gr.min_longitude,gr.max_longitude, grt.id region_type, grt.name region_type_name " +
				"from geo_region gr " +
				"inner join geo_region_type grt on grt.id=gr.region_type " +
				"inner join geo_mapping gm ON gr.id=gm.geo_region_id " +
				"where gm.occurrence_id=? order by gr.name",
                new Object[] {occurrenceRecordId},
                new RowMapperResultSetExtractor(geoRegionRowMapper));
        return geoRegions;
	}

    /**
	 * Utility to create a LinkRecord for a row
	 */
	protected class GeoRegionRowMapper implements RowMapper {

        public GeoRegion mapRow(ResultSet rs, int rowNumber) throws SQLException {
        	GeoRegion gr = new GeoRegion();
            gr.setId(rs.getLong("id"));
            gr.setName(rs.getString("name"));
            gr.setAcronym(rs.getString("acronym"));
            gr.setIsoCountryCode(rs.getString("iso_country_code"));
            gr.setMinLongitude(rs.getInt("min_longitude"));
            gr.setMinLatitude(rs.getInt("min_latitude"));
            gr.setMaxLatitude(rs.getInt("max_latitude"));
            gr.setMaxLongitude(rs.getInt("max_longitude"));
            gr.setOccurrenceCount(rs.getInt("occurrence_count"));
            gr.setOccurrenceCoordinateCount(rs.getInt("occurrence_coordinate_count"));
        	GeoRegionType grt = new GeoRegionType();
        	grt.setId(rs.getInt("region_type"));
        	grt.setName(rs.getString("region_type_name"));
            gr.setGeoRegionType(grt);
            return gr;
		}
	}
}
