package au.org.ala.sds.model;


public interface Message {

    public enum Type {
        ERROR, WARNING, ALERT, INFO
    }

    Type getType();

    String getMessageText();
}
