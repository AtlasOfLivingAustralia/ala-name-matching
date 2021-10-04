package au.org.ala.names.index;

import javax.annotation.Nullable;
import java.util.List;

/**
 * An exception caused by something like a synonym loop
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class ResolutionException extends IndexBuilderException {
    private List<TaxonomicElement> trace;

    public ResolutionException(String message, @Nullable List<TaxonomicElement> trace) {
        super(message);
        this.trace = trace;
    }

    public ResolutionException(String message, Throwable cause, @Nullable List<TaxonomicElement> trace) {
        super(message, cause);
        this.trace = trace;
    }

    public ResolutionException(String message) {
        super(message);
    }

    public ResolutionException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public List<TaxonomicElement> getTrace() {
        return trace;
    }
}
