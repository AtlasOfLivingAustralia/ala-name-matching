
package org.ala.biocache.dto;

/**
 * DTO for species group information including counts and indent levels
 * @author "Natasha Carter <natasha.carter@csiro.au>"
 */
public class SpeciesGroupDTO {
    private String name;
    private long count;
    private long speciesCount;
    private int level;

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "SpeciesGroupDTO[" + "name=" + name + ", count=" + count + ", level=" + level + ']';
    }

    /**
     * @return the speciesCount
     */
    public long getSpeciesCount() {
        return speciesCount;
    }

    /**
     * @param speciesCount the speciesCount to set
     */
    public void setSpeciesCount(long speciesCount) {
        this.speciesCount = speciesCount;
    }
    
    


}
