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
    private boolean controlledAccess=false;
    private ValidationReport report;
    private Map<String, Object> result;

    public ValidationOutcome(){
        super();
        this.valid = true;
    }

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

    public void setControlledAccess(boolean controlledAccess){
        this.controlledAccess = controlledAccess;
    }

    public boolean isControlledAccess(){
        return this.controlledAccess;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "ValidationOutcome{" +
                "valid=" + valid +
                ", sensitive=" + sensitive +
                ", loadable=" + loadable +
                ", report=" + report +
                ", result=" + result +
                '}';
    }
}
