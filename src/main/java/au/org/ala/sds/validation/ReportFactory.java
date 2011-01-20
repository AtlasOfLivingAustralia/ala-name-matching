package au.org.ala.sds.validation;

import au.org.ala.sds.model.Message;

public interface ReportFactory {
    ValidationReport createValidationReport();

    Message createMessage(Message.Type type, String messageKey, Object... context);

}
