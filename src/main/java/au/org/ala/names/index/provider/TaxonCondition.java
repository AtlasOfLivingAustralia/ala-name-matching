package au.org.ala.names.index.provider;

import au.org.ala.names.index.TaxonConceptInstance;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A possible condition that a provider can apply to a taxon instance.
 * <p>
 * Conditions follow a polymorphic composition rule,
 * so JSON serialisation uses a @class property to identify the condition type.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2017 Atlas of Living Australia
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
@JsonInclude(JsonInclude.Include.NON_NULL)
abstract public class TaxonCondition {
    /**
     * Does this condition match an instance?
     *
     * @param instance The instance to match
     *
     * @return True on a match
     */
    abstract public boolean match(TaxonConceptInstance instance);
}
