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
package org.ala.dao;

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
