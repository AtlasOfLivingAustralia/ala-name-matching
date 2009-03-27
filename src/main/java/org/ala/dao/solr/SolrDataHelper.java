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
package org.ala.dao.solr;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * Class that helps populate the results of SOLR queries
 * with data from the database.
 *
 * @author Dave Martin (David.Martin@csiro.au)
 */
public class SolrDataHelper extends JdbcDaoSupport {

	public static final String CONCEPT_QUERY = "select tc.id, tn.canonical scientific_name, cn.name common_name " +
			"from taxon_concept tc " +
			"inner join taxon_name tn on tn.id=tc.taxon_name_id " +
			"left outer join " + 
			"common_name cn on tc.id=cn.taxon_concept_id " +
			"WHERE tc.id IN (:idList) group by tc.id order by tc.id";

	/**
	 * Returns a list of scientific names an common names for the supplied concept ids.
	 * 
	 * @param conceptIds
	 * @return
	 */
	public List<Map<String, Object>> getScientificNamesForConceptsIds(List<Long> conceptIds){
		if(conceptIds==null | conceptIds.isEmpty())
			throw new IllegalArgumentException("Empty or null list supplied");
		String query = modifiedQueryForList(CONCEPT_QUERY, ":idList", conceptIds);
		return getJdbcTemplate().queryForList(query, conceptIds.toArray());
	}

	/**
	 * Modify the query to take the required number of arguments.
	 * 
	 * @param query
	 * @param placeHolder
	 * @param conceptIds
	 * @return modified query
	 */
	private String modifiedQueryForList(String query, String placeHolder,
			List<Long> conceptIds) {
		StringBuffer sb = new StringBuffer();
		sb.append("?");
		for(int i=1; i<conceptIds.size(); i++){
			sb.append(",?");
		}
		return query.replace(placeHolder, sb.toString());
	}
}
