/**
 *
 */
package au.org.ala.sds.validation;

import au.org.ala.sds.util.GeneralisedLocation;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class ConservationOutcome extends ValidationOutcome {

    private GeneralisedLocation generalisedLocation;

    /**
     * @param report
     */
    public ConservationOutcome(ValidationReport report) {
        super(report);
    }

    public GeneralisedLocation getGeneralisedLocation() {
        return generalisedLocation;
    }

    public void setGeneralisedLocation(GeneralisedLocation generalisedLocation) {
        this.generalisedLocation = generalisedLocation;
    }

}
