package au.org.ala.sds.model;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public enum SensitivityZone {
    AUS("Australia"),
    ACT("Australian Capital Territory"),
    NSW("New South Wales"),
    QLD("Queensland"),
    VIC("Victoria"),
    TAS("Tasmania"),
    SA("South Australia"),
    WA("Western Australia"),
    NT("Northern Territory"),
    CC("Cocos (Keeling) Islands"),
    CX("Christmas Island"),
    AC("Ashmore and Cartier Islands"),
    CS("Coral Sea Islands"),
    NF("Norfolk Island"),
    HM("Heard and McDonald Islands"),
    AQ("Australian Antartic Territory"),
    TSPZ("Torres Strait Protected Zone"),
    PFFPQA1995("Papaya Fruit Fly Pest Quarantine Area 1995"),
    ECCPQA2004("Emerald Citrus Canker Pest Quarantine Area 2004"),
    NOTAUS("Not in Australia");

    private String value;

    private SensitivityZone(String value) {
        this.value = value;
    }

    public static SensitivityZone getZone(String value) {
        for (SensitivityZone zone : SensitivityZone.values()) {
            if (zone.getValue().equalsIgnoreCase(value)) {
                return zone;
            }
        }

        // Try abbreviation
        return SensitivityZone.valueOf(value.toUpperCase());
    }

    public String getValue() {
        return value;
    }

    public static boolean isInAustralia(SensitivityZone zone) {
        Set<SensitivityZone> ausZones = EnumSet.of(ACT, NSW, NT, QLD, SA, TAS, VIC, WA);
        return zone == SensitivityZone.AUS || ausZones.contains(zone) ;
    }

    public static boolean isInAustralia(Set<SensitivityZone> zones) {
        Set<SensitivityZone> ausZones = EnumSet.of(ACT, NSW, NT, QLD, SA, TAS, VIC, WA);
        for (SensitivityZone zone : zones) {
            if (zone == SensitivityZone.AUS || ausZones.contains(zone)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isExternalTerritory(SensitivityZone zone) {
        Set<SensitivityZone> externalTerritories = EnumSet.of(CX, CC, AC, CS, AQ, HM, NF);
        return externalTerritories.contains(zone);
    }

    public static boolean isExternalTerritory(Set<SensitivityZone> zones) {
        Set<SensitivityZone> externalTerritories = EnumSet.of(CX, CC, AC, CS, AQ, HM, NF);
        for (SensitivityZone zone : zones) {
            if (externalTerritories.contains(zone)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNotAustralia(SensitivityZone zone) {
        return zone == null || zone.equals(NOTAUS);
    }

    public static Set<SensitivityZone> getSetFromString(String string) {
        Set<SensitivityZone> zoneSet = new HashSet<SensitivityZone>();
        String[] zones = StringUtils.split(StringUtils.substringBetween(string, "[", "]"), ',');
        for (String zone : zones) {
            zoneSet.add(SensitivityZone.valueOf(StringUtils.strip(zone)));
        }
        return zoneSet;
    }

}
