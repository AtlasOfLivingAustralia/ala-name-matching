/**
 *
 */
package au.org.ala.sds.validation;

import org.drools.KnowledgeBase;

import au.org.ala.sds.knowledgebase.KnowledgeBaseFactory;
import au.org.ala.sds.model.ConservationInstance;
import au.org.ala.sds.model.PlantPestInstance;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.model.SensitivityInstance;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class ServiceFactory {

    public static ValidationService createValidationService(SensitiveTaxon species) {
        ReportFactory reportFactory = new SdsReportFactory();
        ValidationService service = null;

        if (species.getInstances().get(0) instanceof ConservationInstance) {
            service = new ConservationService(species, reportFactory);
        } else if (species.getInstances().get(0) instanceof PlantPestInstance) {
            SensitivityInstance instance = species.getInstances().get(0);
            KnowledgeBase knowledgeBase = KnowledgeBaseFactory.getKnowledgeBase(instance.getCategory());
            service = new PlantPestService(species, knowledgeBase, reportFactory);
        }

        return service;
    }

}
