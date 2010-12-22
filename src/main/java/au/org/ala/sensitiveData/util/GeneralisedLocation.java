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
package au.org.ala.sensitiveData.util;

import java.math.BigDecimal;

import au.org.ala.sensitiveData.model.SensitivityCategory;

/**
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class GeneralisedLocation {
    private String originalLatitude;
    private String originalLongitude;
    private String generalisedLatitude;
    private String generalisedLongitude;
    private String generalisationInMetres;
    private SensitivityCategory category;
    
    public GeneralisedLocation(String latitude, String Longitude, SensitivityCategory category) {
        originalLatitude = latitude;
        originalLongitude = Longitude;
        this.category = category;
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

    public SensitivityCategory getCategory() {
        return category;
    }

   private void generaliseCoordinates() {
       int decimalPlaces;
       switch (this.category) {
       case CONSERVATION_EXTREME:
           this.generalisedLatitude = "";
           this.generalisedLongitude = "";
           this.generalisationInMetres = "";
           return;
       case CONSERVATION_HIGH:
           decimalPlaces = 1;
           this.generalisationInMetres = "10000";
           break;
       case CONSERVATION_MEDIUM:
           decimalPlaces = 2;
           this.generalisationInMetres = "1000";
           break;
       case CONSERVATION_LOW:
           decimalPlaces = 3;
           this.generalisationInMetres = "100";
           break;
       default:
           this.generalisedLatitude = this.originalLatitude;
           this.generalisedLongitude = this.originalLongitude;
           this.generalisationInMetres = "";
           return;
       }
       
       this.generalisedLatitude = round(this.originalLatitude, decimalPlaces);
       this.generalisedLongitude = round(this.originalLongitude, decimalPlaces);
       
   }

   private String round(String number, int decimalPlaces) {
       if (number == null || number.equals("")) {
           return "";
       } else {
           return String.format("%." + decimalPlaces + "f", new BigDecimal(number));
       }
   }
}
