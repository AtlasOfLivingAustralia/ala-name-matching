/**
 *
 */
package au.org.ala.sds.validation;

import java.util.Set;

import org.apache.commons.lang.StringUtils;

import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.model.SensitivityZone;
import au.org.ala.sds.util.GeneralisedLocation;
import au.org.ala.sds.util.GeneralisedLocationFactory;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class ConservationService implements ValidationService {

    private ReportFactory reportFactory;

    public ConservationService(ReportFactory reportFactory) {
        super();
        this.reportFactory = reportFactory;
    }

    /**
     * @param taxon
     * @param facts
     * @return
     */
    public ValidationOutcome validate(SensitiveTaxon taxon, FactCollection facts) {
        ValidationReport report = reportFactory.createValidationReport(taxon);

        if (!ValidationUtils.validateFacts(facts, report)) {
            return new ValidationOutcome(report, false);
        }

        String latitude = facts.get(FactCollection.LATITUDE_KEY);
        String longitude = facts.get(FactCollection.LONGITUDE_KEY);
        Set<SensitivityZone> zones = SensitivityZone.getSetFromString(facts.get(FactCollection.ZONES_KEY));

        if (StringUtils.isBlank(latitude) || StringUtils.isBlank(longitude)) {
            addInfoMessage(report, taxon);
            return new ConservationOutcome(report);
        }

        GeneralisedLocation gl = GeneralisedLocationFactory.getGeneralisedLocation(latitude, longitude, taxon, zones);

        ValidationOutcome outcome = new ConservationOutcome(report);
        ((ConservationOutcome) outcome).setGeneralisedLocation(gl);
        outcome.setValid(true);

        return outcome;
    }

    public void setReportFactory(ReportFactory reportFactory) {
        this.reportFactory = reportFactory;
    }

    private void addInfoMessage(ValidationReport report, SensitiveTaxon ss) {
        // TODO Auto-generated method stub

    }
}
