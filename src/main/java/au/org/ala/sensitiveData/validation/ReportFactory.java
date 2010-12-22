package au.org.ala.sensitiveData.validation;

import au.org.ala.sensitiveData.model.Message;

public interface ReportFactory {
    ValidationReport createValidationReport();

    Message createMessage(Message.Type type, String messageKey, Object... context);

}
