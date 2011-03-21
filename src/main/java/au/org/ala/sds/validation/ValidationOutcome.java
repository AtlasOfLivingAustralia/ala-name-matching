/**
 *
 */
package au.org.ala.sds.validation;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class ValidationOutcome {

    private boolean valid;
    private ValidationReport report;
    private String annotation;

    public ValidationOutcome(ValidationReport report) {
        super();
        this.report = report;
        this.valid = true;
    }

    public ValidationOutcome(ValidationReport report, boolean valid) {
        super();
        this.report = report;
        this.valid = valid;
    }

    public ValidationReport getReport() {
        return report;
    }

    public void setReport(ValidationReport report) {
        this.report = report;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}
