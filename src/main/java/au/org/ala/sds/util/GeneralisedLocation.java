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
import au.org.ala.sds.model.SensitivityInstance;

/**
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class GeneralisedLocation {
    private final String originalLatitude;
    private final String originalLongitude;
    private final String locationGeneralisation;
    private String generalisedLatitude;
    private String generalisedLongitude;
    private String generalisationInMetres;
    private String description;

    public GeneralisedLocation(String latitude, String longitude, SensitiveSpecies ss) {
        this.originalLatitude = latitude;
        this.originalLongitude = longitude;
        SensitivityInstance instance = ss.getSensitivityInstance(latitude, longitude);
        if (instance != null) {
            this.locationGeneralisation = instance.getLocationGeneralisation();
        } else {
            this.locationGeneralisation = null;
        }
        generaliseCoordinates();
    }

    public GeneralisedLocation(String latitude, String longitude, SensitiveSpecies ss, String state) {
        this.originalLatitude = latitude;
        this.originalLongitude = longitude;
        SensitivityInstance instance = ss.getSensitivityInstance(state);
        if (instance != null) {
            this.locationGeneralisation = instance.getLocationGeneralisation();
        } else {
            this.locationGeneralisation = null;
        }
        generaliseCoordinates();
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

    public String getDescription() {
        return description;
    }

    private void generaliseCoordinates() {

        if (this.locationGeneralisation == null) {
            generalisedLatitude = originalLatitude;
            generalisedLongitude = originalLongitude;
            generalisationInMetres = "";
            description = "Location not generalised because it is not sensitive in that area.";
            return;
        }

        int decimalPlaces;
        if (this.locationGeneralisation.equalsIgnoreCase("WITHHOLD")) {
            generalisedLatitude = "";
            generalisedLongitude = "";
            generalisationInMetres = "";
            description = "Location withheld.";
            return;
        } else if (this.locationGeneralisation.equalsIgnoreCase("10km")) {
            decimalPlaces = 1;
            generalisationInMetres = "10000";
            description = "Location generalised to one decimal place.";
        } else if (this.locationGeneralisation.equalsIgnoreCase("1km")) {
            decimalPlaces = 2;
            generalisationInMetres = "1000";
            description = "Location generalised to two decimal places.";
        } else if (this.locationGeneralisation.equalsIgnoreCase("100m")) {
            decimalPlaces = 3;
            generalisationInMetres = "100";
            description = "Location generalised to three decimal places.";
        } else {
            generalisedLatitude = originalLatitude;
            generalisedLongitude = originalLongitude;
            generalisationInMetres = "";
            description = "Location not generalised because it is undefined.";
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
