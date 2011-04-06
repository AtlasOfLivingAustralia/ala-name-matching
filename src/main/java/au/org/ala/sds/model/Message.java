package au.org.ala.sds.model;


public interface Message {

    public enum Type {
        ERROR, WARNING, ALERT, INFORMATION
    }

    Type getType();

    String getMessageText();
}
