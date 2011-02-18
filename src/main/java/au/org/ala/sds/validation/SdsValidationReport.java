package au.org.ala.sds.validation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.org.ala.sds.model.Message;

public class SdsValidationReport implements ValidationReport, Serializable {

    private static final long serialVersionUID = 1287347805422093157L;

    protected Map<Message.Type, List<Message>> messagesMap = new HashMap<Message.Type, List<Message>>();

    public List<Message> getMessages() {
        List<Message> messagesAll = new ArrayList<Message>();
        for (Collection<Message> messages : messagesMap.values()) {
            messagesAll.addAll(messages);
        }
        return messagesAll;
    }

    public List<Message> getMessagesByType(Message.Type type) {
        if (type == null)
            return Collections.emptyList();
        List<Message> messages = messagesMap.get(type);
        if (messages == null)
            return Collections.emptyList();
        else
            return messages;
    }

    public boolean contains(String messageKey) {
        for (Message message : getMessages()) {
            if (messageKey.equals(message.getMessageKey())) {
                return true;
            }
        }
        return false;
    }

    public boolean addMessage(Message message) {
        if (message == null)
            return false;
        List<Message> messages = messagesMap.get(message.getType());
        if (messages == null) {
            messages = new ArrayList<Message>();
            messagesMap.put(message.getType(), messages);
        }
        return messages.add(message);
    }

}
