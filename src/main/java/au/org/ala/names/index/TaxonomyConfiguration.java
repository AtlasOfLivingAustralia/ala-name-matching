package au.org.ala.names.index;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
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
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class TaxonomyConfiguration {
    /** The type of name analyser */
    public Class<? extends NameAnalyser> nameAnalyserClass;
    /** The type of resolver */
    public Class<? extends TaxonResolver> resolverClass;
    /** The name providers */
    public List<NameProvider> providers;
    /** The default provider */
    public NameProvider defaultProvider;
    /** The name provider that represents inferences made by the taxon algorithm */
    public NameProvider inferenceProvider;

    /**
     * Write the
     * @param writer
     * @throws IOException
     */
    public void write(Writer writer) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        mapper.enable(SerializationFeature.INDENT_OUTPUT);
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
     * Read a configuration from a source.
     * <p>
     * The configuration is assumed to be encoded in UTF-8
     * </p>
     *
     * @param stream The configuration source.
     *
     * @return A taxonomy configuration
     *
     * @throws IOException if unable to read the configuration
     */
    public static TaxonomyConfiguration read(InputStream stream) throws IOException {
        return read(new InputStreamReader(stream, "UTF-8"));
    }

    /**
     * Validate this configuration.
     *
     * @throws IndexBuilderException if the configuration is invalid in some way
     */
    public void validate() throws IndexBuilderException {
        if (!this.providers.stream().anyMatch(p -> p == this.defaultProvider))
            throw new IndexBuilderException("Default provider not in provider list");
        if (this.inferenceProvider != null && !this.providers.stream().anyMatch(p -> p == this.inferenceProvider))
            throw new IndexBuilderException("Inference provider not in provider list");
    }
}
