package au.org.ala.names.index;

import au.ala.org.vocab.ALATerm;
import au.com.bytecode.opencsv.CSVWriter;
import au.org.ala.names.lucene.analyzer.LowerCaseKeywordAnalyzer;
import au.org.ala.names.model.RankType;
import au.org.ala.names.model.SynonymType;
import au.org.ala.names.model.TaxonomicType;
import au.org.ala.names.util.FileUtils;
import com.google.common.collect.Maps;
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
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.dwc.terms.*;
import org.gbif.dwc.terms.Term;
import org.gbif.dwca.io.DwcaWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
public class Taxonomy {
    private static Logger logger = LoggerFactory.getLogger(Taxonomy.class);
    private static DateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    /** How many things to go before giving an update */
    public static int PROGRESS_INTERVAL = 1000;

    /**
     * The default term list
     */
    private static final List<Term> DEFAULT_TERMS = Collections.unmodifiableList(Arrays.asList(
            DwcTerm.taxonID
    ));


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
        this.analyser = new ALANameAnalyser();
        this.resolver = new ALATaxonResolver(this);
        this.defaultProvider = new NameProvider("default", 100);
        this.inferenceProvider = this.defaultProvider;
        this.providers = new HashMap<>();
        this.providers.put(this.defaultProvider.getId(), this.defaultProvider);
        this.names = new HashMap<>();
        this.instances = new HashMap<>();
        this.makeBaseOutputMap();
        this.makeWorkArea(null);
        this.indexAnalyzer = new KeywordAnalyzer();
        this.resources = ResourceBundle.getBundle("taxonomy");
        this.counts = new HashMap<>();
        this.fieldNames = new HashMap<>();
        this.fieldTerms = new HashMap<>();
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
            this.analyser = new ALANameAnalyser();
            this.resolver = new ALATaxonResolver(this);
            this.defaultProvider = new NameProvider("default", 100);
            this.inferenceProvider = this.defaultProvider;
            this.providers = new HashMap<>();
            this.providers.put(this.defaultProvider.getId(), this.defaultProvider);
        } else {
            configuration.validate();
            try {
                this.analyser = configuration.nameAnalyserClass.newInstance();
            } catch (Exception ex) {
                throw new IndexBuilderException("Unable to create analyser", ex);
            }
            try {
                this.resolver = configuration.resolverClass.getConstructor(Taxonomy.class).newInstance(this);
            } catch (Exception ex) {
                throw new IndexBuilderException("Unable to create resolver", ex);
            }
            this.providers = Maps.uniqueIndex(configuration.providers, p -> p.getId());
            this.defaultProvider = configuration.defaultProvider;
            this.inferenceProvider = configuration.inferenceProvider != null ? configuration.inferenceProvider : this.defaultProvider;

        }
        this.names = new HashMap<>();
        this.instances = new HashMap<>();
        this.makeBaseOutputMap();
        this.makeWorkArea(work);
        this.indexAnalyzer = new KeywordAnalyzer();
        this.resources = ResourceBundle.getBundle("taxonomy");
        this.counts = new HashMap<>();
        this.fieldNames = new HashMap<>();
        this.fieldTerms = new HashMap<>();
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
        this.analyser = analyser;
        this.resolver = resolver;
        this.providers = providers;
        this.defaultProvider = defaultProvider;
        this.names = new HashMap<>();
        this.instances = new HashMap<>();
        this.makeBaseOutputMap();
        this.makeWorkArea(null);
        this.indexAnalyzer = new KeywordAnalyzer();
        this.resources = ResourceBundle.getBundle("taxonomy");
        this.counts = new HashMap<>();
        this.fieldNames = new HashMap<>();
        this.fieldTerms = new HashMap<>();
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
       if (count % PROGRESS_INTERVAL == 0) {
           String message = this.resources.getString(type);
           message = MessageFormat.format(message, count);
           this.logger.info(message);
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
                source.loadIntoTaxonomy(this);
                this.indexWriter.commit();
                this.searcherManager.maybeRefresh();
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
        this.resolveTaxon();
        this.resolvePrincipal();
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
        this.logger.info("Resolving links");
        this.instances.values().parallelStream().forEach(instance -> instance.resolveLinks(this));
        this.logger.info("Finished resolving links");
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
        this.logger.info("Starting validation");
        boolean valid = true;
        valid = this.instances.values().parallelStream().map(instance -> instance.validate(this)).reduce(valid, (a, b) -> a && b);
        valid = this.names.values().parallelStream().map(instance -> instance.validate(this)).reduce(valid, (a, b) -> a && b);
        this.logger.info("Finished validation");
        return valid;
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
        this.logger.info("Resolving taxa");
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
            this.logger.debug("Resolved " + prevResolved + " -> " + resolved);
        } while (resolved != prevResolved);
        this.logger.info("Finished resolving taxa");

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
        NameKey taxonKey;
        NameKey nameKey;
        try {
            taxonKey = this.analyser.analyse(instance.getCode(), instance.getScientificName(), instance.getScientificNameAuthorship());
        } catch (UnparsableException ex) {
            if (ex.type == NameType.PLACEHOLDER) {
                // Handle a placeholder name by generating a fake name key and making this instance forbidden
                this.report(IssueType.PROBLEM, "taxonomy.load.placeholder", instance);
                taxonKey = new NameKey(this.analyser, instance.getCode(), UUID.randomUUID().toString(), null, ex.type);
                instance.setForbidden(true);
            } else
                throw ex;  // Still can't handle it
        }
        if (!instance.isForbidden() && instance.getProvider().forbid(instance)) {
            this.report(IssueType.NOTE, "taxonomy.load.forbidden", instance);
            instance.setForbidden(true);
        }
        nameKey = taxonKey.toNameKey();
        if (this.instances.containsKey(taxonID)) {
            this.report(IssueType.VALIDATION, "taxonomy.load.collision", instance);
            instance = new TaxonConceptInstance(
                    UUID.randomUUID().toString(),
                    instance.getCode(),
                    this.getInferenceProvider(),
                    instance.getScientificName(),
                    instance.getScientificNameAuthorship(),
                    instance.getYear(),
                    TaxonomicType.INFERRED_UNPLACED,
                    instance.getRank(),
                    instance.getStatus(),
                    instance.getParentNameUsageID(),
                    instance.getAcceptedNameUsageID(),
                    instance.getClassification()
            );
        }
        ScientificName name = this.names.get(nameKey);
        if (name == null) {
            name = new ScientificName(nameKey);
            this.names.put(nameKey, name);
        }
        name.addInstance(taxonKey, instance);
        this.instances.put(taxonID, instance);
        this.count("count.load.instance");
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
     *
     * @return The matching instance, or null for not found
     */
    public TaxonConceptInstance findInstance(NomenclaturalCode code, String name, NameProvider provider) {
        NameKey nameKey = null;
        try {
            nameKey = this.analyser.analyse(code, name, null).toNameKey();
        } catch (UnparsableException ex) {
            this.logger.warn("Unable to parse " + name, ex);
            return null;
        }
        ScientificName scientificName = this.names.get(nameKey);
        return scientificName == null ? null : scientificName.findInstance(provider);
    }

    /**
     * Get the resolved least upper bound (lub) of two instances.
     *
     * @param i1 The first instance
     * @param i2 The second instance
     *
     * @return The lowest common resolved taxon or null for not resolved or no common taxon
     */
    public TaxonConceptInstance lub(TaxonConceptInstance i1, TaxonConceptInstance i2) {
        while (i1 != null) {
            TaxonConceptInstance r1 = i1.getResolved();
            if (r1 == null)
                return null;
            TaxonConceptInstance p2 = i2;
            while (p2 != null && p2.getResolved() != null) {
                if (p2.getResolved() == r1)
                    return r1;
                p2 = p2.getParent();
            }
            i1 = i1.getParent();
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
        this.logger.info("Creating DwCA");
        this.addOutputTerms(GbifTerm.Identifier, Arrays.asList(DcTerm.title, ALATerm.status, DcTerm.source)); // Generated by taxon concept instance
        DwcaWriter dwcaWriter = new DwcaWriter(DwcTerm.Taxon, DwcTerm.taxonID, directory, false);
        List<ScientificName> nameList = new ArrayList<>(this.names.values());
        Collections.sort(nameList);
        for (ScientificName name: nameList)
            name.write(this, dwcaWriter);
        dwcaWriter.close();
        this.logger.info("Finished creating DwCA");
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
    public void report(IssueType type, String code, String... args) {
        String message;
        try {
            message = this.resources.getString(code);
            message = MessageFormat.format(message == null ? code : message, args);
        } catch (MissingResourceException ex) {
            this.logger.error("Can't find resource for " + code + " defaulting to code");
            message = code;
        }
        if (type == IssueType.ERROR || type == IssueType.VALIDATION)
            this.logger.error(message);
        if (type == IssueType.PROBLEM)
            this.logger.warn(message);
        if (type == IssueType.NOTE)
            this.logger.debug(message);
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
            this.logger.error("Unable to write report to index", ex);
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
            this.logger.error("Can't find resource for " + code + " defaulting to code");
            message = code;
        }
        if (type == IssueType.ERROR || type == IssueType.VALIDATION)
            this.logger.error(message);
        if (type == IssueType.PROBLEM)
            this.logger.warn(message);
        if (type == IssueType.NOTE)
            this.logger.debug(message);
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
            this.logger.error("Unable to write report to index", ex);
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
        this.logger.info("Writing report to " + report);
        int pageSize = 100;
        Writer fw = new OutputStreamWriter(new FileOutputStream(report), "UTF-8");
        CSVWriter writer = new CSVWriter(fw);
        List<Term> output = this.outputTerms(ALATerm.TaxonomicIssue);
        String[] headers = output.stream().map(term -> term.toString()).collect(Collectors.toList()).toArray(new String[output.size()]);
        writer.writeNext(headers);
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
        this.logger.info("Finished creating report");
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
        List<Term> additional = NameSource.ADDIIONAL_TERMS.get(type);
        if (additional != null) {
            for (Term term : additional) {
                if (!seen.contains(additional) && terms.contains(term)) {
                    map.add(term);
                    seen.add(term);
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
