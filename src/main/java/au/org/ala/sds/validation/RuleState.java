/**
 *
 */
package au.org.ala.sds.validation;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class RuleState {

    private boolean complete;
    private boolean loadable;
    private String delegateRules;
    private String annotation;

    public RuleState() {
        super();
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public boolean isLoadable() {
        return loadable;
    }

    public void setLoadable(boolean loadable) {
        this.loadable = loadable;
    }

    public String getDelegateRules() {
        return delegateRules;
    }

    public void setDelegateRules(String delegateRules) {
        this.delegateRules = delegateRules;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }
}
