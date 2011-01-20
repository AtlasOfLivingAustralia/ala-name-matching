package au.org.ala.sensitiveData.model;

public enum SensitivityZone {
    AUS("Australia"),
    ACT("Australian Capital Territory"), 
    NSW("New South Wales"),
    QLD("Queensland"),
    VIC("Victoria"),
    TAS("Tasmania"),
    SA("South Australia"),
    WA("Western Australia"),
    NT("Northern Territory");
    
    private String value;
    
    private SensitivityZone(String value) {
        this.value = value;
    }
    
    public static SensitivityZone getZone(String value) {
        for (SensitivityZone zone : SensitivityZone.values()) {
            if (zone.getValue().equals(value)) {
                return zone;
            }
        }
        return null;
    }
    
    public String getValue() {
        return value;
    }


}
