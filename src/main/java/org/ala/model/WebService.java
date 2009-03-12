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
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@javax.persistence.Table(name="resource_access_point", schema="portal")
public class WebService implements Serializable {

	private static final long serialVersionUID = -1142815858957706781L;
	@Id
	@GeneratedValue
	protected long id;
	@Column(name="remote_id_at_url")
	protected String remoteId;
	protected String url;
	@Column(name="iso_country_code")
	protected String isoCountryCode;
	@Column(name="available")
	protected boolean available; //so did the metadata request work
	@Column(name="last_harvest_start")
	protected Date lastHarvestStart;
	@Column(name="last_extract_start")
	protected Date lastExtractStart;
	@Column(name="supports_date_last_modified")
	protected boolean supportsDateLastModified;
	@Column(name="occurrence_count")
	protected int occurrenceCount;
	
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
	 * @return the remoteId
	 */
	public String getRemoteId() {
		return remoteId;
	}
	/**
	 * @param remoteId the remoteId to set
	 */
	public void setRemoteId(String remoteId) {
		this.remoteId = remoteId;
	}
	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}
	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
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
	 * @return the supportsDateLastModified
	 */
	public boolean isSupportsDateLastModified() {
		return supportsDateLastModified;
	}
	/**
	 * @param supportsDateLastModified the supportsDateLastModified to set
	 */
	public void setSupportsDateLastModified(boolean supportsDateLastModified) {
		this.supportsDateLastModified = supportsDateLastModified;
	}
	/**
	 * @return the available
	 */
	public boolean isAvailable() {
		return available;
	}
	/**
	 * @param available the available to set
	 */
	public void setAvailable(boolean available) {
		this.available = available;
	}
	/**
	 * @return the lastHarvestStart
	 */
	public Date getLastHarvestStart() {
		return lastHarvestStart;
	}
	/**
	 * @param lastHarvestStart the lastHarvestStart to set
	 */
	public void setLastHarvestStart(Date lastHarvestStart) {
		this.lastHarvestStart = lastHarvestStart;
	}
	/**
	 * @return the lastExtractStart
	 */
	public Date getLastExtractStart() {
		return lastExtractStart;
	}
	/**
	 * @param lastExtractStart the lastExtractStart to set
	 */
	public void setLastExtractStart(Date lastExtractStart) {
		this.lastExtractStart = lastExtractStart;
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
}