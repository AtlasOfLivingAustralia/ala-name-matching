package au.org.ala.vocab;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A vocabulary constructed from
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
@JsonDeserialize(converter = Vocabulary.VocabularyConverter.class)
public class Vocabulary<T extends Concept<T>> extends Concept<Vocabulary<?>> {
    /** The concepts */
    @JsonBackReference
    private List<Concept<T>> concepts;
    /** The vocabulary concepts */
    private Map<URI, Concept<T>> uriConceptMap;
    /** The vocabulary names */
    private Map<String, Concept<T>> nameConceptMap;

    public Vocabulary() {
        this.concepts = new ArrayList<>();
        this.uriConceptMap = new HashMap<>();
        this.nameConceptMap = new HashMap<>();
    }

    public Vocabulary(URI uri, String id, String description) {
        super(null, uri, id, description, null, null);
        this.concepts = new ArrayList<>();
        this.uriConceptMap = new HashMap<>();
        this.nameConceptMap = new HashMap<>();
    }

    /**
     * Add a concept to the vocabulary
     *
     * @param concept The concept
     */
    public void add(Concept<T> concept) {
        this.concepts.add(concept);
        this.resolve(concept);
    }

    /**
     * Build vocabulary maps to allow get by name/get by URI
     */
    protected void resolve() {
        this.uriConceptMap = new HashMap<>(this.concepts.size());
        this.nameConceptMap = new HashMap<>(this.concepts.size());
        for (Concept<T> concept: this.concepts)
            this.resolve(concept);
    }

    /**
     * Add a concept to the lookup tables
     *
     * @param concept The concept to add
     *
     *  @throws IllegalStateException if the concept URI or name has already been added
     */
    protected void resolve(Concept<T> concept) {
        if (concept.getUri() != null) {
            if (this.uriConceptMap.containsKey(concept.getUri()))
                throw new IllegalStateException("Duplicate uri " + concept.getUri() + " for " + concept.getId());
            this.uriConceptMap.put(concept.getUri(), concept);
        }
        if (this.nameConceptMap.containsKey(concept.getId()))
            throw new IllegalStateException("Duplicate id " + concept.getId());
        this.nameConceptMap.put(concept.getId(), concept);
        if (concept.getNames() != null) {
            for (String name: concept.getNames()) {
                if (this.nameConceptMap.containsKey(concept.getId()))
                    throw new IllegalStateException("Duplicate name " + name + " for " + concept.getId());
                this.nameConceptMap.put(name, concept);
            }
        }
    }

    /**
     * Converter to allow post-construction concept maps to be built
     */
    protected static class VocabularyConverter extends StdConverter<Vocabulary<?>, Vocabulary<?>> {
        @Override
        public Vocabulary<?> convert(Vocabulary<?> value) {
            value.resolve();
            return value;
        }
    }

}
