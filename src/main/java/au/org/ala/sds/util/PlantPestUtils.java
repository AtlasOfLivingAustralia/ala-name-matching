/**
 *
 */
package au.org.ala.sds.util;

import java.util.Date;
import java.util.Set;

import au.org.ala.sds.model.PlantPestInstance;
import au.org.ala.sds.model.SensitiveSpecies;
import au.org.ala.sds.model.SensitivityInstance;
import au.org.ala.sds.model.SensitivityZone;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class PlantPestUtils {

    public static boolean isInZoneDuringPeriod(SensitiveSpecies ss, Set<SensitivityZone> zones, Date date) {
        for (SensitivityInstance si : ss.getInstances()) {
            if (zones.contains(si.getZone()) &&
                date.getTime() >= ((PlantPestInstance) si).getFromDate().getTime() &&
                date.getTime() <= ((PlantPestInstance) si).getToDate().getTime()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInZoneBeforePeriod(SensitiveSpecies ss, Set<SensitivityZone> zones, Date date) {
        for (SensitivityInstance si : ss.getInstances()) {
            if (zones.contains(si.getZone()) &&
                date.getTime() < ((PlantPestInstance) si).getFromDate().getTime()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInZoneAfterPeriod(SensitiveSpecies ss, Set<SensitivityZone> zones, Date date) {
        for (SensitivityInstance si : ss.getInstances()) {
            if (zones.contains(si.getZone()) &&
                date.getTime() > ((PlantPestInstance) si).getToDate().getTime()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOutsideZone(SensitiveSpecies ss, Set<SensitivityZone> zones) {
        for (SensitivityInstance si : ss.getInstances()) {
            if (zones.contains(si.getZone())) {
                return false;
            }
        }
        return true;
    }
}
