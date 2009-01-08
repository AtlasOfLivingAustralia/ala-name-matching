package org.ala.web.controller;

/**
 * Utility class to generate the range of Ids for a given
 * "class" of geo region types (REST style parameter)
 * 
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 *
 */
public class RegionType {
	private String name;

	private Long minTypeId;
	private Long maxTypeId;
	
	public RegionType(String regionType) {
		super();
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
		else if("ibra".equals(name)) {
			minTypeId = 2000L;
			maxTypeId = 2999L;
		}
		else if("imra".equals(name)) {
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
