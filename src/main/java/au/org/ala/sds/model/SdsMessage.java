package au.org.ala.sds.model;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class SdsMessage implements Message, Serializable {

    private static final long serialVersionUID = 7415409339201655333L;
    private final Message.Type type;
    private final String messageText;

    public SdsMessage(Message.Type type, String text) {
        if (type == null || text == null) {
            throw new IllegalArgumentException(
                    "Type and message text cannot be null");
        }
        this.type = type;
        this.messageText = text;
    }

    public Message.Type getType() {
        return type;
    }

    public String getMessageText() {
        return messageText;
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
        return new ToStringBuilder(this).append("type", type).append(
                "messageText", messageText).toString();
    }
}
