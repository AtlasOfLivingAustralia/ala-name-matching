package au.org.ala.names.index;

import java.util.List;

/**
 * An exception caused by something like a synonym loop
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class ResolutionException extends IndexBuilderException {
    private List<TaxonomicElement> trace;

    public ResolutionException(String message, List<TaxonomicElement> trace) {
        super(message);
        this.trace = trace;
    }

    public ResolutionException(String message, Throwable cause, List<TaxonomicElement> trace) {
        super(message, cause);
        this.trace = trace;
    }

    public ResolutionException(String message) {
        this(message, (List<TaxonomicElement>) null);
    }

    public ResolutionException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public List<TaxonomicElement> getTrace() {
        return trace;
    }
}
