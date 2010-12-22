package au.org.ala.sensitiveData.model;

import java.util.List;

public interface Message {

    public enum Type {
        ERROR, WARNING, ALERT
    }
    
    Type getType();
    
    String getMessageKey();
    
    List<Object> getContextOrdered();
}
