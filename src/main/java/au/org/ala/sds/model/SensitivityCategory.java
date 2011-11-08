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

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class SensitivityCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String NOT_EVALUATED = "NE";
    public static final String DATA_DEFICIENT = "DD";
    public static final String LEAST_CONCERN = "LC";
    public static final String NEAR_THREATENED = "NT";
    public static final String CONSERVATION_DEPENDENT = "CD";
    public static final String VULNERABLE = "VU";
    public static final String ENDANGERED = "EN";
    public static final String CRITICALLY_ENDANGERED = "CR";
    public static final String EXTINCT_IN_THE_WILD = "EW";
    public static final String EXTINCT = "EX";
    public static final String RARE = "R";
    public static final String WA_PRIORITY_1 = "P1";
    public static final String WA_PRIORITY_2 = "P2";
    public static final String WA_PRIORITY_3 = "P3";
    public static final String WA_PRIORITY_4 = "P4";
    public static final String WA_PRIORITY_5 = "P5";
    public static final String WA_SPECIALLY_PROTECTED = "SP";

    public static final String PLANT_PEST_NOT_KNOWN_IN_AUSTRALIA = "PBC1";
    public static final String PLANT_PEST_ERADICATED = "PBC2";
    public static final String PLANT_PEST_UNDER_ERADICATION = "PBC3";
    public static final String PLANT_PEST_SUBJECT_TO_OFFICIAL_CONTROL = "PBC4";
    public static final String PLANT_PEST_IN_TORRES_STRAIT_ZONE = "PBC5a";
    public static final String PLANT_PEST_IS_QUEENSLAND_FRUIT_FLY = "PBC5b";
    public static final String PLANT_PEST_NOTIFIABLE_UNDER_STATE_LEGISLATION = "PBC6";

    public enum CategoryType { CONSERVATION, PLANT_PEST }

    private final String id;
    private final String value;
    private final CategoryType type;

    public SensitivityCategory(String id, String value, CategoryType type) {
        this.id = id;
        this.value = value;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public CategoryType getType() {
        return type;
    }

    public boolean isConservationSensitive() {
        return type.equals(CategoryType.CONSERVATION);
    }

    public boolean isPlantPest() {
        return type.equals(CategoryType.PLANT_PEST);
    }
}
