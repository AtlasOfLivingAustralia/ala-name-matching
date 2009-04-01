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
import org.ala.dao.LocalityDAO;
import org.ala.model.GeoRegion;
import org.ala.model.Locality;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * JDBC Implementation
 *
 * @author Dave Martin (David.Martin@csiro.au)
 */
public class LocalityDAOImpl extends JdbcDaoSupport implements LocalityDAO {

	LocalityRowMapper localityRowMapper = new LocalityRowMapper();

	/**
	 * @see org.ala.dao.LocalityDAO#getLocalitiesFor(java.lang.Long)
	 */
	public List<Locality> getLocalitiesFor(Long localityId) {
        List<Locality> localities = (List) getJdbcTemplate().query(
				"SELECT id, name, state, postcode, geo_region_id, point_id, " +
                "min_longitude, min_latitude, max_longitude, max_latitude " +
                "FROM locality WHERE id=?",
                new Object[] {localityId},
                new RowMapperResultSetExtractor(localityRowMapper));
        return localities;
	}

    /**
	 * @see org.ala.dao.LocalityDAO#getLocalityFor(java.lang.Long)
	 */
    public Locality getLocalityFor(Long localityId) {
        Locality locality = (Locality) getJdbcTemplate().query(
				"SELECT id, name, state, postcode, geo_region_id, point_id, " +
                "min_longitude, min_latitude, max_longitude, max_latitude " +
                "FROM locality WHERE id=?",
                new Object[] {localityId},
                new ResultSetExtractor(){
					public Object extractData(ResultSet rs) throws SQLException,
							DataAccessException {
						Locality lo = new Locality();
                        while(rs.next()){
                            lo.setId(rs.getInt("id"));
                            lo.setName(rs.getString("name"));
                            lo.setState(rs.getString("state"));
                            lo.setPostcode(rs.getString("postcode"));
                            GeoRegion gr =  new GeoRegion();
                            gr.setId(rs.getInt("geo_region_id"));
                            lo.setGeoRegion(gr);
                            lo.setPointId(rs.getInt("point_id"));
                            lo.setMinLongitude(rs.getInt("min_longitude"));
                            lo.setMinLatitude(rs.getInt("min_latitude"));
                            lo.setMaxLongitude(rs.getInt("max_longitude"));
                            lo.setMaxLatitude(rs.getInt("max_latitude"));
                            //return lo;
						}
						return lo;
					}
				});
        return locality;
	}

    /**
	 * Utility to create a LinkRecord for a row
	 */
	protected class LocalityRowMapper implements RowMapper {

        public Locality mapRow(ResultSet rs, int rowNumber) throws SQLException {
        	Locality lo = new Locality();
            lo.setId(rs.getInt("id"));
            lo.setName(rs.getString("name"));
            lo.setState(rs.getString("state"));
            lo.setPostcode(rs.getString("postcode"));
            GeoRegion gr =  new GeoRegion();
            gr.setId(rs.getInt("geo_region_id"));
            lo.setGeoRegion(gr);
            lo.setPointId(rs.getInt("point_id"));
            lo.setMinLongitude(rs.getInt("min_longitude"));
            lo.setMinLatitude(rs.getInt("min_latitude"));
            lo.setMaxLongitude(rs.getInt("max_longitude"));
            lo.setMaxLatitude(rs.getInt("max_latitude"));
            return lo;
		}
	}
}
