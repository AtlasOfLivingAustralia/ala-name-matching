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

import org.ala.dao.GeoRegionDAO;
import org.ala.model.GeoRegion;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * A hibernate based DAO implementation of the {@link GeoRegionDAO} interface.
 * 
 * @author dmartin
 */
public class GeoRegionDAOImpl extends HibernateDaoSupport implements GeoRegionDAO {

    /**
	 * @see org.gbif.portal.dao.geospatial.GeoRegionDAO#getGeoRegionFor(java.lang.Long)
	 */
	public GeoRegion getGeoRegionFor(final Long geoRegionId) {
		return (GeoRegion) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				return (GeoRegion) session.get(GeoRegion.class, geoRegionId);
			}
		});
	}

    /** 
     * @see org.ala.dao.geospatial.GeoRegionDAO#getGeoRegionsForOccurrenceRecord(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<GeoRegion> getGeoRegionsForOccurrenceRecord(final Long occurrenceRecordId){
		return (List<GeoRegion>) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				Query query = session.createQuery("select gr from OccurrenceRecord ore " +
						" inner join ore.geoMappings as gm" +
						" inner join gm.geoRegion as gr "+
						" where ore.id=:id");
				query.setLong("id", occurrenceRecordId);
				return query.list();
			}
		});
	}


	/**
	 * @see org.ala.dao.geospatial.GeoRegionDAO#getGeoRegionsForGeoRegionType(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<GeoRegion> getGeoRegionsForGeoRegionType(final Long minGeoRegionTypeId, final Long maxGeoRegionTypeId) {
		return (List<GeoRegion>) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				Query query =  session.createQuery("from GeoRegion gr where gr.regionType>=:minRegionTypeId and gr.regionType<=:maxRegionTypeId order by gr.regionType, gr.name");
				query.setLong("minRegionTypeId", minGeoRegionTypeId);
				query.setLong("maxRegionTypeId", maxGeoRegionTypeId);
				return (List<GeoRegion>)query.list();
			}
		});
	}	
}