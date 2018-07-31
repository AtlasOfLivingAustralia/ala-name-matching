package au.org.ala.names.index;

import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonomicType;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Ways of handling unranked taxa
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2018 Atlas of Living Australia
 */
public enum UnrankedStrategy {
    ALL(TaxonomicType.values()),
    SYNONYMS(
            TaxonomicType.HOMOTYPIC_SYNONYM,
            TaxonomicType.INFERRED_SYNONYM,
            TaxonomicType.SYNONYM,
            TaxonomicType.HETEROTYPIC_SYNONYM,
            TaxonomicType.OBJECTIVE_SYNONYM,
            TaxonomicType.PRO_PARTE_SYNONYM,
            TaxonomicType.SUBJECTIVE_SYNONYM),
    NONE();

    /** The taxonomic types to reassign */
    private Set<TaxonomicType> reassign;

    /** Build for a specific set of taxonomic types */
    private UnrankedStrategy(TaxonomicType... types) {
        this.reassign = new HashSet<>(Arrays.asList(types));
    }

    /**
     * Is this taxon concept instance reassignable?
     *
     * @param tci The taxon concept instance to check
     *
     * @return True if the instance should be reassigned, if possible.
     */
    public boolean isReassignable(TaxonConceptInstance tci) {
        return !tci.isForbidden() && (tci.getRank() == null || tci.getRank() == RankType.UNRANKED) && this.reassign.contains(tci.getTaxonomicStatus());
    }
}
