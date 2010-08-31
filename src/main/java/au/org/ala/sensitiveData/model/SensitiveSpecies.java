/***************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia
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
package au.org.ala.sensitiveData.model;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitiveSpecies implements Comparable<SensitiveSpecies> {

	private String scientificName;
	private SensitivityCategory sensitivityCategory;
	
	public SensitiveSpecies(String scientificName, SensitivityCategory sensitivityCategory) {
		super();
		this.scientificName = scientificName;
		this.sensitivityCategory = sensitivityCategory;
	}

	public String getScientificName() {
		return this.scientificName;
	}

	public SensitivityCategory getSensitivityCategory() {
		return sensitivityCategory;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((scientificName == null) ? 0 : scientificName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SensitiveSpecies other = (SensitiveSpecies) obj;
		if (scientificName == null) {
			if (other.scientificName != null)
				return false;
		} else if (!scientificName.equals(other.scientificName))
			return false;
		return true;
	}

	@Override
	public int compareTo(SensitiveSpecies ss) {
		return scientificName.compareTo(ss.getScientificName());
	}


}
