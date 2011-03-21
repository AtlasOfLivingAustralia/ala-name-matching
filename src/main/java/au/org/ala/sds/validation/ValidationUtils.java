package au.org.ala.sds.validation;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import au.org.ala.sds.model.SensitivityZone;
import au.org.ala.sds.util.GeoLocationHelper;

public class ValidationUtils {
    protected static final Logger logger = Logger.getLogger(ValidationUtils.class);

    public static boolean validateFacts(FactCollection facts, ValidationReport report) {
        String state;
        Set<SensitivityZone> zones = new HashSet<SensitivityZone>();

        if ((state = facts.get(FactCollection.STATE_KEY)) == null) {
            if (StringUtils.isNotBlank(facts.get(FactCollection.LATITUDE_KEY)) &&
                StringUtils.isNotBlank(facts.get(FactCollection.LONGITUDE_KEY))) {
                try {
                    zones = GeoLocationHelper.getZonesContainingPoint(
                            facts.get(FactCollection.LATITUDE_KEY), facts.get(FactCollection.LONGITUDE_KEY));
                } catch (Exception e) {
                    logger.error("Problem getting zone from lat/long", e);
                }
            } else {
                if (StringUtils.isNotBlank(facts.get(FactCollection.COUNTRY_KEY))) {
                    if (facts.get(FactCollection.COUNTRY_KEY).equalsIgnoreCase(SensitivityZone.AUS.getValue())) {
                        zones.add(SensitivityZone.AUS);
                    } else {
                        zones.add(SensitivityZone.NOTAUS);
                    }
                }
            }
        } else {
            zones.add(SensitivityZone.getZone(state));
        }

        facts.add(FactCollection.ZONES_KEY, zones.toString());

        return true;
    }

    public static Date parseDate(String date) {
        if (StringUtils.isNotBlank(date)) {
            DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
            DateTime dt = fmt.parseDateTime(date);
            return dt.toDate();
        } else {
            return null;
        }
    }


}