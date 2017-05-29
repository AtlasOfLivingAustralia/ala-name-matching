package au.org.ala.names.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A scientific name.
 * <p>
 * Scientific names are unique across nomenclatural codes.
 * They can have numerous taxon concepts attached to them.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2017 CSIRO
 */
public class ScientificName extends Name {
    /** The concepts that correspond to this name */
    private Map<NameKey, TaxonConcept> concepts;

    public ScientificName(NameKey key) {
        super(key);
        this.concepts = new HashMap<NameKey, TaxonConcept>();
    }

    /**
     * Add an instance to the list of concepts.
     *
     * @param instanceKey The name key for this instance
     * @param instance The instance
     *
     * @return The matched taxon concept
     */
    public TaxonConcept addInstance(NameKey instanceKey, TaxonConceptInstance instance) {
        TaxonConcept concept = this.concepts.get(instanceKey);
        if (concept == null) {
            concept = new TaxonConcept(this, instanceKey);
            this.concepts.put(instanceKey, concept);
        }
        concept.addInstance(instance);
        return concept;
    }
}
