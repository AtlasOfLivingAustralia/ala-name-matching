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
package org.ala.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.apache.solr.analysis.LowerCaseFilterFactory;
import org.apache.solr.analysis.SnowballPorterFilterFactory;
import org.apache.solr.analysis.StandardTokenizerFactory;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;

@Entity
@Indexed(index="GeoRegions")
@javax.persistence.Table(name="geo_region", schema="portal")
@AnalyzerDef(name = "customanalyzer",
  tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
  filters = {
    @TokenFilterDef(factory = LowerCaseFilterFactory.class),
    @TokenFilterDef(factory = SnowballPorterFilterFactory.class, params = {
      @Parameter(name = "language", value = "English")
    })
  })
public class GeoRegion implements Serializable {

	private static final long serialVersionUID = 1780062588651878219L;

	@Id
	@GeneratedValue
	@DocumentId
	protected long id;
	
	@Fields( {
        @Field(index=Index.TOKENIZED, store=Store.YES),
        @Field(name = "nameForSort", index=Index.UN_TOKENIZED, store = Store.YES)
    })
	protected String name;
	
	@Field(index=Index.TOKENIZED, store=Store.YES)
	protected String acronym;

	@ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="region_type")
    @IndexedEmbedded(depth=2)
	protected GeoRegionType geoRegionType;
	
	@Column(name="iso_country_code")
	protected String isoCountryCode;
	
	@Column(name="occurrence_count")
	protected int occurrenceCount; 
	
	@Column(name="occurrence_coordinate_count")
	protected int occurrenceCoordinateCount;
	
	@Column(name="min_latitude")
	protected int minLatitude; 
	
	@Column(name="max_latitude")
	protected int maxLatitude; 
	
	@Column(name="min_longitude")
	protected int minLongitude;
	
	@Column(name="max_longitude")
	protected int maxLongitude; 
	
	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the geoRegionType
	 */
	public GeoRegionType getGeoRegionType() {
		return geoRegionType;
	}

	/**
	 * @param geoRegionType the geoRegionType to set
	 */
	public void setGeoRegionType(GeoRegionType geoRegionType) {
		this.geoRegionType = geoRegionType;
	}

	/**
	 * @return the acronym
	 */
	public String getAcronym() {
		return acronym;
	}

	/**
	 * @param acronym the acronym to set
	 */
	public void setAcronym(String acronym) {
		this.acronym = acronym;
	}

	/**
	 * @return the isoCountryCode
	 */
	public String getIsoCountryCode() {
		return isoCountryCode;
	}

	/**
	 * @param isoCountryCode the isoCountryCode to set
	 */
	public void setIsoCountryCode(String isoCountryCode) {
		this.isoCountryCode = isoCountryCode;
	}

	/**
	 * @return the occurrenceCount
	 */
	public int getOccurrenceCount() {
		return occurrenceCount;
	}

	/**
	 * @param occurrenceCount the occurrenceCount to set
	 */
	public void setOccurrenceCount(int occurrenceCount) {
		this.occurrenceCount = occurrenceCount;
	}

	/**
	 * @return the occurrenceCoordinateCount
	 */
	public int getOccurrenceCoordinateCount() {
		return occurrenceCoordinateCount;
	}

	/**
	 * @param occurrenceCoordinateCount the occurrenceCoordinateCount to set
	 */
	public void setOccurrenceCoordinateCount(int occurrenceCoordinateCount) {
		this.occurrenceCoordinateCount = occurrenceCoordinateCount;
	}

	/**
	 * @return the minLatitude
	 */
	public int getMinLatitude() {
		return minLatitude;
	}

	/**
	 * @param minLatitude the minLatitude to set
	 */
	public void setMinLatitude(int minLatitude) {
		this.minLatitude = minLatitude;
	}

	/**
	 * @return the maxLatitude
	 */
	public int getMaxLatitude() {
		return maxLatitude;
	}

	/**
	 * @param maxLatitude the maxLatitude to set
	 */
	public void setMaxLatitude(int maxLatitude) {
		this.maxLatitude = maxLatitude;
	}

	/**
	 * @return the minLongitude
	 */
	public int getMinLongitude() {
		return minLongitude;
	}

	/**
	 * @param minLongitude the minLongitude to set
	 */
	public void setMinLongitude(int minLongitude) {
		this.minLongitude = minLongitude;
	}

	/**
	 * @return the maxLongitude
	 */
	public int getMaxLongitude() {
		return maxLongitude;
	}

	/**
	 * @param maxLongitude the maxLongitude to set
	 */
	public void setMaxLongitude(int maxLongitude) {
		this.maxLongitude = maxLongitude;
	}
}
