package au.org.ala.names.index;

import au.ala.org.vocab.DocumentType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import au.org.ala.names.model.NameIndexField;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.util.BytesRef;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.text.Archive;
import org.gbif.dwc.text.ArchiveFactory;
import org.gbif.dwc.text.ArchiveFile;
import org.gbif.dwc.text.StarRecord;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * A source of names from a Darwin Core Archive
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2016 CSIRO
 */
public class DwcaNameSource extends NameSource {
    /** Fields expected in the DwCA */
    private static final Set<Term> TAXON_REQUIRED = new HashSet<Term>(Arrays.asList(
            DwcTerm.acceptedNameUsageID,
            DwcTerm.parentNameUsageID,
            DwcTerm.scientificName,
            DwcTerm.scientificNameAuthorship,
            DwcTerm.taxonomicStatus,
            DwcTerm.taxonRank
    ));
    /** Optional fields from the DwCA */
    private static final Set<Term> ADDITIONAL_FIELDS = new HashSet<Term>(Arrays.asList(
            DwcTerm.taxonConceptID,
            DwcTerm.acceptedNameUsageID,
            DwcTerm.parentNameUsageID,
            DwcTerm.scientificName,
            DwcTerm.scientificNameAuthorship,
            DwcTerm.taxonomicStatus,
            DwcTerm.taxonRank
    ));
    private static final Map<String, Set<Term>> REQUIRED_TERMS = MapUtils.putAll(new HashMap<String, Set<Term>>(),
            new Object[][] {
                    { DwcTerm.Taxon.qualifiedName(), TAXON_REQUIRED }
            }
    );
    private Archive archive;

    /**
     * Create a name source for an archive
     *
     * @param id The source identifier
     * @param priority The source priority
     * @param archive The source archive
     */
    public DwcaNameSource(String id, float priority, Archive archive) {
        super(id, priority);
        this.archive = archive;
        this.checkArchiveStructure();
    }

    /**
     * Create a name source for an archive in a working directory
     *
     * @param id The source identifier
     * @param priority The source priority
     * @param archiveDir The source archive directory
     */
    public DwcaNameSource(String id, float priority, File archiveDir) throws IOException {
        this(id, priority, ArchiveFactory.openArchive(archiveDir));
    }

    /**
     * Ensure that all the things we expect in an archive are
     *
     * @throws IndexBuilderException if ther archive is not usable
     */
    protected void checkArchiveStructure() throws IndexBuilderException {
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
        Set<Term> required = REQUIRED_TERMS.get(af.getRowType());

        if (required != null) {
            for (Term term: required)
                if (!af.hasTerm(term))
                    throw new IndexBuilderException("File " + af.getTitle() + " is of type " + af.getRowType() + " and is missing " + term);

        }
    }

    @Override
    public Iterator<Collection<Document>> iterator() {
        return new DwcaIterator();
    }

    private class DwcaIterator implements Iterator<Collection<Document>> {
        private Iterator<StarRecord> iterator;

        public DwcaIterator() {
            this.iterator = DwcaNameSource.this.archive.iterator();
        }

        @Override
        public boolean hasNext() {
            return this.iterator.hasNext();
        }


        /**
         * Create a list of documents for this entry.
         * <p>
         * This doesn't care what sort of documents are being created.
         * All it does is create a list of documents to add to the index.
         * This allows additional information to be added to something via additional archives,
         * rather than relying on a single archive containing everything.
         * </p>
         * <p>
         * If you do this, you'll need to ensure that the supplied taxonIDs line up.
         * Otherwise it will all vanish as there will be nothing to connect it to the taxon of choice.
         * </p>
         *
         * @return A collection of new documents.
         */
        @Override
        public Collection<Document> next() {
            List<Document> docs = Collections.emptyList();
            StarRecord record = this.iterator.next();
            Record core = record.core();
            String id = core.id();
            String taxonID = core.value(DwcTerm.taxonID);

            if (taxonID == null)
                taxonID = id;
            docs.add(this.create(core, core.rowType(), taxonID));
            for (Map.Entry<String, List<Record>> entry: record.extensions().entrySet()) {
                for (Record ext: entry.getValue())
                    docs.add(this.create(ext, entry.getKey(), id));
            }
            return docs;
        }

        protected Document create(Record record, String type, String taxonID) {
            if (DwcTerm.Taxon.qualifiedName().equals(type))
                return createTaxon(record, taxonID);
            else
                return createDetail(record, taxonID);
        }

        protected Document createTaxon(Record record, String taxonID) {
            Document doc = new Document();
            doc.add(new StringField(DcTerm.type.toString(), DwcTerm.Taxon.qualifiedName(), Field.Store.YES));
            for (Term t: record.terms()) {
                doc.add(new StringField(t.toString(), record.value(t), Field.Store.YES));
            }
            return doc;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("No remove for DwCA iterator");
        }
    }
}
