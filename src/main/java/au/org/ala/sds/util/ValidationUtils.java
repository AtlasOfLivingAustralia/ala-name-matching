/**
 *
 */
package au.org.ala.sds.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import au.org.ala.sds.model.SensitivityZone;
import au.org.ala.sds.model.SensitivityZoneFactory;
import au.org.ala.sds.validation.FactCollection;
import au.org.ala.sds.validation.MessageFactory;
import au.org.ala.sds.validation.ValidationReport;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class ValidationUtils {

    protected static final Logger logger = Logger.getLogger(ValidationUtils.class);

    public static boolean validateLocation(FactCollection facts, ValidationReport report) {
        String state = facts.get(FactCollection.STATE_PROVINCE_KEY);
        String decimalLatitude = facts.get(FactCollection.DECIMAL_LATITUDE_KEY);
        String decimalLongitude = facts.get(FactCollection.DECIMAL_LONGITUDE_KEY);

        List<SensitivityZone> zones = new ArrayList<SensitivityZone>();

        if (state == null) {
            if (StringUtils.isNotBlank(decimalLatitude) && StringUtils.isNotBlank(decimalLongitude)) {
                try {
                    zones = GeoLocationHelper.getZonesContainingPoint(decimalLatitude, decimalLongitude);
                } catch (Exception e) {
                    logger.error("Problem getting zone from lat/long", e);
                }
            } else {
                if (StringUtils.isNotBlank(facts.get(FactCollection.COUNTRY_KEY))) {
                    if (facts.get(FactCollection.COUNTRY_KEY).equalsIgnoreCase(SensitivityZoneFactory.getZone(SensitivityZone.AUS).getName())) {
                        zones.add(SensitivityZoneFactory.getZone(SensitivityZone.AUS));
                    } else {
                        zones.add(SensitivityZoneFactory.getZone(SensitivityZone.NOTAUS));
                    }
                }
            }
        } else {
            SensitivityZone zone = SensitivityZoneFactory.getZoneByName(state);
            if (zone == null) {
                zone = SensitivityZoneFactory.getZone(state.toUpperCase());
            }
            if (zone == null) {
                String country = facts.get(FactCollection.COUNTRY_KEY);
                if (StringUtils.isNotBlank(country) && !country.equalsIgnoreCase("Australia") && !country.equalsIgnoreCase("AUS")) {
                    report.addMessage(MessageFactory.createErrorMessage(MessageFactory.NOT_AUSTRALIA, country));
                } else {
                    report.addMessage(MessageFactory.createErrorMessage(MessageFactory.STATE_INVALID, state));
                }
                return false;
            }
            zones.add(zone);
        }

        facts.add(FactCollection.ZONES_KEY, zones.toString());

        return true;
    }

    public static boolean validateLocationCoords(FactCollection facts, ValidationReport report) {
        String decimalLatitude = facts.get(FactCollection.DECIMAL_LATITUDE_KEY);
        String decimalLongitude = facts.get(FactCollection.DECIMAL_LONGITUDE_KEY);

        if (StringUtils.isBlank(decimalLatitude) || StringUtils.isBlank(decimalLongitude)) {
            report.addMessage(MessageFactory.createErrorMessage(MessageFactory.LOCATION_MISSING));
            return false;
        }

        return isValidNumber(decimalLatitude) && isValidNumber(decimalLongitude);
    }

    public static String validateName(FactCollection facts) {
        String scientificName = facts.get(FactCollection.SCIENTIFIC_NAME_KEY);
        if (StringUtils.isNotBlank(scientificName) && !scientificName.equalsIgnoreCase("\\N")) {
            return scientificName;
        } else {
            String genus = facts.get(FactCollection.GENUS_KEY);
            String specificEpithet = facts.get(FactCollection.SPECIFIC_EPITHET_KEY);
            String intraSpecificEpithet = facts.get(FactCollection.INTRA_SPECIFIC_EPITHET_KEY);

            if (StringUtils.isBlank(specificEpithet)) {
                return StringUtils.isBlank(genus) ? "" : genus;
            } else {
                StringBuilder name = new StringBuilder(genus);
                name.append(" ").append(specificEpithet);
                if (StringUtils.isNotBlank(intraSpecificEpithet)) {
                    name.append(" ").append(intraSpecificEpithet);
                }
                return name.toString();
            }
        }
    }

    public static boolean isValidNumber(String number) {
        if (StringUtils.isBlank(number)) {
            return false;
        }

        try {
            Float.parseFloat(number);
        } catch (NumberFormatException ex) {
            return false;
        }

        return true;
    }
}
