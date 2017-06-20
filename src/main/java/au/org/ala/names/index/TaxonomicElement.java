package au.org.ala.names.index;

/**
 * Some sort of taxonomic element.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
abstract public class TaxonomicElement {
    /**
     * A label for the element for logging purposes
     *
     * @return A human readable label for loggging
     */
    abstract public String getLabel();
}
