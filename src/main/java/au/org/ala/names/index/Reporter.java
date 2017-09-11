package au.org.ala.names.index;

/**
 * Report errors and issues in the taxonomy.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public interface Reporter {
    /**
     * Add a report.
     * <p>
     * Message codes are retrieved using a message bundle pointing to <code>taxonomy.properties</code>
     * </p>
     *
     * @param type The type of report
     * @param code The message code to use for the readable version of the report
     * @param args The arguments for the report message
     */
    void report(IssueType type, String code, String... args);

    /**
     * Add a report.
     * <p>
     * Message codes are retrieved using a message bundle pointing to <code>taxonomy.properties</code>
     * These are formatted with a message formatter and have the following arguments:
     * </p>
     * <ul>
     *     <li>{0} The taxonID of the source element, either a name or a proper taxonID</li>
     *     <li>{1} The scientific name of the source element</li>
     *     <li>{2} The scientific name authorship of the source element</li>
     *     <li>{3} Any associated taxon identifiers</li>
     * </ul>
     *
     * @param type The type of report
     * @param code The message code to use for the readable version of the report
     * @param elements The elements that impact the report. The first element is the source (causative) element
     */
    void report(IssueType type, String code, TaxonomicElement... elements);
}
