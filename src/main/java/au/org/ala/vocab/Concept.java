package au.org.ala.vocab;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

/**
 * Abstract vocabulary concept.
 * <p>
 * These are modelled as data rather than enums or the like so that ... ahem ... unique source
 * vocabularies can be mapped.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
@JsonInclude(JsonInclude.Include.NON_NULL)
abstract public class Concept<T extends Concept<T>> {
    /** The concept URI */
    @JsonProperty
    private URI uri;
    /** The concept id; a unique identifier */
    @JsonProperty
    private String id;
    /** Alternative names for a concept */
    @JsonProperty
    private List<String> names;
    /** The concept description */
    @JsonProperty
    private String description;
    /** The concept vocabulary that this concept is a member of */
    @JsonManagedReference
    private Vocabulary<T> vocabulary;
    /** A parent concept */
    @JsonProperty
    private Concept<T> parent;

    public Concept() {
    }

    public Concept(Vocabulary<T> vocabulary, URI uri, String id, String description, Concept<T> parent, String... names) {
        this.vocabulary = vocabulary;
        this.uri = uri;
        this.id = id;
        this.names = names == null ? null : Arrays.asList(names);
        this.description = description;
        this.parent = parent;
    }

    public Concept(Vocabulary<T> vocabulary, String id, String description, Concept<T> parent, String... names) {
        this(vocabulary, null, id, description, parent, names);
        try {
            this.uri = new URI(this.vocabulary.getUri().getScheme(), this.vocabulary.getUri().getSchemeSpecificPart(), id);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Unable to construct concept " + id, ex);
        }
    }

    public Concept(Vocabulary<T> vocabulary, String id, String... names) {
        this(vocabulary, id, null, null, names);
    }


    /**
     * Get the URI associated with this concept.
     *
     * @return The concept URI
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Get the id of the concept.
     * <p>
     * The id is a unique identifier for this concept.
     * </p>
     *
     * @return The concept id
     */
    public String getId() {
        return id;
    }

    /**
     * Get the list of alternative names for a concept.
     *
     * @return The alternative name list
     */
    public List<String> getNames() {
        return names;
    }

    /**
     * Get the long description of the concept.
     *
     * @return The long description
     */
    public String getDescription() {
        return description;
    }


    /**
     * Get the vocabulary that the concept is part of
     *
     * @return The vocabulary
     */
    public Vocabulary<T> getVocabulary() {
        return vocabulary;
    }

    /**
     * Get the parent concept.
     *
     * @return A wider or more general version of the concept.
     */
    public Concept<T> getParent() {
        return parent;
    }

    /**
     * Write the concept to a writer
     *
     * @param writer
     * @throws IOException
     */
    public void write(Writer writer) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(writer, this);
    }
}
