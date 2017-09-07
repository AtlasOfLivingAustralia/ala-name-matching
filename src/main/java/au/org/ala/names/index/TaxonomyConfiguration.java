package au.org.ala.names.index;

import au.org.ala.names.util.GbifModule;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.gbif.api.model.registry.Contact;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    /** The configuration identifier */
    public String id = UUID.randomUUID().toString();
    /** The configuration name/title */
    public String name;
    /** The configurationm description */
    public String description;
    /** A reference URI for the configuration */
    public URI uri;
    /** The contact information */
    public Contact contact;
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
     * Construct an empty configuration
     */
    public TaxonomyConfiguration() {
        this.id = UUID.randomUUID().toString();
        this.name = "New taxonomy configuration";
        this.nameAnalyserClass = ALANameAnalyser.class;
        this.resolverClass = ALATaxonResolver.class;
        this.providers = new ArrayList<>();
    }

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
        mapper.registerModule(new GbifModule());
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

    /**
     * Get a suitable contact name.
     *
     * @return The contact name
     */
    @JsonIgnore
    public String getContactName() {
        if (this.contact == null)
            return null;
        StringBuilder sb = new StringBuilder(32);
        if (this.contact.getFirstName() != null) {
            sb.append(this.contact.getFirstName());
        }
        if (this.contact.getLastName() != null) {
            sb.append(" ");
            sb.append(this.contact.getLastName());
        }
        if (this.contact.getOrganization() != null) {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(" ");
            sb.append(this.contact.getOrganization());
        }
        return sb.toString().trim();
    }
}
