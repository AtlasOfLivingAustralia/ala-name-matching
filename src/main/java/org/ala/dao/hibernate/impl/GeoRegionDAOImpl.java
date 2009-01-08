/**
 * 
 */
package org.ala.dao.hibernate.impl;

import java.util.List;

import org.ala.dao.geo.GeoRegionDAO;
import org.gbif.portal.model.geospatial.GeoRegion;
import org.gbif.portal.model.occurrence.OccurrenceRecord;
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
	 * @see org.ala.dao.geospatial.GeoRegionDAO#getOccurrencesForGeoRegion(java.lang.Long, int, int)
	 */
	@SuppressWarnings("unchecked")
	public List<OccurrenceRecord> getOccurrencesForGeoRegion(final Long geoRegionId, final int startIndex, final int maxResults){
		return (List<OccurrenceRecord>) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) {
//				StringBuffer sb = new StringBuffer("select gm.occurrenceRecord from GeoMapping gm where gm.identifier.geoRegionId = :geoRegionId");
				StringBuffer sb = new StringBuffer("from org.gbif.portal.model.occurrence.OccurrenceRecord as oc " +
						"inner join fetch oc.catalogueNumber " +
						"inner join fetch oc.collectionCode " +
						"inner join fetch oc.institutionCode  " +
						"where oc.geoMappings.geoRegionId = :geoRegionId");
				Query query = session.createQuery(sb.toString());
				query.setLong("geoRegionId", geoRegionId);
				query.setFirstResult(startIndex);
				query.setMaxResults(maxResults);
				return query.list();
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
	 * @see org.ala.dao.geospatial.GeoRegionDAO#getGeoRegionFor(java.lang.Long)
	 */
	public GeoRegion getGeoRegionFor(final Long geoRegionId) {
		return (GeoRegion) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				return (GeoRegion) session.get(GeoRegion.class, geoRegionId);
			}
		});
	}
	
	/**
	 * @see org.ala.dao.geospatial.GeoRegionDAO#getGeoRegions()
	 */
	@SuppressWarnings("unchecked")
	public List<GeoRegion> getGeoRegions() {
		return (List<GeoRegion>) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				Query query =  session.createQuery("from GeoRegion gr order by gr.regionType, gr.name");
				return (List<GeoRegion>)query.list();
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