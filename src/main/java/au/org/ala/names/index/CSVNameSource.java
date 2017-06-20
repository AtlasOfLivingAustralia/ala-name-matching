package au.org.ala.names.index;

import au.ala.org.vocab.ALATerm;
import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonomicType;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A source of names from a CSV file.
 * <p>
 * The first row must contain
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2016 CSIRO
 */
public class CSVNameSource extends NameSource {
    private CSVReader reader;
    private List<Term> terms;
    private Map<Term, Integer> termLocations;

    /**
     * Open a CSV source.
     *
     * @param reader The
     */
    public CSVNameSource(Reader reader) throws IOException {
        this.reader = new CSVReader(reader);
        this.collectColumns();
    }

    /**
     * Open a file as a CSV name source.
     *
     * @param file The source file
     * @param encoding The source encoding
     */
    public CSVNameSource(File file, String encoding) throws IOException {
        this(new InputStreamReader(new FileInputStream(file), encoding));
    }

    protected void collectColumns() throws IOException {
        TermFactory factory = TermFactory.instance();
        int index = 0;
        String[] header = reader.readNext();

        if (header == null || header.length == 0)
            throw new IndexBuilderException("No header in CSV file");
        this.termLocations = new HashMap<>(header.length);
        this.terms = new ArrayList<>(header.length);
        for (String heading: header) {
            heading = heading.trim();
            Term term = factory.findTerm(heading);
            if (term == null)
                term = ALATerm.valueOf(heading);
            if (term != null)
                this.termLocations.put(term, index);
            this.terms.add(term);
            index++;
        }
    }

    /**
     * Validate the archive.
     * <p>
     * Ensure all the expected terms are present.
     * </p>
     *
     * @throws IndexBuilderException if ther archive is not usable
     */
    public void validate() throws IndexBuilderException {
        for (Term term: TAXON_REQUIRED)
            if (!this.termLocations.containsKey(term))
                throw new IndexBuilderException("CSV file does not contain required term " + term);
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
        Integer parentNameUsageIDIndex = termLocations.get(DwcTerm.parentNameUsageID);
        Integer acceptedNameUsageIDIndex = termLocations.get(DwcTerm.acceptedNameUsageID);
        Set<Term> classifications = TaxonConceptInstance.CLASSIFICATION_FIELDS.stream().filter(t -> termLocations.containsKey(t)).collect(Collectors.toSet());
        try {
            String[] r;

            while ((r = this.reader.readNext()) != null) {
                final String[] record = r;
                String taxonID = this.get(record, taxonIDIndex);
                NomenclaturalCode code = taxonomy.resolveCode(this.get(record, nomenclaturalCodeIndex));
                NameProvider provider = taxonomy.resolveProvider(this.get(record, datasetIDIndex), this.get(record, datasetNameIndex));
                String scientificName = this.get(record, scientificNameIndex);
                String scientificNameAuthorship = this.get(record, scientificNameAuthorshipIndex);
                String year = this.get(record, namePublishedInYearIndex);
                TaxonomicType taxonomicStatus = taxonomy.resolveTaxonomicType(this.get(record, taxonomicStatusIndex));
                RankType rank = taxonomy.resolveRank(this.get(record, taxonRankIndex));
                Set<NomenclaturalStatus> nomenclaturalStatus = taxonomy.resolveNomenclaturalStatus(this.get(record, nomenclaturalStatusIndex));
                String parentNameUsageID = this.get(record, parentNameUsageIDIndex);
                String acceptedNameUsageID = this.get(record, acceptedNameUsageIDIndex);
                Map<Term, Optional<String>> classification = null;
                if (!classifications.isEmpty()) {
                   classification = classifications.stream().collect(Collectors.toMap(t -> t, t -> Optional.ofNullable(this.get(record, termLocations.get(t)))));
                }
                TaxonConceptInstance instance = new TaxonConceptInstance(taxonID, code, provider, scientificName, scientificNameAuthorship, year, taxonomicStatus, rank, nomenclaturalStatus, parentNameUsageID, acceptedNameUsageID, classification);
                instance.normalise();
                taxonomy.addInstance(instance);
                Document doc = new Document();
                doc.add(new StringField("type", DwcTerm.Taxon.qualifiedName(), Field.Store.YES));
                doc.add(new StringField("id", UUID.randomUUID().toString(), Field.Store.YES));
                for (int i = 0; i < record.length; i++) {
                    Term term = this.terms.get(i);
                    String value = record[i];
                    if (term != null && value != null && !value.isEmpty())
                        doc.add(new StringField(term.qualifiedName(), value, Field.Store.YES));
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
