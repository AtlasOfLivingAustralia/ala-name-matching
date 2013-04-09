/**
 *
 */
package au.org.ala.sds.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import au.org.ala.sds.util.DateHelper;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class PlantPestInstance extends SensitivityInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Date fromDate;
    private final Date toDate;
    private java.util.List<TransientEvent> transientEventList;

    /**
     * @param category
     * @param authority
     * @param fromDate
     * @param toDate
     * @param zone
     */
    public PlantPestInstance(
            SensitivityCategory category,
            String authority,
            String dataResourceId,
            SensitivityZone zone,
            String reason,
            String remarks,
            String fromDate,
            String toDate) {
        super(category, authority, dataResourceId, zone, reason, remarks);
        this.fromDate = StringUtils.isNotEmpty(fromDate) ? DateHelper.parseDate(fromDate) : null;
        this.toDate = StringUtils.isNotEmpty(toDate) ? DateHelper.parseDate(toDate) : null;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void addTransientEvent(String eventDate, SensitivityZone zone){
        if(transientEventList == null)
            transientEventList = new ArrayList<TransientEvent>();
        transientEventList.add(new TransientEvent(eventDate, zone));
    }

    public java.util.List<TransientEvent> getTransientEventList(){
        return transientEventList;
    }

    public class TransientEvent implements Serializable{
        private final Date eventDate;
        private final SensitivityZone zone;
        public TransientEvent(String eventDate, SensitivityZone zone){
            this.eventDate = StringUtils.isNotEmpty(eventDate) ? DateHelper.parseDate(eventDate) : null;
            this.zone = zone;
        }

        public Date getEventDate() {
            return eventDate;
        }


        public SensitivityZone getZone() {
            return zone;
        }

        @Override
        public String toString() {
            return "TransientEvent{" +
                    "eventDate=" + eventDate +
                    ", zone=" + zone +
                    '}';
        }
    }

}
