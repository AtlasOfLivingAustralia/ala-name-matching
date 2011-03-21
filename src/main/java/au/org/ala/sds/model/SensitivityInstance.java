    package au.org.ala.sds.model;

/**
*
* @author Peter Flemming (peter.flemming@csiro.au)
*/
public class SensitivityInstance {

    private final SensitivityCategory category;
    private final String authority;
    private final SensitivityZone zone;

    public SensitivityInstance(SensitivityCategory category, String authority, SensitivityZone zone) {
        this.category = category;
        this.authority = authority;
        this.zone = zone;
    }

    public SensitivityCategory getCategory() {
        return category;
    }

    public String getAuthority() {
        return authority;
    }

    public SensitivityZone getZone() {
        return zone;
    }

}
