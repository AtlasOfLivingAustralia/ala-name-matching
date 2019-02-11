package au.org.ala.names.index;


import au.org.ala.names.model.RankType;
import org.gbif.api.vocabulary.NomenclaturalCode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A name or some sort.
 * <p>
 * Names contain a set of "concepts" that can be either more specific names or taxon concepts.
 * </p>
 *
 * @param <T> The type of element
 * @param <C> The type of the containing element
 * @param <E> The type of the concept element
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2017 CSIRO
 */
abstract public class Name<T extends TaxonomicElement, C extends TaxonomicElement, E extends TaxonomicElement> extends TaxonomicElement<T, C> {
    /** The name key */
    private NameKey key;
    /** The list of concepts contained by the name */
    private List<E> concepts;
    /** The keyed map of concepts */
    private Map<NameKey, E> conceptMap;
    /** The principal concept that can be used when there is no more distinguishing information about a name */
    private E principal;
    /** Has this name been cleared of concepts? */
    private boolean cleared;

    /**
     * Construct for a key and name
     *
     * @param key The name name key
     */
    public Name(NameKey key) {
        this(null, key);
    }


    /**
     * Construct for a key and container
     *
     * @param container conytainer
     * @param key The name name key
     */
    public Name(C container, NameKey key) {
        super(container);
        this.key = key;
        this.concepts = new ArrayList<>();
        this.conceptMap = new HashMap<>();
        this.principal = null;
        this.cleared = false;
    }

    /**
     * Get the key associated with this name
     *
     * @return The name key
     */
    public NameKey getKey() {
        return key;
    }

    /**
     * Get the concept map for this name.
     *
     * @return The concept map
     */
    public List<E> getConcepts() {
        return concepts;
    }

    /**
     * Get the principal concept.
     * <p>
     * The principal concept is the element chosen to be representative of this name if there is no
     * additional disambiguating information.
     * This is null until set during resolution.
     * </p>
     *
     * @return The principal concept
     */
    public E getPrincipal() {
        return principal;
    }

    /**
     * Has this name been cleared of concepts?
     *
     * @return True if the name has been emptied
     */
    public boolean isCleared() {
        return cleared;
    }

    /**
     * Get the rank associated with this element
     *
     * @return The associated rank
     */
    @Override
    public RankType getRank() {
        return this.key.getRank();
    }

    /**
     * Get the score associated with the principal.
     *
     * @return The score of the principal element
     */
    @Override
    public int getPrincipalScore() {
        return this.principal != null ? this.principal.getPrincipalScore() : TaxonomicElement.MIN_SCORE;
    }

    /**
     * Get the provider associated with the principal.
     *
     * @return The score of the principal element
     */
    @Override
    public int getProviderScore() {
        return this.principal != null ? this.principal.getProviderScore() : TaxonomicElement.MIN_SCORE;
    }

    /**
     * Create a new concept for this instance key.
     *
     * @param stageKey The key for the stage. See {@link #buildStageKey(NameKey)}
     *
     * @return The new concept
     */
    abstract E createConcept(NameKey stageKey);

    /**
     * Remove a list of concepts from the name.
     *
     * @param remove The concepts to remove
     */
    public void removeConcepts(Collection<E> remove) {
        if (remove.isEmpty())
            return;
        this.concepts.removeAll(remove);
        Set<NameKey> keys = this.conceptMap.entrySet().stream().filter(e -> remove.contains(e.getValue())).map(Map.Entry::getKey).collect(Collectors.toSet());
        keys.forEach(k -> this.conceptMap.remove(k));
        if (remove.contains(this.principal))
            this.principal = null;
    }

    /**
     * Remove everything from this name and redirect principal enquiries to the new principal.
     *
     * @param principal The new principal
     */
    public void clear(E principal) {
        this.concepts.clear();
        this.conceptMap.clear();
        this.principal = principal;
        this.cleared = true;
    }

    /**
     * Create a key for this stage.
     * <p>
     * A stage is a point in attempting to match a taxon concept, eg. without author, without rank, without nomenclatural code, ...
     * The stange key is a name key with the unavailble information stripped out.
     * </p>
     *
     * @param instanceKey
     *
     * @return A key that corresponds to the keys used by the concept map
     */
    abstract NameKey buildStageKey(NameKey instanceKey);

    /**
     * Add an instance to the list of concepts.
     * <p>
     * If this is an uncoded instance, we attempt to add it to the most likely concept
     * </p>
     *
     * @param instanceKey The name key for this instance
     * @param instance The instance
     *
     * @return The matched taxon concept
     */
    public E addInstance(NameKey instanceKey, TaxonConceptInstance instance) {
        NameKey stageKey = this.buildStageKey(instanceKey);
        E concept = this.conceptMap.get(stageKey);
        if (concept == null) {
            concept = this.createConcept(stageKey);
            this.concepts.add(concept);
            this.conceptMap.put(stageKey, concept);
        }
        concept.addInstance(instanceKey, instance);
        return concept;
    }


    /**
     * Validate this name.
     *
     * @param taxonomy The taxonomy to validate against and report to
     *
     * @return True if the scientific name is valid
     */
    @Override
    public boolean validate(Taxonomy taxonomy) {
        boolean valid = true;
        if (!this.cleared && this.concepts.isEmpty()) {
            taxonomy.report(IssueType.VALIDATION, "scientificName.validation.noConcepts", this, null);
            valid = false;
        }
        for (E concept: this.concepts) {
            if (concept.getContainer() != this) {
                taxonomy.report(IssueType.VALIDATION, "scientificName.validation.conceptParent", concept, Arrays.asList(this));
                valid = false;
            }
            valid = concept.validate(taxonomy) && valid;
        }
        return valid;
    }


    /**
     * Work out what concept to use as the 'principal' concept for this name
     * and reallocate dangling concepts to that principal.
     *
     * @param taxonomy The resolving taxonomy
     */
    public void resolvePrincipal(Taxonomy taxonomy) {
        this.principal = this.findPrincipal(taxonomy);
        if (this.principal == null)
            taxonomy.report(IssueType.PROBLEM, "name." + this.getClass().getSimpleName() + ".noPrincipal", this, null);
        else if (this.concepts.size() > 1)
            taxonomy.report(IssueType.NOTE, "name." + this.getClass().getSimpleName() + ".principal", this, Arrays.asList(principal));
        taxonomy.count("count.name." + this.getClass().getSimpleName() + ".principal");
        this.reallocateDanglingConcepts(taxonomy, principal);
    }

    /**
     * Reallocate any concepts that are not complete to the principal concept.
     * <p>
     * Generally, this means that concepts that came in with inadequate information
     * For example, unranked concepts may be placed in an existing ranked concept
     * </p>
     *
     * @param taxonomy
     * @param principal
     */
    protected abstract void reallocateDanglingConcepts(Taxonomy taxonomy, E principal);

    /**
     * Choose an element to act as the principal element for this name.
     * <p>
     * This method assumes that the contained concepts have already been resolved.
     * </p>
     *
     * @param taxonomy The resolving taxonomy.
     *
     * @return The principal element
     */
    protected abstract E findPrincipal(Taxonomy taxonomy);


    /**
     * {@inheritDoc}
     */
    @Override
    public String getTaxonID() {
        return this.getPrincipal() == null ? null : this.getPrincipal().getTaxonID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        if (this.getScientificNameAuthorship() == null)
            return this.key.getScientificName();
        return this.key.getScientificName() + " " + this.key.getScientificNameAuthorship();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScientificName() {
        return this.key.getScientificName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScientificNameAuthorship() {
        return this.key.getScientificNameAuthorship();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNameComplete() {
        TaxonConceptInstance representative = this.getRepresentative();
        return representative != null ? representative.getNameComplete() : null;
    }

    /**
     * Get the representative instance.
     *
     * @return The principals instance or null for none
     */
    @Override
    public TaxonConceptInstance getRepresentative() {
        return this.principal != null ? this.principal.getRepresentative() : null;
    }

}
