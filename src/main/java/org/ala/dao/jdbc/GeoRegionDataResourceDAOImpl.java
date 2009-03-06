/* **************************************************************************
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

import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.ala.dao.GeoRegionDataResourceDAO;
import org.ala.model.DataResource;
import org.ala.model.GeoRegion;
import org.ala.model.GeoRegionDataResource;
import org.gbif.portal.model.occurrence.BasisOfRecord;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * DAO for selecting details of a Geo region data resource object.
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class GeoRegionDataResourceDAOImpl extends JdbcDaoSupport implements GeoRegionDataResourceDAO {

    protected GrdrRowMapper grdrRowMapper = new GrdrRowMapper();

    public List<GeoRegionDataResource> getDataResourcesForGeoRegion(Long geoRegionId) {
        List<GeoRegionDataResource> grdrs = (List<GeoRegionDataResource>) getJdbcTemplate().query(
				"SELECT grr.geo_region_id, gr.name geo_region_name, grr.data_resource_id, " +
                "dr.name data_resource_name, grr.occurrence_count, grr.occurrence_coordinate_count, " +
				"grr.basis_of_record FROM geo_region_resource grr " +
                "INNER JOIN data_resource dr ON dr.id=grr.data_resource_id " +
                "INNER JOIN geo_region gr ON gr.id=grr.geo_region_id " +
                "WHERE geo_region_id = ? ORDER by data_resource_name",
                new Object[] { geoRegionId },
                new RowMapperResultSetExtractor(grdrRowMapper));
        return grdrs;
    }

    /**
	 * Utility to create a LinkRecord for a row
	 */
	protected class GrdrRowMapper implements RowMapper {
		public GeoRegionDataResource mapRow(ResultSet rs, int rowNumber) throws SQLException {
            GeoRegionDataResource grdr = new GeoRegionDataResource();
            grdr.setGeoRegionId(rs.getLong("geo_region_id"));
            grdr.setGeoRegionName(rs.getString("geo_region_name"));
            grdr.setDataResourceId(rs.getLong("data_resource_id"));
            grdr.setDataResourceName(rs.getString("data_resource_name"));
            grdr.setOccurrenceCount(rs.getLong("occurrence_count"));
            grdr.setOccurrenceCoordinateCount(rs.getLong("occurrence_coordinate_count"));
            grdr.setBasisOfRecord(BasisOfRecord.getBasisOfRecord(rs.getInt("basis_of_record")));
			return grdr;
		}
	}
}
