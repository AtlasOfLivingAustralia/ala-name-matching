package au.org.ala.names.index;

import au.org.ala.names.search.ALANameSearcher;
import au.org.ala.names.search.DwcaNameIndexer;
import au.org.ala.names.util.FileUtils;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
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
            DwcaNameIndexer indexer;
            TaxonomyConfiguration config = null;

            Option o = OptionBuilder.withLongOpt("output").withDescription("Output directory - defaults to 'combined' in the current directory").hasArg().withArgName("DIR").withType(File.class).create('o');
            Option w = OptionBuilder.withLongOpt("work").withDescription("Working directory - defaults to the current directory").hasArg().withArgName("DIR").withType(File.class).create('w');
            Option c = OptionBuilder.withLongOpt("config").withDescription("Configuration file").hasArg().withArgName("FILE").withType(File.class).create('c');
            Option r = OptionBuilder.withLongOpt("report").withDescription("Report file").hasArg().withArgName("FILE").withType(File.class).create('r');
            Option p = OptionBuilder.withLongOpt("previous").withDescription("Previous taxonomy DwCA").hasArg().withArgName("DIR").withType(File.class).create('p');
            Option ncl = OptionBuilder.withLongOpt("noclean").withDescription("Don't clean up work area").create();
            Option nc = OptionBuilder.withLongOpt("nocreate").withDescription("Don't create an output taxonomy").create();
            options.addOption(o);
            options.addOption(w);
            options.addOption(c);
            options.addOption(r);
            options.addOption(p);
            options.addOption(ncl);
            options.addOption(nc);
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
            List<NameSource> sources = Arrays.asList(cmd.getArgs()).stream().map(f -> NameSource.create(f)).collect(Collectors.toList());
            Taxonomy taxonomy = new Taxonomy(config, work);
            taxonomy.begin();
            taxonomy.load(sources);
            taxonomy.resolve();

            // Create a working index for use with the taxonomy and then resolve any unplaced vernaculars
            taxonomy.createWorkingIndex();
            taxonomy.resolveUnplacedVernacular();

            if (output != null)
                taxonomy.createDwCA(output);
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

