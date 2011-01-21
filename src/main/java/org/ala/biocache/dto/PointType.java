package org.ala.biocache.dto;

/**
 * Enum to store the groupings or lat/long accuracy level (for zoom levels)
 *
 * label correspond to SOLR fields in $SOLR_HOME/conf/schema.xml
 * value corresponds to the "accuracy" in decimal degrees
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public enum PointType {
    POINT_1     ("point-1", 1.0f),
    POINT_01    ("point-0.1", 0.1f),
    POINT_001   ("point-0.01", 0.01f),
    POINT_0001  ("point-0.001", 0.001f),
    POINT_00001 ("point-0.0001", 0.0001f),
    POINT_RAW   ("lat_long", 0f);
    
    private String label;
    private Float value;

    PointType(String label,Float value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Float getValue() {
        return value;
    }

    public void setValue(Float value) {
        this.value = value;
    }
    
}
