/*
 * Copyright (c) 2021 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 */

package au.org.ala.names.index;

import au.org.ala.names.model.RankType;
import au.org.ala.names.model.TaxonFlag;
import au.org.ala.names.model.TaxonomicType;
import au.org.ala.vocab.ALATerm;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.dwc.terms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
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
    public CSVNameSource(Reader reader, Term rowType) throws IOException, CsvValidationException {
        this.name = "Reader " + System.identityHashCode(reader) + " - " + rowType;
        this.reader = new CSVReaderBuilder(reader).build();
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
    public CSVNameSource(Path path, String encoding, Term rowType) throws IOException, CsvValidationException {
        this(Files.newBufferedReader(path, Charset.forName(encoding)), rowType);
        this.name = path.toUri().toASCIIString();
    }

    /**
     * Get the type of core row this source represents
     *
     * @return The core type
     */
    @Override
    public Term getCoreType() {
        return this.rowType;
    }

    protected void collectColumns() throws IOException, CsvValidationException {
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
        else if (this.rowType == ALATerm.Location)
            this.loadLocation(taxonomy);
        else if (this.rowType == GbifTerm.Reference)
            this.loadReference(taxonomy);
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
        Integer nameCompleteIndex = termLocations.get(ALATerm.nameComplete);
        Integer namePublishedInYearIndex = termLocations.get(DwcTerm.namePublishedInYear);
        Integer taxonomicStatusIndex = termLocations.get(DwcTerm.taxonomicStatus);
        Integer taxonRankIndex = termLocations.get(DwcTerm.taxonRank);
        Integer nomenclaturalStatusIndex = termLocations.get(DwcTerm.nomenclaturalStatus);
        Integer parentNameUsageIndex = termLocations.get(DwcTerm.parentNameUsage);
        Integer parentNameUsageIDIndex = termLocations.get(DwcTerm.parentNameUsageID);
        Integer acceptedNameUsageIndex = termLocations.get(DwcTerm.acceptedNameUsage);
        Integer acceptedNameUsageIDIndex = termLocations.get(DwcTerm.acceptedNameUsageID);
        Integer taxonRemarksIndex = termLocations.get(DwcTerm.taxonRemarks);
        Integer provenanceIndex = termLocations.get(DcTerm.provenance);
        Integer taxonomicFlagsIndex = termLocations.get(ALATerm.taxonomicFlags);
        Integer distributionIndex = termLocations.get(ALATerm.distribution);
        Set<Term> classifications = TaxonConceptInstance.CLASSIFICATION_FIELDS.stream().filter(t -> termLocations.containsKey(t)).collect(Collectors.toSet());
        try {
            String[] r;

            while ((r = this.reader.readNext()) != null) {
                final String[] record = r;
                String taxonID = this.get(record, taxonIDIndex);
                String verbatimNomenclautralCode = this.get(record, nomenclaturalCodeIndex);
                NomenclaturalClassifier code = taxonomy.resolveCode(verbatimNomenclautralCode);
                NameProvider provider = taxonomy.resolveProvider(this.get(record, datasetIDIndex), this.get(record, datasetNameIndex));
                String scientificName = this.get(record, scientificNameIndex);
                String scientificNameAuthorship = this.get(record, scientificNameAuthorshipIndex);
                String nameComplete = this.get(record, nameCompleteIndex);
                String year = this.get(record, namePublishedInYearIndex);
                String verbatimTaxonomicStatus = this.get(record, taxonomicStatusIndex);
                TaxonomicType taxonomicStatus = taxonomy.resolveTaxonomicType(verbatimTaxonomicStatus);
                String verbatimTaxonRank = this.get(record, taxonRankIndex);
                RankType rank = taxonomy.resolveRank(verbatimTaxonRank);
                String verbatimNomenclaturalStatus = this.get(record, nomenclaturalStatusIndex);
                Set<NomenclaturalStatus> nomenclaturalStatus = taxonomy.resolveNomenclaturalStatus(verbatimNomenclaturalStatus);
                String verbatimTaxonomicFlags = this.get(record, taxonomicFlagsIndex);
                Set<TaxonFlag> flags = taxonomy.resolveTaxonomicFlags(verbatimTaxonomicFlags);
                String parentNameUsage = this.get(record, parentNameUsageIndex);
                String parentNameUsageID = this.get(record, parentNameUsageIDIndex);
                String acceptedNameUsage = this.get(record, acceptedNameUsageIndex);
                String acceptedNameUsageID = this.get(record, acceptedNameUsageIDIndex);
                String verbatimTaxonRemarks = this.get(record, taxonRemarksIndex);
                String verbatimProvenance = this.get(record, provenanceIndex);
                List<String> taxonRemarks = verbatimTaxonRemarks == null || verbatimTaxonRemarks.isEmpty() ? null : Arrays.stream(verbatimTaxonRemarks.split("\\|")).map(s -> s.trim()).collect(Collectors.toList());
                List<String> provenance = verbatimProvenance == null || verbatimProvenance.isEmpty() ? null : Arrays.stream(verbatimProvenance.split("\\|")).map(s -> s.trim()).collect(Collectors.toList());
                Map<Term, Optional<String>> classification = null;
                if (!classifications.isEmpty()) {
                   classification = classifications.stream().collect(Collectors.toMap(t -> t, t -> Optional.ofNullable(this.get(record, termLocations.get(t)))));
                }
                List<Distribution> distribution = null;
                String dist = this.get(record, distributionIndex);
                if (dist != null) {
                    distribution = Arrays.stream(dist.split("\\|"))
                            .map(id -> taxonomy.resolveLocation(id))
                            .filter(Objects::nonNull)
                            .map(l -> new Distribution(provider, l, null, null, null))
                            .collect(Collectors.toList());
                }

                TaxonConceptInstance instance = new TaxonConceptInstance(
                        taxonID,
                        code,
                        verbatimNomenclautralCode,
                        provider,
                        scientificName,
                        scientificNameAuthorship,
                        nameComplete,
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
                        taxonRemarks,
                        verbatimTaxonRemarks,
                        provenance,
                        classification,
                        flags,
                        distribution);
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
     * Load taxon records into a taxonomy.
     *
     * @param taxonomy The taxonomy
     *
     * @throws IndexBuilderException if unable to load a record into the taxonomy.
     */
    protected void loadLocation(Taxonomy taxonomy) throws IndexBuilderException {
        final Map<Term, Integer> termLocations = this.termLocations; // Lambda requires final local variable
        taxonomy.addOutputTerms(ALATerm.Location, this.terms);
        Integer locationIDIndex = termLocations.get(DwcTerm.locationID);
        Integer parentLocationIDIndex = termLocations.get(ALATerm.parentLocationID);
        Integer localityIndex = termLocations.get(DwcTerm.locality);
        Integer geographyTypeIndex = termLocations.get(ALATerm.geographyType);
        try {
            String[] r;

            while ((r = this.reader.readNext()) != null) {
                final String[] record = r;
                String locationID = this.get(record, locationIDIndex);
                String parentLocationID = this.get(record, parentLocationIDIndex);
                String locality = this.get(record, localityIndex);
                String geographyType = this.get(record, geographyTypeIndex);
                Location location = new Location(locationID, parentLocationID, locality, geographyType);
                taxonomy.addLocation(location);
             }
        } catch (IndexBuilderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IndexBuilderException("Unable to load CSV file", ex);
        }
    }

    /**
     * Load references into a taxonomy.
     *
     * @param taxonomy The taxonomy
     *
     * @throws IndexBuilderException if unable to load a record into the taxonomy.
     */
    public void loadReference(Taxonomy taxonomy) throws IndexBuilderException {
        final Map<Term, Integer> termLocations = this.termLocations; // Lambda requires final local variable
        taxonomy.addOutputTerms(GbifTerm.Reference, this.terms);
        try {
            String[] r;

            while ((r = this.reader.readNext()) != null) {
                final String[] record = r;
                Document doc = new Document();
                doc.add(new StringField("type", ALATerm.UnplacedReference.qualifiedName(), Field.Store.YES));
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
