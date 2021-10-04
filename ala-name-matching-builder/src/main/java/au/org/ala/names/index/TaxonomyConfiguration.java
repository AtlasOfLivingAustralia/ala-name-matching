package au.org.ala.names.index;

import au.org.ala.names.util.GbifModule;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import org.gbif.api.model.registry.Contact;
import org.gbif.checklistbank.authorship.AuthorComparator;
import org.gbif.utils.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.util.*;

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
    /** The logger */
    private static final Logger logger = LoggerFactory.getLogger(TaxonomyConfiguration.class);

    /** Default location of GBIF author map, from {@link AuthorComparator} */
    public static final String AUTHOR_MAP_FILENAME = "/authorship/authormap.txt";

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
    /** The cutoff score for accepted taxa */
    public int acceptedCutoff;
    /** Additional name mapping */
    public Map<String, String> authorMap;

    /**
     * Construct an empty configuration
     */
    public TaxonomyConfiguration() {
        this.id = UUID.randomUUID().toString();
        this.name = "New taxonomy configuration";
        this.nameAnalyserClass = ALANameAnalyser.class;
        this.resolverClass = ALATaxonResolver.class;
        this.providers = new ArrayList<>();
        this.acceptedCutoff = 0;
        this.authorMap = new HashMap<>();
    }

    /**
     * Write the
     * @param writer
     * @throws IOException
     */
    public void write(Writer writer) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
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
                sb.append(", ");
            sb.append(this.contact.getOrganization());
        }
        return sb.toString().trim();
    }

    /**
     * Build a priority list of boosts for the providers and other elements.
     * <p>
     * Used whem building name matching indexes.
     * </p>
     *
     * @return The resulting priorities
     */
    @JsonIgnore
    public Properties getPriorities() {
        int max = this.providers.stream().mapToInt(NameProvider::getDefaultScore).max().orElse(1);
        double scale = 2.0 / max; // Highest boost is 2
        Properties properties = new Properties();
        for (NameProvider p: this.providers)
            properties.setProperty(p.getId(), Double.toString(Math.max(0.25, p.getDefaultScore() * scale)));
        return properties;
    }

    /**
     * Create an author comparator with additional authors from the configuration.
     * <p>
     * This first grabs the standard {@link AuthorComparator} list and extends it with additional abbreviations from the
     * configuration.
     * If there is a key collision, then the local configuration wins out.
     * </p>
     *
     * @return An author comparator
     *
     * @throws IOException If unable to read the default map
     */
    public AuthorComparator newAuthorComparator() throws IOException {
        Map<String, String> authors = FileUtils.streamToMap(Resources.asByteSource(AuthorComparator.class.getResource(AUTHOR_MAP_FILENAME)).openStream(),
                Maps.<String, String>newHashMap(), 0, 2, true);
        for (String key: this.authorMap.keySet())
            if (authors.containsKey(key))
                logger.warn("Collision in author key " + key + " overriding");
        authors.putAll(this.authorMap);
        return AuthorComparator.createWithAuthormap(authors);
    }
}
