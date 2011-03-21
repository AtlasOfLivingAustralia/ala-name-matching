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
import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import au.org.ala.sds.dto.SensitiveSpeciesDto;
import au.org.ala.sds.model.ConservationInstance;
import au.org.ala.sds.model.PlantPestInstance;
import au.org.ala.sds.model.SensitiveSpecies;
import au.org.ala.sds.model.SensitivityCategory;
import au.org.ala.sds.model.SensitivityZone;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */

public class SensitiveSpeciesMySqlDao extends JdbcDaoSupport implements SensitiveSpeciesDao {

    public static final String SELECT_ALL = "SELECT * FROM sensitive_species_zones ORDER BY scientific_name, sensitivity_zone";

    public SensitiveSpeciesMySqlDao(DataSource dataSource) throws Exception {
        this.setDataSource(dataSource);
    }

    @Override
    public void initDao() throws Exception {
        super.initDao();
    }

    public List<SensitiveSpecies> getAll() {

        List<SensitiveSpeciesDto> dtoList = getJdbcTemplate().query(
                SELECT_ALL,
                new RowMapper<SensitiveSpeciesDto>() {
                    public SensitiveSpeciesDto mapRow(ResultSet rs, int row) throws SQLException {
                        SensitiveSpeciesDto dto = new SensitiveSpeciesDto();
                        dto.setScientificName(rs.getString("scientific_name"));
                        dto.setSensitivityZone(rs.getString("sensitivity_zone"));
                        dto.setAuthority(rs.getString("authority_name"));
                        dto.setFromDate(rs.getString("from_date"));
                        dto.setToDate(rs.getString("to_date"));
                        dto.setSensitivityCategory(rs.getString("sensitivity_category"));
                        dto.setLocationGeneralisation(rs.getString("location_generalisation"));
                        return dto;
                    }
                });

        List<SensitiveSpecies> speciesList = new ArrayList<SensitiveSpecies>();
        SensitiveSpecies ss = null;
        String currentName = "";

        for (SensitiveSpeciesDto dto : dtoList) {
            if (!dto.getScientificName().equals(currentName)) {
                ss = new SensitiveSpecies(dto.getScientificName());
                speciesList.add(ss);
                currentName = dto.getScientificName();
            }

            SensitivityCategory category = SensitivityCategory.getCategory(dto.getSensitivityCategory());
            if (SensitivityCategory.isConservationSensitive(category)) {
                ss.getInstances().add(new ConservationInstance(
                        category,
                        dto.getAuthority(),
                        SensitivityZone.valueOf(dto.getSensitivityZone()),
                        dto.getLocationGeneralisation()));
            } else if (SensitivityCategory.isPlantPest(category)) {
                ss.getInstances().add(new PlantPestInstance(
                        category,
                        dto.getAuthority(),
                        SensitivityZone.valueOf(dto.getSensitivityZone()),
                        dto.getFromDate(),
                        dto.getToDate()));
            }
       }

        return speciesList;
    }

}
