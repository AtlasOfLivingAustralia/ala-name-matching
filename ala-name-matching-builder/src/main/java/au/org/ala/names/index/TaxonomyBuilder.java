/*
 * Copyright (c) 2021 Atlas of Living Australia
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

package au.org.ala.names.index;

import au.org.ala.names.search.DwcaNameIndexer;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Collect data and build a taxonomy to last the ages.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class TaxonomyBuilder {
    private static Logger logger = LoggerFactory.getLogger(TaxonomyBuilder.class);

    /**
     * Recursively find sources.
     * <p>
     * The directory and sub-directory are first searched for a meta.xml file and,
     * if present, the source is added as a DwCA.
     * Otherwise, any csv files are added to the list and subdirectories recursively
     * searched.
     * </p>
     * @param path
     * @return
     */
    protected static List<NameSource> findSources(File path) {
        List<NameSource> sources = new ArrayList<>();
        try {
            if (!path.exists()) {
                logger.info("Path does not exist " + path);
                return sources;
            }
            if (path.isFile()) {
                logger.info("Adding source file at " + path);
                sources.add(NameSource.create(path));
                return sources;
            }
            if (!path.isDirectory()) {
                logger.info("Unknown file type for " + path);
            }
            File meta = new File(path, "meta.xml");
            if (meta.exists()) {
                logger.info("Adding DwCA at " + path);
                sources.add(NameSource.create(path));
                return sources;
            } else {
                for (File f : path.listFiles()) {
                    if (f.isDirectory() || f.getName().endsWith(".csv"))
                        sources.addAll(findSources(f));
                }
            }
        } catch (Exception ex) {
            logger.error("Unable to get sources for " + path, ex);
        }
        return sources;
    }

    public static void main(String[] args) {
        try {
            Options options = new Options();
            boolean cleanup = true;
            File work = new File(System.getProperty("user.dir"));
            File output = new File(work, "combined");
            File interim;
            File index;
            File indexerTmp;
            File report;
            Integer samples = null;
            DwcaNameIndexer indexer;
            TaxonomyConfiguration config = null;
            List<NameSource> sources;

            Option o = OptionBuilder.withLongOpt("output").withDescription("Output directory - defaults to 'combined' in the current directory").hasArg().withArgName("DIR").withType(File.class).create('o');
            Option w = OptionBuilder.withLongOpt("work").withDescription("Working directory - defaults to the current directory").hasArg().withArgName("DIR").withType(File.class).create('w');
            Option c = OptionBuilder.withLongOpt("config").withDescription("Configuration file").hasArg().withArgName("FILE").withType(File.class).create('c');
            Option r = OptionBuilder.withLongOpt("report").withDescription("Report file").hasArg().withArgName("FILE").withType(File.class).create('r');
            Option p = OptionBuilder.withLongOpt("previous").withDescription("Previous taxonomy DwCA").hasArg().withArgName("DIR").withType(File.class).create('p');
            Option recurse = OptionBuilder.withLongOpt("recurse").withDescription("Input file is a directory, recurse through subdirectories").create('R');
            Option ncl = OptionBuilder.withLongOpt("noclean").withDescription("Don't clean up work area").create();
            Option nc = OptionBuilder.withLongOpt("nocreate").withDescription("Don't create an output taxonomy").create();
            Option s = OptionBuilder.withLongOpt("sample").withDescription("Output a sample taxonomy, consisting of n concepts plus their parents/accepted").hasArg().withArgName("N").withType(Integer.class).create();
            options.addOption(o);
            options.addOption(w);
            options.addOption(c);
            options.addOption(r);
            options.addOption(p);
            options.addOption(recurse);
            options.addOption(ncl);
            options.addOption(nc);
            options.addOption(s);
            CommandLineParser parser = new BasicParser();
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("noclean"))
                cleanup = false;
            if (cmd.hasOption('o'))
                output = (File) cmd.getOptionObject('o');
            if (cmd.hasOption('w'))
                work = (File) cmd.getOptionObject('w');
            if (cmd.hasOption('c')) {
                File cf = (File) cmd.getOptionObject('c');
                if (!cf.exists())
                    throw new IllegalArgumentException("Configuration file " + cf + " does not exist");
                config = TaxonomyConfiguration.read(new FileInputStream(cf));
            }
            if (cmd.hasOption('r')) {
                report = (File) cmd.getOptionObject('r');
            } else {
                report = new File(work, "taxonomy_report.csv");
            }
            if (cmd.hasOption("nocreate"))
                output = null;
            if (cmd.hasOption("previous")) {
                cmd.getOptionValues("previous");
            }
            if (cmd.hasOption("sample")) {
                samples = Integer.parseInt(cmd.getOptionValue("sample"));
            }
            if (cmd.hasOption("recurse")) {
                sources = Arrays.asList(cmd.getArgs()).stream().map(File::new).map(f -> findSources(f)).flatMap(List::stream).collect(Collectors.toList());
            } else {
                sources = Arrays.asList(cmd.getArgs()).stream().map(File::new).map(f -> NameSource.create(f)).collect(Collectors.toList());
            }
            Taxonomy taxonomy = new Taxonomy(config, work);
            taxonomy.begin();
            taxonomy.load(sources);
            taxonomy.resolve();

            // Create a working index for use with the taxonomy and then resolve any unplaced vernaculars
            taxonomy.createWorkingIndex();
            taxonomy.resolveUnplacedVernacular();

            if (samples != null)
                taxonomy.sample(samples);
            if (output != null) {
                output.mkdirs();
                taxonomy.createDwCA(output);
            }
            if (report != null)
                taxonomy.createReport(report);
            taxonomy.close();
            if (cleanup)
                taxonomy.clean();
        } catch (Exception ex) {
            logger.error("Unable to combine taxa", ex);
            System.out.println(ex.getMessage());
            System.exit(1);
        }
    }
}

