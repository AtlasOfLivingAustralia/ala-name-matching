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
package au.org.ala.sds.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitiveSpecies implements Comparable<SensitiveSpecies> {

    protected static final Logger logger = Logger.getLogger(SensitiveSpecies.class);

    private final String scientificName;
    private String acceptedName;
    private String lsid;
    private final List<SensitivityInstance> instances;

    public SensitiveSpecies(String scientificName) {
        super();
        this.scientificName = scientificName;
        this.instances = new ArrayList<SensitivityInstance>();
    }

    public String getScientificName() {
        return this.scientificName;
    }

    public String getAcceptedName() {
        return this.acceptedName;
    }

    public void setAcceptedName(String acceptedName) {
        this.acceptedName = acceptedName;
    }

    public List<SensitivityInstance> getInstances() {
        return this.instances;
    }

    public String getLsid() {
        return this.lsid;
    }

    public void setLsid(String lsid) {
        this.lsid = lsid;
    }

    public boolean isSensitiveForZone(SensitivityZone zone) {
        for (SensitivityInstance si : this.instances) {
            if (zone.equals(si.getZone())) {
                return true;
            }
        }

        return false;
    }

    public List<SensitivityInstance> getInstancesForZones(Set<SensitivityZone> zones) {
        List<SensitivityInstance> instanceList = new ArrayList<SensitivityInstance>();
        for (SensitivityInstance si : this.instances) {
            if (zones.contains(si.getZone())) {
                instanceList.add(si);
            } else if (si.getZone().equals(SensitivityZone.AUS) &&
                       SensitivityZone.isInAustralia(zones)) {
                instanceList.add(si);
            }
        }
        return instanceList;
    }

    public SensitivityInstance getSensitivityInstance(String state) {
        return getInstanceForState(SensitivityZone.getZone(state));
    }

    public SensitivityInstance getInstanceForState(SensitivityZone state) {
        SensitivityInstance instance = null;
        SensitivityInstance ausInstance = null;
        for (SensitivityInstance si : this.instances) {
            if (state == si.getZone()) {
                instance = si;
            } else {
                if (si.getZone() == SensitivityZone.AUS) {
                    ausInstance = si;
                }
            }
        }

        if (instance == null) {
            instance = ausInstance;
        }
        return instance;
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
