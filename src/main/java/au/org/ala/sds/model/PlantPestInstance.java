/**
 *
 */
package au.org.ala.sds.model;

import java.util.Date;

import au.org.ala.sds.util.DateHelper;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class PlantPestInstance extends SensitivityInstance {

    private final Date fromDate;
    private final Date toDate;

    /**
     * @param category
     * @param authority
     * @param fromDate
     * @param toDate
     * @param zone
     * @param generalisation
     */
    public PlantPestInstance(
            SensitivityCategory category,
            String authority,
            SensitivityZone zone,
            String fromDate,
            String toDate) {
        super(category, authority, zone);
        this.fromDate = fromDate == null || fromDate.equals("") ? null : DateHelper.parseDate(fromDate);
        this.toDate = toDate == null || toDate.equals("") ? null : DateHelper.parseDate(toDate);
    }

    public Date getFromDate() {
        return fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

}
