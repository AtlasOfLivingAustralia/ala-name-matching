package au.org.ala.sds.validation;

import java.util.List;

import au.org.ala.sds.model.Message;

public interface ValidationReport {

    List<Message> getMessages();

    /**
     * @return all messages of specified type in this report
     */
    List<Message> getMessagesByType(Message.Type type);

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
