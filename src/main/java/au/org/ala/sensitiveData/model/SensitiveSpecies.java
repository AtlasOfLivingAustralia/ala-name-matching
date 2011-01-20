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

import java.util.ArrayList;
import java.util.List;

import au.org.ala.sensitiveData.util.GeoLocationHelper;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitiveSpecies implements Comparable<SensitiveSpecies> {

	private String scientificName;
	private List<SensitivityInstance> instances;
	
    public SensitiveSpecies(String scientificName) {
		super();
		this.scientificName = scientificName;
		this.instances = new ArrayList<SensitivityInstance>();
	}

	public String getScientificName() {
		return this.scientificName;
	}

    public List<SensitivityInstance> getInstances() {
        return instances;
    }

    public boolean isSensitiveForZone(SensitivityZone zone) {
        for (SensitivityInstance si : instances) {
            if (zone.equals(si.getZone())) {
                return true;
            }
        }
        
        return false;
    }
    
    public ConservationCategory getConservationCategory(String latitude, String longitude) {
        ConservationCategory category = null;
        
        // Avoid spatial gazetteer lookup if possible
        if (instances.size() == 1 && instances.get(0).getZone() == SensitivityZone.AUS) {
            category = instances.get(0).getCategory();
        } else {
            SensitivityZone state = null;

            try {
                state = GeoLocationHelper.getStateContainingPoint(latitude, longitude);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (state != null) {
                ConservationCategory ausCategory = null;
                for (SensitivityInstance instance : instances) {
                    if (state == instance.getZone()) {
                        category = instance.getCategory();
                    } else {
                        if (instance.getZone() == SensitivityZone.AUS) {
                            ausCategory = instance.getCategory();
                        }
                    }
                }
                
                if (category == null) {
                    category = ausCategory;
                }
            }
        }
        
        return category;
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
