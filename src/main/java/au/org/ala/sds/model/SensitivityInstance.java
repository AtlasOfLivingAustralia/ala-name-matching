package au.org.ala.sds.model;

import java.util.Date;

import au.org.ala.sds.util.DateHelper;

/**
*
* @author Peter Flemming (peter.flemming@csiro.au)
*/
public class SensitivityInstance {

    private final SensitivityCategory category;
    private final String authority;
    private final SensitivityZone zone;
    private final Date fromDate;
    private final Date toDate;
    private final String locationGeneralisation;

    public SensitivityInstance(SensitivityCategory category, String authority, String fromDate, String toDate, SensitivityZone zone, String generalisation) {
        this.category = category;
        this.authority = authority;
        this.fromDate = fromDate == null ? null : DateHelper.parseDate(fromDate);
        this.toDate = toDate == null ? null : DateHelper.parseDate(toDate);
        this.zone = zone;
        this.locationGeneralisation = generalisation;
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

     public Date getToDate() {
        return toDate;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public String getLocationGeneralisation() {
        return locationGeneralisation;
    }

}
