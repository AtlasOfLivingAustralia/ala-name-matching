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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import au.org.ala.sensitiveData.dto.SensitiveSpeciesDto;
import au.org.ala.sensitiveData.model.SensitiveSpecies;
import au.org.ala.sensitiveData.model.ConservationCategory;
import au.org.ala.sensitiveData.model.SensitivityInstance;
import au.org.ala.sensitiveData.model.SensitivityZone;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitiveSpeciesDaoImpl extends JdbcDaoSupport implements SensitiveSpeciesDao {

	protected static final Logger logger = Logger.getLogger(SensitiveSpeciesDaoImpl.class);
	
	private List<SensitiveSpecies> species;

	public static final String SELECT_ALL = "SELECT * FROM sensitive_species_location ORDER BY scientific_name, sensitivity_zone";

	@Inject
	public SensitiveSpeciesDaoImpl(DataSource dataSource) {
		this.setDataSource(dataSource);
		logger.debug("Sensitive Species data source set - " + dataSource.toString());
	}
	
	protected void initDao() throws Exception {
		super.initDao();
		this.species = getAll();
	}

	public List<SensitiveSpecies> getAll() {

	    List<SensitiveSpeciesDto> dtoList = getJdbcTemplate().query(
				SELECT_ALL,
				new RowMapper<SensitiveSpeciesDto>() {
					public SensitiveSpeciesDto mapRow(ResultSet rs, int row) throws SQLException {
					    SensitiveSpeciesDto dto = new SensitiveSpeciesDto();
					    dto.setScientificName(rs.getString("scientific_name"));
					    dto.setSensitivityZone(rs.getString("sensitivity_zone"));
					    dto.setDataProvider(rs.getString("data_provider"));
					    dto.setSensitivityCategory(rs.getString("sensitivity_category"));
						return dto;
					}
				});
	    
	    List<SensitiveSpecies> speciesList = new ArrayList<SensitiveSpecies>();
	    SensitiveSpecies ss = null;;
	    String currentName = "";

	    for (SensitiveSpeciesDto dto : dtoList) {
	        if (!dto.getScientificName().equals(currentName)) {
	            ss = new SensitiveSpecies(dto.getScientificName());
	            speciesList.add(ss);
	        }
	        ss.getInstances().add(new SensitivityInstance(
	                ConservationCategory.getCategory(dto.getSensitivityCategory()), 
	                dto.getDataProvider(), 
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
}
