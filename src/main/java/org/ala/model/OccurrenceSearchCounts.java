/* *************************************************************************
 *  Copyright (C) 2009 Atlas of Living Australia
 *  All Rights Reserved.
 *  
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *  
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/

package org.ala.model;

import java.util.Map;

/**
 * Bean to store occurrence search counts via SOLR facet search
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class OccurrenceSearchCounts {
    
    protected Long recordCount;

    protected Integer speciesCount;

    protected Integer genusCount;

    protected Integer familyCount;

    protected Map<String, Long> basisOfRecordCounts;

    @Override
    public String toString() {
        // TODO add basisOfRecordCounts 
        return "record count="+recordCount+"|species count="+speciesCount+"|genus count="+
                genusCount+"|family count="+familyCount;
    }

    /*
     * Getters & Setters
     */
    public Map<String, Long> getBasisOfRecordCounts() {
        return basisOfRecordCounts;
    }

    public void setBasisOfRecordCounts(Map<String, Long> basisOfRecordCounts) {
        this.basisOfRecordCounts = basisOfRecordCounts;
    }

    public Integer getFamilyCount() {
        return familyCount;
    }

    public void setFamilyCount(Integer familyCount) {
        this.familyCount = familyCount;
    }

    public Integer getGenusCount() {
        return genusCount;
    }

    public void setGenusCount(Integer genusCount) {
        this.genusCount = genusCount;
    }

    public Long getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(Long recordCount) {
        this.recordCount = recordCount;
    }

    public Integer getSpeciesCount() {
        return speciesCount;
    }

    public void setSpeciesCount(Integer speciesCount) {
        this.speciesCount = speciesCount;
    }

}
