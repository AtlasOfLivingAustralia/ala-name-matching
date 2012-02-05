package au.org.ala.sds.validation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import au.org.ala.sds.model.Message;
import au.org.ala.sds.model.SensitiveTaxon;

public class SdsValidationReport implements ValidationReport, Serializable {

    private static final long serialVersionUID = 1287347805422093157L;

    private final SensitiveTaxon species;

    private final List<Message> messages;

    public SdsValidationReport(SensitiveTaxon species) {
        super();
        this.species = species;
        this.messages = new ArrayList<Message>();
    }

    public SensitiveTaxon getSpecies() {
        return species;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public List<Message> getMessagesByType(Message.Type type) {
        if (type == null)
            return Collections.emptyList();
        List<Message> msgList = new ArrayList<Message>();
        for (Message msg : messages) {
            if (msg.getType().equals(type)) {
                msgList.add(msg);
            }
        }
        return msgList;
    }

    public void addMessage(Message message) {
        if (message != null) {
            messages.add(message);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
//        sb.append(species.getTaxonName());
//        if (StringUtils.isNotBlank(species.getCommonName())) {
//            sb.append(" (").append(species.getCommonName()).append(")");
//        }
//        sb.append("\n");
        for (Message message : messages) {
            sb.append(message).append("\n");
        }
        return sb.toString();
    }


}
