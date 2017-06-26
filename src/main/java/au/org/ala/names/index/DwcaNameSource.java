package au.org.ala.names.index;

import au.org.ala.names.model.RankType;
import au.org.ala.names.model.SynonymType;
import au.org.ala.names.model.TaxonomicType;
import org.apache.commons.collections.MapUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.NomenclaturalStatus;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.dwca.record.Record;
import org.gbif.dwca.record.StarRecord;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveFactory;
import org.gbif.dwca.io.ArchiveFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A source of names from a Darwin Core Archive
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2016 CSIRO
 */
public class DwcaNameSource extends NameSource {
    private Archive archive;

    /**
     * Create a name source for an archive
     *
     * @param archive The source archive
     */
    public DwcaNameSource(Archive archive) {
        this.archive = archive;
    }

    /**
     * Create a name source for an archive in a working directory
     *
     * @param archiveDir The source archive directory
     */
    public DwcaNameSource(File archiveDir) throws IOException {
        this(ArchiveFactory.openArchive(archiveDir));
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
        this.checkStructure(this.archive.getCore());
        for (ArchiveFile ext: this.archive.getExtensions())
            this.checkStructure(ext);
    }

    /**
     * Ensure that an archive file has the structure we expect of a file of the supplied row type.
     *
     * @param af The archive file
     *
     * @throws IndexBuilderException if there is a problem with the file
     */
    protected void checkStructure(ArchiveFile af) throws IndexBuilderException {
        List<Term> required = REQUIRED_TERMS.get(af.getRowType());

        if (required != null) {
            for (Term term: required)
                if (!af.hasTerm(term))
                    throw new IndexBuilderException("File " + af.getTitle() + " is of type " + af.getRowType() + " and is missing " + term);

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
        List<Term> classifiers = TaxonConceptInstance.CLASSIFICATION_FIELDS.stream().filter(t -> archive.getCore().hasTerm(t)).collect(Collectors.toList());
        taxonomy.addOutputTerms(archive.getCore().getRowType(), archive.getCore().getTerms());
        String taxonID = null;
        for (ArchiveFile ext: archive.getExtensions())
            taxonomy.addOutputTerms(ext.getRowType(), ext.getTerms());
        try {
            for (StarRecord record : this.archive) {
                Record core = record.core();
                taxonID = core.value(DwcTerm.taxonID);
                NameProvider provider = taxonomy.resolveProvider(core.value(DwcTerm.datasetID), core.value(DwcTerm.datasetName));
                NomenclaturalCode code = taxonomy.resolveCode(core.value(DwcTerm.nomenclaturalCode));
                if (code == null) {
                    taxonomy.report(IssueType.PROBLEM, "taxonomy.load.nullCode", taxonID, core.value(DwcTerm.nomenclaturalCode));
                    code = provider.getDefaultNomenclaturalCode();
                    taxonomy.count("count.load.problem");
                }
                String scientificName = core.value(DwcTerm.scientificName);
                String scientificNameAuthorship = core.value(DwcTerm.scientificNameAuthorship);
                String year = core.value(DwcTerm.namePublishedInYear);
                TaxonomicType taxonomicStatus = taxonomy.resolveTaxonomicType(core.value(DwcTerm.taxonomicStatus));
                RankType rank = taxonomy.resolveRank(core.value(DwcTerm.taxonRank));
                Set<NomenclaturalStatus> nomenclaturalStatus = taxonomy.resolveNomenclaturalStatus(core.value(DwcTerm.nomenclaturalStatus));
                String parentNameUsageID = core.value(DwcTerm.parentNameUsageID);
                String acceptedNameUsageID = core.value(DwcTerm.acceptedNameUsageID);
                Map<Term, Optional<String>> classification = classifiers.stream().collect(Collectors.toMap(t -> t, t -> Optional.ofNullable(core.value(t))));
                TaxonConceptInstance instance = new TaxonConceptInstance(taxonID, code, provider, scientificName, scientificNameAuthorship, year, taxonomicStatus, rank, nomenclaturalStatus, parentNameUsageID, acceptedNameUsageID, classification);
                taxonomy.addInstance(instance);

                List<Document> docs = new ArrayList<>();
                docs.add(this.makeDocument(taxonomy, core));
                for (List<Record> ext: record.extensions().values()) {
                    for (Record er: ext) {
                        docs.add(makeDocument(taxonomy, er));
                    }
                }
                taxonomy.addRecords(docs);

            }
        } catch (Exception ex) {
            throw new IndexBuilderException("Unable to load archive " + this.archive.getLocation() + " at taxon " + taxonID, ex);
        }
    }

    /**
     * Convert a record into a lucene document
     *
     * @param taxonomy The target taxonomy
     * @param record The record
     *
     * @return The record as a document
     */
    private Document makeDocument(Taxonomy taxonomy, Record record) {
        Document doc = new Document();
        doc.add(new StringField("type", record.rowType().qualifiedName(), Field.Store.YES));
        doc.add(new StringField("id", UUID.randomUUID().toString(), Field.Store.YES));
        for (Term term: record.terms()) {
            String value = record.value(term);
            if (term != null && value != null && !value.isEmpty())
                doc.add(new StringField(taxonomy.fieldName(term), value, Field.Store.YES));
        }
        return doc;
    }
}
