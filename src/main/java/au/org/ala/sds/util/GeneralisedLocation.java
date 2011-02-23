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
package au.org.ala.sds.util;

import java.math.BigDecimal;

import au.org.ala.sds.model.SensitiveSpecies;
import au.org.ala.sds.model.SensitivityCategory;

/**
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class GeneralisedLocation {
    private final String originalLatitude;
    private final String originalLongitude;
    private String generalisedLatitude;
    private String generalisedLongitude;
    private String generalisationInMetres;
    private final SensitivityCategory category;
    private String description;

    public GeneralisedLocation(String latitude, String longitude, SensitivityCategory category) {
        originalLatitude = latitude;
        originalLongitude = longitude;
        this.category = category;
        generaliseCoordinates();
    }

    public GeneralisedLocation(String latitude, String longitude, SensitiveSpecies ss, String state) {
        this(latitude, longitude, ss.getConservationCategory(state));
    }

    public String getOriginalLatitude() {
        return originalLatitude;
    }

    public String getOriginalLongitude() {
        return originalLongitude;
    }

    public String getGeneralisedLatitude() {
        return generalisedLatitude;
    }

    public String getGeneralisedLongitude() {
        return generalisedLongitude;
    }

    public String getGeneralisationInMetres() {
        return generalisationInMetres;
    }

    public SensitivityCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    private void generaliseCoordinates() {

        if (category == null) {
            generalisedLatitude = originalLatitude;
            generalisedLongitude = originalLongitude;
            generalisationInMetres = "";
            description = "Location not generalised because it is not deemed sensitive in that area.";
            return;
        }

        int decimalPlaces;
        switch (category) {
            case CRITICALLY_ENDANGERED:
                generalisedLatitude = "";
                generalisedLongitude = "";
                generalisationInMetres = "";
                description = "Location withheld because species is " + category.getValue() + ".";
                return;
            case ENDANGERED:
                decimalPlaces = 1;
                generalisationInMetres = "10000";
                description = "Location generalised to one decimal place because species is " + category.getValue() + ".";
                break;
            case VULNERABLE:
                decimalPlaces = 2;
                generalisationInMetres = "1000";
                description = "Location generalised to two decimal places because species is " + category.getValue() + ".";
                break;
            case NEAR_THREATENED:
                decimalPlaces = 3;
                generalisationInMetres = "100";
                description = "Location generalised to three decimal places because species is " + category.getValue() + ".";
                break;
            default:
                generalisedLatitude = originalLatitude;
                generalisedLongitude = originalLongitude;
                generalisationInMetres = "";
                description = "Location not generalised because species conservation status is " + category.getValue() + ".";
                return;
        }

        generalisedLatitude = round(originalLatitude, decimalPlaces);
        generalisedLongitude = round(originalLongitude, decimalPlaces);
    }

    private String round(String number, int decimalPlaces) {
        if (number == null || number.equals("")) {
            return "";
        } else {
            return String.format("%." + decimalPlaces + "f", new BigDecimal(number));
        }
    }
}
