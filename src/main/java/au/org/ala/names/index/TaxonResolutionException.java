package au.org.ala.names.index;

/**
 * An exception that indicates a non-fudgeable error in taxon resolution.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class TaxonResolutionException extends IndexBuilderException {
    /** The offending taxon of resolution */
    private TaxonomicElement taxon;

    public TaxonResolutionException(TaxonomicElement taxon) {
        super();
        this.taxon = this.taxon;
    }

    public TaxonResolutionException(TaxonomicElement taxon, String message) {
        super(message);
        this.taxon = taxon;
    }

    public TaxonResolutionException(TaxonomicElement taxon, String message, Throwable cause) {
        super(message, cause);
        this.taxon = taxon;
    }

    public TaxonResolutionException(TaxonomicElement taxon, Throwable cause) {
        super(cause);
        this.taxon = taxon;
    }

    protected TaxonResolutionException(TaxonomicElement taxon, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.taxon = taxon;
    }

    public TaxonomicElement getTaxon() {
        return taxon;
    }
}
