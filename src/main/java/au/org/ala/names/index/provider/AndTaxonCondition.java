package au.org.ala.names.index.provider;

import au.org.ala.names.index.TaxonConceptInstance;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * A condition composed of sub-conditions, all of which need to be true.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class AndTaxonCondition extends TaxonCondition {
    @JsonProperty
    private List<TaxonCondition> and;

    /**
     * Construct an empty and condition
     */
    public AndTaxonCondition() {
        this.and = new ArrayList<>();
    }

    /**
     * Add a condition to the conjunction.
     *
     * @param condition The condition to add
     */
    public void add(TaxonCondition condition) {
        this.and.add(condition);
    }

    /**
     * See if an instanc matches.
     * <p>
     * All sub-conditions need to match for this condition to be true.
     * </p>
     * @param instance The instance to match
     *
     * @return True if all conditions match.
     */
    @Override
    public boolean match(TaxonConceptInstance instance) {
        return this.and.stream().allMatch(c -> c.match(instance));
    }
}
