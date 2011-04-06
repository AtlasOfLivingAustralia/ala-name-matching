package au.org.ala.sds.validation;

import org.drools.runtime.KnowledgeRuntime;
import org.drools.runtime.rule.RuleContext;

import au.org.ala.sds.model.Message;

public class ValidationHelper {

    public static void alert(RuleContext drools, String msgKey) {
        addMessage(drools, Message.Type.ALERT, msgKey, new Object [] {});
    }

    public static void alert(RuleContext drools, String msgKey, Object... context) {
        addMessage(drools, Message.Type.ALERT, msgKey, context);
    }

    public static void error(RuleContext drools, String msgKey) {
        addMessage(drools, Message.Type.ERROR, msgKey, new Object [] {});
    }

    public static void error(RuleContext drools, String msgKey, Object... context) {
        addMessage(drools, Message.Type.ERROR, msgKey, context);
    }

    public static void warning(RuleContext drools, String msgKey) {
        addMessage(drools, Message.Type.WARNING, msgKey, new Object [] {});
    }

    public static void warning(RuleContext drools, String msgKey, Object... context) {
        addMessage(drools, Message.Type.WARNING, msgKey, context);
    }

    public static void information(RuleContext drools, String msgKey) {
        addMessage(drools, Message.Type.INFORMATION, msgKey, new Object [] {});
    }

    public static void information(RuleContext drools, String msgKey, Object... context) {
        addMessage(drools, Message.Type.INFORMATION, msgKey, context);
    }

    public static void addMessage(RuleContext drools, Message.Type type, String msgKey, Object... context) {
        KnowledgeRuntime knowledgeRuntime = drools.getKnowledgeRuntime();
        ValidationReport validationReport = (ValidationReport) knowledgeRuntime.getGlobal("validationReport");
        validationReport.addMessage(MessageFactory.createMessage(type, msgKey, context));
    }

}
