package au.org.ala.names.index.provider;

import au.org.ala.names.index.TaxonConceptInstance;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * File description.
 * <p>
 * More description.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class ScoreAdjustment {
    /** The condition */
    @JsonProperty
    private TaxonCondition condition;
    /** The adjustment */
    @JsonProperty
    private int adjustment;

    public ScoreAdjustment() {
    }

    public ScoreAdjustment(TaxonCondition condition, int adjustment) {
        this.condition = condition;
        this.adjustment = adjustment;
    }

    public int adjust(int base, TaxonConceptInstance instance) {
        return this.condition.match(instance) ? base + this.adjustment : base;
    }
}
