package au.org.ala.sds.model;

import java.util.List;

public interface Message {

    public enum Type {
        ERROR, WARNING, ALERT
    }
    
    Type getType();
    
    String getMessageKey();
    
    List<Object> getContextOrdered();
}
