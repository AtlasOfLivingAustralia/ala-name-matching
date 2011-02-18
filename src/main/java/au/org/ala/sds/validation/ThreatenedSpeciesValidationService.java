/**
 * 
 */
package au.org.ala.sds.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.drools.KnowledgeBase;
import org.drools.runtime.StatelessKnowledgeSession;

import au.org.ala.sds.model.SensitiveSpecies;
import au.org.ala.sds.model.SensitivityZone;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class ThreatenedSpeciesValidationService implements ValidationService {

    private KnowledgeBase knowledgeBase;
    private ReportFactory reportFactory;
    
    public ThreatenedSpeciesValidationService(KnowledgeBase knowledgeBase, ReportFactory reportFactory) {
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
        Boolean loadable = new Boolean(true);
        
        StatelessKnowledgeSession session = knowledgeBase.newStatelessKnowledgeSession();
        
        session.setGlobal("validationReport", report);
        session.setGlobal("loadable", loadable);
        
        session.execute(getFacts(ss, zone));
        
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
    private Collection<Object> getFacts(SensitiveSpecies ss, SensitivityZone zone) {
      ArrayList<Object> facts = new ArrayList<Object>();
      facts.add(ss);
      facts.add(zone);
      return facts;
    }
}
