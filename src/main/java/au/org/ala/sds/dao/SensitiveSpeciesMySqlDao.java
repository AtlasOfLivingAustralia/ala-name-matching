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
package au.org.ala.sds.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import au.org.ala.sds.dto.SensitiveSpeciesDto;
import au.org.ala.sds.model.ConservationInstance;
import au.org.ala.sds.model.PlantPestInstance;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.model.SensitiveTaxon.Rank;
import au.org.ala.sds.model.SensitivityCategory;
import au.org.ala.sds.model.SensitivityCategoryFactory;
import au.org.ala.sds.model.SensitivityZoneFactory;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */

public class SensitiveSpeciesMySqlDao extends JdbcDaoSupport implements SensitiveSpeciesDao {

   // public static final String SELECT_ALL = "SELECT * FROM sensitive_species_zones ORDER BY scientific_name, sensitivity_zone";

    public static final String SELECT_ALL =
        "SELECT z.scientific_name, common_name, family, sensitivity_category, sensitivity_zone, reason, remarks, authority_name, from_date, to_date, location_generalisation " +
        "FROM sensitive_species_zones z, sensitive_species s " +
        "WHERE z.scientific_name = s.scientific_name " +
        "ORDER BY z.scientific_name";

    public SensitiveSpeciesMySqlDao(DataSource dataSource) throws Exception {
        this.setDataSource(dataSource);
    }

    @Override
    public void initDao() throws Exception {
        super.initDao();
    }

    public List<SensitiveTaxon> getAll() {

        List<SensitiveSpeciesDto> dtoList = getJdbcTemplate().query(
                SELECT_ALL,
                new RowMapper<SensitiveSpeciesDto>() {
                    public SensitiveSpeciesDto mapRow(ResultSet rs, int row) throws SQLException {
                        SensitiveSpeciesDto dto = new SensitiveSpeciesDto();
                        dto.setScientificName(rs.getString("scientific_name"));
                        dto.setCommonName(rs.getString("common_name"));
                        dto.setFamily(rs.getString("family"));
                        dto.setSensitivityZone(rs.getString("sensitivity_zone"));
                        dto.setAuthority(rs.getString("authority_name"));
                        dto.setReason(rs.getString("reason"));
                        dto.setRemarks(rs.getString("remarks"));
                        dto.setFromDate(rs.getString("from_date"));
                        dto.setToDate(rs.getString("to_date"));
                        dto.setSensitivityCategory(rs.getString("sensitivity_category"));
                        dto.setLocationGeneralisation(rs.getString("location_generalisation"));
                        return dto;
                    }
                });

        List<SensitiveTaxon> speciesList = new ArrayList<SensitiveTaxon>();
        SensitiveTaxon ss = null;
        String currentName = "";

        for (SensitiveSpeciesDto dto : dtoList) {
            if (!dto.getScientificName().equals(currentName)) {
                if (StringUtils.contains(dto.getScientificName(), ' ')) {
                    ss = new SensitiveTaxon(dto.getScientificName(), Rank.SPECIES);
                } else {
                    ss = new SensitiveTaxon(dto.getScientificName(), Rank.GENUS);
                }
                if (StringUtils.isNotBlank(dto.getFamily())) {
                    ss.setFamily(dto.getFamily());
                }
                if (StringUtils.isNotBlank(dto.getCommonName())) {
                    ss.setCommonName(dto.getCommonName());
                }
                speciesList.add(ss);
                currentName = dto.getScientificName();
            }

            SensitivityCategory category = SensitivityCategoryFactory.getCategory(dto.getSensitivityCategory());
            if (category.isConservationSensitive()) {
                ss.getInstances().add(new ConservationInstance(
                        category,
                        dto.getAuthority(),
                        SensitivityZoneFactory.getZone(dto.getSensitivityZone()),
                        dto.getReason(),
                        dto.getRemarks(),
                        dto.getLocationGeneralisation()));
            } else if (category.isPlantPest()) {
                ss.getInstances().add(new PlantPestInstance(
                        category,
                        dto.getAuthority(),
                        SensitivityZoneFactory.getZone(dto.getSensitivityZone()),
                        dto.getReason(),
                        dto.getRemarks(),
                        dto.getFromDate(),
                        dto.getToDate()));
            }
        }

        // Sort list since MySQL sort order is not the same as Java's
        Collections.sort(speciesList);

        return speciesList;
    }

}
