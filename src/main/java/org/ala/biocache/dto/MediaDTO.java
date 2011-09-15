package org.ala.biocache.dto;

import java.util.HashMap;
import java.util.Map;

public class MediaDTO {

    protected String contentType;
    protected String filePath;
    protected Map<String,String> alternativeFormats = new HashMap<String,String>();

    public Map<String, String> getAlternativeFormats() {
        return alternativeFormats;
    }

    public void setAlternativeFormats(Map<String, String> alternativeFormats) {
        this.alternativeFormats = alternativeFormats;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
