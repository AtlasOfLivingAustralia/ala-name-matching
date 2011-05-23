/**
 *
 */
package au.org.ala.sds.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.drools.KnowledgeBase;
import org.drools.runtime.StatelessKnowledgeSession;

import au.org.ala.sds.knowledgebase.KnowledgeBaseFactory;
import au.org.ala.sds.model.SensitiveTaxon;
import au.org.ala.sds.model.SensitivityCategoryFactory;
import au.org.ala.sds.model.SensitivityZone;
import au.org.ala.sds.util.DateHelper;
import au.org.ala.sds.util.ValidationUtils;

/**
 *
 * @author Peter Flemming (peter.flemming@csiro.au)
 */
public class PlantPestService implements ValidationService {

    protected static final Logger logger = Logger.getLogger(PlantPestService.class);

    private KnowledgeBase knowledgeBase;
    private ReportFactory reportFactory;
    private final SensitiveTaxon taxon;

    public PlantPestService(SensitiveTaxon taxon, KnowledgeBase knowledgeBase, ReportFactory reportFactory) {
        super();
        this.taxon = taxon;
        this.knowledgeBase = knowledgeBase;
        this.reportFactory = reportFactory;
    }

    /**
     * @param taxon
     * @param facts
     * @return
     */
    public ValidationOutcome validate(FactCollection facts) {
        ValidationReport report = reportFactory.createValidationReport(taxon);

        if (!ValidationUtils.validateLocation(facts, report)) {
            return new ValidationOutcome(report, false);
        }

        List<SensitivityZone> zones = SensitivityZone.getListFromString(facts.get(FactCollection.ZONES_KEY));
        Date date = facts.get(FactCollection.EVENT_DATE_KEY) == null ? null : DateHelper.parseDate(facts.get(FactCollection.EVENT_DATE_KEY));
        RuleState state = new RuleState();

        do {
            state.setComplete(true);
            StatelessKnowledgeSession session = knowledgeBase.newStatelessKnowledgeSession();

            session.setGlobal("validationReport", report);
            session.setGlobal("state", state);
            session.setGlobal("logger", logger);

            session.execute(getFacts(taxon, zones, date));

            if (!state.isComplete()) {
                if (StringUtils.isNotBlank(state.getDelegateRules())) {
                    knowledgeBase = KnowledgeBaseFactory.getKnowledgeBase(SensitivityCategoryFactory.getCategory(state.getDelegateRules()));
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
    private Collection<Object> getFacts(SensitiveTaxon st, List<SensitivityZone> zones, Date date) {
      ArrayList<Object> facts = new ArrayList<Object>();
      facts.add(st);
      facts.add(zones);
      facts.add(date);
      return facts;
    }

}
