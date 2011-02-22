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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import au.org.ala.checklist.lucene.CBIndexSearch;
import au.org.ala.checklist.lucene.SearchResultException;
import au.org.ala.checklist.lucene.model.NameSearchResult;
import au.org.ala.data.util.RankType;
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
        verifyAndInitialiseSpeciesList();
    }

    private void verifyAndInitialiseSpeciesList() throws SearchResultException {
        this.lsidMap = new HashMap<String, Integer>();
        for (int index = 0; index < species.size(); index++) {
            SensitiveSpecies ss = species.get(index);
            NameSearchResult match = cbIdxSearcher.searchForRecord(ss.getScientificName(), RankType.SPECIES);
            if (match != null) {
                String acceptedName = match.getRankClassification().getSpecies();
                String lsid = match.getLsid();
                if (!ss.getScientificName().equalsIgnoreCase(acceptedName)) {
                    logger.warn("Sensitive species '" + ss.getScientificName() + "' is not accepted name - '" + acceptedName + "'");
                } else {
                    logger.debug("'" + acceptedName + "'\t'" + lsid + "'");
                    ss.setLsid(lsid);
                    lsidMap.put(lsid, index);
                }
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
                        dto.setAuthority(rs.getString("authority"));
                        dto.setFromDate(rs.getString("from_date"));
                        dto.setToDate(rs.getString("to_date"));
                        dto.setSensitivityCategory(rs.getString("sensitivity_category"));
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
                    SensitivityZone.valueOf(dto.getSensitivityZone())));
        }

        return speciesList;
    }

    public SensitiveSpecies findByName(String scientificName) {
        try {
            SensitiveSpecies ss = new SensitiveSpecies(scientificName);
            int match = Collections.binarySearch(species, ss);
            if (species.get(match).equals(ss)) {
                logger.debug("Sensitive Species exact match - " + scientificName);
                return species.get(match);
            } else {
                String sensitiveSpeciesName = species.get(match).getScientificName();
                if (ss.getScientificName().startsWith(sensitiveSpeciesName) && sensitiveSpeciesName.indexOf(" ") == -1) {
                    logger.debug("Sensitive Species genus match - " + scientificName);
                    return species.get(match);
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
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
}
