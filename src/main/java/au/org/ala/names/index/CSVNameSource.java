package au.org.ala.names.index;

import au.ala.org.vocab.ALATerm;
import au.com.bytecode.opencsv.CSVReader;
import au.org.ala.names.model.RankType;
import au.org.ala.names.model.SynonymType;
import org.apache.commons.collections.MapUtils;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.record.StarRecord;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.dwc.text.Archive;
import org.gbif.dwc.text.ArchiveFactory;
import org.gbif.dwc.text.ArchiveFile;

import java.io.*;
import java.util.*;

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
        for (String heading: header) {
            heading = heading.trim();
            Term term = factory.findTerm(heading);
            if (term == null)
                term = ALATerm.valueOf(heading);
            if (term != null)
                this.termLocations.put(term, index);
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
        Integer taxonIDIndex = this.termLocations.get(DwcTerm.taxonID);
        Integer nomenclaturalCodeIndex = this.termLocations.get(DwcTerm.nomenclaturalCode);
        Integer datasetIDIndex = this.termLocations.get(DwcTerm.datasetID);
        Integer datasetNameIndex = this.termLocations.get(DwcTerm.datasetName);
        Integer scientificNameIndex = this.termLocations.get(DwcTerm.scientificName);
        Integer scientificNameAuthorshipIndex = this.termLocations.get(DwcTerm.scientificNameAuthorship);
        Integer namePublishedInYearIndex = this.termLocations.get(DwcTerm.namePublishedInYear);
        Integer taxonomicStatusIndex = this.termLocations.get(DwcTerm.taxonomicStatus);
        Integer taxonRankIndex = this.termLocations.get(DwcTerm.taxonRank);
        Integer nomenclaturalStatusIndex = this.termLocations.get(DwcTerm.nomenclaturalStatus);
        Integer parentNameUsageIDIndex = this.termLocations.get(DwcTerm.parentNameUsageID);
        Integer acceptedNameUsageIDIndex = this.termLocations.get(DwcTerm.acceptedNameUsageID);
        try {
            String[] record;

            while ((record = this.reader.readNext()) != null) {
                String taxonID = this.get(record, taxonIDIndex);
                NomenclaturalCode code = taxonomy.resolveCode(this.get(record, nomenclaturalCodeIndex));
                NameProvider provider = taxonomy.resolveProvider(this.get(record, datasetIDIndex), this.get(record, datasetNameIndex));
                String scientificName = this.get(record, scientificNameIndex);
                String scientificNameAuthorship = this.get(record, scientificNameAuthorshipIndex);
                String year = this.get(record, namePublishedInYearIndex);
                TaxonomicStatus taxonomicStatus = taxonomy.resolveTaxonomicStatus(this.get(record, taxonomicStatusIndex));
                SynonymType synonymType = taxonomy.resolveSynonymType(this.get(record, taxonomicStatusIndex));
                RankType rank = taxonomy.resolveRank(this.get(record, taxonRankIndex));
                Set<NomenclaturalStatus> nomenclaturalStatus = taxonomy.resolveNomenclaturalStatus(this.get(record, nomenclaturalStatusIndex));
                String parentNameUsageID = this.get(record, parentNameUsageIDIndex);
                String acceptedNameUsageID = this.get(record, acceptedNameUsageIDIndex);
                TaxonConceptInstance instance = new TaxonConceptInstance(taxonID, code, provider, scientificName, scientificNameAuthorship, year, taxonomicStatus, synonymType, rank, nomenclaturalStatus, parentNameUsageID, acceptedNameUsageID);
                instance.normalise();
                taxonomy.addInstance(instance);
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
