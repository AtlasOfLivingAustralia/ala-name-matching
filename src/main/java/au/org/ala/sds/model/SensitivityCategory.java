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

import java.util.EnumSet;
import java.util.Set;

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
    RARE("R"),
    WA_PRIORITY_1("P1"),
    WA_PRIORITY_2("P2"),
    WA_PRIORITY_3("P3"),
    WA_PRIORITY_4("P4"),
    WA_PRIORITY_5("P5"),
    WA_SPECIALLY_PROTECTED("SP"),

    PLANT_PEST_NOT_KNOWN_IN_AUSTRALIA("PBC1"),
    PLANT_PEST_ERADICATED("PBC2"),
    PLANT_PEST_UNDER_ERADICATION("PBC3"),
    PLANT_PEST_SUBJECT_TO_OFFICIAL_CONTROL("PBC4"),
    PLANT_PEST_IN_TORRES_STRAIT_ZONE("PBC5a"),
    PLANT_PEST_IS_QUEENSLAND_FRUIT_FLY("PBC5b"),
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

    public static boolean isConservationSensitive(SensitivityCategory category) {
        Set<SensitivityCategory> conservationCategories = EnumSet.of(
                NOT_EVALUATED, DATA_DEFICIENT, LEAST_CONCERN,
                NEAR_THREATENED, CONSERVATION_DEPENDENT, VULNERABLE,
                ENDANGERED, CRITICALLY_ENDANGERED, EXTINCT_IN_THE_WILD,
                EXTINCT, RARE, WA_PRIORITY_1, WA_PRIORITY_2, WA_PRIORITY_3,
                WA_PRIORITY_4, WA_PRIORITY_5, WA_SPECIALLY_PROTECTED);

        return conservationCategories.contains(category);
    }

    public static boolean isPlantPest(SensitivityCategory category) {
        Set<SensitivityCategory> plantPestCategories = EnumSet.of(
                PLANT_PEST_NOT_KNOWN_IN_AUSTRALIA,
                PLANT_PEST_ERADICATED,
                PLANT_PEST_UNDER_ERADICATION,
                PLANT_PEST_SUBJECT_TO_OFFICIAL_CONTROL,
                PLANT_PEST_IN_TORRES_STRAIT_ZONE,
                PLANT_PEST_IS_QUEENSLAND_FRUIT_FLY,
                PLANT_PEST_NOTIFIABLE_UNDER_STATE_LEGISLATION);

        return plantPestCategories.contains(category);
    }
}
