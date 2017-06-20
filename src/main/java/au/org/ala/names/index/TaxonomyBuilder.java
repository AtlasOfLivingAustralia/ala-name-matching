package au.org.ala.names.index;

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
            TaxonomyConfiguration config = null;

            Option o = OptionBuilder.withLongOpt("output").withDescription("Output directory - defaults to 'combined' in the current directory").hasArg().withArgName("DIR").withType(File.class).create('o');
            Option w = OptionBuilder.withLongOpt("work").withDescription("Working directory - defaults to the current directory").hasArg().withArgName("DIR").withType(File.class).create('w');
            Option c = OptionBuilder.withLongOpt("config").withDescription("Configuration file").hasArg().withArgName("FILE").withType(File.class).create('c');
            Option cl = OptionBuilder.withLongOpt("noclean").withDescription("Don't clean up work area").create();
            options.addOption(o);
            options.addOption(w);
            options.addOption(c);
            options.addOption(cl);
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
            List<NameSource> sources = Arrays.asList(cmd.getArgs()).stream().map(f -> NameSource.create((String) f)).collect(Collectors.toList());
            Taxonomy taxonomy = new Taxonomy(config, work);
            taxonomy.load(sources);
            taxonomy.resolve();
            taxonomy.createDwCA(output);
            if (cleanup)
                taxonomy.clean();
        } catch (Exception ex) {
            logger.error("Unable to combine taxa", ex);
            System.out.println(ex.getMessage());
            System.exit(1);
        }
    }
}

