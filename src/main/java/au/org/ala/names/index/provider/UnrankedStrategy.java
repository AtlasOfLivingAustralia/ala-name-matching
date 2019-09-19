package au.org.ala.names.index.provider;

import au.org.ala.names.index.TaxonConceptInstance;
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
    /** Reassign all unranked taxa. If there isn't a matching ranked taxon, then infer rank. */
    ALL_INFER(true, TaxonomicType.values()),
    /** Reassign all unranked taxa with a matching, ranked name */
    ALL(false, TaxonomicType.values()),
    /** Reassign any useful inferred taxa and synonyms. If there isn't a matching unranked taxon, the infer rank */
    INFERRED_AND_SYNONYMS_INFER(
            true,
            TaxonomicType.INFERRED_UNPLACED,
            TaxonomicType.INFERRED_SYNONYM,
            TaxonomicType.INFERRED_ACCEPTED,
            TaxonomicType.HOMOTYPIC_SYNONYM,
            TaxonomicType.SYNONYM,
            TaxonomicType.HETEROTYPIC_SYNONYM,
            TaxonomicType.OBJECTIVE_SYNONYM,
            TaxonomicType.PRO_PARTE_SYNONYM,
            TaxonomicType.SUBJECTIVE_SYNONYM),
    /** Reassign any useful inferred taxa and synonyms. */
    INFERRED_AND_SYNONYMS(
            false,
            TaxonomicType.INFERRED_UNPLACED,
            TaxonomicType.INFERRED_SYNONYM,
            TaxonomicType.INFERRED_ACCEPTED,
            TaxonomicType.HOMOTYPIC_SYNONYM,
            TaxonomicType.SYNONYM,
            TaxonomicType.HETEROTYPIC_SYNONYM,
            TaxonomicType.OBJECTIVE_SYNONYM,
            TaxonomicType.PRO_PARTE_SYNONYM,
            TaxonomicType.SUBJECTIVE_SYNONYM),
    /** Reassign all synonyms. If there isn't a matching ranked taxon, then infer rank. */
    SYNONYMS_INFER(
            true,
            TaxonomicType.HOMOTYPIC_SYNONYM,
            TaxonomicType.INFERRED_SYNONYM,
            TaxonomicType.SYNONYM,
            TaxonomicType.HETEROTYPIC_SYNONYM,
            TaxonomicType.OBJECTIVE_SYNONYM,
            TaxonomicType.PRO_PARTE_SYNONYM,
            TaxonomicType.SUBJECTIVE_SYNONYM),
    /** Reassign all synonyms with a matching, ranked name */
    SYNONYMS(
            false,
            TaxonomicType.HOMOTYPIC_SYNONYM,
            TaxonomicType.INFERRED_SYNONYM,
            TaxonomicType.SYNONYM,
            TaxonomicType.HETEROTYPIC_SYNONYM,
            TaxonomicType.OBJECTIVE_SYNONYM,
            TaxonomicType.PRO_PARTE_SYNONYM,
            TaxonomicType.SUBJECTIVE_SYNONYM),
    /** Reassign nothing */
    NONE(false);

    /** The taxonomic types to reassign */
    private Set<TaxonomicType> reassign;
    /** Infer rank, even if there is no reassignable rank */
    private boolean inferRank;

    /** Build for a specific set of taxonomic types */
    private UnrankedStrategy(boolean inferRank, TaxonomicType... types) {
        this.inferRank = inferRank;
        this.reassign = new HashSet<>(Arrays.asList(types));
    }

    /**
     * Infer a rank from the taxon, if not able to find a matching, ranked taxon
     *
     * @return True if rank is to be inferred
     */
    public boolean isInferRank() {
        return this.inferRank;
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
