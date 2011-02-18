package au.org.ala.sds.model;

import java.util.List;

public interface Message {

    public enum Type {
        ERROR, WARNING, ALERT, INFORMATION
    }
    
    Type getType();
    
    String getMessageKey();
    
    List<Object> getContextList();
}
