package au.org.ala.names.index;

import au.org.ala.names.model.RankType;
import au.org.ala.names.model.SynonymType;
import com.google.common.collect.Maps;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.TaxonomicStatus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A complete taxonomy description.
 * <p>
 * This is the jumping off point for buckets of names and taxon concepts.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class Taxonomy {
    /** The name analyser */
    private NameAnalyser analyser;
    /** The list of scientific names */
    private Map<NameKey, ScientificName> names;
    /** The list of name instances, keyed by taxonID */
    private Map<String, TaxonConceptInstance> instances;
    /** The list of name sources, keyed by identifier */
    private Map<String, NameProvider> providers;
    /** The default source */
    private NameProvider defaultProvider;

    /**
     * Default taxonomy constructor.
     * <p>
     * Creates a default taxonomy with an ALA name analyser and a default provider.
     * </p>
     */
    public Taxonomy() {
        this.analyser = new ALANameAnalyser();
        this.defaultProvider = new NameProvider("default", 1.0f);
        this.providers = new HashMap<>();
        this.providers.put(this.defaultProvider.getId(), this.defaultProvider);
        this.names = new HashMap<>();
        this.instances = new HashMap<>();
    }

    /**
     * Construct a taxonomy from a configuration
     *
     * @param configuration
     *
     * @throws IndexBuilderException If the configuration is invalid in some way
     */
    public Taxonomy(TaxonomyConfiguration configuration) throws IndexBuilderException {
        configuration.validate();
        try {
            this.analyser = configuration.nameAnalyserClass.newInstance();
        } catch (Exception ex) {
            throw new IndexBuilderException("Unable to create analyser", ex);
        }
        this.providers = Maps.uniqueIndex(configuration.providers, p -> p.getId());
        this.defaultProvider = this.providers.get(configuration.defaultProvider);
        this.names = new HashMap<>();
        this.instances = new HashMap<>();
    }

    /**
     * Construct a taxonomy.
     *
     * @param analyser The name analyser to use
     * @param providers The list of data providers
     * @param defaultProvider The default provider
     */
    public Taxonomy(NameAnalyser analyser, Map<String, NameProvider> providers, NameProvider defaultProvider) {
        this.analyser = analyser;
        this.providers = providers;
        this.defaultProvider = defaultProvider;
        this.names = new HashMap<>();
        this.instances = new HashMap<>();
    }

    /**
     * Get the name map.
     *
     * @return The name map
     */
    public Map<NameKey, ScientificName> getNames() {
        return names;
    }

    /**
     * Get an instance corresponding to a taxonID.
     *
     * @param taxonID The taxon identifier
     *
     * @return The corresponding instance or null for not found
     */
    public TaxonConceptInstance getInstance(String taxonID) {
        return this.instances.get(taxonID);
    }

    /**
     * Resolve everything.
     * <ul>
     *     <li>First ensure that the tree is linked together.</li>
     *     <li>Then descend the tree, choosing a preferred instance for each taxon concept</li>
     * </ul>
     *
     * @throws IndexBuilderException
     */
    public void resolve() throws IndexBuilderException {
        for (TaxonConceptInstance instance: this.instances.values())
            instance.resolveLinks(this);
        Stream<TaxonConceptInstance> tops = this.instances.values().stream().filter(p -> p.getParent() == null);
        tops.forEach(top -> this.preferences(top));
    }

    protected void preferences(TaxonConceptInstance top) {

    }

    /**
     * Map a provider dataset id or name onto a source.
     * <p>
     * If one cannot be found, the default provider is returned.
     * </p>
     *
     * @param datasetID The dataset identifier
     * @param datasetName The dataset name
     *
     * @return The name provider
     */
    public NameProvider resolveProvider(String datasetID, String datasetName) {
        NameProvider provider;
        if ((provider = this.providers.get(datasetID)) != null)
            return provider;
        if ((provider = this.providers.get(datasetName)) != null)
            return provider;
        return this.defaultProvider;
    }

    /**
     * Get a GBIF normenclatural code.
     *
     * @param nomenclaturalCode The nomenclatural code
     *
     * @return The code
     */
    public NomenclaturalCode resolveCode(String nomenclaturalCode) {
        return this.analyser.canonicaliseCode(nomenclaturalCode);
    }

    /**
     * Get an ALA synonym type for a taxonomic status
     *
     * @param taxonomicStatus The taxonomic status
     *
     * @return The synonym type
     */
    public TaxonomicStatus resolveTaxonomicStatus(String taxonomicStatus) {
        return this.analyser.canonicaliseTaxonomicStatus(taxonomicStatus);
    }

    /**
     * Get an ALA synonym type for a taxonomic status
     *
     * @param taxonomicStatus The taxonomic status
     *
     * @return The synonym type
     */
    public SynonymType resolveSynonymType(String taxonomicStatus) {
        return this.analyser.canonicaliseSynonymType(taxonomicStatus);
    }

    /**
     * Get an ALA rank type for a rank
     *
     * @param rank The rank
     *
     * @return The rank
     */
    public RankType resolveRank(String rank) {
        return this.analyser.canonicaliseRank(rank);
    }

    /**
     * Convert a separated list of nomenclatural status terms into a set of status indicators.
     * <p>
     * A name can have multiple status markers.
     * The status names can be separated by a , ; or |
     * </p>
     *
     * @param nomenclaturalStatus The nomenclatural status terms
     *
     * @return The set or matched terms
     */
    public Set<NomenclaturalStatus> resolveNomenclaturalStatus(String nomenclaturalStatus) {
        String[] values;
        if (nomenclaturalStatus == null || nomenclaturalStatus.isEmpty())
            return null;
        values = nomenclaturalStatus.split("[,;|]");
        Set<NomenclaturalStatus> status = new HashSet<>(values.length);
        for (String v: values)
            status.add(this.analyser.canonicaliseNomenclaturalStatus(v));
        return status;
    }


    /**
     * Add a new instance of a taxon concept.
     * <p>
     * The instance is first put into a bucket by code/name and then assigned a taxon concept for all the names with the same author.
     * </p>
     *
     * @param instance The instance
     *
     * @throws Exception if unable to slot the instance in.
     */
    public void addInstance(TaxonConceptInstance instance) throws Exception {
        String taxonID = instance.getTaxonID();
        NameKey taxonKey = this.analyser.analyse(instance.getCode(), instance.getScientificName(), null);
        NameKey nameKey = taxonKey.toNameKey();
        if (this.instances.containsKey(taxonID))
            throw new IndexBuilderException("Collision in taxonIDs on " + taxonID);
        ScientificName name = this.names.get(nameKey);
        if (name == null) {
            name = new ScientificName(nameKey);
            this.names.put(nameKey, name);
        }
        name.addInstance(taxonKey, instance);
        this.instances.put(taxonID, instance);
    }


}
