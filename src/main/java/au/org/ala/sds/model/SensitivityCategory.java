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

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public enum SensitivityCategory {

    NOT_EVALUATED("NE"),
    DATA_DEFICIENT("DD"),
    LEAST_CONCERN("LC"),
    NEAR_THREATENED("NT"),
    CONSERVATION_DEPENDENT("CD"),
    VULNERABLE("VU"),
    ENDANGERED("EN"),
    CRITICALLY_ENDANGERED("CR"),
    EXTINCT_IN_THE_WILD("EW"),
    EXTINCT("EX"),
    
    PLANT_PEST_NOT_KNOWN_IN_AUSTRALIA("PBC1"),
    PLANT_PEST_ERADICATED("PBC2"),
    PLANT_PEST_UNDER_ERADICATION("PBC3"),
    PLANT_PEST_SUBJECT_TO_OFFICIAL_CONTROL("PBC4"),
    PLANT_PEST_IN_QUARANTINE_OR_OTHER_PLANT_HEALTH_ZONE("PBC5"),
    PLANT_PEST_NOTIFIABLE_UNDER_STATE_LEGISLATION("PBC6");
    
    private String value;
    
    private SensitivityCategory(String value) {
        this.value = value;
    }
    
    public static SensitivityCategory getCategory(String value) {
        for (SensitivityCategory cat : SensitivityCategory.values()) {
            if (cat.getValue().equals(value)) {
                return cat;
            }
        }
        return null;
    }
    
    public String getValue() {
        return value;
    }

}
