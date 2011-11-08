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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.log4j.Logger;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitiveTaxon implements Serializable, Comparable<SensitiveTaxon> {

    private static final long serialVersionUID = 1L;

    protected static final Logger logger = Logger.getLogger(SensitiveTaxon.class);

    public enum Rank { SPECIES, GENUS, FAMILY };

    private final String name;
    private String family;
    private String commonName;
    private final Rank rank;
    private String acceptedName;
    private String lsid;
    private final List<SensitivityInstance> instances;

    public SensitiveTaxon(String taxon, Rank rank) {
        super();
        this.name = taxon;
        this.rank = rank;
        this.instances = new ArrayList<SensitivityInstance>();
    }

    public String getName() {
        return this.name;
    }

    public String getFamily() {
        return this.family == null ? "" : this.family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public Rank getRank() {
        return this.rank;
    }

    public String getTaxonName() {
        if (StringUtils.isNotBlank(acceptedName)) {
            return acceptedName;
        } else {
            return name;
        }
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

    public boolean isDateRequired() {
        for (SensitivityInstance si : this.instances) {
            if (si instanceof PlantPestInstance) {
                if (((PlantPestInstance) si).getFromDate() != null || ((PlantPestInstance) si).getToDate() != null) {
                    return true;
                }
            }
        }

        return false;
    }

    public List<SensitivityInstance> getInstancesForZones(List<SensitivityZone> zones) {
        List<SensitivityInstance> instanceList = new ArrayList<SensitivityInstance>();
        for (SensitivityInstance si : this.instances) {
            if (zones.contains(si.getZone())) {
                instanceList.add(si);
            } else if (si.getZone().equals(SensitivityZoneFactory.getZone(SensitivityZone.AUS)) &&
                       SensitivityZone.isInAustralia(zones)) {
                instanceList.add(si);
            }
        }
        return instanceList;
    }

    public SensitivityInstance getSensitivityInstance(String state) {
        return getInstanceForState(SensitivityZoneFactory.getZone(state));
    }

    public SensitivityInstance getInstanceForState(SensitivityZone state) {
        SensitivityInstance instance = null;
        SensitivityInstance ausInstance = null;
        for (SensitivityInstance si : this.instances) {
            if (state == si.getZone()) {
                instance = si;
            } else {
                if (si.getZone() == SensitivityZoneFactory.getZone(SensitivityZone.AUS)) {
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
        return new HashCodeBuilder(17, 37).
            append(this.name).
            append(this.family).
            append(this.rank).
            append(this.commonName).
            toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SensitiveTaxon other = (SensitiveTaxon) obj;
        return this.name.equals(other.name);
    }

    @Override
    public int compareTo(SensitiveTaxon st) {
        return this.name.compareTo(st.name);
    }

    @Override
    public String toString() {
        return getTaxonName() + " (" + this.rank + ")";
    }
}
