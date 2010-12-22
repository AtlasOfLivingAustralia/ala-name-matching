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
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import au.org.ala.sensitiveData.model.SensitiveSpecies;
import au.org.ala.sensitiveData.model.SensitivityCategory;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitiveSpeciesDaoImpl extends JdbcDaoSupport implements SensitiveSpeciesDao {

	protected static final Logger logger = Logger.getLogger(SensitiveSpeciesDaoImpl.class);
	
	private List<SensitiveSpecies> species;

	public static final String SELECT_ALL = "SELECT * FROM sensitive_species ORDER BY scientific_name";

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
		return (List<SensitiveSpecies>) getJdbcTemplate().query(
				SELECT_ALL,
				new RowMapper<SensitiveSpecies>() {
					public SensitiveSpecies mapRow(ResultSet rs, int row) throws SQLException {
						return new SensitiveSpecies(rs.getString("scientific_name"), SensitivityCategory.getCategory(rs.getString("sensitivity_category")));
					}
				});
	}

	public SensitiveSpecies findByName(String scientificName) {
		try {
			SensitiveSpecies ss = new SensitiveSpecies(scientificName, null);
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
