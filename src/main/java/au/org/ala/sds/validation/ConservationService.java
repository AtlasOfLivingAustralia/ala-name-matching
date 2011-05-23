/**
 *
 */
package au.org.ala.sds.validation;

import java.util.List;

import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.model.SensitivityZone;
import au.org.ala.sds.util.GeneralisedLocation;
import au.org.ala.sds.util.GeneralisedLocationFactory;
import au.org.ala.sds.util.ValidationUtils;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class ConservationService implements ValidationService {

    private ReportFactory reportFactory;
    private final SensitiveTaxon taxon;

    public ConservationService(SensitiveTaxon taxon, ReportFactory reportFactory) {
        super();
        this.reportFactory = reportFactory;
        this.taxon = taxon;
    }

    /**
     * @param taxon
     * @param facts
     * @return
     */
    public ValidationOutcome validate(FactCollection facts) {
        ValidationReport report = reportFactory.createValidationReport(taxon);

        // Validate location
        if (!ValidationUtils.validateLocationCoords(facts, report)) {
            return new ValidationOutcome(report, false);
        }
        if (!ValidationUtils.validateLocation(facts, report)) {
            return new ValidationOutcome(report, false);
        }

        // Generalise location
        List<SensitivityZone> zones = SensitivityZone.getListFromString(facts.get(FactCollection.ZONES_KEY));
        String latitude = facts.get(FactCollection.DECIMAL_LATITUDE_KEY);
        String longitude = facts.get(FactCollection.DECIMAL_LONGITUDE_KEY);

        GeneralisedLocation gl = GeneralisedLocationFactory.getGeneralisedLocation(latitude, longitude, taxon, zones);

        ValidationOutcome outcome = new ConservationOutcome(report);
        ((ConservationOutcome) outcome).setGeneralisedLocation(gl);
        outcome.setValid(true);

        return outcome;
    }

    public void setReportFactory(ReportFactory reportFactory) {
        this.reportFactory = reportFactory;
    }

}
