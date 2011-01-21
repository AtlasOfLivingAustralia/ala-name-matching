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

package org.ala.biocache.dto;

import java.util.ArrayList;
import java.util.List;


/**
 * DTO bean to represent an occurrence record annotation.
 * 
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
 */
public class OccurrenceAnnotationBody {
    private Integer occurrenceId;
    private Integer dataResourceId;
    private String section;  // dataset, taxomony or geospatial
    private List<OccurrenceAnnotationUpdate> fieldUpdates;
    private String comment;

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("Annotation for occurrrence record id: "
                +occurrenceId+"; data resource id: "+dataResourceId+"; section: "+
                section+"; comment: "+comment+"; ");
        for (OccurrenceAnnotationUpdate oau : fieldUpdates) {
            sb.append("field update= "+oau.toString()+"; ");
        }
        return sb.toString();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getDataResourceId() {
        return dataResourceId;
    }

    public void setDataResourceId(Integer dataResourceId) {
        this.dataResourceId = dataResourceId;
    }

    public Integer getOccurrenceId() {
        return occurrenceId;
    }

    public void setOccurrenceId(Integer occurrenceId) {
        this.occurrenceId = occurrenceId;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public List<OccurrenceAnnotationUpdate> getFieldUpdates() {
        return fieldUpdates;
    }

    public void setFieldUpdates(List<OccurrenceAnnotationUpdate> fieldUpdates) {
        this.fieldUpdates = fieldUpdates;
    }

    public void addFieldUpdate(OccurrenceAnnotationUpdate oau) {
        if (fieldUpdates == null) fieldUpdates = new ArrayList<OccurrenceAnnotationUpdate>();
        fieldUpdates.add(oau);
    }
}
