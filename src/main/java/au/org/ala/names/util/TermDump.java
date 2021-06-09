package au.org.ala.names.util;

import org.apache.commons.cli.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Dump the terms in an index.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 *         <p/>
 *         Copyright (c) 2016 CSIRO
 */
public class TermDump {
    /** The index file */
    private File index;
    /** The writer to dump to */
    private Writer output;

    /**
     * Construct a term dumper
     *
     * @param index The directory containing the index
     * @param output The stream to write to
     */
    public TermDump(File index, Writer output) {
        this.index = index;
        this.output = output;
    }

    public void dump() throws IOException {
        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(this.index.toPath()));
        Set<String> fields = new HashSet<>();
        for (LeafReaderContext lc: reader.leaves()) {
            for (FieldInfo fi: lc.reader().getFieldInfos())
                fields.add(fi.name);
        }
        PrintWriter pw = new PrintWriter(this.output);

        for (String field: fields) {
            pw.println(field);
            /*
            Terms terms = fields.terms(field);
            TermsEnum termsEnum = terms.iterator(null);
            BytesRef text;
            while((text = termsEnum.next()) != null) {
                this.output.write(field);
                this.output.write(",");
                this.output.write(text.utf8ToString());
                this.output.write("\n");
            }
            */
        }
        reader.close();
    }

    public static void main(String[] args) {
        Options options = new Options();

        Option o = OptionBuilder.withLongOpt("output").withDescription("Output file name - defaults to standard output").hasArg().withArgName("FILE").create('o');
        Option i = OptionBuilder.withLongOpt("index").withDescription("Lucene index directory - defaults to /data/lucene/namematching/cb").hasArg().withArgName("DIR").create('i');
        options.addOption(o);
        options.addOption(i);
        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            String of = cmd.getOptionValue('o', "-");
            Writer output = of == null || of.equals("-") ? new OutputStreamWriter(System.out) : new FileWriter(of);
            File index = new File(cmd.getOptionValue('i', "/data/lucene/namematching/cb"));
            TermDump dumper = new TermDump(index, output);

            dumper.dump();
            output.close();
        } catch (ParseException ex) {
            System.err.println("Unable to parse command line: " + ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }

    }

}
