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


package org.ala.dao.hibernate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.gbif.portal.model.resources.DataProvider;
import org.gbif.portal.model.resources.DataResource;
import org.gbif.portal.model.resources.ResourceNetwork;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

/**
 * Overrides the GBIF Portal implementation.
 *
 * @author Dave Martin (David.Martin@csiro.au)
 */
public class DataResourceDAOImpl extends org.gbif.portal.dao.resources.impl.hibernate.DataResourceDAOImpl {

	protected boolean onlyShowResourcesWithRecord = true;
	protected boolean onlyShowProvidersWithRecord = true;
	
	/**
	 * @see org.gbif.portal.dao.resources.impl.hibernate.DataResourceDAOImpl#findDataResourcesAndProvidersAndNetworks(java.lang.String, boolean, boolean, boolean, int, int)
	 */
	@Override
	public List findDataResourcesAndProvidersAndNetworks(final String nameStub, final boolean fuzzy, final boolean anyOccurrence, final boolean includeCountrySearch, final int startIndex, final int maxResults) {
		List modelObjects = new ArrayList();
		try {
			HibernateTemplate template = getHibernateTemplate();
			List results =  (List) template.execute(new HibernateCallback() {
				public Object doInHibernate(Session session) {
					
					String searchString = nameStub;
					if(fuzzy)
						searchString=searchString+'%';
					String anyPartNameString =  "%"+searchString;					
					
					StringBuffer sb = new StringBuffer("Select 'provider', dp.id, dp.name, 0 as shared_taxonomy, dp.occurrence_count, dp.occurrence_coordinate_count, dp.concept_count, dp.species_count, dp.data_resource_count, dp.id as provider_id, dp.name as provider_name, dp.iso_country_code from data_provider dp");
					sb.append(" where (dp.name like :nameStub ");
					if(anyOccurrence){
						sb.append(" or dp.name like '"+anyPartNameString +"'");
					}
					for(String prefix: resourceNamePrefixes)
						sb.append(" or dp.name like '"+prefix+" "+searchString+"'");
					
					sb.append(" and dp.deleted is null) ");
					if(onlyShowProvidersWithRecord)
						sb.append(" and dp.occurrence_count > 0 ");
					sb.append(" UNION ");
					sb.append(" Select 'resource', dr.id, dr.display_name, dr.shared_taxonomy as shared_taxonomy, dr.occurrence_count, dr.occurrence_coordinate_count, dr.concept_count, dr.species_count, dr.species_count, dp.id as provider_id, dp.name as provider_name, 'XX' from data_resource dr inner join data_provider dp on dr.data_provider_id=dp.id");
					sb.append(" where ( dr.display_name like :nameStub ");				
					if(anyOccurrence){
						sb.append(" or dr.display_name like '"+anyPartNameString +"'");
					}
					for(String prefix: resourceNamePrefixes){
						sb.append(" or dr.display_name like '");
						sb.append(prefix);
						sb.append(' ');
						sb.append(searchString);
						sb.append("'");
					}	
					sb.append(") and dr.deleted is null");
					if(onlyShowResourcesWithRecord)
						sb.append(" and dr.occurrence_count > 0");
					sb.append(" UNION ");
					sb.append(" Select 'network', rn.id, rn.name, 0 as shared_taxonomy, rn.occurrence_count, rn.occurrence_coordinate_count, rn.concept_count, rn.species_count, rn.data_resource_count, rn.id as provider_id, rn.name as provider_name, rn.code from resource_network rn left join country_name cn on rn.code = cn.iso_country_code");
					sb.append(" where ( rn.name like :nameStub ");
					if(includeCountrySearch){
						sb.append("or rn.code like :nameStub or cn.name like :nameStub");
					}
					if(anyOccurrence){
						sb.append(" or rn.name like '"+anyPartNameString +"'");
					}
					for(String prefix: resourceNamePrefixes){
						sb.append(" or rn.name like '");
						sb.append(prefix);
						sb.append(' ');
						sb.append(searchString);
						sb.append("'");
					}	
					sb.append(")");
					
					Query query = session.createSQLQuery(sb.toString());
					query.setParameter("nameStub", searchString);
					query.setMaxResults(maxResults);
					query.setFirstResult(startIndex);			
					query.setCacheable(true);
					return query.list();
				}
			});		
			
			for (Iterator iter = results.iterator(); iter.hasNext();) {
				Object[] result = (Object[]) iter.next();
				Object modelObject=null;
				Long id = null;
				if(result[1] instanceof BigInteger)
					id = ((BigInteger)result[1]).longValue();
				else if (result[1] instanceof Integer)
					id = ((Integer)result[1]).longValue();
				else if (result[1] instanceof Long)
					id = (Long)result[1];
				
				
				if(result[0].equals("resource")){
					modelObject = new DataResource(id, 
							(String) result[2], 
							((BigInteger)result[3]).intValue()==1, 
							(Integer)result[4], 
							(Integer)result[5], 
							(Integer)result[6], 
							(Integer)result[7], 
							(Integer)result[9], 
							(String)result[10]);
				} else if(result[0].equals("provider")){
					modelObject = new DataProvider(
							id, 
							(String) result[2], 
							(Integer)result[4], 
							(Integer)result[5], 
							(Integer)result[6], 
							(Integer)result[7], 
							(Integer)result[8], 
							(String)result[11]);
				} else {
					modelObject = new ResourceNetwork(id, (String)result[2], (String)result[11], (Integer)result[4], (Integer)result[5], (Integer)result[6], (Integer)result[7], (Integer)result[8]);
				} 
				modelObjects.add(modelObject);
			}
		} catch (Exception e){
			logger.error(e.getMessage(), e);
		}
		return modelObjects;
	}
	
	/**
	 * @see org.gbif.portal.dao.resources.DataResourceDAO#getDatasetAlphabet()
	 */
	@SuppressWarnings("unchecked")
	public List<Character> getDatasetAlphabet() {
		HibernateTemplate template = getHibernateTemplate();		
		List<String> dataResourceChars =  (List<String>) template.execute(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				Query query = session.createSQLQuery("select distinct(SUBSTRING(display_name,1,1)) from data_resource where display_name is not null and occurrence_count>0 and deleted is null order by display_name");				
				return query.list();
			}
		});
		List<String> dataProviderChars =  (List<String>) template.execute(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				Query query = session.createSQLQuery("select distinct(SUBSTRING(name,1,1)) from data_provider where name is not null and occurrence_count>0 and deleted is null order by name");				
				return query.list();
			}
		});		
		List<String> resourceNetworksChars =  (List<String>) template.execute(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				Query query = session.createSQLQuery("select distinct(SUBSTRING(name,1,1)) from resource_network where name is not null and occurrence_count>0  order by name");				
				return query.list();
			}
		});		
		
		ArrayList<Character> chars = new ArrayList<Character>();
		for(String result: dataResourceChars){
			if(StringUtils.isNotEmpty(result))
				chars.add(new Character(Character.toUpperCase(result.charAt(0))));
		}
		for(String result: dataProviderChars){
			if(StringUtils.isNotEmpty(result)){
				Character theChar = new Character(Character.toUpperCase(result.charAt(0)));
				if(!chars.contains(theChar))
					chars.add(theChar);
			}
		}
		for(String result: resourceNetworksChars){
			if(StringUtils.isNotEmpty(result)){
				Character theChar = new Character(Character.toUpperCase(result.charAt(0)));
				if(!chars.contains(theChar))
					chars.add(theChar);
			}
		}		
		
		Collections.sort(chars);
		return chars;
	}
}
