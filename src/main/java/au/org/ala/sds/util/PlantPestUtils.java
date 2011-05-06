/**
 *
 */
package au.org.ala.sds.util;

import java.util.Date;
import java.util.List;

import au.org.ala.sds.model.PlantPestInstance;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.model.SensitivityInstance;
import au.org.ala.sds.model.SensitivityZone;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class PlantPestUtils {

    public static boolean isInsideZone(SensitiveTaxon st, String categoryId, List<SensitivityZone> zones) {
        for (SensitivityInstance si : st.getInstances()) {
            if (si.getCategory().getId().equals(categoryId) && zones.contains(si.getZone())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOutsideZone(SensitiveTaxon st, String categoryId, List<SensitivityZone> zones) {
        for (SensitivityInstance si : st.getInstances()) {
            if (si.getCategory().getId().equals(categoryId) && zones.contains(si.getZone())) {
                return false;
            }
        }
        return true;
    }

    public static boolean isInZoneDuringPeriod(SensitiveTaxon st, String categoryId, List<SensitivityZone> zones, Date date) {
        PlantPestInstance ppi = (PlantPestInstance) getMatchingSensitivityInstance(st, categoryId, zones);
        if ((ppi != null)) {
            if (ppi.getFromDate() != null && ppi.getFromDate().getTime() > date.getTime()) {
                return false;
            }
            if (ppi.getToDate() == null) {
                return true;
            }
            if (ppi.getToDate().getTime() >= date.getTime()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInZoneBeforePeriod(SensitiveTaxon st, String categoryId, List<SensitivityZone> zones, Date date) {
        for (SensitivityInstance si : st.getInstances()) {
            if (si.getCategory().getId().equals(categoryId) && zones.contains(si.getZone())) {
                if (((PlantPestInstance) si).getFromDate() != null && date.getTime() < ((PlantPestInstance) si).getFromDate().getTime()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isInZoneAfterPeriod(SensitiveTaxon st, String categoryId, List<SensitivityZone> zones, Date date) {
        for (SensitivityInstance si : st.getInstances()) {
            if (si.getCategory().getId().equals(categoryId) && zones.contains(si.getZone()) &&
                date.getTime() > ((PlantPestInstance) si).getToDate().getTime()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAfterPeriod(SensitiveTaxon st, String categoryId, Date date) {
        for (SensitivityInstance si : st.getInstances()) {
            if (si.getCategory().getId().equals(categoryId) && date.getTime() > ((PlantPestInstance) si).getToDate().getTime()) {
                return true;
            }
        }
        return false;
    }

    public static String getSensitivityZone(SensitiveTaxon st, String categoryId, List<SensitivityZone> zones) {
        SensitivityInstance si = getMatchingSensitivityInstance(st, categoryId, zones);
        if (si != null) {
            return si.getZone().getName();
        } else {
            return "?";
        }
    }

    public static Date getSensitivityZoneStartDate(SensitiveTaxon st, String categoryId, List<SensitivityZone> zones) {
        SensitivityInstance si = getMatchingSensitivityInstance(st, categoryId, zones);
        if (si != null) {
            return ((PlantPestInstance) si).getFromDate();
        } else {
            return null;
        }
    }

    private static SensitivityInstance getMatchingSensitivityInstance(SensitiveTaxon st, String categoryId, List<SensitivityZone> zones) {
        for (SensitivityInstance si : st.getInstances()) {
            if (si.getCategory().getId().equals(categoryId) && zones.contains(si.getZone())) {
                    return si;
            }
        }
        return null;
    }
}
