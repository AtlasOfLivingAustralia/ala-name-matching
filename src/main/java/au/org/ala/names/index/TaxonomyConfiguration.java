package au.org.ala.names.index;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

/**
 * A readable description of a taxonomy construction.
 * <p>
 * This is intended to be a JSON or XML file that can be read as a configuration.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class TaxonomyConfiguration {
    /** The type of name analyser */
    public Class<? extends NameAnalyser> nameAnalyserClass;
    /** The name providers */
    public List<NameProvider> providers;
    /** The identifier for the default provider */
    public String defaultProvider;

    /**
     * Write the
     * @param writer
     * @throws IOException
     */
    public void write(Writer writer) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
        mapper.writeValue(writer, this);
    }

    /**
     * Read a configuration from a source.
     *
     * @param reader The configuration source.
     *
     * @return A taxonomy configuration
     *
     * @throws IOException if unable to read the configuration
     */
    public static TaxonomyConfiguration read(Reader reader) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(reader, TaxonomyConfiguration.class);
    }

    /**
     * Validate this configuration.
     *
     * @throws IndexBuilderException if the configuration is invalid in some way
     */
    public void validate() throws IndexBuilderException {
        if (!this.providers.stream().anyMatch(p -> p.getId().equals(this.defaultProvider)))
            throw new IndexBuilderException("Defauiklt provider does not exist");
    }
}
