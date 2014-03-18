package au.org.ala.sds.model;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class SdsMessage implements Message, Serializable {

    private static final long serialVersionUID = 7415409339201655333L;
    private final Message.Type type;
    private final String messageText;
    private final String category;

    public SdsMessage(Message.Type type, String text, String category) {
        if (type == null || text == null) {
            throw new IllegalArgumentException(
                    "Type and message text cannot be null");
        }
        this.type = type;
        this.messageText = text;
        this.category = category == null?"":category;
    }

    public Message.Type getType() {
        return type;
    }

    public String getMessageText() {
        return messageText;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other)
            return true;
        if (!(other instanceof SdsMessage))
            return false;
        SdsMessage castOther = (SdsMessage) other;
        return new EqualsBuilder().append(type, castOther.type).append(
                messageText, castOther.messageText).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(98587969, 810426655).append(type).append(
                messageText).toHashCode();
    }

    @Override
    public String toString() {
        return type + " - " + messageText;
    }
}
