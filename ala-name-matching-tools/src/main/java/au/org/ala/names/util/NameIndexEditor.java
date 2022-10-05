/*
 * Copyright (c) 2022 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 */

package au.org.ala.names.util;

import au.org.ala.names.lucene.analyzer.LowerCaseKeywordAnalyzer;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.commons.cli.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * Edit an existing name index without disturbing the delicate balance of left and right values.
 * <p>
 * This class basically takes a set of taxon IDs and performs edit actions on them,
 * then optimses the resulting index.
 * </p>
 */
public class NameIndexEditor implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(NameIndexEditor.class);

    private static final String TAXONOMY_INDEX = "cb";
    private static final String VERNACULAR_INDEX = "vernacular";
    private static final String IDENTIFIER_INDEX = "id";
    private static final String IRMNG_INDEX = "irmng";

    /** The directory that contains the original sub-indexes */
    private File sourceDirectory;
    /** The basic work directory */
    private File workDirectory;
    /** The taxonomy work index */
    private IndexWriter taxonomy;
    /** The veractula work index */
    private IndexWriter vernaculer;
    /** The identifier work index */
    private IndexWriter identifier;
    /** The analyser to use */
    private Analyzer analyzer;

    public NameIndexEditor(File sourceDirectory, File workDirectory) throws IOException {
        this.sourceDirectory = sourceDirectory;
        this.workDirectory = workDirectory;
        this.analyzer = LowerCaseKeywordAnalyzer.newInstance();
        this.taxonomy = this.copyIndex(this.sourceDirectory, this.workDirectory, TAXONOMY_INDEX);
        this.vernaculer = this.copyIndex(this.sourceDirectory, this.workDirectory, VERNACULAR_INDEX);
        this.identifier = this.copyIndex(this.sourceDirectory, this.workDirectory, IDENTIFIER_INDEX);
    }

    public void makeEdits(File edits, boolean tabs) throws IOException, CsvValidationException {
        CSVParser parser = new CSVParserBuilder().withSeparator(tabs ? '\t' : ',').build();
        CSVReader reader = new CSVReaderBuilder(new FileReader(edits)).withCSVParser(parser).build();
        String[] row;
        while ((row = reader.readNext()) != null) {
            if (row[0].startsWith("#"))
                continue;
            String lsid = row[0];
            String action = row[1];
            if (action.equals("delete")) {
                this.deleteTaxon(lsid);
            } else {
                throw new IllegalArgumentException("Unknown operation " + action);
            }
        }
        reader.close();
    }

    protected void deleteTaxon(String lsid) throws IOException {
        logger.info("Deleting " + lsid);
        Term term = new Term("lsid", lsid);
        this.taxonomy.deleteDocuments(term);
        this.vernaculer.deleteDocuments(term);
        this.identifier.deleteDocuments(term);
    }

    protected IndexWriter copyIndex(File source, File dest, String index) throws IOException {
        Directory d = FSDirectory.open(new File(dest, index).toPath());
        IndexWriterConfig conf = new IndexWriterConfig(this.analyzer);
        conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        logger.info("Creating destination " + index + " directory " + " at " + d);
        IndexWriter writer = new IndexWriter(d, conf);
        Directory s = FSDirectory.open(new File(source, index).toPath());
        logger.info("Adding source " + index + " directory " + " from " + s);
        writer.addIndexes(s);
        return writer;
    }

    protected void copyAndCloseIndex(File source, File dest, String index) throws IOException {
        IndexWriter writer = this.copyIndex(source, dest, index);
        writer.commit();
        writer.close();
    }

    protected void copySupportFiles(File source, File dest) throws IOException {
        List<String> names = Arrays.asList("metadata.json", "idmap.txt");
        for (String name: names) {
            File from = new File(source, name);
            File to = new File(dest, name);
            org.apache.commons.io.FileUtils.copyFile(from, to, false);
        }
    }

    @Override
    public void close() throws Exception {
        this.taxonomy.commit();
        this.taxonomy.close();
        this.report(this.workDirectory, TAXONOMY_INDEX);
        this.vernaculer.commit();
        this.vernaculer.close();
        this.report(this.workDirectory, VERNACULAR_INDEX);
        this.identifier.close();
        this.identifier.close();
        this.report(this.workDirectory, IDENTIFIER_INDEX);
    }

    public void create(File output) throws IOException {
        this.copyAndCloseIndex(this.workDirectory, output, TAXONOMY_INDEX);
        this.report(output, TAXONOMY_INDEX);
        this.copyAndCloseIndex(this.workDirectory, output, VERNACULAR_INDEX);
        this.report(output, VERNACULAR_INDEX);
        this.copyAndCloseIndex(this.workDirectory, output, IDENTIFIER_INDEX);
        this.report(output, IDENTIFIER_INDEX);
        this.copyAndCloseIndex(this.sourceDirectory, output, IRMNG_INDEX);
        this.copySupportFiles(this.sourceDirectory, output);
    }

    public void report(File dir, String index) throws IOException {
        Directory d = FSDirectory.open(new File(dir, index).toPath());
        IndexReader reader = DirectoryReader.open(d);
        logger.info("Status of " + d);
        logger.info("  Documents " + reader.numDocs());
        logger.info("  Has deletions " + reader.hasDeletions());
        logger.info("  Deleted documents " + reader.numDeletedDocs());
        reader.close();
    }

    public static void main(String[] args) {
        Options options = new Options();

        Option v = OptionBuilder.withLongOpt("verbose").withDescription("Be verbose when logging").create('v');
        Option t = OptionBuilder.withLongOpt("tabs").withDescription("Use tab-separated, rather than comma separated values").create('t');
        options.addOption(v);
        options.addOption(t);
        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.getArgs().length < 2) {
                throw new IllegalArgumentException("usage: [-v] [-t] source dest edits ...");
            }
            boolean tabs = cmd.hasOption('t');
            boolean verbose = cmd.hasOption('v');
            File source = new File(cmd.getArgs()[0]);
            if (!source.exists() || !source.isDirectory())
                throw new IllegalArgumentException("Expecting directory for index, got  " + source);
            File output = new File(cmd.getArgs()[1]);
            if (!output.exists()) {
                logger.info("Creating " + output);
                output.mkdirs();
            } else if (!output.isDirectory()) {
                throw new IllegalArgumentException("Expecting directory for output, got  " + output);
            } else {
                logger.info("Clearing " + output);
                org.apache.commons.io.FileUtils.cleanDirectory(output);

            }
            File work = File.createTempFile("nameindex", ".work", new File("/data/tmp"));
            work.delete();
            work.mkdirs();
            NameIndexEditor editor = new NameIndexEditor(source, work);
            for (int i = 2; i < cmd.getArgs().length; i++) {
                editor.makeEdits(new File(cmd.getArgs()[i]), tabs);
            }
            editor.close();
            editor.create(output);
        } catch (ParseException ex) {
            System.err.println("Unable to parse command line: " + ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }

    }
}
