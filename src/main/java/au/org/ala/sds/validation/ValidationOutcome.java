/**
 *
 */
package au.org.ala.sds.validation;

import java.util.Map;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class ValidationOutcome {

    private boolean valid;
    private boolean sensitive = false;
    private boolean loadable = false;
    private ValidationReport report;
    private Map<String, Object> result;

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

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }

    public boolean isLoadable() {
        return loadable;
    }

    public void setLoadable(boolean loadable) {
        this.loadable = loadable;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }
}
