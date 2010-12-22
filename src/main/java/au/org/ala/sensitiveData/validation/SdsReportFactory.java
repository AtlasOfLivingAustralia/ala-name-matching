package au.org.ala.sensitiveData.validation;

import java.util.Arrays;

import au.org.ala.sensitiveData.model.Message;
import au.org.ala.sensitiveData.model.SdsMessage;

public class SdsReportFactory implements ReportFactory {

    public Message createMessage(Message.Type type, String messageKey, Object... context) {
        return new SdsMessage(type, messageKey, Arrays.asList(context));
    }
        
    public ValidationReport createValidationReport() {
        return new SdsValidationReport();
    }

}
