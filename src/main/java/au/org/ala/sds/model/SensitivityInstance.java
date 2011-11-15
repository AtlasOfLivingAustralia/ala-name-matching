    package au.org.ala.sds.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
*
* @author Peter Flemming (peter.flemming@csiro.au)
*/
public class SensitivityInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String BIRDS_AUSTRALIA_INSTANCE = "Birds Australia";

    private final SensitivityCategory category;
    private final String authority;
    private final SensitivityZone zone;
    private final String reason = null;
    private final String remarks = null;

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

    public String getReason() {
        return reason;
    }

    public String getRemarks() {
        return remarks;
    }

    public static void removeInstance(List<SensitivityInstance> instances, String authority) {
        List<SensitivityInstance> authInstances = new ArrayList<SensitivityInstance>();
        for (SensitivityInstance instance : instances) {
            if (instance.authority.equalsIgnoreCase(authority)) {
                authInstances.add(instance);
            }
        }
        for (SensitivityInstance authInstance : authInstances) {
            instances.remove(authInstance);
        }
    }

    public static void removeAllButInstance(List<SensitivityInstance> instances, String authority) {
        List<SensitivityInstance> notAuthInstances = new ArrayList<SensitivityInstance>();
        for (SensitivityInstance instance : instances) {
            if (!instance.authority.equalsIgnoreCase(authority)) {
                notAuthInstances.add(instance);
            }
        }
        for (SensitivityInstance authInstance : notAuthInstances) {
            instances.remove(authInstance);
        }
    }

}
