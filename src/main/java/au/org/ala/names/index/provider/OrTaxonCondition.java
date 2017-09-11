package au.org.ala.names.index.provider;

import au.org.ala.names.index.TaxonConceptInstance;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * A condition composed of sub-conditions, one of which needs to be true.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
public class OrTaxonCondition extends TaxonCondition {
    @JsonProperty
    private List<TaxonCondition> any;

    /**
     * Construct an empty or condition
     */
    public OrTaxonCondition() {
        this.any = new ArrayList<>();
    }

    /**
     * Add a condition to the disjunction.
     *
     * @param condition The condition to add
     */
    public void add(TaxonCondition condition) {
        this.any.add(condition);
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
        return this.any.stream().anyMatch(c -> c.match(instance));
    }
}
