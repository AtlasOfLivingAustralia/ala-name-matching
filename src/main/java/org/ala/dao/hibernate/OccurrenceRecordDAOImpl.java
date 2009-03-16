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

import java.util.List;

import org.ala.dao.OccurrenceRecordDAO;
import org.gbif.portal.model.occurrence.OccurrenceRecord;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;

/**
 * 
 *
 * @author Dave Martin (David.Martin@csiro.au)
 */
public class OccurrenceRecordDAOImpl extends org.gbif.portal.dao.occurrence.impl.hibernate.OccurrenceRecordDAOImpl implements OccurrenceRecordDAO{

	/**
	 * @see org.ala.dao.OccurrenceRecordDAO#getOccurrenceRecordsForCentiCell(int, long, int, int)
	 */
	public List<OccurrenceRecord> getOccurrenceRecordsForCentiCell(
			final int entityType, final long entityId, final int cellId, final int centiCellId) {
		
		if(entityType==org.ala.dao.EntityType.TYPE_GEO_REGION.getId()){
			return (List<OccurrenceRecord>) getHibernateTemplate().execute(new HibernateCallback() {
				public Object doInHibernate(Session session) {
					Query query = session.createQuery(
							"select oc from OccurrenceRecord oc " +
							"inner join oc.geoMappings gm " +
							"inner join fetch oc.dataProvider " +
							"inner join fetch oc.dataResource " +
							"inner join fetch oc.taxonName " +
							"inner join fetch oc.institutionCode " +
							"inner join fetch oc.collectionCode " +
							"inner join fetch oc.catalogueNumber " +
							"where oc.cellId=? and oc.centiCellId=? and gm.geoRegionId=? " +
							"order by oc.taxonName.canonical");
					query.setInteger(0, cellId);
					query.setInteger(1, centiCellId);
					query.setLong(2, entityId);
					return query.list();
				}
			});	
		}
		throw new IllegalArgumentException("Unsupported entity type :"+entityType);
	}
}
