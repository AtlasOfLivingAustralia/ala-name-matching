package au.org.ala.names.index;

import au.ala.org.vocab.ALATerm;
import au.com.bytecode.opencsv.CSVWriter;
import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonomicType;
import au.org.ala.names.util.FileUtils;
import com.google.common.collect.Maps;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;
import org.apache.lucene.util.Version;
import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.vocabulary.*;
import org.gbif.dwc.terms.*;
import org.gbif.dwc.terms.Term;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveFactory;
import org.gbif.dwca.io.ArchiveFile;
import au.org.ala.names.util.DwcaWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A complete taxonomy description.
 * <p>
 * This is the jumping off point for buckets of names and taxon concepts.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class Taxonomy implements Reporter {
    private static Logger logger = LoggerFactory.getLogger(Taxonomy.class);
    private static DateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    /** How many things to go before giving an update */
    public static int PROGRESS_INTERVAL = 10000;

    /** The counts we do progress reports on */
    public Set<String> COUNT_PROGRESS = new HashSet<>(Arrays.asList(
            "count.load.instance",
            "count.resolve.instance.links",
            "count.resolve.taxonConcept",
            "count.write.taxonConcept"
    ));

    /**
     * The default term list
     */
    private static final List<Term> DEFAULT_TERMS = Collections.unmodifiableList(Arrays.asList(
            DwcTerm.taxonID
    ));


    /** The configuration */
    private TaxonomyConfiguration configuration;
    /** The name analyser */
    private NameAnalyser analyser;
    /** The taxon resolver */
    private TaxonResolver resolver;
    /** The work directory */
    private File work;
    /** The index to use for storing documents */
    private Analyzer indexAnalyzer;
    private File index;
    private Directory indexDir;
    private IndexWriter indexWriter;
    private SearcherManager searcherManager;
    /** The list of scientific names */
    private Map<NameKey, ScientificName> names;
    /** The list of unranked scientific names */
    private Map<NameKey, List<ScientificName>> unrankedNames;
    /** The list of unranked/uncoded scientific names */
    private Map<NameKey, List<ScientificName>> uncodedNames;
    /** The list of name instances, keyed by taxonID */
    private Map<String, TaxonConceptInstance> instances;
    /** The list of name sources, keyed by identifier */
    private Map<String, NameProvider> providers;
    /** The default source */
    private NameProvider defaultProvider;
    /** The source associated with inferences from this process */
    private NameProvider inferenceProvider;
    /** The output map */
    private Map<Term, List<Term>> outputMap;
    /** The resource bundle for error reporting */
    private ResourceBundle resources;
    /** The progress counts */
    private Map<String, Integer> counts;
    /** The map of terms onto lucene fields */
    private Map<Term, String> fieldNames;
    /** The map of lucene fields to terms */
    private Map<String, Term> fieldTerms;
    /** The list of name sources used */
    private List<NameSource> sources;

    /**
     * Default taxonomy constructor.
     * <p>
     * Creates a default taxonomy with an ALA name analyser and a default provider.
     * Work directories are created in a temporary space.
     * </p>
     *
     * @throws IndexBuilderException if unable to construct
     */
    public Taxonomy() throws IndexBuilderException {
        this(null, null);
    }

    /**
     * Construct a taxonomy from a configuration
     *
     * @param configuration The configuration to use
     * @param work The work directory (null for a temporary directory)
     *
     * @throws IndexBuilderException If the configuration is invalid in some way
     */
    public Taxonomy(TaxonomyConfiguration configuration, File work) throws IndexBuilderException {
        if (configuration == null) {
            configuration = new TaxonomyConfiguration();
            configuration.nameAnalyserClass = ALANameAnalyser.class;
            configuration.resolverClass = ALATaxonResolver.class;
            configuration.defaultProvider = new NameProvider("default", 100);
            configuration.inferenceProvider = configuration.defaultProvider;
            configuration.providers = new ArrayList<>();
            configuration.providers.add(configuration.defaultProvider);
        }
        this.configuration = configuration;
        this.configuration.validate();
        try {
            this.analyser = this.configuration.nameAnalyserClass.newInstance();
            this.analyser.setReporter(this);
        } catch (Exception ex) {
            throw new IndexBuilderException("Unable to create analyser", ex);
        }
        try {
            this.resolver = this.configuration.resolverClass.getConstructor(Taxonomy.class).newInstance(this);
        } catch (Exception ex) {
            throw new IndexBuilderException("Unable to create resolver", ex);
        }
        this.providers = new HashMap<>(Maps.uniqueIndex(configuration.providers, p -> p.getId()));
        this.defaultProvider = this.configuration.defaultProvider;
        this.inferenceProvider = this.configuration.inferenceProvider != null ? configuration.inferenceProvider : this.defaultProvider;
        this.names = new HashMap<>();
        this.unrankedNames = new HashMap<>();
        this.uncodedNames = new HashMap<>();
        this.instances = new HashMap<>();
        this.makeBaseOutputMap();
        this.makeWorkArea(work);
        this.indexAnalyzer = new KeywordAnalyzer();
        this.resources = ResourceBundle.getBundle("taxonomy");
        this.counts = new HashMap<>();
        this.fieldNames = new HashMap<>();
        this.fieldTerms = new HashMap<>();
        this.sources = new ArrayList<>();
    }

    /**
     * Construct a taxonomy.
     *
     * @param analyser The name analyser to use
     * @param resolver The resolver to use
     * @param providers The list of data providers
     * @param defaultProvider The default provider
     */
    public Taxonomy(NameAnalyser analyser, TaxonResolver resolver, Map<String, NameProvider> providers, NameProvider defaultProvider) {
        this.configuration = new TaxonomyConfiguration();
        this.analyser = analyser;
        this.analyser.setReporter(this);
        this.resolver = resolver;
        this.providers = providers;
        this.defaultProvider = defaultProvider;
        this.names = new HashMap<>();
        this.unrankedNames = new HashMap<>();
        this.uncodedNames = new HashMap<>();
        this.instances = new HashMap<>();
        this.makeBaseOutputMap();
        this.makeWorkArea(null);
        this.indexAnalyzer = new KeywordAnalyzer();
        this.resources = ResourceBundle.getBundle("taxonomy");
        this.counts = new HashMap<>();
        this.fieldNames = new HashMap<>();
        this.fieldTerms = new HashMap<>();
        this.sources = new ArrayList<>();
    }

    /**
     * Get the working directory
     *
     * @return Thr working directory
     */
    public File getWork() {
        return work;
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
     * Get the resolver instance.
     *
     * @return The resolver
     */
    public TaxonResolver getResolver() {
        return resolver;
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
     * Add a count to the statistics
     *
     * @param type The count type
     */
    public synchronized void count(String type) {
       Integer count = this.counts.get(type);
       if (count == null)
           count = 0;
       count++;
       this.counts.put(type, count);
       if (COUNT_PROGRESS.contains(type) && count % PROGRESS_INTERVAL == 0) {
           String message = this.resources.getString(type);
           message = MessageFormat.format(message, count);
           logger.info(message);
       }
    }

    /**
     * Begin loading and processing.
     */
    public void begin() throws IndexBuilderException {
        try {
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_4, this.indexAnalyzer);
            this.indexWriter = new IndexWriter(this.indexDir, config);
            this.indexWriter.commit();
            this.searcherManager = new SearcherManager(this.indexWriter, true, null);
        } catch (IOException ex) {
            throw new IndexBuilderException("Error creating working index", ex);
        }
    }

    /**
     * Close the taxonomy.
     */
    public void close() throws IndexBuilderException {
        try {
            if (this.indexWriter != null) {
                this.indexWriter.close();
                this.indexWriter = null;
            }
            if (this.searcherManager != null) {
                this.searcherManager.close();
                this.searcherManager = null;
            }
        } catch (IOException ex) {
            throw new IndexBuilderException("Error closing taxonomy", ex);
        }
    }
    /**
     * Load a sequence of name sources into the taxonomy.
     * <p>
     * This gets done as one unit to ensure that the index is completed by the time
     * the all sources are loaded.
     * </p>
     *
     * @param sources The sources
     *
     * @throws IndexBuilderException
     */
    public void load(List<NameSource> sources) throws IndexBuilderException {
        try {
            if (this.indexWriter == null)
                throw new IllegalStateException("Index not opened");
            for (NameSource source: sources) {
                logger.info("Loading " + source.getName());
                source.loadIntoTaxonomy(this);
                this.indexWriter.commit();
                this.searcherManager.maybeRefresh();
                this.sources.add(source);
            }
        } catch (IOException ex) {
            throw new IndexBuilderException("Error constructing source index", ex);
        }
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
        this.resolveLinks();
        if (!this.validate())
            throw new IndexBuilderException("Invalid source data");
        this.validateNameCollisions();
        this.resolveTaxon();
        this.resolvePrincipal();
        if (!this.validate())
            throw new IndexBuilderException("Invalid resolution");

    }

    /**
     * Resolve all the links in the taxonomy.
     * <p>
     * Each instance either needs to have a null parent or a parent that matches the parentNameUsageID, if supplied,
     * or the kingdom - phylum - class - order - family - genus - specificEpithet - infraspecificEpithet classificatio, if supplied.
     * </p>
     * @throws IndexBuilderException
     */
    public void resolveLinks() throws IndexBuilderException {
        logger.info("Resolving links");
        this.instances.values().parallelStream().forEach(instance -> instance.resolveLinks(this));
        logger.info("Finished resolving links");
    }

    /**
     * Validate the loaded data.
     * <p>
     * Errors are added as reports
     * </p>
     *
     * @return True if the loaded data is valid, false otherwise
     *
     * @throws IndexBuilderException if unable to validate the loaded data
     */
    public boolean validate() throws IndexBuilderException {
        logger.info("Starting validation");
        boolean valid = true;
        valid = this.instances.values().parallelStream().map(instance -> instance.validate(this)).reduce(valid, (a, b) -> a && b);
        valid = this.names.values().parallelStream().map(instance -> instance.validate(this)).reduce(valid, (a, b) -> a && b);
        logger.info("Finished validation");
        return valid;
    }

    /**
     * Check for similar/identical names at different ranks
     *
     * @throws IndexBuilderException
     */
    protected void validateNameCollisions() throws IndexBuilderException {
        Map<String, Integer> counts = new HashMap<>(this.names.size());
        for (NameKey key: this.names.keySet()) {
            String name = key.getScientificName();
            counts.put(name, counts.getOrDefault(name, 0) + 1);
        }
        for (Map.Entry<String, Integer> entry: counts.entrySet()) {
            if (entry.getValue() > 1)
                this.report(IssueType.NOTE, "name.homonym", entry.getKey());
        }
    }


    /**
     * Resolve the preferred instance associated with a taxon concept
     * and the preferred taxon concept associated with a name.
     * <p>
     * Each taxon concept can contain multiple instances.
     * Choose an instance, based on the data providers, that is the "most representative"
     * </p>
     * <p>
     * We work down the rank structure, so that higher-order concepts have a preferred data provider
     * when queried. This allows us to say things like "find the common parent bwteen these two taxa"
     * </p>
     *
     * @throws IndexBuilderException
     *
     * @see TaxonConcept#resolveTaxon(Taxonomy)
     */
    public void resolveTaxon() throws IndexBuilderException {
        logger.info("Resolving taxa");
        final Collection<TaxonConceptInstance> allInstances = this.instances.values();
        final Set<RankType> rs = allInstances.parallelStream().map(TaxonConceptInstance::getRank).collect(Collectors.toSet());
        List<RankType> ranks = new ArrayList<>(rs);
        Collections.sort(ranks, (r1, r2) -> r1.getSortOrder() - r2.getSortOrder());
        long prevResolved = 0;
        long resolved = 0;
        do {
            for (RankType rank : ranks) {
                Set<TaxonConcept> concepts = allInstances.parallelStream().filter(instance -> instance.getRank() == rank).map(TaxonConceptInstance::getTaxonConcept).collect(Collectors.toSet());
                concepts.parallelStream().forEach(tc -> tc.resolveTaxon(this));
            }
            prevResolved = resolved;
            resolved = allInstances.stream().filter(instance -> instance.isResolved()).count();
            logger.debug("Resolved " + prevResolved + " -> " + resolved);
        } while (resolved != prevResolved);
        logger.info("Finished resolving taxa");

    }

    /**
     * Resolve the principal taxon concept for the scientific names.
     *
     * @throws IndexBuilderException
     *
     * @see ScientificName#resolvePrincipal(Taxonomy)
     */
    public void resolvePrincipal() throws IndexBuilderException {
        this.names.values().forEach(name -> name.resolvePrincipal(this));
    }

    /**
     * Map a provider dataset id or name onto a source.
     * <p>
     * If one cannot be found, a nre provider with the correct ID is created, linked to the default provider.
     * </p>
     *
     * @param datasetID The dataset identifier
     * @param datasetName The dataset name
     *
     * @return The name provider
     */
    public NameProvider resolveProvider(String datasetID, String datasetName) {
        NameProvider provider;
        if (datasetID == null && datasetName == null)
            return this.defaultProvider;
        if ((provider = this.providers.get(datasetID)) != null)
            return provider;
        if ((provider = this.providers.get(datasetName)) != null)
            return provider;
        provider = new NameProvider(datasetID != null ? datasetID : datasetName, datasetName, this.defaultProvider, true);
        this.report(IssueType.NOTE, "taxonomy.load.provider", provider.getId());
        if (datasetID != null)
            this.providers.put(datasetID, provider);
        if (datasetName != null)
            this.providers.put(datasetName, provider);
        return provider;
    }

    /**
     * Get a GBIF normenclatural code.
     * <p>
     * If not matched, then
     * </p>
     *
     * @param nomenclaturalCode The nomenclatural code
     *
     * @return The code
     */
    public NomenclaturalCode resolveCode(String nomenclaturalCode) {
        return this.analyser.canonicaliseCode(nomenclaturalCode);
    }

    /**
     * Get an ALA taxonomic type for a taxonomic status
     *
     * @param taxonomicStatus The taxonomic status
     *
     * @return The synonym type
     */
    public TaxonomicType resolveTaxonomicType(String taxonomicStatus) {
        return this.analyser.canonicaliseTaxonomicType(taxonomicStatus);
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
        for (String v: values) {
            NomenclaturalStatus s = this.analyser.canonicaliseNomenclaturalStatus(v);
            if (s != null)
                status.add(s);
        }
        return status.isEmpty() ? null : status;
    }


    /**
     * Add a new instance of a taxon concept.
     * <p>
     * The instance is first put into a bucket by code/name and then assigned a taxon concept for all the names with the same author.
     * </p>
     *
     * @param instance The instance
     *
     * @return The instance that was actually added
     *
     * @throws Exception if unable to slot the instance in.
     */
    public TaxonConceptInstance addInstance(TaxonConceptInstance instance) throws Exception {
        String taxonID = instance.getTaxonID();
        boolean loose = instance.getProvider().isLoose();
        NameKey taxonKey;
        NameKey nameKey;
        NameKey unrankedKey;
        NameKey uncodedKey;

        taxonKey = this.analyser.analyse(instance.getCode(), instance.getScientificName(), instance.getScientificNameAuthorship(), instance.getRank(), loose);
        taxonKey = instance.getProvider().adjustKey(taxonKey, instance);
        switch (taxonKey.getType()) {
            case PLACEHOLDER:
                // Placeholder and invalid names are forbidden
                this.report(IssueType.NOTE, "taxonomy.load.placeholder", instance);
                instance.setForbidden(true);
                break;
            case NO_NAME:
                // Impossible names are made forbdden
                this.report(IssueType.VALIDATION, "taxonomy.load.no_name", instance);
                instance.setForbidden(true);
                break;
            case INFORMAL:
            case DOUBTFUL:
            case CANDIDATUS:
                this.report(IssueType.NOTE, "taxonomy.load.as_is", instance);
                break;
            case SCIENTIFIC:
            case VIRUS:
            case HYBRID:
            case CULTIVAR:
                break;
        }
        this.count("count.load." + taxonKey.getType().name());
        if (!instance.isForbidden() && instance.getProvider().forbid(instance)) {
            this.report(IssueType.NOTE, "taxonomy.load.forbidden", instance);
            instance.setForbidden(true);
        }

        nameKey = taxonKey.toNameKey();
        unrankedKey = taxonKey.toUnrankedNameKey();
        uncodedKey = taxonKey.toUncodedNameKey();

        if (this.instances.containsKey(taxonID)) {
            this.report(IssueType.VALIDATION, "taxonomy.load.collision", instance, this.instances.get(taxonID));
            instance = new TaxonConceptInstance(
                    UUID.randomUUID().toString(),
                    instance.getCode(),
                    instance.getVerbatimNomenclaturalCode(),
                    instance.getProvider(),
                    instance.getScientificName(),
                    instance.getScientificNameAuthorship(),
                    instance.getYear(),
                    instance.getTaxonomicStatus(),
                    instance.getVerbatimTaxonomicStatus(),
                    instance.getRank(),
                    instance.getVerbatimTaxonRank(),
                    instance.getStatus(),
                    instance.getVerbatimNomenclaturalStatus(),
                    instance.getParentNameUsage(),
                    instance.getParentNameUsageID(),
                    instance.getAcceptedNameUsage(),
                    instance.getAcceptedNameUsageID(),
                    instance.getClassification()
            );
        }

        // Uncoded names mess everthing up. So we allocate it to a coded name
        if (nameKey.isUncoded()) {
            List<ScientificName> coded = this.uncodedNames.get(uncodedKey);
            if (coded != null && !coded.isEmpty()) {
                if (coded.size() > 2)
                    this.report(IssueType.PROBLEM, "taxonomy.load.uncoded.multiple", instance, coded.get(0), coded.get(1));
                this.report(IssueType.NOTE, "taxonomy.load.uncoded", instance, coded.get(0));
                nameKey = coded.get(0).getKey();
            } else {
                this.report(IssueType.NOTE, "taxonomy.load.uncoded.add", instance);
            }
        }

        ScientificName name = this.names.get(nameKey);
        if (name == null) {
            name = new ScientificName(nameKey);
            this.names.put(nameKey, name);
            List<ScientificName> unrankedNames = this.unrankedNames.get(unrankedKey);
            if (unrankedNames == null) {
                unrankedNames = new ArrayList<>();
                this.unrankedNames.put(unrankedKey, unrankedNames);
            }
            unrankedNames.add(name);
            List<ScientificName> uncodedNames = this.uncodedNames.get(uncodedKey);
            if (uncodedNames == null) {
                uncodedNames = new ArrayList<>();
                this.uncodedNames.put(uncodedKey, uncodedNames);
            }
            uncodedNames.add(name);
        }
        name.addInstance(taxonKey, instance);
        this.instances.put(taxonID, instance);
        this.count("count.load.instance");
        return instance;
    }

    /**
     * Find a taxon instance for a particular name.
     * <p>
     * The taxon instance will be matched to the instance provided by a particular provider.
     * This allows taxonomies that don't have a tree structure to link up with their higher taxonomy.
     * </p>
     * @param code The nomenclatural code
     * @param name The scientific name to find
     * @param provider The provider
     * @param rank The instance ranks
     *
     * @return The matching instance, or null for not found
     */
    public TaxonConceptInstance findInstance(NomenclaturalCode code, String name, NameProvider provider, RankType rank) {
        NameKey nameKey = null;
        nameKey = this.analyser.analyse(code, name, null, rank, provider.isLoose()).toNameKey();
        ScientificName scientificName = this.names.get(nameKey);
        return scientificName == null ? null : scientificName.findInstance(provider);
    }

    /**
     * Find a taxon instance for a particular name without considering rank.
     * <p>
     * The taxon instance will be matched to the instance provided by a particular provider.
     * This allows taxonomies that don't have a tree structure to link up with their higher/accepted taxonomy.
     * </p>
     * @param code The nomenclatural code
     * @param name The scientific name to find
     * @param provider The provider
     *
     * @return The matching instance, or null for not found
     */
    public TaxonConceptInstance findUnrankedInstance(NomenclaturalCode code, String name, NameProvider provider) {
        NameKey nameKey = null;
        nameKey = this.analyser.analyse(code, name, null, null, provider.isLoose()).toUnrankedNameKey();
        List<ScientificName> names = this.unrankedNames.get(nameKey);
        if (names == null)
            return null;
        for (ScientificName sn: names) {
            TaxonConceptInstance instance = sn.findInstance(provider);
            if (instance != null)
                return instance;
        }
        return null;
    }

    /**
     * Find a taxon instance for a particular name without considering or nomenclatural code.
     * <p>
     * The taxon instance will be matched to the instance provided by a particular provider.
     * This allows taxonomies that don't have a tree structure to link up with their higher/accepted taxonomy.
     * </p>
     * @param name The scientific name to find
     *
     * @return The matching instance, or null for not found
     */
    public List<ScientificName> findCodedInstances(String name) {
        NameKey nameKey = null;
        nameKey = this.analyser.analyse(null, name, null, null, true).toUncodedNameKey();
        return this.uncodedNames.get(nameKey);
    }

    /**
     * Create a DwCA containing the resolved taxonomy.
     * <p>
     * This taxonomy links all elements of the supplied taxonomies together
     * with the main line being the core taxon.
     * </p>
     *
     * @param directory The directory to write to
     *
     * @throws IndexBuilderException if unable to build the archive
     * @throws IOException if unable to write to the archive
     */
    public void createDwCA(File directory) throws IndexBuilderException, IOException {
        logger.info("Creating DwCA");
        this.addOutputTerms(GbifTerm.Identifier, Arrays.asList(DcTerm.title, ALATerm.status, DcTerm.source)); // Generated by taxon concept instance
        DwcaWriter dwcaWriter = new DwcaWriter(DwcTerm.Taxon, DwcTerm.taxonID, directory, true);
        dwcaWriter.setEml(this.buildEml());
        for (NameProvider provider: this.providers.values()) {
            if (provider.isExternal())
                dwcaWriter.addDetatchedRecord(DcTerm.rightsHolder, provider.getProviderMap());
        }
        List<ScientificName> nameList = new ArrayList<>(this.names.values());
        Collections.sort(nameList);
        for (ScientificName name: nameList)
            name.write(this, dwcaWriter);
        dwcaWriter.close();
        logger.info("Finished creating DwCA");
    }

    /**
     * Build an EML description of the taxonomy.
     *
     * @return The description
     */
    protected Dataset buildEml() throws IndexBuilderException {
        Dataset dataset = new Dataset();
        Date now = new Date();
        String altId = MessageFormat.format("{0}-{1,date,yyyyMMdd}", this.configuration.id, now);

        dataset.setAbbreviation(this.configuration.id);
        dataset.setAdditionalInfo(this.resources.getString("dwca.additionalInfo"));
        dataset.setAlias(altId);
        dataset.setCitation(new Citation(this.configuration.name, this.configuration.id));
        dataset.setCreatedBy(this.configuration.getContactName());
        List<Contact> contacts = new ArrayList<>();
        if (this.configuration.contact != null) {
            try {
                Contact primary = (Contact) BeanUtils.cloneBean(this.configuration.contact);
                primary.setType(ContactType.ORIGINATOR);
                primary.setPrimary(true);
                contacts.add(primary);
                Contact metadata = (Contact) BeanUtils.cloneBean(this.configuration.contact);
                metadata.setType(ContactType.METADATA_AUTHOR);
                metadata.setPrimary(true);
                contacts.add(metadata);
                Contact processor = (Contact) BeanUtils.cloneBean(this.configuration.contact);
                metadata.setType(ContactType.PROCESSOR);
                metadata.setPrimary(false);
                contacts.add(metadata);
            } catch (Exception ex) {
                logger.error("Unable to clone contact", ex);
            }
        }
        contacts.addAll(this.sources.stream().flatMap(s -> s.getContacts().stream()).collect(Collectors.toSet()));
        dataset.setContacts(contacts);
        dataset.setCountryCoverage(this.sources.stream().flatMap(s -> s.getCountries().stream()).collect(Collectors.toSet()));
        dataset.setCreated(new Date());
        dataset.setDescription(this.configuration.description);
        List<Citation> citations = new ArrayList<>();
        citations.addAll(this.sources.stream().map(s -> s.getCitation()).filter(c -> c != null).collect(Collectors.toList()));
        citations.addAll(this.providers.values().stream().filter(p -> p.isExternal()).map(p -> p.getCitation()).collect(Collectors.toList()));
        dataset.setBibliographicCitations(citations);
        List<Identifier> ids = new ArrayList<>();
        if (this.configuration.uri != null)
            ids.add(new Identifier(IdentifierType.URI, this.configuration.uri.toString()));
        if (this.configuration.id != null)
            ids.add(new Identifier(IdentifierType.UNKNOWN, this.configuration.id));
        dataset.setIdentifiers(ids);
        dataset.setPubDate(now);
        dataset.setTitle(this.configuration.name);
        return dataset;
    }


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
    @Override
    public void report(IssueType type, String code, String... args) {
        String message;
        try {
            message = this.resources.getString(code);
            message = message == null ? code : message;
            message = MessageFormat.format(message, args);
        } catch (MissingResourceException ex) {
            logger.error("Can't find resource for " + code + " defaulting to code");
            message = code;
        }
        if (type == IssueType.ERROR || type == IssueType.VALIDATION)
            logger.error(message);
        if (type == IssueType.PROBLEM)
            logger.warn(message);
        if (type == IssueType.NOTE || type == IssueType.COUNT)
            logger.debug(message);
        Document doc = new Document();
        doc.add(new StringField("type", ALATerm.TaxonomicIssue.qualifiedName(), Field.Store.YES));
        doc.add(new StringField("id", UUID.randomUUID().toString(), Field.Store.YES));
        doc.add(new StringField(this.fieldName(DcTerm.type), type.name(), Field.Store.YES));
        doc.add(new StringField(this.fieldName(DcTerm.subject), code, Field.Store.YES));
        doc.add(new StringField(this.fieldName(DcTerm.description), message, Field.Store.YES));
        doc.add(new StringField(this.fieldName(DcTerm.date), ISO8601.format(new Date()), Field.Store.YES));
        try {
            this.indexWriter.addDocument(doc);
            this.indexWriter.commit();
            this.searcherManager.maybeRefresh();
        } catch (IOException ex) {
            logger.error("Unable to write report to index", ex);
        }
    }

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
    @Override
    public void report(IssueType type, String code, TaxonomicElement... elements) {
        String taxonID = "";
        String scientificName = "";
        String scientificNameAuthorship = "";
        String associatedTaxa = "";
        String datasetID = "";
        TaxonomicElement main = elements.length > 0 ? elements[0] : null;
        if (main != null) {
            taxonID = main.getId();
            scientificName = main.getScientificName();
            scientificNameAuthorship = main.getScientificNameAuthorship();
            if (scientificNameAuthorship == null)
                scientificNameAuthorship = "";
            if (main instanceof TaxonConceptInstance)
                datasetID = ((TaxonConceptInstance) main).getProvider().getId();
        }
        if (elements.length > 1) {
            StringBuilder associated = new StringBuilder();
            for (int i = 1; i < elements.length; i++) {
                if (associated.length() > 0)
                    associated.append("|");
                associated.append(elements[i].getId());
            }
            associatedTaxa = associated.toString();
        }
        String message;
        try {
            message = this.resources.getString(code);
            message = MessageFormat.format(message == null ? code : message, taxonID, scientificName, scientificNameAuthorship, associatedTaxa);
        } catch (MissingResourceException ex) {
            logger.error("Can't find resource for " + code + " defaulting to code");
            message = code;
        }
        if (type == IssueType.ERROR || type == IssueType.VALIDATION)
            logger.error(message);
        if (type == IssueType.PROBLEM)
            logger.warn(message);
        if (type == IssueType.NOTE)
            logger.debug(message);
        Document doc = new Document();
        doc.add(new StringField("type", ALATerm.TaxonomicIssue.qualifiedName(), Field.Store.YES));
        doc.add(new StringField("id", UUID.randomUUID().toString(), Field.Store.YES));
        doc.add(new StringField(this.fieldName(DcTerm.type), type.name(), Field.Store.YES));
        doc.add(new StringField(this.fieldName(DcTerm.subject), code, Field.Store.YES));
        doc.add(new StringField(this.fieldName(DcTerm.description), message, Field.Store.YES));
        doc.add(new StringField(this.fieldName(DcTerm.date), ISO8601.format(new Date()), Field.Store.YES));
        doc.add(new StringField(this.fieldName(DwcTerm.taxonID), taxonID, Field.Store.YES));
        doc.add(new StringField(this.fieldName(DwcTerm.scientificName), scientificName, Field.Store.YES));
        doc.add(new StringField(this.fieldName(DwcTerm.scientificNameAuthorship), scientificNameAuthorship, Field.Store.YES));
        doc.add(new StringField(this.fieldName(DwcTerm.associatedTaxa), associatedTaxa, Field.Store.YES));
        doc.add(new StringField(this.fieldName(DwcTerm.datasetID), datasetID, Field.Store.YES));
        try {
            this.indexWriter.addDocument(doc);
            this.indexWriter.commit();
            this.searcherManager.maybeRefresh();
        } catch (IOException ex) {
            logger.error("Unable to write report to index", ex);
        }
    }


    /**
     * Create a report of all issues that have arisen.
     *
     * @param report The report file
     *
     * @throws IOException if unable to created the report
     */
    public void createReport(File report) throws IOException {
        logger.info("Writing report to " + report);
        int pageSize = 100;
        Writer fw = new OutputStreamWriter(new FileOutputStream(report), "UTF-8");
        CSVWriter writer = new CSVWriter(fw);
        List<Term> output = this.outputTerms(ALATerm.TaxonomicIssue);
        String[] headers = output.stream().map(term -> term.toString()).collect(Collectors.toList()).toArray(new String[output.size()]);
        writer.writeNext(headers);
        for (String type: this.counts.keySet()) {
            String message = this.resources.getString(type);
            Integer count = this.counts.get(type);
            message = MessageFormat.format(message, count);
            logger.info(message);
            String[] values = new String[] { IssueType.COUNT.name(), type, message, Integer.toString(count) };
            writer.writeNext(values);
        }
        IndexSearcher searcher = this.searcherManager.acquire();
        try {
            Query query = new TermQuery(new org.apache.lucene.index.Term("type", ALATerm.TaxonomicIssue.qualifiedName()));
            TopDocs docs = searcher.search(query, pageSize);
            ScoreDoc last = null;
            while (docs.scoreDocs.length > 0) {
                for (ScoreDoc sd : docs.scoreDocs) {
                    last = sd;
                    Document doc = searcher.doc(sd.doc);
                    String[] values = output.stream().map(term -> doc.get(this.fieldName(term))).collect(Collectors.toList()).toArray(new String[output.size()]);
                    writer.writeNext(values);
                    this.count("count.write.report");
                }
                docs = searcher.searchAfter(last, query, pageSize);
            }
        } finally {
            this.searcherManager.release(searcher);
        }
        writer.close();
        logger.info("Finished creating report");
    }

    /**
     *
     * @param work The work area, null for a default.
     *
     * @throws IndexBuilderException If unable to make the work area
     */
    private void makeWorkArea(File work) throws IndexBuilderException {
        try {
            if (work == null) {
                this.work = File.createTempFile("name", "work");
            } else {
                this.work = File.createTempFile("name", ".work", work);
            }
            this.work.delete();
            this.work.mkdirs();
            FileUtils.clear(this.work, false);
            this.index = new File(this.work, "index");
            this.index.mkdir();
            this.indexDir = new SimpleFSDirectory(this.index);
        } catch (IOException ex) {
            throw new IndexBuilderException("Unable to build work area", ex);
        }
    }

    /**
     * Set up the minimal fields required for an output row.
     */
    private void makeBaseOutputMap() {
        this.outputMap = new HashMap<>();
        for (Map.Entry<Term, List<Term>> entry: NameSource.REQUIRED_TERMS.entrySet())
            this.outputMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }


    /**
     * Add records to the document index.
     *
     * @param documents The records to add
     *
     * @throws IOException if unable to write
     */
    public void addRecords(List<Document> documents) throws IOException {
        for (Document doc: documents)
            this.indexWriter.addDocument(doc);
    }

    /**
     * Add terms to the output map.
     * <p>
     * Anything already present is not added.
     * If it is in the additional fields lists, then it is added before anything else.
     * </p>
     * <p>
     * Name sources can use this to extend the output map, so that all data provided can be
     * included in the output file.
     * </p>
     *
     * @param type The row type
     * @param terms The terms. If ordered, then the output map will respect the ordering
     */
    public void addOutputTerms(Term type, Collection<Term> terms) {
        List<Term> map = this.outputMap.get(type);
        if (map == null) {
            map = new ArrayList<>(terms.size());
            this.outputMap.put(type, map);
        }
        Set<Term> seen = new HashSet<>(map);
        List<Term> remove = NameSource.FORBIDDEN_TERMS.get(type);
        boolean strict = NameSource.ONLY_INCLUDE_ALLOWED.getOrDefault(type, false);
        if (remove != null)
            seen.addAll(remove);
        List<Term> additional = NameSource.ADDITIONAL_TERMS.get(type);
        if (additional != null) {
            for (Term term : additional) {
                if (!seen.contains(term) && terms.contains(term)) {
                    map.add(term);
                    seen.add(term);
                }

            }
        }
        Map<Term, List<Term>> implied = NameSource.IMPLIED_TERMS.get(type);
        if (implied != null) {
            for (Map.Entry<Term, List<Term>> entry: implied.entrySet()) {
                if (terms.contains(entry.getKey())) {
                    for (Term term: entry.getValue()) {
                        if (!seen.contains(term)) {
                            map.add(term);
                            seen.add(term);
                        }
                    }
                }
            }
        }
        if (!strict) {
            for (Term term : terms)
                if (!seen.contains(term))
                    map.add(term);
        }
    }

    /**
     * Get the output term list for this type of row
     *
     * @param type The row type
     *
     * @return The list of terms to include
     */
    public List<Term> outputTerms(Term type) {
        return this.outputMap.getOrDefault(type, DEFAULT_TERMS);
    }

    /**
     * Query the taxon index for source documents.
     *
     * @param type The type of document
     * @param taxonID The associated taxonID
     *
     * @return A list of matching documents
     *
     * @throws IOException Id unable to read the index
     */
    public List<Map<Term,String>> getIndexValues(Term type, String taxonID) throws IOException {
        BooleanQuery query = new BooleanQuery();
        query.add(new TermQuery(new org.apache.lucene.index.Term("type", type.qualifiedName())), BooleanClause.Occur.MUST);
        query.add(new TermQuery(new org.apache.lucene.index.Term(this.fieldName(DwcTerm.taxonID), taxonID)), BooleanClause.Occur.MUST);
        IndexSearcher searcher = this.searcherManager.acquire();
        try {
            TopDocs docs = searcher.search(query, 100, Sort.INDEXORDER);
            List<Map<Term, String>> valueList = new ArrayList<>(docs.totalHits);
            for (ScoreDoc sd : docs.scoreDocs) {
                Document document = searcher.doc(sd.doc);
                Map<Term, String> values = new HashMap<>();
                for (IndexableField field : document) {
                    if (!field.name().equals("id") && !field.name().equals("type")) {
                        Term term = this.fieldTerms.get(field.name());
                        if (term == null)
                            throw new IllegalStateException("Can't find term for " + field.name());
                        values.put(term, field.stringValue());
                    }
                }
                valueList.add(values);
            }
            return valueList;
        } finally {
            this.searcherManager.release(searcher);
        }
    }

    /**
     * Clean up after ourselves.
     *
     * @throws IOException If unable to get rid of the mess
     */
    public void clean() throws IOException {
        FileUtils.clear(this.work, true);
    }

    /**
     * Return the data provider that is the source of inferences made by the
     * taxonomy resolution algorithm.
     *
     * @return The inference provider
     */
    public NameProvider getInferenceProvider() {
        return this.inferenceProvider;
    }

    /**
     * Create a lucene-fiendly field name for a term.
     * <p>
     * Basically, get rid of colons which might gum up the works.
     * We then need to map back to the correct term.
     * </p>
     *
     * @param term The term
     *
     * @return The field name
     */
    public String fieldName(Term term) {
        String name = this.fieldNames.get(term);
        if (name == null) {
            name = term.toString().replace(':', '_');
            synchronized (this.fieldNames) {
                this.fieldNames.put(term, name);
                this.fieldTerms.put(name, term);
            }
        }
        return name;
    }
}
