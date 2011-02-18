package au.org.ala.sds.model;

import java.util.Date;

import au.org.ala.sds.util.DateHelper;

/**
*
* @author Peter Flemming (peter.flemming@csiro.au)
*/
public class SensitivityInstance {

    private SensitivityCategory category;
    private String authority;
    private SensitivityZone zone;
    private Date fromDate;
    private Date toDate;
    
    public SensitivityInstance(SensitivityCategory category, String authority, String fromDate, String toDate, SensitivityZone zone) {
        this.category = category;
        this.authority = authority;
        this.fromDate = fromDate == null ? null : DateHelper.parseDate(fromDate);
        this.toDate = toDate == null ? null : DateHelper.parseDate(toDate);
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

    public void setZone(SensitivityZone zone) {
        this.zone = zone;
    }

     public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

}
