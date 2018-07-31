package au.org.ala.names.index;

import au.org.ala.names.model.RankType;

import java.util.Collection;
import java.util.List;

/**
 * Something that can construct a taxon resolution out of a bunch of taxon instances.
 * <p>
 * Resolvers are generally singleton instances that are attached to a {@link Taxonomy} so
 * beware of including state.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
abstract public interface TaxonResolver {
    /**
     * Compute a list of principal instances.
     * <p>
     * The principal instances are the "preferred" list of instances that
     * can be used to decide which of several instances the taxonomy is going to use.
     * </p>
     *
     * @param concept The parent taxon concept
     * @param instances The complete set of instances
     *
     * @return The instance list, ordered with the highest score first
     *
     * @throws IndexBuilderException if unable to collect principals
     */
    public List<TaxonConceptInstance> principals(TaxonConcept concept, Collection<TaxonConceptInstance> instances) throws IndexBuilderException;

    /**
     * Resolve a set of instances against a list of principals.
     *
     * @param concept The parent taxon concept
     * @param principals The list of principals.
     * @param instances The instances
     *
     * @return A complete resolution of the instances against the principals
     *
     * @throws IndexBuilderException If unable to resolve things.
     */
    public TaxonResolution resolve(TaxonConcept concept, List<TaxonConceptInstance> principals, Collection<TaxonConceptInstance> instances) throws IndexBuilderException;

    /**
     * Estimate the rank of an unranked instance.
     * <p>
     * This follows whatever heuristics can be used to locate a rank.
     * </p>
     *
     * @param instance The unranked instance
     * @param parent The parent unranked name, which should contain any found instances
     *
     * @return The estimated rank, or null for unable to estimate
     *
     * @throws IndexBuilderException
     */
    public RankType estimateRank(TaxonConceptInstance instance, UnrankedScientificName parent) throws IndexBuilderException;
}
