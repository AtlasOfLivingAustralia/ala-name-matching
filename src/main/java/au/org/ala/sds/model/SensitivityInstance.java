    package au.org.ala.sds.model;

import java.io.Serializable;

/**
*
* @author Peter Flemming (peter.flemming@csiro.au)
*/
public class SensitivityInstance implements Serializable {

    private static final long serialVersionUID = 1L;

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
