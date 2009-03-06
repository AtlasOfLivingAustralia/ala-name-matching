/* *************************************************************************
 *  Copyright (C) 2009 Atlas of Living Australia
 *  All Rights Reserved.
 *  
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *  
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/

package org.ala.dao.jdbc;

import java.sql.ResultSet;
import java.util.List;
import java.sql.SQLException;
import org.ala.dao.GeoRegionTaxonConceptDAO;
import org.ala.model.GeoRegionTaxonConcept;
import org.gbif.portal.dto.taxonomy.BriefTaxonConceptDTO;
import org.gbif.portal.model.occurrence.BasisOfRecord;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * JDBC implementation of DAO interface providing access to
 *   TaxonConcept DTO objects via Geo Region view
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class GeoRegionTaxonConceptDAOImpl extends JdbcDaoSupport implements GeoRegionTaxonConceptDAO {

    protected GrtcRowMapper grtcRowMapper = new GrtcRowMapper();
    
    protected Integer initialRank =  4000;

    public List<GeoRegionTaxonConcept> getOrderTaxonConceptsForGeoRegion(Long geoRegionId) {
        List<GeoRegionTaxonConcept> btcs = (List<GeoRegionTaxonConcept>) getJdbcTemplate().query(
				"SELECT tc.id taxon_concept_id, tc.rank rank_id, tr.name rank_name, tn.canonical, " +
                "cn.name common_name, grc.occurrence_count, grc.occurrence_coordinate_count, grc.basis_of_record " +
                "FROM `geo_region_taxon` grc " +
                "INNER JOIN taxon_concept tc ON tc.id = grc.taxon_concept_id " +
                "INNER JOIN taxon_name tn ON tn.id = tc.taxon_name_id " +
                "INNER JOIN rank tr ON tc.rank = tr.id " +
                "LEFT JOIN common_name cn ON cn.taxon_concept_id = tc.id " +
                "WHERE grc.geo_region_id = ? " +
                //"AND tc.rank = ?",
                "ORDER by grc.occurrence_coordinate_count DESC",
                new Object[] { geoRegionId }, //, initialRank
                new RowMapperResultSetExtractor(grtcRowMapper));
        return btcs;
    }

    public void setInitialRank(Integer initialRank) {
        this.initialRank = initialRank;
    }
    
    /**
	 * Utility to create a LinkRecord for a row
	 */
	protected class GrtcRowMapper implements RowMapper {

        public GeoRegionTaxonConcept mapRow(ResultSet rs, int rowNumber) throws SQLException {
            GeoRegionTaxonConcept grtc = new GeoRegionTaxonConcept();
            grtc.setTaxonConceptId(rs.getLong("taxon_concept_id"));
            grtc.setRankId(rs.getInt("rank_id"));
            grtc.setRankName(rs.getString("rank_name"));
            grtc.setTaxonConceptName(rs.getString("canonical"));
            grtc.setCommonName(rs.getString("common_name"));
            grtc.setOccurrenceCount(rs.getLong("occurrence_count"));
            grtc.setOccurrenceCoordinateCount(rs.getLong("occurrence_coordinate_count"));
            grtc.setBasisOfRecord(BasisOfRecord.getBasisOfRecord(rs.getInt("basis_of_record")));
            return grtc;
		}
	}
}
