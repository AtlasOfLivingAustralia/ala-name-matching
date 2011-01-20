package au.org.ala.sds.validation;

import org.drools.runtime.KnowledgeRuntime;
import org.drools.runtime.rule.RuleContext;

import au.org.ala.sds.model.Message;

public class ValidationHelper {

    public static void alert(RuleContext drools, Object... context) {
        KnowledgeRuntime knowledgeRuntime = drools.getKnowledgeRuntime();
        ValidationReport validationReport = (ValidationReport) knowledgeRuntime.getGlobal("validationReport");
        ReportFactory reportFactory = (ReportFactory) knowledgeRuntime.getGlobal("reportFactory");

        validationReport.addMessage(reportFactory.createMessage(
                Message.Type.ALERT, drools.getRule().getName(), context));
    }

    public static void error(RuleContext drools, Object... context) {
        KnowledgeRuntime knowledgeRuntime = drools.getKnowledgeRuntime();
        ValidationReport validationReport = (ValidationReport) knowledgeRuntime.getGlobal("validationReport");
        ReportFactory reportFactory = (ReportFactory) knowledgeRuntime.getGlobal("reportFactory");

        validationReport.addMessage(reportFactory.createMessage(
                Message.Type.ERROR, drools.getRule().getName(), context));
    }

    public static void warning(RuleContext drools, Object... context) {
        KnowledgeRuntime knowledgeRuntime = drools.getKnowledgeRuntime();
        ValidationReport validationReport = (ValidationReport) knowledgeRuntime.getGlobal("validationReport");
        ReportFactory reportFactory = (ReportFactory) knowledgeRuntime.getGlobal("reportFactory");

        validationReport.addMessage(reportFactory.createMessage(
                Message.Type.WARNING, drools.getRule().getName(), context));
    }

}
