/**
 * 
 */
package org.ala.dao.geo;

import java.util.List;

import org.gbif.portal.model.geospatial.GeoRegion;
import org.gbif.portal.model.occurrence.OccurrenceRecord;
/**
 * DAO interface providing access to GeoRegion model objects
 * 
 * @author nick
 */
public interface GeoRegionDAO {
	
	/**
	 * Retrieve a list of occurrences for this region.
	 * 
	 * @param geoRegionId
	 * @param startIndex
	 * @param maxResults
	 * @return list of occurrences
	 */
	public List<OccurrenceRecord> getOccurrencesForGeoRegion(Long geoRegionId, int startIndex, int maxResults);
	
	/**
	 * Get Geo Regions for Occurrence Record
	 * 
	 * @param occurrenceRecordId
	 * @return
	 */
	public List<GeoRegion> getGeoRegionsForOccurrenceRecord(Long occurrenceRecordId);
	
	/**
	 * Get the geo region for this id
	 * 
	 * @param geoRegionId
	 * @return
	 */
	public GeoRegion getGeoRegionFor(Long geoRegionId);
	
	/**
	 * Get a list of all the geo regions
	 * 
	 * @return list of geo regions
	 */
	public List<GeoRegion> getGeoRegions();
	
	/**
	 * Get a list of the geo regions for a range of geo region types
	 * 
	 * @param minGeoRegionTypeId
	 * @param maxGeoRegionTypeId
	 * @return list of geo regions
	 */
	public List<GeoRegion> getGeoRegionsForGeoRegionType(final Long minGeoRegionTypeId, final Long maxGeoRegionTypeId);
}
