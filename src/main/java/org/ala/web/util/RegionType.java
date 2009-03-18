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
package org.ala.web.util;

/**
 * Utility class to generate the range of Ids for a given
 * "class" of geo region types 
 * 
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class RegionType {

	private String name;
	private Long minTypeId;
	private Long maxTypeId;
	
	public RegionType(String regionType) {
		this.name = regionType;
		this.getIdRanges();
	}
	
	/**
	 * Generate the min and max Ids for a given region type
	 * via a simple lookup table
	 */
	private void getIdRanges() {
		if("states".equals(name)) {
			minTypeId = 1L;
			maxTypeId = 2L;
		}
		else if("cities".equals(name)) {
			minTypeId = 3L;
			maxTypeId = 4L;
		}
		else if("lga".equals(name)) {
			minTypeId = 5L;
			maxTypeId = 8L;
		}
		else if("shires".equals(name)) {
			minTypeId = 9L;
			maxTypeId = 9L;
		}
		else if("towns".equals(name)) {
			minTypeId = 10L;
			maxTypeId = 11L;
		}
		else if("ibra".equals(name)) {
			minTypeId = 2000L;
			maxTypeId = 2999L;
		}
		else if("imcra".equals(name)) {
			minTypeId = 3000L;
			maxTypeId = 3999L;
		}
		else if("rivers".equals(name)) {
			minTypeId = 5000L;
			maxTypeId = 5999L;
		}
	}
	
	/**
	 * @return the minTypeId
	 */
	public Long getMinTypeId() {
		return minTypeId;
	}

	/**
	 * @return the maxTypeId
	 */
	public Long getMaxTypeId() {
		return maxTypeId;
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param regionType the regionType to set
	 */
	public void setName(String regionType) {
		this.name = regionType;
	}
	
}
