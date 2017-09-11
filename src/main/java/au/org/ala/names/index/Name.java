package au.org.ala.names.index;


import org.gbif.api.exception.UnparsableException;
import org.gbif.api.vocabulary.NomenclaturalCode;
import org.gbif.api.vocabulary.NomenclaturalStatus;

import java.util.Set;

/**
 * A name variant.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright (c) 2017 CSIRO
 */
abstract public class Name extends TaxonomicElement {
    /** The name key */
    private NameKey key;

    /**
     * Construct for a key and name
     *
     * @param key The name name key
     */
    public Name(NameKey key) {
        this.key = key;
    }

    /**
     * Get the key associated with this name
     *
     * @return The name key
     */
    public NameKey getKey() {
        return key;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        if (this.getScientificNameAuthorship() == null)
            return this.key.getScientificName();
        return this.key.getScientificName() + " " + this.key.getScientificNameAuthorship();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScientificName() {
        return this.key.getScientificName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScientificNameAuthorship() {
        return this.key.getScientificNameAuthorship();
    }

}
