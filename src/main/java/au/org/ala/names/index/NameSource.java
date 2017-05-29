package au.org.ala.names.index;

import org.apache.commons.collections.MapUtils;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;

import java.util.*;

/**
 * An abstract source of names.
 * <p>
 * Subclasses can be used to load name information into a taxonomy.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
abstract public class NameSource {
    /** Fields expected in the DwCA */
    protected static final Set<Term> TAXON_REQUIRED = new HashSet<Term>(Arrays.asList(
            DwcTerm.taxonID,
            DwcTerm.nomenclaturalCode,
            DwcTerm.acceptedNameUsageID,
            DwcTerm.parentNameUsageID,
            DwcTerm.scientificName,
            DwcTerm.scientificNameAuthorship,
            DwcTerm.taxonomicStatus,
            DwcTerm.taxonRank
    ));
    /** Optional fields from the DwCA */
    protected static final Set<Term> ADDITIONAL_FIELDS = new HashSet<Term>(Arrays.asList(
            DwcTerm.taxonConceptID,
            DwcTerm.scientificNameID,
            DwcTerm.nomenclaturalStatus,
            DwcTerm.kingdom,
            DwcTerm.phylum,
            DwcTerm.class_,
            DwcTerm.order,
            DwcTerm.family,
            DwcTerm.genus,
            DwcTerm.specificEpithet,
            DwcTerm.infraspecificEpithet
    ));
    /** A map of row types and what is needed to ensure that row is useful */
    protected static final Map<String, Set<Term>> REQUIRED_TERMS = MapUtils.putAll(new HashMap<String, Set<Term>>(),
            new Object[][] {
                    { DwcTerm.Taxon.qualifiedName(), TAXON_REQUIRED }
            }
    );

    /**
     * Validate the source of information before loading
     *
     * @throws IndexBuilderException if the source is not valid
     */
    abstract public void validate() throws IndexBuilderException;

    /**
     * Load the information from this source into a taxonomy.
     *
     * @param taxonomy The taxonomy
     *
     * @throws IndexBuilderException if unable to load the data
     */
    abstract public void loadIntoTaxonomy(Taxonomy taxonomy) throws IndexBuilderException;
}
