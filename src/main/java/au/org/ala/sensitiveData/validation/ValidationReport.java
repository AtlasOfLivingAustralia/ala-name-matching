package au.org.ala.sensitiveData.validation;

import java.util.Set;

import au.org.ala.sensitiveData.model.Message;

public interface ValidationReport {

    Set<Message> getMessages();

    /**
     * @return all messages of specified type in this report
     */
    Set<Message> getMessagesByType(Message.Type type);

    /**
     * @return true if this report contains message with 
     *  specified key, false otherwise
     */
    boolean contains(String messageKey);
    
    /**
     * adds specified message to this report
     */
    boolean addMessage(Message message);

}
