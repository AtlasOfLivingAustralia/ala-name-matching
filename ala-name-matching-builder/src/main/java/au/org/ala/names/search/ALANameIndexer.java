/*
 * Copyright (C) 2014 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */
package au.org.ala.names.search;

import au.org.ala.names.lucene.analyzer.LowerCaseKeywordAnalyzer;
import au.org.ala.names.model.*;
import au.org.ala.names.util.CleanedScientificName;
import au.org.ala.names.util.TaxonNameSoundEx;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveFactory;
import org.gbif.dwca.record.Record;
import org.gbif.nameparser.PhraseNameParser;

import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Creates the Lucene index based on the names that are exported from
 * http://code.google.com/p/ala-portal/source/browse/trunk/ala-names-generator/src/main/resources/create-dumps.sql
 * @Deprecated
 * @author Natasha
 */
public class ALANameIndexer {

    //private String cbExportFile = "cb_name_usages.txt";
    //private String lexFile = "cb_lex_names.txt";
    //private String irmngFile = "irmng_classification.txt";

    private String extraALAConcepts = "/data/bie-staging/ala-names/ala-extra.txt";
    private String alaConcepts = "/data/bie-staging/ala-names/ala_accepted_concepts_dump.txt";
    private String alaSynonyms = "/data/bie-staging/ala-names/ala_synonyms_dump.txt";
    private String irmngDwcaDirectory = "/data/bie-staging/irmng/IRMNG_DWC_HOMONYMS";

    private String colFile = "/data/bie-staging/ala-names/col_common_names.txt";
    private String afdFile = "/data/bie-staging/anbg/AFD-common-names.csv";
    private String apniFile = "/data/bie-staging/anbg/APNI-common-names.csv";
    private Log log = LogFactory.getLog(ALANameIndexer.class);
    // protected ApplicationContext context;
    //private DataSource dataSource;
    // protected JdbcTemplate dTemplate;
    private IndexSearcher idSearcher;
    //the position in the line for each of the required values

    private final int POS_ID = 0;
    private final int POS_PARENT_ID = 1;
    private final int POS_LSID = 2;
    private final int POS_PARETN_LSID = 3;
    private final int POS_ACC_LSID = 4;
    private final int POS_NAME_LSID = 5;
    private final int POS_SCI_NAME = 6;
    private final int POS_GENUS_OR_HIGHER = 7;
    private final int POS_SP_EPITHET = 8;
    private final int POS_INFRA_EPITHET = 9;
    private final int POS_AUTHOR = 10;
    private final int POS_AUTHOR_YEAR = 11;
    private final int POS_RANK_ID = 12;
    private final int POS_RANK = 13;
    private final int POS_LFT = 14;
    private final int POS_RGT = 15;
    private final int POS_KID = 16;
    private final int POS_K = 17;
    private final int POS_PID = 18;
    private final int POS_P = 19;
    private final int POS_CID = 20;
    private final int POS_C = 21;
    private final int POS_OID = 22;
    private final int POS_O = 23;
    private final int POS_FID = 24;
    private final int POS_F = 25;
    private final int POS_GID = 26;
    private final int POS_G = 27;
    private final int POS_SID = 28;
    private final int POS_S = 29;
    private final int POS_SRC = 30;
    private final int POS_EXCLUDED = 36;

    private String indexDirectory;
    private IndexWriter cbIndexWriter;

    PhraseNameParser parser = new PhraseNameParser();
    Set<String> knownHomonyms = new HashSet<String>();
    Set<String> blacklist = new HashSet<String>();

    public void init() throws Exception {

        // init the known homonyms
        LineIterator lines = new LineIterator(new BufferedReader(
                new InputStreamReader(
                        this.getClass().getClassLoader().getResource(
                                "au/org/ala/propertystore/known_homonyms.txt").openStream(), "ISO-8859-1")));
        LineIterator blines = new LineIterator(new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResource("blacklist.txt").openStream())));
        try {
            //load known homonyms
            while (lines.hasNext()) {
                String line = lines.nextLine().trim();
                knownHomonyms.add(line.toUpperCase());
            }
            //load the blacklist
            while (blines.hasNext()) {
                String line = blines.nextLine().trim();
                if (!line.startsWith("#") && StringUtils.isNotBlank(line))
                    blacklist.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lines.close();
            blines.close();
        }
    }

    /**
     * Creates the index from the specified checklist bank names usage export file into
     * the specified index directory.
     *
     * @param exportsDir   The directory that contains the exports that are necesary to generate the index.
     * @param indexDir     The directory in which the 2 indices will be created.
     * @param generateSciNames true when the scientific name index should be created
     * @param generateCommonNames true when the common name index should be generated
     * @throws Exception
     */
    public void createIndex(String exportsDir, String indexDir, boolean generateSciNames, boolean generateCommonNames)
            throws Exception {
        createIndex(exportsDir, indexDir, alaConcepts, alaSynonyms, irmngDwcaDirectory, generateSciNames, generateCommonNames);
    }

    /**
     * Creates the IRMNG homonym index based on the DWCA and species homonyms supplied from the NSL
     * @param exportsDir
     * @param indexDir
     * @throws Exception
     */
    public void createIrmngIndex(String exportsDir, String indexDir) throws Exception {
        Analyzer analyzer = LowerCaseKeywordAnalyzer.newInstance();
        IndexWriter irmngWriter = createIndexWriter(new File(indexDir + File.separator + "irmng"), analyzer, true);
        indexIrmngDwcA(irmngWriter, irmngDwcaDirectory);
        indexIRMNG(irmngWriter, exportsDir + File.separator + "ala-species-homonyms.txt", RankType.SPECIES);
        irmngWriter.forceMerge(1);
        irmngWriter.close();
    }

    public void createIndex(String exportsDir, String indexDir, String acceptedFile, String synonymFile,
                            String irmngDwca, boolean generateSciNames, boolean generateCommonNames) throws Exception {

        Analyzer analyzer = LowerCaseKeywordAnalyzer.newInstance();
        //generate the extra id index
        createExtraIdIndex(indexDir + File.separator + "id", new File(exportsDir + File.separator + "identifiers.txt"));
        if (generateSciNames) {
            indexALA(createIndexWriter(new File(indexDir + File.separator + "cb"), analyzer, true), acceptedFile, synonymFile);
            //IRMNG index to aid in the resolving of homonyms
            IndexWriter irmngWriter = createIndexWriter(new File(indexDir + File.separator + "irmng"), analyzer, true);
            indexIrmngDwcA(irmngWriter, irmngDwca);

            indexIRMNG(irmngWriter, exportsDir + File.separator + "ala-species-homonyms.txt", RankType.SPECIES);
            irmngWriter.forceMerge(1);
            irmngWriter.close();
        }
        if (generateCommonNames) {
            //vernacular index to search for common names
            indexCommonNames(createIndexWriter(new File(indexDir + File.separator + "vernacular"),
                    new KeywordAnalyzer(), true), exportsDir, indexDir);
        }
    }

    /**
     * Creates the temporary index that provides a lookup of checklist bank id to
     * GUID
     */
    private IndexSearcher createTmpGuidIndex(String cbExportFile) throws Exception {
        System.out.println("Starting to create the tmp guid index...");
        IndexWriter iw = createIndexWriter(new File("/data/tmp/guid"), new KeywordAnalyzer(), true);
        CSVReader cbreader = this.buildCSVReader(cbExportFile, '\t', '\\', '"', 1);

        for (String[] values = cbreader.readNext(); values != null; values = cbreader.readNext()) {
            Document doc = new Document();
            String id = values[POS_ID];
            String guid = values[POS_LSID];
            NameIndexField.ID.store(id, doc);
             if (StringUtils.isEmpty(id))
                guid = id;
            NameIndexField.GUID.store(guid, doc);
            iw.addDocument(doc);
        }
        System.out.println("Finished writing the tmp guid index...");
        iw.commit();
        iw.forceMerge(1);
        iw.close();
        //As of lucene 4.0 all IndexReaders are read only
        return new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File("/data/tmp/guid").toPath())));
    }

    /**
     * Creates an index writer in the specified directory.  It will create/recreate
     * the target directory
     *
     * @param directory
     * @param analyzer
     * @return
     * @throws Exception
     */
    protected IndexWriter createIndexWriter(File directory, Analyzer analyzer, boolean replace) throws Exception {
        IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        if (replace)
            conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        else
            conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        if (directory.exists() && replace) {
            FileUtils.forceDelete(directory);
        }
        FileUtils.forceMkdir(directory);
        return new IndexWriter(FSDirectory.open(directory.toPath()), conf);
    }

    /**
     * Adds the extra ALA concepts from the legislated lists that are missing from the NSL.
     *
     * @param iw
     * @param file
     * @throws Exception
     */
    private void addExtraALAConcept(IndexWriter iw, String file) throws Exception {
        if (new File(file).exists()) {
            CSVReader reader = this.buildCSVReader(file, ',', '"', '\\', 1);
            for (String[] values = reader.readNext(); values != null; values = reader.readNext()) {
                String lsid = values[0];
                String scientificName = values[1];
                String authority = values[2];
                Document doc = createALAIndexDocument(scientificName, "-1", lsid, authority, null);
                iw.addDocument(doc);
            }
        }
    }

    private void addALASyonyms(IndexWriter iw, String file) throws Exception {
        CSVReader reader = this.buildCSVReader(file, '\t', '"', '\\', 1);
        for (String[] values = reader.readNext(); values != null; values = reader.readNext()) {

            String source = values[11];
            //give CoL synonyms a lower boost than NSL
            int priority = source.trim().equals("") || source.equalsIgnoreCase("CoL") ? MatchMetrics.DEFAULT_PRIORITY * 3 / 4 : MatchMetrics.DEFAULT_PRIORITY;
            Document doc = createALASynonymDocument(values[5], values[6], null, null, values[0], values[1], values[2], values[3], values[4], priority, values[9]);
            if (doc != null)
                iw.addDocument(doc);
        }
    }

    private void indexALA(IndexWriter iw, String file, String synonymFile) throws Exception {
        int records = 0;
        long time = System.currentTimeMillis();
        CSVReader reader = this.buildCSVReader(file, '\t', '"', '\\', 1);
        for (String[] values = reader.readNext(); values != null; values = reader.readNext()) {

            String lsid = values[POS_LSID];
            String id = values[POS_ID];
            int rankId = -1;
            try {
                rankId = Integer.parseInt(values[POS_RANK_ID]);
            } catch (Exception e) {
            }

            String acceptedValues = values[POS_ACC_LSID];
            float boost = 1.0f;
            // reduce minor ranks
            if (rankId % 1000 != 0) {
                boost *= 0.2f;
            }
            //give reduce non-col concepts a higher boost
            String source = values[POS_SRC];
            if (source.trim().equals("") || source.equalsIgnoreCase("CoL")) {
                boost *= 0.5f;
            }
            int priority = Math.round(boost * MatchMetrics.DEFAULT_PRIORITY);


            Document doc = createALAIndexDocument(values[POS_SCI_NAME], id, lsid, values[POS_RANK_ID],
                    values[POS_RANK], values[POS_K], values[POS_KID], values[POS_P],
                    values[POS_PID], values[POS_C], values[POS_CID],
                    values[POS_O], values[POS_OID], values[POS_F], values[POS_FID],
                    values[POS_G], values[POS_GID], values[POS_S], values[POS_SID],
                    Integer.parseInt(values[POS_LFT]), Integer.parseInt(values[POS_RGT]), acceptedValues,
                    values[POS_SP_EPITHET], values[POS_INFRA_EPITHET], values[POS_AUTHOR], null, null, priority);


            //add the excluded information if applicable
            if ("T".equals(values[POS_EXCLUDED]) || "Y".equals(values[POS_EXCLUDED])) {
                NameIndexField.SYNONYM_TYPE.store(SynonymType.EXCLUDES.getId().toString(), doc);
            }
            if (doc != null) {
                iw.addDocument(doc);
                records++;
                if (records % 100000 == 0) {
                    log.info("Processed " + records + " in " + (System.currentTimeMillis() - time) + " msecs");
                }
            }
        }
        addExtraALAConcept(iw, extraALAConcepts);
        //add the synonyms
        addALASyonyms(iw, synonymFile);
        iw.commit();
        iw.forceMerge(1);
        iw.close();
        log.info("Lucene index created - processed a total of " + records + " records in " + (System.currentTimeMillis() - time) + " msecs ");
    }

    public void addAdditionalName(String lsid, String scientificName, String author, LinnaeanRankClassification cl) throws Exception {

        if (cbIndexWriter == null)
            cbIndexWriter = createIndexWriter(new File(indexDirectory + File.separator + "cb"), LowerCaseKeywordAnalyzer.newInstance(), false);
        Document doc = createALAIndexDocument(scientificName, "-1", lsid, author, cl);
        cbIndexWriter.addDocument(doc);

    }

    /**
     * Deletes the entry that has the supplied lsid. It will also delete all the synonyms associated with it
     * @param lsid
     * @throws Exception
     */
    public void deleteName(String lsid) throws Exception{
        if(cbIndexWriter == null){
            cbIndexWriter = createIndexWriter(new File(indexDirectory+ File.separator + "cb"), LowerCaseKeywordAnalyzer.newInstance(), false);
        }
        Term term = new Term("lsid", lsid);
        cbIndexWriter.deleteDocuments(new TermQuery(term));
        term = new Term("accepted_lsid", lsid);
        cbIndexWriter.deleteDocuments(new TermQuery(term));

    }

    public void commit() throws Exception{
        commit(false, false);
    }

    /**
     *
     * @param merge whether or not to merge the index
     * @param close whether or not to close the index
     * @throws Exception
     */
    public void commit(boolean close, boolean merge) throws Exception{
        if(cbIndexWriter !=  null){
            cbIndexWriter.commit();
            if(merge){
                cbIndexWriter.forceMerge(1);
            }
            if(close){
                cbIndexWriter.close();
            }
        }
    }

    /**
     * Indexes the IRMNG homonyms from the supplied DWCA direcory
     * <p>
     * Only accept those with a taxonomic status of accepted
     * </p>
     * @param iw The index writer to write the lucene docs to
     * @param archiveDirectory  The directory in which the IRMNG DWCA has been unzipped.
     * @throws Exception
     */
    protected void indexIrmngDwcA(IndexWriter iw, String archiveDirectory) throws Exception {
        log.info("Creating the IRMNG index from the DWCA " + archiveDirectory);
        //open the archive to extract the required information
        Archive archive = ArchiveFactory.openArchive(new File(archiveDirectory));
        Iterator<Record> it = archive.getCore().iterator();
        while (it.hasNext()) {
            Record dwcr = it.next();
            String taxonomicStatus = dwcr.value(DwcTerm.taxonomicStatus);
            if (taxonomicStatus == null || !taxonomicStatus.equalsIgnoreCase("accepted"))
                continue;
            Document doc = new Document();
            String kingdom = dwcr.value(DwcTerm.kingdom);
            if (StringUtils.isNotEmpty(kingdom)) {
                NameIndexField.KINGDOM.store(kingdom, doc);
            }
            String phylum = dwcr.value(DwcTerm.phylum);
            if (StringUtils.isNotEmpty(phylum)) {
                NameIndexField.PHYLUM.store(phylum, doc);
            }
            String classs = dwcr.value(DwcTerm.class_);
            if (StringUtils.isNotEmpty(classs)) {
                NameIndexField.CLASS.store(classs, doc);
            }
            String order = dwcr.value(DwcTerm.order);
            if (StringUtils.isNotEmpty(order)) {
                NameIndexField.ORDER.store(order, doc);
            }
            String family = dwcr.value(DwcTerm.family);
            if (StringUtils.isNotEmpty(family)) {
                NameIndexField.FAMILY.store(kingdom, doc);
            }
            String genus = dwcr.value(DwcTerm.genus);
            String calculatedRank = "genus";
            if (StringUtils.isNotEmpty(genus)) {
                NameIndexField.GENUS.store(genus, doc);
                String specificEpithet = dwcr.value(DwcTerm.specificEpithet);
                if (StringUtils.isNotEmpty(specificEpithet)) {
                    calculatedRank = "species";
                    NameIndexField.SPECIES.store(genus + " " + specificEpithet, doc);
                }
            }
            String rank = dwcr.value(DwcTerm.taxonRank);
            if (StringUtils.isEmpty(rank))
                rank = calculatedRank;
            NameIndexField.RANK.store(rank, doc);
            //now add the author - we don't do anything about this on homonym resolution yet
            //Add the author information
            String author = dwcr.value(DwcTerm.scientificNameAuthorship);
            if (StringUtils.isNotEmpty(author)) {
                //TODO think about whether we need to treat the author string with the taxamatch
                NameIndexField.AUTHOR.store(author, doc);
            }
            //now add it to the index
            iw.addDocument(doc);

        }
    }

    /**
     * Indexes an IRMNG export for use in homonym resolution.
     *
     * @param iw
     * @param irmngExport
     * @throws Exception
     */
    void indexIRMNG(IndexWriter iw, String irmngExport, RankType rank) throws Exception {
        log.info("Creating IRMNG index ...");
        File file = new File(irmngExport);
        if (file.exists()) {
            CSVReader reader = this.buildCSVReader(irmngExport, '\t', '"', '~', CSVReader.DEFAULT_SKIP_LINES);
            int count = 0;
            String[] values = null;
            while ((values = reader.readNext()) != null) {
                Document doc = new Document();
                if (values != null && values.length >= 7) {
                    NameIndexField.KINGDOM.store(values[0], doc);
                    NameIndexField.PHYLUM.store(values[1], doc);
                    NameIndexField.CLASS.store(values[2], doc);
                    NameIndexField.ORDER.store(values[3], doc);
                    NameIndexField.FAMILY.store(values[4], doc);
                    NameIndexField.GENUS.store(values[5], doc);
                    if (rank == RankType.GENUS) {

                        NameIndexField.ID.store(values[6], doc);
                        NameIndexField.ACCEPTED.store(values[8], doc);
                        NameIndexField.HOMONYM.store(values[10], doc);
                    } else if (rank == RankType.SPECIES) {
                        NameIndexField.SPECIES.store(values[6], doc);
                    }
                    NameIndexField.RANK.store(rank.getRank(), doc);
                    iw.addDocument(doc);
                    count++;
                }
            }
            iw.commit();

            log.info("Finished indexing " + count + " IRMNG " + rank + " taxa.");
        } else
            log.warn("Unable to create IRMNG index.  Can't locate " + irmngExport);
    }

    /**
     * Indexes common names from CoL and ANBG for use in the Common name search.
     *
     * @param iw  The index writer to write the common documents to
     * @param exportDir  The directory that contains the common name export files.
     * @param indexDir The directory in which to create the index.
     * @throws Exception
     */
    private void indexCommonNames(IndexWriter iw, String exportDir, String indexDir) throws Exception {
        log.info("Creating Common Names Index ...");

        IndexSearcher currentNameSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(indexDir + File.separator + "cb").toPath())));
        IndexSearcher extraSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(indexDir + File.separator + "id").toPath())));

        addCoLCommonNames(iw, currentNameSearcher);
        addAnbgCommonNames(afdFile, iw, currentNameSearcher, extraSearcher, '\t');
        addAnbgCommonNames(apniFile, iw, currentNameSearcher, extraSearcher, ',');

        iw.commit();
        iw.forceMerge(1);
        iw.close();
    }

    /**
     * Adds the CoL common names to the common name index.
     * @param iw
     * @param currentSearcher
     * @throws Exception
     *
     */
    private void addCoLCommonNames(IndexWriter iw, IndexSearcher currentSearcher) throws Exception {
        File fileCol = new File(colFile);
        if (fileCol.exists()) {
            CSVReader reader = this.buildCSVReader(colFile, ',', '"', '~', CSVReader.DEFAULT_SKIP_LINES);
            int count = 0;
            String[] values = null;
            while ((values = reader.readNext()) != null) {
                if (values.length == 3) {
                    if (doesTaxonConceptExist(currentSearcher, values[2])) {
                        iw.addDocument(createCommonNameDocument(values[0], values[1], values[2], null));
                        count++;
                    } else {
                        System.out.println("Unable to locate LSID " + values[2] + " in current dump");
                    }
                }
            }
            log.info("Finished indexing " + count + " common names from " + fileCol);
        } else {
            log.warn("Unable to index common names. Unable to locate : " + fileCol);
        }
    }

    /**
     * Adds an ANBG CSV file of common names to the common name index.
     *
     * @param fileName The file name to add to the common name index
     * @param iw  The index writer to write the common documents to
     * @param currentSearcher The searcher to find a scientific name
     * @param idSearcher  The searcher to find an lsid
     * @param recordSep The record separator for the CSV file
     * @throws Exception
     */
    private void addAnbgCommonNames(String fileName, IndexWriter iw, IndexSearcher currentSearcher, IndexSearcher idSearcher, char recordSep) throws Exception {
        File namesFile = new File(fileName);
        Pattern p = Pattern.compile(",");
        if (namesFile.exists()) {
            CSVReader reader = this.buildCSVReader(fileName, recordSep, '"', '\\', CSVReader.DEFAULT_SKIP_LINES);
            int count = 0;
            String[] values = reader.readNext();
            while ((values = reader.readNext()) != null) {
                if (values != null && values.length >= 4) {
                    //all ANBG records should have the highest boost as they are our authoritive source
                    //we only want to add an ANBG record if the taxon concept LSID exists in the taxonConcepts.txt export
                    if (doesTaxonConceptExist(currentSearcher, values[3]) || doesTaxonConceptExist(idSearcher, values[3])) {
                        //each common name could be a comma separated list
                        if (!values[2].contains(",") || values[2].toLowerCase().contains(" and ")) {
                            iw.addDocument(createCommonNameDocument(values[2], null, values[3], null));
                            count++;
                        } else {
                            //we need to process each common name in the list
                            String[] names = p.split(values[2]);
                            for (String name : names) {
                                iw.addDocument(createCommonNameDocument(name, null, values[3],null));
                                count++;
                            }
                        }
                    } else {
                        System.out.println("Unable to locate LSID " + values[3] + " in current dump");
                    }
                }

            }
            log.info("Finished indexing " + count + " common names from " + fileName);
        } else
            log.warn("Unable to index common names. Unable to locate : " + fileName);
    }

    /**
     * Creates a temporary index that will provide a lookup up of lsid to "real lsid".
     * <p/>
     * This deals with the following situations:
     * - common names that are sourced from CoL (LSIDs will be mapped to corresponding ANBG LSID)
     * - Multiple ANBG LSIDs exist for the same scientific name and more than 1 are mapped to the same common name.
     * <p>
     * The {@link #idSearcher} is set after creating this index
     * </p>
     *
     * @param iw The index writer
     * @param idFile The file containing additional ids
     *
     * @throws Exception
     */
    protected void createExtraIdIndex(IndexWriter iw, File idFile) throws Exception {
        String[] values = null;

        if(idFile.exists()) {
            CSVReader reader = this.buildCSVReader(idFile.getPath(), '\t', '"', '~', CSVReader.DEFAULT_SKIP_LINES);
            while ((values = reader.readNext()) != null) {

                if (values != null && values.length >= 3) {
                    Document doc = new Document();
                    //doc.add(new Field("lsid", values[2], Store.NO, Index.NOT_ANALYZED));
                    NameIndexField.LSID.store(values[2], doc);
                    //doc.add(new Field("reallsid", values[1], Store.YES, Index.NO));
                    NameIndexField.REAL_LSID.store(values[1], doc);
                    iw.addDocument(doc);
                }
            }
        }
        iw.flush();
        iw.commit();
        iw.forceMerge(1);
        idSearcher = new IndexSearcher(DirectoryReader.open(iw.getDirectory()));
    }

    /**
     * Creates a temporary index that will provide a lookup up of lsid to "real lsid".
     * <p/>
     * This deals with the following situations:
     * - common names that are sourced from CoL (LSIDs will be mapped to corresponding ANBG LSID)
     * - Multiple ANBG LSIDs exist for the same scientific name and more than 1 are mapped to the same common name.
     *
     * @param idFile
     * @throws Exception
     */
    protected void createExtraIdIndex(String idxLocation, File idFile) throws Exception {
        File indexDir = new File(idxLocation);
        IndexWriter iw = this.createIndexWriter(indexDir, new KeywordAnalyzer(), true);
        this.createExtraIdIndex(iw, idFile);
        iw.close();
    }

    /**
     * Creates a temporary index that stores the taxon concept LSIDs that were
     * included in the last ANBG exports.
     *
     * @param tcFileName
     * @return
     * @throws Exception
     */
    private IndexSearcher createTmpIndex(String tcFileName) throws Exception {
        //creating the tmp index in the /tmp/taxonConcept directory
        CSVReader reader = this.buildCSVReader(tcFileName, '\t', '"', '~', CSVReader.DEFAULT_SKIP_LINES);
        File indexDir = new File("/tmp/taxonConcept");
        IndexWriter iw = createIndexWriter(indexDir, new KeywordAnalyzer(), true);
        String[] values = null;
        while ((values = reader.readNext()) != null) {
            if (values != null && values.length > 1) {
                //just add the LSID to the index
                Document doc = new Document();

                NameIndexField.LSID.store(values[0], doc);
                iw.addDocument(doc);

            }
        }
        iw.commit();
        iw.forceMerge(1);
        iw.close();
        return new IndexSearcher(DirectoryReader.open(FSDirectory.open(indexDir.toPath())));
    }

    /**
     * Determines whether or not the supplied taxon lsid was included in the
     * latest ANBG exports.
     *
     * @param is
     * @param lsid
     * @return
     */
    private boolean doesTaxonConceptExist(IndexSearcher is, String lsid) {
        TermQuery query = new TermQuery(new Term("lsid", lsid));
        try {
            org.apache.lucene.search.TopDocs results = is.search(query, 1);
            return results.totalHits.value > 0;
        } catch (IOException e) {
            return false;
        }

    }

    /**
     * Uses the id index to find the accepted id for the supplied LSID.
     * <p/>
     * When no accepted id can be found the original LSID is returned.
     *
     * @param value
     * @return
     */
    private String getAcceptedLSID(String value) {
        if(idSearcher != null) {
            try {
                TermQuery tq = new TermQuery(new Term("lsid", value));
                org.apache.lucene.search.TopDocs results = idSearcher.search(tq, 1);
                if (results.totalHits.value > 0)
                    return idSearcher.doc(results.scoreDocs[0].doc).get("reallsid");
            } catch (IOException e) {
            }
        }
        return value;
    }

    protected Document createCommonNameDocument(String cn, String sn, String lsid, String language){
        return createCommonNameDocument(cn, sn, lsid, language, true);
    }

    protected Document createCommonNameDocument(String cn, String sn, String lsid, String language, boolean checkAccepted) {
        Document doc = new Document();
        // Uses field type to normalise
        NameIndexField.SEARCHABLE_COMMON_NAME.store(cn, doc);

        if (sn != null) {
            NameIndexField.NAME.store(sn, doc);
        }

        String newLsid = getAcceptedLSID(lsid);
        NameIndexField.COMMON_NAME.store(cn, doc);
        NameIndexField.LSID.store(newLsid, doc);
        if(language != null) {
            NameIndexField.LANGUAGE.store(language.toLowerCase().trim(), doc);
         }

        return doc;
    }

    public Document createALAIndexDocument(String name, String id, String lsid, String author, LinnaeanRankClassification cl){
        return createALAIndexDocument(name,id, lsid, author,null,null, 0, 0, cl, null, null, MatchMetrics.DEFAULT_PRIORITY);
    }

    public Document createALAIndexDocument(String name, String id, String lsid, String author, String rank, String rankId, int left, int right, LinnaeanRankClassification cl, String nameComplete, Collection<String> otherNames, int priority){
        if(cl == null)
            cl = new LinnaeanRankClassification();
        return createALAIndexDocument(name, id, lsid, rankId, rank, cl.getKingdom(), cl.getKid(), cl.getPhylum()
                , cl.getPid(), cl.getKlass(), cl.getCid(), cl.getOrder(), cl.getOid(), cl.getFamily(),
                cl.getFid(), cl.getGenus(), cl.getGid(), cl.getSpecies(), cl.getSid(), left, right, null, null, null, author, nameComplete, otherNames, priority);
    }

    protected Document createALASynonymDocument(String scientificName, String author, String nameComplete, Collection<String> otherNames, String id, String lsid, String nameLsid, String acceptedLsid, String acceptedId, int priority, String synonymType) {
        lsid = StringUtils.isBlank(lsid) ? nameLsid : lsid;
        Document doc = createALAIndexDocument(scientificName, id, lsid, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, 0,
                acceptedLsid, null, null, author, nameComplete, otherNames, priority);
        if (doc != null && synonymType != null) {
            try {
                NameIndexField.SYNONYM_TYPE.store(synonymType, doc);
            } catch (Exception e) {
                System.out.println("Error on " + scientificName + " " + author + " " + id + ".  " + e.getMessage());
            }
        }
        return doc;
    }

    private boolean isBlacklisted(String scientificName) {
        return scientificName == null || blacklist.contains(scientificName.trim());
    }

    protected Document createALAIndexDocument(String name, String id, String lsid, String rank, String rankString,
                                            String kingdom, String kid, String phylum, String pid, String clazz, String cid, String order,
                                            String oid, String family, String fid, String genus, String gid,
                                            String species, String sid, int left, int right, String acceptedConcept, String specificEpithet,
                                            String infraspecificEpithet, String author, String nameComplete, Collection<String> otherNames,
                                            int priority) {
        //
        if (isBlacklisted(name)) {
            System.out.println(name + " has been blacklisted");
            return null;
        }

        int rankIndex = rank == null || rankString.isEmpty() ? -1 : Integer.parseInt(rank);
        nameComplete = buildNameComplete(name, author, nameComplete);
        CleanedScientificName cname = new CleanedScientificName(name);
        CleanedScientificName cnameComplete = new CleanedScientificName(nameComplete);
        Document doc = new Document();
        String soundexGenus = genus;

        //Add the ids
        NameIndexField.ID.store(id, doc);
        NameIndexField.LSID.store(lsid, doc);
        if (lsid.startsWith("ALA")) {
            NameIndexField.ALA.store("T", doc);
        }


        HashSet<String> nameSet = otherNames != null ? new HashSet<String>(otherNames) : new HashSet<>();
        nameSet.add(cname.getName());
        nameSet.add(cname.getNormalised());
        nameSet.add(cname.getBasic());
        nameSet.add(cnameComplete.getName());
        nameSet.add(cnameComplete.getNormalised());
        nameSet.add(cnameComplete.getBasic());
        for (String n: nameSet) {
            NameIndexField.NAME.store(n, doc);
        }
        NameIndexField.NAME_CANONICAL.store(cname.getNormalised(), doc);
        NameIndexField.NAME_COMPLETE.store(cnameComplete.getNormalised(), doc);

        //rank information
        if (rankIndex >= 0) {
            NameIndexField.RANK_ID.store(rankIndex, doc);
        }
        if (StringUtils.isNotEmpty(rankString)) {
            NameIndexField.RANK.store(rankString, doc);
        }


        //handle the synonyms
        if (StringUtils.isNotEmpty(acceptedConcept)) {
            NameIndexField.ACCEPTED.store(acceptedConcept, doc);
            NameIndexField.iS_SYNONYM.store("T", doc);
         } else {
            NameIndexField.iS_SYNONYM.store("F", doc);
        }

        //Add the classification information
        if (StringUtils.trimToNull(kingdom) != null) {
            NameIndexField.KINGDOM.store(kingdom, doc);
             if (StringUtils.isNotBlank(kid)) {
                 NameIndexField.KINGDOM_ID.store(kid, doc);
            }
        }
        if (StringUtils.trimToNull(phylum) != null) {
            NameIndexField.PHYLUM.store(phylum, doc);
            if (StringUtils.isNotBlank(pid)) {
                NameIndexField.PHYLUM_ID.store(pid, doc);
            }
        }
        if (StringUtils.trimToNull(clazz) != null) {
            NameIndexField.CLASS.store(clazz, doc);
            if (StringUtils.isNotBlank(cid)) {
                NameIndexField.CLASS_ID.store(cid, doc);
            }
        }
        if (StringUtils.trimToNull(order) != null) {
            NameIndexField.ORDER.store(order, doc);
            if (StringUtils.isNotBlank(oid)) {
                NameIndexField.ORDER_ID.store(oid, doc);
            }
        }
        if (StringUtils.trimToNull(family) != null) {
            NameIndexField.FAMILY.store(family, doc);
            if (StringUtils.isNotBlank(fid)) {
                NameIndexField.FAMILY_ID.store(fid, doc);
            }
        }
        if (StringUtils.trimToNull(genus) != null) {
            NameIndexField.GENUS.store(genus, doc);
            if (StringUtils.isNotBlank(gid)) {
                NameIndexField.GENUS_ID.store(gid, doc);
            }
        }
        if (StringUtils.trimToNull(species) != null) {
            NameIndexField.SPECIES.store(species, doc);
            if (StringUtils.isNotBlank(sid)) {
                NameIndexField.SPECIES_ID.store(sid, doc);
            }
        }
        if (left > 0) {
            NameIndexField.LEFT.store(left, doc);
        }
        if (right > 0) {
            NameIndexField.RIGHT.store(right, doc);
        }
        NameIndexField.PRIORITY.store(priority, doc);

        //Add the author information
        if (StringUtils.isNotEmpty(author)) {
            //TODO think about whether we need to treat the author string with the taxamatch
            NameIndexField.AUTHOR.store(author, doc);
        }


        //Generate the canonical
        //add the canonical form of the name
        try {
            ParsedName cn = parser.parse(cname.getNormalised());
            //if(cn != null && !cn.hasProblem() && !cn.isIndetermined()){
            if (cn != null && cn.isParsableType() && !cn.isIndetermined()
                    // a scientific name with some informal addition like "cf." or indetermined like Abies spec.
                    // ALSO prevent subgenus because they parse down to genus plus author
                    && cn.getType() != NameType.INFORMAL && !"6500".equals(rank) && cn.getType() != NameType.DOUBTFUL)
            {
                if (!nameSet.contains(cn.canonicalName())) {
                    NameIndexField.NAME.store(cn.canonicalName(), doc);
                }
                if (specificEpithet == null && cn.isBinomial()) {
                    //check to see if we need to determine the epithets from the parse
                    soundexGenus = cn.getGenusOrAbove();
                    if (specificEpithet == null) specificEpithet = cn.getSpecificEpithet();
                    if (infraspecificEpithet == null) infraspecificEpithet = cn.getInfraSpecificEpithet();
                }
            }
            //check to see if the concept represents a phrase name
            if (cn != null && cn instanceof ALAParsedName) {
                //set up the field type that is stored and Index.ANALYZED_NO_NORMS
                ALAParsedName alapn = (ALAParsedName) cn;
                if (alapn.getRank() != Rank.SPECIES && alapn.getSpecificEpithet() != null) {
                    NameIndexField.SPECIFIC.store(alapn.getSpecificEpithet(), doc);
                } else if (alapn.getRank() != Rank.SPECIES && alapn.getSpecificEpithet() == null) {
                    log.warn(lsid + " " + name + " has an empty specific for non sp. phrase");
                }
                if (StringUtils.trimToNull(alapn.getLocationPhraseDescription()) != null) {
                    NameIndexField.PHRASE.store(alapn.cleanPhrase, doc);
                }
                if (alapn.getPhraseVoucher() != null) {
                    NameIndexField.VOUCHER.store(alapn.cleanVoucher, doc);
                }
                if (StringUtils.isBlank(genus) && StringUtils.isNotBlank(alapn.getGenusOrAbove())) {
                    //add the genus to the index as it is necessary to match on the phrase name.
                    NameIndexField.GENUS.store(alapn.getGenusOrAbove(), doc);
                }

            }
        } catch (org.gbif.api.exception.UnparsableException e) {
            //check to see if the name is a virus in which case an extra name is added without the virus key word
            if (e.type == NameType.VIRUS) {
                NameIndexField.NAME.store(ALANameSearcher.virusStopPattern.matcher(name).replaceAll(" "), doc);
            }

        } catch (Exception e) {
            e.printStackTrace();
            //throw e;
        }

        //add the sound expressions for the name if required
        try {
            if (StringUtils.isNotBlank(soundexGenus)) {
                NameIndexField.GENUS_EX.store(TaxonNameSoundEx.treatWord(soundexGenus, "genus"), doc);
            }
            if (StringUtils.isNotBlank(specificEpithet)) {
                String soundex = TaxonNameSoundEx.treatWord(specificEpithet, "species");
                if (soundex == null)
                    soundex = "<null>";
                NameIndexField.SPECIES_EX.store(soundex, doc);
            } else if (StringUtils.isNotBlank(soundexGenus)) {
                NameIndexField.SPECIES_EX.store("<null>", doc);
            }
            if (StringUtils.isNotBlank(infraspecificEpithet)) {
                String soundex = TaxonNameSoundEx.treatWord(infraspecificEpithet, "species");
                if (soundex == null)
                    soundex = "<null>";
                NameIndexField.INFRA_EX.store(soundex, doc);
             } else if (StringUtils.isNotBlank(specificEpithet)) {
                //make searching for an empty infraspecific soudex easier
                NameIndexField.INFRA_EX.store("<null>", doc);
            }
        } catch (Exception e) {
            log.warn(lsid + " " + name + " has issues creating a soundex: " + e.getMessage());
        }

        return doc;

    }

    public String getIndexDirectory() {
        return indexDirectory;
    }

    public void setIndexDirectory(String indexDirectory) {
        this.indexDirectory = indexDirectory;
    }

    protected String buildNameComplete(String name, String author, String nameComplete) {
        if (StringUtils.isNotBlank(nameComplete))
            return nameComplete;
        StringBuilder ncb = new StringBuilder(64);
        if (name != null)
            ncb.append(name);
        ncb.append(" ");
        if (author != null)
            ncb.append(author);
        return ncb.toString().trim();
    }

    /**
     * Create a CSV reader for a file
     *
     * @param file The name of the file
     * @param separator The field separator character
     * @param escape The escape character
     * @param quote The field quote character
     * @param skipLines The number of lines at the start to skip
     *
     * @return A CSV reader
     */
    protected CSVReader buildCSVReader(String file, char separator, char escape, char quote, int skipLines) throws IOException {
        CSVParser parser = new CSVParserBuilder().withSeparator(separator).withEscapeChar(escape).withQuoteChar(quote).build();
        CSVReader reader = new CSVReaderBuilder(new FileReader(file)).withCSVParser(parser).withSkipLines(skipLines).build();
        return reader;
    }

    /**
     * Generates the Lucene index required for the name matching API.
     * eg
     * au.org.ala.names.search.ALANameIndexer "/data/exports" "/data/lucene/namematching"
     * Extra optional args that should appear after the directory names
     * -sn: Only create the indexes necessary for the scientific name lookups
     * -cn: Only create the indexes necessary for the common name lookups
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        ALANameIndexer indexer = new ALANameIndexer();
        indexer.init();
        for (String arg : args)
            System.out.println(arg);
        if (args.length >= 2) {
            boolean sn = true;
            boolean cn = true;
            if (args.length == 3) {
                sn = args[2].equals("-sn");
                cn = args[2].equals("-cn");
            }
            if (args.length == 5) {
                //the file names have been supplied to the generated.  Used in the case where we are not generating the ALA names index.
                indexer.createIndex(args[0], args[1], args[2], args[3], args[4], sn, cn);
            } else {
                indexer.createIndex(args[0], args[1], sn, cn);
            }
        } else {
            System.out.println("au.org.ala.names.search.ALANameIndexer <directory with export files> <directory in which to create indexes> [<accepted name file>] [<synonym name file>][-cn OR -sn]");

        }
    }
}
