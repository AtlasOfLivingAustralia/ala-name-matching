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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.checklist.lucene.model.NameSearchResult;
import au.org.ala.sds.dto.SensitiveSpeciesDto;
import au.org.ala.sds.model.SensitiveSpecies;
import au.org.ala.sds.model.SensitivityCategory;
import au.org.ala.sds.model.SensitivityInstance;
import au.org.ala.sds.model.SensitivityZone;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitiveSpeciesDaoImpl extends JdbcDaoSupport implements SensitiveSpeciesDao {

    protected static final Logger logger = Logger.getLogger(SensitiveSpeciesDaoImpl.class);

    private List<SensitiveSpecies> species;

    private Map<String, Integer> lsidMap;
    private Map<String, Integer> nameMap;


    @Inject
    protected CBIndexSearch cbIdxSearcher;

    public static final String SELECT_ALL = "SELECT * FROM sensitive_species_zones ORDER BY scientific_name, sensitivity_zone";

    @Inject
    public SensitiveSpeciesDaoImpl(DataSource dataSource, CBIndexSearch cbIdxSearcher) throws Exception {
        this.setDataSource(dataSource);
        logger.debug("Sensitive Species data source set - " + dataSource.toString());
        this.cbIdxSearcher = cbIdxSearcher;
        this.initDao();
    }

    @Override
    protected void initDao() throws Exception {
        super.initDao();
        this.species = getAll();
        this.nameMap = new HashMap<String, Integer>();
        this.lsidMap = new HashMap<String, Integer>();
        verifyAndInitialiseSpeciesList();
    }

    private void verifyAndInitialiseSpeciesList() {
        for (int index = 0; index < species.size(); index++) {
            SensitiveSpecies ss = species.get(index);
            NameSearchResult match = getAcceptedName(ss.getScientificName());
            if (match != null) {
                String acceptedName = match.getRankClassification().getScientificName();
                String lsid = match.getLsid();
                if (!ss.getScientificName().equalsIgnoreCase(acceptedName)) {
                    logger.warn("Sensitive species '" + ss.getScientificName() + "' is not accepted name - '" + acceptedName + "'");
                    ss.setAcceptedName(acceptedName);
                }
                logger.debug("'" + ss.getScientificName() + "' ('" + acceptedName + "')\t'" + lsid + "'");
                nameMap.put(acceptedName, index);
                ss.setLsid(lsid);
                lsidMap.put(lsid, index);
            } else {
                logger.warn("Sensitive species '" + ss.getScientificName() + "' not found in NameMatching index");
            }
        }
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
            ss.getInstances().add(new SensitivityInstance(
                    SensitivityCategory.getCategory(dto.getSensitivityCategory()),
                    dto.getAuthority(),
                    dto.getFromDate(),
                    dto.getToDate(),
                    SensitivityZone.valueOf(dto.getSensitivityZone()),
                    dto.getLocationGeneralisation()));
        }

        return speciesList;
    }

    public SensitiveSpecies findByName(String scientificName) {
        String acceptedName = scientificName;
        NameSearchResult result = getAcceptedName(scientificName);
        if (result != null) {
            acceptedName = result.getRankClassification().getScientificName();
        }

        Integer index = nameMap.get(acceptedName);
        if (index != null) {
            return species.get(index);
        } else {
            return null;
        }
    }

    public SensitiveSpecies findByLsid(String lsid) {
        Integer index = lsidMap.get(lsid);
        if (index != null) {
            return species.get(index);
        } else {
            return null;
        }
    }

    private NameSearchResult getAcceptedName(String name) {
        NameSearchResult match = null;
        try {
            match = cbIdxSearcher.searchForRecord(name, null);
            if (match != null) {
                if (match.isSynonym()) {
                    match = cbIdxSearcher.searchForRecordByID(Long.toString(match.getAcceptedId()));
                }
            }
        } catch (Exception e) {
            logger.warn("'" + name + "' - " + e.getMessage());
        }

        return match;
    }
}
