package au.org.ala.sds.util;

import java.util.EnumSet;
import java.util.Set;

import au.org.ala.sds.model.SensitivityZone;

public class SensitivityZoneHelper {
    public static boolean isInAustralia(SensitivityZone zone) {
        Set<SensitivityZone> ausZones = EnumSet.of(
                SensitivityZone.ACT,
                SensitivityZone.NSW,
                SensitivityZone.NT,
                SensitivityZone.QLD,
                SensitivityZone.SA,
                SensitivityZone.TAS,
                SensitivityZone.VIC,
                SensitivityZone.WA);
        
        return ausZones.contains(zone) || zone == SensitivityZone.AUS;
    }
    
    public static boolean isExternalTerritory(SensitivityZone zone) {
        Set<SensitivityZone> externalTerritories = EnumSet.of(
                SensitivityZone.CX,
                SensitivityZone.CC,
                SensitivityZone.AC,
                SensitivityZone.CS,
                SensitivityZone.AQ,
                SensitivityZone.HM,
                SensitivityZone.NF);
        
        return externalTerritories.contains(zone);
    }

    public static boolean isNotAustralia(SensitivityZone zone) {
        return zone == null;
    }
    
}
