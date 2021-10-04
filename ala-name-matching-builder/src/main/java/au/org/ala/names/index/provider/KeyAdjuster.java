package au.org.ala.names.index.provider;

import au.org.ala.names.index.NameKey;
import au.org.ala.names.index.TaxonConceptInstance;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Adjust name keys, based on specifiec properties.
 *
 * @see KeyAdjustment
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class KeyAdjuster {
    @JsonProperty
    private List<KeyAdjustment> adjustments;

    public KeyAdjuster() {
        this.adjustments = new ArrayList<>();
    }

    public void addAdjustment(KeyAdjustment adjustment) {
        this.adjustments.add(adjustment);
    }

    public NameKey adjustKey(NameKey base, TaxonConceptInstance instance) {
        return this.adjustments.stream().reduce(base, (key, adjuster) -> adjuster.adjust(key, instance), (a, b) -> a);
    }
}
