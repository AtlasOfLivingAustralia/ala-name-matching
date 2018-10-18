package au.org.ala.names.index;

import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonomicType;
import au.org.ala.names.model.TaxonomicTypeGroup;
import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.nameparser.PhraseNameParser;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolve taxonomy according to ALA rules.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class ALATaxonResolver implements TaxonResolver {
    /** The parent taxonomy */
    private Taxonomy taxonomy;

    /**
     * Construct for a taxonomy
     *
     * @param taxonomy The taxonomy
     */
    public ALATaxonResolver(Taxonomy taxonomy) {
        this.taxonomy = taxonomy;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Principals are effectively chosen on the basis of the provider that provides the highest priority
     * for one of the instances.
     * </p>
     */
    @Override
    public List<TaxonConceptInstance> principals(TaxonConcept concept, Collection<TaxonConceptInstance> instances) throws IndexBuilderException {
        final int cutoff = taxonomy.getAcceptedCutoff();
        List<TaxonConceptInstance> principals = instances.stream().filter(tci -> tci.isPrimary() && tci.getScore() > cutoff).collect(Collectors.toList());
        if (principals.isEmpty()) {
           this.taxonomy.report(IssueType.NOTE, "taxonResolver.noPrincipals", concept, null);
            principals = new ArrayList<>(instances);
        }
        Optional<TaxonConceptInstance> max = principals.stream().max(TaxonConceptInstance.SCORE_COMPARATOR);
        Optional<NameProvider> provider = max.map(TaxonConceptInstance::getProvider);
        if (!provider.isPresent()) {
            this.taxonomy.report(IssueType.NOTE, "taxonResolver.noProvider", concept, null);
            max = instances.stream().max(TaxonConceptInstance.SCORE_COMPARATOR);
            provider = max.map(TaxonConceptInstance::getProvider);
            principals = new ArrayList<>(instances);
        }
        final NameProvider source = provider.orElse(taxonomy.getInferenceProvider());
        principals = principals.stream().filter(instance -> instance.getProvider() == source).collect(Collectors.toList());
        principals.sort(TaxonConceptInstance.INVERSE_SCORE_COMPARATOR);
        return principals;
    }

    /**
     * {@inheritDoc}
     *
     * @see #resolve(TaxonConceptInstance, TaxonResolution)
     */
    @Override
    public TaxonResolution resolve(TaxonConcept concept, List<TaxonConceptInstance> principals, Collection<TaxonConceptInstance> instances) throws IndexBuilderException {
        TaxonResolution resolution = new TaxonResolution(principals);
        for (TaxonConceptInstance instance: instances) {
            this.resolve(instance, resolution);
        }
        return resolution;
    }

    /**
     * Resolve an individual taxon.
     * <p>
     * Resolution occurs by the following mechanism:
     * </p>
     * <ul>
     *     <li>Principal instances map onto themselves</li>
     *     <li>For accepted taxa:
     *     <ol>
     *         <li>If there is a principal accepted taxon for the same taxon concept, then choose that one</li>
     *         <li>If there is a principal accepted taxon for the same scientific name, then choose that one</li>
     *         <li>If there are any principal accepted taxa then choose the lub (least upper bound) of those taxa</li>
     *         <li>If there are any principal synonym taxa then choose the lub of those taxa</li>
     *         <li>Otherwise, add this as a non-principal</li>
     *     </ol>
     *     </li>
     *     <li>For synonyms:
     *     <ol>
     *         <li>If there is a principal synonym taxon matching the same taxon concept with the same status, then choose that one</li>
     *         <li>If there is a principal synonym taxon matching the same taxon concept with the same status group, then choose that one</li>
     *         <li>If there is a principal synonym taxon matching the same scientific name with the same status, then choose that one</li>
     *         <li>If there is a principal synonym taxon matching the same scientific name with the same status group, then choose that one</li>
     *         <li>If there are any principal synonym taxa then choose the lub of those taxa</li>
     *         <li>Otherwise, add this as a non-principal</li>
     *     </ol>
     *     </li>
     *     <li>For non-accepted, non-synonym taxa with an accepted taxon
     *     <ol>
     *         <li>If there is a principal taxon matching the same taxon concept with the same status, then choose that one</li>
     *         <li>If there is a principal taxon matching the same scientific name with the same status, then choose that one</li>
     *         <li>Otherwise, add this as a non-principal</li>
     *     </ol>
     *     <li>For non-accepted, non-synonym taxa without an accepted taxon
     *     <ol>
     *         <li>If there is a principal taxon with the same status, then choose that one</li>
     *         <li>Otherwise, add this as a non-principal</li>
     *     </ol>
     *     </li>
     * </ul>
     * <p>
     *     Note that resoution can be non-deterministic if ther resolution starts with multiple
     *     principals with the same score. If that is the case, then the choice is abitrary.
     *     If it's not, then the scores should be adjusted.
     *     See {@link #principals(TaxonConcept, Collection)} for principal selection.
     * </p>
     *
     * @param instance The instance to resolve
     * @param resolution The current resolution
     *
     * @throws IndexBuilderException if unable to build a resolution
     */
    protected void resolve(TaxonConceptInstance instance, TaxonResolution resolution) throws IndexBuilderException {
        final TaxonomicType taxonomicStatus = instance.getTaxonomicStatus();
        final TaxonomicTypeGroup taxonomicGroup = taxonomicStatus.getGroup();
        final TaxonConcept taxonConcept = instance.getContainer();
        final ScientificName scientificName = taxonConcept == null ? null : taxonConcept.getContainer();
        final TaxonConceptInstance accepted = instance.getAccepted() == null ? null : instance.getAccepted().getRepresentative();
        final TaxonConcept acceptedTaxonConcept = accepted == null ? null : accepted.getContainer();
        final ScientificName acceptedScientificName = acceptedTaxonConcept == null ? null : acceptedTaxonConcept.getContainer();
        Optional<TaxonConceptInstance> resolved;

        if (resolution.getPrincipal().contains(instance)) {
            resolution.addInternal(instance, instance, this.taxonomy);
            return;
        }
        if (instance.isAccepted() && instance.isPrimary()) {
            if ((resolved = resolution.getUsed().stream().filter(tci -> tci.isAccepted() && tci.getContainer() == taxonConcept).findFirst()).isPresent()) {
                resolution.addInternal(instance, resolved.get(), this.taxonomy);
                return;
            }
            if ((resolved = resolution.getUsed().stream().filter(tci -> tci.isAccepted() && tci.getContainer().getContainer() == scientificName).findFirst()).isPresent()) {
                resolution.addInternal(instance, resolved.get(), this.taxonomy);
                return;
            }
            if ((resolved = resolution.getUsed().stream().filter(tci -> tci.isAccepted()).findFirst()).isPresent()) {
                resolution.addInternal(instance, resolved.get(), this.taxonomy);
                return;
            }
            List<TaxonConceptInstance> synonyms = resolution.getUsed().stream().filter(tci -> tci.isSynonym() && !tci.isForbidden()).collect(Collectors.toList());
            if (!synonyms.isEmpty()) {
                TaxonConceptInstance r = this.lub(synonyms);
                if (r != null) {
                    taxonomy.report(IssueType.NOTE, "taxonResolver.synonyms", instance, Arrays.asList(r));
                    resolution.addExternal(instance, r, taxonomy);
                    return;
                }
            }
            resolution.addInternal(instance, instance, taxonomy);
            return;
        } else if (instance.isSynonym() && instance.isPrimary() && accepted != null) {
            if ((resolved = resolution.getUsed().stream().filter(tci -> tci.getTaxonomicStatus() == taxonomicStatus && tci.getAccepted() != null && tci.getAccepted().getContainer() == acceptedTaxonConcept).findFirst()).isPresent()) {
                resolution.addInternal(instance, resolved.get(), this.taxonomy);
                return;
            }
            if ((resolved = resolution.getUsed().stream().filter(tci -> tci.getTaxonomicStatus().getGroup() == taxonomicGroup && tci.getAccepted() != null && tci.getAccepted().getContainer() == acceptedTaxonConcept).findFirst()).isPresent()) {
                resolution.addInternal(instance, resolved.get(), this.taxonomy);
                return;
            }
            if ((resolved = resolution.getUsed().stream().filter(tci -> tci.getTaxonomicStatus() == taxonomicStatus && tci.getAccepted() != null && tci.getAccepted().getContainer().getContainer() == acceptedScientificName).findFirst()).isPresent()) {
                resolution.addInternal(instance, resolved.get(), this.taxonomy);
                return;
            }
            if ((resolved = resolution.getUsed().stream().filter(tci -> tci.getTaxonomicStatus().getGroup() == taxonomicGroup && tci.getAccepted() != null && tci.getAccepted().getContainer().getContainer() == acceptedScientificName).findFirst()).isPresent()) {
                resolution.addInternal(instance, resolved.get(), this.taxonomy);
                return;
            }
            List<TaxonConceptInstance> synonyms = resolution.getUsed().stream().filter(tci -> tci.isSynonym() && !tci.isForbidden()).collect(Collectors.toList());
            if (!synonyms.isEmpty()) {
                TaxonConceptInstance r = synonyms.size() == 1 ? synonyms.get(0) : this.lub(synonyms);
                if (r != null) {
                    taxonomy.report(IssueType.NOTE, "taxonResolver.synonyms", instance, Arrays.asList(r));
                    resolution.addExternal(instance, r, taxonomy);
                    return;
                }
            }
            resolution.addInternal(instance, instance, taxonomy);
            return;
        } else if (instance.getAccepted() != null) {
            if ((resolved = resolution.getUsed().stream().filter(tci -> tci.getTaxonomicStatus() == taxonomicStatus && tci.getAccepted() != null && tci.getAccepted().getContainer() == acceptedTaxonConcept).findFirst()).isPresent()) {
                resolution.addInternal(instance, resolved.get(), this.taxonomy);
                return;
            }
            if ((resolved = resolution.getUsed().stream().filter(tci -> tci.getTaxonomicStatus() == taxonomicStatus && tci.getAccepted() != null && tci.getAccepted().getContainer().getContainer() == acceptedScientificName).findFirst()).isPresent()) {
                resolution.addInternal(instance, resolved.get(), this.taxonomy);
                return;
            }
            resolution.addInternal(instance, instance, taxonomy);
            return;
        } else {
            if ((resolved = resolution.getUsed().stream().filter(tci -> tci.getTaxonomicStatus() == taxonomicStatus).findFirst()).isPresent()) {
                resolution.addInternal(instance, resolved.get(), this.taxonomy);
                return;
            }
            resolution.addInternal(instance, instance, taxonomy);
            return;
        }
    }

    /**
     * Estimate the rank of an unranked instance.
     * <ol>
     *     <li>Try and find a taxon with the same name and author and a rank closest to any accepted rank.</li>
     *     <li>Parse the name to see if it gives any clues</li>
     *     <li>If there is an accepted rank, use the accepted rank</li>
     * </ol>
     *
     * @param instance The unranked instance
     * @param parent The parent unranked name, which should contain any found instances
     *
     * @return The
     *
     * @throws IndexBuilderException
     */
    @Override
    public RankType estimateRank(TaxonConceptInstance instance, UnrankedScientificName parent) throws IndexBuilderException {
        RankType acceptedRank = instance.getAccepted() != null ? instance.getAccepted().getRank() : null;
        TaxonConcept found = parent.findRankedConcept(instance.getContainer().getKey(), acceptedRank, this.taxonomy);
        if (found != null) {
            return found.getRank();
        } else if (instance.getProvider().getUnrankedStrategy().isInferRank()) {
            // Try name analysis to see whether we can get anywhere
            try {
                NameParser parser = new PhraseNameParser();
                ParsedName pn = parser.parse(instance.getScientificName(), acceptedRank == null ? null : acceptedRank.getCbRank());
                if (pn.getRank() != null)
                    return RankType.getForCBRank(pn.getRank());
            } catch (UnparsableException ex) {
            }
            return acceptedRank;
        }
        return null;
    }

    /**
     * Get the resolved least upper bound (lub) of two instances.
     * <p>
     * Note that this has to work with unresolved instances.
     * </p>
     *
     * @param i1 The first instance
     * @param i2 The second instance
     *
     * @return The lowest common resolved taxon or null for not resolved or no common taxon
     */
    public TaxonConceptInstance lub(TaxonConceptInstance i1, TaxonConceptInstance i2) {
        while (i1 != null) {
            TaxonConceptInstance r1 = i1.getAccepted() == null ? i1 : i1.getAccepted().getRepresentative();
            TaxonConceptInstance p2 = i2;
            while (p2 != null) {
                p2 = p2.getAccepted() == null ? p2 : p2.getAccepted().getRepresentative();
                if (p2.getContainer() == r1.getContainer())
                    return r1;
                p2 = p2.getParent() == null ? null : p2.getParent().getRepresentative();
            }
            i1 = i1.getParent() == null ? null : i1.getParent().getRepresentative();
        }
        return null;
    }

    /**
     * Compute the least upper bound of a collection of taxa.
     *
     * @param instances The collection
     *
     * @return The least upper bound, or null if the instances have not been resolved or if there is no lub
     *
     * @see #lub(TaxonConceptInstance, TaxonConceptInstance)
     */
    public TaxonConceptInstance lub(Collection<TaxonConceptInstance> instances) {
        Iterator<TaxonConceptInstance> i = instances.iterator();
        TaxonConceptInstance lub = i.hasNext() ? i.next() : null;
        while (i.hasNext() && lub != null)
            lub = this.lub(lub, i.next());
        return lub;
    }


}
