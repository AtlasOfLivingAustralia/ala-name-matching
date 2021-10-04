package au.org.ala.names.index;

/**
 * An exception caused by building an index.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2016 CSIRO
 */
public class IndexBuilderException extends RuntimeException {
    public IndexBuilderException() {
        super();
    }

    public IndexBuilderException(String message) {
        super(message);
    }

    public IndexBuilderException(String message, Throwable cause) {
        super(message, cause);
    }

    public IndexBuilderException(Throwable cause) {
        super(cause);
    }

    protected IndexBuilderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
