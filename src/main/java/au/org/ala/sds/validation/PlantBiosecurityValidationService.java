/**
 * 
 */
package au.org.ala.sds.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.drools.KnowledgeBase;
import org.drools.runtime.StatelessKnowledgeSession;

import au.org.ala.sds.knowledgebase.KnowledgeBaseFactory;
import au.org.ala.sds.model.SensitiveSpecies;
import au.org.ala.sds.model.SensitivityZone;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class PlantBiosecurityValidationService implements ValidationService {

    private KnowledgeBase knowledgeBase;
    private ReportFactory reportFactory;
    
    public PlantBiosecurityValidationService(KnowledgeBase knowledgeBase, ReportFactory reportFactory) {
        super();
        this.knowledgeBase = knowledgeBase;
        this.reportFactory = reportFactory;
    }

    /**
     * @param ss
     * @param zone
     * @param date
     * @return
     */
    public ValidationOutcome validate(SensitiveSpecies ss, SensitivityZone zone, Date date) {
        ValidationReport report = reportFactory.createValidationReport();
        Boolean complete;
        String delegateRules = null;
        Boolean loadable = new Boolean(true);
        
        do {
            complete = true;
            StatelessKnowledgeSession session = knowledgeBase.newStatelessKnowledgeSession();
            
            session.setGlobal("validationReport", report);
            session.setGlobal("complete", complete);
            session.setGlobal("delegateRules", delegateRules);
            session.setGlobal("loadable", loadable);
            
            session.execute(getFacts(ss, zone, date));
            
            if (!complete) {
                if (delegateRules != null) {
                    knowledgeBase = KnowledgeBaseFactory.getKnowledgeBase(delegateRules);
                    delegateRules = null;
                } else {
                    throw new IllegalStateException("Delegate rules not specified.");
                }
            }
        } while (!complete);
        
        ValidationOutcome outcome = new ValidationOutcome(report, loadable);
        
        return outcome;
    }

    public void setKnowledgeBase(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public void setReportFactory(ReportFactory reportFactory) {
        this.reportFactory = reportFactory;
    }

    /**
     * @return facts that the rules will reason upon
     */
    private Collection<Object> getFacts(SensitiveSpecies ss, SensitivityZone zone, Date date) {
      ArrayList<Object> facts = new ArrayList<Object>();
      facts.add(ss);
      facts.add(zone);
      facts.add(date);
      return facts;
    }
}
