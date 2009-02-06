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

import org.ala.dao.InstitutionDAO;
import org.ala.model.Institution;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * DAO for selecting details of a Institution.
 * 
 * Note this DAO is using the "institution_code" table.
 *
 * @author "Dave Martin (David.Martin@csiro.au)"
 */
public class InstitutionDAOImpl extends JdbcDaoSupport implements InstitutionDAO {

	protected static final String SELECT_BY_CODE = "select id, code, name, lsid from institution_code where code=?"; 
	
	protected InstitutionRowMapper institutionRowMapper = new InstitutionRowMapper();
	/**
	 * @see org.ala.dao.InstitutionDAO#getInstitutionForCode(java.lang.String)
	 */
	public Institution getInstitutionForCode(String code) {
		List<Institution> results = (List<Institution>) getJdbcTemplate()
		.query(SELECT_BY_CODE,
			new Object[]{code},
			new RowMapperResultSetExtractor(institutionRowMapper, 1));
		return results.get(0);
	}
	
	/**
	 * Utility to create a LinkRecord for a row 
	 */
	protected class InstitutionRowMapper implements RowMapper {
		public Institution mapRow(ResultSet rs, int rowNumber) throws SQLException {
			Institution i = new Institution();
			i.setId(rs.getLong("id"));
			i.setCode(rs.getString("code"));
			i.setLsid(rs.getString("lsid"));
			i.setName(rs.getString("name"));
			return i;
		}
	}	
}