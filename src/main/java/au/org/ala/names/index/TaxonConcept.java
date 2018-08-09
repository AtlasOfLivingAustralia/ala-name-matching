package au.org.ala.names.index;

import au.org.ala.vocab.ALATerm;
import au.org.ala.names.model.RankType;
import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.service.checklistbank.NameParser;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.terms.Term;
import au.org.ala.names.util.DwcaWriter;
import org.gbif.nameparser.PhraseNameParser;

import java.io.IOException;
import java.text.MessageFormat;
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
     * Get the instances of this concept.
     *
     * @return An unmodifiable list of instances.
     */
    public List<TaxonConceptInstance> getInstances() {
        return Collections.unmodifiableList(this.instances);
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
        this.instances.removeIf(tci -> tci.getTaxonID().equals(instance.getTaxonID()));
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
     * @param reset Reset the resolution
     *
     * @throws IndexBuilderException if unable to resolve preferences
     */
    public void resolveTaxon(Taxonomy taxonomy, boolean reset) throws IndexBuilderException {
        if (this.resolution != null && !reset)
            return;
        TaxonResolver resolver = taxonomy.getResolver();
        List<TaxonConceptInstance> principals = resolver.principals(this, this.instances);
        this.resolution = resolver.resolve(this, principals, this.instances);
        taxonomy.count("count.resolve.taxonConcept");
        taxonomy.count("count.resolve.taxonConceptInstance", this.instances.size());
    }

    /**
     * Reallocate the elements of another taxon concept to this taxon concept.
     * <p>
     * At the end of this reallocation, the element will contain no instances, since they have all
     * migrated into this concept.
     * </p>
     *
     * @param element The element to reallocate
     * @param taxonomy The resolving taxonomy
     * @param reason Why the concept is being reallocated
     */
    @Override
    public void reallocate(TaxonConcept element, Taxonomy taxonomy, String reason) {
        taxonomy.report(IssueType.NOTE, "taxonConcept.reallocated", element, Arrays.asList(this));
        taxonomy.count("count.reallocate.taxonConcept");
        TaxonConceptInstance representative = this.getRepresentative();
        if (representative == null || this.resolution == null)
            throw new IndexBuilderException("Unable to reallocate " + element + " to " + this + " without representative and resolution");
        String reallocated = taxonomy.getResources().getString(reason);
        reallocated = MessageFormat.format(reallocated, representative.getTaxonID(), representative.getScientificName(), representative.getScientificNameAuthorship() == null ? "" : representative.getScientificNameAuthorship());
        taxonomy.addProvenanceToOutput();
        element.resolution = new TaxonResolution();
        for (TaxonConceptInstance tci: element.instances) {
            tci.setContainer(this);
            tci.addProvenance(reallocated);
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
            if (tci.isForbidden()) {
                continue;
            }
            Collection<TaxonConceptInstance> allocated = this.resolution.getChildren(tci);
            if (allocated == null || allocated.isEmpty()) {
                taxonomy.report(IssueType.NOTE, "taxonConcept.noInstances", tci, null);
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
                    taxonomy.count("count.write.taxonConceptInstance");
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
            taxonomy.report(IssueType.ERROR, "taxonConcept.representative", principal, Arrays.asList(this));
            return;
        }
        List<TaxonConceptInstance> inferred = this.resolution.getUsed().stream().filter(tci -> tci.isAccepted()).map(tci -> representative.createInferredSynonym(this, tci.getScientificName(), tci.getScientificNameAuthorship(), tci.getYear(), taxonomy)).collect(Collectors.toList());
        if (inferred.isEmpty()) {
            taxonomy.report(IssueType.ERROR, "taxonConcept.inferredSynonyms", principal, Arrays.asList(this));
            return;
        }
        this.instances.addAll(inferred);
        List<TaxonConceptInstance> used = this.instances.stream().filter(tci -> tci.isInferredSynonym()).collect(Collectors.toList());
        if (used.size() > 1)
            taxonomy.report(IssueType.NOTE, "taxonConcept.multipleInferredSynonyms", this, used);
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
        if (this.resolution == null)
            return this.instances.isEmpty() ? null : this.instances.get(0);
        return this.resolution.getUsed().isEmpty() ? null : this.resolution.getUsed().get(0);
    }


    /**
     * A human readbale label for the concept
     *
     * @return The label
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(64);
        TaxonConceptInstance representative = this.getRepresentative();
        builder.append("TC[");
        builder.append(this.key.getCode() == null ? "no code" : this.key.getCode().getAcronym());
        builder.append(", ");
        builder.append(this.key.getScientificName());
        builder.append(", ");
        builder.append(this.key.getScientificNameAuthorship());
        builder.append(", ");
        builder.append(this.key.getRank());
        if (representative != null) {
            builder.append(" = ");
            builder.append(representative.getLocator());
        }
        builder.append("]");
        return builder.toString();
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
        return accepted != null && accepted.getContainer() == this && accepted.isOwned();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTaxonID() {
        return this.getRepresentative() == null ? null : this.getRepresentative().getTaxonID();
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
            sb.append(rep.getLocator());
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

        return representative != null ? representative.getPrincipalScore() : TaxonomicElement.MIN_SCORE;
    }

    /**
     * Get the provider score of the principal element.
     *
     * @return The principal score
     */
    @Override
    public int getProviderScore() {
        TaxonConceptInstance representative = this.getRepresentative();

        return representative != null ? representative.getProviderScore() : TaxonomicElement.MIN_SCORE;
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
            taxonomy.report(IssueType.VALIDATION, "taxonConcept.validation.noInstances", this, null);
            valid = false;
        }
        for (TaxonConceptInstance tci: this.instances) {
            if (tci.getContainer() != this) {
                taxonomy.report(IssueType.VALIDATION, "taxonConcept.validation.instanceParent", tci, Arrays.asList(this));
                valid = false;
            }
            if (!tci.isForbidden() && taxonomy.getInstance(tci.getTaxonID()) != tci) {
                taxonomy.report(IssueType.VALIDATION, "taxonConcept.validation.instanceTaxonomy", tci, Arrays.asList(this));
                valid = false;
            }
        }
        if (this.isResolved())
            if (!this.resolution.validate(this.instances, taxonomy))
                valid = false;
        return valid;
    }
    /**
     * Resolve any unranked taxon conceptss.
     * <p>
     * Look for taxon concepts that are unranked and re-assignable.
     * After this follow these heuristics:
     * </p>
     * <ol>
     *     <li>Try and find a taxon with the same name and author and a rank closest to any accepted rank.</li>
     *     <li>Parse the name to see if it gives any clues</li>
     *     <li>If there is an accepted rank, use the accepted rank</li>
     * </ol>
     *
     * @param accepted Reloace accepted/non-accepted taxa
     * @param taxonomy The base taxonomy
     */
    public void resolveUnranked(boolean accepted, Taxonomy taxonomy, UnrankedScientificName parent) throws IndexBuilderException {
        if (!this.getKey().isUnranked())
            throw new IndexBuilderException("Expecting unranked taxon concept " + this);
        TaxonResolver resolver = taxonomy.getResolver();
        List<TaxonConceptInstance> reassign = this.instances.stream().filter(tci -> (accepted && tci.isAccepted() || !accepted && !tci.isAccepted()) && tci.getProvider().getUnrankedStrategy().isReassignable(tci)).collect(Collectors.toList());
        if (!reassign.isEmpty()) {
            Set<TaxonConcept> resolve = new HashSet<>();
            for (TaxonConceptInstance tci: reassign) {
                RankType acceptedRank = resolver.estimateRank(tci, parent);
                if (acceptedRank != null && acceptedRank != RankType.UNRANKED) {
                    TaxonConceptInstance newTci = tci.createRankedInstance(acceptedRank, taxonomy);
                    tci.forwardTo(newTci, taxonomy);
                    resolve.add(newTci.getContainer());
                    resolve.add(this);
                }
            }
            for (TaxonConcept tc: resolve) {
                tc.resolveTaxon(taxonomy, true);
            }
        }
    }

}
