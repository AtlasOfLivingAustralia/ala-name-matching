package au.org.ala.names.index.provider;

import au.org.ala.names.index.NameKey;
import au.org.ala.names.index.TaxonConceptInstance;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A score adjustment for applying to a specific
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class ScoreAdjuster {
    @JsonProperty
    private List<TaxonCondition> forbidden;
    @JsonProperty
    private List<ScoreAdjustment> adjustments;

    public ScoreAdjuster() {
        this.forbidden = new ArrayList<>();
        this.adjustments = new ArrayList<>();
    }

    public void addForbidden(TaxonCondition condition) {
        this.forbidden.add(condition);
    }

    public void addAdjustment(ScoreAdjustment adjustment) {
        this.adjustments.add(adjustment);
    }

    /**
     * Is this instance forbidden?
     *
     * @param instance The instance
     * @param key The associated name key
     *
     * @return An explanation for forbdding this instance or null for not forbidden
     */
    public String forbid(TaxonConceptInstance instance, NameKey key) {
        for (TaxonCondition condition: this.forbidden) {
            if (condition.match(instance, key))
                return condition.explain();
        }
        return null;
    }

    public int score(int base, TaxonConceptInstance instance, NameKey key) {
        return this.adjustments.stream().reduce(base, (score, adjuster) -> adjuster.adjust(score, instance, key), (a, b) -> a);
    }
}
