package au.org.ala.names.index;

import au.org.ala.vocab.ALATerm;
import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonomicType;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.dwc.terms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A source of names from a CSV file.
 * <p>
 * The first row must contain column labels.
 * </p>
 * <p>
 * Two types of row are supported: {@link DwcTerm#Taxon} and {@link org.gbif.dwc.terms.GbifTerm#VernacularName}
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2016 CSIRO
 */
public class CSVNameSource extends NameSource {
    private static final Logger logger = LoggerFactory.getLogger(CSVNameSource.class);

    private String name;
    private CSVReader reader;
    private Term rowType;
    private List<Term> terms;
    private Map<Term, Integer> termLocations;

    /**
     * Open a CSV source.
     *
     * @param reader The file reader
     * @param rowType The type of row in the CSV
     */
    public CSVNameSource(Reader reader, Term rowType) throws IOException {
        this.name = "Reader " + System.identityHashCode(reader);
        this.reader = new CSVReader(reader);
        this.rowType = rowType;
        this.collectColumns();
    }

    /**
     * Open a file as a CSV name source.
     *
     * @param path The source file
     * @param encoding The source encoding
     * @param rowType The type of row in the CSV
     */
    public CSVNameSource(Path path, String encoding, Term rowType) throws IOException {
        this(Files.newBufferedReader(path, Charset.forName(encoding)), rowType);
        this.name = path.toUri().toASCIIString();
    }

    protected void collectColumns() throws IOException {
        TermFactory factory = TermFactory.instance();
        int index = 0;
        String[] header = reader.readNext();

        if (header == null || header.length == 0)
            throw new IndexBuilderException("No header in CSV file");
        this.termLocations = new LinkedHashMap<>(header.length);
        this.terms = new ArrayList<>(header.length);
        for (String heading: header) {
            heading = heading.trim();
            Term term = factory.findTerm(heading);
            if (term == null)
                term = ALATerm.valueOf(heading);
            if (term != null)
                this.termLocations.put(term, index);
            else
                logger.warn("Unable to map " + heading + " onto a term");
            this.terms.add(term);
            index++;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Get a citation for this source.
     *
     * @return The citation
     */
    @Override
    public Citation getCitation() throws IndexBuilderException {
        return new Citation(this.getName(), null);
    }

    /**
     * Get a list of countries for this resource
     *
     * @return The country list
     */
    @Override
    public Collection<Country> getCountries()  {
        return Collections.emptySet();
    }

    /**
     * Get a list of contacts for this resource
     *
     * @return The country list
     */
    @Override
    public Collection<Contact> getContacts()  {
        return Collections.emptySet();
    }


    /**
     * Validate the archive.
     * <p>
     * Ensure all the expected terms are present.
     * </p>
     *
     * @throws IndexBuilderException if the archive is not usable
     */
    public void validate() throws IndexBuilderException {
        List<Term> required = REQUIRED_TERMS.get(this.rowType);
        if (required == null)
            throw new IndexBuilderException("Unable to support row type " + this.rowType);
        for (Term term: required) {
            if (!this.termLocations.containsKey(term))
                throw new IndexBuilderException("CSV file does not contain required term " + term);
        }
    }

    /**
     * Load this DwCA into a taxonomy.
     *
     * @param taxonomy The taxonomy
     *
     * @throws IndexBuilderException if unable to load a record into the taxonomy.
     */
    @Override
    public void loadIntoTaxonomy(Taxonomy taxonomy) throws IndexBuilderException {
        if (this.rowType == DwcTerm.Taxon)
            this.loadTaxon(taxonomy);
        else if (this.rowType == GbifTerm.VernacularName)
            this.loadVernacular(taxonomy);
        else
            throw new IndexBuilderException("Unable to support row type " + this.rowType);

    }

    /**
     * Load taxon records into a taxonomy.
     *
     * @param taxonomy The taxonomy
     *
     * @throws IndexBuilderException if unable to load a record into the taxonomy.
     */
    protected void loadTaxon(Taxonomy taxonomy) throws IndexBuilderException {
        final Map<Term, Integer> termLocations = this.termLocations; // Lambda requires final local variable
        taxonomy.addOutputTerms(DwcTerm.Taxon, this.terms);
        taxonomy.addOutputTerms(ALATerm.TaxonVariant, this.terms);
        Integer taxonIDIndex = termLocations.get(DwcTerm.taxonID);
        Integer nomenclaturalCodeIndex = termLocations.get(DwcTerm.nomenclaturalCode);
        Integer datasetIDIndex = termLocations.get(DwcTerm.datasetID);
        Integer datasetNameIndex = termLocations.get(DwcTerm.datasetName);
        Integer scientificNameIndex = termLocations.get(DwcTerm.scientificName);
        Integer scientificNameAuthorshipIndex = termLocations.get(DwcTerm.scientificNameAuthorship);
        Integer namePublishedInYearIndex = termLocations.get(DwcTerm.namePublishedInYear);
        Integer taxonomicStatusIndex = termLocations.get(DwcTerm.taxonomicStatus);
        Integer taxonRankIndex = termLocations.get(DwcTerm.taxonRank);
        Integer nomenclaturalStatusIndex = termLocations.get(DwcTerm.nomenclaturalStatus);
        Integer parentNameUsageIndex = termLocations.get(DwcTerm.parentNameUsage);
        Integer parentNameUsageIDIndex = termLocations.get(DwcTerm.parentNameUsageID);
        Integer acceptedNameUsageIndex = termLocations.get(DwcTerm.acceptedNameUsage);
        Integer acceptedNameUsageIDIndex = termLocations.get(DwcTerm.acceptedNameUsageID);
        Set<Term> classifications = TaxonConceptInstance.CLASSIFICATION_FIELDS.stream().filter(t -> termLocations.containsKey(t)).collect(Collectors.toSet());
        try {
            String[] r;

            while ((r = this.reader.readNext()) != null) {
                final String[] record = r;
                String taxonID = this.get(record, taxonIDIndex);
                String verbatimNomenclautralCode = this.get(record, nomenclaturalCodeIndex);
                NomenclaturalCode code = taxonomy.resolveCode(verbatimNomenclautralCode);
                NameProvider provider = taxonomy.resolveProvider(this.get(record, datasetIDIndex), this.get(record, datasetNameIndex));
                String scientificName = this.get(record, scientificNameIndex);
                String scientificNameAuthorship = this.get(record, scientificNameAuthorshipIndex);
                String year = this.get(record, namePublishedInYearIndex);
                String verbatimTaxonomicStatus = this.get(record, taxonomicStatusIndex);
                TaxonomicType taxonomicStatus = taxonomy.resolveTaxonomicType(verbatimTaxonomicStatus);
                String verbatimTaxonRank = this.get(record, taxonRankIndex);
                RankType rank = taxonomy.resolveRank(verbatimTaxonRank);
                String verbatimNomenclaturalStatus = this.get(record, nomenclaturalStatusIndex);
                Set<NomenclaturalStatus> nomenclaturalStatus = taxonomy.resolveNomenclaturalStatus(verbatimNomenclaturalStatus);
                String parentNameUsage = this.get(record, parentNameUsageIndex);
                String parentNameUsageID = this.get(record, parentNameUsageIDIndex);
                String acceptedNameUsage = this.get(record, acceptedNameUsageIndex);
                String acceptedNameUsageID = this.get(record, acceptedNameUsageIDIndex);
                Map<Term, Optional<String>> classification = null;
                if (!classifications.isEmpty()) {
                   classification = classifications.stream().collect(Collectors.toMap(t -> t, t -> Optional.ofNullable(this.get(record, termLocations.get(t)))));
                }
                TaxonConceptInstance instance = new TaxonConceptInstance(
                        taxonID,
                        code,
                        verbatimNomenclautralCode,
                        provider,
                        scientificName,
                        scientificNameAuthorship,
                        year,
                        taxonomicStatus,
                        verbatimTaxonomicStatus,
                        rank,
                        verbatimTaxonRank,
                        nomenclaturalStatus,
                        verbatimNomenclaturalStatus,
                        parentNameUsage,
                        parentNameUsageID,
                        acceptedNameUsage,
                        acceptedNameUsageID,
                        classification);
                instance.normalise();
                instance = taxonomy.addInstance(instance);
                Document doc = new Document();
                doc.add(new StringField("type", DwcTerm.Taxon.qualifiedName(), Field.Store.YES));
                doc.add(new StringField("id", UUID.randomUUID().toString(), Field.Store.YES));
                for (int i = 0; i < record.length; i++) {
                    Term term = this.terms.get(i);
                    String value = term == DwcTerm.taxonID ? instance.getTaxonID() : record[i]; // Allow for changed taxonIDs
                    if (term != null && value != null && !value.isEmpty())
                        doc.add(new StringField(Taxonomy.fieldName(term), value, Field.Store.YES));
                }
                taxonomy.addRecords(Collections.singletonList(doc));
            }
        } catch (IndexBuilderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IndexBuilderException("Unable to load CSV file", ex);
        }
    }

    /**
     * Load vernacular names into a taxonomy.
     *
     * @param taxonomy The taxonomy
     *
     * @throws IndexBuilderException if unable to load a record into the taxonomy.
     */
    public void loadVernacular(Taxonomy taxonomy) throws IndexBuilderException {
        final Map<Term, Integer> termLocations = this.termLocations; // Lambda requires final local variable
        taxonomy.addOutputTerms(GbifTerm.VernacularName, this.terms);
        try {
            String[] r;

            while ((r = this.reader.readNext()) != null) {
                final String[] record = r;
                Document doc = new Document();
                doc.add(new StringField("type", ALATerm.UnplacedVernacularName.qualifiedName(), Field.Store.YES));
                doc.add(new StringField("id", UUID.randomUUID().toString(), Field.Store.YES));
                for (int i = 0; i < record.length; i++) {
                    Term term = this.terms.get(i);
                    String value = record[i];
                    if (term != null && value != null && !value.isEmpty())
                        doc.add(new StringField(Taxonomy.fieldName(term), value, Field.Store.YES));
                }
                taxonomy.addRecords(Collections.singletonList(doc));
            }
        } catch (IndexBuilderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IndexBuilderException("Unable to load CSV file", ex);
        }
    }

    /**
     * Get and clean up CSV data.
     *
     * @param record the CSV record
     * @param index The index into the record, null for an unused value
     *
     * @return A trimmed string corresponding to the record[index] with nullified empty strings.
     */
    private String get(String[] record, Integer index) {
        if (index == null || index < 0 || index >= record.length)
            return null;
        String s = record[index];
        if (s == null)
            return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }
}
