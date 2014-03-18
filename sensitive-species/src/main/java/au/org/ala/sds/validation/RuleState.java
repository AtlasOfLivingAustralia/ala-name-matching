/**
 *
 */
package au.org.ala.sds.validation;

import java.text.MessageFormat;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class RuleState {

    private boolean complete;
    private boolean loadable;
    /** Indicates that the properties of the supplied record should be restricted to taxonomic and institution information */
    private boolean restricted=false;
    /** Indicates that the supplied record is fully viewable under controlled access **/
    private boolean controlledAccess=false;
    private String delegateRules;
    /** NC 2013-04-29: Is this the annotation to place against the record?? **/
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

    public void setAnnotation(String annotation, Object... context) {
        this.annotation = MessageFormat.format(annotation, context);
    }

    public boolean isRestricted() {
        return restricted;
    }

    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }
    public void setControlledAccess(boolean controlledAccess){
        this.controlledAccess = controlledAccess;
    }

    public boolean isControlledAccess(){
        return this.controlledAccess;
    }
}
