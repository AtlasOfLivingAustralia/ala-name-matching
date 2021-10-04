package au.org.ala.names.util;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.gbif.api.model.registry.Dataset;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveField;
import org.gbif.dwca.io.ArchiveFile;
import org.gbif.dwca.io.MetaDescriptorWriter;
import org.gbif.dwca.record.Record;
import org.gbif.io.TabWriter;
import org.gbif.registry.metadata.EMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * A more flexible DwcaWriter, based on the GBIF DwcaWriter.
 * <p>
 * This is interface compatible with the GBIF DwcaWriter but allows such things as
 * non-linked extensions.
 * </p>
 *
 * @see org.gbif.dwca.io.DwcaWriter
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class DwcaWriter implements AutoCloseable {
    private Logger log = LoggerFactory.getLogger(DwcaWriter.class);
    private final File dir;
    private final boolean useHeaders;
    private long recordNum;
    private String coreId;
    private Map<Term, String> coreRow;
    private final Term coreRowType;
    private final Term coreIdTerm;
    private final Map<Term, TabWriter> writers = Maps.newHashMap();
    private final Set<Term> headersOut = Sets.newHashSet();
    private final Map<Term, String> dataFileNames = Maps.newHashMap();
    // key=rowType, value=columns
    private final Map<Term, List<Term>> terms = Maps.newHashMap();
    private final Map<Term, Boolean> detatchedExtension = Maps.newHashMap();
    // key=rowType, value=default values per column
    private final Map<Term, Map<Term, String>> defaultValues = Maps.newHashMap();
    private Dataset eml;

    /**
     * Creates a new writer without header rows.
     * @param coreRowType the core row type.
     * @param dir         the directory to create the archive in.
     */
    public DwcaWriter(Term coreRowType, File dir) throws IOException {
        this(coreRowType, dir, false);
    }

    /**
     * If headers are used the first record must include all terms ever used for that file.
     * If in subsequent rows additional terms are introduced an IllegalArgumentException is thrown.
     *
     * @param coreRowType    the core row type
     * @param dir            the directory to create the archive in
     * @param useHeaders if true the first row in every data file will include headers
     */
    public DwcaWriter(Term coreRowType, File dir, boolean useHeaders) throws IOException {
        this(coreRowType, null, dir, useHeaders);
    }

    /**
     * If headers are used the first record must include all terms ever used for that file.
     * If in subsequent rows additional terms are introduced an IllegalArgumentException is thrown.
     *
     * @param coreRowType the core row type
     * @param coreIdTerm the term of the id column
     * @param dir the directory to create the archive in
     * @param useHeaders if true the first row in every data file will include headers
     */
    public DwcaWriter(Term coreRowType, Term coreIdTerm, File dir, boolean useHeaders) throws IOException {
        this.dir = dir;
        this.coreRowType = coreRowType;
        this.coreIdTerm = coreIdTerm;
        this.useHeaders = useHeaders;
        addRowType(coreRowType, false);
    }

    public static Map<Term, String> recordToMap(Record rec, ArchiveFile af) {
        Map<Term, String> map = new HashMap<Term, String>();
        for (Term t : af.getTerms()) {
            map.put(t, rec.value(t));
        }
        return map;
    }

    public static String dataFileName(Term rowType) {
        return rowType.simpleName().toLowerCase() + ".txt";
    }

    protected void addRowType(Term rowType, boolean detatched) throws IOException {
        terms.put(rowType, new ArrayList<Term>());
        detatchedExtension.put(rowType, detatched);
        String dfn = dataFileName(rowType);
        dataFileNames.put(rowType, dfn);
        File df = new File(dir, dfn);
        org.apache.commons.io.FileUtils.forceMkdir(df.getParentFile());
        OutputStream out = new FileOutputStream(df);
        TabWriter wr = new TabWriter(out);
        writers.put(rowType, wr);
    }

    /**
     * A new core record is started and the last core and all extension records are written.
     * @param id the new records id
     * @throws IOException
     */
    public void newRecord(String id) throws IOException {
        // flush last record
        flushLastCoreRecord();
        // start new
        recordNum++;
        coreId = id;
        coreRow = new HashMap<Term, String>();
    }

    protected void flushLastCoreRecord() throws IOException {
        if (coreRow != null) {
            writeRow(coreRow, coreRowType, false);
        }
    }

    public long getRecordsWritten() {
        return recordNum;
    }

    protected void writeRow(Map<Term, String> rowMap, Term rowType, boolean detatched) throws IOException {
        TabWriter writer = writers.get(rowType);
        List<Term> columns = terms.get(rowType);
        if (useHeaders && !headersOut.contains(rowType)) {
            // write header row
            writeHeader(writer, rowType, columns, detatched);
        }

        String[] row;
        if (detatched) {
            row = new String[columns.size()];
            for (Map.Entry<Term, String> conceptTermStringEntry : rowMap.entrySet()) {
                int column = columns.indexOf(conceptTermStringEntry.getKey());
                row[column] = conceptTermStringEntry.getValue();
            }
        } else {
            // make sure coreId is not null for extensions
            if (coreRowType != rowType && coreId == null)
                log.warn("Adding an {} extension record to a core without an Id! Skip this record", rowType);
            row = new String[columns.size() + 1];
            row[0] = coreId;
            for (Map.Entry<Term, String> conceptTermStringEntry : rowMap.entrySet()) {
                int column = 1 + columns.indexOf(conceptTermStringEntry.getKey());
                row[column] = conceptTermStringEntry.getValue();
            }
        }
        writer.write(row);
    }

    protected void writeHeader(TabWriter writer, Term rowType, List<Term> columns, boolean detatched) throws IOException {
        int idx = 0;
        String[] row = new String[columns.size() + (detatched ? 0 : 1)];
        if (!detatched) {
            Term idTerm;
            if (DwcTerm.Taxon == coreRowType) {
                idTerm = DwcTerm.taxonID;
            } else if (DwcTerm.Occurrence == coreRowType) {
                idTerm = DwcTerm.occurrenceID;
            } else if (DwcTerm.Identification == coreRowType) {
                idTerm = DwcTerm.identificationID;
            } else if (DwcTerm.Event == coreRowType) {
                idTerm = DwcTerm.eventID;
            } else {
                // default to generic dc identifier for id column
                idTerm = DcTerm.identifier;
            }
            row[idx++] = idTerm.simpleName();
        }

        for (Term term : columns) {
            row[idx++] = term.simpleName();
        }
        writer.write(row);

        headersOut.add(rowType);
    }


    /**
     * Add a single value for the current core record.
     * Calling this method requires that #newRecord() has been called at least once,
     * otherwise an IllegalStateException is thrown.
     * @param term
     * @param value
     */
    public void addCoreColumn(Term term, String value) {
        // ensure we do not overwrite the coreIdTerm if one is defined
        if (coreIdTerm != null && coreIdTerm.equals(term)) {
            throw new IllegalStateException("You cannot add a term that was specified as coreId term");
        }

        List<Term> coreTerms = terms.get(coreRowType);
        if (!coreTerms.contains(term)) {
            if (recordNum>1){
                throw new IllegalStateException("You cannot add new terms after the first row when headers are enabled");
            }
            coreTerms.add(term);
        }
        try {
            coreRow.put(term, value);
        } catch (NullPointerException e) {
            // no core record has been started yet
            throw new IllegalStateException("No core record has been created yet. Call newRecord() at least once");
        }
    }

    /**
     * Add a default value to a term of the core.
     *
     * @param term
     * @param defaultValue
     */
    protected void addCoreDefaultValue(Term term, String defaultValue){
        addDefaultValue(coreRowType, term, defaultValue);
    }

    /**
     * Add a default value to a term of the provided rowType.
     *
     * @param rowType
     * @param term
     * @param defaultValue
     */
    public void addDefaultValue(Term rowType, Term term, String defaultValue){

        if(!defaultValues.containsKey(rowType)){
            defaultValues.put(rowType, new HashMap<Term, String>());
        }
        Map<Term,String> currentDefaultValues= defaultValues.get(rowType);
        if(currentDefaultValues.containsKey(term)){
            throw new IllegalStateException("The default value of term "+ term + " is already defined");
        }
        currentDefaultValues.put(term, defaultValue);
    }

    /**
     * @return new map of all current data file names by their rowTypes.
     */
    public Map<Term, String> getDataFiles() {
        return Maps.newHashMap(dataFileNames);
    }

    protected void addExtensionRecord(Term rowType, Map<Term, String> row, boolean detatched) throws IOException {
        // make sure we know the extension rowtype
        if (!terms.containsKey(rowType)) {
            addRowType(rowType, detatched);
        }

        // make sure we know all terms
        List<Term> knownTerms = terms.get(rowType);
        final boolean isFirst = knownTerms.isEmpty();
        for (Term term : row.keySet()) {
            if (!knownTerms.contains(term)) {
                if (!isFirst){
                    throw new IllegalStateException("You cannot add new terms after the first row when headers are enabled");
                }
                knownTerms.add(term);
            }
        }

        // write extension record
        writeRow(row, rowType, detatched);
    }

    /**
     * Add an extension record associated with the current core record.
     *
     * @param rowType
     * @param row
     * @throws IOException
     */
    public void addExtensionRecord(Term rowType, Map<Term, String> row) throws IOException {
        this.addExtensionRecord(rowType, row, false);
    }


    /**
     * Add an extension record not associated with a core record.
     *
     * @param rowType
     * @param row
     * @throws IOException
     */
    public void addDetatchedRecord(Term rowType, Map<Term, String> row) throws IOException {
        this.addExtensionRecord(rowType, row, true);
    }


    public void setEml(Dataset eml) {
        this.eml = eml;
    }

    /**
     * Writes meta.xml and eml.xml to the archive and closes tab writers.
     *
     * @deprecated Use {@link #close()} instead. This method will be removed in version 1.12.
     */
    @Deprecated
    public void finalize() throws IOException {
        close();
    }

    /**
     * Writes meta.xml and eml.xml to the archive and closes tab writers.
     */
    @Override
    public void close() throws IOException {
        try {
            addEml();
            addMeta();
            // flush last record
            flushLastCoreRecord();
            // TODO: add missing columns in second iteration of data files

         } finally {
            for (TabWriter w : writers.values()) {
                try {
                    w.close();
                } catch (Exception ex) {
                    log.error("Unable to close writer " + w, ex);
                }
            }
        }
    }

    protected void addEml() throws IOException {
        if (eml != null) {
            Writer writer = new FileWriter(new File(dir, "eml.xml"));
            EMLWriter.newInstance().writeTo(eml, writer);
        }
    }

    protected void addMeta() throws IOException {
        File metaFile = new File(dir, "meta.xml");

        Archive arch = new Archive();
        if (eml != null) {
            arch.setMetadataLocation("eml.xml");
        }
        arch.setCore(buildArchiveFile(arch, coreRowType, coreIdTerm));
        for (Term rowType : this.terms.keySet()) {
            if (!coreRowType.equals(rowType)) {
                Term idTerm = detatchedExtension.getOrDefault(rowType, false) ? terms.get(rowType).get(0) : null;
                arch.addExtension(buildArchiveFile(arch, rowType, idTerm));
            }
        }
        MetaDescriptorWriter.writeMetaFile(metaFile, arch);
    }

    protected ArchiveFile buildArchiveFile(Archive archive, Term rowType, Term idTerm) {
        ArchiveFile af = ArchiveFile.buildTabFile();
        af.setArchive(archive);
        af.addLocation(dataFileNames.get(rowType));

        af.setEncoding("utf-8");
        af.setIgnoreHeaderLines(useHeaders ? 1 : 0);
        af.setRowType(rowType);

        ArchiveField id = new ArchiveField();
        id.setIndex(0);
        af.setId(id);
        // id an idTerm is provided, always use the index 0
        if (idTerm != null) {
            ArchiveField field = new ArchiveField();
            field.setIndex(0);
            field.setTerm(idTerm);
            af.addField(field);
        }

        Map<Term,String> termDefaultValueMap = defaultValues.get(rowType);
        List<Term> rowTypeTerms = terms.get(rowType);
        int idx = 0;
        for (Term c : rowTypeTerms) {
            if (c == idTerm)
                continue;
            idx++;
            ArchiveField field = new ArchiveField();
            field.setIndex(idx);
            field.setTerm(c);
            if(termDefaultValueMap !=null && termDefaultValueMap.containsKey(c)){
                field.setDefaultValue(termDefaultValueMap.get(c));
            }
            af.addField(field);
        }

        // check if default values are provided for this rowType
        if(termDefaultValueMap != null){
            ArchiveField field = null;
            for (Term t : termDefaultValueMap.keySet()) {
                if(!rowTypeTerms.contains(t)){
                    field = new ArchiveField();
                    field.setTerm(t);
                    field.setDefaultValue(termDefaultValueMap.get(t));
                    af.addField(field);
                }
            }
        }

        return af;
    }

}
