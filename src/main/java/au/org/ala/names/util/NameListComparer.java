package au.org.ala.names.util;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import au.org.ala.names.model.*;
import au.org.ala.names.search.*;
import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Compare a list of existing names with what the index comes up with an produce a report.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 *         <p/>
 *         Copyright (c) 2016 CSIRO
 */
public class NameListComparer {
    private static Log log = LogFactory.getLog(NameListComparer.class);

    private CSVReader names;
    private CSVWriter output;
    private ALANameSearcher searcher;
    private Map<String, Integer> columnMap;

    public NameListComparer(Reader names, Writer output, File index, boolean tabs) throws IOException {
        this.names = new CSVReader(names, tabs ? '\t' : ',');
        this.output = new CSVWriter(output);
        this.searcher = new ALANameSearcher(index.getAbsolutePath());
    }

    protected String getColumn(String[] row, String... columns) {
        for (String column: columns) {
            Integer pos = this.columnMap.get(column);
            if (pos != null && pos.intValue() < row.length) {
                String value = row[pos.intValue()];

                return StringUtils.isBlank(value) ? null : value;
            }
        }
        return null;
    }

    protected void readHeader() throws IOException {
        String[] header = names.readNext();
        int i = 0;

        this.columnMap = new HashMap<String, Integer>();
        for (String column: header) {
            this.columnMap.put(column, i);
            i++;
        }
    }

    protected void writeHeader() throws IOException {
        this.output.writeNext(new String[] {
                "originalId",
                "id",
                "acceptedId",
                "originalScientificName",
                "scientificName",
                "originalScientificNameAuthorship",
                "scientificNameAuthorship",
                "matchType",
                "originalRank",
                "rank",
                "originalKingdom",
                "kingdom",
                "originalPhylum",
                "phylum",
                "originalClass",
                "class",
                "originalOrder",
                "order",
                "originalFamily",
                "family",
                "originalGenus",
                "genus",
                "species",
                "originalVernacular",
                "errors"
        });

    }

    public String[] match(String[] row) {
        MetricsResultDTO metrics = null;
        NameSearchResult nsr = null;
        String originalId = this.getColumn(row, "Species", "taxonConceptID", "taxon_concept_lsid", "taxonID");
        String originalScientificName = this.getColumn(row, "Species Name", "scientificName", "raw_taxon_name");
        String originalScientificNameAuthorship = this.getColumn(row, "Scientific Name Authorship", "scientificNameAuthorship");
        String originalRank = this.getColumn(row, "Taxon Rank", "rank", "taxonomicRank");
        String originalKingdom = this.getColumn(row, "Kingdom", "kingdom");
        String originalPhylum = this.getColumn(row, "Phylum", "phylum");
        String originalClass = this.getColumn(row, "Class", "class", "class_");
        String originalOrder = this.getColumn(row, "Order", "order");
        String originalFamily = this.getColumn(row, "Family", "family");
        String originalGenus = this.getColumn(row, "Genus", "genus");
        String originalVernacular = this.getColumn(row, "Vernacular Name", "raw_common_name", "vernacularName");
        String id = null;
        String acceptedId = null;
        String matchType = null;
        String scientificName = null;
        String scientificNameAuthorship = null;
        String rank = null;
        String kingdom = null;
        String phylum = null;
        String klass = null;
        String order = null;
        String family = null;
        String genus = null;
        String species = null;
        String errors = "";
        if (originalScientificName == null && originalVernacular == null)
            return null;
        try {
            if (originalScientificName != null && !originalScientificName.isEmpty()) {
                LinnaeanRankClassification cl = new LinnaeanRankClassification();
                cl.setFamily(originalFamily);
                cl.setOrder(originalOrder);
                cl.setKlass(originalClass);
                cl.setPhylum(originalPhylum);
                cl.setKingdom(originalKingdom);
                cl.setRank(originalRank);
                cl.setRank(originalRank);
                cl.setScientificName(originalScientificName);
                cl.setAuthorship(originalScientificNameAuthorship);
                metrics = this.searcher.searchForRecordMetrics(cl, true, true);
                for (ErrorType err: metrics.getErrors())
                    errors = errors + " " + err;
                nsr = metrics.getResult();
             }
            if (metrics == null && originalVernacular != null) {
                nsr = this.searcher.searchForCommonName(originalVernacular);
                errors = errors + " vernacular";
            }
            if (nsr != null) {
                id = nsr.getId();
                acceptedId = nsr.getAcceptedLsid();
                matchType = nsr.getMatchType().toString();
                scientificName = nsr.getRankClassification().getScientificName();
                scientificNameAuthorship = nsr.getRankClassification().getAuthorship();
                rank = nsr.getRank() != null ? nsr.getRank().getRank() : null;
                kingdom = nsr.getRankClassification().getKingdom();
                phylum = nsr.getRankClassification().getPhylum();
                klass = nsr.getRankClassification().getKlass();
                order = nsr.getRankClassification().getOrder();
                family = nsr.getRankClassification().getFamily();
                genus = nsr.getRankClassification().getGenus();
                species = nsr.getRankClassification().getSpecies();
            }
        } catch (SearchResultException ex) {
            log.error("Unexpected exception " + ex);
        } catch (Exception ex) {
            errors = errors + " exception:" + ex.getClass();
            log.error("Really bad exception " + ex);
        }
        return new String[] {
                originalId,
                id,
                acceptedId,
                originalScientificName,
                scientificName,
                originalScientificNameAuthorship,
                scientificNameAuthorship,
                matchType,
                originalRank,
                rank,
                originalKingdom,
                kingdom,
                originalPhylum,
                phylum,
                originalClass,
                klass,
                originalOrder,
                order,
                originalFamily,
                family,
                originalGenus,
                genus,
                species,
                originalVernacular,
                errors.trim()
        };
    }

    public void compare() throws IOException {
        String[] row, match;
        int count = 0;

        this.readHeader();
        this.writeHeader();
        while ((row = this.names.readNext()) != null) {
            match = this.match(row);
            if (match != null)
                this.output.writeNext(match);
            if (++count % 1000 == 0)
                log.info("Processed " + count + " names");
        }
    }

    public void close() throws IOException {
        this.names.close();
        this.output.close();
    }

    public static void main(String[] args) {
        Options options = new Options();

        Option n = OptionBuilder.withLongOpt("names").withDescription("Name list to compare").hasArg().withArgName("FILE").create('n');
        Option o = OptionBuilder.withLongOpt("output").withDescription("Output file name - defaults to standard output").hasArg().withArgName("FILE").create('o');
        Option i = OptionBuilder.withLongOpt("index").withDescription("Lucene index directory - defaults to /data/lucene/namematching").hasArg().withArgName("DIR").create('i');
        Option t = OptionBuilder.withLongOpt("tabs").withDescription("Use tab-separated, rather than comma separated values").create();
        options.addOption(n);
        options.addOption(o);
        options.addOption(i);
        options.addOption(t);
        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            File nf = new File(cmd.getArgs()[0]);
            if (!nf.exists())
                throw new IllegalArgumentException("Can't find input file " + nf);
            Reader names = new InputStreamReader(new FileInputStream(nf), "UTF-8");
            String of = cmd.getOptionValue('o', "-");
            Writer output = of == null || of.equals("-") ? new OutputStreamWriter(System.out) : new FileWriter(of);
            File index = new File(cmd.getOptionValue('i', "/data/lucene/namematching"));
            boolean tabs = cmd.hasOption("tabs");
            NameListComparer comparer = new NameListComparer(names, output, index, tabs);

            comparer.compare();
            comparer.close();
        } catch (ParseException ex) {
            System.err.println("Unable to parse command line: " + ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }

    }

}
