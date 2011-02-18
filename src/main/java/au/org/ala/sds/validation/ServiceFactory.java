/**
 * 
 */
package au.org.ala.sds.validation;

import org.drools.KnowledgeBase;

import au.org.ala.sds.knowledgebase.KnowledgeBaseFactory;
import au.org.ala.sds.model.SensitivityCategory;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class ServiceFactory {

    public static ValidationService createValidationService(SensitivityCategory category) {
        ReportFactory reportFactory = new SdsReportFactory();
        KnowledgeBase knowledgeBase = KnowledgeBaseFactory.getKnowledgeBase(category.getValue());
        
        return new PlantBiosecurityValidationService(knowledgeBase, reportFactory);
    }
}
