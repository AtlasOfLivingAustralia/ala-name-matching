/**
 *
 */
package au.org.ala.sds.validation;

import java.util.List;

import au.org.ala.sds.model.Message;
import au.org.ala.sds.model.SdsMessage;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.model.SensitivityInstance;
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

        // Add sensitivity information message
        StringBuilder message = new StringBuilder("Sensitive in ");
        for (SensitivityInstance si : taxon.getInstances()) {
            message.append(si.getZone().getName() + " [" + si.getAuthority() + "], ");
        }
        message.replace(message.length() - 2, message.length(), "");
        report.addMessage(new SdsMessage(Message.Type.INFO, message.toString()));

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

        // Add generalisation message
        report.addMessage(new SdsMessage(Message.Type.INFO, gl.getDescription() + " [" + gl.getGeneralisedLatitude() + "," + gl.getGeneralisedLongitude() + "]"));
        ((ConservationOutcome) outcome).setGeneralisedLocation(gl);
        outcome.setValid(true);

        return outcome;
    }

    public void setReportFactory(ReportFactory reportFactory) {
        this.reportFactory = reportFactory;
    }

}
