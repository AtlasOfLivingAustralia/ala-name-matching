package au.org.ala.sds.model;

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
    TSPZ("Torres Strait Protected Zone");

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


}
