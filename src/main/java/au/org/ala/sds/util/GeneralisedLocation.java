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
import java.util.List;

import au.org.ala.sds.model.ConservationInstance;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.model.SensitivityInstance;
import au.org.ala.sds.model.SensitivityZone;
import au.org.ala.sds.validation.MessageFactory;

/**
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class GeneralisedLocation {
    private final String originalLatitude;
    private final String originalLongitude;
    private final List<SensitivityZone> zones;
    private final List<SensitivityInstance> instances;
    private String locationGeneralisation;
    private String generalisedLatitude;
    private String generalisedLongitude;
    private String generalisationInMetres;
    private String description;
    private boolean sensitive;

    public GeneralisedLocation(String latitude, String longitude, SensitiveTaxon st, List<SensitivityZone> zones) {
        this.originalLatitude = latitude;
        this.originalLongitude = longitude;
        this.locationGeneralisation = null;
        this.zones = zones;
        this.instances = st.getInstancesForZones(zones);
        this.locationGeneralisation = getLocationGeneralistion();
        this.sensitive = true;
        generaliseCoordinates();
    }

    private String getLocationGeneralistion() {
        for (SensitivityInstance si : instances) {
            if (si instanceof ConservationInstance) {
                return ((ConservationInstance) si).getLocationGeneralisation();
                // TODO pick most restrictive generalisation when there are multiple instances
            }
        }
        return null;
    }

    public boolean isGeneralised() {
        return !originalLatitude.equals(generalisedLatitude) || !originalLongitude.equals(generalisedLongitude);
    }

    public boolean isSensitive() {
        return sensitive;
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

    public List<SensitivityInstance> getSensitivityInstances() {
        return this.instances;
    }

    public List<SensitivityZone> getMatchingZones() {
        return zones;
    }

    private void generaliseCoordinates() {

        if (this.locationGeneralisation == null) {
            generalisedLatitude = originalLatitude;
            generalisedLongitude = originalLongitude;
            generalisationInMetres = "";
            description = MessageFactory.getMessageText(MessageFactory.LOCATION_NOT_GENERALISED, SensitivityZone.getState(zones));
            sensitive = false;
            return;
        }

        generalisationInMetres = "";
        if (this.locationGeneralisation.equalsIgnoreCase("WITHHOLD")) {
            generalisedLatitude = "";
            generalisedLongitude = "";
            description = MessageFactory.getMessageText(MessageFactory.LOCATION_WITHHELD);
        } else if (this.locationGeneralisation.equalsIgnoreCase("10km")) {
            generaliseCoordinates(1);
            if (isGeneralised()) {
                description = MessageFactory.getMessageText(MessageFactory.LOCATION_GENERALISED, SensitivityZone.getState(zones), "0.1");
                generalisationInMetres = "10000";
            } else {
                description = MessageFactory.getMessageText(MessageFactory.LOCATION_ALREADY_GENERALISED);
            }
        } else if (this.locationGeneralisation.equalsIgnoreCase("1km")) {
            generaliseCoordinates(2);
            if (isGeneralised()) {
                description = MessageFactory.getMessageText(MessageFactory.LOCATION_GENERALISED, SensitivityZone.getState(zones), "0.01");
                generalisationInMetres = "1000";
            } else {
                description = MessageFactory.getMessageText(MessageFactory.LOCATION_ALREADY_GENERALISED);
            }
        } else if (this.locationGeneralisation.equalsIgnoreCase("100m")) {
            generaliseCoordinates(3);
            if (isGeneralised()) {
                description = MessageFactory.getMessageText(MessageFactory.LOCATION_GENERALISED, SensitivityZone.getState(zones), "0.001");
                generalisationInMetres = "100";
            } else {
                description = MessageFactory.getMessageText(MessageFactory.LOCATION_ALREADY_GENERALISED);
            }
        } else {
            generalisedLatitude = originalLatitude;
            generalisedLongitude = originalLongitude;
            description = "Location not generalised because it is undefined.";
            sensitive = false;
        }
    }

    private void generaliseCoordinates(int decimalPlaces) {
        generalisedLatitude = round(originalLatitude, decimalPlaces);
        generalisedLongitude = round(originalLongitude, decimalPlaces);
    }

    private String round(String number, int decimalPlaces) {
        if (number == null || number.equals("")) {
            return "";
        } else {
            BigDecimal bd = new BigDecimal(number);
            if (bd.scale() > decimalPlaces) {
                return String.format("%." + decimalPlaces + "f", bd);
            } else {
                return number;
            }
        }
    }

}
