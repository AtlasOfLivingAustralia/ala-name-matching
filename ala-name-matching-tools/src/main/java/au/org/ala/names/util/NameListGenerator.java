package au.org.ala.names.util;

import com.opencsv.CSVWriter;
import au.org.ala.names.model.SynonymType;
import org.apache.commons.cli.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Generate a list of names known to the index, suitable for importing or
 * comparing with other indexes.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 *         <p/>
 *         Copyright (c) 2015 CSIRO
 */
public class NameListGenerator implements Closeable {
    private static Logger log = LoggerFactory.getLogger(NameListGenerator.class);

    private static String[][] FIELDS = {
            {"lsid", "taxonID"},
            {"name", "scientificName"},
            {"author", "scientificNameAuthorship"},
            {"rank", "taxonRank"},
            {"accepted_lsid", "acceptedNameUsageId"},
            {"syn_type", "taxonomicStatus" }
    };

    private CSVWriter writer;
    private IndexReader cbReader;
    private Set<String> synonymTypes;

    public NameListGenerator(Writer output, File index) throws IOException {
        this.writer = new CSVWriter(output);
        this.cbReader = DirectoryReader.open(FSDirectory.open(new File(index, "cb").toPath()));
        this.synonymTypes = new HashSet<String>();
    }

    /**
     * Generate the header line for the output CSV file
     */
    public void generateHeader() {
        String[] headers = new String[FIELDS.length];

        for (int i = 0; i < FIELDS.length; i++)
            headers[i] = FIELDS[i][1];
        writer.writeNext(headers);
    }

    public void generate() {
        String[] values = new String[FIELDS.length];

        this.generateHeader();
        for (int i = 0; i < this.cbReader.maxDoc(); i++) {
            try {
                Document doc = this.cbReader.document(i);

                for (int j = 0; j < FIELDS.length; j++) {
                    String value = doc.get(FIELDS[j][0]);

                    if (value != null && FIELDS[j][0].equals("syn_type")) {
                        this.synonymTypes.add(value);
                        SynonymType type = SynonymType.getTypeFor(value);
                        if (type != null)
                            value = type.name();
                     }
                    values[j] = value;
                }
                writer.writeNext(values);
            } catch (IOException ex) {
                log.warn("Unable to get document " + i + ": " + ex.getMessage());
            }
        }
    }

    @Override
    public void close() throws IOException {
        try {
            writer.close();
        } finally {
            this.cbReader.close();
        }
    }

    public void dumpSynonyms(PrintStream out) {
        List<String> synonyms = new ArrayList<String>(this.synonymTypes);

        Collections.sort(synonyms);
        out.println("Synonym Types");
        out.println("-------------");
        for (String s: synonyms) {
            out.print(s);
            out.print(" = ");
            out.println(SynonymType.getTypeFor(s));
        }
    }

    public static void main(String[] args) {
        Options options = new Options();

        Option o = OptionBuilder.withLongOpt("output").withDescription("Output file name - defaults to standard output").hasArg().withArgName("FILE").create('o');
        Option i = OptionBuilder.withLongOpt("index").withDescription("Lucene index directory - defaults to /data/lucene/namematching").hasArg().withArgName("DIR").create('i');
        options.addOption(o);
        options.addOption(i);
        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            String of = cmd.getOptionValue('o', "-");
            Writer output = of == null || of.equals("-") ? new OutputStreamWriter(System.out) : new FileWriter(of);
            File index = new File(cmd.getOptionValue('i', "/data/lucene/namematching"));
            try (NameListGenerator generator = new NameListGenerator(output, index);) {
                generator.generate();
                generator.dumpSynonyms(System.out);
            }
        } catch (ParseException ex) {
            System.err.println("Unable to parse command line: " + ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }

    }
}
