/**
 *
 */
package au.org.ala.sds.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.drools.KnowledgeBase;
import org.drools.runtime.StatelessKnowledgeSession;

import au.org.ala.sds.knowledgebase.KnowledgeBaseFactory;
import au.org.ala.sds.model.SensitiveSpecies;
import au.org.ala.sds.model.SensitivityCategory;
import au.org.ala.sds.model.SensitivityZone;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class PlantPestService implements ValidationService {

    protected static final Logger logger = Logger.getLogger(PlantPestService.class);

    private KnowledgeBase knowledgeBase;
    private ReportFactory reportFactory;

    public PlantPestService(KnowledgeBase knowledgeBase, ReportFactory reportFactory) {
        super();
        this.knowledgeBase = knowledgeBase;
        this.reportFactory = reportFactory;
    }

    /**
     * @param ss
     * @param facts
     * @return
     */
    public ValidationOutcome validate(SensitiveSpecies ss, FactCollection facts) {
        ValidationReport report = reportFactory.createValidationReport();

        if (!ValidationUtils.validateFacts(facts, report)) {
            return new ValidationOutcome(report, false);
        }

        Set<SensitivityZone> zones = SensitivityZone.getSetFromString(facts.get(FactCollection.ZONES_KEY));
        RuleState state = new RuleState();

        do {
            state.setComplete(true);
            StatelessKnowledgeSession session = knowledgeBase.newStatelessKnowledgeSession();

            session.setGlobal("validationReport", report);
            session.setGlobal("state", state);
            session.setGlobal("logger", logger);

            session.execute(getFacts(ss, zones, ValidationUtils.parseDate(facts.get(FactCollection.DATE_KEY))));

            if (!state.isComplete()) {
                if (StringUtils.isNotBlank(state.getDelegateRules())) {
                    knowledgeBase = KnowledgeBaseFactory.getKnowledgeBase(SensitivityCategory.getCategory(state.getDelegateRules()));
                    state.setDelegateRules(null);
                } else {
                    throw new IllegalStateException("Delegate rules not specified.");
                }
            }
        } while (!state.isComplete());

        ValidationOutcome outcome = new PlantPestOutcome(report);
        outcome.setAnnotation(state.getAnnotation());
        ((PlantPestOutcome) outcome).setLoadable(state.isLoadable());

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
    private Collection<Object> getFacts(SensitiveSpecies ss, Set<SensitivityZone> zones, Date date) {
      ArrayList<Object> facts = new ArrayList<Object>();
      facts.add(ss);
      facts.add(zones);
      facts.add(date);
      return facts;
    }

}
