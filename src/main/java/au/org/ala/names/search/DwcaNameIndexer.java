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
import au.org.ala.names.model.LinnaeanRankClassification;
import au.org.ala.names.model.NameIndexField;
import au.org.ala.names.model.NameSearchResult;
import au.org.ala.names.model.RankType;
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
import org.gbif.dwc.record.DarwinCoreRecord;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.dwc.text.Archive;
import org.gbif.dwc.text.ArchiveFactory;
import org.gbif.dwc.text.ArchiveField;
import org.gbif.dwc.text.ArchiveFile;

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
     * Creates the name matching index based on a complete list of names supplied in a single DwCA
     *
     * @param namesDwc The absolute path to the directory that contains the unzipped DWC archive to index
     * @throws Exception
     */
    public void create(File namesDwc) throws Exception{
        if (namesDwc == null || !namesDwc.exists()) {
            log.warn("Skipping " + namesDwc + " as it does not exist");
            return;
        }
        log.info("Loading synonyms for " + namesDwc);
        addSynonymsToIndex(namesDwc);
        writer.commit();
        writer.forceMerge(1);
        this.indexCommonNameExtension(namesDwc);
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
        au.com.bytecode.opencsv.CSVReader cbreader = new au.com.bytecode.opencsv.CSVReader(new FileReader(file), '\t', '"', '\\', 0);
        for (String[] values = cbreader.readNext(); values != null; values = cbreader.readNext()) {
            i++;
            if(values.length == 6){
                //relies on having the same lsid supplied as the DWCA file
                String lsid = StringUtils.isNotEmpty(values[1]) ? values[1] : values[0];
                //check to see if it exists
                TopDocs result = getLoadIdxResults(null, "lsid", lsid, 1);
                if(result.totalHits>0){
                    //we can add the common name
                    Document doc = createCommonNameDocument(values[3], values[2], lsid, 1.0f, false);
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

    private void indexCommonNameExtension(File archiveDirectory) throws Exception {
        Archive archive = ArchiveFactory.openArchive(archiveDirectory);
        ArchiveFile vernacularArchiveFile = archive.getExtension(GbifTerm.VernacularName);
        Iterator<Record> iter = vernacularArchiveFile == null ? null : vernacularArchiveFile.iterator();
        int i = 0, count = 0;

        if (vernacularArchiveFile == null)
            return;
        log.info("Starting to load the common names extension from " + archiveDirectory);
        while (iter.hasNext()) {
            i++;
            Record record = iter.next();
            String taxonID = record.id();
            String vernacularName = record.value(DwcTerm.vernacularName);
            TopDocs result = getLoadIdxResults(null, "lsid", taxonID, 1);
            if(result.totalHits > 0){
                Document sciNameDoc = lsearcher.doc(result.scoreDocs[0].doc);
                //get the scientific name
                //we can add the common name
                Document doc = createCommonNameDocument(vernacularName, sciNameDoc.get(NameIndexField.NAME.toString()), taxonID, 1.0f, false);
                this.vernacularIndexWriter.addDocument(doc);
                count++;
            }
            if(i%1000 == 0){
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
    private void createLoadingIndex(File archiveDirectory) throws Exception{
        if (archiveDirectory == null || !archiveDirectory.exists()) {
            log.warn("Unable to created loading index for " + archiveDirectory + " as it does not exisit");
            return;
        }
        if (!this.loadingIndex) {
            log.warn("Skipping loading index for " + archiveDirectory);
            return;
        }
        log.info("Starting to create the temporary loading index for " + archiveDirectory);
        //create the loading index so that left right values and classifications can be generated
        Archive archive = ArchiveFactory.openArchive(archiveDirectory);
        ArchiveField nameCompleteField = archive.getCore().getField("nameComplete");
        org.gbif.dwc.terms.Term nameCompleteTerm = nameCompleteField == null ? null : nameCompleteField.getTerm();
        Iterator<DarwinCoreRecord> it = archive.iteratorDwc();
        int i=0;
        long start=System.currentTimeMillis();
        while(it.hasNext()){
            Document doc = new Document();
            DarwinCoreRecord dwcr = it.next();
            String id = dwcr.getId();
            String lsid = dwcr.getTaxonID() == null ? id : dwcr.getTaxonID();
            String acceptedLsid = dwcr.getAcceptedNameUsageID();
            String nameComplete = nameCompleteTerm == null ? null: dwcr.getProperty(nameCompleteTerm);
            String scientificName = dwcr.getScientificName();
            String scientificNameAuthorship = dwcr.getScientificNameAuthorship();
            nameComplete = this.buildNameComplete(scientificName, scientificNameAuthorship, nameComplete);
            //add and store the identifier for the record
            doc.add(new StringField(NameIndexField.ID.toString(), dwcr.getId(), Field.Store.YES));
            if(StringUtils.isNotBlank(lsid)){
                doc.add(new StringField(NameIndexField.LSID.toString(), lsid, Field.Store.YES));
            } else {
                System.out.println("LSID is null for " + id + " " + lsid + " " + lsid + " " + acceptedLsid);
            }
            if(StringUtils.isNotBlank(dwcr.getParentNameUsageID())) {
                doc.add(new StringField("parent_id", dwcr.getParentNameUsageID(), Field.Store.YES));
            }
            if(StringUtils.isNotBlank(dwcr.getAcceptedNameUsageID())) {
                doc.add(new StringField(NameIndexField.ACCEPTED.toString(),dwcr.getAcceptedNameUsageID(), Field.Store.YES));
            }
            if(StringUtils.isNotBlank(scientificName)) {
                //stored no need to search on
                doc.add(new StoredField(NameIndexField.NAME.toString(),dwcr.getScientificName()));
            }
            if(StringUtils.isNotBlank(scientificNameAuthorship)) {
                //stored no need to search on
                doc.add(new StoredField(NameIndexField.AUTHOR.toString(),dwcr.getScientificNameAuthorship()));
            }
            if (StringUtils.isNotBlank(nameComplete)) {
                doc.add(new StoredField(NameIndexField.NAME_COMPLETE.toString(), nameComplete));
            }
            if(StringUtils.isNotBlank(dwcr.getGenus())) {
                //stored no need to search on
                doc.add(new StoredField("genus",dwcr.getGenus()));
            }
            if(StringUtils.isNotBlank(dwcr.getSpecificEpithet())) {
                //stored no need to search on
                doc.add(new StoredField(NameIndexField.SPECIFIC.toString(),dwcr.getSpecificEpithet()));
            }
            if(StringUtils.isNotBlank(dwcr.getInfraspecificEpithet())) {
                //stored no need to search on
                doc.add(new StoredField(NameIndexField.INFRA_SPECIFIC.toString(),dwcr.getInfraspecificEpithet()));
            }
            if(StringUtils.isNotBlank(dwcr.getTaxonRank())){
                //match the supplied rank
                RankType rt = RankType.getForStrRank(dwcr.getTaxonRank());
                if(rt != null){
                    doc.add(new StringField(NameIndexField.RANK.toString(), rt.getRank(), Field.Store.YES));
                    doc.add(new StringField(NameIndexField.RANK_ID.toString(), rt.getId().toString(), Field.Store.YES));
                } else {
                    doc.add(new StringField(NameIndexField.RANK.toString(), dwcr.getTaxonRank(), Field.Store.YES));
                    doc.add(new StringField(NameIndexField.RANK_ID.toString(), RankType.UNRANKED.getId().toString(), Field.Store.YES));
                }
            } else {
                //put in unknown rank
                doc.add(new StringField(NameIndexField.RANK.toString(), "Unknown", Field.Store.YES));
                doc.add(new StringField(NameIndexField.RANK_ID.toString(), RankType.UNRANKED.getId().toString(), Field.Store.YES));
            }
            if(StringUtils.equals(lsid, acceptedLsid) || StringUtils.equals(id, acceptedLsid) || acceptedLsid == null){
                //mark this one as an accepted concept
                doc.add(new StringField(NameIndexField.iS_SYNONYM.toString(),"F", Field.Store.YES));
                if (StringUtils.isBlank(dwcr.getParentNameUsageID())){
                    doc.add(new StringField("root","T", Field.Store.YES));
                }
            } else {
                doc.add(new StringField(NameIndexField.iS_SYNONYM.toString(),"T", Field.Store.YES));
            }
            if (StringUtils.isNotBlank(dwcr.getDatasetID())) {
                doc.add(new StoredField(NameIndexField.DATASET_ID.toString(), dwcr.getDatasetID()));
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
    }

    public void commitLoadingIndexes() throws IOException {
        if (this.loadingIndexWriter != null) {
            this.loadingIndexWriter.close(true);
            this.loadingIndexWriter = null;
        }
        this.lsearcher = null;
    }

    private TopDocs getLoadIdxResults(ScoreDoc after, String field, String value,int max) throws Exception {
        if(lsearcher == null && this.tmpDir.exists()) {
            lsearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(this.tmpDir)));
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
    private void generateIndex() throws Exception{
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
                right = addIndex(doc, 1, left, new LinnaeanRankClassification());
                if (right - lastRight > 1000) {
                    log.info("Finished loading root " + doc.get(NameIndexField.LSID.toString()) + " " + doc.get(NameIndexField.NAME.toString()) + " left:" + left + " right" + right + " root count:" + count);
                    lastRight = right;
                }
                count++;
            }
            rootConcepts = lastConcept == null ? null : getLoadIdxResults(lastConcept, "root", "T", PAGE_SIZE);
            if (rootConcepts != null && rootConcepts.scoreDocs.length > 0)
                log.info("Loading next page of root concepts");
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
    private int addIndex(Document doc,int currentDepth,int currentLeft, LinnaeanRankClassification higherClass ) throws Exception {
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
        String cname = getCanonical(name);
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
                break;
        }
        while (children != null && children.scoreDocs.length > 0) {
            ScoreDoc lastChild = null;
            for (ScoreDoc child : children.scoreDocs) {
                lastChild = child;
                Document cdoc = lsearcher.doc(child.doc);
                //child, currentDepth + 1, right + 1, map.toMap, dao)
                right = addIndex(cdoc, currentDepth + 1, right + 1, newcl);
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
        Document indexDoc = this.createALAIndexDocument(name, doc.get(NameIndexField.ID.toString()), lsid, doc.get(NameIndexField.AUTHOR.toString()),doc.get(NameIndexField.RANK.toString()),doc.get(NameIndexField.RANK_ID.toString()), Integer.toString(left), Integer.toString(right), newcl, nameComplete, boost);
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
     *
     * @param name
     * @return The canonical form of the supplied name.
     */
    private String getCanonical(String name){
        try {
            ParsedName pn = parser.parse(name);
            if(pn.isParsableType()){
                return pn.canonicalName();
            }
        } catch(Exception e){
            //do nothing
        }
        return name;
    }

    /**
     * Adds the synonyms to the indexed based on the dwca.  A synonym is where the id, lsid is different to the accepted lsid
     * @param dwcaDir
     */
    private void addSynonymsToIndex(File dwcaDir) throws Exception {
        Archive archive = ArchiveFactory.openArchive(dwcaDir);
        Iterator<DarwinCoreRecord> it = archive.iteratorDwc();
        int i = 0;
        int count = 0;
        ArchiveField nameCompleteField = archive.getCore().getField("nameComplete");
        org.gbif.dwc.terms.Term nameCompleteTerm = nameCompleteField == null ? null : nameCompleteField.getTerm();
        while(it.hasNext()){
            DarwinCoreRecord dwcr = it.next();
            i++;
            String lsid = dwcr.getTaxonID() != null ? dwcr.getTaxonID() : dwcr.getId();
            String id = dwcr.getId();
            String acceptedId = dwcr.getAcceptedNameUsageID();
            String nameComplete = nameCompleteTerm == null ? null: dwcr.getProperty(nameCompleteTerm);
            String scientificName = dwcr.getScientificName();
            String scientificNameAuthorship = dwcr.getScientificNameAuthorship();
            nameComplete = this.buildNameComplete(scientificName, scientificNameAuthorship, nameComplete);
            float boost = this.getBoost(dwcr.getDatasetID(), -1);
            if(StringUtils.isNotEmpty(acceptedId) && (!StringUtils.equals(acceptedId , id) && !StringUtils.equals(acceptedId, lsid))){
                count++;
                //we have a synonym that needs to be load
                try {
                    if(log.isDebugEnabled()){
                        log.debug("Scientific name:  " + dwcr.getScientificName() + ", LSID:  " + dwcr.getId());
                    }
                    Document doc = createALASynonymDocument(
                            scientificName,
                            scientificNameAuthorship,
                            nameComplete,
                            dwcr.getId(),
                            lsid,
                            lsid,
                            dwcr.getAcceptedNameUsageID(),
                            dwcr.getAcceptedNameUsageID(),
                            boost,
                            dwcr.getTaxonomicStatus());

                    if(doc != null){
                        writer.addDocument(doc);
                    } else {
                        log.warn("Problem processing scientificName:  " + dwcr.getScientificName() + ", ID:  " + dwcr.getId() + ", LSID:  " + lsid);
                    }
                } catch (Exception e){
                    log.error("Exception thrown processing Scientific name:  " + dwcr.getScientificName() + ", LSID:  " + dwcr.getId());
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

        Options options = new Options();
        options.addOption("v", "version", false, "Retrieve version information");
        options.addOption("h", "help", false, "Retrieve options");
        options.addOption("all", false, "Generates the load index and search index");
        options.addOption("load", false, "Generate the load index only. " +
                "The load index is a temporary index generated from the raw data files" +
                " used to load the main search index");
        options.addOption("search", false, "Generates the search index. A load index must already be created for this to run.");
        options.addOption("irmng", true, "The absolute path to the unzipped irmng DwCA. IRMNG is used to detect homonyms. Defaults to " + DEFAULT_IRMNG);
        options.addOption("dwca", true, "The absolute path to the unzipped DwCA for the scientific names. If  Defaults to " + DEFAULT_DWCA + " See also, the recurse option");
        options.addOption("recurse", false, "Recurse through the sub-directories of the dwca directory, looking for directories with a meta.xml");
        options.addOption("priorities", true, "A properties file containing priority multiplers for the different data sources, keyed by datasetID->float. Defaults to " + DEFAULT_PRIORITIES);
        options.addOption("target", true, "The target directory to write the new name index to. Defaults to " + DEFAULT_TARGET_DIR);
        options.addOption("tmp", true, "The tmp directory for the load index. Defaults to " + DEFAULT_TMP_DIR);
        options.addOption("common", true, "The common (vernacular) name file. Defaults to " + DEFAULT_COMMON_NAME);
        options.addOption("testSearch", true, "Debug a name search. This uses the target directory to search against.");

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
                    System.out.println("Search for name");
                    ALANameSearcher searcher = new ALANameSearcher(line.getOptionValue("target", DEFAULT_TARGET_DIR));
                    NameSearchResult nsr = searcher.searchForRecord(line.getOptionValue("testSearch"));
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
            File prioritiesFile = new File(line.getOptionValue("priorities", DEFAULT_PRIORITIES));
            Properties priorities = new Properties();
            if (prioritiesFile.exists())
                priorities.load(new FileInputStream(prioritiesFile));
            List<File> dwcas = new ArrayList<File>();
            File base = new File(line.getOptionValue("dwca", DEFAULT_DWCA));
            findDwcas(base, recurse, dwcas);
            if (dwcas.isEmpty()) {
                log.warn("No DwCA directories found under " + base);
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
            if (load) {
                for (File dwca : dwcas)
                    indexer.createLoadingIndex(dwca);
                indexer.commitLoadingIndexes();
            }
            indexer.generateIndex();
            for (File dwca: dwcas)
                indexer.create(dwca);
            indexer.indexCommonNames(commonNameFile);
            indexer.createIrmng(irmngFile);
            indexer.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
