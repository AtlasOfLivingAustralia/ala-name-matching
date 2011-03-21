/**
 *
 */
package au.org.ala.sds.model;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class ConservationInstance extends SensitivityInstance {

    private final String locationGeneralisation;

    /**
     * @param category
     * @param authority
     * @param fromDate
     * @param toDate
     * @param zone
     * @param generalisation
     */
    public ConservationInstance(
            SensitivityCategory category,
            String authority,
            SensitivityZone zone,
            String generalisation) {
        super(category, authority, zone);
        this.locationGeneralisation = generalisation;
    }

    public String getLocationGeneralisation() {
        return locationGeneralisation;
    }

}
