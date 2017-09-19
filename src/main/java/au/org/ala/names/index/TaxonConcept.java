package au.org.ala.names.index;

import au.org.ala.vocab.ALATerm;
import au.org.ala.names.model.RankType;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import au.org.ala.names.util.DwcaWriter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A taxonomic concept. A scientific name, author and placement in
 * <p>
 * A taxon concept has a <em>resolved</em> instance, which is the particular taxonomic concept that
 * has been chosen as most representative of the concept.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2017 CSIRO
 */

public class TaxonConcept extends TaxonomicElement<TaxonConcept, ScientificName> {
    /** The name key for this concept */
    private NameKey key;
    /** The list of instances that correspond to this concept */
    private List<TaxonConceptInstance> instances;
    /** The resolution for this taxon concept */
    private TaxonResolution resolution;

    /**
     * Construct for new scientific name and a name key
     *
     * @param name The parent scientific name
     * @param key The name key
     */
    public TaxonConcept(ScientificName name, NameKey key) {
        super(name);
        this.key = key;
        this.instances = new ArrayList<>();
        this.resolution = null;
    }

    /**
     * Get the name key for this concept
     *
     * @return A code/name/author key
     */
    public NameKey getKey() {
        return key;
    }

    /**
     * Has this concept been resolved.
     *
     * @return True if there is a resolved instance attached to the concept.
     */
    public boolean isResolved() {
        return this.resolution != null;
    }

    /**
     * Get a resolved instance.
     *
     * @param instance The instance to resolve
     *
     * @return The resolved instance
     *
     * @throws IllegalStateException if unable to resolve the instance
     */
    public TaxonConceptInstance getResolved(TaxonConceptInstance instance) {
        if (this.resolution == null)
            return instance;
        TaxonConceptInstance resolved = this.resolution.getResolved(instance);
        if (resolved == null)
            throw new IllegalStateException("Unable to get resolution for " + this);
        return resolved;
    }

    /**
     * Add an instance to the taxon concept.
     *
     * @param instance The instance
     */
    @Override
    public TaxonConceptInstance addInstance(NameKey instanceKey, TaxonConceptInstance instance) {
        instance.setContainer(this);
        this.instances.add(instance);
        return instance;
    }

    /**
     * See if we have an instance matching a particular provider.
     * <p>
     * First
     * </p>
     *
     * @param provider The provider
     * @param acceptedOnly Only accepted instances
     *
     * @return The matching instance or null for not found
     */
    public TaxonConceptInstance findInstance(NameProvider provider, boolean acceptedOnly) {
        for (TaxonConceptInstance instance: this.instances)
            if (instance.getProvider().equals(provider) && (!acceptedOnly || instance.isAccepted()))
                return instance;
        return null;
    }

    /**
     * Choose the preferred taxon instances for this concept.
     * <p>
     * This assumes that {@link TaxonConceptInstance#resolveLinks(Taxonomy)} has already been called on the contained instances.
     * </p>
     *
     * @see TaxonResolver
     *
     * @param taxonomy The source taxonomy
     *
     * @throws IndexBuilderException if unable to resolve preferences
     */
    public void resolveTaxon(Taxonomy taxonomy) throws IndexBuilderException {
        if (this.resolution != null)
            return;
        TaxonResolver resolver = taxonomy.getResolver();
        List<TaxonConceptInstance> principals = resolver.principals(this, this.instances);
        this.resolution = resolver.resolve(this, principals, this.instances);
        Map<TaxonConceptInstance, TaxonConceptInstance> resolution = new HashMap<>(this.instances.size());
        taxonomy.count("count.resolve.taxonConcept");
    }

    /**
     * Reallocate the elements of another taxon concept to this taxon concept.
     *
     * @param element The element to reallocate
     * @param taxonomy The resolving taxonomy
     */
    @Override
    public void reallocate(TaxonConcept element, Taxonomy taxonomy) {
        taxonomy.report(IssueType.NOTE, "taxonConcept.reallocated", element, this);
        taxonomy.count("count.reallocate.taxonConcept");
        TaxonConceptInstance representative = this.getRepresentative();
        if (representative == null || this.resolution == null)
            throw new IndexBuilderException("Unable to reallocate " + element + " to " + this + " without representative or resolution");
        element.resolution = new TaxonResolution();
        for (TaxonConceptInstance tci: element.instances) {
            tci.setContainer(this);
            this.instances.add(tci);
            this.resolution.addExternal(tci, representative, taxonomy);
            element.resolution.addExternal(tci, representative, taxonomy);
        }
        element.instances.clear();
    }

    /**
     * Write this taxon concept to a dwca writer.
     * <p>
     * Write out the primary taxa, then write out the additional taxa as instances.
     * </p>
     *
     * @param taxonomy
     *
     * @throws IOException if unable to write
     */
    public void write(Taxonomy taxonomy, DwcaWriter writer) throws IOException {
        for (TaxonConceptInstance tci : this.resolution.getUsed()) {
            if (tci.isForbidden())
                continue;
            Collection<TaxonConceptInstance> allocated = this.resolution.getChildren(tci);
            if (allocated == null || allocated.isEmpty()) {
                taxonomy.report(IssueType.NOTE, "taxonConcept.noInstances", tci);
            } else {
                writer.newRecord(tci.getTaxonID());
                Map<Term, String> values = tci.getTaxonMap(taxonomy);
                for (Term term : taxonomy.outputTerms(DwcTerm.Taxon))
                    if (term != DwcTerm.taxonID) // Already added as coreId
                        writer.addCoreColumn(term, values.get(term));
                for (TaxonConceptInstance sub : allocated) {
                    if (sub.isForbidden())
                        continue;
                    this.writeExtension(ALATerm.TaxonVariant, sub == tci ? values : sub.getTaxonMap(taxonomy), taxonomy, writer);
                    for (Map<Term, String> id : sub.getIdentifierMaps(taxonomy))
                        this.writeExtension(GbifTerm.Identifier, id, taxonomy, writer);
                    for (Map<Term, String> vn : sub.getVernacularMaps(taxonomy))
                        this.writeExtension(GbifTerm.VernacularName, vn, taxonomy, writer);
                    for (Map<Term, String> dist : sub.getDistributionMaps(taxonomy))
                        this.writeExtension(GbifTerm.Distribution, dist, taxonomy, writer);
                }
            }
        }
        taxonomy.count("count.write.taxonConcept");
    }

    private void writeExtension(Term type, Map<Term, String> values, Taxonomy taxonomy, DwcaWriter writer) throws IOException {
        Map<Term, String> ext = new LinkedHashMap<>(); // Preserve term order
        List<Term> terms = taxonomy.outputTerms(type);
        for (Term term: terms) {
            String value = values.get(term);
            ext.put(term, value);
        }
        if (!ext.isEmpty())
            writer.addExtensionRecord(type, ext);
    }

    /**
     * Get the rank of the taxonomic concept.
     *
     * @return The rank of the resolved element, or the rank of the key for not present
     */
    @Override
    public RankType getRank() {
        return this.resolution == null ? this.key.getRank() : this.resolution.getRank();
    }

    /**
     * Does this concept have an primary, accepted instance?
     * <p>
     * If we haven't resolved the instances, any accepted, non-forbidden instance will do
     * </p>
     *
     * @return True if there is one accepted instance in the primary list
     */
    public boolean hasAccepted() {
        return this.resolution != null ? this.resolution.hasAccepted() : this.instances.stream().anyMatch(tci -> tci.isAccepted() && !tci.isForbidden());
    }

    /**
     * Does this concept represent a formal scientific name, suitable for being used
     * as a primary taxon.
     *
     * @return True if this is a formal concept
     */
    public boolean isFormal() {
        return this.key.isFormal();
    }


    /**
     * Is this an authored taxon concept (ie. a proper taxon concept rather than just a name)?
     *
     * @return True if this taxon concept has an author
     */
    public boolean isAuthored() {
        return this.key.getScientificNameAuthorship() != null;
    }

    /**
     * Link this taxon concept to a principal taxon concept.
     * <p>
     * We generate inferred synonyms towards that concept.
     * If this concept is owned, then we ignore this, as we assume that the
     * </p>
     *
     * @param principal
     * @param taxonomy
     */
    public void addInferredSynonym(TaxonConcept principal, Taxonomy taxonomy) {
        TaxonResolver resolver = taxonomy.getResolver();
        TaxonConceptInstance representative = principal.getRepresentative();
        if (representative == null) {
            taxonomy.report(IssueType.ERROR, "taxonConcept.representative", principal, this);
            return;
        }
        List<TaxonConceptInstance> inferred = this.resolution.getUsed().stream().filter(tci -> tci.isAccepted()).map(tci -> representative.createInferredSynonym(this, tci.getScientificName(), tci.getScientificNameAuthorship(), tci.getYear(), taxonomy)).collect(Collectors.toList());
        if (inferred.isEmpty()) {
            taxonomy.report(IssueType.ERROR, "taxonConcept.inferredSynonyms", principal, this);
            return;
        }
        this.instances.addAll(inferred);
        List<TaxonConceptInstance> used = this.instances.stream().filter(tci -> tci.isInferredSynonym()).collect(Collectors.toList());
        if (used.size() > 1)
            taxonomy.report(IssueType.NOTE, "taxonConcept.multipleInferredSynonyms", this, used.get(0), used.get(1));
        this.resolution = resolver.resolve(this, used, this.instances);
        taxonomy.count("count.resolve.inferredSynonym");
    }

    /**
     * Get a representative taxon instance.
     * <p>
     * This is, basically, the highest priority instance that the concept has used.
     * </p>
     *
     * @return The representative instance
     */
    @Override
    public TaxonConceptInstance getRepresentative() {
        return this.resolution == null ? this.instances.get(0) : this.resolution.getUsed().get(0);
    }


    /**
     * A human readbale label for the concept
     *
     * @return The label
     */
    @Override
    public String toString() {
        return "TC[" + this.key.getScientificName() + ", " + this.key.getScientificNameAuthorship() + "]";
    }

    /**
     * Is this taxon concept "owned" by a specicfic data provider?
     * <p>
     * This is true if it is accepted and the resolved taxon is owned.
     * </p>
     *
     * @return True if this is an owned taxon concept
     */
    public boolean isOwned() {
        TaxonConceptInstance accepted = this.getRepresentative();
        return (accepted.getContainer() == this && accepted.isOwned());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        StringBuilder sb = new StringBuilder(32);
        sb.append(this.key.getScientificName());
        if (this.key.getScientificNameAuthorship() != null) {
            sb.append(" ");
            sb.append(this.key.getScientificNameAuthorship());
        }
        TaxonConceptInstance rep = this.getRepresentative();
        if (rep != null) {
            sb.append(" [");
            sb.append(rep.getTaxonID());
            sb.append("]");
        }
        return sb.toString();
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
     * Get the score of the principal element.
     *
     * @return The principal score
     */
    @Override
    public int getPrincipalScore() {
        TaxonConceptInstance representative = this.getRepresentative();

        return representative != null ? representative.getPrincipalScore() : Integer.MIN_VALUE;
    }

    /**
     * Get the provider score of the principal element.
     *
     * @return The principal score
     */
    @Override
    public int getProviderScore() {
        TaxonConceptInstance representative = this.getRepresentative();

        return representative != null ? representative.getProviderScore() : Integer.MIN_VALUE;
    }

    /**
     * Validate this taxon concept.
     * <p>
     * Note that this does not validate the taxon concept instances; these get done separately
     * </p>
     *
     * @param taxonomy The taxonomy to validate against and report to
     *
     * @return True if the scientific name is valid
     */
    @Override
    public boolean validate(Taxonomy taxonomy) {
        boolean valid = true;
        if (this.instances.isEmpty()) {
            taxonomy.report(IssueType.VALIDATION, "taxonConcept.validation.noInstances", this);
            valid = false;
        }
        for (TaxonConceptInstance tci: this.instances) {
            if (tci.getContainer() != this) {
                taxonomy.report(IssueType.VALIDATION, "taxonConcept.validation.instanceParent", tci, this);
                valid = false;
            }
        }
        if (this.isResolved())
            if (!this.resolution.validate(this.instances, taxonomy))
                valid = false;
        return valid;
    }

}
