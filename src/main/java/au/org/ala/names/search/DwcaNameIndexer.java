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

import au.org.ala.vocab.ALATerm;
import au.org.ala.names.lucene.analyzer.LowerCaseKeywordAnalyzer;
import au.org.ala.names.model.LinnaeanRankClassification;
import au.org.ala.names.model.NameIndexField;
import au.org.ala.names.model.NameSearchResult;
import au.org.ala.names.model.RankType;
import com.opencsv.CSVReader;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.dwca.record.Record;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwca.io.Archive;
import org.gbif.dwca.io.ArchiveFactory;
import org.gbif.dwca.io.ArchiveFile;
import org.gbif.dwca.record.StarRecord;

import java.io.*;
import java.util.*;

/**
 *
 * Create a name index from a DWCA.  All the required names should exist in the supplied DWCA.
 *
 * The indexer will create a temporary index in order to generate the higher level classification and nested sets for
 * the hierarchy. The main method uses the following options:
 *
 * <ul>
 *     <li>load - flag to indicate that only the load index should be created</li>
 *     <li>search - flag to indicate that only the search index should be created</li>
 *     <li>all - flag to indicate that both the load and search index should be created</li>
 *     <li>irmng - optional param that is used to specify where the IRMNG dwca archive is located.  When this is NOT provided no homonym index is created.</li>
 *     <li>dwca - compulsory param that provides the unzipped location of the DWCA for the scientific names</li>
 *     <li>target - optional param to provide the target location for the name matching index. This value will default to /data/namematching when not provided</li>
 *     <li>tmp - optional param to provide the location for the temporary index. This value will default to /data/tmp/lucene/nmload when not provided</li>
 *     <li>common - optional param to specify when the common name CSV file is located.  When this is NOT provided no common name index is created.</li>
 * </ul>
 *
 * @author Natasha Quimby (natasha.quimby@csiro.au)
 */
public class DwcaNameIndexer extends ALANameIndexer {

    static protected Logger log = Logger.getLogger(DwcaNameIndexer.class);
    private static int PAGE_SIZE = 25000;
    private boolean loadingIndex;
    private boolean sciIndex;
    private File targetDir;
    private File tmpDir;
    private IndexSearcher lsearcher;
    private IndexWriter writer = null;
    private IndexWriter loadingIndexWriter = null;
    private IndexWriter vernacularIndexWriter = null;
    private IndexWriter idWriter = null;
    private LowerCaseKeywordAnalyzer analyzer;
    private Map<String, Float> priorities;

    public DwcaNameIndexer(File targetDir, File tmpDir, Properties priorities, boolean loadingIndex, boolean sciIndex) {
        this.targetDir = targetDir;
        this.tmpDir = tmpDir;
        this.loadingIndex = loadingIndex;
        this.sciIndex = sciIndex;
        this.analyzer = new LowerCaseKeywordAnalyzer();
        this.priorities = this.buildPriorities(priorities);
    }

    /**
     * Begin index construction by initialising the indexes.
     *
     * @throws Exception
     */
    public void begin() throws Exception {
        if (this.loadingIndex) {
             this.loadingIndexWriter = this.createIndexWriter(this.tmpDir, new KeywordAnalyzer(), true);
        }
        if (this.sciIndex) {
            this.writer = createIndexWriter(new File(this.targetDir, "cb"), analyzer, true);
            this.idWriter = createIndexWriter(new File(this.targetDir, "id"), analyzer, true);
            this.vernacularIndexWriter = createIndexWriter(new File(this.targetDir, "vernacular"), new KeywordAnalyzer(), true);
        }
    }

    /**
     * Commit and close the constructed indexes.
     */
    public void commit()  {
        if (this.loadingIndexWriter != null) {
            try {
                this.loadingIndexWriter.close();
            } catch (IOException ex) {
                log.error("Unable to close loading index", ex);
            } finally {
                this.loadingIndexWriter = null;
            }
        }
        if (this.writer != null) {
            try {
                this.writer.close();
            } catch (IOException ex) {
                log.error("Unable to close index", ex);
            } finally {
                this.writer = null;
            }
        }
        if (this.vernacularIndexWriter != null) {
            try {
                this.vernacularIndexWriter.close();
            } catch (IOException ex) {
                log.error("Unable to close vernacular index", ex);
            } finally {
                this.vernacularIndexWriter = null;
            }
        }
        if (this.idWriter != null) {
            try {
                this.idWriter.close();
            } catch (IOException ex) {
                log.error("Unable to close index", ex);
            } finally {
                this.idWriter = null;
            }
        }
    }

    /**
     * Build a datasetID -> Priority map for different data sources
     *
     * @param properties A propetty list for the priorities
     *
     * @return The resulting map
     */
    protected Map<String, Float> buildPriorities(Properties properties) {
        Map<String, Float> map = new HashMap<String, Float>(properties.size());

        for (String ds: properties.stringPropertyNames()) {
            String p = properties.getProperty(ds);
            float pr = 1.0f;
            try {
                pr = Float.parseFloat(p);
            } catch (NumberFormatException ex) {
                log.warn("Unable to parse priority " + p + " for " + ds + " defaulting to 1.0");
            }
            map.put(ds, pr);
        }
        return map;
    }

    /**
     * Creates the name matching index based on a complete list of names supplied in a single DwCA.
     * This will also add vernacular names.
     *
     * @param namesDwc The absolute path to the directory that contains the unzipped DWC archive to index
     * @throws Exception
     */
    public boolean create(File namesDwc) throws Exception{
        if (namesDwc == null || !namesDwc.exists()) {
            log.warn("Skipping " + namesDwc + " as it does not exist");
            return false;
        }
        Archive archive = ArchiveFactory.openArchive(namesDwc);
        if (!archive.getCore().getRowType().equals(DwcTerm.Taxon)) {
            log.info("Skipping non-taxon DwCA");
            return false;
        }
        log.info("Loading synonyms for " + namesDwc);
        addSynonymsToIndex(archive);
        writer.commit();
        writer.forceMerge(1);
        log.info("Loading vernacular for " + namesDwc);
        this.indexCommonNameExtension(archive);
        return true;
     }

    public void createIrmng(File irmngDwc) throws Exception {
        if (irmngDwc == null || !irmngDwc.exists())
            return;
        IndexWriter irmngWriter = this.createIndexWriter(new File(this.targetDir, "irmng"), this.analyzer, true);
        this.indexIrmngDwcA(irmngWriter, irmngDwc.getCanonicalPath());
        irmngWriter.commit();
        irmngWriter.forceMerge(1);
        irmngWriter.close();
    }

    public void createExtraIdIndex(File extraIds) throws Exception {
        this.createExtraIdIndex(this.idWriter, extraIds);
    }

    /**
     * Load common names from a vernacular DwcA with a core row type of gbif:VernacularName
     *
     * @param verncacularDwc The archive directory
     * @return True if used
     *
     * @throws Exception if unable to open the archive
     */
    private boolean loadCommonNames(File verncacularDwc) throws Exception {
        if (verncacularDwc == null || !verncacularDwc.exists()) {
            log.warn("Skipping " + verncacularDwc + " as it does not exist");
            return false;
        }
        Archive archive = ArchiveFactory.openArchive(verncacularDwc);
        if (!archive.getCore().getRowType().equals(GbifTerm.VernacularName)) {
            log.info("Skipping non-vernacular DwCA");
            return false;
        }
        if (!archive.getCore().hasTerm(DwcTerm.vernacularName)) {
            log.error("Vernacular file " + verncacularDwc + " requires " + DwcTerm.vernacularName);
            return false;
        }
        if (!archive.getCore().hasTerm(DwcTerm.scientificName) && !archive.getCore().hasTerm(DwcTerm.taxonID)) {
            log.error("Vernacular file " + verncacularDwc + " requires either " + DwcTerm.scientificName + " or " + DwcTerm.taxonID);
            return false;
        }
        if (archive.getCore().hasTerm(DwcTerm.scientificName) && !archive.getCore().hasTerm(DwcTerm.scientificNameAuthorship)) {
            log.warn("Vernacular file " + verncacularDwc + " has" + DwcTerm.scientificName + " but not " + DwcTerm.scientificNameAuthorship);
            return false;
        }
        for (org.gbif.dwc.terms.Term term: Arrays.asList(DcTerm.language)) {
            log.warn("Vernacular file " + verncacularDwc + " is missing " + term);
        }
        log.info("Loading vernacular names for " + verncacularDwc);
        ALANameSearcher searcher = new ALANameSearcher(this.targetDir.getAbsolutePath());
        Iterator<Record> records = archive.getCore().iterator();
        while (records.hasNext()) {
            Record record = records.next();
            String taxonId = record.value(DwcTerm.taxonID);
            String scientificName = record.value(DwcTerm.scientificName);
            String vernacularName = record.value(DwcTerm.vernacularName);
            String scientificNameAuthorship = record.value(DwcTerm.scientificNameAuthorship);
            String kingdom = record.value(DwcTerm.kingdom);
            String phylum = record.value(DwcTerm.phylum);
            String klass = record.value(DwcTerm.class_);
            String order = record.value(DwcTerm.order);
            String family = record.value(DwcTerm.family);
            String genus = record.value(DwcTerm.genus);
            String specificEpithet = record.value(DwcTerm.specificEpithet);
            String infraspecificEpithet = record.value(DwcTerm.infraspecificEpithet);
            String rank = record.value(DwcTerm.taxonRank);
            String language = record.value(DcTerm.language);
            LinnaeanRankClassification classification = new LinnaeanRankClassification();
            NameSearchResult result = null;
            String lsid;

            if (taxonId != null) {
                result = searcher.searchForRecordByLsid(taxonId);
            }
            if (result == null && scientificName != null) {
                // Try and give as many hints as possible, if available
                classification.setScientificName(scientificName);
                classification.setAuthorship(scientificNameAuthorship);
                classification.setKingdom(kingdom);
                classification.setPhylum(phylum);
                classification.setKlass(klass);
                classification.setOrder(order);
                classification.setFamily(family);
                classification.setGenus(genus);
                classification.setSpecificEpithet(specificEpithet);
                classification.setInfraspecificEpithet(infraspecificEpithet);
                classification.setRank(rank);
                try {
                    result = searcher.searchForRecord(classification, false, false);
                } catch (SearchResultException ex) {
                    log.warn("Can't find matching taxon for " + classification + " and vernacular name " + vernacularName + " exception " + ex.getMessage());
                    continue;
                }
            }
            if (result == null) {
                log.warn("Can't find matching taxon for " + classification + " and vernacular name " + vernacularName);
                continue;
            }
            lsid = result.getAcceptedLsid() != null ? result.getAcceptedLsid() : result.getLsid();
            if (scientificName == null)
                scientificName = result.getRankClassification().getScientificName();
            Document doc = this.createCommonNameDocument(vernacularName, scientificName, lsid, language,1.0f, false);
            this.vernacularIndexWriter.addDocument(doc);
        }
        return true;
    }

    /**
     * Index the common names CSV file supplied.
     *
     * CSV header need to be taxonId, taxonLsid, scientificName, vernacularName, languageCode, countryCode
     *
     * The languageCode and countryCode are not necessary as they are not used.
     *
     * @param file
     * @throws Exception
     */
    private void indexCommonNames(File file) throws Exception {
        //assumes that the quoted TSV file is in the following format
        //taxon id, taxon lsid, scientific name, vernacular name, language code, country code
        if (file == null || !file.exists()) {
            log.info("Skipping common name file " + file);
            return;
        }
        log.info("Starting to load the common names from " + file);
        int i =0, count=0;
        CSVReader cbreader = this.buildCSVReader(file.getPath(), '\t', '"', '\\', 0);
        for (String[] values = cbreader.readNext(); values != null; values = cbreader.readNext()) {
            i++;
            if(values.length == 6){
                //relies on having the same lsid supplied as the DWCA file
                String lsid = StringUtils.isNotEmpty(values[1]) ? values[1] : values[0];
                //check to see if it exists
                TopDocs result = getLoadIdxResults(null, "lsid", lsid, 1);
                if(result.totalHits>0){
                    //we can add the common name
                    Document doc = createCommonNameDocument(values[3], values[2], lsid, values[4], 1.0f, false);
                    this.vernacularIndexWriter.addDocument(doc);
                    count++;
                }
            } else {
                log.info("Issue on line " + i + "  " + values[0]);
            }
            if(i%1000 == 0){
                log.info("Processed " + i + " common names with " + count + " added to index");
            }
        }
        log.info("Finished processing " + i + " common names with " + count + " added to index");
        this.vernacularIndexWriter.commit();
        this.vernacularIndexWriter.forceMerge(1);
    }

    private void indexCommonNameExtension(Archive archive) throws Exception {
        ArchiveFile vernacularArchiveFile = archive.getExtension(GbifTerm.VernacularName);
        Iterator<Record> iter = vernacularArchiveFile == null ? null : vernacularArchiveFile.iterator();
        int i = 0, count = 0;

        if (vernacularArchiveFile == null) {
            log.info("No common names extension from found in " + archive.getLocation());
            return;
        }
        log.info("Starting to load the common names extension from " + archive.getLocation());
        while (iter.hasNext()) {
            i++;
            Record record = iter.next();
            String taxonID = record.id();
            String vernacularName = record.value(DwcTerm.vernacularName);
            String language = record.value(DcTerm.language);
            TopDocs result = getLoadIdxResults(null, "lsid", taxonID, 1);
            if(result.totalHits > 0){
                Document sciNameDoc = lsearcher.doc(result.scoreDocs[0].doc);
                //get the scientific name
                //we can add the common name
                Document doc = createCommonNameDocument(
                        vernacularName,
                        sciNameDoc.get(NameIndexField.NAME.toString()),
                        taxonID,
                        language,
                        1.0f,
                        false);
                this.vernacularIndexWriter.addDocument(doc);
                count++;
            }
            if(i % 1000 == 0){
                log.info("Processed " + i + " common names with " + count + " added to index");
            }
        }
        log.info("Finished processing " + i + " common names with " + count + " added to index");
        this.vernacularIndexWriter.commit();
        this.vernacularIndexWriter.forceMerge(1);
    }


    /**
     * Creates a loading index to use to generate the hierarchy including the left right values.
     *
     * @param archiveDirectory
     * @throws Exception
     */
    public boolean createLoadingIndex(File archiveDirectory) throws Exception{
        if (archiveDirectory == null || !archiveDirectory.exists()) {
            log.warn("Unable to created loading index for " + archiveDirectory + " as it does not exisit");
            return false;
        }
        if (!this.loadingIndex) {
            log.warn("Skipping loading index for " + archiveDirectory);
            return false;
        }
        log.info("Starting to create the temporary loading index for " + archiveDirectory);
        //create the loading index so that left right values and classifications can be generated
        Archive archive = ArchiveFactory.openArchive(archiveDirectory);
        if (!archive.getCore().getRowType().equals(DwcTerm.Taxon)) {
            log.info("Skipping non-taxon DwCA");
            return false;
        }
        Iterator<StarRecord> it = archive.iterator();
        int i=0;
        long start=System.currentTimeMillis();
        while(it.hasNext()){
            Document doc = new Document();
            StarRecord dwcr = it.next();
            Record core = dwcr.core();
            String id = core.id();
            String taxonID = core.value(DwcTerm.taxonID) == null ? id : core.value(DwcTerm.taxonID);
            String acceptedNameUsageID = core.value(DwcTerm.acceptedNameUsageID);
            String parentNameUsageID = core.value(DwcTerm.parentNameUsageID);
            String nameComplete = core.value(ALATerm.nameComplete);
            String scientificName = core.value(DwcTerm.scientificName);
            String scientificNameAuthorship = core.value(DwcTerm.scientificNameAuthorship);
            String genus = core.value(DwcTerm.genus);
            String specificEpithet = core.value(DwcTerm.specificEpithet);
            String infraspecificEpithet = core.value(DwcTerm.infraspecificEpithet);
            String taxonRank = core.value(DwcTerm.taxonRank);
            String datasetID = core.value(DwcTerm.datasetID);
            nameComplete = this.buildNameComplete(scientificName, scientificNameAuthorship, nameComplete);
            //add and store the identifier for the record
            doc.add(new StringField(NameIndexField.ID.toString(), id, Field.Store.YES));
            if(StringUtils.isNotBlank(taxonID)){
                doc.add(new StringField(NameIndexField.LSID.toString(), taxonID, Field.Store.YES));
            } else {
                System.out.println("LSID is null for " + id + " " + taxonID + " " + taxonID + " " + acceptedNameUsageID);
            }
            if(StringUtils.isNotBlank(parentNameUsageID)) {
                doc.add(new StringField("parent_id", parentNameUsageID, Field.Store.YES));
            }
            if(StringUtils.isNotBlank(acceptedNameUsageID)) {
                doc.add(new StringField(NameIndexField.ACCEPTED.toString(),acceptedNameUsageID, Field.Store.YES));
            }
            if(StringUtils.isNotBlank(scientificName)) {
                //stored no need to search on
                doc.add(new StoredField(NameIndexField.NAME.toString(),scientificName));
            }
            if(StringUtils.isNotBlank(scientificNameAuthorship)) {
                //stored no need to search on
                doc.add(new StoredField(NameIndexField.AUTHOR.toString(),scientificNameAuthorship));
            }
            if (StringUtils.isNotBlank(nameComplete)) {
                doc.add(new StoredField(NameIndexField.NAME_COMPLETE.toString(), nameComplete));
            }
            if(StringUtils.isNotBlank(genus)) {
                //stored no need to search on
                doc.add(new StoredField("genus",genus));
            }
            if(StringUtils.isNotBlank(specificEpithet)) {
                //stored no need to search on
                doc.add(new StoredField(NameIndexField.SPECIFIC.toString(),specificEpithet));
            }
            if(StringUtils.isNotBlank(infraspecificEpithet)) {
                //stored no need to search on
                doc.add(new StoredField(NameIndexField.INFRA_SPECIFIC.toString(),infraspecificEpithet));
            }
            if(StringUtils.isNotBlank(taxonRank)){
                //match the supplied rank
                RankType rt = RankType.getForStrRank(taxonRank);
                if(rt != null){
                    doc.add(new StringField(NameIndexField.RANK.toString(), rt.getRank(), Field.Store.YES));
                    doc.add(new StringField(NameIndexField.RANK_ID.toString(), rt.getId().toString(), Field.Store.YES));
                } else {
                    doc.add(new StringField(NameIndexField.RANK.toString(), taxonRank, Field.Store.YES));
                    doc.add(new StringField(NameIndexField.RANK_ID.toString(), RankType.UNRANKED.getId().toString(), Field.Store.YES));
                }
            } else {
                //put in unknown rank
                doc.add(new StringField(NameIndexField.RANK.toString(), "Unknown", Field.Store.YES));
                doc.add(new StringField(NameIndexField.RANK_ID.toString(), RankType.UNRANKED.getId().toString(), Field.Store.YES));
            }
            if(StringUtils.equals(taxonID, acceptedNameUsageID) || StringUtils.equals(id, acceptedNameUsageID) || acceptedNameUsageID == null){
                //mark this one as an accepted concept
                doc.add(new StringField(NameIndexField.iS_SYNONYM.toString(),"F", Field.Store.YES));
                if (StringUtils.isBlank(parentNameUsageID)){
                    doc.add(new StringField("root","T", Field.Store.YES));
                }
            } else {
                doc.add(new StringField(NameIndexField.iS_SYNONYM.toString(),"T", Field.Store.YES));
            }
            if (StringUtils.isNotBlank(datasetID)) {
                doc.add(new StoredField(NameIndexField.DATASET_ID.toString(), datasetID));
            }

            this.loadingIndexWriter.addDocument(doc);
            i++;
            if(i % 1000 == 0){
                long finish = System.currentTimeMillis();
                log.debug("Loading index: " + i + " records per sec: " + (1000/(((float)(finish/start))/1000)));
                start =finish;
            }
        }

        log.info("Finished creating the temporary load index with " + i + " concepts");
        this.loadingIndexWriter.commit();
        this.loadingIndexWriter.forceMerge(1);
        return true;
    }

    public void commitLoadingIndexes() throws IOException {
        if (this.loadingIndexWriter != null) {
            this.loadingIndexWriter.close();
            this.loadingIndexWriter = null;
        }
        this.lsearcher = null;
    }

    private TopDocs getLoadIdxResults(ScoreDoc after, String field, String value,int max) throws Exception {
        if(lsearcher == null && this.tmpDir.exists()) {
            lsearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(this.tmpDir.toPath())));
        } else if(lsearcher == null && !this.tmpDir.exists()){
            throw new RuntimeException("A load index has not been generated. Please run this tool with '-load' before creating the search index.");
        }
        TermQuery tq = new TermQuery(new Term(field, value));
        return after == null ? lsearcher.search(tq, max) : lsearcher.searchAfter(after, tq, max);
    }

    /**
     * generates the accepted concepts for the name matching index.
     *
     * Relies on the loading indexing being created
     *
     * @throws Exception
     */
    public void generateIndex() throws Exception{
        //get all the records that don't have parents that are accepted
        log.info("Loading index from temporary index.");
        TopDocs rootConcepts = getLoadIdxResults(null, "root", "T", PAGE_SIZE);
        int left = 0;
        int right = left;
        int lastRight = right;
        int count = 0;
        while (rootConcepts != null && rootConcepts.totalHits > 0) {
            ScoreDoc lastConcept = null;
            for (ScoreDoc sd : rootConcepts.scoreDocs) {
                lastConcept = sd;
                left = right + 1;
                Document doc = lsearcher.doc(sd.doc);
                right = addIndex(doc, 1, left, new LinnaeanRankClassification(), 0);
                if (right - lastRight > 1000) {
                    log.info("Finished loading root " + doc.get(NameIndexField.LSID.toString()) + " " + doc.get(NameIndexField.NAME.toString()) + " left:" + left + " right:" + right + " root count:" + count);
                    lastRight = right;
                }
                count++;
                if(count % 10000 == 0){
                    log.info("Loading index:" + count);
                }
            }
            rootConcepts = lastConcept == null ? null : getLoadIdxResults(lastConcept, "root", "T", PAGE_SIZE);
            if (rootConcepts != null && rootConcepts.scoreDocs.length > 0) {
                log.info("Loading next page of root concepts");
            }
        }
    }

    /**
     * Adds a document to the name matching index after populating the hierarchy
     * @param doc
     * @param currentDepth
     * @param currentLeft
     * @param higherClass
     * @return
     * @throws Exception
     */
    private int addIndex(Document doc, int currentDepth, int currentLeft, LinnaeanRankClassification higherClass, int stackCheck ) throws Exception {
        //log.info("Add to index " + doc.get(NameIndexField.ID.toString()) + "/" + doc.get(NameIndexField.NAME.toString()) + "/" + doc.get(NameIndexField.RANK_ID.toString()) + " depth=" + currentDepth + " left=" + currentLeft);
        String id = doc.get(NameIndexField.ID.toString());
        //get children for this record
        TopDocs children = getLoadIdxResults(null, "parent_id", id, PAGE_SIZE);
        if(children.totalHits == 0){
            id =  doc.get(NameIndexField.LSID.toString());
            children = getLoadIdxResults(null, "parent_id", id, PAGE_SIZE);
        }
        int left = currentLeft;
        int right = left;
        int rankId = Integer.parseInt(doc.get(NameIndexField.RANK_ID.toString()));
        String name = doc.get(NameIndexField.NAME.toString());
        String nameComplete = doc.get(NameIndexField.NAME_COMPLETE.toString());
        String lsid = doc.get(NameIndexField.LSID.toString());
        //get the canonical version if the sciname
        String cname = name;
        ParsedName pn = null;
        try {
            pn = parser.parse(name);
            if(pn.isParsableType()){
                cname = pn.canonicalName();
            }
        } catch(Exception e){
            //do nothing
        }
        //create a new classification for this entry based on the parent
        LinnaeanRankClassification newcl = new LinnaeanRankClassification(higherClass);
        switch(rankId){
            case 1000:
                newcl.setKingdom(cname);
                newcl.setKid(lsid);
                break;
            case 2000:
                newcl.setPhylum(cname);
                newcl.setPid(lsid);
                break;
            case 3000:
                newcl.setKlass(cname);
                newcl.setCid(lsid);
                break;
            case 4000:
                newcl.setOrder(cname);
                newcl.setOid(lsid);
                break;
            case 5000:
                newcl.setFamily(cname);
                newcl.setFid(lsid);
                break;
            case 6000:
                newcl.setGenus(cname);
                newcl.setGid(lsid);
                break;
            case 7000:
                newcl.setSpecies(cname);
                newcl.setSid(lsid);
                if (pn != null && pn.isParsableType()) {
                    newcl.setSpecificEpithet(pn.getSpecificEpithet());
                }
                break;
        }
        while (children != null && children.scoreDocs.length > 0) {
            ScoreDoc lastChild = null;
            for (ScoreDoc child : children.scoreDocs) {
                lastChild = child;
                Document cdoc = lsearcher.doc(child.doc);
                if(cdoc != null && !cdoc.get("id").equals(doc.get("id"))){
                    if(stackCheck > 900){
                        log.warn("Stack check depth " + stackCheck +
                                "\n\t\tParent: " + doc.get("id") + " - " +  doc.get("lsid") + " - "  + doc.get("parent_id") + " - " + doc.get("name") +
                                "\n\t\tChild: " + cdoc.get("id") + " - " +  cdoc.get("lsid") + " _ " + cdoc.get("parent_id") + " - " +  cdoc.get("name")
                        );
                    }

                    if(stackCheck < 1000) {
//                        catch stack overflow
                        right = addIndex(cdoc, currentDepth + 1, right + 1, newcl, stackCheck + 1);
                    } else {
                        log.warn("Stack overflow detected for name - depth " + stackCheck +
                                "\n\t\tParent: " + doc.get("id") + " - " +  doc.get("lsid") + " - "  + doc.get("parent_id") + " - " + doc.get("name") +
                                "\n\t\tChild: " + cdoc.get("id") + " - " +  cdoc.get("lsid") + " _ " + cdoc.get("parent_id") + " - " +  cdoc.get("name")

                        );
                    }
                }
            }
            children = lastChild == null ? null : this.getLoadIdxResults(lastChild, "parent_id", id, PAGE_SIZE);
            if (children != null && children.scoreDocs.length > 0)
                log.info("Loading next page of children for " + id);
        }
        if(left % 2000 == 0){
            log.debug("Last processed lft:" + left + " rgt:" + right + " depth:" + currentDepth + " classification " + newcl );
        }
        //now insert this term
        float boost = this.getBoost(doc.get(NameIndexField.DATASET_ID.toString()), rankId);
        Document indexDoc = this.createALAIndexDocument(
                name,
                doc.get(NameIndexField.ID.toString()),
                lsid,
                doc.get(NameIndexField.AUTHOR.toString()),
                doc.get(NameIndexField.RANK.toString()),
                doc.get(NameIndexField.RANK_ID.toString()),
                Integer.toString(left),
                Integer.toString(right),
                newcl,
                nameComplete,
                boost);
        writer.addDocument(indexDoc);
        return right + 1;
    }

    /**
     * Build a boost level for this data.
     * <p>
     * Derived from the dataset ID priorities, with extra emphasis placed on major ranks.
     * Default initial boost is 1.0.
     * </p>
     *
     * @param datasetID The dataset id (may be null)
     * @param rankId The rank level (-1 for unknown/not used)
     *
     * @return The boost level
     */
    protected float getBoost(String datasetID, int rankId) {
        float boost = this.priorities.containsKey(datasetID) ? this.priorities.get(datasetID) : 1.0f;

        if (rankId >= 0 && rankId % 1000 == 0)
            boost *= 5.0f;
        return boost;
    }

    /**
     * Adds the synonyms to the indexed based on the dwca.  A synonym is where the id, lsid is different to the accepted lsid
     * @param archive The archive for synonyms
     */
    private void addSynonymsToIndex(Archive archive) throws Exception {
        Iterator<StarRecord> it = archive.iterator();
        int i = 0;
        int count = 0;
        while(it.hasNext()){
            StarRecord dwcr = it.next();
            Record core = dwcr.core();
            i++;
            String id = core.id();
            String lsid = core.value(DwcTerm.taxonID) != null ? core.value(DwcTerm.taxonID) : id;
            String acceptedNameUsageID = core.value(DwcTerm.acceptedNameUsageID);
            String nameComplete = core.value(ALATerm.nameComplete);
            String scientificName = core.value(DwcTerm.scientificName);
            String scientificNameAuthorship = core.value(DwcTerm.scientificNameAuthorship);
            nameComplete = this.buildNameComplete(scientificName, scientificNameAuthorship, nameComplete);
            String datasetID = core.value(DwcTerm.datasetID);
            String taxonomicStatus = core.value(DwcTerm.taxonomicStatus);
            float boost = this.getBoost(datasetID, -1);
            if(StringUtils.isNotEmpty(acceptedNameUsageID) && (!StringUtils.equals(acceptedNameUsageID , id) && !StringUtils.equals(acceptedNameUsageID, lsid))){
                count++;
                //we have a synonym that needs to be load
                try {
                    if(log.isDebugEnabled()){
                        log.debug("Scientific name:  " + scientificName + ", LSID:  " + lsid);
                    }
                    Document doc = createALASynonymDocument(
                            scientificName,
                            scientificNameAuthorship,
                            nameComplete,
                            id,
                            lsid,
                            lsid,
                            acceptedNameUsageID,
                            acceptedNameUsageID,
                            boost,
                            taxonomicStatus);

                    if(doc != null){
                        writer.addDocument(doc);
                    } else {
                        log.warn("Problem processing scientificName:  " + scientificName + ", ID:  " + id + ", LSID:  " + lsid);
                    }
                } catch (Exception e){
                    log.error("Exception thrown processing Scientific name:  " + scientificName + ", LSID:  " + lsid);
                    log.error(e.getMessage(), e);
                }
            }
            if(i % 1000 == 0){
                log.debug("Processed " + i + " records " + count + " synonyms" );
            }
        }
    }

    public static void findDwcas(File dir, boolean recurse, List<File> found) {
        File meta = new File(dir, "meta.xml");

        if (meta.exists())
            found.add(dir);
        if (recurse && dir.exists()) {
            for (File d : dir.listFiles()) {
                if (d.isDirectory())
                    findDwcas(d, recurse, found);
            }
        }
    }

    /**
     * Example run
     *
     * java –cp .:names.jar au.org.ala.checklist.lucene.DwcaNameIndexer
     * -all
     * -dwca /data/bie-staging/names-lists/dwca-col
     * -target /data/lucene/testdwc-namematching
     * -irmng /data/bie-staging/irmng/IRMNG_DWC_HOMONYMS
     * -common /data/bie-staging/ala-names/col_vernacular.txt
     *
     * @param args
     */
    public static void main(String[] args) {

        final String DEFAULT_DWCA = "/data/lucene/sources/dwca-col";
        final String DEFAULT_IRMNG = "/data/lucene/sources/IRMNG_DWC_HOMONYMS";
        final String DEFAULT_COMMON_NAME = "/data/lucene/sources/col_vernacular.txt";
        final String DEFAULT_TARGET_DIR = "/data/lucene/namematching";
        final String DEFAULT_TMP_DIR = "/data/lucene/nmload-tmp";
        final String DEFAULT_PRIORITIES = "/data/lucene/sources/priorities.properties";
        final String DEFAULT_IDENTIFIERS = "/data/lucene/sources/identifiers.txt";

        Options options = new Options();
        options.addOption("v", "version", false, "Retrieve version information");
        options.addOption("h", "help", false, "Retrieve options");
        options.addOption("all", false, "Generates the load index and search index");
        options.addOption("load", false, "Generate the load index only. " +
                "The load index is a temporary index generated from the raw data files" +
                " used to load the main search index");
        options.addOption("search", false, "Generates the search index. A load index must already be created for this to run.");
        options.addOption("irmng", true, "The absolute path to the unzipped irmng DwCA. IRMNG is used to detect homonyms. Defaults to " + DEFAULT_IRMNG);
        options.addOption("dwca", true, "The absolute path to the unzipped DwCA (or a directory containing unzipped DWC-A - see recurse) for the scientific names. If  Defaults to " + DEFAULT_DWCA + " See also, the recurse option");
        options.addOption("recurse", false, "Recurse through the sub-directories of the dwca directory, looking for directories with a meta.xml");
        options.addOption("priorities", true, "A properties file containing priority multiplers for the different data sources, keyed by datasetID->float. Defaults to " + DEFAULT_PRIORITIES);
        options.addOption("ids", true, "A tab seperated values file containing additional taxzon identifiers. Defaults to " + DEFAULT_IDENTIFIERS);
        options.addOption("target", true, "The target directory to write the new name index to. Defaults to " + DEFAULT_TARGET_DIR);
        options.addOption("tmp", true, "The tmp directory for the load index. Defaults to " + DEFAULT_TMP_DIR);
        options.addOption("common", true, "The common (vernacular) name file. Defaults to " + DEFAULT_COMMON_NAME);
        options.addOption("testSearch", true, "Debug a name search. This uses the target directory to search against.");
        options.addOption("testCommonSearch", true, "Debug a common name search. This takes a taxonID for the search.");
        options.addOption("testCommonSearchLang", true, "Debug a common name search, supplying a language.");

        CommandLineParser parser = new BasicParser();

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("v")){
                //only load the properties file if it exists otherwise default to the biocache-test-config.properties on the classpath
                InputStream stream = DwcaNameIndexer.class.getResourceAsStream("/git.properties");
                Properties properties = new Properties();
                if(stream != null){
                    properties.load(stream);
                    properties.list(System.out);
                } else {
                    System.err.println("Unable to retrieve versioning information");
                }
                new HelpFormatter().printHelp("nameindexer", options);
                System.exit(-1);
            }

            if (line.hasOption("help")){
                //only load the properties file if it exists otherwise default to the biocache-test-config.properties on the classpath
                new HelpFormatter().printHelp("nameindexer", options);
                System.exit(-1);
            }

            if (line.hasOption("testSearch")){

                boolean indexExists = (new File(DEFAULT_TARGET_DIR).exists());

                if(indexExists) {
                    //do a name search - with option flag pointing to index location
                    System.out.println("Search for name: " + line.getOptionValue("testSearch"));
                    ALANameSearcher searcher = new ALANameSearcher(line.getOptionValue("target", DEFAULT_TARGET_DIR));
                    NameSearchResult nsr = searcher.searchForRecord(line.getOptionValue("testSearch"));

                    if(nsr == null){
                         nsr = searcher.searchForRecordByLsid(line.getOptionValue("testSearch"));
                    }

                    if(nsr != null) {
                        Map<String, String> props = nsr.toMap();
                        for (Map.Entry<String, String> entry : props.entrySet()) {
                            System.out.println(entry.getKey() + ": " + entry.getValue());
                        }
                    } else {
                        nsr = searcher.searchForCommonName(line.getOptionValue("testSearch"));
                        if(nsr != null) {
                            Map<String, String> props = nsr.toMap();
                            for (Map.Entry<String, String> entry : props.entrySet()) {
                                System.out.println(entry.getKey() + ": " + entry.getValue());
                            }
                        } else {
                            System.err.println("No match for " + line.getOptionValue("testSearch"));
                        }
                    }
                    System.exit(1);
                } else {
                    System.err.println("Index unreadable. Check " + DEFAULT_TARGET_DIR);
                }
                new HelpFormatter().printHelp("nameindexer", options);
                System.exit(-1);
            }

            if (line.hasOption("testCommonSearch")){
                boolean indexExists = (new File(DEFAULT_TARGET_DIR).exists());
                if(indexExists) {
                    //do a name search - with option flag pointing to index location
                    System.out.println("Search for name: " + line.getOptionValue("testCommonSearch"));
                    ALANameSearcher searcher = new ALANameSearcher(line.getOptionValue("target", DEFAULT_TARGET_DIR));

                    String lsid = line.getOptionValue("testCommonSearch");
                    String language = line.getOptionValue("testCommonSearchLang");

                    String commonName = null;
                    if(StringUtils.isNotBlank(language)){
                        commonName = searcher.getCommonNameForLSID(lsid, new String[]{language});
                    } else {
                        commonName = searcher.getCommonNameForLSID(lsid);
                    }

                    if(commonName == null){
                        if(StringUtils.isNotBlank(language)){
                            System.err.println("No common name indexed for taxonID: " + lsid + " and language " + language);
                        } else {
                            System.err.println("No common name indexed for taxonID: " + lsid);
                        }
                    } else {
                        System.out.println("Match: " + commonName);
                    }

                    System.exit(1);
                } else {
                    System.err.println("Index unreadable. Check " + DEFAULT_TARGET_DIR);
                }
                new HelpFormatter().printHelp("nameindexer", options);
                System.exit(-1);

            }

            boolean recurse = line.hasOption("recurse");
            boolean load = line.hasOption("load") || line.hasOption("all");
            boolean search = line.hasOption("search") || line.hasOption("all");

            if(!line.hasOption("load") && !line.hasOption("search") && !line.hasOption("all") ){
                load = true;
                search = true;
            }

            log.info("Generating loading index: " + load);
            log.info("Generating searching index: " + search);

            boolean defaultIrmngReadable = (new File(DEFAULT_IRMNG).exists());
            boolean defaultCommonReadable = (new File(DEFAULT_COMMON_NAME).exists());
            boolean defaultDwcaReadable = (new File(DEFAULT_DWCA).exists());
            boolean defaultPriorities = (new File(DEFAULT_PRIORITIES).exists());
            boolean defaultIdentifiers =  (new File(DEFAULT_IDENTIFIERS).exists());

            if(line.getOptionValue("dwca") != null){
                log.info("Using the  DwCA name file: " + line.getOptionValue("dwca"));
            } else if (defaultDwcaReadable){
                log.info("Using the default DwCA name file: " + DEFAULT_DWCA);
            } else {
                log.error("No DwC Archive specified and the default file path does not exist or is inaccessible. Default path: " + DEFAULT_DWCA);
                new HelpFormatter().printHelp("nameindexer", options);
                System.exit(-1);
            }

            if(line.getOptionValue("irmng") == null && !defaultIrmngReadable){
                log.warn("No IRMNG export specified and the default file path does not exist or is inaccessible. Default path: " + DEFAULT_IRMNG);
            } else if(line.getOptionValue("irmng") == null) {
                log.info("Using the default IRMNG name file: " + DEFAULT_IRMNG);
            } else {
                log.info("Using the  IRMNG name file: " + line.getOptionValue("irmng"));
            }

            if(line.getOptionValue("common") == null && !defaultCommonReadable) {
                log.warn("No common name export specified and the default file path does not exist or is inaccessible. Default path: " + DEFAULT_COMMON_NAME);
            } else if(line.getOptionValue("common") == null){
                log.info("Using the default common name file: " + DEFAULT_COMMON_NAME);
            } else {
                log.info("Using the common name file: " + line.getOptionValue("common"));
            }

            if(line.getOptionValue("priorities") == null && !defaultPriorities) {
                log.warn("No priorities file, defaulting to uniform priorities.");
            } else if(line.getOptionValue("priorities") == null){
                log.info("Using the default priorities file: " + DEFAULT_PRIORITIES);
            } else {
                log.info("Using the priorities file: " + line.getOptionValue("priorities"));
            }

            if(line.getOptionValue("ids") == null && !defaultIdentifiers) {
                log.warn("No identifiers file, Defailts is " + DEFAULT_IDENTIFIERS);
            } else if(line.getOptionValue("ids") == null){
                log.info("Using the default identifiers file: " + DEFAULT_IDENTIFIERS);
            } else {
                log.info("Using the identifiers file: " + line.getOptionValue("ids"));
            }

            File targetDirectory = new File(line.getOptionValue("target", DEFAULT_TARGET_DIR));
            if(targetDirectory.exists()){
                String newPath =  targetDirectory.getAbsolutePath() + "_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_hh-mm-ss");
                log.info("Target directory already exists. Backing up to : " + newPath);
                File newTargetDirectory = new File(newPath);
                FileUtils.moveDirectory(targetDirectory, newTargetDirectory);
                FileUtils.forceMkdir(targetDirectory);
            }
            File commonNameFile = new File(line.getOptionValue("common", DEFAULT_COMMON_NAME));
            File irmngFile = new File(line.getOptionValue("irmng", DEFAULT_IRMNG));
            File identifiersFile = new File(line.getOptionValue("ids", DEFAULT_IDENTIFIERS));
            File prioritiesFile = new File(line.getOptionValue("priorities", DEFAULT_PRIORITIES));
            Properties priorities = new Properties();
            if (prioritiesFile.exists())
                priorities.load(new FileInputStream(prioritiesFile));
            List<File> dwcas = new ArrayList<File>();
            List<File> bases = new ArrayList<File>();
            if (line.hasOption("dwca"))
                for (String base: line.getOptionValues("dwca"))
                    bases.add(new File(base));
            else
                bases.add(new File(DEFAULT_DWCA));
            log.info("Base sources: " + bases);
            for (File base: bases)
                findDwcas(base, recurse, dwcas);
            if (dwcas.isEmpty()) {
                log.warn("No DwCA directories found under " + bases);
                System.exit(1);
            }

            log.info("Loading DwCAs: " + dwcas);
            DwcaNameIndexer indexer = new DwcaNameIndexer(
                    targetDirectory,
                    new File(line.getOptionValue("tmp", DEFAULT_TMP_DIR)),
                    priorities,
                    load,
                    search
            );
            indexer.begin();
            Set<File> used = new HashSet<File>();
            if (load) {
                for (File dwca : dwcas)
                    if (indexer.createLoadingIndex(dwca))
                        used.add(dwca);
                indexer.commitLoadingIndexes();
            }
            indexer.generateIndex();
            for (File dwca: dwcas)
                if (indexer.create(dwca))
                    used.add(dwca);
            indexer.indexCommonNames(commonNameFile);
            indexer.createIrmng(irmngFile);
            indexer.createExtraIdIndex(identifiersFile);
            for (File dwca: dwcas)
                if (indexer.loadCommonNames(dwca))
                    used.add(dwca);
            indexer.commit();
            for (File dwca: dwcas)
                if (!used.contains(dwca))
                    log.warn("Source " + dwca + " is unused");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
