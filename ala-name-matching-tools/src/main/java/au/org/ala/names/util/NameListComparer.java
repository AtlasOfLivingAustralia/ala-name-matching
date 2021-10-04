package au.org.ala.names.util;

import com.opencsv.*;
import au.org.ala.names.model.*;
import au.org.ala.names.search.*;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Compare a list of existing names with what the index comes up with an produce a report.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 *         <p/>
 *         Copyright (c) 2016 CSIRO
 */
public class NameListComparer {
    private static Logger log = LoggerFactory.getLogger(NameListComparer.class);

    private static String[][] TERMS = {
            { "originalId", "Species", "taxonConceptID", "taxon_concept_lsid", "taxonID" },
            { "originalScientificName", "Species Name", "scientificName", "taxon_name", "raw_taxon_name" },
            { "originalScientificNameAuthorship", "Scientific Name Authorship", "scientificNameAuthorship" },
            { "originalRank", "Taxon Rank", "taxonRank", "rank", "taxonomicRank" },
            { "originalKingdom", "Kingdom", "kingdom" },
            { "originalPhylum",  "Phylum", "phylum" },
            { "originalClass", "Class", "class", "class_", "_class" },
            { "originalOrder", "Order", "order" },
            { "originalFamily", "Family", "family" },
            { "originalGenus", "Genus", "genus" },
            { "originalVernacular", "Vernacular Name", "raw_common_name","vernacularName", "taxon_common_name" }
    };

    private CSVReader names;
    private CSVWriter output;
    private ALANameSearcher searcher;
    private Map<String, Integer> columnMap;
    private Map<String, Integer> termMap;
    private List<String> additional;

    public NameListComparer(Reader names, Writer output, File index, boolean tabs) throws IOException {
        CSVParser parser = new CSVParserBuilder().withSeparator(tabs ? '\t' : ',').build();
        this.names = new CSVReaderBuilder(names).withCSVParser(parser).build();
        this.output = new CSVWriter(output);
        this.searcher = new ALANameSearcher(index.getAbsolutePath());
    }

    protected String getColumn(String[] row, String column) {
        Integer pos = this.termMap.get(column);

        if (pos == null)
            pos = columnMap.get(column);
        if (pos != null && pos.intValue() < row.length) {
            String value = row[pos.intValue()];

            return StringUtils.isBlank(value) ? null : value;
        }
        return null;
    }

    protected String mapTerm(String column) {
        for (String[] term: TERMS) {
            String original = term[0];
            for (int i = 1; i < term.length; i++)
                if (column.equals(term[i]))
                    return original;
        }
        return null;
    }

    protected void readHeader() throws IOException {
        String[] header = names.readNext();
        int i = 0;

        this.columnMap = new HashMap<String, Integer>();
        this.termMap = new HashMap<String, Integer>();
        this.additional = new ArrayList<>();
        for (String column: header) {
            column = column.trim();
            this.columnMap.put(column, i);
            String original = mapTerm(column);
            if (original != null)
                termMap.put(original, i);
            else
                additional.add(column);
            i++;
        }
    }

    protected void writeHeader() throws IOException {
        // Basic required columns
        List<String> columns = new ArrayList<>();
        columns.addAll(Arrays.asList(
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
        ));
        columns.addAll(additional);
        this.output.writeNext(columns.toArray(new String[columns.size()]));
    }

    public String[] match(String[] row) {
        MetricsResultDTO metrics = null;
        NameSearchResult nsr = null;
        String originalId = this.getColumn(row, "originalId");
        String originalScientificName = this.getColumn(row, "originalScientificName");
        String originalScientificNameAuthorship = this.getColumn(row, "originalScientificNameAuthorship");
        String originalRank = this.getColumn(row, "originalRank");
        String originalKingdom = this.getColumn(row, "originalKingdom");
        String originalPhylum = this.getColumn(row, "originalPhylum");
        String originalClass = this.getColumn(row, "originalClass");
        String originalOrder = this.getColumn(row, "originalOrder");
        String originalFamily = this.getColumn(row, "originalFamily");
        String originalGenus = this.getColumn(row, "originalGenus");
        String originalVernacular = this.getColumn(row, "originalVernacularName");
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
        List<String> values = new ArrayList<>(additional.size() + 30);
        values.addAll(Arrays.asList(
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
        ));
        for (String column: additional)
            values.add(this.getColumn(row, column));
        return values.toArray(new String[values.size()]);
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
            OutputStream os = of == null || of.equals("-") ? System.out : new FileOutputStream(of);
            Writer output = new OutputStreamWriter(os, "UTF-8");
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
