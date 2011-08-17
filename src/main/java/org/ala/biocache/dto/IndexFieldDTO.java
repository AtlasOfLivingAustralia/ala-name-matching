package org.ala.biocache.dto;

/**
 * DTO for the fields that belong to the index.
 * 
 * A field is available for faceting IFF indexed=true and stored=true
 * 
 * @author "Natasha Carter <Natasha.Carter@csiro.au>"
 */
public class IndexFieldDTO {
    /** The name of the field in the index */
    private String name;
    /** The SOLR data type for the field */
    private String dataType;
    /** True when the field is available in the index for searching purposes */
    private boolean indexed;
    /** True when the field is available for extraction in search results */
    private boolean stored;
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return the dataType
     */
    public String getDataType() {
        return dataType;
    }
    /**
     * @param dataType the dataType to set
     */
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
    /**
     * @return the indexed
     */
    public boolean isIndexed() {
        return indexed;
    }
    /**
     * @param indexed the indexed to set
     */
    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }
    /**
     * @return the stored
     */
    public boolean isStored() {
        return stored;
    }
    /**
     * @param stored the stored to set
     */
    public void setStored(boolean stored) {
        this.stored = stored;
    }
    
    
}
