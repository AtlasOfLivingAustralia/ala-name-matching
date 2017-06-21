package au.org.ala.names.index;

/**
 * Some sort of taxonomic element.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
abstract public class TaxonomicElement {
    /**
     * Validate this element.
     * <p>
     * Validation errors are reported to the taxonomy
     * </p>
     *
     * @param taxonomy The taxonomy to validate against and report to
     *
     * @return False if the element is not valid, true otherwise
     *
     */
    abstract public boolean validate(Taxonomy taxonomy);

    /**
     * An identifier for the element for logging purposes
     *
     * @return A human readable identifier for loggging
     */
    abstract public String getId();

    /**
     * The scientific name for the element for logging purposes
     *
     * @return A human readable name for loggging
     */
    abstract public String getScientificName();

    /**
     * The scientific name authorship for the element for logging purposes
     *
     * @return A human readable authorship for loggging
     */
    abstract public String getScientificNameAuthorship();
}
