/**
 *
 */
package au.org.ala.sds.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.drools.KnowledgeBase;
import org.drools.runtime.StatelessKnowledgeSession;

import au.org.ala.sds.knowledgebase.KnowledgeBaseFactory;
import au.org.ala.sds.model.Message;
import au.org.ala.sds.model.SdsMessage;
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
    public ValidationOutcome validate(Map<String, String> biocacheData) {
        FactCollection facts = new FactCollection(biocacheData);
        ValidationReport report = reportFactory.createValidationReport(taxon);
        Date date = null;

        // Validate location
        if (!ValidationUtils.validateLocation(facts, report)) {
            return new ValidationOutcome(report, false);
        }

        // Validate date
        try {
            date = DateHelper.validateDate(facts);
        } catch (Exception e) {
            report.addMessage(new SdsMessage(Message.Type.ERROR, e.getMessage(),null));
            return new ValidationOutcome(report, false);
        }

        List<SensitivityZone> zones = SensitivityZone.getListFromString(facts.get(FactCollection.ZONES_KEY));

        RuleState state = new RuleState();
        String category = taxon.getInstances().get(0).getCategory().getId();
        do {
            state.setComplete(true);
            StatelessKnowledgeSession session = knowledgeBase.newStatelessKnowledgeSession();

            session.setGlobal("validationReport", report);
            session.setGlobal("state", state);
            session.setGlobal("logger", logger);

            session.execute(getFacts(taxon, zones, date, biocacheData));

            if (!state.isComplete()) {
                if (StringUtils.isNotBlank(state.getDelegateRules())) {
                    knowledgeBase = KnowledgeBaseFactory.getKnowledgeBase(SensitivityCategoryFactory.getCategory(state.getDelegateRules()));
                    category = state.getDelegateRules();
                    state.setDelegateRules(null);
                } else {
                    throw new IllegalStateException("Delegate rules not specified.");
                }
            }
        } while (!state.isComplete());

        ValidationOutcome outcome = new ValidationOutcome(report);
        outcome.setLoadable(state.isLoadable());
        outcome.setSensitive(!state.isLoadable());
        report.setAssertion(state.getAnnotation());
        report.setCategory(category);
        outcome.setControlledAccess(state.isControlledAccess());

        //remove the properties if the final state is restricted
        if(state.isRestricted()){
            Map<String,Object> result =ValidationUtils.restrictForPests(biocacheData);
            outcome.setResult(result);
        }


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
    private Collection<Object> getFacts(SensitiveTaxon st, List<SensitivityZone> zones, Date date, Map<String,String> props) {
      ArrayList<Object> facts = new ArrayList<Object>();
      facts.add(st);
      facts.add(zones);
      facts.add(date);
      facts.add(props);
      return facts;
    }

}
