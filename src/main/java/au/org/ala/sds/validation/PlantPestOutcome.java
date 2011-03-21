/**
 *
 */
package au.org.ala.sds.validation;


/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class PlantPestOutcome extends ValidationOutcome {

    private boolean loadable;

    /**
     * @param report
     */
    public PlantPestOutcome(ValidationReport report) {
        super(report);
    }

    public boolean isLoadable() {
        return loadable;
    }

    public void setLoadable(boolean loadable) {
        this.loadable = loadable;
    }


}
