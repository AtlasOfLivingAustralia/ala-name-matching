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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.gbif.api.exception.UnparsableException;
import org.gbif.api.model.checklistbank.ParsedName;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.nameparser.PhraseNameParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The API used to perform a search on the ALA Name Matching Lucene Index.  It follows the following
 * algorithm when trying to find a match:
 * <p/>
 * 1. Search for a direct match for supplied name on the name field (with the optional rank provided).
 * <p/>
 * 2. Search for a match on the alternative name field (with optional rank)
 * <p/>
 * 3. Generate a searchable canonical name for the supplied name.  Search for a match on
 * the searchable canonical field using the generated name
 * <p/>
 *
 * When a match is found the existence of homonyms are checked.  Where a homonym exists,
 * if the kingdom of the result does not match the supplied kingdom a HomonymException is thrown.
 *
 * For more details about the algorithm please see
 * http://code.google.com/p/ala-portal/wiki/ALANames#Understanding_the_Name_Matching_Algorithm
 *
 *
 * @author Natasha
 */
public class ALANameSearcher {
    /** Don't consider taxa below this limit for matches, unless there's nothing else */
    public static float MATCH_LIMIT = 0.5f;

    protected Log log = LogFactory.getLog(ALANameSearcher.class);
    protected DirectoryReader cbReader, irmngReader, vernReader;
    protected IndexSearcher cbSearcher, irmngSearcher, vernSearcher, idSearcher;
    protected ThreadLocal<QueryParser> queryParser;
    protected ThreadLocal<QueryParser> idParser;
    protected TaxonNameSoundEx tnse;
    protected PhraseNameParser parser;
    public static final Pattern virusStopPattern = Pattern.compile(" virus| ictv| ICTV");
    public static final Pattern voucherRemovePattern = Pattern.compile(" |,|&|\\.");
    public static final Pattern affPattern = Pattern.compile("([\\x00-\\x7F\\s]*) aff[#!?\\\\. ]([\\x00-\\x7F\\s]*)");
    public static final Pattern cfPattern = Pattern.compile("([\\x00-\\x7F\\s]*) cf[#!?\\\\. ]([\\x00-\\x7F\\s]*)");

    /**
     * A set of names that are cross rank homonyms.
     */
    private Set crossRankHomonyms;

    public ALANameSearcher(){}

    /**
     * Creates a new name searcher. Using the indexDirectory
     * as the source directory
     *
     * @param indexDirectory The directory that contains the index files for the scientific names, irmng and vernacular names.
     * @throws CorruptIndexException
     * @throws IOException
     */
    public ALANameSearcher(String indexDirectory) throws IOException {
        //Initialise CB index searching items
        log.debug("Creating the search object for the name matching api...");
        //make the query parsers thread safe
        queryParser = new ThreadLocal<QueryParser>() {
            @Override
            protected QueryParser initialValue() {
                QueryParser qp = new QueryParser("genus", LowerCaseKeywordAnalyzer.newInstance());
                qp.setFuzzyMinSim(0.8f); //fuzzy match similarity setting. used to match the authorship.
                return qp;
            }
        };
        idParser = new ThreadLocal<QueryParser>() {
            @Override
            protected QueryParser initialValue() {
                return new QueryParser( "lsid", new org.apache.lucene.analysis.core.KeywordAnalyzer());
            }
        };

        cbReader = DirectoryReader.open(FSDirectory.open(createIfNotExist(indexDirectory + File.separator + "cb")));//false
        cbSearcher = new IndexSearcher(cbReader);
        //Initialise the IRMNG index searching items
        irmngReader = DirectoryReader.open(FSDirectory.open(createIfNotExist(indexDirectory + File.separator + "irmng")));
        irmngSearcher = new IndexSearcher(irmngReader);
        //initialise the Common name index searching items
        vernReader = DirectoryReader.open(FSDirectory.open(createIfNotExist(indexDirectory + File.separator + "vernacular")));
        vernSearcher = new IndexSearcher(vernReader);
        //initialise the identifier index
        idSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(createIfNotExist(indexDirectory + File.separator + "id"))));
        tnse = new TaxonNameSoundEx();
        parser = new PhraseNameParser();
        crossRankHomonyms = au.org.ala.names.util.FileUtils.streamToSet(
                this.getClass().getClassLoader().getResourceAsStream("au/org/ala/homonyms/cross_rank_homonyms.txt"), new java.util.HashSet<String>(), true);
    }

    private Path createIfNotExist(String indexDirectory) throws IOException {

        File idxFile = new File(indexDirectory);
        Path path = Paths.get(indexDirectory);
        if (!idxFile.exists()) {
            FileUtils.forceMkdir(idxFile);
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig conf = new IndexWriterConfig(analyzer);
            IndexWriter iw = new IndexWriter(FSDirectory.open(path), conf);
            iw.commit();
            iw.close();
        }
        return path;
    }

    /**
     * Dumps a list of the species LSID's that are contained in the index.
     */
    public void dumpSpecies() {
        try {
            OutputStreamWriter fileOut = new OutputStreamWriter(new FileOutputStream("/data/species.txt"), "UTF-8");
            Term term = new Term("rank", "species");
            TopDocs hits = cbSearcher.search(new TermQuery(term), 2000000);

            for (ScoreDoc sdoc : hits.scoreDocs) {
                Document doc = cbReader.document(sdoc.doc);
                if (doc.getField("synonym") == null) {
                    String lsid = StringUtils.trimToNull(doc.getField("lsid").stringValue());
                    if (lsid == null)
                        lsid = doc.getField("id").stringValue();
                    fileOut.write(lsid + "\n");
                }
            }
            fileOut.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Searches the index for the supplied name with or without fuzzy name matching.
     * Returns null when there is no result
     * or the LSID for the first result. Where no LSID exist for the record the
     * CB ID is returned instead
     *
     * @param name
     * @param fuzzy look for a fuzzy match
     * @return
     * @throws HomonymException when an unresolved homonym is detected
     */
    public String searchForLSID(String name, boolean fuzzy) throws SearchResultException {
        return searchForLSID(name, null, fuzzy);
    }

    /**
     * Search for lsid with or without fuzzy matching. Ignoring or using homonyms.
     * @param name
     * @param fuzzy when true fuzzy matches are accepted
     * @param ignoreHomonyms  When true homonyms will be ignored if a single result is located.
     * @return
     * @throws SearchResultException
     */
    public String searchForLSID(String name, boolean fuzzy, boolean ignoreHomonyms) throws SearchResultException {
        return searchForLSID(name, null, fuzzy, ignoreHomonyms);
    }

    /**
     * Searches for the name without using fuzzy name matching...
     *
     * @param name scientific name for a taxon
     * @see #searchForLSID(java.lang.String, boolean)
     */
    public String searchForLSID(String name) throws SearchResultException {
        return searchForLSID(name, false);
    }

    /**
     * Searches the index for the supplied name of the specified rank with or without
     * fuzzy name matching.  Returns
     * null when there is no result or the LSID for the first result.
     * Where no LSID exist for the record the
     * CB ID is returned instead
     * <p/>
     * When the result is a synonym the "accepted" taxons's LSID is returned.
     *
     * @param name
     * @param rank
     * @param fuzzy look for a fuzzy match
     * @return
     * @throws HomonymException when an unresolved homonym is detected
     */
    public String searchForLSID(String name, RankType rank, boolean fuzzy) throws SearchResultException {
        return searchForLSID(name, null, rank, fuzzy, false);
    }

    /**
     * Searches for the supplied name of the specified rank with or without fuzzy name matching. When ignoreHomonyms is true
     * A homonym exception will only be thrown if a homonym is detected where the ALA has both names.
     *
     * @param name
     * @param rank
     * @param fuzzy
     * @param ignoreHomonyms
     * @return
     * @throws SearchResultException
     */
    public String searchForLSID(String name, RankType rank, boolean fuzzy, boolean ignoreHomonyms) throws SearchResultException {
        return searchForLSID(name, null, rank, fuzzy, ignoreHomonyms);
    }

    /**
     * Searches for an LSID of the supplied name and rank without a fuzzy match...
     *
     * @param name
     * @param rank
     * @return
     * @throws SearchResultException
     * @see #searchForLSID(java.lang.String, au.org.ala.names.model.RankType, boolean)
     */
    public String searchForLSID(String name, RankType rank) throws SearchResultException {
        return searchForLSID(name, null, rank, false, false);
    }

    /**
     * Searches for the LSID of the supplied name and rank. Using the kingdom to
     * resolve homonym issues.
     *
     * @param name
     * @param kingdom
     * @param scientificName
     * @param rank
     * @return
     * @throws SearchResultException
     * @deprecated Use {@link #searchForLSID(String, au.org.ala.names.model.LinnaeanRankClassification, au.org.ala.names.model.RankType)}  instead.
     * It is more extensible to supply a classification object then a list of higher classification
     */
    @Deprecated
    public String searchForLSID(String name, String kingdom, String scientificName, RankType rank) throws SearchResultException {
        LinnaeanRankClassification cl = new LinnaeanRankClassification(kingdom, scientificName);
        return searchForLSID(name, cl, rank, false, false);
    }

    /**
     * Search for an LSID based on the supplied name, classification and rank with or without fuzzy name matching.
     * <p/>
     * When a classification is supplied it is used for 2 purposes:
     * <ol>
     * <li> To try and resolve potential homonyms</li>
     * <li> To provide "optional" components to the search.  Thus an incorrect higher
     * classification will not prevent matches from occurring.</li>
     * </ol>
     * If it is not provided and a homonym is detected in the result a HomonymException is thrown.
     *
     * @param name
     * @param cl    The high taxa that form the classification for the search item
     * @param rank
     * @param fuzzy look for a fuzzy match
     * @return
     * @throws HomonymException When an unresolved homonym is detected
     */
    public String searchForLSID(String name, LinnaeanRankClassification cl, RankType rank, boolean fuzzy, boolean ignoreHomonym) throws SearchResultException {
        String lsid = null;
        NameSearchResult result = searchForRecord(name, cl, rank, fuzzy, ignoreHomonym);
        if (result != null) {
            if (result.getAcceptedLsid() == null && result.getLsid() == null) {
                log.warn("LSID missing for [name=" + name + ", id=" + result.getId() + "]");
            } else {
                lsid = result.getAcceptedLsid() != null ? result.getAcceptedLsid() : result.getLsid();
            }
        }
        return lsid;
    }

    /**
     * Search for an LSID with the supplied classification without a fuzzy match.
     * Supplying to classification in this way allows the API to try and ascertain the rank and
     * the correct scientific name to use.
     *
     * @param cl the classification to work with
     * @return An LSID for the taxon or null if nothing matched or homonym issues detected
     * @throws SearchResultException
     */
    public String searchForLSID(LinnaeanRankClassification cl, boolean recursiveMatching) throws SearchResultException {
        NameSearchResult nsr = searchForRecord(cl, recursiveMatching);
        if (nsr != null) {
            return nsr.getLsid();
        }
        return null;
    }

    /**
     * Updates the supplied classification so that the supplied ID's are substituted with GUIDs.
     *
     * @param cl
     */
    public void updateClassificationWithGUID(LinnaeanRankClassification cl) {
        if (cl.getKid() != null) {
            cl.setKid(searchForLsidById(cl.getKid()));
        }
        if (cl.getPid() != null)
            cl.setPid(searchForLsidById(cl.getPid()));
        if (cl.getCid() != null)
            cl.setCid(searchForLsidById(cl.getCid()));
        if (cl.getOid() != null)
            cl.setOid(searchForLsidById(cl.getOid()));
        if (cl.getFid() != null)
            cl.setFid(searchForLsidById(cl.getFid()));
        if (cl.getGid() != null)
            cl.setGid(searchForLsidById(cl.getGid()));
        if (cl.getSid() != null)
            cl.setSid(searchForLsidById(cl.getSid()));
    }

    /**
     * Search for a result - optionally allowing for a recursive search
     * @param cl The classification to perform the match on
     * @param recursiveMatching When true attempt to match on higher classification
     * @return
     * @throws SearchResultException
     */
    public NameSearchResult searchForRecord(LinnaeanRankClassification cl, boolean recursiveMatching) throws SearchResultException {
        return searchForRecord(cl, recursiveMatching, false, false);
    }

    /**
     * Searches for a result returning a metrics of the result.  Optionally allowing for a recursive match
     * @param cl  The classification to perform the match on
     * @param recursiveMatching When true attempt to match on higher classification
     * @return The MetricResultDTO for the matched result.
     * @throws SearchResultException
     */
    public MetricsResultDTO searchForRecordMetrics(LinnaeanRankClassification cl, boolean recursiveMatching) throws SearchResultException {
        return searchForRecordMetrics(cl, recursiveMatching, false, false);
    }

    /**
     * Searches for a result returning a metrics of the result.  Optionally allowing for a recursive match and fuzzy matching.
     * @param cl The classification to perform the match on
     * @param recursiveMatching When true attempt to match on higher classification
     * @param fuzzy When true allow fuzzy matching on scientific names
     * @return The MetricResultDTO for the matched result.
     * @throws SearchResultException
     */
    public MetricsResultDTO searchForRecordMetrics(LinnaeanRankClassification cl, boolean recursiveMatching, boolean fuzzy) throws SearchResultException {
        return searchForRecordMetrics(cl, recursiveMatching, false, fuzzy);
    }

    /**
     *  Search for a result - optionally allowing for a recursive search and fuzzy matching
     * @param cl The classification to perform the match on
     * @param recursiveMatching When true attempt to match on higher classification
     * @param fuzzy When true allow fuzzy matching on scientific names
     * @return
     * @throws SearchResultException
     */
    public NameSearchResult searchForRecord(LinnaeanRankClassification cl, boolean recursiveMatching, boolean fuzzy) throws SearchResultException {
        return searchForRecord(cl, recursiveMatching, false, fuzzy);
    }

    /**
     * Search for an LSID with the supplied classification without a fuzzy match.
     * Supplying to classification in this way allows the API to try and ascertain the rank and
     * the correct scientific name to use.
     *
     * @param cl                the classification to work with
     * @param recursiveMatching whether to try matching to a higher taxon when leaf taxa matching fails
     * @return An LSID for the taxon or null if nothing matched or homonym issues detected
     * @throws SearchResultException
     */
    public NameSearchResult searchForRecord(LinnaeanRankClassification cl, boolean recursiveMatching, boolean addGuids, boolean fuzzy) throws SearchResultException {
        MetricsResultDTO res = searchForRecordMetrics(cl, recursiveMatching, addGuids, fuzzy);
        if (res.getLastException() != null)
            throw res.getLastException();
        return res.getResult();
    }

    /**
     * Search for a specific name returning extra metrics that can be reported as name match quality...
     *
     * @param cl
     * @param recursiveMatching
     * @param addGuids  When true will look up the guids for the higher classification (deprecated param as these are now stored with the index)
     * @param fuzzy When true allow fuzzy matching on scientific names
     * @return
     */
    public MetricsResultDTO searchForRecordMetrics(LinnaeanRankClassification cl, boolean recursiveMatching, boolean addGuids, boolean fuzzy) {
        return searchForRecordMetrics(cl, recursiveMatching, addGuids, fuzzy, false);
    }

    /**
     * Searches for a result returning a metrics of the result.  Optionally allowing for a recursive match and fuzzy matching and ignoring homonyms.
     * @param cl The classification to perform the match on
     * @param recursiveMatching When true attempt to match on higher classification
     * @param addGuids When true will look up the guids for the higher classification (deprecated param as these are now stored with the index)
     * @param fuzzy When true allow fuzzy matching on scientific names
     * @param ignoreHomonym When true ignore the homonym exception if a single result is returned.
     * @return
     */
    public MetricsResultDTO searchForRecordMetrics(LinnaeanRankClassification cl, boolean recursiveMatching, boolean addGuids, boolean fuzzy, boolean ignoreHomonym) {

        //set up the Object to return
        MetricsResultDTO metrics = new MetricsResultDTO();


        RankType rank = cl.getRank()!=null ?RankType.getForStrRank(cl.getRank()):null;
        String name = cl.getScientificName();
        String originalName = name;

        NameSearchResult nsr = null;
        metrics.setErrors(new HashSet<ErrorType>());

        if (name == null) {
            //ascertain the rank and construct the scientific name
            if (StringUtils.isNotEmpty(cl.getInfraspecificEpithet()) && !isInfraSpecificMarker(cl.getSubspecies())) {
                rank = RankType.SUBSPECIES;
                //construct the full scientific name from the parts
                if (StringUtils.isNotEmpty(cl.getGenus()) && StringUtils.isNotEmpty(cl.getSpecificEpithet())) {
                    name = cl.getGenus() + " " + cl.getSpecificEpithet() + " " + cl.getInfraspecificEpithet();
                }
            } else if (StringUtils.isNotEmpty(cl.getSubspecies()) && !isInfraSpecificMarker(cl.getSubspecies())) {
                rank = RankType.SUBSPECIES;
                name = cl.getSubspecies();
            } else if (StringUtils.isNotEmpty(cl.getSpecificEpithet()) && !isSpecificMarker(cl.getSpecies())) {
                rank = RankType.SPECIES;
                //construct the full scientific name from the parts
                if (StringUtils.isNotEmpty(cl.getGenus())) {
                    name = cl.getGenus() + " " + cl.getSpecificEpithet();
                }
            } else if (StringUtils.isNotEmpty(cl.getSpecies()) && !isSpecificMarker(cl.getSpecies())) {
                rank = RankType.SPECIES;
                //construct the full scientific name from the parts
                name = cl.getSpecies();
                //check to see of the name is a binomial
                if (!name.trim().contains(" ")) {
                    //construct the binomial
                    if (StringUtils.isNotEmpty(cl.getGenus())) {
                        name = cl.getGenus() + " " + cl.getSpecificEpithet();
                    } else {
                        name = null;
                    }
                }
            } else if (StringUtils.isNotEmpty(cl.getGenus())) {
                rank = RankType.GENUS;
                //construct the full scientific name from the parts
                name = cl.getGenus();
            } else if (StringUtils.isNotEmpty(cl.getFamily())) {
                rank = RankType.FAMILY;
                //construct the full scientific name from the parts
                name = cl.getFamily();
            } else if (StringUtils.isNotEmpty(cl.getOrder())) {
                rank = RankType.ORDER;
                //construct the full scientific name from the parts
                name = cl.getOrder();
            } else if (StringUtils.isNotEmpty(cl.getKlass())) {
                rank = RankType.CLASS;
                //construct the full scientific name from the parts
                name = cl.getKlass();
            } else if (StringUtils.isNotEmpty(cl.getPhylum())) {
                rank = RankType.PHYLUM;
                //construct the full scientific name from the parts
                name = cl.getPhylum();
            } else if (StringUtils.isNotEmpty(cl.getKingdom())) {
                rank = RankType.KINGDOM;
                //construct the full scientific name from the parts
                name = cl.getKingdom();
            }
            originalName = name;
            // nsr = searchForRecord(name, cl, rank, false);
        } else {
            //check to see if the rank can be determined by matching the scentific name to one of values
            if (rank == null && StringUtils.equalsIgnoreCase(name, cl.getSubspecies()))
                rank = RankType.SUBSPECIES;
            else if (rank == null && StringUtils.equalsIgnoreCase(name, cl.getSpecies()))
                rank = RankType.SPECIES;
            else if (rank == null && StringUtils.equalsIgnoreCase(name, cl.getGenus()))
                rank = RankType.GENUS;
            else if (rank == null && StringUtils.equalsIgnoreCase(name, cl.getFamily()))
                rank = RankType.FAMILY;
            else if (rank == null && StringUtils.equalsIgnoreCase(name, cl.getOrder()))
                rank = RankType.ORDER;
            else if (rank == null && StringUtils.equalsIgnoreCase(name, cl.getKlass()))
                rank = RankType.CLASS;
            else if (rank == null && StringUtils.equalsIgnoreCase(name, cl.getPhylum()))
                rank = RankType.PHYLUM;
            else if (rank == null && StringUtils.equalsIgnoreCase(name, cl.getKingdom()))
                rank = RankType.KINGDOM;

            if (rank == null) {
                if (recursiveMatching) {
                    if (name.endsWith(" sp") || name.endsWith(" sp.")) {
                        name = name.substring(0, name.lastIndexOf(" "));
                        cl.setGenus(name);
                    }
                }
                //check to see if the rank can be determined from the scientific name
                try {
                    ParsedName cn = parser.parse(name.replaceAll("\\?", ""));
                    if (cn != null && cn.getType() == NameType.DOUBTFUL) {
                        //if recursive set the issue
                        if (recursiveMatching) {
                            name = cn.getGenusOrAbove();
                            rank = RankType.GENUS;
                            metrics.setNameType(NameType.DOUBTFUL);

                        }
                    } else if (cn != null && cn.isBinomial()) {
                        //set the genus if it is empty
                        if (StringUtils.isEmpty(cl.getGenus()))
                            cl.setGenus(cn.getGenusOrAbove());
                        if (cn.getRank() == null && cn.getCultivarEpithet() == null && cn.isParsableType()) {

                            if (cn.getInfraSpecificEpithet() != null) {
                                rank = RankType.SUBSPECIES;
                                //populate the species if it is empty
                                if (StringUtils.isEmpty(cl.getSpecies()))
                                    cl.setSpecies(cn.getGenusOrAbove() + " " + cn.getSpecificEpithet());
                            } else
                                rank = RankType.SPECIES;
                        } else if (cn.getCultivarEpithet()!= null) {
                            rank = RankType.CULTIVAR;
                        } else if (cn.getRank() != null) {
                            // It is not necesary to update the rank based on rank markers at this point
                            // This is because it is done at the lowest level possible just before the search is performed

                        }
                    }
                } catch (org.gbif.api.exception.UnparsableException e) {
                    //TODO log error maybe??
                    metrics.setNameType(e.type);
                }
            }
        }

        nsr = performErrorCheckSearch(name.replaceAll("\\?", ""), cl, rank, fuzzy, ignoreHomonym, metrics);

        if (nsr == null && recursiveMatching) {
            //get the name type for the original name
            //remove the authorship from the search
            String authorship = cl.getAuthorship();
            cl.setAuthorship(null);
            try {
                ParsedName pn = parser.parse(name);
                metrics.setNameType(pn.getType());
                if (pn.isBinomial() && pn.getType() != NameType.DOUBTFUL && (pn.getType() != NameType.INFORMAL || (pn.getRank() != null && pn.getRank().isInfraspecific())) && (rank == null || rank.getId() >= 7000))
                    nsr = performErrorCheckSearch(pn.canonicalSpeciesName(), cl, null, fuzzy, ignoreHomonym, metrics);
                if (nsr == null && (pn.getType() == NameType.DOUBTFUL || (rank != null && rank.getId() <= 7000) || rank == null))
                    nsr = performErrorCheckSearch(pn.getGenusOrAbove(), cl, null, fuzzy, ignoreHomonym, metrics);

            } catch (Exception ex) {
            }
            if (nsr == null && rank != RankType.SPECIES
                    && ((StringUtils.isNotEmpty(cl.getSpecificEpithet()) && !isSpecificMarker(cl.getSpecificEpithet())) ||
                    (StringUtils.isNotEmpty(cl.getSpecies()) && !isSpecificMarker(cl.getSpecies())))) {
                name = cl.getSpecies();
                if (StringUtils.isEmpty(name))
                    name = cl.getGenus() + " " + cl.getSpecificEpithet();

                nsr = performErrorCheckSearch(name, cl, RankType.SPECIES, fuzzy, ignoreHomonym, metrics);
            }
            if (nsr == null && cl.getGenus() != null) {
                nsr = performErrorCheckSearch(cl.getGenus(), cl, RankType.GENUS, fuzzy, ignoreHomonym, metrics);
            }
            if (nsr == null && cl.getFamily() != null) {
                nsr = performErrorCheckSearch(cl.getFamily(), cl, RankType.FAMILY, fuzzy, ignoreHomonym, metrics);
            }
            if (nsr == null && cl.getOrder() != null) {
                nsr = performErrorCheckSearch(cl.getOrder(), cl, RankType.ORDER, fuzzy, ignoreHomonym, metrics);
            }
            if (nsr == null && cl.getKlass() != null) {
                nsr = performErrorCheckSearch(cl.getKlass(), cl, RankType.CLASS, fuzzy, ignoreHomonym, metrics);
            }
            if (nsr == null && cl.getPhylum() != null) {
                nsr = performErrorCheckSearch(cl.getPhylum(), cl, RankType.PHYLUM, fuzzy, ignoreHomonym, metrics);
            }
            if (nsr == null && cl.getKingdom() != null) {
                nsr = performErrorCheckSearch(cl.getKingdom(), cl, RankType.KINGDOM, fuzzy, ignoreHomonym, metrics);
            }

            if (nsr != null) {
                nsr.setMatchType(MatchType.RECURSIVE);
            }
            //rest the author
            cl.setAuthorship(authorship);
        }

        //now start to get the metric object ready
        if (metrics.getNameType() == null) {
            try {
                ParsedName pn = parser.parse(originalName);
                metrics.setNameType(pn.getType());
            } catch (UnparsableException e) {
                metrics.setNameType(e.type);
            }
        }

        checkOtherIssues(originalName, metrics);
        if (nsr != null) {
            //Obtain and store the GUIDs for the classification identifiers
            if (addGuids)
                updateClassificationWithGUID(nsr.getRankClassification());
        }

        if (metrics.getErrors().size() == 0)
            metrics.getErrors().add(ErrorType.NONE);

        metrics.setResult(nsr);

        return metrics;
    }

    private void checkOtherIssues(String originalName, MetricsResultDTO metrics) {
        if (originalName.contains("?")) {
            metrics.getErrors().add(ErrorType.QUESTION_SPECIES);
            metrics.setNameType(NameType.DOUBTFUL); //a questionable species is always a doubtful name type
        }
        if (cfPattern.matcher(originalName).matches())
            metrics.getErrors().add(ErrorType.CONFER_SPECIES);
        if (affPattern.matcher(originalName).matches())
            metrics.getErrors().add(ErrorType.AFFINITY_SPECIES);
    }

    /**
     * Performs a search.  Any error's encountered will be added to the supplied error set.
     *
     * @param name scientific name ro search for
     * @param cl The classification to perform the match on
     * @param rank Rank to perform the match on , when null no specific rank
     * @param fuzzy When true allow fuzzy matching on scientific names
     * @param ignoreHomonym When true ignore the homonym exception if a single result is returned.
     * @param metrics The metrics for this search. Errors will be applied to this metric
     * @return
     */
    private NameSearchResult performErrorCheckSearch(String name, LinnaeanRankClassification cl, RankType rank, boolean fuzzy, boolean ignoreHomonym, MetricsResultDTO metrics) {
        NameSearchResult nsr = null;
        try {
            nsr = searchForRecord(name, cl, rank, fuzzy, ignoreHomonym);
        } catch (MisappliedException e) {
            metrics.setLastException(e);
            metrics.getErrors().add(e.errorType);
            nsr = e.getMatchedResult();
        } catch (ParentSynonymChildException e) {
            metrics.setLastException(e);
            metrics.getErrors().add(e.errorType);
            // Use the parent result, since we can't tell whether the name supplied is from before the reassignment or after
            nsr = e.getParentResult();
        } catch (ExcludedNameException e) {
            metrics.setLastException(e);
            metrics.getErrors().add(e.errorType);
            nsr = e.getNonExcludedName() != null ? e.getNonExcludedName() : e.getExcludedName();
        } catch (SearchResultException e) {
            metrics.setLastException(e);
            metrics.getErrors().add(e.errorType);
        }
        return nsr;
    }

    /**
     * FIXME need to include other types of marker
     *
     * @param subspecies
     * @return
     */
    private boolean isInfraSpecificMarker(String subspecies) {
        String epithet = StringUtils.trimToNull(subspecies);
        if (epithet != null) {
            if ("spp".equalsIgnoreCase(epithet) || "spp.".equalsIgnoreCase(epithet)) return true;
        }
        return false;
    }

    /**
     * FIXME need to include other types of marker
     *
     * @param species
     * @return
     */
    private boolean isSpecificMarker(String species) {
        String epithet = StringUtils.trimToNull(species);
        if (epithet != null) {
            if ("sp".equalsIgnoreCase(epithet) || "sp.".equalsIgnoreCase(epithet) || "sp.nov.".equalsIgnoreCase(species.replaceAll(" ", "")))
                return true;
        }
        return false;
    }

    /**
     * Search for an LSID based on suppled name, classification and rank without a fuzzy match...
     *
     * @param name
     * @param cl
     * @param rank
     * @return
     * @throws SearchResultException
     */
    public String searchForLSID(String name, LinnaeanRankClassification cl, RankType rank) throws SearchResultException {
        return searchForLSID(name, cl, rank, false, false);
    }

    /**
     * Searches the index for the supplied name of the specified rank.  Returns
     * null when there is no result or the result object for the first result.
     *
     * @param name
     * @param rank
     * @param fuzzy look for a fuzzy match
     * @return
     */
    public NameSearchResult searchForRecord(String name, RankType rank, boolean fuzzy) throws SearchResultException {
        return searchForRecord(name, null, rank, fuzzy);
    }

    /**
     * Searches index for the supplied name and rank without a fuzzy match.
     *
     * @param name
     * @return
     * @throws SearchResultException
     */
    public NameSearchResult searchForRecord(String name) throws SearchResultException {
        return searchForRecord(name, null, false);
    }

    /**
     * Searches index for the supplied name and rank without a fuzzy match.
     *
     * @param name
     * @param rank
     * @return
     * @throws SearchResultException
     */
    public NameSearchResult searchForRecord(String name, RankType rank) throws SearchResultException {
        return searchForRecord(name, rank, false);
    }

    /**
     * Returns the accepted LSID for the supplied classification.
     * <p/>
     * If a synonym is matched the accepted LSID is retrieved and returned.
     * <p/>
     * It uses the default error handling.  For example matches to excluded concepts are permitted.
     * <p/>
     * Do not use this method if you would like control over how error conditions are handled.
     *
     * @param cl
     * @param fuzzy
     * @return
     */
    public String searchForAcceptedLsidDefaultHandling(LinnaeanRankClassification cl, boolean fuzzy) {
        return searchForAcceptedLsidDefaultHandling(cl, fuzzy, false);
    }

    public String searchForAcceptedLsidDefaultHandling(LinnaeanRankClassification cl, boolean fuzzy, boolean ignoreHomonyms) {
        NameSearchResult nsr = searchForAcceptedRecordDefaultHandling(cl, fuzzy, ignoreHomonyms);
        if (nsr == null)
            return null;
        return nsr.getLsid();
    }

    /**
     * Returns the accepted result for the supplied classification.
     * <p/>
     * If a synonym is matched the accepted result is retrieved and returned.
     * <p/>
     * It uses the default error handling.  For example matches to excluded concepts are permitted.
     * <p/>
     * Do not use this method if you would like control over how error conditions are handled.
     *
     * @param cl
     * @param fuzzy
     * @return
     */
    public NameSearchResult searchForAcceptedRecordDefaultHandling(LinnaeanRankClassification cl, boolean fuzzy) {
        return searchForAcceptedRecordDefaultHandling(cl, fuzzy, false);
    }

    public NameSearchResult searchForAcceptedRecordDefaultHandling(LinnaeanRankClassification cl, boolean fuzzy, boolean ignoreHomonym) {
        NameSearchResult nsr = null;
        try {
            nsr = searchForRecord(cl.getScientificName(), cl, null, fuzzy, ignoreHomonym);
        } catch (MisappliedException e) {
            nsr = e.getMatchedResult();
        } catch (ParentSynonymChildException e) {
            // Use the parent result, since we can't tell whether the name supplied is from before the reassignment or after
            nsr = e.getParentResult();
        } catch (ExcludedNameException e) {
            nsr = e.getNonExcludedName() != null ? e.getNonExcludedName() : e.getExcludedName();
        } catch (SearchResultException e) {
            //do nothing
        }

        //now check for accepted concepts
        if (nsr != null && nsr.isSynonym())
            nsr = searchForRecordByLsid(nsr.getAcceptedLsid());

        return nsr;
    }

    /**
     * Searches for a record based on the supplied name and rank. It uses the kingdom and genus to resolve homonyms.
     *
     * @param name
     * @param kingdom
     * @param genus
     * @param rank
     * @return
     * @throws SearchResultException
     * @deprecated Use {@link #searchForRecord(java.lang.String, au.org.ala.names.model.LinnaeanRankClassification, au.org.ala.names.model.RankType, boolean)} instead.
     * It is more extensible to supply a classification object then a list of higher classification
     */
    @Deprecated
    public NameSearchResult searchForRecord(String name, String kingdom, String genus, RankType rank) throws SearchResultException {
        LinnaeanRankClassification cl = new LinnaeanRankClassification(kingdom, genus);
        return searchForRecord(name, cl, rank, false);
    }

    public NameSearchResult searchForRecord(String name, LinnaeanRankClassification cl, RankType rank, boolean fuzzy) throws SearchResultException {
        return searchForRecord(name, cl, rank, fuzzy, false);
    }

    /**
     * Searches for a record based on the supplied name, rank and classification
     * with or without fuzzy name matching.
     *
     * @param name
     * @param cl
     * @param rank
     * @param fuzzy
     * @return
     * @throws SearchResultException
     */
    public NameSearchResult searchForRecord(String name, LinnaeanRankClassification cl, RankType rank, boolean fuzzy, boolean ignoreHomonyms) throws SearchResultException {
        //search for more than 1 term in case homonym resolution takes place at a lower level??
        List<NameSearchResult> results = searchForRecords(name, rank, cl, 10, fuzzy, ignoreHomonyms);
        if (results != null && results.size() > 0)
            return results.get(0);

        return null;
    }

    /**
     * Searches for a record based on the supplied name, classification and rank without fuzzy name matching
     *
     * @param name
     * @param cl
     * @param rank
     * @return
     * @throws SearchResultException
     */
    public NameSearchResult searchForRecord(String name, LinnaeanRankClassification cl, RankType rank) throws SearchResultException {
        return searchForRecord(name, cl, rank, false);
    }

    /**
     * Returns the records that has the supplied checklist bank id
     *
     * @param id
     * @return
     */
    public NameSearchResult searchForRecordByID(String id) {
        try {
            List<NameSearchResult> results = performSearch(ALANameIndexer.IndexField.ID.toString(), id, null, null, 1, null, false, idParser.get());
            if (results.size() > 0) {
                results.get(0).setMatchType(MatchType.TAXON_ID);
                return results.get(0);
            }
        } catch (SearchResultException e) {
            //this should not happen as we are  not checking for homonyms
            //homonyms should only be checked if a search is being performed by name
        } catch (IOException e) {
        }
        return null;
    }

    /**
     * Gets the LSID for the record that has the supplied checklist bank id.
     *
     * @param id
     * @return
     */
    public String searchForLsidById(String id) {
        NameSearchResult result = searchForRecordByID(id);
        if (result != null)
            return result.getAcceptedLsid() != null ? result.getAcceptedLsid() : result.getLsid();
        return null;
    }

    /**
     * Searches for records with the specified name and rank with or without fuzzy name matching
     *
     * @param name
     * @param rank
     * @param fuzzy search for a fuzzy match
     * @return
     */
    public List<NameSearchResult> searchForRecords(String name, RankType rank, boolean fuzzy) throws SearchResultException {
        return searchForRecords(name, rank, null, 10, fuzzy);
    }

    /**
     * Searches for a list of results for the supplied name, classification and rank without fuzzy match
     *
     * @param name
     * @param rank
     * @param cl
     * @param max
     * @return
     * @throws SearchResultException
     */
    public List<NameSearchResult> searchForRecords(String name, RankType rank, LinnaeanRankClassification cl, int max) throws SearchResultException {
        return searchForRecords(name, rank, cl, max, false);
    }

    /**
     * Searches for the records that satisfy the given conditions using the algorithm
     * outlined in the class description.
     *
     * @param name scientific name to search for
     * @param rank Rank to perform the match on , when null no specific rank
     * @param cl The high taxa that form the classification for the search item
     * @param max     The maximum number of results to return
     * @param fuzzy   search for a fuzzy match
     * @return
     * @throws SearchResultException
     */
    public List<NameSearchResult> searchForRecords(String name, RankType rank, LinnaeanRankClassification cl, int max, boolean fuzzy) throws SearchResultException {
        return searchForRecords(name, rank, cl, max, fuzzy, true, false);
    }

    public List<NameSearchResult> searchForRecords(String name, RankType rank, LinnaeanRankClassification cl, int max, boolean fuzzy, boolean ignoreHomonyms) throws SearchResultException {
        return searchForRecords(name, rank, cl, max, fuzzy, true, ignoreHomonyms);
    }

    /**
     * The new implementation for a name search as on December 2011.  It performs the following steps in an attempt to find a match:
     * <ol>
     * <li> Exact String Match of scientific name. </li>
     * <li> Canonical String Match when the parsed name is valid </li>
     * <li> Phrase Name, genus and optionally specificEpithet match when the name is determined to be a phrase </li>
     * <li> Sounds LIke match on genus, specific epithet and optionally infra specific epithet </li>
     * </ol>
     *
     * @param name
     * @param rank
     * @param cl
     * @param max
     * @param fuzzy
     * @param clean
     * @param ignoreHomonym When true ignore the homonym exception if a single result is returned.
     * @return
     * @throws SearchResultException
     */
    private List<NameSearchResult> searchForRecords(String name, RankType rank, LinnaeanRankClassification cl, int max, boolean fuzzy, boolean clean, boolean ignoreHomonym) throws SearchResultException {
        //The name is not allowed to be null


        //Check for null name before attempting to do anything else
        if (name == null)
            throw new SearchResultException("Unable to perform search. Null value supplied for the name.");
        //Check that the scientific name supplied is NOT a rank marker.
        if (PhraseNameParser.RANK_MARKER.matcher(name).matches())
            throw new SearchResultException("Supplied scientific name is a rank marker.");

        //remove all the "stop" words from the scientific name
        try {
            name = virusStopPattern.matcher(name).replaceAll(" ").trim();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        //According to http://en.wikipedia.org/wiki/Species spp. is used as follows:
        //The authors use "spp." as a short way of saying that something applies to many species within a genus,
        //but do not wish to say that it applies to all species within that genus.
        //Thus we don't want to attempt to match on spp.
        if (name.contains("spp."))
            throw new SPPException();//SearchResultException("Unable to perform search. Can not match to a subset of species within a genus.");

        try {
            CleanedScientificName cleaned = new CleanedScientificName(name);
            NameType nameType = null;
            ParsedName pn = null;
            try {
                pn = parser.parse(cleaned.getNormalised());
                nameType = pn != null ? pn.getType() : null;
            } catch (UnparsableException e) {
                log.warn("Unable to parse " + name + ". " + e.getMessage());
            }
            //Check for the exact match
            List<NameSearchResult> hits = performSearch(NameIndexField.NAME.toString(), cleaned.getNormalised(), rank, cl, max, MatchType.EXACT, true, queryParser.get());
            if (hits == null) // situation where searcher has not been initialised
            {
                return null;
            }
            if (hits.size() > 0) {
                return hits;
            }

            //Use the parsed name and see what type of check to do next
            //at this point we don't want to match informal names
            //if(pn.getType() == NameType.informal)
            //    throw new InformalNameException();

            if (pn instanceof ALAParsedName) {
                //check the phrase name
                ALAParsedName alapn = (ALAParsedName) pn;
                String genus = alapn.getGenusOrAbove();
                String phrase = alapn.cleanPhrase;//alapn.getLocationPhraseDesciption();
                String voucher = alapn.cleanVoucher;
                //String voucher = alapn.phraseVoucher != null ? voucherRemovePattern.matcher(alapn.phraseVoucher).replaceAll("") :null;
                String specific = alapn.getRank() != null && alapn.getRank().equals(Rank.SPECIES) ? null : alapn.getSpecificEpithet();
                String[][] searchFields = new String[4][];
                searchFields[0] = new String[]{RankType.GENUS.getRank(), genus};
                searchFields[1] = new String[]{NameIndexField.PHRASE.toString(), phrase};
                searchFields[2] = new String[]{NameIndexField.VOUCHER.toString(), voucher};
                searchFields[3] = new String[]{NameIndexField.SPECIFIC.toString(), specific};
                hits = performSearch(searchFields, rank, cl, max, MatchType.PHRASE, false, queryParser.get()); //don't want to check for homonyms yet...
                if (hits.size() == 1) {
                    return hits;
                } else if (hits.size() > 1) {
                    //this represents a homonym issue between vouchers.
                    //don't throw a homonym if all results point to the same accepted concept
                    NameSearchResult commonAccepted = getCommonAcceptedConcept(hits);
                    if (commonAccepted != null)
                        return Collections.singletonList(commonAccepted);
                    throw new HomonymException(hits);
                }
            } else if (pn != null && pn.isParsableType() && pn.getType() != NameType.INFORMAL && pn.getType() != NameType.DOUBTFUL) {
                //check the canonical name
                String canonicalName = pn.canonicalName();
                if (cl == null) {
                    cl = new LinnaeanRankClassification();
                }
                //set the authorship if it has been supplied as part of the scientific name
                if (cl.getAuthorship() == null && pn.isAuthorsParsed()) {
                    cl.setAuthorship(pn.authorshipComplete());
                }
                hits = performSearch(ALANameIndexer.IndexField.NAME.toString(), canonicalName, rank, cl, max, MatchType.CANONICAL, true, queryParser.get());
                if (hits.size() > 0) {
                    return hits;
                }
                //if the parse type was a cultivar and we didn't match it check to see if we can match as a phrase name
                if (pn.getType() == NameType.CULTIVAR) {
                    String genus = pn.getGenusOrAbove();
                    String phrase = pn.getCultivarEpithet();
                    String voucher = null;
                    String specific = pn.getRank() != null && pn.getRank().equals(Rank.SPECIES) ? null : pn.getSpecificEpithet();
                    String[][] searchFields = new String[4][];
                    searchFields[0] = new String[]{RankType.GENUS.getRank(), genus};
                    searchFields[1] = new String[]{NameIndexField.PHRASE.toString(), phrase};
                    searchFields[2] = new String[]{NameIndexField.VOUCHER.toString(), voucher};
                    searchFields[3] = new String[]{NameIndexField.SPECIFIC.toString(), specific};
                    hits = performSearch(searchFields, rank, cl, max, MatchType.PHRASE, false, queryParser.get());
                    if (hits.size() > 0) {
                        return hits;
                    }
                }
            }
            //now check for a "sounds like" match if we don't have an informal name
            if (pn != null && fuzzy && pn.isBinomial() && pn.getType() != NameType.INFORMAL && pn.getType() != NameType.DOUBTFUL) {
                String genus = TaxonNameSoundEx.treatWord(pn.getGenusOrAbove(), "genus");
                String specific = TaxonNameSoundEx.treatWord(pn.getSpecificEpithet(), "species");
                String infra = pn.getInfraSpecificEpithet() == null ? null : TaxonNameSoundEx.treatWord(pn.getInfraSpecificEpithet(), "species");
                String[][] searchFields = new String[3][];
                searchFields[0] = new String[]{NameIndexField.GENUS_EX.toString(), genus};
                searchFields[1] = new String[]{NameIndexField.SPECIES_EX.toString(), specific};
                if (StringUtils.isNotEmpty(infra)) {
                    searchFields[2] = new String[]{NameIndexField.INFRA_EX.toString(), infra};
                } else {
                    searchFields[2] = new String[]{NameIndexField.INFRA_EX.toString(), "<null>"};
                }
                hits = performSearch(searchFields, rank, cl, max, MatchType.SOUNDEX, false, queryParser.get()); //don't want to check for homonyms yet...
                if (hits.size() > 0) {
                    return hits;
                }

            }
            return null;
        } catch (HomonymException e) {
            if (ignoreHomonym && e.getResults().size() == 1) {
                return e.getResults();
            } else {
                throw e;
            }
        } catch (IOException e) {
            log.warn(e.getMessage());
            return null;
        }
    }

    /**
     * If all results point to the same accepted concept it is returned.
     * Otherwise null is returned.
     *
     * @param hits
     * @return
     */
    private NameSearchResult getCommonAcceptedConcept(List<NameSearchResult> hits) {
        String acceptedLsid = null;

        for (NameSearchResult hit : hits) {
            if (acceptedLsid == null)
                acceptedLsid = hit.getAcceptedLsid() != null ? hit.getAcceptedLsid() : hit.getLsid();
            else if (hit.getAcceptedLsid() != null) {
                if (!acceptedLsid.equals(hit.getAcceptedLsid()))
                    return null;
            } else {
                if (!acceptedLsid.equals(hit.getLsid()))
                    return null;
            }
        }
        return  acceptedLsid == null ? null : searchForRecordByLsid(acceptedLsid);
    }

    private List<NameSearchResult> performSearch(String field, String value, RankType rank,
                                                 LinnaeanRankClassification cl, int max, MatchType type,
                                                 boolean checkHomo, QueryParser parser) throws IOException, SearchResultException {
        String[][] compValues = new String[1][];
        compValues[0] = new String[]{field, value};
        return performSearch(compValues, rank, cl, max, type, checkHomo, parser);
    }

    /**
     * Performs an index search based on the supplied field and name
     *
     * @param compulsoryValues 2D array of field and value mappings to perform the search on
     * @param rank      Optional rank of the value
     * @param cl        The high taxa that form the classification for the search item
     * @param max       The maximum number of results to return
     * @param type      The type of search that is being performed
     * @param checkHomo Whether or not the result should check for homonyms.
     * @param parser
     * @return
     * @throws IOException
     * @throws SearchResultException
     */

    private List<NameSearchResult> performSearch(String[][] compulsoryValues, RankType rank,
                                                 LinnaeanRankClassification cl, int max, MatchType type, boolean checkHomo,
                                                 QueryParser parser) throws IOException, SearchResultException {
        if (cbSearcher != null) {
            String scientificName = null;
            StringBuilder query = new StringBuilder();
            for (String[] values : compulsoryValues) {
                if (values[1] != null) {

                    query.append("+" + values[0] + ":\"" + values[1] + "\"");

                    if (values[0].equals(NameIndexField.NAME.toString()))
                        scientificName = values[1];
                }

            }

            if (rank != null) {
                //if the rank is below species include all names that are species level and below in case synonyms have changed ranks.
                query.append("+(");
                if (rank.getId() >= RankType.SPECIES.getId()) {
                    query.append(NameIndexField.RANK_ID.toString()).append(":[7000 TO 9999]");

                } else
                    query.append(NameIndexField.RANK.toString() + ":\"" + rank.getRank() + "\"");
                //cater for the situation where the search term could be a synonym that does not have a rank
                // also ALA added concepts do NOT have ranks.
                query.append(" OR ").append(NameIndexField.iS_SYNONYM.toString()).append(":T OR ").append(NameIndexField.ALA).append(":T)");

            }
            if (cl != null) {
                query.append(cl.getLuceneSearchString(true));

            }

            try {
                Query scoreQuery = parser.parse(query.toString());
                TopDocs hits = cbSearcher.search(scoreQuery, max);//cbSearcher.search(boolQuery, max);

                //now put the hits into the arrayof NameSearchResult
                List<NameSearchResult> results = new java.util.ArrayList<NameSearchResult>();

                for (ScoreDoc sdoc : hits.scoreDocs) {
                    NameSearchResult nsr = new NameSearchResult(cbReader.document(sdoc.doc), type);
                    nsr.computeMatch(cl);
                    results.add(nsr);
                }
                results.sort(Comparator.comparing(NameSearchResult::getMatchMetrics).reversed());
                if (results.stream().filter(r -> r.getMatchMetrics().getMatch() > MATCH_LIMIT).count() > 0) {
                    results = results.stream().filter(r -> r.getMatchMetrics().getMatch() > MATCH_LIMIT).collect(Collectors.toList());
                }
                //HOMONYM CHECKS and other checks
                if (checkHomo) {

                    //check to see if one of the results is excluded
                    if (results.size() > 0) {
                        int exclCount = 0;
                        NameSearchResult notExcludedResult = null;
                        NameSearchResult excludedResult = null;
                        for (NameSearchResult nsr : results) {
                            if (nsr.getSynonymType() == au.org.ala.names.model.SynonymType.EXCLUDES) {
                                exclCount++;
                                excludedResult = nsr;
                            } else if (notExcludedResult == null) {
                                notExcludedResult = nsr;
                            }
                        }
                        if (exclCount > 0) {
                            //throw the basic exception if count == result size
                            if (exclCount == results.size()) {
                                throw new ExcludedNameException("The result is a name that has been excluded from the NSL", excludedResult);
                            } else if (notExcludedResult != null) {
                                //one of the results was an excluded concept
                                throw new ExcludedNameException("One of the results was excluded.  Use the nonExcludedName for your match.", notExcludedResult, excludedResult);
                            }
                        }
                    }

                    //check to see if we have a situtation where a species has been split into subspecies and a synonym exists to the subspecies
                    checkForSpeciesSplit(results);

                    //check to see if one of the results is a misapplied synonym
                    checkForMisapplied(results);


                    //check result level homonyms
                    //TODO 2012-04-17: Work out edge case issues for canonical matches...
                    //checkResultLevelHomonym(results);

                    //check to see if we have a cross rank homonym
                    //cross rank homonyms are resolvable if a rank has been supplied
                    if (rank == null) {
                        checkForCrossRankHomonym(results);
                    }

                    //check to see if the search criteria could represent an unresolved genus or species homonym
                    if (results.size() > 0) {
                        RankType resRank = results.get(0).getRank();
                        if ((resRank == RankType.GENUS || resRank == RankType.SPECIES) || (results.get(0).isSynonym() && (rank == null || rank == RankType.GENUS || rank == RankType.SPECIES))) {
                            NameSearchResult result = (cl != null && StringUtils.isNotBlank(cl.getAuthorship())) ? validateHomonymByAuthor(results, scientificName, cl) : validateHomonyms(results, scientificName, cl);
                            results.clear();
                            results.add(result);
                        }
                    }
                }

                return results;
            } catch (ParseException e) {
                throw new SearchResultException("Error parsing " + query.toString() + "." + e.getMessage());
            }

        }
        return null;
    }

    private void checkResultLevelHomonym(List<NameSearchResult> results) throws HomonymException {
        //They are result level homonyms if multiple records and they don't all point to the same accepted concept...
        //They are not homonyms if they have different Kingdoms...
        if (results.size() > 1) {
            String lastAcceptedLsid = "";
            String lastKingdom = "";
            boolean lastWasSyn = false;
            for (NameSearchResult result : results) {
                if (result.isSynonym() || result.getRank().getId() >= 7000) {
                    String accepted = result.isSynonym() ? result.getAcceptedLsid() : result.getLsid();
                    String kingdom = result.getRankClassification().getKingdom() == null ? "" : result.getRankClassification().getKingdom();
                    if (lastAcceptedLsid.length() > 0) {
                        if (!lastAcceptedLsid.equals(accepted) && (lastKingdom.equals(kingdom) || lastWasSyn || result.isSynonym())) {
                            throw new HomonymException(accepted, results);
                        }
                    }
                    lastAcceptedLsid = accepted;
                    lastWasSyn = result.isSynonym();
                }
            }
        }
    }

    private void checkForMisapplied(List<NameSearchResult> results) throws MisappliedException {
        if (results.size() >= 1 && results.stream().anyMatch(r -> r.getSynonymType() == SynonymType.MISAPPLIED)) {
            List<NameSearchResult> accepted = results.stream().filter(r -> !r.isSynonym() || (r.isSynonym() && r.getSynonymType() != SynonymType.MISAPPLIED && r.getSynonymType() != SynonymType.EXCLUDES)).collect(Collectors.toList());
            List<NameSearchResult> misapplied = results.stream().filter(r -> r.getSynonymType() == SynonymType.MISAPPLIED).collect(Collectors.toList());
            Set<String> misAccepted = misapplied.stream().map(NameSearchResult::getAcceptedLsid).collect(Collectors.toSet());
            NameSearchResult matched = searchForRecordByLsid(misapplied.get(0).getAcceptedLsid());
            // There ia an accepted or usuable synonym version, as well, use it
            if (!accepted.isEmpty()) {
                throw new MisappliedException(accepted.get(0), matched);
            }
            // All misapplied versions resolve to the same value
            if (misAccepted.size() == 1) {
                throw new MisappliedException(matched);
            }
            // Misapplications resolve to different values, so we can't use this
            throw new MisappliedException(null);
        }
    }

    private void checkForSpeciesSplit(List<NameSearchResult> results) throws ParentSynonymChildException {
        //very specific situtation - there will be 2 results one accepted and the other a synonym to a child of the accepted name
        if (results.size() == 2) {
            if (results.get(0).isSynonym() != results.get(1).isSynonym() && ((!results.get(0).isSynonym() && results.get(0).getRank() == RankType.SPECIES) || (!results.get(1).isSynonym() && results.get(1).getRank() == RankType.SPECIES))) {
                NameSearchResult synResult = results.get(0).isSynonym() ? results.get(0) : results.get(1);
                NameSearchResult accResult = results.get(0).isSynonym() ? results.get(1) : results.get(0);
                NameSearchResult accSynResult = searchForRecordByLsid(synResult.getAcceptedLsid());
                if (accSynResult != null && accResult.getLeft() != null && accSynResult.getLeft() != null) {
                    int asyLeft = Integer.parseInt(accSynResult.getLeft());
                    if (asyLeft > Integer.parseInt(accResult.getLeft()) && asyLeft < Integer.parseInt(accResult.getRight()))
                        throw new ParentSynonymChildException(accResult, accSynResult);
                }
            }
        } else if (results.size() > 2) {
            //check to see if the all other results as synonyms of the same concept AND that concept is a child to the acc concept
            NameSearchResult accResult = null;
            String acceptedLsid = null;
            for (NameSearchResult nsr : results) {
                if (!nsr.isSynonym()) {
                    if (accResult == null)
                        accResult = nsr;
                    else
                        return;
                } else {
                    if (acceptedLsid != null) {
                        if (!acceptedLsid.equals(nsr.getAcceptedLsid()))
                            return;
                    } else {
                        acceptedLsid = nsr.getAcceptedLsid();
                    }
                }
            }
            //now check to see if the accepted concept is a child of the accResult
            if (accResult != null && acceptedLsid != null) {
                NameSearchResult accSynResult = searchForRecordByLsid(acceptedLsid);
                if (accResult != null && accResult.getLeft() != null && accSynResult.getLeft() != null) {
                    int asyLeft = Integer.parseInt(accSynResult.getLeft());
                    if (asyLeft > Integer.parseInt(accResult.getLeft()) && asyLeft < Integer.parseInt(accResult.getRight()))
                        throw new ParentSynonymChildException(accResult, accSynResult);
                }
            }
        }

    }

    /**
     * Checks to see if the first result represents a scientific name that is a cross
     * rank homonym.
     * <p/>
     * This method should only be called if a rank has not been supplied
     *
     * @param results
     * @throws HomonymException When the first result's scientific name is a cross rank homonym
     */
    private void checkForCrossRankHomonym(List<NameSearchResult> results) throws HomonymException {
        if (results != null && results.size() > 0) {
            if (crossRankHomonyms.contains(results.get(0).getRankClassification().getScientificName().toLowerCase()))
                throw new HomonymException("Cross rank homonym detected.  Please repeat search with a rank specified.", results);
        }
    }


    public NameSearchResult validateHomonymByAuthor(List<NameSearchResult> result, String name, LinnaeanRankClassification cl) throws HomonymException {
        //based on the facte that the author is included in the search the first result should be the most complete
        String suppliedAuthor = prepareAuthor(cl.getAuthorship());
        String resultAuthor = result.get(0).getRankClassification().getAuthorship();
        uk.ac.shef.wit.simmetrics.similaritymetrics.SmithWatermanGotoh similarity = new uk.ac.shef.wit.simmetrics.similaritymetrics.SmithWatermanGotoh();
        if (resultAuthor == null || similarity.getSimilarity(suppliedAuthor, resultAuthor) < 0.8) {
            //test based on the irmng list of homoymns
            validateHomonyms(result, name, cl);
        }
        return result.get(0);
    }

    private String prepareAuthor(String author) {
        return author.replaceAll("\\p{P}", "").replaceAll("\\p{Z}", "");
    }


    /**
     * Takes a result set that contains a homonym and then either throws a HomonymException
     * or returns the first result that matches the supplied taxa.
     * <p/>
     * AS OF 22/07/2010:
     * Homonyms are ONLY being tested if the result was a genus. According to Tony it is
     * very rare for a species to be a homonym with another species that belongs to a homonym
     * of the same genus.  Eventually we should get a list of the known cases to
     * test against.
     * <p/>
     * This should provide overall better name matching.
     * <p/>
     * 2011-01-14:
     * The homonym validation has been modified to include species level homonyms.
     * The indexing of the irmng species is different to the genus. IRMNG has a
     * more complete genus coverage than species. Thus only the species that are
     * homonyms are included in the index.
     *
     * @param results The results to on which to validate the homonyms
     * @param name The scientific name for the search
     * @param cl The high taxa that form the classification for the search item
     * @return
     * @throws HomonymException
     */
    public NameSearchResult validateHomonyms(List<NameSearchResult> results, String name, LinnaeanRankClassification cl) throws HomonymException {
        //get the rank so that we know which type of homonym we are evaluating
        RankType rank = results.get(0).getRank();

        //check to see if the homonym is resolvable given the details provide
        try {

            if (rank == null && results.get(0).isSynonym()) {
                cl = new LinnaeanRankClassification(null, null);
                String synName = results.get(0).getRankClassification().getScientificName();
                try{
                ParsedName pn = parser.parse(synName);
                if (pn.isBinomial()) {
                    cl.setSpecies(pn.canonicalName());
                    rank = RankType.SPECIES;
                } else {
                    cl.setGenus(pn.getGenusOrAbove());
                    rank = RankType.GENUS;
                }
                } catch(Exception e){
                    //don't do anything
                }
            }

            if (cl == null) {
                if (rank == RankType.GENUS)
                    cl = new LinnaeanRankClassification(null, name);
                else if (rank == RankType.SPECIES) {
                    cl = new LinnaeanRankClassification(null, null);
                    cl.setSpecies(name);
                }

            }
            if (rank == RankType.GENUS && cl.getGenus() == null)
                cl.setGenus(name);
            else if (rank == RankType.SPECIES && cl.getSpecies() == null)
                cl.setSpecies(name);

            //Find out which rank the homonym can be resolved at.
            //This will indeicate which ranks of the supplied classifications need to match the result's classification in order to resolve the homonym
            RankType resolveLevel = resolveIRMNGHomonym(cl, rank);
            if (resolveLevel == null) {
                //there was no need to resolve the homonym
                return results.get(0);
            }
            //result must match at the kingdom level and resolveLevel of the taxonomy (TODO)
            log.debug("resolve the homonym at " + resolveLevel + " rank");


            //the first result should be the one that most closely resembles the required classification

            for (NameSearchResult result : results) {
                if (result.isSynonym()) {
                    //if the result is a synonym it is difficult to resolve the homonym.
                    //This is because synonyms do not have the corresponding classificaitons.
                    //There are 2 situations that we *may* be able to resolve the homonym
                    // 1) The IRMNG entry that resolves the homonym includes an "accepted" concepts
                    // 2) The resolveLevel is Kingdom and we make an assumption that the concept has not changed kingdoms
                    //    -- This is not always true especially with plants/algae/fungi and animalia/protozoa
                    //TODO algorithm to handle this situations see above comment

                } else {
                    if (cl.hasIdenticalClassification(result.getRankClassification(), resolveLevel))
                        return result;
                }
            }

            throw new HomonymException(results);
        } catch (HomonymException e) {
            e.setResults(results);
            throw e;
        }

    }

    /**
     * Uses the IRMNG index to determine whether or not a homonym can be resolved
     * with the supplied details.
     *
     * @return
     */
    private boolean isHomonymResolvable(LinnaeanRankClassification cl) {
        TopDocs results = getIRMNGGenus(cl, RankType.GENUS);
        if (results != null)
            return results.totalHits.value <= 1;
        return false;
    }

    /**
     * Multiple genus indicate that an unresolved homonym exists for the supplied
     * search details.
     *
     * @param cl   The classification to test
     * @param rank The rank level of the homonym being tested either RankType.GENUS or RankType.SPECIES
     */
    public TopDocs getIRMNGGenus(LinnaeanRankClassification cl, RankType rank) {
        if (cl != null && (cl.getGenus() != null || cl.getSpecies() != null)) {

            try {

                String searchString = "+rank:" + rank + " " + cl.getLuceneSearchString(false).trim();


                log.debug("Search string : " + searchString + " classification : " + cl);
                Query query = queryParser.get().parse(searchString);
                log.debug("getIRMNG query: " + query.toString());
                return irmngSearcher.search(query, 10);

            } catch (Exception e) {
                log.warn("Error searching IRMNG index.", e);
            }
        }
        return null;
    }

    /**
     * Attempt to resolve the homonym using the IRMNG index.
     * <p/>
     * The ability to resolve the homonym is dependent on the quality and quantity
     * of the higher taxa provided in the search via cl.
     *
     * @param cl The classification used to determine the rank at which the homonym is resolvable
     * @return
     * @throws HomonymException
     */
    public RankType resolveIRMNGHomonym(LinnaeanRankClassification cl, RankType rank) throws HomonymException {
        //check to see if we need to resolve the homonym
        if (cl.getGenus() != null || cl.getSpecies() != null) {
            LinnaeanRankClassification newcl = new LinnaeanRankClassification(null, cl.getGenus());
            if (rank == RankType.SPECIES)
                newcl.setSpecies(cl.getSpecies());
            if (cl != null && (cl.getGenus() != null || cl.getSpecies() != null)) {
                TopDocs results = getIRMNGGenus(newcl, rank);
                if (results == null || results.totalHits.value <= 1)
                    return null;

                if (cl != null && cl.getKingdom() != null) {
                    //create a local classification to work with we will only add a taxon when we are ready to try and resolve with it
                    newcl.setKingdom(cl.getKingdom());
                    //Step 1 search for kingdom and genus
                    results = getIRMNGGenus(newcl, rank);
                    if (results.totalHits.value == 1)
                        return RankType.KINGDOM;
                }
                //Step 2 add the phylum
                if (cl.getPhylum() != null && results.totalHits.value > 1) {
                    newcl.setPhylum(cl.getPhylum());
                    results = getIRMNGGenus(newcl, rank);
                    if (results.totalHits.value == 1)
                        return RankType.PHYLUM;
                        //This may not be a good idea
                    else if (results.totalHits.value == 0)
                        newcl.setPhylum(null);//just in case the phylum was specified incorrectly
                }
                //Step 3 try the class
                if (cl.getKlass() != null) {// && results.totalHits>1){
                    newcl.setKlass(cl.getKlass());
                    results = getIRMNGGenus(newcl, rank);
                    if (results.totalHits.value == 1)
                        return RankType.CLASS;

                }
                //step 4 try order
                if (cl.getOrder() != null && results.totalHits.value > 1) {
                    newcl.setOrder(cl.getOrder());
                    results = getIRMNGGenus(newcl, rank);
                    if (results.totalHits.value == 1)
                        return RankType.ORDER;
                }
                //step 5 try  the family
                if (cl.getFamily() != null && results.totalHits.value > 1) {
                    newcl.setFamily(cl.getFamily());
                    results = getIRMNGGenus(newcl, rank);
                    if (results.totalHits.value == 1)
                        return RankType.FAMILY;
                }
            }
        }
        throw new HomonymException("Problem resolving the classification: " + cl);
    }

    /**
     * Performs a search on the common name index for the supplied name.
     *
     * @param commonName
     * @return
     */
    public String searchForLSIDCommonName(String commonName) {
        return getLSIDForUniqueCommonName(commonName);
    }

    /**
     * Retrieve a single common name for this LSID.
     * @param lsid
     * @return
     */
    public String getCommonNameForLSID(String lsid) {
        if (lsid != null) {
            TermQuery query = new TermQuery(new Term(ALANameIndexer.IndexField.LSID.toString(), lsid));
            try {
                TopDocs results = vernSearcher.search(query, 1);
                log.debug("Number of matches for " + lsid + " " + results.totalHits);
                for (ScoreDoc sdoc : results.scoreDocs) {
                    org.apache.lucene.document.Document doc = vernSearcher.doc(sdoc.doc);
                    return doc.get(ALANameIndexer.IndexField.COMMON_NAME.toString());
                }
            } catch (IOException e) {
                log.debug("Unable to access document for common name.", e);
            }
        }
        return null;
    }

    /**
     * Retrieve a single common name for this LSID.
     * @param lsid
     * @param languages to select
     * @return a single common name
     */
    public String getCommonNameForLSID(String lsid, String[] languages) {
        if (lsid != null) {
            for (String language: languages) {
                try {
                    Query query = queryParser.get().parse(
                    ALANameIndexer.IndexField.LSID.toString() + ":\"" + lsid + "\" " +
                            " AND " +
                            ALANameIndexer.IndexField.LANGUAGE.toString() + ":\"" + language + "\" "
                    );
                    TopDocs results = vernSearcher.search(query, 1);
                    log.debug("Number of matches for " + lsid + " " + results.totalHits);
                    for (ScoreDoc sdoc : results.scoreDocs) {
                        org.apache.lucene.document.Document doc = vernSearcher.doc(sdoc.doc);
                        return doc.get(ALANameIndexer.IndexField.COMMON_NAME.toString());
                    }
                } catch (Exception e) {
                    log.debug("Unable to access document for common name.", e);
                }
            }
        }
        return null;
    }

    /**
     * Retrieve a single common name for this LSID.
     * @param lsid
     * @return
     */
    public Set<String> getCommonNamesForLSID(String lsid, int maxNumberOfNames) {
        if (lsid != null) {
            TermQuery query = new TermQuery(new Term(ALANameIndexer.IndexField.LSID.toString(), lsid));
            try {
                TopDocs results = vernSearcher.search(query, maxNumberOfNames);
                //if all the results have the same scientific name result the LSID for the first
                log.debug("Number of matches for " + lsid + " " + results.totalHits);
                Set<String> names = new HashSet<String>();
                Set<String> lowerCaseResults = new HashSet<String>();

                int idx = 0;
                for (ScoreDoc sdoc : results.scoreDocs) {
                    org.apache.lucene.document.Document doc = vernSearcher.doc(sdoc.doc);
                    String name = doc.get(ALANameIndexer.IndexField.COMMON_NAME.toString());
                    if(!lowerCaseResults.contains(name.toLowerCase())){
                        lowerCaseResults.add(name.toLowerCase());
                        names.add(name);
                    }

                    idx++;
                }
                return names;
            } catch (IOException e) {
                log.debug("Unable to access document for common name.", e);
            }
        }
        return new HashSet<String>();
    }

    /**
     * Returns the LSID for the CB name usage for the supplied common name.
     * <p/>
     * When the common name returns more than 1 hit a result is only returned if all the scientific names match
     *
     * @param name
     * @return
     */
    private String getLSIDForUniqueCommonName(String name) {
        if (name != null) {
            TermQuery query = new TermQuery(new Term(ALANameIndexer.IndexField.SEARCHABLE_COMMON_NAME.toString(), name.toUpperCase().replaceAll("[^A-Z0-9ÏËÖÜÄÉÈČÁÀÆŒ]", "")));
            try {
                TopDocs results = vernSearcher.search(query, 10);
                //if all the results have the same scientific name result the LSID for the first
                String firstLsid = null;
                String firstName = null;
                log.debug("Number of matches for " + name + " " + results.totalHits);
                for (ScoreDoc sdoc : results.scoreDocs) {
                    org.apache.lucene.document.Document doc = vernSearcher.doc(sdoc.doc);
                    if (firstLsid == null) {
                        firstLsid = doc.get(ALANameIndexer.IndexField.LSID.toString());
                        firstName = doc.get(ALANameIndexer.IndexField.NAME.toString());
                    } else {
                        if (!doSciNamesMatch(firstName, doc.get(ALANameIndexer.IndexField.NAME.toString())))
                            return null;
                    }
                }
                //want to get the primary lsid for the taxon name thus we get the current lsid in the index...
                return getPrimaryLsid(firstLsid);
            } catch (IOException e) {
                //
                log.debug("Unable to access document for common name.", e);
            }
        }
        return null;
    }

    /**
     * Returns true when the parsed names match.
     *
     * @param n1
     * @param n2
     * @return
     */
    private boolean doSciNamesMatch(String n1, String n2) {
        try {
            ParsedName pn1 = parser.parse(n1);
            ParsedName pn2 = parser.parse(n2);
            if (pn1 != null && pn2 != null)
                return pn1.canonicalName().equals(pn2.canonicalName());
            return false;
        } catch (org.gbif.api.exception.UnparsableException e) {
            return false;
        }
    }

    /**
     * Performs a search on the supplied common name returning a NameSearchResult.
     * Useful if you required CB ID's etc.
     *
     * @param name
     * @return
     */
    public NameSearchResult searchForCommonName(String name) {
        NameSearchResult result = null;
        String lsid = getLSIDForUniqueCommonName(name);
        if (lsid != null) {
            //we need to get the CB ID for the supplied LSID
            result = searchForRecordByLsid(lsid);
            if (result != null)
                result.setMatchType(MatchType.VERNACULAR);
        }
        return result;
    }

    /**
     * Returns the primary LSID for the supplied lsid.
     * <p/>
     * This is useful in the situation where multiple LSIDs are associated with
     * a scientific name and there is a reference to the non-primary LSID.
     *
     * @param lsid
     * @return
     */
    public String getPrimaryLsid(String lsid) {
        if (lsid != null) {
            TermQuery tq = new TermQuery(new Term("lsid", lsid));
            try {
                org.apache.lucene.search.TopDocs results = idSearcher.search(tq, 1);
                if (results.totalHits.value > 0)
                    return idSearcher.doc(results.scoreDocs[0].doc).get("reallsid");
            } catch (IOException e) {
            }
        }

        return lsid;
    }

    public NameSearchResult searchForRecordByLsid(String lsid) {
        NameSearchResult result = null;
        try {
            Query query = new TermQuery(new Term(NameIndexField.LSID.toString(), lsid));
            TopDocs hits = this.idSearcher.search(query, 1);
            if (hits.totalHits.value == 0)
                hits = this.cbSearcher.search(query, 1);
            if (hits.totalHits.value > 0)
                return new NameSearchResult(cbSearcher.doc(hits.scoreDocs[0].doc), MatchType.TAXON_ID);
        } catch (Exception ex) {
            log.error("Unable to search for record by LSID " + lsid, ex);
        }
        return result;
    }

    /**
     * from bie ws/guid/batch
     *
     * returned list of guid that is the same length as the input list
     *
     * @param taxaQueries a list of taxa queries
     * @return
     */
    public List<String> getGuidsForTaxa(List<String> taxaQueries) {
        List guids = new ArrayList<String>();
        for (int i = 0; i < taxaQueries.size(); i++) {
            String scientificName = taxaQueries.get(i);
            String lsid = getLsidByNameAndKingdom(scientificName);
            if (lsid != null && lsid.length() > 0) {
                String guid = null;
                try {
                    guid = getExtendedTaxonConceptByGuid(lsid, true, true);
                } catch (Exception e) {
                }
                guids.add(guid);
            }

            if (guids.size() < i + 1) guids.add(null);
        }
        return guids;
    }

    private void appendAutocompleteResults(Map<String, Map> output, TopDocs results, boolean includeSynonyms, boolean commonNameResults) throws IOException {
        ScoreDoc[] scoreDocs = results.scoreDocs;
        int scoreDocsCount = scoreDocs.length;
        for(int excludedResult = 0; excludedResult < scoreDocsCount; ++excludedResult) {
            ScoreDoc i = scoreDocs[excludedResult];
            Document src = commonNameResults ? vernSearcher.doc(i.doc) : cbSearcher.doc(i.doc);
            NameSearchResult nsr = commonNameResults ?
                    searchForRecordByLsid(src.get("lsid"))
                    : new NameSearchResult(src, null);

            if (nsr == null || (nsr.getLeft() == null && !includeSynonyms)) continue;

            Map m = formatAutocompleteNsr(i.score, nsr);

            //use the matched common name
            if (commonNameResults) {
                m.put("commonname", src.get("common_orig"));
                m.put("match", "commonName");
            } else {
                m.put("match", "scientificName");
            }

            while (includeSynonyms && nsr != null && m != null && nsr.getAcceptedLsid() != null) {
                if (output.containsKey(nsr.getAcceptedLsid())) {
                    List list = (List) output.get(nsr.getAcceptedLsid()).get("synonymMatch");
                    if (list == null) list = new ArrayList();
                    list.add(m);
                    output.get(nsr.getAcceptedLsid()).put("synonymMatch", list);
                    m = null;
                    nsr = null;
                } else {
                    nsr = searchForRecordByLsid(nsr.getAcceptedLsid());

                    if (nsr != null) {
                        List list = new ArrayList();
                        list.add(m);

                        m = formatAutocompleteNsr(i.score, nsr);
                        m.put("synonymMatch", list);
                    }
                }
            }

            if (((nsr != null && nsr.getAcceptedLsid() == null) || includeSynonyms) && m != null) {
                if (m.get("name").toString().equals("Acacia")) {
                    int aa = 4;
                }
                Map existing = output.get(m.get("lsid").toString());
                if (existing == null) {
                    output.put(m.get("lsid").toString(), m);
                } else {
                    //use best score
                    if ((Float) m.get("score") > (Float) existing.get("score")) {
                        output.put(m.get("lsid").toString(), m);
                    }
                }
            }
        }
    }

    private Query buildAutocompleteQuery(String field, String q, boolean allSearches) {
        //best match
        Query fq1 = new BoostQuery(new TermQuery(new Term(field,q)), 12f);  //exact match

        //partial matches
        Query fq5 = new WildcardQuery(new Term(field,q + "*")); //begins with that begins with
        Query fq6 = new WildcardQuery(new Term(field,"* " + q + "*")); //contains word that begins with

        //any match
        Query fq7 = new WildcardQuery(new Term(field,"*" + q + "*")); //any match

        //join
        BooleanQuery o = new BooleanQuery.Builder()
                .add(new BooleanClause(fq1, BooleanClause.Occur.SHOULD))
                .add(new BooleanClause(fq5, BooleanClause.Occur.SHOULD))
                .add(new BooleanClause(fq6, BooleanClause.Occur.SHOULD))
                .add(new BooleanClause(fq7, BooleanClause.Occur.SHOULD))
                .build();
        return o;
    }

    private String getPreferredGuid(String taxonConceptGuid) throws Exception {
        Query qGuid = new TermQuery(new Term("guid", taxonConceptGuid));
        Query qOtherGuid = new TermQuery(new Term("otherGuid", taxonConceptGuid));

        BooleanQuery fullQuery = new BooleanQuery.Builder()
                .add(qGuid, BooleanClause.Occur.SHOULD)
                .add(qOtherGuid, BooleanClause.Occur.SHOULD).build();

        TopDocs topDocs = cbSearcher.search(fullQuery, 1);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = cbSearcher.doc(scoreDoc.doc);
            return doc.get("guid");
        }
        return taxonConceptGuid;
    }

    private boolean isKingdom(String name) {
        try {
            LinnaeanRankClassification lc = new LinnaeanRankClassification(name, null);
            NameSearchResult nsr = searchForRecord(lc, false);
            return nsr != null && nsr.getRank() == RankType.KINGDOM;
        } catch (Exception e) {
            return false;
        }
    }

    private String[] extractComponents(String in) {
        String[] retArray = new String[2];
        int lastOpen = in.lastIndexOf("(");
        int lastClose = in.lastIndexOf(")");
        if (lastOpen < lastClose) {
            //check to see if the last brackets are a kingdom
            String potentialKingdom = in.substring(lastOpen + 1, lastClose);
            if (isKingdom(potentialKingdom)) {
                retArray[0] = in.substring(0, lastOpen);
                retArray[1] = potentialKingdom;
            } else {
                retArray[0] = in;
            }
        } else {
            retArray[0] = in;
            //kingdom is null
        }
        return retArray;

    }

    private String getLsidByNameAndKingdom(String parameter) {
        String lsid = null;
        String name = null;
        String kingdom = null;

        String[] parts = extractComponents(parameter);
        name = parts[0];
        name = name.replaceAll("_", " ");
        name = name.replaceAll("\\+", " ");
        kingdom = parts[1];
        if (kingdom != null) {
            LinnaeanRankClassification cl = new LinnaeanRankClassification(kingdom, null);
            cl.setScientificName(name);
            try {
                lsid = searchForLSID(cl.getScientificName(), cl, null);
            } catch (ExcludedNameException e) {
                if (e.getNonExcludedName() != null)
                    lsid = e.getNonExcludedName().getLsid();
                else
                    lsid = e.getExcludedName().getLsid();
            } catch (ParentSynonymChildException e) {
                //the parent is the one we want, since we don't know whether this is just a higher taxon or not
                lsid = e.getParentResult().getLsid();
            } catch (MisappliedException e) {
                if (e.getMatchedResult() != null)
                    lsid = e.getMatchedResult().getLsid();
            } catch (SearchResultException e) {
            }
        }
        //check for a scientific name first - this will lookup in the name matching index.  This will produce the correct result in a majority of scientific name cases.
        if (lsid == null || lsid.length() < 1) {
            try {
                lsid = searchForLSID(name, true, true);
            } catch (ExcludedNameException e) {
                if (e.getNonExcludedName() != null)
                    lsid = e.getNonExcludedName().getLsid();
                else
                    lsid = e.getExcludedName().getLsid();
            } catch (ParentSynonymChildException e) {
                //the parent is the one we want, since we don't know whether this is just a higher taxon or not
                lsid = e.getParentResult().getLsid();
            } catch (MisappliedException e) {
                if (e.getMatchedResult() != null)
                    lsid = e.getMatchedResult().getLsid();
            } catch (SearchResultException e) {
            }
        }

        if (lsid == null || lsid.length() < 1) {
            lsid = searchForLSIDCommonName(name);
        }

        if (lsid == null || lsid.length() < 1) {
            lsid = findLSIDByConcatName(name);
        }

        return lsid;
    }

    private String concatName(String name) {
        String patternA = "[^a-zA-Z]";
        /* replace multiple whitespaces between words with single blank */
        String patternB = "\\b\\s{2,}\\b";

        String cleanQuery = "";
        if (name != null) {
            cleanQuery = escapeQueryChars(name);
            cleanQuery = cleanQuery.toLowerCase();
            cleanQuery = cleanQuery.replaceAll(patternA, "");
            cleanQuery = cleanQuery.replaceAll(patternB, "");
            cleanQuery = cleanQuery.trim();
        }
        return cleanQuery;
    }

    private String findLSIDByConcatName(String name) {
        try {
            String concatName = concatName(name);

            Query query = new TermQuery(new Term("concat_name", concatName));

            TopDocs topDocs = cbSearcher.search(query, 2);
            if (topDocs != null && topDocs.totalHits.value == 1) {
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document doc = cbSearcher.doc(scoreDoc.doc);
                    return doc.get("guid");
                }
            }
        } catch (Exception e) {
            // do nothing
        }
        return null;

    }

    private String getExtendedTaxonConceptByGuid(String guid, boolean checkPreferred, boolean checkSynonym) throws Exception {

        //Because a concept can be accepted and a synonym we need to check if the original guid exists before checking preferred
        NameSearchResult nsr = searchForRecordByLsid(guid);
        boolean hasAccepted = nsr != null && nsr.getAcceptedLsid() == null;

        if (checkPreferred && !hasAccepted) {
            guid = getPreferredGuid(guid);
        }
        if (checkSynonym && !hasAccepted) {
            if (nsr != null && nsr.isSynonym()) {
                guid = nsr.getAcceptedLsid();
            }
        }

        return guid;
    }

    /**
     * Basic autocomplete. All matches are resolved to accepted LSID.
     *
     * @param q
     * @param max
     * @param includeSynonyms
     * @return
     */
    public List<Map> autocomplete(String q, int max, boolean includeSynonyms) {
        try {
            if(false) {
                return null;
            } else {
                Map<String, Map> output = new HashMap<String, Map>();

                //more queries for better scoring values
                String lq = q.toLowerCase();
                String uq = q.toUpperCase();

                //name search
                Query fq = buildAutocompleteQuery("name", lq, false);
                BooleanQuery b = new BooleanQuery.Builder()
                    .add(fq, BooleanClause.Occur.MUST)
                    .add(new WildcardQuery(new Term("left", "*")), includeSynonyms ? BooleanClause.Occur.SHOULD : BooleanClause.Occur.MUST)
                    .build();
                TopDocs results = cbSearcher.search(b, max);
                appendAutocompleteResults(output, results, includeSynonyms, false);

                //format search term for the current common name index
                uq = concatName(uq).toUpperCase();

                //common name search
                fq = buildAutocompleteQuery("common", uq, true);
                results = vernSearcher.search(fq, max);
                appendAutocompleteResults(output, results, includeSynonyms, true);

                return new ArrayList(output.values());
            }
        } catch (Exception e) {
            log.error("Autocomplete error.",e);
        }
        return null;
    }


    private Map formatAutocompleteNsr(float score, NameSearchResult nsr) {
        Map m = new HashMap();
        m.put("score", score);
        m.put("lsid", nsr.getLsid());
        m.put("left", nsr.getLeft());
        m.put("right", nsr.getRight());
        m.put("rank", nsr.getRank());
        m.put("rankId", nsr.getRank() != null ? nsr.getRank().getId() : 10000);
        m.put("cl", nsr.getRankClassification());
        m.put("name", nsr.getRankClassification() != null ? nsr.getRankClassification().getScientificName() : null);
        m.put("acceptedLsid", nsr.getAcceptedLsid());
        m.put("commonname", getCommonNameForLSID(nsr.getLsid()));
        m.put("commonnames", getCommonNamesForLSID(nsr.getLsid(),1000));

        return m;
    }



    private String escapeQueryChars(String s) {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':' || c == '^' || c == '[' || c == ']' || c == '"' || c == '{' || c == '}' || c == '~' || c == '*' || c == '?' || c == '|' || c == '&' || c == ';' || c == '/' || Character.isWhitespace(c)) {
                sb.append('\\');
            }

            sb.append(c);
        }

        return sb.toString();
    }

    public static void main(String[] args) throws IOException {

        ALANameSearcher nameindex = new ALANameSearcher(args[0]);
        String name = nameindex.getCommonNameForLSID("urn:lsid:biodiversity.org.au:afd.taxon:31a9b8b8-4e8f-4343-a15f-2ed24e0bf1ae");
        System.out.println(name);

        Set<String> names = nameindex.getCommonNamesForLSID("urn:lsid:biodiversity.org.au:apni.taxon:295861", 100);
        for(String commonName: names){
            System.out.println(commonName);
        }
    }

}
