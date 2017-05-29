package au.org.ala.names.index;

import java.util.ArrayList;
import java.util.List;

/**
 * A taxonomic concept. A scientific name, author and placement in
 * <p>
 * A taxon concept has a <em>resolved</em> instance, which is the particular taxonomic concept that
 * has been chosen as most representative of the concept.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2017 CSIRO
 */

public class TaxonConcept {
    /** The parent scientific name */
    private ScientificName scientificName;
    /** The name key for this concept */
    private NameKey key;
    /** The list of instances that correspond to this concept */
    private List<TaxonConceptInstance> instances;
    /** The chosen representative instance */
    private TaxonConceptInstance resolved;


    /**
     * Construct for new scientific name and a name key
     *
     * @param scientificName The parent scientific name
     * @param key The name key
     */
    public TaxonConcept(ScientificName scientificName, NameKey key) {
        this.scientificName = scientificName;
        this.key = key;
        this.instances = new ArrayList<>();
        this.resolved = null;
    }

    /**
     * Get the parent scientific name
     *
     * @return The scientific name
     */
    public ScientificName getScientificName() {
        return scientificName;
    }

    /**
     * Get the name key for this concept
     *
     * @return A code/name/author key
     */
    public NameKey getKey() {
        return key;
    }

    /**
     * Get the resolved instance.
     *
     * @return The resolved instance, or null for not resolved.
     */
    public TaxonConceptInstance getResolved() {
        return resolved;
    }

    /**
     * Add an instance to the taxon concept.
     *
     * @param instance The instance
     */
    public void addInstance(TaxonConceptInstance instance) {
        instance.setTaxonConcept(this);
        this.instances.add(instance);
    }

}
